package com.laptophub.backend.controller;

import com.laptophub.backend.dto.CreateReviewDTO;
import com.laptophub.backend.dto.ReviewResponseDTO;
import com.laptophub.backend.dto.UpdateReviewDTO;
import com.laptophub.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ReviewResponseDTO create(@Valid @RequestBody CreateReviewDTO dto, @RequestParam UUID userId) {
        return reviewService.createReview(dto, userId);
    }

    @GetMapping("/product/{productId}")
    public Page<ReviewResponseDTO> getByProduct(@PathVariable Long productId, @NonNull Pageable pageable) {
        return reviewService.getReviewsByProduct(productId, pageable);
    }

    @GetMapping("/product/{productId}/user/{userId}")
    public ReviewResponseDTO getUserReview(
            @PathVariable Long productId,
            @PathVariable UUID userId
    ) {
        return reviewService.getUserReviewForProduct(productId, userId);
    }

    @PutMapping("/{reviewId}")
    public ReviewResponseDTO update(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewDTO dto
    ) {
        return reviewService.updateReview(reviewId, dto);
    }

    @DeleteMapping("/{reviewId}")
    public void delete(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
    }

    @GetMapping("/product/{productId}/average")
    public Double average(@PathVariable Long productId) {
        return reviewService.getAverageRating(productId);
    }
}