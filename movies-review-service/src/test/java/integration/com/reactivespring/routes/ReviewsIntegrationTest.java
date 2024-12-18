package com.reactivespring.routes;

import com.reactivespring.domain.Review;
import com.reactivespring.repository.ReviewReactiveRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class ReviewsIntegrationTest {

    public static final String REVIEWS_PATH = "/v1/reviews";
    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ReviewReactiveRepository reviewReactiveRepository;

    @BeforeEach
    void setUp() {

        var reviewsList = List.of(
                new Review(null, 1L, "Awesome movie", 9.0),
                new Review(null, 1L, "good movie", 8.0),
                new Review("abc", 2L, "nice movie", 9.0));
        reviewReactiveRepository.saveAll(reviewsList).blockLast();
    }

    @AfterEach
    void tearDown() {
        reviewReactiveRepository.deleteAll().block();
    }

    @Test
    void addReview() {
        var review = new Review(null, 3L, "Awesome movie", 8.0);

        webTestClient.post()
                .uri("/v1/reviews")
                .bodyValue(review)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Review.class)
                .consumeWith(result -> {
                    var savedReview = result.getResponseBody();
                    assert savedReview != null;
                    assert savedReview.getReviewId()!=null;
                    assert savedReview.getMovieInfoId() == 3L;
                });
    }

    @Test
    void getReviews() {
        var movieInfoId = 1;
        var uri = UriComponentsBuilder.fromUriString(REVIEWS_PATH)
                .queryParam("movieInfoId",movieInfoId).buildAndExpand().toUri();
        webTestClient.get()
                .uri(uri).exchange().expectStatus().is2xxSuccessful().expectBodyList(Review.class).hasSize(2);
    }

    @Test
    void getReviews1() {
        webTestClient
                .get()
                .uri(REVIEWS_PATH)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Review.class)
                .value(reviews -> {
                    assertEquals(3, reviews.size());
                });

    }

    @Test
    void updateReview() {
        final var review = new Review("abc", 2L, "nice movie review", 9.0);

        webTestClient.put().uri(REVIEWS_PATH+"/{id}","abc")
                .bodyValue(review)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Review.class)
                .consumeWith(result -> {
                    var review1 = result.getResponseBody();
                    assert review1 != null;
                    assertEquals("nice movie review",review1.getComment());
                });
    }

    @Test
    void deleteReview() {
        webTestClient.delete().uri(REVIEWS_PATH+"/{id}","abc")
                .exchange()
                .expectStatus()
                .isNoContent();
    }
}
