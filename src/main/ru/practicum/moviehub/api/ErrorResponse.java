package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;
    private final int code;
    private final String details;

    public ErrorResponse(String error, int code, String details) {
        this.error = error;
        this.code = code;
        this.details = details;
    }

    public ErrorResponse(String error, int code) {

        this(error, code, null);
    }

    public ErrorResponse(String error) {
        this(error, 400);
    }

    public String getError() {
        return error;
    }

    public int getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }
}