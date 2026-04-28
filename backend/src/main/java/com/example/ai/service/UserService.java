package com.example.ai.service;

import com.example.ai.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User create(User user, String operator);
    List<User> list();
    Optional<User> get(Integer id);
    Optional<User> update(Integer id, User user, String operator);
    boolean delete(Integer id, String operator);
}
