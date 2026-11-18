package com.elemental.tech.workitems.service;

public class StaleWorkItemException extends RuntimeException {

    public StaleWorkItemException(long id, Long expectedVersion, Long actualVersion) {
        super("work item " + id + " has been modified (expected version " + expectedVersion
                + ", current version " + actualVersion + ")");
    }
}
