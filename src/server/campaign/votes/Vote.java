package server.campaign.votes;

public class Vote {
	
	//ivars
	private int voteType = 0;//default to abstainance
	private String caster;
	private String recipient;
	
	//NOTE: Abstain must always be the lowest int
	//NOTE: Negative must always be the greatest int
	public static final int POSITIVE_VOTE = 1;
	public static final int NEGATIVE_VOTE = 2;
	public static final int ABSTAIN_VOTE = 0;//polling vote?
	
	//CONSTRUCTOR
	public Vote(int i, String castingname, String receivingname) {
		voteType = i;
		caster = castingname;
		recipient = receivingname;
	}

	//METHODS
	//all getters and setters
	/**
	 * @return int vote type
	 * 
	 * Types are declared as public final ints in Vote.java
	 */
	public int getType() {
		return voteType;
	}
	
	/**
	 * 
	 * @param i type of vote to set
	 */
	public void setType(int i) {
		voteType = i;
	}
	
	/**
	 * 
	 * @return string name of vote caster
	 */
	public String getCaster() {
		return caster;
	}
	
	/**
	 * 
	 * @param s player name to set as caster
	 */
	public void setCaster(String s) {
		caster = s;
	}
	
	/**
	 * 
	 * @return String name of receiving player
	 */
	public String getRecipient() {
		return recipient;
	}
	
	/**
	 * 
	 * @param s String name to set as receiving player
	 */
	public void setRecipient(String s) {
		recipient = s;
	}
	
	/**
	 * 
	 * @param v a vote to compare against
	 * @param typeCounts boolean if type should be considered in equality
	 * @return boolean true is votes have same elements
	 * 
	 * Take two votes and see if they have the same elements:
	 * Caster, Recipient and Type.
	 */
	public boolean isEqualTo(Vote v, boolean typeCounts) {
		boolean sameCaster = false;
		boolean sameRecipient = false;
		boolean sameType = false;
		
		boolean toReturn = false;
		
		if (this.getCaster().equals(v.getCaster()))
			sameCaster = true;
		if (this.getRecipient().equals(v.getRecipient()))
			sameRecipient = true;
		if (this.getType() == v.getType())
			sameType = true;
		
		//if type doesnt matter, and the other 2 elements match
		if (!typeCounts && sameCaster && sameRecipient) {
			toReturn = true;
		}
		//else, if all match ...
		else if (sameCaster && sameRecipient && sameType) {
			toReturn= true;
		}
	
		return toReturn;
	}//end isEqualTo
	
}//end VoteManager class