package com.techno.economy.api;

/**
 * Represents the outcome of an API operation that may fail due to a business rule
 * without throwing an exception. Exceptions are reserved for programming errors
 * such as null arguments or non-positive amounts.
 *
 * <p>Use {@link #isSuccess()} to branch on the result, {@link #getValue()} to read
 * the produced value on success, and {@link #getFailureReason()} to display an
 * English reason string on failure.</p>
 *
 * @param <T> the type of the value returned on success
 */
public final class ApiResult<T> {

    private final boolean success;
    private final T value;
    private final String failureReason;

    private ApiResult(boolean success, T value, String failureReason) {
        this.success = success;
        this.value = value;
        this.failureReason = failureReason;
    }

    /**
     * Creates a successful result carrying the given value.
     *
     * @param <T>   the value type
     * @param value the value produced by the operation; may be {@code null} when
     *              the operation has no meaningful return value
     * @return a successful {@code ApiResult}
     */
    public static <T> ApiResult<T> success(T value) {
        return new ApiResult<>(true, value, null);
    }

    /**
     * Creates a failed result with a human-readable English reason.
     *
     * @param <T>    the value type (unused on failure)
     * @param reason a short English explanation of why the operation failed;
     *               must not be {@code null}
     * @return a failed {@code ApiResult}
     * @throws IllegalArgumentException if {@code reason} is {@code null}
     */
    public static <T> ApiResult<T> failure(String reason) {
        if (reason == null) throw new IllegalArgumentException("failure reason must not be null");
        return new ApiResult<>(false, null, reason);
    }

    /**
     * Returns {@code true} if the operation completed successfully.
     *
     * @return {@code true} on success, {@code false} on failure
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the value produced by the operation, or {@code null} if the result is a failure.
     *
     * @return the result value, or {@code null}
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns the human-readable English reason for the failure, or {@code null} if the
     * operation succeeded.
     *
     * @return failure reason, or {@code null}
     */
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        return success
                ? "ApiResult{success, value=" + value + "}"
                : "ApiResult{failure, reason=" + failureReason + "}";
    }
}
