package cn.edu.tju.core.model;

public enum ResultCodeEnum {
  SUCCESS("OK", "successful"),
  NOT_FOUND("NOT_FOUND", "not found"),
  SERVER_ERROR("GENERAL_ERROR", "server error"),
  FORBIDDEN("FORBIDDEN", "AUTHORITY LACKED");

  private final String code;
  private final String message;

  ResultCodeEnum(String code, String msg) {
    this.code = code;
    this.message = msg;
  }

  public String getCode() { return code; }
  public String getMessage() { return message; }
}
