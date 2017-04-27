/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package agents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import project.fitness.CampaignFitness;
import project.fitness.Fitness;
import project.fitness.GeolocationFitness;
import project.simulation.RandomContexGenerator;

public class NodeAgent extends Agent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -238427612196093820L;

	// The set of known seller agents
	private Set <AID> nodeAgents = new CopyOnWriteArraySet<AID>();
		
	public final static float MIN_BATTERY_LVL = 0.3F;
	public final static float MIN_ACCURACY_LVL = 0;
	private static final String GEO_ROLE = "GEO_ROLE";
	private GPSBehavior gpsBehavior = new GPSBehavior(this, 10 * 1000);
	private boolean geoAssignmentStarted = false;
	private boolean geoRoleActive = false;
	
	private static final String CAMPAIGN_ROLE = "CAMPAIGN_ROLE";
	private boolean campaignAgentStarted = false;
	
	private Map<String, Set<Fitness>> fitnessTable = new ConcurrentHashMap<String, Set<Fitness>>();
	private Map<String, LinkedList<String>> assignmentTable = new ConcurrentHashMap<String, LinkedList<String>>();
	private Map<String, Integer> cardinalityTable = new HashMap<String, Integer>(); 
	private boolean updatedFitnessValues = true;
	
	private float batteryLevel;
		
	protected void setup() {
		System.out.println("Hallo! Node-agent "+getAID().getName()+" is ready.");
		
		registerAgent();		
		setupFitnessTable();	
		addBehaviors();
	}

	private void registerAgent() {
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("node-agent");
		sd.setName("Role-fitness-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	private void setupFitnessTable() {
		batteryLevel = RandomContexGenerator.batteryLevel(MIN_BATTERY_LVL);
		
		cardinalityTable.put(CAMPAIGN_ROLE, 1);
		cardinalityTable.put(GEO_ROLE, 0);
		assignmentTable.put(GEO_ROLE, new LinkedList<>());
		assignmentTable.put(CAMPAIGN_ROLE, new LinkedList<>());
				
		float geoRoleFV = GeolocationFitness.fitnessValue(
			batteryLevel, 
			RandomContexGenerator.sensorAccuracyLevel(MIN_ACCURACY_LVL)
		);		
		Set<Fitness> fitnessValues = new TreeSet<Fitness>();
		fitnessValues.add(new GeolocationFitness(getAID().getName(), geoRoleFV));
		fitnessTable.put(GEO_ROLE, fitnessValues);
		
		fitnessValues = new TreeSet<Fitness>();
		float campaignRoleFV = CampaignFitness.fitnessValue(
				batteryLevel 
		);
		fitnessValues.add(new CampaignFitness(getAID().getName(), campaignRoleFV));
		fitnessTable.put(CAMPAIGN_ROLE, fitnessValues);
	}
	
	private void addBehaviors() {
		// Checks for other agents
		addBehaviour(new Discovery(this, 10 * 1000));
		// Receive from other agents their fitness values
		addBehaviour(new ReceiveFitness());
		// Inform other agents about my fitness values
		addBehaviour(new InformFitness(this, 10 * 1000));
		// Reassign roles according to the fitness values
		addBehaviour(new ReassignRoles(this, 10 * 1000));
	}
	
	protected void takeDown() {
		dropRole(GEO_ROLE);
		dropRole(CAMPAIGN_ROLE);
		System.out.println("Node-agent "+getAID().getName()+" terminating.");
	}
	
	protected void assumeRole(String role, int p){		
		if(role.equals(GEO_ROLE) && !geoRoleActive){
			geoRoleActive = true;
			addBehaviour(gpsBehavior);
			System.out.println("Node-agent "+getAID().getName()+" assumed role " + role);
		}else if(role.equals(CAMPAIGN_ROLE) && !campaignAgentStarted){
			addBehaviour(new CampaignBehavior());
			System.out.println("Node-agent "+getAID().getName()+" assumed role " + role);
		}
		
	}
	
	protected void dropRole(String role){
		if(role.equals(GEO_ROLE) && geoRoleActive){
			geoRoleActive = false;
			removeBehaviour(gpsBehavior);
			System.out.println("Node-agent "+getAID().getName()+" droped role " + role);
		}else if(role.equals(CAMPAIGN_ROLE) && campaignAgentStarted){
			destroyCampaignAgent();
			System.out.println("Node-agent "+getAID().getName()+" droped role " + role);
		}
	}
	
	protected void printCurrentAssignments(){
		System.out.println("\n");
		for(String roleName : assignmentTable.keySet()){
			System.out.println("Role " + roleName + " assigned to:");
			for(String agentName : assignmentTable.get(roleName))
				System.out.println(agentName);
		}
		System.out.println("\n");
	}
	
	private float getFitnessValue(String agentName, String roleName) {
		for(Fitness geolocationFitness : fitnessTable.get(roleName))
			if(geolocationFitness.getAgentName().equals(agentName))
				return geolocationFitness.getFitnessValue();
		return 0;//TODO
	}
	
	private void createCampaignAgent(){
		if(!campaignAgentStarted){
			System.out.println(getAID().getName() + ": starting campaign agent");
			ContainerController cc = getContainerController();
			AgentController ac;
			try {
				ac = cc.createNewAgent("CampaignAgent_" + getAID().getLocalName(), "project.CampaignAgent",null);
				ac.start();
				campaignAgentStarted = true;
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void destroyCampaignAgent(){
		System.out.println(getAID().getName() + ": destroying campaign agent");
		ContainerController cc = getContainerController();
		AgentController ac;
		try {
			ac = cc.getAgent("CampaignAgent_" + getAID().getLocalName());
			ac.kill();
			campaignAgentStarted = false;
		} catch (StaleProxyException e) {
			e.printStackTrace();
		} catch (ControllerException e) {
			e.printStackTrace();
		}
	}
	
	private class CampaignBehavior extends Behaviour{
		
		@Override
		public void action() {
			block(30 * 1000);
			createCampaignAgent();
		}
		
		@Override
		public boolean done() {
			return true;
		}
	}
	
	private class GPSBehavior extends TickerBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 6604752680048723088L;

		public GPSBehavior(Agent agent, long period){
			super(agent, period);
		}
		
		protected void onTick() {
			batteryLevel = batteryLevel * 0.99F;
		}
	}
	
	private class Discovery extends TickerBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 3443385913013815707L;

		public Discovery(Agent agent, long period){
			super(agent, period);
		}
		
		protected void onTick() {
			//System.out.println("Checking for other node agents");
			// Update the list of seller agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("node-agent");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				//System.out.println("Found the following node agents:");
				for (int i = 0; i < result.length; ++i) {
					//System.out.println(result[i].getName().getName());
					nodeAgents.add(result[i].getName());
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}
	} 
	
	private class ReceiveFitness extends CyclicBehaviour {
		
		public void action() {			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null && msg.getContent() != null) {
				if(msg.getConversationId().equals("geo-role-cd")){
					cardinalityTable.put(GEO_ROLE, Integer.parseInt(msg.getContent()));
					geoAssignmentStarted = true;
				}else{
					String senderName = msg.getSender().getName();
					float fitnessValue = Float.parseFloat(msg.getContent());
					if(msg.getConversationId().equals("campaign-role-fv")){
						CampaignFitness campaignFitness = new CampaignFitness(senderName, fitnessValue);
						fitnessTable.get(CAMPAIGN_ROLE).remove(campaignFitness);
						fitnessTable.get(CAMPAIGN_ROLE).add(campaignFitness);
					}else if(msg.getConversationId().equals("geo-role-fv")){						
						GeolocationFitness geolocationFitness = new GeolocationFitness(senderName, fitnessValue);
						fitnessTable.get(GEO_ROLE).remove(geolocationFitness);
						fitnessTable.get(GEO_ROLE).add(geolocationFitness);
					}
					updatedFitnessValues = true;
				}
			}
			else {
				block();
			}
		}
	}
	
	private class ReassignRoles extends TickerBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1971473389326392565L;

		public ReassignRoles(Agent agent, long period){
			super(agent, period);
		}
		
		protected void onTick() {
			if(updatedFitnessValues){
				updatedFitnessValues = false;
				//System.out.println("Checking for reassignment after fitness value update");																									
				for(String roleName : fitnessTable.keySet()){	
					boolean assume = false;
					assignmentTable.get(roleName).clear();
					Iterator <Fitness> it = fitnessTable.get(roleName).iterator();
					for(int i = 0; it.hasNext() && i < cardinalityTable.get(roleName); i++){
						Fitness geolocationFitness = it.next();
						assignmentTable.get(roleName).add(i, geolocationFitness.getAgentName());	
						if(geolocationFitness.getAgentName().equals(getAID().getName())){
							assume = true;
							assumeRole(roleName, i + 1);
						}
					}
					if(!assume)
						dropRole(roleName);
				}													
				printCurrentAssignments();
			}
		}
	} 
	
	private class InformFitness extends TickerBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 3812798662773749987L;

		public InformFitness(Agent agent, long period){
			super(agent, period);
		}
		
		public void onTick() {
			checkCampaignFitness();
			if(geoAssignmentStarted)
				checkGPSFitness();
		}
		
		private void checkCampaignFitness(){
			float fv = CampaignFitness.fitnessValue(batteryLevel);			
			float oldRoleFV = getFitnessValue(getAID().getName(), CAMPAIGN_ROLE);
			if(fv != oldRoleFV)				
				informCampaignFitness(getAgent(), fv);	
		}
		
		private void informCampaignFitness(Agent myAgent, float newRoleFV){
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			for (AID agent : nodeAgents) {
				cfp.addReceiver(agent);
			} 			
			cfp.setContent(newRoleFV + "");
			cfp.setConversationId("campaign-role-fv");
			cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			myAgent.send(cfp);	
			//System.out.println(getAID().getName() + ": Campaign fitness value informed with value " + newRoleFV);
		}
		
		private void checkGPSFitness(){
			float geoRoleFV = GeolocationFitness.fitnessValue(
				batteryLevel, 
				RandomContexGenerator.sensorAccuracyLevel(MIN_ACCURACY_LVL)
			);			
			float oldRoleFV = 0;
			oldRoleFV = getFitnessValue(getAID().getName(), GEO_ROLE);
			if(geoRoleFV != oldRoleFV)				
				informGPSFitness(getAgent(), geoRoleFV);	
		}
		
		private void informGPSFitness(Agent myAgent, float newRoleFV){
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			for (AID agent : nodeAgents) {
				cfp.addReceiver(agent);
			} 			
			cfp.setContent(newRoleFV + "");
			cfp.setConversationId("geo-role-fv");
			cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			myAgent.send(cfp);	
			//System.out.println(getAID().getName() + ": Geolocation Fitness value informed with value " + newRoleFV);
		}
	}
}
