package com.laptophub.backend.service;


import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.Review;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final ProductService productService;
    
    private void validateRating(Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("El rating debe estar entre 1 y 5");
        }
    }
    
    @Transactional
    @SuppressWarnings("null")
    public Review createReview(Long productId, Long userId, Integer rating, String comentario) {
        validateRating(rating);
        
        User user = userService.findById(userId);
        Product product = productService.findById(productId);
        
        Optional<Review> existingReview = reviewRepository.findByProductAndUser(product, user);
        if (existingReview.isPresent()) {
            throw new RuntimeException("Ya has dejado una reseña para este producto");
        }
        
        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(rating)
                .comentario(comentario)
                .build();
        
        return reviewRepository.save(review);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getReviewsByProduct(Long productId) {
        Product product = productService.findById(productId);
        return reviewRepository.findByProduct(product);
    }
    
    @Transactional(readOnly = true)
    public Review getUserReviewForProduct(Long productId, Long userId) {
        User user = userService.findById(userId);
        Product product = productService.findById(productId);
        
        return reviewRepository.findByProductAndUser(product, user)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró una reseña del usuario para este producto"));
    }
    
    @Transactional
    @SuppressWarnings("null")
    public Review updateReview(Long reviewId, Integer newRating, String newComentario) {
        validateRating(newRating);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review no encontrada con id: " + reviewId));
        
        review.setRating(newRating);
        review.setComentario(newComentario);
        
        return reviewRepository.save(review);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Review no encontrada con id: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
    
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        List<Review> reviews = getReviewsByProduct(productId);
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
