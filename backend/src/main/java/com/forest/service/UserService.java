package com.forest.service;

import com.forest.entity.User;

public interface UserService {
    User getByUsername(String username);
    void updateLoginInfo(Integer userId, String ip);
}