package com.elemental.tech.workitems.persistence;

import com.elemental.tech.workitems.domain.WorkItem;
import org.springframework.data.jpa.domain.Specification;

public final class WorkItemSpecifications {

    private WorkItemSpecifications() {
    }

    public static Specification<WorkItem> hasStatus(WorkItem.Status status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<WorkItem> hasPriority(WorkItem.Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }
}
