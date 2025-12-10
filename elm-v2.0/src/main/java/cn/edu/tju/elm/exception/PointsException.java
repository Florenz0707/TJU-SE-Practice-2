package cn.edu.tju.elm.exception;

public class PointsException extends RuntimeException {
    public static final String ACCOUNT_NOT_FOUND = "Points Account Not Found";
    public static final String INSUFFICIENT_POINTS = "Insufficient Points";
    public static final String RULE_NOT_FOUND = "Points Rule Not Found";
    public static final String FREEZE_FAILED = "Freeze Points Failed";
    public static final String DEDUCT_FAILED = "Deduct Points Failed";
    public static final String ROLLBACK_FAILED = "Rollback Points Failed";
    public static final String INVALID_CHANNEL_TYPE = "Invalid Channel Type";
    public static final String INVALID_RECORD_TYPE = "Invalid Record Type";

    public PointsException(String message) {
        super(message);
    }
}
