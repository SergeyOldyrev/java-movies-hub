package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final Gson gson = new Gson();
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();

            if (path.equals("/movies")) {

                String queryString = ex.getRequestURI().getQuery();

                List<Movie> moviesToReturn;

                if (queryString != null && !queryString.isEmpty()) {

                    String yearParam = extractQueryParam(queryString);

                    if (yearParam != null) {

                        try {
                            int targetYear = Integer.parseInt(yearParam);

                            moviesToReturn = store.getAllMovies().stream().filter(m -> m.getYear() == targetYear).toList();

                        } catch (NumberFormatException e) {

                            sendError(ex, 400, "Параметр 'year' должен быть числом", yearParam);
                            return;
                        }
                    } else {

                        moviesToReturn = store.getAllMovies();
                    }
                } else {

                    moviesToReturn = store.getAllMovies();
                }

                String json = gson.toJson(moviesToReturn);
                sendJson(ex, 200, json);

            } else if (path.startsWith("/movies/") && path.length() > "/movies/".length()) {

                String idString = path.substring("/movies/".length());

                try {
                    int id = Integer.parseInt(idString);

                    Optional<Movie> movieOpt = store.getMovieById(id);

                    if (movieOpt.isPresent()) {

                        String json = gson.toJson(movieOpt.get());
                        sendJson(ex, 200, json);
                    } else {

                        sendError(ex, 404, "Фильм с ID " + id + " не найден");
                    }

                } catch (NumberFormatException e) {

                    sendError(ex, 400, "Некорректный формат ID: должно быть число");
                }
            } else {
                sendJson(ex, 404, "[]");
            }
        } else if (method.equalsIgnoreCase("POST")) {
            String path = ex.getRequestURI().getPath();

            if (path.equals("/movies")) {

                String contentType = ex.getRequestHeaders().getFirst("Content-Type");

                if (!isValidJsonContentType(contentType)) {

                    ErrorResponse errorBody = new ErrorResponse("Invalid Content-Type. Expected application/json");

                    String json = gson.toJson(errorBody);

                    sendJson(ex, 415, json);
                    return;
                }
                try {

                    String jsonBody;

                    try (var inputStream = ex.getRequestBody()) {
                        jsonBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    }

                    Movie newMovie = gson.fromJson(jsonBody, Movie.class);

                    if (newMovie == null) {
                        sendError(ex, 400, "Тело запроса отсутствует или некорректно");
                        return;
                    }

                    if (newMovie.getTitle() == null || newMovie.getTitle().isBlank()) {
                        sendError(ex, 400, "Название фильма обязательно");
                        return;
                    }

                    if (newMovie.getTitle().length() > 100) {
                        sendError(ex, 400, "Название фильма слишком длинное (максимум 100 символов)");
                        return;
                    }
                    int currentYear = java.time.Year.now().getValue();

                    if (newMovie.getYear() < 1888) {
                        sendError(ex, 400, "Некорректный год: фильм не мог быть снят раньше 1888 года");
                        return;
                    }

                    if (newMovie.getYear() > currentYear + 1) {
                        sendError(ex, 400, "Некорректный год: год не может быть больше " + (currentYear + 1));
                        return;
                    }

                    int nextId = store.getNextId();

                    newMovie.setId(nextId);

                    store.addMovie(newMovie);

                    String responseJson = gson.toJson(newMovie);

                    sendJson(ex, 201, responseJson);

                } catch (com.google.gson.JsonSyntaxException e) {
                    sendError(ex, 400, "Неверный формат JSON");
                } catch (IOException e) {
                    sendError(ex, 500, "Ошибка чтения запроса");
                }
            } else {
                sendJson(ex, 404, "[]");
            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();

            if (path.startsWith("/movies/") && path.length() > "/movies/".length()) {
                String idString = path.substring("/movies/".length());

                try {
                    int id = Integer.parseInt(idString);

                    boolean removed = store.removeMovieById(id);

                    if (removed) {
                        ex.sendResponseHeaders(204, -1);
                    } else {

                        sendError(ex, 404, "Фильм с ID " + id + " не найден для удаления");
                    }

                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный формат ID: должно быть число");
                }
            } else {
                sendError(ex, 404, "Неверный путь для удаления");
            }
        } else {
            sendJson(ex, 405, "[]");
        }
    }

    private String extractQueryParam(String queryString) {

        String[] pairs = queryString.split("&");

        for (String pair : pairs) {

            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);

                if (key.equalsIgnoreCase("year")) {
                    return value;
                }
            }
        }
        return null;
    }

    private boolean isValidJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String normalized = contentType.toLowerCase();

        String mainType = normalized.split(";", 2)[0].trim();

        return mainType.equals("application/json");
    }
}




