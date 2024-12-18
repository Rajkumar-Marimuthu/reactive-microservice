package com.reactivespring.controller;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.repository.MovieInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class MoviesInfoControllerIntegrationTest {

    public static final String MOVIEINFOS_URI = "/v1/movieinfos";
    @Autowired
    MovieInfoRepository movieInfoRepository;

    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        var movieInfos = List.of(new MovieInfo(null,"Batman Begins", 2005, List.of("Christian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "TheDarkKnight", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20")));

        movieInfoRepository.saveAll(movieInfos).blockLast();
    }

    @AfterEach
    void tearDown() {
        movieInfoRepository.deleteAll().block();
    }

    @Test
    void addMovieInfo() {
        final var movieInfo = new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18"));

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
    void getAllMovies() {
        webTestClient.get()
                .uri(MOVIEINFOS_URI)
                .exchange().expectStatus().is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(3);
    }

    @Test
    void getAllMovies_stream() {

        final var movieInfo = new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18"));

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

        var moviesStreamFlux = webTestClient.get()
                .uri(MOVIEINFOS_URI+"/stream")
                .exchange().expectStatus().is2xxSuccessful()
                .returnResult(MovieInfo.class)
                .getResponseBody();

        StepVerifier.create(moviesStreamFlux)
                .assertNext(mi -> {
                    assert mi.getMovieInfoId()!=null;
                })
                .thenCancel()
                .verify();
    }

    @Test
    void getMoviesInfoByYear() {
        var uri = UriComponentsBuilder.fromUriString(MOVIEINFOS_URI)
                        .queryParam("year",2012).buildAndExpand().toUri();

        webTestClient.get()
                .uri(uri)
                .exchange().expectStatus().is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(1);
    }

    @Test
    void getMoviesInfoByName() {
        String movieName = "TheDarkKnight";
        String encodedMovieName = UriUtils.encodePathSegment(movieName, StandardCharsets.UTF_8);

        webTestClient.get()
                .uri(MOVIEINFOS_URI+"/name/{encodedMovieName}",encodedMovieName)
                .exchange().expectStatus().is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(1);
    }

    @Test
    void getMovieInfoById() {
        var movieInfoId = "abc";
        webTestClient.get().uri(MOVIEINFOS_URI+"/id/{movieInfoId}",movieInfoId).exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Dark Knight Rises");
/*                .expectBody(MovieInfo.class)
                .consumeWith(s -> {
                    var movieInfo = s.getResponseBody();
                    assertNotNull(movieInfo);
                });*/

    }

    @Test
    void getMovieInfoByIdNotFound() {
        var movieInfoId = "dev";
        webTestClient.get().uri(MOVIEINFOS_URI+"/id/{movieInfoId}",movieInfoId).exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void updateMovieInfo() {
        final var movieInfo = new MovieInfo("abc", "The Dark Knight Rises", 2018, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18"));

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
    void updateMovieInfoNotFound() {
        final var movieInfo = new MovieInfo("abc", "The Dark Knight Rises", 2018, List.of("Christian Bale", "HeathLedger"), LocalDate.parse("2008-07-18"));

        webTestClient.put().uri(MOVIEINFOS_URI+"/{id}","any")
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isNotFound()
                /*.expectBody(MovieInfo.class)
                .consumeWith(mi -> {
                    var m = mi.getResponseBody();
                    assert m != null;
                    assertEquals("The Dark Knight Rises", m.getName());
                    assertEquals(2018,m.getYear());
                })*/;
    }

    @Test
    void deleteMovieInfo() {

        webTestClient.delete().uri(MOVIEINFOS_URI+"/{id}","abc")
                .exchange()
                .expectStatus()
                .isNoContent();
    }
}