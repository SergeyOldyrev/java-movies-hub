package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;

public class MovieHubApp {
    public static void main(String[] args) {

        final MoviesServer server = new MoviesServer(8080);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("Запуск сервера...");
        server.start();
    }
}