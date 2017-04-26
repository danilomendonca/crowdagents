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

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class CampaignAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1459331970706327956L;

	// The set of known seller agents
	private Set <AID> nodeAgents = new CopyOnWriteArraySet<AID>();
	
	private static final String GEO_ROLE_FV = "GEO_ROLE_FV";
	public final static int GEO_ROLE_K = 2;
	
	private Map<String, LinkedList<String>> assignmentTable = new ConcurrentHashMap<String, LinkedList<String>>();
	
	protected void setup() {
		System.out.println("Hallo! Campaign-agent "+getAID().getName()+" is ready.");
		
		registerAgent();		
		addBehaviors();
	}

	private void registerAgent() {
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("campaign-agent");
		sd.setName("Crowdsensing");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	private void addBehaviors() {
		// Checks for other agents
		addBehaviour(new Discovery(this, 10 * 1000));
	}
	
	protected void takeDown() {
		System.out.println("Node-agent "+getAID().getName()+" terminating.");
	}
	
	protected void currentAssignments(){
		for(String roleName : assignmentTable.keySet()){
			System.out.println("Role " + roleName + " assigned to:");
			for(String agentName : assignmentTable.get(roleName))
				System.out.println(agentName);
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
}
