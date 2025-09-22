package cn.edu.tju.elm.model;

public class OrderState {
    public static final Integer CANCELED = 0;
    public static final Integer UNPAID = 1;
    public static final Integer DELIVERY = 2;
    public static final Integer COMPLETE = 3;
    public static final Integer COMMENTED = 4;

    public static boolean isValidOrderState(Integer orderState) {
        return CANCELED <= orderState && orderState <= COMMENTED;
    }
}
