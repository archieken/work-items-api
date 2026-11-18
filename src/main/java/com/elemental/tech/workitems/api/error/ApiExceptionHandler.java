package com.elemental.tech.workitems.api.error;

import com.elemental.tech.workitems.domain.InvalidStatusTransitionException;
import com.elemental.tech.workitems.service.StaleWorkItemException;
import com.elemental.tech.workitems.service.WorkItemNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(WorkItemNotFoundException.class)
    ProblemDetail handleNotFound(WorkItemNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Work item not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        return problem(HttpStatus.CONFLICT, "Invalid status transition", ex.getMessage());
    }

    @ExceptionHandler(StaleWorkItemException.class)
    ProblemDetail handleStale(StaleWorkItemException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "work item was modified concurrently; re-read and retry");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
