package com.forest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.forest.entity.User;

public interface UserService extends IService<User> {
    User getByUsername(String username);
    void updateLoginInfo(Integer userId, String ip);
}
