package com.laptophub.backend.service;


import com.laptophub.backend.dto.CreateReviewDTO;
import com.laptophub.backend.dto.DTOMapper;
import com.laptophub.backend.dto.ReviewResponseDTO;
import com.laptophub.backend.dto.UpdateReviewDTO;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.Review;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.ReviewRepository;
import com.laptophub.backend.exception.ConflictException;
import com.laptophub.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final ProductService productService;
    
    @Transactional
    @SuppressWarnings("null")
    public ReviewResponseDTO createReview(CreateReviewDTO dto, UUID userId) {
        User user = userService.findById(userId);
        Product product = productService.findById(dto.getProductId());
        
        Optional<Review> existingReview = reviewRepository.findByProductAndUser(product, user);
        if (existingReview.isPresent()) {
            throw new ConflictException("Ya has dejado una reseña para este producto");
        }
        
        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(dto.getRating())
                .comentario(dto.getComentario())
                .build();
        
        Review saved = reviewRepository.save(review);
        return DTOMapper.toReviewResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviewsByProduct(Long productId, @NonNull Pageable pageable) {
        Product product = productService.findById(productId);
        return reviewRepository.findByProduct(product, pageable).map(DTOMapper::toReviewResponse);
    }
    
    @Transactional(readOnly = true)
    public ReviewResponseDTO getUserReviewForProduct(Long productId, UUID userId) {
        User user = userService.findById(userId);
        Product product = productService.findById(productId);
        
        Review review = reviewRepository.findByProductAndUser(product, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una reseña del usuario para este producto"));
        return DTOMapper.toReviewResponse(review);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public ReviewResponseDTO updateReview(Long reviewId, UpdateReviewDTO dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con id: " + reviewId));
        
        review.setRating(dto.getRating());
        review.setComentario(dto.getComentario());
        
        Review saved = reviewRepository.save(review);
        return DTOMapper.toReviewResponse(saved);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review no encontrada con id: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
    
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        Product product = productService.findById(productId);
        List<Review> reviews = reviewRepository.findByProduct(product);
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
