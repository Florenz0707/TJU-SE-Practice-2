package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_outbox_event")
public class IntegrationOutboxEvent extends BaseEntity {
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_RETRY = "RETRY";
  public static final String STATUS_SENT = "SENT";
  public static final String STATUS_FAILED = "FAILED";

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @Column(name = "next_retry_at", nullable = false)
  private LocalDateTime nextRetryAt;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  public static IntegrationOutboxEvent newPending(String eventType, String payload) {
    IntegrationOutboxEvent event = new IntegrationOutboxEvent();
    event.setEventType(eventType);
    event.setPayload(payload);
    event.setStatus(STATUS_PENDING);
    event.setRetryCount(0);
    event.setNextRetryAt(LocalDateTime.now());
    event.setLastError(null);
    event.setProcessedAt(null);
    EntityUtils.setNewEntity(event);
    return event;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public LocalDateTime getNextRetryAt() {
    return nextRetryAt;
  }

  public void setNextRetryAt(LocalDateTime nextRetryAt) {
    this.nextRetryAt = nextRetryAt;
  }

  public LocalDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(LocalDateTime processedAt) {
    this.processedAt = processedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}
