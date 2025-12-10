package cn.edu.tju.elm.constant;

public class PointsRecordType {
    public static final String EARN = "EARN";
    public static final String CONSUME = "CONSUME";
    public static final String EXPIRE = "EXPIRE";

    public static boolean isValidType(String type) {
        return EARN.equals(type) || CONSUME.equals(type) || EXPIRE.equals(type);
    }
}
