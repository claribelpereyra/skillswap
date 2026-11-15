package com.uade.skillswap.controllers;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.domain.common.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ControllerResultTest {
    @Test void classifiesSharedErrorsAndPreservesPermissionPrecedence() {
        assertError(new PermissionDeniedException("Forbidden"), ControllerResult.ErrorType.ACCESS_DENIED);
        assertError(new BusinessRuleException("Invalid state"), ControllerResult.ErrorType.BUSINESS_RULE);
        assertError(new AccessDeniedException("No session"), ControllerResult.ErrorType.ACCESS_DENIED);
        assertError(new ResourceNotFoundException("Booking", 1), ControllerResult.ErrorType.NOT_FOUND);
        assertError(new ConcurrentUpdateException(new IllegalStateException()), ControllerResult.ErrorType.CONFLICT);
        assertError(new IllegalArgumentException("Invalid page"), ControllerResult.ErrorType.INVALID_INPUT);
    }

    @Test void unexpectedErrorsDoNotLeakTechnicalDetails() {
        var result = ControllerResult.of(() -> { throw new IllegalStateException("SQL password=secret"); });
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getData()).isNull();
        assertThat(result.getErrorType()).isEqualTo(ControllerResult.ErrorType.UNEXPECTED);
        assertThat(result.getMessage()).doesNotContain("SQL", "secret");
        var success = ControllerResult.of(() -> "ok");
        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getData()).isEqualTo("ok");
        assertThat(success.getErrorType()).isNull();
    }

    private void assertError(RuntimeException exception, ControllerResult.ErrorType expected) {
        var result = ControllerResult.of(() -> { throw exception; });
        assertThat(result.getErrorType()).isEqualTo(expected);
        assertThat(result.getMessage()).isEqualTo(exception.getMessage());
    }
}
