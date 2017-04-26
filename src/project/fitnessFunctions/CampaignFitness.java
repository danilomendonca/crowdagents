package project.fitnessFunctions;

public class CampaignFitness extends Fitness{
	
	public CampaignFitness(String agentName, float fitnessValue) {
		super(agentName, fitnessValue);
	}

	public static float fitnessValue(float batteryLevel){
		float fv = batteryLevel;
		return fv;
	}
	
	@Override
	public boolean equals(Object obj) {		
		if(obj instanceof CampaignFitness){
			CampaignFitness toCompare = (CampaignFitness) obj;
			return getAgentName().equals(toCompare.getAgentName());
		}
		return false;
	}
}
