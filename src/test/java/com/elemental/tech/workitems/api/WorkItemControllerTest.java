package com.elemental.tech.workitems.api;

import com.elemental.tech.workitems.api.dto.PageResponse;
import com.elemental.tech.workitems.api.dto.WorkItemResponse;
import com.elemental.tech.workitems.domain.InvalidStatusTransitionException;
import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.service.InvalidSortException;
import com.elemental.tech.workitems.service.StaleWorkItemException;
import com.elemental.tech.workitems.service.WorkItemNotFoundException;
import com.elemental.tech.workitems.service.WorkItemService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkItemController.class)
class WorkItemControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WorkItemService service;

    @Test
    void patchStatus() throws Exception {
        var now = Instant.now();
        when(service.updateStatus(anyLong(), any()))
                .thenReturn(new WorkItemResponse(1, 1L, "Fix login bug", null, "IN_PROGRESS", "HIGH", now, now));

        mvc.perform(patchStatus("""
                        {"status": "IN_PROGRESS", "version": 0}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void versionIsRequired() throws Exception {
        mvc.perform(patchStatus("""
                        {"status": "IN_PROGRESS"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void unknownStatus() throws Exception {
        mvc.perform(patchStatus("""
                        {"status": "ARCHIVED", "version": 0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notFound() throws Exception {
        when(service.updateStatus(anyLong(), any())).thenThrow(new WorkItemNotFoundException(1));

        mvc.perform(patchStatus("""
                        {"status": "IN_PROGRESS", "version": 0}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void badTransitionIsConflict() throws Exception {
        when(service.updateStatus(anyLong(), any()))
                .thenThrow(new InvalidStatusTransitionException(WorkItem.Status.OPEN, WorkItem.Status.DONE));

        mvc.perform(patchStatus("""
                        {"status": "DONE", "version": 0}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void staleVersionIsConflict() throws Exception {
        when(service.updateStatus(anyLong(), any())).thenThrow(new StaleWorkItemException(1, 0L, 3L));

        mvc.perform(patchStatus("""
                        {"status": "IN_PROGRESS", "version": 0}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void listWithFiltersAndPaging() throws Exception {
        when(service.list(any(), any(), any())).thenReturn(new PageResponse<>(List.of(), 2, 5, 11, 3));

        mvc.perform(get("/api/work-items?status=OPEN&priority=HIGH&page=2&size=5&sort=title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(eq(WorkItem.Status.OPEN), eq(WorkItem.Priority.HIGH), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(Sort.by("title"), pageable.getValue().getSort());
    }

    @Test
    void listDefaults() throws Exception {
        when(service.list(any(), any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/work-items")).andExpect(status().isOk());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(isNull(), isNull(), pageable.capture());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.getValue().getSort());
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        when(service.list(any(), any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

        mvc.perform(get("/api/work-items?size=5000")).andExpect(status().isOk());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(any(), any(), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void badStatusFilter() throws Exception {
        mvc.perform(get("/api/work-items?status=ARCHIVED")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void badSortField() throws Exception {
        when(service.list(any(), any(), any())).thenThrow(new InvalidSortException("password", Set.of("title")));

        mvc.perform(get("/api/work-items?sort=password")).andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder patchStatus(String json) {
        return patch("/api/work-items/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }
}
