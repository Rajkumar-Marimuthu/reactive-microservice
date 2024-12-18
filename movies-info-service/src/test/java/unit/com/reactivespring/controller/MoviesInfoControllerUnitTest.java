package com.reactivespring.controller;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MovieInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = MoviesInfoController.class)
@AutoConfigureWebTestClient
class MoviesInfoControllerUnitTest {

    public static final String MOVIEINFOS_URI = "/v1/movieinfos";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MovieInfoService movieInfoService;

    @Test
    void getAllMovies() {

        var movieInfos = List.of(new MovieInfo(null,"Batman Begins", 2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20")));

        when(movieInfoService.getAllMovies()).thenReturn(Flux.fromIterable(movieInfos));

        webTestClient.get()
                .uri(MOVIEINFOS_URI)
                .exchange().expectStatus().is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(3);
    }

    @Test
    void getMovieInfoById() {

        var movieInfos = new MovieInfo(null,"Batman Begins", 2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));
        when(movieInfoService.getMovieById(anyString())).thenReturn(Mono.just(movieInfos));

        webTestClient.get()
                .uri(MOVIEINFOS_URI+"/id/abc")
                .exchange().expectStatus().is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(1);

    }

    @Test
    void addMovieInfo() {
        final var movieInfo = new MovieInfo("mockid", "The Dark Knight", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18"));

        when(movieInfoService.addMovieInfo(isA(MovieInfo.class))).thenReturn(Mono.just(movieInfo));
        webTestClient.post().uri(MOVIEINFOS_URI)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(MovieInfo.class)
                .consumeWith(mi -> {
                    var m = mi.getResponseBody();
                    assert m != null;
                    assert m.getMovieInfoId() != null;
                });
    }

    @Test
    void addMovieInfoValidation() {
        final var movieInfo = new MovieInfo("mockid", "", -2008, List.of("Christian Bale", ""), LocalDate.parse("2008-07-18"));

        webTestClient.post().uri(MOVIEINFOS_URI)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .consumeWith(mi -> {
                    var m = mi.getResponseBody();
                    System.out.println(m);
                    assert m != null;
                    assertEquals("movieInfo.cast must be present, movieInfo.name must be present, movieInfo.year must be a positive value", m);
                });
    }


    @Test
    void updateMovieInfo() {
        final var movieInfo = new MovieInfo("abc", "The Dark Knight Rises", 2018, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18"));

        when(movieInfoService.updateMovieinfo(isA(MovieInfo.class),anyString())).thenReturn(Mono.just(movieInfo));
        webTestClient.put().uri(MOVIEINFOS_URI+"/{id}","abc")
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(MovieInfo.class)
                .consumeWith(mi -> {
                    var m = mi.getResponseBody();
                    assert m != null;
                    assertEquals("The Dark Knight Rises", m.getName());
                    assertEquals(2018,m.getYear());
                });
    }

    @Test
    void deleteMovieInfo() {

        when(movieInfoService.deleteMovieinfo(anyString())).thenReturn(Mono.empty());
        webTestClient.delete().uri(MOVIEINFOS_URI+"/{id}","abc")
                .exchange()
                .expectStatus()
                .isNoContent();
    }
}