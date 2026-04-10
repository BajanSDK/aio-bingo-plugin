package com.bajansdk.aiobingo;

import java.io.IOException;

/**
 * Thrown by {@link BingoApiClient} when the API returns a non-2xx HTTP status.
 * Carries the HTTP status code so callers can distinguish token errors from server errors.
 */
public class BingoApiException extends IOException {

    private final int httpCode;

    public BingoApiException(int httpCode, String message) {
        super(message);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }

    /** 401, 403, or 404 — token is invalid, revoked, or the board no longer exists. */
    public boolean isTokenInvalid() {
        return httpCode == 401 || httpCode == 403 || httpCode == 404;
    }

    /** 500+ — server-side problem, worth retrying later. */
    public boolean isServerError() {
        return httpCode >= 500;
    }
}
