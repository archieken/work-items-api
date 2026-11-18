package com.elemental.tech.workitems.domain;

import org.junit.jupiter.api.Test;

import static com.elemental.tech.workitems.domain.WorkItem.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkItemTest {

    private final WorkItem item = new WorkItem("Fix login bug", null, WorkItem.Priority.HIGH);

    @Test
    void newItemStartsOpen() {
        assertEquals(OPEN, item.getStatus());
    }

    @Test
    void openToInProgressToDone() {
        item.transitionTo(IN_PROGRESS);
        item.transitionTo(DONE);

        assertEquals(DONE, item.getStatus());
    }

    @Test
    void canMoveBackToOpenFromInProgress() {
        item.transitionTo(IN_PROGRESS);
        item.transitionTo(OPEN);

        assertEquals(OPEN, item.getStatus());
    }

    @Test
    void cantSkipStraightToDone() {
        assertThrows(InvalidStatusTransitionException.class, () -> item.transitionTo(DONE));
        assertEquals(OPEN, item.getStatus());
    }

    @Test
    void cantReopenCancelledItem() {
        item.transitionTo(CANCELLED);

        assertThrows(InvalidStatusTransitionException.class, () -> item.transitionTo(OPEN));
    }

    @Test
    void doneIsFinal() {
        item.transitionTo(IN_PROGRESS);
        item.transitionTo(DONE);

        assertThrows(InvalidStatusTransitionException.class, () -> item.transitionTo(CANCELLED));
    }

    @Test
    void sameStatusIsRejected() {
        assertThrows(InvalidStatusTransitionException.class, () -> item.transitionTo(OPEN));
    }
}
