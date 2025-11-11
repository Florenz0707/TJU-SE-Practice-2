package cn.edu.tju.elm.constant;

public class OrderState {
    public static final Integer CANCELED = 0;
    public static final Integer PAID = 1;
    public static final Integer ACCEPTED = 2;
    public static final Integer DELIVERY = 3;
    public static final Integer COMPLETE = 4;
    public static final Integer COMMENTED = 5;

    public static boolean isValidOrderState(Integer orderState) {
        return CANCELED <= orderState && orderState <= COMMENTED;
    }
}
