package project.fitnessFunctions;

public class Fitness implements Comparable<Fitness>{
	private String agentName;
	private float fitnessValue;	
	
	public Fitness(String agentName, float fitnessValue) {
		this.agentName = agentName;
		this.fitnessValue = fitnessValue;
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
	public int compareTo(Fitness other) {
		if(this.equals(other))
			return 0;
		else
			return (int)(getFitnessValue() * 100) - (int)(other.getFitnessValue() * 100);
	}
	
	@Override
	public int hashCode() {
		return getAgentName().hashCode();
	}
	
}
