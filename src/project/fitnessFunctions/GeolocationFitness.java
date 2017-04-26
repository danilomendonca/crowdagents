package project.fitnessFunctions;

public class GeolocationFitness {

	public static float fitnessValue(float batteryLevel, float sensorAccuracy){
		float fv = batteryLevel * sensorAccuracy;
		return fv;
	}
}
