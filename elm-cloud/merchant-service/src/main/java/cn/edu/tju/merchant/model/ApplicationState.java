package cn.edu.tju.merchant.model;

public class ApplicationState {
    public static final int PENDING = 1;
    public static final int APPROVED = 2;
    public static final ApplicationState APPROVE = new ApplicationState();
    public static final ApplicationState PENDING_STATE = new ApplicationState();
    
    // Some controllers passed it directly where an integer is expected:
    // e.g., setApplicationState(ApplicationState.APPROVE) where param is int
    // If param is int but ApplicationState is passed, it means ApplicationState is an enum or has constants?
    // Let's check MerchantApplicationController.java
}
