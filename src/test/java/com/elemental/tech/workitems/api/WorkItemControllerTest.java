package com.elemental.tech.workitems.api;

import com.elemental.tech.workitems.api.dto.WorkItemResponse;
import com.elemental.tech.workitems.domain.InvalidStatusTransitionException;
import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.service.StaleWorkItemException;
import com.elemental.tech.workitems.service.WorkItemNotFoundException;
import com.elemental.tech.workitems.service.WorkItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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

    private MockHttpServletRequestBuilder patchStatus(String json) {
        return patch("/api/work-items/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }
}
