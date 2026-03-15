package com.example.ai.service.impl;

import com.example.ai.entity.User;
import com.example.ai.mapper.UserMapper;
import com.example.ai.service.OperationLogService;
import com.example.ai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    @Override
    public User create(User user, String operator) {
        user.setId(null);
        userMapper.insert(user);
        User saved = userMapper.selectById(user.getId());
        operationLogService.log(operator, "创建用户 id=" + saved.getId());
        return saved;
    }

    @Override
    public List<User> list() {
        return userMapper.selectAll();
    }

    @Override
    public Optional<User> get(Integer id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }

    @Override
    public Optional<User> update(Integer id, User user, String operator) {
        return Optional.ofNullable(userMapper.selectById(id)).map(existing -> {
            existing.setUsername(user.getUsername());
            existing.setPassword(user.getPassword());
            existing.setRole(user.getRole());
            existing.setEmail(user.getEmail());
            existing.setStatus(user.getStatus());
            userMapper.update(existing);
            User saved = userMapper.selectById(existing.getId());
            operationLogService.log(operator, "更新用户 id=" + saved.getId());
            return saved;
        });
    }

    @Override
    public boolean delete(Integer id, String operator) {
        if (userMapper.countById(id) == 0) {
            return false;
        }
        userMapper.deleteById(id);
        operationLogService.log(operator, "删除用户 id=" + id);
        return true;
    }
}
