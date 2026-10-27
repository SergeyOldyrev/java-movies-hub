package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    private final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Headers headers = ex.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");

        ex.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.getResponseHeaders().set("Content-Length", "0");

        ex.sendResponseHeaders(204, -1);
    }

    protected void sendError(HttpExchange ex, int statusCode, String message) throws IOException {
        sendError(ex, statusCode, message, null);
    }

    void sendError(HttpExchange ex, int statusCode, String message, String details) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(message, statusCode, details);

        String json = gson.toJson(errorResponse);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Headers headers = ex.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");

        ex.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}