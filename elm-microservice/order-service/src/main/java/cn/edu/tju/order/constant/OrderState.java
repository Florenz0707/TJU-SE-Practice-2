package cn.edu.tju.order.constant;

public class OrderState {
  public static final int CANCELED = 0;
  public static final int PAID = 1;
  public static final int RECEIVED = 2;
  public static final int DELIVERY = 3;
  public static final int COMPLETE = 4;
  public static final int COMMENTED = 5;

  private OrderState() {}
}
