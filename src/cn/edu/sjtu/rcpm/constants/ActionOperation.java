package cn.edu.sjtu.rcpm.constants;

public enum ActionOperation {
	
	ADD("Add"), DELETE("Delete"), UPDATE("Update"), JUMP("Jump");
	
	private final String operation;
    private ActionOperation(String operation) { 
           this.operation = operation; 
    }
    
    @Override
    public String toString() {
        return this.operation;
    }
}
