package com.elemental.tech.workitems.persistence;

import com.elemental.tech.workitems.domain.WorkItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static com.elemental.tech.workitems.domain.WorkItem.Priority.HIGH;
import static com.elemental.tech.workitems.domain.WorkItem.Priority.LOW;
import static com.elemental.tech.workitems.domain.WorkItem.Status.IN_PROGRESS;
import static com.elemental.tech.workitems.domain.WorkItem.Status.OPEN;
import static com.elemental.tech.workitems.persistence.WorkItemSpecifications.hasPriority;
import static com.elemental.tech.workitems.persistence.WorkItemSpecifications.hasStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class WorkItemRepositoryTest {

    @Autowired
    WorkItemRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(new WorkItem("a", null, HIGH));
        repository.save(new WorkItem("b", null, LOW));
        repository.save(new WorkItem("c", null, HIGH));

        var started = new WorkItem("d", null, HIGH);
        started.transitionTo(IN_PROGRESS);
        repository.save(started);
    }

    @Test
    void noFilters() {
        assertEquals(4, repository.findAll(Specification.allOf(hasStatus(null), hasPriority(null))).size());
    }

    @Test
    void filterByStatus() {
        var result = repository.findAll(hasStatus(IN_PROGRESS));

        assertEquals(1, result.size());
        assertEquals("d", result.get(0).getTitle());
    }

    @Test
    void filterByStatusAndPriority() {
        var result = repository.findAll(Specification.allOf(hasStatus(OPEN), hasPriority(HIGH)), Sort.by("title"));

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getTitle());
        assertEquals("c", result.get(1).getTitle());
    }

    @Test
    void paging() {
        var page = repository.findAll(hasPriority(HIGH), PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "title")));

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getContent().size());
        assertEquals("a", page.getContent().get(0).getTitle());
    }
}
