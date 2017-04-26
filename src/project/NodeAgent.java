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
import java.util.Map;

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
import project.fitnessFunctions.GeolocationFitness;
import project.setup.RandomContexGenerator;

public class NodeAgent extends Agent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -238427612196093820L;

	// The list of known seller agents
	private AID[] nodeAgents;
		
	public final static float MIN_BATTERY_LVL = 0.3F;
	public final static float MIN_ACCURACY_LVL = 0;
	private static final String GEO_ROLE_FV = "GEO_ROLE_FV";
	public final static int GEO_ROLE_K = 3;
	private GeolocationFitness geolocationFitness;
	
	private Map<String, Map<String, Float>> fitnessTable;
	private boolean updatedFitnessValues = false;
		
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
		fitnessTable = new HashMap<String, Map<String, Float>>();
		geolocationFitness = new GeolocationFitness();
		float geoRoleFV = geolocationFitness.fitnessValue(
			RandomContexGenerator.batteryLevel(MIN_BATTERY_LVL), 
			RandomContexGenerator.sensorAccuracyLevel(MIN_ACCURACY_LVL)
		);		
		Map<String, Float> fitnessValues = new HashMap<String, Float>();
		fitnessValues.put(getAID().getName(), geoRoleFV);
		fitnessTable.put(GEO_ROLE_FV, fitnessValues);
		System.out.println(geoRoleFV);
	}
	
	private void addBehaviors() {
		// Checks for other agents
		addBehaviour(new Discovery(this, 10 * 1000));
		// Receive from other agents their fitness values
		addBehaviour(new ReceiveFitness());
		// Inform other agents about my fitness values
		addBehaviour(new InformFitness(this, 30 * 1000));
		// Reassign roles according to the fitness values
		addBehaviour(new ReassignRoles(this, 30 * 1000));
	}
	
	protected void takeDown() {
		System.out.println("Node-agent "+getAID().getName()+" terminating.");
	}
	
	protected void assumeRole(String role){
		System.out.println("Node-agent "+getAID().getName()+" assuming the " + role + " role");
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
				nodeAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					nodeAgents[i] = result[i].getName();
					System.out.println(nodeAgents[i].getName());
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
			if (msg != null &&
				!msg.getSender().getName().equals(myAgent.getName())) {
				if(msg.getContent() != null){
					String senderName = msg.getSender().getName();
					float fitnessValue = Float.parseFloat(msg.getContent());					
					fitnessTable.get(GEO_ROLE_FV).put(senderName, fitnessValue);
					updatedFitnessValues = true;
					System.out.println(getAID().getName() + ": Fitness value for agent " + senderName + " updated with value " + fitnessValue);
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
				float agentNodeFV = fitnessTable.get(GEO_ROLE_FV).get(getAID().getName());
				for(String roleName : fitnessTable.keySet())
					for(String agentName : fitnessTable.get(roleName).keySet())
						if(!agentName.equals(getAID().getName())){
							float otherAgentNodeFV = fitnessTable.get(roleName).get(agentName);
							if(otherAgentNodeFV > agentNodeFV)
								return;
						}
				
				NodeAgent.this.assumeRole(GEO_ROLE_FV);
			}
		}
	} 
	
	private class InformFitness extends TickerBehaviour {
		
		public InformFitness(Agent agent, long period){
			super(agent, period);
		}
		
		public void onTick() {
			float geoRoleFV = geolocationFitness.fitnessValue(
				RandomContexGenerator.batteryLevel(MIN_BATTERY_LVL), 
				RandomContexGenerator.sensorAccuracyLevel(MIN_ACCURACY_LVL)
			);			
			float oldRoleFV = fitnessTable.get(GEO_ROLE_FV).get(getAID().getName());
			if(geoRoleFV != oldRoleFV){
				fitnessTable.get(GEO_ROLE_FV).put(getAID().getName(), geoRoleFV);
				informFitness(getAgent());
			}
		}
		
		private void informFitness(Agent myAgent){
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			for (int i = 0; i < nodeAgents.length; ++i) {
				cfp.addReceiver(nodeAgents[i]);
			} 
			String fitnessValue = fitnessTable.get(GEO_ROLE_FV).get(getAID().getName())+ "";
			cfp.setContent(fitnessValue);
			cfp.setConversationId("geo-role-fv");
			cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			myAgent.send(cfp);	
			System.out.println(getAID().getName() + ": Fitness value informed with value " + fitnessValue);
		}
	}
}
