package cn.edu.tju.core.model;

public enum ResultCodeEnum {
  SUCCESS("200", "success"),
  NOT_FOUND("404", "not found"),
  BAD_REQUEST("400", "bad request"),
  INTERNAL_ERROR("500", "internal error"),
  SERVER_ERROR("500", "server error");

  private final String code;
  private final String message;

  ResultCodeEnum(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
