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

package project;

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
import jade.wrapper.StaleProxyException;
import project.fitnessFunctions.CampaignFitness;
import project.fitnessFunctions.Fitness;
import project.fitnessFunctions.GeolocationFitness;
import project.setup.RandomContexGenerator;

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
	
	private static final String CAMPAIGN_ROLE = "CAMPAIGN_ROLE";
	
	private Map<String, Set<Fitness>> fitnessTable = new ConcurrentHashMap<String, Set<Fitness>>();
	private Map<String, LinkedList<String>> assignmentTable = new ConcurrentHashMap<String, LinkedList<String>>();
	private Map<String, Integer> cardinalityTable = new HashMap<String, Integer>(); 
	private boolean updatedFitnessValues = false;
	
	private float batteryLevel = RandomContexGenerator.batteryLevel(MIN_BATTERY_LVL);
		
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
		
		cardinalityTable.put(GEO_ROLE, 2);
		
		assignmentTable.put(GEO_ROLE, new LinkedList<>());
		assignmentTable.put(CAMPAIGN_ROLE, new LinkedList<>());
				
		float geoRoleFV = GeolocationFitness.fitnessValue(
			batteryLevel, 
			RandomContexGenerator.sensorAccuracyLevel(MIN_ACCURACY_LVL)
		);		
		Set<Fitness> fitnessValues = new TreeSet<Fitness>();
		fitnessValues.add(new GeolocationFitness(getAID().getName(), geoRoleFV));
		fitnessTable.put(GEO_ROLE, fitnessValues);
		
		System.out.println(geoRoleFV);
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
		System.out.println("Node-agent "+getAID().getName()+" terminating.");
	}
	
	protected void assumeRole(String role, int p){		
		addBehaviour(gpsBehavior);
		System.out.println("Node-agent "+getAID().getName()+" assumed role " + role);
	}
	
	protected void dropRole(String role){
		removeBehaviour(gpsBehavior);
		System.out.println("Node-agent "+getAID().getName()+" droped role " + role);
	}
	
	protected void currentAssignments(){
		for(String roleName : assignmentTable.keySet()){
			System.out.println("Role " + roleName + " assigned to:");
			for(String agentName : assignmentTable.get(roleName))
				System.out.println(agentName);
		}
	}
	
	private float getFitnessValue(String agentName, String roleName) {
		for(Fitness geolocationFitness : fitnessTable.get(roleName))
			if(geolocationFitness.getAgentName().equals(agentName))
				return geolocationFitness.getFitnessValue();
		return 0;//TODO
	}
	
	private void createCampaignAgent(){
		ContainerController cc = getContainerController();
		AgentController ac;
		try {
			ac = cc.createNewAgent("CampaignAgent", "project.CampaignAgent",null);
			ac.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	private class GPSBehavior extends TickerBehaviour {
		
		public GPSBehavior(Agent agent, long period){
			super(agent, period);
		}
		
		protected void onTick() {
			batteryLevel = batteryLevel * 0.99F;
		}
	}
	
	private class Discovery extends TickerBehaviour {
		
		public Discovery(Agent agent, long period){
			super(agent, period);
		}
		
		protected void onTick() {
			System.out.println("Checking for other node agents");
			// Update the list of seller agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("node-agent");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				System.out.println("Found the following node agents:");
				for (int i = 0; i < result.length; ++i) {
					System.out.println(result[i].getName().getName());
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
			if (msg != null) {
				if(msg.getContent() != null){
					String senderName = msg.getSender().getName();
					float fitnessValue = Float.parseFloat(msg.getContent());
					if(msg.getConversationId().equals("geo-role-fv")){
						CampaignFitness campaignFitness = new CampaignFitness(senderName, fitnessValue);
						fitnessTable.get(CAMPAIGN_ROLE).remove(campaignFitness);
						fitnessTable.get(CAMPAIGN_ROLE).add(campaignFitness);
						System.out.println("Updated the fitness for the role " + CAMPAIGN_ROLE + " for agent " + senderName);
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
		
		public ReassignRoles(Agent agent, long period){
			super(agent, period);
		}
		
		protected void onTick() {
			if(updatedFitnessValues){
				updatedFitnessValues = false;
				System.out.println("Checking for reassignment after fitness value update");																									
				
				boolean assume = false;
				for(String roleName : fitnessTable.keySet()){	
					assignmentTable.get(roleName).clear();
					Iterator <Fitness> it = fitnessTable.get(roleName).iterator();
					for(int i = 0; it.hasNext() && i < cardinalityTable.get(GEO_ROLE); i++){
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
				currentAssignments();
			}
		}
	} 
	
	private class InformFitness extends TickerBehaviour {
		
		public InformFitness(Agent agent, long period){
			super(agent, period);
		}
		
		public void onTick() {
			checkCampaignFitness();
			checkGPSFitness();
		}
		
		private void checkCampaignFitness(){
			float fv = batteryLevel;			
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
			System.out.println(getAID().getName() + ": Campaign fitness value informed with value " + newRoleFV);
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
			System.out.println(getAID().getName() + ": Geolocation Fitness value informed with value " + newRoleFV);
		}
	}
}
