package project.fitnessFunctions;

public class GeolocationFitness implements Comparable<GeolocationFitness>{
	
	private String agentName;
	private float fitnessValue;	
	
	public GeolocationFitness(String agentName, float fitnessValue) {
		this.agentName = agentName;
		this.fitnessValue = fitnessValue;
	}

	public static float fitnessValue(float batteryLevel, float sensorAccuracy){
		float fv = batteryLevel * sensorAccuracy;
		return fv;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public float getFitnessValue() {
		return fitnessValue;
	}

	public void setFitnessValue(float fitnessValue) {
		this.fitnessValue = fitnessValue;
	}

	@Override
	public int compareTo(GeolocationFitness other) {
		if(this.equals(other))
			return 0;
		else
			return (int)(getFitnessValue() * 100) - (int)(other.getFitnessValue() * 100);
	}
	
	@Override
	public int hashCode() {
		return agentName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {		
		if(obj instanceof GeolocationFitness){
			GeolocationFitness toCompare = (GeolocationFitness) obj;
			return agentName.equals(toCompare.getAgentName());
		}
		return false;
	}
}
