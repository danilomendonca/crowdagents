package project.fitnessFunctions;

public class CampaignFitness extends Fitness implements Comparable<CampaignFitness>{
	
	public CampaignFitness(String agentName, float fitnessValue) {
		super(agentName, fitnessValue);
	}

	public static float fitnessValue(float batteryLevel, float sensorAccuracy){
		float fv = batteryLevel * sensorAccuracy;
		return fv;
	}
}
