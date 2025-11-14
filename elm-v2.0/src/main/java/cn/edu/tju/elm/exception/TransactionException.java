package cn.edu.tju.elm.exception;

public class TransactionException extends RuntimeException {
    public static final String NOT_FOUND = "Transaction NOT FOUND";
    public static final String ALREADY_FINISHED = "Transaction ALREADY FINISHED";
    public static final String BALANCE_NOT_ENOUGH = "Balance NOT ENOUGH";
    public static final String IN_WALLET_NOT_FOUND = "InWallet NOT FOUND";
    public static final String OUT_WALLET_NOT_FOUND = "OutWallet NOT FOUND";
    public static final String UNKNOWN_EXCEPTION = "Unknown Exception";

    public TransactionException(String message) {
        super(message);
    }
}
