package project.fitnessFunctions;

public class GeolocationFitness extends Fitness{
	
	public GeolocationFitness(String agentName, float fitnessValue) {
		super(agentName, fitnessValue);
	}

	public static float fitnessValue(float batteryLevel, float sensorAccuracy){
		float fv = batteryLevel * sensorAccuracy;
		return fv;
	}
}
