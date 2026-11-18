package com.elemental.tech.workitems.service;

import com.elemental.tech.workitems.api.dto.CreateWorkItemRequest;
import com.elemental.tech.workitems.api.dto.PageResponse;
import com.elemental.tech.workitems.api.dto.UpdateStatusRequest;
import com.elemental.tech.workitems.api.dto.WorkItemResponse;
import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.persistence.WorkItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

import static com.elemental.tech.workitems.persistence.WorkItemSpecifications.hasPriority;
import static com.elemental.tech.workitems.persistence.WorkItemSpecifications.hasStatus;

@Service
public class WorkItemService {

    static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "title");

    private final WorkItemRepository repository;

    public WorkItemService(WorkItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkItemResponse create(CreateWorkItemRequest request) {
        WorkItem workItem = new WorkItem(
                request.title(),
                request.description(),
                WorkItem.Priority.valueOf(request.priority().name())
        );

        WorkItem saved = repository.save(workItem);
        return WorkItemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkItemResponse> list(WorkItem.Status status, WorkItem.Priority priority, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new InvalidSortException(order.getProperty(), SORTABLE);
            }
        }

        Pageable withTieBreak = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));

        var page = repository.findAll(Specification.allOf(hasStatus(status), hasPriority(priority)), withTieBreak)
                .map(WorkItemMapper::toResponse);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public WorkItemResponse get(long id) {
        WorkItem workItem = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("work item not found: " + id));
        return WorkItemMapper.toResponse(workItem);
    }

    @Transactional
    public WorkItemResponse updateStatus(long id, UpdateStatusRequest request) {
        WorkItem workItem = findOrThrow(id);

        if (workItem.getVersion() != request.version()) {
            throw new StaleWorkItemException(id, request.version(), workItem.getVersion());
        }

        workItem.transitionTo(WorkItem.Status.valueOf(request.status().name()));

        WorkItem saved = repository.saveAndFlush(workItem);
        return WorkItemMapper.toResponse(saved);
    }

    private WorkItem findOrThrow(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new WorkItemNotFoundException(id));
    }
}
