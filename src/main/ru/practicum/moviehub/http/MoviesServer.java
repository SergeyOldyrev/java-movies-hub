package ru.practicum.moviehub.http;


import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.http.MoviesHandler;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store) {
        this(store, 8080);
    }

    public MoviesServer(int port) {
        this(new MoviesStore(), port);
    }

    private MoviesServer(MoviesStore store, int port) {
        this.store = store;

        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/movies", new MoviesHandler(store));

            System.out.println("Сервер создан на порту " + port);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getStore() {
        return store;
    }
}