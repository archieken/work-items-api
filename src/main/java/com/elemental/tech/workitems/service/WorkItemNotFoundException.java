package com.elemental.tech.workitems.service;

public class WorkItemNotFoundException extends RuntimeException {

    public WorkItemNotFoundException(long id) {
        super("work item not found: " + id);
    }
}
