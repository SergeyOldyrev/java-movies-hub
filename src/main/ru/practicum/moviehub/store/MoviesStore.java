package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {

    private final List<Movie> movies;

    public MoviesStore() {
        this.movies = new ArrayList<>();
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies);
    }

    public Optional<Movie> getMovieById(int id) {
        return movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public void addMovie(Movie movie) {
        movies.add(movie);
    }

    public boolean deleteMovie(int id) {
        return movies.removeIf(movie -> movie.getId() == id);
    }

    public int getNextId() {
        if (movies.isEmpty()) {
            return 1;
        }
        return movies.stream()
                .mapToInt(Movie::getId)
                .max()
                .getAsInt() + 1;
    }

    public boolean removeMovieById(int id) {
        return movies.removeIf(movie -> movie.getId() == id);
    }
}