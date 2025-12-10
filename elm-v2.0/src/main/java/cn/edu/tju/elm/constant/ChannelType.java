package cn.edu.tju.elm.constant;

public class ChannelType {
    public static final String ORDER = "ORDER";
    public static final String COMMENT = "COMMENT";
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";

    public static boolean isValidChannelType(String channelType) {
        return ORDER.equals(channelType) || COMMENT.equals(channelType)
                || LOGIN.equals(channelType) || REGISTER.equals(channelType);
    }
}
