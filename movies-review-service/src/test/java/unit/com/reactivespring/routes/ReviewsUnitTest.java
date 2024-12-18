package com.reactivespring.routes;

import com.reactivespring.domain.Review;
import com.reactivespring.exceptionhandler.GlobalErrorHandler;
import com.reactivespring.handler.ReviewHandler;
import com.reactivespring.repository.ReviewReactiveRepository;
import com.reactivespring.router.ReviewRouter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@WebFluxTest
@ContextConfiguration(classes={ReviewRouter.class, ReviewHandler.class, GlobalErrorHandler.class})
@AutoConfigureWebTestClient
public class ReviewsUnitTest {

    public static final String REVIEWS_PATH = "/v1/reviews";

    @MockitoBean
    private ReviewReactiveRepository reviewReactiveRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void addReview() {
        var review = new Review(null, 3L, "Awesome movie", 8.0);
        Mockito.when(reviewReactiveRepository.save(isA(Review.class)))
                .thenReturn(Mono.just(new Review("abc", 3L, "Awesome movie", 8.0)));

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
    void addReviewValidation() {
        var review = new Review(null, null, "Awesome movie", -2.0);

        webTestClient.post()
                .uri("/v1/reviews")
                .bodyValue(review)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo("rating.movieInfoId : must not be null,rating.negative : rating is negative and please pass a non-negative value");
    }

    @Test
    void getReviews() {
        var movieInfoId = 1;
        var uri = UriComponentsBuilder.fromUriString(REVIEWS_PATH)
                .queryParam("movieInfoId",movieInfoId).buildAndExpand().toUri();
        Mockito.when(reviewReactiveRepository.findReviewsByMovieInfoId(1L))
                .thenReturn(Flux.fromIterable(List.of(new Review("abc", 1L, "Awesome movie", 8.0),
                        new Review("abd", 1L, "Awesome ", 8.0))));
        webTestClient.get()
                .uri(uri).exchange().expectStatus().is2xxSuccessful().expectBodyList(Review.class).hasSize(2);
    }

    @Test
    void getReviews1() {

        Mockito.when(reviewReactiveRepository.findAll())
                .thenReturn(Flux.fromIterable(List.of(new Review("abc", 1L, "Awesome movie", 8.0),
                        new Review("abd", 1L, "Awesome ", 8.0),
                        new Review("acd", 2L, "Great ", 8.0))));

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
        final var review2 = new Review("abc", 2L, "nice movie review2", 9.0);
        Mockito.when(reviewReactiveRepository.findById(anyString())).thenReturn(Mono.just(review));
        Mockito.when(reviewReactiveRepository.save(isA(Review.class))).thenReturn(Mono.just(review2));
        webTestClient.put().uri(REVIEWS_PATH+"/{id}","abc")
                .bodyValue(review)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Review.class)
                .consumeWith(result -> {
                    var review1 = result.getResponseBody();
                    assert review1 != null;
                    assertEquals("nice movie review2",review1.getComment());
                });
    }

    @Test
    void deleteReview() {
        Mockito.when(reviewReactiveRepository.findById(anyString())).thenReturn(Mono.just( new Review("abc", 2L, "nice movie review", 9.0)));
        Mockito.when(reviewReactiveRepository.deleteById(anyString())).thenReturn(Mono.empty());
        webTestClient.delete().uri(REVIEWS_PATH+"/{id}","abc")
                .exchange()
                .expectStatus()
                .isNoContent();
    }

}
