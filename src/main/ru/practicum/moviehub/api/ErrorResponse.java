package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;
    private final int code;

    public ErrorResponse(String error, int code) {
        this.error = error;
        this.code = code;
    }

    public String getError() {
        return error;
    }

    public int getCode() {
        return code;
    }
}