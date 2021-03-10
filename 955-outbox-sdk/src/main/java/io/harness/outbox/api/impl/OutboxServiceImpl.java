package io.harness.outbox.api.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_PAGE_REQUEST;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.Event;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.OutboxEventRepository;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.springframework.data.domain.Pageable;

public class OutboxServiceImpl implements OutboxService {
  private final OutboxEventRepository outboxRepository;
  private final Gson gson;
  private final PageRequest pageRequest;

  @Inject
  public OutboxServiceImpl(OutboxEventRepository outboxRepository, Gson gson) {
    this.outboxRepository = outboxRepository;
    this.gson = gson;
    this.pageRequest = DEFAULT_OUTBOX_POLL_PAGE_REQUEST;
  }

  @Override
  public OutboxEvent save(Event event) {
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .resourceScope(event.getResourceScope())
                                  .resource(event.getResource())
                                  .eventData(gson.toJson(event.getEventData()))
                                  .eventType(event.getEventType())
                                  .build();
    return outboxRepository.save(outboxEvent);
  }

  @Override
  public PageResponse<OutboxEvent> list(PageRequest pageRequest) {
    Pageable pageable = getPageable(pageRequest);
    return getNGPageResponse(outboxRepository.findAll(pageable));
  }

  private Pageable getPageable(PageRequest pageRequest) {
    if (pageRequest == null) {
      pageRequest = this.pageRequest;
    }
    if (isEmpty(pageRequest.getSortOrders())) {
      pageRequest.setSortOrders(this.pageRequest.getSortOrders());
    }
    return getPageRequest(pageRequest);
  }

  @Override
  public boolean delete(String outboxEventId) {
    outboxRepository.deleteById(outboxEventId);
    return true;
  }
}