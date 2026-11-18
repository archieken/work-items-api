package com.elemental.tech.workitems.api;

import com.elemental.tech.workitems.api.dto.CreateWorkItemRequest;
import com.elemental.tech.workitems.api.dto.PageResponse;
import com.elemental.tech.workitems.api.dto.UpdateStatusRequest;
import com.elemental.tech.workitems.api.dto.WorkItemResponse;
import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.service.WorkItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {

    private final WorkItemService service;

    public WorkItemController(WorkItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkItemResponse create (@Valid @RequestBody CreateWorkItemRequest request) {
        return service.create(request);
    }

    @GetMapping
    public PageResponse<WorkItemResponse> list(
            @RequestParam(required = false) WorkItem.Status status,
            @RequestParam(required = false) WorkItem.Priority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, priority, pageable);
    }

    @GetMapping("/{id}")
    public WorkItemResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public WorkItemResponse updateStatus(@PathVariable long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
