package cn.edu.tju.elm.exception;

public class WalletException extends RuntimeException {
    public static final String NOT_FOUND = "Wallet Not Found";
    public static final String ADD_VOUCHER_FAILED = "Add Voucher Failed";
    public static final String ALREADY_EXISTS = "Wallet Already Exists";
    public static final String FORBIDDEN = "AUTHORITY LACKED";

    public WalletException(String message) {
        super(message);
    }
}
