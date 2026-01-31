package com.laptophub.backend.service;


import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("El email " + user.getEmail() + " ya está registrado");
        }
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }
    
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
    }
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Transactional
    @SuppressWarnings("null")
    public User updateUser(Long id, User updatedUser) {
        User existingUser = findById(id);
        if (updatedUser.getNombre() != null) existingUser.setNombre(updatedUser.getNombre());
        if (updatedUser.getApellido() != null) existingUser.setApellido(updatedUser.getApellido());
        if (updatedUser.getTelefono() != null) existingUser.setTelefono(updatedUser.getTelefono());
        if (updatedUser.getDireccion() != null) existingUser.setDireccion(updatedUser.getDireccion());
        return userRepository.save(existingUser);
    }
}
