package com.reactivespring.controller;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MovieInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class MoviesInfoController {

    public static final String MOVIEINFOS_URI = "/movieinfos";
    private final MovieInfoService movieInfoService;
    Sinks.Many<MovieInfo> moviesInfoSink = Sinks.many().replay().all();


    @PostMapping(MOVIEINFOS_URI)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MovieInfo> addMovieInfo(@RequestBody @Valid MovieInfo movieInfo) {
        return movieInfoService.addMovieInfo(movieInfo)
                .doOnNext(savedMovieInfo -> moviesInfoSink.tryEmitNext(savedMovieInfo));
    }

    @GetMapping(MOVIEINFOS_URI)
    @ResponseStatus(HttpStatus.OK)
    public Flux<MovieInfo> getAllMovies(@RequestParam(value="year",required = false) Integer year) {
        if (year != null) {
            return movieInfoService.getMovieByYear(year);
        }
        return movieInfoService.getAllMovies();
    }

    @GetMapping("/movieinfos/id/{id}")
    public Mono<ResponseEntity<MovieInfo>> getMovieInfoById(@PathVariable("id") String id) {
        return movieInfoService.getMovieById(id)
                .map(ResponseEntity.ok()::body)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping(value = "/movieinfos/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<MovieInfo> getMovieInfoFlux() {
        return moviesInfoSink.asFlux();
    }

    @GetMapping("/movieinfos/name/{name}")
    public Mono<ResponseEntity<MovieInfo>> getMovieInfoByName(@PathVariable("name") String name) {
        return movieInfoService.getMovieByName(name)
                .map(ResponseEntity.ok()::body)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("/movieinfos/{id}")
    public Mono<ResponseEntity<MovieInfo>> updateMovieInfo(@RequestBody MovieInfo movieInfo, @PathVariable("id") String id) {
        return movieInfoService.updateMovieinfo(movieInfo, id)
                .map(ResponseEntity.ok()::body)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/movieinfos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMovieInfo(@PathVariable("id") String id) {
        return movieInfoService.deleteMovieinfo(id);
    }
}
