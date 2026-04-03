package cn.edu.tju.merchant.model;
public class R<T> {
    private Integer code;
    private String msg;
    private T data;
    public static <T> R<T> success(T data) { R<T> r = new R<>(); r.code=200; r.msg="success"; r.data=data; return r; }
    public static <T> R<T> success() { return success(null); }
    public static <T> R<T> error(String msg) { R<T> r = new R<>(); r.code=500; r.msg=msg; return r; }
    public static <T> R<T> error(Integer code, String msg) { R<T> r = new R<>(); r.code=code; r.msg=msg; return r; }
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}