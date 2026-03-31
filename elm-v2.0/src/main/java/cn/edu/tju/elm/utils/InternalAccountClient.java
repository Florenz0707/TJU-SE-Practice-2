package cn.edu.tju.elm.utils;

import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InternalAccountClient {
  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public InternalAccountClient(
      RestTemplate restTemplate,
      @Value("${account.service.url:http://localhost:8080/elm}") String accountServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = restTemplate;
    this.baseUrl =
        accountServiceUrl.endsWith("/")
            ? accountServiceUrl.substring(0, accountServiceUrl.length() - 1)
            : accountServiceUrl;
    this.internalServiceToken = internalServiceToken;
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Service-Token", internalServiceToken);
    return headers;
  }

  private Map<?, ?> postInternal(String path, Map<String, Object> requestBody) {
    String url = baseUrl + path;
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    return response.getBody();
  }

  private Map<?, ?> getInternal(String path) {
    String url = baseUrl + path;
    HttpEntity<Void> request = new HttpEntity<>(createHeaders());
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    return response.getBody();
  }

  private Map<?, ?> patchInternal(String path, Map<String, Object> requestBody) {
    String url = baseUrl + path;
    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);
    return response.getBody();
  }

  private boolean isSuccessResponse(Map<?, ?> body) {
    return body != null && Boolean.TRUE.equals(body.get("success"));
  }

  private Map<?, ?> readMapData(Map<?, ?> body) {
    if (!isSuccessResponse(body)) {
      return null;
    }
    Object data = body.get("data");
    if (data instanceof Map<?, ?> map) {
      return map;
    }
    return null;
  }

  private String readMessage(Map<?, ?> body) {
    Object message = body == null ? null : body.get("message");
    return message == null ? null : String.valueOf(message);
  }

  private Map<?, ?> requireMapData(Map<?, ?> body) {
    if (!isSuccessResponse(body)) {
      throw new IllegalStateException(readMessage(body));
    }
    Map<?, ?> data = readMapData(body);
    if (data == null) {
      throw new IllegalStateException(readMessage(body));
    }
    return data;
  }

  private List<?> readListData(Map<?, ?> body) {
    if (!isSuccessResponse(body)) {
      throw new IllegalStateException(readMessage(body));
    }
    Object data = body.get("data");
    if (data instanceof List<?> list) {
      return list;
    }
    throw new IllegalStateException(readMessage(body));
  }

  private static BigDecimal readBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private static Long readLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private static Boolean readBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private static LocalDateTime readDateTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(String.valueOf(value));
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public WalletSnapshot getWalletByUserId(Long userId, boolean createIfAbsent) {
    try {
      Map<?, ?> body =
          getInternal(
              "/api/inner/account/wallet/by-user/" + userId + "?createIfAbsent=" + createIfAbsent);
      Map<?, ?> data = readMapData(body);
      if (data == null) {
        return null;
      }
      return new WalletSnapshot(
          readLong(data.get("id")),
          readLong(data.get("ownerId")),
          readBigDecimal(data.get("balance")));
    } catch (Exception e) {
      System.err.println("Failed to get wallet by user id: " + e.getMessage());
      return null;
    }
  }

  public WalletVO getWalletById(Long walletId, Long operatorId, boolean isAdmin) {
    Map<?, ?> body =
        getInternal("/api/inner/account/wallet/" + walletId + "?operatorId=" + operatorId + "&isAdmin=" + isAdmin);
    return toWalletVO(requireMapData(body));
  }

  public Long getWalletOwnerById(Long walletId) {
    Map<?, ?> body = getInternal("/api/inner/account/wallet/owner/" + walletId);
    if (!isSuccessResponse(body)) {
      throw new IllegalStateException(readMessage(body));
    }
    return readLong(body.get("data"));
  }

  public WalletVO getWalletByOwnerId(Long userId) {
    WalletSnapshot snapshot = getWalletByUserId(userId, false);
    return snapshot == null ? null : new WalletVO(snapshot.walletId(), snapshot.balance(), BigDecimal.ZERO, snapshot.ownerId());
  }

  public WalletVO createWallet(Long userId) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    Map<?, ?> body = postInternal("/api/inner/account/wallet", requestBody);
    return toWalletVO(requireMapData(body));
  }

  public void addVoucher(Long walletId, BigDecimal amount) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("walletId", walletId);
    requestBody.put("amount", amount);
    Map<?, ?> body = postInternal("/api/inner/account/wallet/voucher", requestBody);
    if (!isSuccessResponse(body)) {
      throw new IllegalStateException(readMessage(body));
    }
  }

  public TransactionVO topupWallet(Long userId, BigDecimal amount) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("amount", amount);
    Map<?, ?> body = postInternal("/api/inner/account/wallet/topup", requestBody);
    return toTransactionVO(requireMapData(body));
  }

  public TransactionVO withdrawFromWallet(Long userId, BigDecimal amount) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userId", userId);
    requestBody.put("amount", amount);
    Map<?, ?> body = postInternal("/api/inner/account/wallet/withdraw", requestBody);
    return toTransactionVO(requireMapData(body));
  }

  public TransactionVO getTransactionById(Long transactionId) {
    Map<?, ?> body = getInternal("/api/inner/account/transaction/" + transactionId);
    return toTransactionVO(requireMapData(body));
  }

  public TransactionsRecord getTransactionsByWalletId(Long walletId) {
    Map<?, ?> body = getInternal("/api/inner/account/transaction/list/" + walletId);
    Map<?, ?> data = requireMapData(body);
    return new TransactionsRecord(
        toTransactionVOList(data.get("inTransactions")), toTransactionVOList(data.get("outTransactions")));
  }

  public TransactionVO createTransaction(
      BigDecimal amount, Integer type, Long inWalletId, Long outWalletId) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("amount", amount);
    requestBody.put("type", type);
    requestBody.put("inWalletId", inWalletId);
    requestBody.put("outWalletId", outWalletId);
    Map<?, ?> body = postInternal("/api/inner/account/transaction", requestBody);
    return toTransactionVO(requireMapData(body));
  }

  public TransactionVO finishTransaction(Long id, Long operatorId, boolean isAdmin) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("id", id);
    requestBody.put("operatorId", operatorId);
    requestBody.put("admin", isAdmin);
    Map<?, ?> body = patchInternal("/api/inner/account/transaction/finished", requestBody);
    return toTransactionVO(requireMapData(body));
  }

  public VoucherSnapshot getVoucherSnapshot(Long voucherId) {
    try {
      Map<?, ?> body = getInternal("/api/inner/account/voucher/" + voucherId);
      Map<?, ?> data = readMapData(body);
      if (data == null) {
        return null;
      }
      return new VoucherSnapshot(
          readLong(data.get("id")),
          readLong(data.get("ownerId")),
          readBoolean(data.get("deleted")),
          readDateTime(data.get("expiryDate")),
          readBigDecimal(data.get("faceValue")),
          readBigDecimal(data.get("threshold")));
    } catch (Exception e) {
      System.err.println("Failed to get voucher snapshot: " + e.getMessage());
      return null;
    }
  }

  private WalletVO toWalletVO(Map<?, ?> data) {
    return new WalletVO(
        readLong(data.get("id")),
        readBigDecimal(data.get("balance")),
        readBigDecimal(data.get("voucher")),
        readLong(data.get("ownerId")));
  }

  private TransactionVO toTransactionVO(Map<?, ?> data) {
    TransactionVO transactionVO = new TransactionVO();
    transactionVO.setId(readLong(data.get("id")));
    transactionVO.setAmount(readBigDecimal(data.get("amount")));
    Object type = data.get("type");
    if (type instanceof Number number) {
      transactionVO.setType(number.intValue());
    } else if (type != null) {
      transactionVO.setType(Integer.parseInt(String.valueOf(type)));
    }
    transactionVO.setInWalletId(readLong(data.get("inWalletId")));
    transactionVO.setOutWalletId(readLong(data.get("outWalletId")));
    transactionVO.setFinished(readBoolean(data.get("finished")));
    transactionVO.setRequestId(valueAsString(data.get("requestId")));
    transactionVO.setBizId(valueAsString(data.get("bizId")));
    transactionVO.setReason(valueAsString(data.get("reason")));
    return transactionVO;
  }

  private List<TransactionVO> toTransactionVOList(Object value) {
    List<TransactionVO> transactions = new ArrayList<>();
    if (!(value instanceof List<?> list)) {
      return transactions;
    }
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        transactions.add(toTransactionVO(map));
      }
    }
    return transactions;
  }

  private String valueAsString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  public boolean debitWallet(
      String requestId, Long userId, BigDecimal amount, String bizId, String reason) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("requestId", requestId);
    requestBody.put("userId", userId);
    requestBody.put("amount", amount);
    requestBody.put("bizId", bizId);
    requestBody.put("reason", reason);
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/account/wallet/debit", requestBody);
      return isSuccessResponse(responseBody) && readMapData(responseBody) != null;
    } catch (Exception e) {
      System.err.println("Failed to debit wallet: " + e.getMessage());
      return false;
    }
  }

  public boolean refundWallet(
      String requestId, Long userId, BigDecimal amount, String bizId, String reason) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("requestId", requestId);
    requestBody.put("userId", userId);
    requestBody.put("amount", amount);
    requestBody.put("bizId", bizId);
    requestBody.put("reason", reason);
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/account/wallet/refund", requestBody);
      return isSuccessResponse(responseBody) && readMapData(responseBody) != null;
    } catch (Exception e) {
      System.err.println("Failed to refund wallet: " + e.getMessage());
      return false;
    }
  }

  public boolean redeemVoucher(String requestId, Long userId, Long voucherId, String orderId) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("requestId", requestId);
    requestBody.put("userId", userId);
    requestBody.put("voucherId", voucherId);
    requestBody.put("orderId", orderId);
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/account/voucher/redeem", requestBody);
      if (!isSuccessResponse(responseBody)) {
        return false;
      }
      return Boolean.TRUE.equals(readBoolean(responseBody.get("data")));
    } catch (Exception e) {
      System.err.println("Failed to redeem voucher: " + e.getMessage());
      return false;
    }
  }

  public boolean rollbackVoucher(
      String requestId, Long userId, Long voucherId, String orderId, String reason) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("requestId", requestId);
    requestBody.put("userId", userId);
    requestBody.put("voucherId", voucherId);
    requestBody.put("orderId", orderId);
    requestBody.put("reason", reason);
    try {
      Map<?, ?> responseBody = postInternal("/api/inner/account/voucher/rollback", requestBody);
      if (!isSuccessResponse(responseBody)) {
        return false;
      }
      return Boolean.TRUE.equals(readBoolean(responseBody.get("data")));
    } catch (Exception e) {
      System.err.println("Failed to rollback voucher: " + e.getMessage());
      return false;
    }
  }

  public record WalletSnapshot(Long walletId, Long ownerId, BigDecimal balance) {}

  public record VoucherSnapshot(
      Long voucherId,
      Long ownerId,
      Boolean deleted,
      LocalDateTime expiryDate,
      BigDecimal faceValue,
      BigDecimal threshold) {}
}
