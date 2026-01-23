package com.laptophub.backend.repository;

import com.laptophub.backend.model.Order;
import com.laptophub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByEstado(String estado);
}
