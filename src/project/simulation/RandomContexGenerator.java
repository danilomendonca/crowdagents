package project.simulation;

import java.util.Random;

public class RandomContexGenerator {
	
	public static float batteryLevel(float minValue){
		Random r = new Random();
		return minValue + (1 - minValue) * r.nextFloat();
	}
	
	public static float sensorAccuracyLevel(float minValue){
		Random r = new Random();
		return minValue + (1 - minValue) * r.nextFloat();
	}

}
