package server.campaign.util;

public class SerializedMessage {
	private final String delimiter;
	private final StringBuilder message;
	
	public SerializedMessage(String delimiter) {
		this.delimiter = delimiter;
		this.message = new StringBuilder();
	}
	
	public void append(String element){
		message.append(element);
		message.append(delimiter);
	}

	public void append(int element){
		message.append(element);
		message.append(delimiter);
	}
	
	public void append(long element){
		message.append(element);
		message.append(delimiter);
	}
	
	public void append(double element){
		message.append(element);
		message.append(delimiter);
	}

	public void append(boolean element){
		message.append(element);
		message.append(delimiter);
	}

	
	public String getMessage(){
		return message.toString();
	}
	
	public String toString(){
		return getMessage();
	}
}
