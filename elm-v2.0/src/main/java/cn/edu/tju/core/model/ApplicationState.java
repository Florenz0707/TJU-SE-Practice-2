package cn.edu.tju.core.model;

public class ApplicationState {
    public static final Integer UNDISPOSED = 0;
    public static final Integer APPROVED = 1;
    public static final Integer REJECTED = 2;

    public static boolean isValidApplicationState(Integer applicationState) {
        return UNDISPOSED <= applicationState && applicationState <= REJECTED;
    }
}
