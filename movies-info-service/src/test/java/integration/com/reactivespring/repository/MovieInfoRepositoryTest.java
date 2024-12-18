package com.reactivespring.repository;

import com.reactivespring.domain.MovieInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ActiveProfiles("test")
class MovieInfoRepositoryTest {

    @Autowired
    MovieInfoRepository movieInfoRepository;

    @BeforeEach
    void setup() {
        var movieInfos = List.of(new MovieInfo(null,"Batman Begins", 2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20")));

        movieInfoRepository.saveAll(movieInfos).blockLast();
    }

    @AfterEach
    void tearDown() {
        movieInfoRepository.deleteAll().block();
    }

    @Test
    void findAll() {
      Flux<MovieInfo> flux = movieInfoRepository.findAll();
        StepVerifier.create(flux).expectNextCount(3).verifyComplete();
    }

    @Test
    void findById() {
        Mono<MovieInfo> mono = movieInfoRepository.findById("abc");
        StepVerifier.create(mono).assertNext(m -> {
            assertEquals("Dark Knight Rises", m.getName());
        }).verifyComplete();
    }

    @Test
    void findByYear() {
        Flux<MovieInfo> flux = movieInfoRepository.findByYear(2012);
        StepVerifier.create(flux).consumeNextWith(m -> {
            assertEquals("Dark Knight Rises", m.getName());
        }).verifyComplete();
    }

    @Test
    void save() {
        Mono<MovieInfo> mono = movieInfoRepository.save(new MovieInfo(null, "The Dark", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18")));
        StepVerifier.create(mono).assertNext(m -> {
            assertEquals("The Dark", m.getName());
        }).verifyComplete();
    }

    @Test
    void update() {
        var movie = movieInfoRepository.findById("abc").block();
        assert movie != null;
        movie.setYear(2021);
        Mono<MovieInfo> mono = movieInfoRepository.save(movie);
        StepVerifier.create(mono).assertNext(m -> {
            assertEquals(2021, m.getYear());
        }).verifyComplete();
    }

    @Test
    void delete() {
        movieInfoRepository.deleteById("abc").block();
        Flux<MovieInfo> flux = movieInfoRepository.findAll();
        StepVerifier.create(flux).expectNextCount(2).verifyComplete();
    }


}