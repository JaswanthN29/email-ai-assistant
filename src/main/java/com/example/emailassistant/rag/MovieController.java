package com.example.emailassistant.rag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping({"/movie", "/api/movie"})
    public String getMovieWelcome() {
        return movieService.getWelcomeMessage();
    }
}
