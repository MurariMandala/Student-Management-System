package com.studentmgmt.enrollment.client;

import com.studentmgmt.enrollment.exception.ServiceUnavailableException;

final class FallbackSupport {

    private FallbackSupport() {
    }

    /**
     * Business answers (404, 409) are passed through untouched. Anything else, including an open
     * circuit, a timeout, a refused connection or a 5xx, becomes a 503 for our own caller.
     * The cause chain is walked because the circuit breaker may wrap the original exception.
     */
    static RuntimeException translate(String service, Throwable cause) {
        Throwable t = cause;
        for (int depth = 0; t != null && depth < 10; depth++) {
            if (t instanceof RemoteNotFoundException notFound) {
                return notFound;
            }
            if (t instanceof RemoteConflictException conflict) {
                return conflict;
            }
            t = t.getCause();
        }
        return new ServiceUnavailableException(service + " is currently unavailable", cause);
    }
}
