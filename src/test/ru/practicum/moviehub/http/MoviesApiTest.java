package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    @BeforeAll
    static void beforeAll() {

    }

    @BeforeEach
    void beforeEach() {

    }

    @AfterAll
    static void afterAll() {

    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        MoviesStore moviesStore = new MoviesStore();

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        server.stop();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_notEmpty_returnsArray() throws Exception {

        Movie movie = new Movie(1, "bruh", 1984);
        MoviesStore moviesStore = new MoviesStore();
        moviesStore.addMovie(movie);
        MoviesServer server = new MoviesServer(moviesStore);

        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        server.stop();
        System.out.println("=== ОТВЕТ ОТ СЕРВЕРА ===");
        System.out.println(resp.body());
        System.out.println("========================");
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.equals("[{\"id\":1,\"title\":\"bruh\",\"year\":1984}]"),
                "Ожидается JSON-массив");
    }

    @Test
    void postMovies_addsMovie_returnsCreated() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        String jsonBody = """
                {
                    "title": "The Dark Knight",
                    "year": 2008
                }
                """;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть статус 201 Created");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Ответ должен иметь заголовок Content-Type: application/json");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{"), "Тело ответа должно быть JSON-объектом фильма");

        List<Movie> allMovies = moviesStore.getAllMovies();

        assertEquals(1, allMovies.size(), "В хранилище должен быть ровно 1 фильм после POST-запроса");

        Movie savedMovie = allMovies.get(0);
        assertEquals("The Dark Knight", savedMovie.getTitle(), "Название должно совпадать с отправленным");
        assertEquals(2008, savedMovie.getYear(), "Год должен совпадать с отправленным");

        assertNotNull(savedMovie.getId(), "У фильма должен быть ID");
        assertTrue(savedMovie.getId() > 0, "ID должен быть положительным числом");
    }

    @Test
    void postMovies_emptyTitle_returnsBadRequest() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        String jsonBody = """
                {
                    "title": "",
                    "year": 2023
                }
                """;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(400, resp.statusCode(), "При пустом title сервер должен вернуть 400 Bad Request");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "В теле ответа должен быть ключ 'error'");
        assertTrue(body.contains("Название фильма обязательно"), "Сообщение об ошибке должно быть понятным");

        assertEquals(0, moviesStore.getAllMovies().size(),
                "Фильм с пустым названием не должен быть добавлен в хранилище");
    }

    @Test
    void postMovies_longTitle_returnsBadRequest() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        String longTitle = "A".repeat(101);

        String jsonBody = String.format("""
                {
                    "title": "%s",
                    "year": 2023
                }
                """, longTitle);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(400, resp.statusCode(), "При title > 100 символов сервер должен вернуть 400 Bad Request");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "В ответе должен быть JSON с ошибкой");
        assertTrue(body.contains("слишком длинное") || body.contains("превышает"),
                "Сообщение об ошибке должно говорить о длине названия");

        assertEquals(0, moviesStore.getAllMovies().size(),
                "Фильм с слишком длинным названием не должен быть добавлен в хранилище");
    }

    @Test
    void postMovies_invalidYear_returnsBadRequest() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        String jsonOld = """
                {
                    "title": "Old Movie",
                    "year": 1800
                }
                """;

        HttpRequest reqOld = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonOld))
                .build();

        HttpResponse<String> respOld = client.send(reqOld, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, respOld.statusCode(), "Год меньше 1888 должен вернуть 400");
        assertTrue(respOld.body().toLowerCase().contains("некорректный год"),
                "Ошибка должна упоминать год");
        assertEquals(0, moviesStore.getAllMovies().size(), "Фильм не должен сохраниться");

        int futureYear = java.time.Year.now().getValue() + 2;
        String jsonFuture = String.format("""
                {
                    "title": "Future Movie",
                    "year": %d
                }
                """, futureYear);

        HttpRequest reqFuture = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonFuture))
                .build();

        HttpResponse<String> respFuture = client.send(reqFuture, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(400, respFuture.statusCode(), "Год из будущего должен вернуть 400");
        assertTrue(respFuture.body().toLowerCase().contains("некорректный год"),
                "Ошибка для будущего года тоже должна упоминать год");

        assertEquals(0, moviesStore.getAllMovies().size(), "Фильмы с неверными годами не должны сохраняться");

        server.stop();
    }

    @Test
    void postMovies_wrongContentType_returnsUnsupportedMediaType() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();


        String jsonBody = """
                {
                    "title": "Good Movie",
                    "year": 2020
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(415, resp.statusCode(),
                "При отсутствии или неверном Content-Type сервер должен вернуть 415 Unsupported Media Type");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "В теле ответа должен быть ключ 'error'");

        assertEquals(0, moviesStore.getAllMovies().size(),
                "Фильм не должен сохраниться при неверном типе контента");
    }

    @Test
    void postMovies_invalidJson_returnsBadRequest() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        String badJson = "это не json";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8") // Заголовок правильный!
                .POST(HttpRequest.BodyPublishers.ofString(badJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(400, resp.statusCode(),
                "При некорректном JSON сервер должен вернуть 400 Bad Request");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "В теле ответа должен быть ключ 'error'");

        assertEquals(0, moviesStore.getAllMovies().size(),
                "Фильм не должен сохраниться при битом JSON");
    }

    @Test
    void getMovieById_returnsMovie() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        Movie originalMovie = new Movie(0, "Inception", 2010); // ID пока 0, он изменится при добавлении
        moviesStore.addMovie(originalMovie);

        int id = originalMovie.getId();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(200, resp.statusCode(), "При успешном поиске должен вернуться статус 200");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{"), "Тело ответа должно быть JSON-объектом фильма");
        assertTrue(body.contains("\"title\":\"Inception\""), "В ответе должно быть название фильма");
        assertTrue(body.contains("\"year\":2010"), "В ответе должен быть год фильма");

    }

    @Test
    void getMovieById_notFound_returns404() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        int nonExistentId = 9999;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + nonExistentId))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(404, resp.statusCode(),
                "При запросе несуществующего фильма сервер должен вернуть 404");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "Тело ответа должно содержать ключ 'error'");

        assertTrue(body.contains("не найден") || body.contains("not found"),
                "Сообщение об ошибке должно говорить о том, что ресурс не найден");
    }

    @Test
    void getMovieById_invalidIdFormat_returnsBadRequest() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        moviesStore.addMovie(new Movie(1, "Test Movie", 2000));

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        String invalidId = "abc";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + invalidId))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(400, resp.statusCode(),
                "При нечисловом ID сервер должен вернуть 400 Bad Request");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "Тело ответа должно содержать ключ 'error'");

        assertTrue(body.contains("некорректный формат") || body.contains("число"),
                "Сообщение об ошибке должно объяснять проблему с форматом ID");
    }

    @Test
    void deleteMovieById_removesMovie_returnsNoContent() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        Movie movieToDelete = new Movie(0, "The Matrix", 1999);
        moviesStore.addMovie(movieToDelete);

        int id = movieToDelete.getId();
        if (id == 0) {
            movieToDelete.setId(1);
            id = 1;
        }

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(204, resp.statusCode(),
                "При успешном удалении сервер должен вернуть 204 No Content");

        assertTrue(resp.body().isBlank() || resp.body().isEmpty(),
                "Тело ответа при статусе 204 должно быть пустым");

        assertEquals(0, moviesStore.getAllMovies().size(),
                "После удаления в хранилище не должно остаться фильмов");
    }

    @Test
    void deleteMovieById_notFound_returns404() throws Exception {

        MoviesStore moviesStore = new MoviesStore();

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        int nonExistentId = 999;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + nonExistentId))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(404, resp.statusCode(),
                "При попытке удалить несуществующий фильм сервер должен вернуть 404");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "Тело ответа должно содержать ключ 'error'");

        assertTrue(body.contains("не найден"),
                "Сообщение об ошибке должно говорить, что фильм не найден");

        assertEquals(0, moviesStore.getAllMovies().size(),
                "Удаление несуществующего фильма не должно менять состояние хранилища");
    }

    @Test
    void deleteMovieById_invalidIdFormat_returnsBadRequest() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        moviesStore.addMovie(new Movie(1, "Test Movie", 2000));

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        String invalidId = "abc";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + invalidId))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(400, resp.statusCode(),
                "При нечисловом ID сервер должен вернуть 400 Bad Request");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "Тело ответа должно содержать ключ 'error'");

        assertTrue(body.contains("некорректный формат") || body.contains("число"),
                "Сообщение об ошибке должно объяснять проблему с форматом ID");

        assertEquals(1, moviesStore.getAllMovies().size(),
                "Фильм не должен быть удалён, так как ID был невалидным");
    }

    @Test
    void getMovies_filteredByYear_returnsCorrectList() throws Exception {

        MoviesStore moviesStore = new MoviesStore();

        moviesStore.addMovie(new Movie(1, "Old Movie", 1984));
        moviesStore.addMovie(new Movie(2, "Classic Movie", 1999));
        moviesStore.addMovie(new Movie(3, "Modern Movie", 1999)); // Тот же год, что и выше
        moviesStore.addMovie(new Movie(4, "New Movie", 2020));

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(200, resp.statusCode(), "Запрос должен вернуть 200 OK");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ответ должен быть JSON-массивом");

        assertTrue(body.contains("\"Classic Movie\""));
        assertTrue(body.contains("\"Modern Movie\""));
        assertFalse(body.contains("\"Old Movie\""));
        assertFalse(body.contains("\"New Movie\""));
    }

    @Test
    void getMovies_filteredByYear_returnsEmptyList_ifNotFound() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        moviesStore.addMovie(new Movie(1, "Modern Movie", 2020));
        moviesStore.addMovie(new Movie(2, "Old Movie", 1984));

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=1900"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(200, resp.statusCode(),
                "Даже если фильмов нет, сервер должен вернуть 200 OK, а не 404");

        String body = resp.body().trim();
        assertEquals("[]", body, "При отсутствии фильмов должен вернуться пустой JSON-массив []");

        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ответ должен быть валидным JSON-массивом");
    }

    @Test
    void getMovies_filteredByYear_returnsBadRequest_ifInvalidYear() throws Exception {

        MoviesStore moviesStore = new MoviesStore();
        moviesStore.addMovie(new Movie(1, "Test Movie", 2000));

        MoviesServer server = new MoviesServer(moviesStore);
        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        server.stop();

        assertEquals(400, resp.statusCode(),
                "При нечисловом параметре year сервер должен вернуть 400 Bad Request");

        String body = resp.body().trim();
        assertTrue(body.contains("\"error\""), "Тело ответа должно содержать ключ 'error'");

        assertTrue(body.toLowerCase().contains("некорректный") ||
                        body.toLowerCase().contains("invalid") ||
                        body.toLowerCase().contains("число"),
                "Сообщение об ошибке должно объяснять проблему с форматом года");
    }

}