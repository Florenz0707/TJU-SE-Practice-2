package cn.edu.tju.elm.exception;

public class PublicVoucherException extends RuntimeException {
    public static final String NOT_FOUND = "PublicVoucher NOT FOUND";

    public PublicVoucherException(String message) {
        super(message);
    }
}
