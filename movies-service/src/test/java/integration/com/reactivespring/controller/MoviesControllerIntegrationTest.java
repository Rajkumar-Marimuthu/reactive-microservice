package com.reactivespring.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.reactivespring.domain.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port=8084)
@TestPropertySource(
        properties = {
                "restClient.moviesInfoUrl=http://localhost:8084/v1/movieinfos",
                "restClient.reviewsUrl=http://localhost:8084/v1/reviews"
        }
)
public class MoviesControllerIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void retrieveMovieId() {

        stubFor(get(urlEqualTo("/v1/movieinfos/id/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("movieinfo.json")));

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("review.json")));


        webTestClient.get()
                .uri("/v1/movies/{id}","1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Movie.class)
                .consumeWith(
                        movieEntityExchangeResult -> {
                            var movie = movieEntityExchangeResult.getResponseBody();
                            assert Objects.requireNonNull(movie).getReviewList().size()==2;
                            assertEquals("Batman Begins", movie.getMovieInfo().getName());
                        }
                );
    }

    @Test
    void retrieveMovieId_404_NotFound() {

        stubFor(get(urlEqualTo("/v1/movieinfos/id/1"))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("review.json")));


        webTestClient.get()
                .uri("/v1/movies/{id}","1")
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(String.class)
                .isEqualTo("There is no MovieInfo available for the passed in Id : 1");

        WireMock.verify(1, getRequestedFor(urlEqualTo("/v1/movieinfos/id/1")));
    }

    @Test
    void retrieveMovieId_reviews_404_NotFound() {

        stubFor(get(urlEqualTo("/v1/movieinfos/id/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("movieinfo.json")));

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(aResponse()
                        .withStatus(404)));

        webTestClient.get()
                .uri("/v1/movies/{id}","1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Movie.class)
                .consumeWith(
                        movieEntityExchangeResult -> {
                            var movie = movieEntityExchangeResult.getResponseBody();
                            assert Objects.requireNonNull(movie).getReviewList().isEmpty();
                            assertEquals("Batman Begins", movie.getMovieInfo().getName());
                        }
                );
    }

    @Test
    void retrieveMovieId_500_InternalError() {

        stubFor(get(urlEqualTo("/v1/movieinfos/id/1"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("MovieInfo Service unavailable")));

        webTestClient.get()
                .uri("/v1/movies/{id}","1")
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(String.class)
                .isEqualTo("Server Exception in MoviesInfoService MovieInfo Service unavailable");

        WireMock.verify(3, getRequestedFor(urlEqualTo("/v1/movieinfos/id/1")));
    }

    @Test
    void retrieveMovieId_Review_500_InternalError() {

        stubFor(get(urlEqualTo("/v1/movieinfos/id/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("movieinfo.json")));

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Review Service unavailable")));

        webTestClient.get()
                .uri("/v1/movies/{id}","1")
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(String.class)
                .isEqualTo("Server Exception in MovieReviewsService Review Service unavailable");

        WireMock.verify(3, getRequestedFor(urlPathEqualTo("/v1/reviews")));
    }
}
