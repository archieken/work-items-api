package com.elemental.tech.workitems.service;

import com.elemental.tech.workitems.api.dto.UpdateStatusRequest;
import com.elemental.tech.workitems.domain.InvalidStatusTransitionException;
import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.persistence.WorkItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.elemental.tech.workitems.api.dto.UpdateStatusRequest.Status.DONE;
import static com.elemental.tech.workitems.api.dto.UpdateStatusRequest.Status.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    @Mock
    WorkItemRepository repository;

    @InjectMocks
    WorkItemService service;

    @Test
    void updateStatus() {
        WorkItem item = itemWithVersion(2);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.saveAndFlush(item)).thenReturn(item);

        var result = service.updateStatus(1L, new UpdateStatusRequest(IN_PROGRESS, 2L));

        assertEquals("IN_PROGRESS", result.status());
    }

    @Test
    void staleVersionIsRejected() {
        when(repository.findById(1L)).thenReturn(Optional.of(itemWithVersion(5)));

        assertThrows(StaleWorkItemException.class,
                () -> service.updateStatus(1L, new UpdateStatusRequest(IN_PROGRESS, 4L)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidTransitionIsNotSaved() {
        when(repository.findById(1L)).thenReturn(Optional.of(itemWithVersion(0)));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(1L, new UpdateStatusRequest(DONE, 0L)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void unknownId() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(WorkItemNotFoundException.class,
                () -> service.updateStatus(42L, new UpdateStatusRequest(IN_PROGRESS, 0L)));
    }

    private static WorkItem itemWithVersion(long version) {
        WorkItem item = new WorkItem("Write docs", "API docs", WorkItem.Priority.LOW);
        ReflectionTestUtils.setField(item, "id", 1L);
        ReflectionTestUtils.setField(item, "version", version);
        return item;
    }
}
