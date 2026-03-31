package cn.edu.tju.core.model;

public enum ResultCodeEnum {
  SUCCESS(20000, "操作成功"),
  NOT_FOUND(40400, "资源不存在"),
  SERVER_ERROR(50000, "服务器内部错误");

  private final Integer code;
  private final String message;

  ResultCodeEnum(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() { return code; }
  public String getMessage() { return message; }
}
