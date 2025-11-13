package cn.edu.tju.elm.constant;

public class TransactionType {
    public static final Integer TOP_UP = 0;
    public static final Integer WITHDRAW = 1;
    public static final Integer PAYMENT = 2;

    public static boolean isValidTransactionType(Integer transactionType) {
        return TOP_UP <= transactionType && transactionType <= PAYMENT;
    }
}
