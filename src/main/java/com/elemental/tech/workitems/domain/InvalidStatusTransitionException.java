package com.elemental.tech.workitems.domain;

public class InvalidStatusTransitionException extends RuntimeException {

    private final WorkItem.Status from;
    private final WorkItem.Status to;

    public InvalidStatusTransitionException(WorkItem.Status from, WorkItem.Status to) {
        super("cannot transition work item from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public WorkItem.Status getFrom() {
        return from;
    }

    public WorkItem.Status getTo() {
        return to;
    }
}
