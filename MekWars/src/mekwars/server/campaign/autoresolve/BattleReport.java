package server.campaign.autoresolve;

public class BattleReport {

	private StringBuilder report = new StringBuilder();
	
	
	public void addMovementEvent(VirtualUnit unit, String text){
		report.append("M: " + text);
	}
	
	public void addFireEvent(VirtualUnit unit, String text){
		report.append("F: " + text);
	}

	public StringBuilder getReport() {
		return report;
	}

	public void addTargetEvent(VirtualUnit unit, VirtualUnit target) {
		report.append("T: " + unit.getUnit().getSmallDescription() + " has targeted " + target.getUnit().getSmallDescription() + "<br>");
	}
	
	
	
}
