package cn.edu.tju.core.model;

public class HttpResult<T> {
  private Integer code;
  private String message;
  private Boolean success;
  private T data;

  public static <T> HttpResult<T> success(T data) {
    HttpResult<T> result = new HttpResult<>();
    result.code = ResultCodeEnum.SUCCESS.getCode();
    result.message = ResultCodeEnum.SUCCESS.getMessage();
    result.success = true;
    result.data = data;
    return result;
  }

  public static <T> HttpResult<T> failure(ResultCodeEnum codeEnum, String message) {
    HttpResult<T> result = new HttpResult<>();
    result.code = codeEnum.getCode();
    result.message = message;
    result.success = false;
    return result;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public Boolean getSuccess() {
    return success;
  }

  public T getData() {
    return data;
  }
}