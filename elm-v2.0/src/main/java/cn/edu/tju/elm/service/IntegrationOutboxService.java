package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.IntegrationOutboxEvent;
import cn.edu.tju.elm.model.event.PointsOrderSuccessEvent;
import cn.edu.tju.elm.model.event.PointsReviewSuccessEvent;
import cn.edu.tju.elm.repository.IntegrationOutboxEventRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.InternalServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationOutboxService {
  private static final Logger log = LoggerFactory.getLogger(IntegrationOutboxService.class);

  public static final String EVENT_POINTS_ORDER_SUCCESS = "POINTS_ORDER_SUCCESS";
  public static final String EVENT_POINTS_REVIEW_SUCCESS = "POINTS_REVIEW_SUCCESS";

  private final IntegrationOutboxEventRepository outboxEventRepository;
  private final InternalServiceClient internalServiceClient;
  private final ObjectMapper objectMapper;

  @Value("${integration.outbox.batch-size:50}")
  private int batchSize;

  @Value("${integration.outbox.max-retries:10}")
  private int maxRetries;

  public IntegrationOutboxService(
      IntegrationOutboxEventRepository outboxEventRepository,
      InternalServiceClient internalServiceClient,
      ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.internalServiceClient = internalServiceClient;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void enqueuePointsOrderSuccess(
      Long userId, String bizId, Double amount, String eventTime, String extraInfo) {
    try {
      String payload =
          objectMapper.writeValueAsString(
              new PointsOrderSuccessEvent(userId, bizId, amount, eventTime, extraInfo));
      outboxEventRepository.save(
          IntegrationOutboxEvent.newPending(EVENT_POINTS_ORDER_SUCCESS, payload));
    } catch (Exception e) {
      throw new IllegalStateException("failed to enqueue order success event", e);
    }
  }

  @Transactional
  public void enqueuePointsReviewSuccess(
      Long userId, String bizId, Integer amount, String eventTime, String extraInfo) {
    try {
      String payload =
          objectMapper.writeValueAsString(
              new PointsReviewSuccessEvent(userId, bizId, amount, eventTime, extraInfo));
      outboxEventRepository.save(
          IntegrationOutboxEvent.newPending(EVENT_POINTS_REVIEW_SUCCESS, payload));
    } catch (Exception e) {
      throw new IllegalStateException("failed to enqueue review success event", e);
    }
  }

  @Scheduled(fixedDelayString = "${integration.outbox.dispatch-interval-ms:5000}")
  @Transactional
  public void dispatch() {
    dispatchBatch();
  }

  @Transactional
  public int dispatchBatch() {
    List<IntegrationOutboxEvent> events =
        outboxEventRepository.findDispatchable(
            List.of(IntegrationOutboxEvent.STATUS_PENDING, IntegrationOutboxEvent.STATUS_RETRY),
            LocalDateTime.now(),
            PageRequest.of(0, batchSize));

    int processed = 0;
    for (IntegrationOutboxEvent event : events) {
      try {
        boolean delivered = handleEvent(event);
        if (delivered) {
          event.setStatus(IntegrationOutboxEvent.STATUS_SENT);
          event.setProcessedAt(LocalDateTime.now());
          event.setLastError(null);
          EntityUtils.updateEntity(event);
          outboxEventRepository.save(event);
        } else {
          markRetry(event, "remote service returned unsuccessful response");
        }
      } catch (Exception e) {
        markRetry(event, e.getMessage());
      }
      processed += 1;
    }
    return processed;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getStatusSummary() {
    Map<String, Object> result = new HashMap<>();
    long pendingCount =
        outboxEventRepository.countByStatusAndDeletedFalse(IntegrationOutboxEvent.STATUS_PENDING);
    long retryCount =
        outboxEventRepository.countByStatusAndDeletedFalse(IntegrationOutboxEvent.STATUS_RETRY);
    long sentCount =
        outboxEventRepository.countByStatusAndDeletedFalse(IntegrationOutboxEvent.STATUS_SENT);
    long failedCount =
        outboxEventRepository.countByStatusAndDeletedFalse(IntegrationOutboxEvent.STATUS_FAILED);

    List<IntegrationOutboxEvent> dispatchQueue =
        outboxEventRepository.findTop20ByStatusInAndDeletedFalseOrderByCreateTimeAsc(
            List.of(IntegrationOutboxEvent.STATUS_PENDING, IntegrationOutboxEvent.STATUS_RETRY));
    List<IntegrationOutboxEvent> failedEvents =
        outboxEventRepository.findTop20ByStatusAndDeletedFalseOrderByUpdateTimeDesc(
            IntegrationOutboxEvent.STATUS_FAILED);

    result.put("pending", pendingCount);
    result.put("retry", retryCount);
    result.put("sent", sentCount);
    result.put("failed", failedCount);
    result.put("dispatchQueueTop20", dispatchQueue);
    result.put("failedTop20", failedEvents);
    return result;
  }

  @Transactional
  public boolean requeueFailedEvent(Long eventId, String reason) {
    Optional<IntegrationOutboxEvent> eventOpt = outboxEventRepository.findById(eventId);
    if (eventOpt.isEmpty()) {
      return false;
    }
    IntegrationOutboxEvent event = eventOpt.get();
    if (event.getDeleted() != null && event.getDeleted()) {
      return false;
    }
    if (!IntegrationOutboxEvent.STATUS_FAILED.equals(event.getStatus())) {
      return false;
    }
    event.setStatus(IntegrationOutboxEvent.STATUS_RETRY);
    event.setNextRetryAt(LocalDateTime.now());
    event.setLastError(reason != null && !reason.isEmpty() ? reason : "manual requeue");
    EntityUtils.updateEntity(event);
    outboxEventRepository.save(event);
    return true;
  }

  @Transactional
  public int requeueAllFailedEvents(String reason) {
    List<IntegrationOutboxEvent> failedEvents =
        outboxEventRepository.findTop20ByStatusAndDeletedFalseOrderByUpdateTimeDesc(
            IntegrationOutboxEvent.STATUS_FAILED);
    int requeued = 0;
    for (IntegrationOutboxEvent event : failedEvents) {
      event.setStatus(IntegrationOutboxEvent.STATUS_RETRY);
      event.setNextRetryAt(LocalDateTime.now());
      event.setLastError(reason != null && !reason.isEmpty() ? reason : "manual batch requeue");
      EntityUtils.updateEntity(event);
      outboxEventRepository.save(event);
      requeued += 1;
    }
    return requeued;
  }

  private void markRetry(IntegrationOutboxEvent event, String errorMessage) {
    int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
    retries += 1;
    event.setRetryCount(retries);
    event.setLastError(errorMessage);
    event.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(300, 15L * retries)));
    event.setStatus(
        retries >= maxRetries
            ? IntegrationOutboxEvent.STATUS_FAILED
            : IntegrationOutboxEvent.STATUS_RETRY);
    EntityUtils.updateEntity(event);
    outboxEventRepository.save(event);
    log.warn(
        "outbox event retry scheduled: id={}, type={}, retries={}, status={}",
        event.getId(),
        event.getEventType(),
        retries,
        event.getStatus());
  }

  private boolean handleEvent(IntegrationOutboxEvent event) throws Exception {
    if (EVENT_POINTS_ORDER_SUCCESS.equals(event.getEventType())) {
      PointsOrderSuccessEvent payload =
          objectMapper.readValue(event.getPayload(), PointsOrderSuccessEvent.class);
      return internalServiceClient.notifyOrderSuccessReliable(
          payload.getUserId(),
          payload.getBizId(),
          payload.getAmount(),
          payload.getEventTime(),
          payload.getExtraInfo());
    }
    if (EVENT_POINTS_REVIEW_SUCCESS.equals(event.getEventType())) {
      PointsReviewSuccessEvent payload =
          objectMapper.readValue(event.getPayload(), PointsReviewSuccessEvent.class);
      return internalServiceClient.notifyReviewSuccessReliable(
          payload.getUserId(),
          payload.getBizId(),
          payload.getAmount(),
          payload.getEventTime(),
          payload.getExtraInfo());
    }
    throw new IllegalArgumentException("unsupported outbox event type: " + event.getEventType());
  }
}
