package project.fitness;

public class GeolocationFitness extends Fitness{
	
	public GeolocationFitness(String agentName, float fitnessValue) {
		super(agentName, fitnessValue);
	}

	public static float fitnessValue(float batteryLevel, float sensorAccuracy){
		float fv = batteryLevel * sensorAccuracy;
		return fv;
	}
	
	@Override
	public boolean equals(Object obj) {		
		if(obj instanceof GeolocationFitness){
			GeolocationFitness toCompare = (GeolocationFitness) obj;
			return getAgentName().equals(toCompare.getAgentName());
		}
		return false;
	}
}
