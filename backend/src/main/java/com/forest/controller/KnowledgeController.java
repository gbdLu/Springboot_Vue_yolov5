package com.forest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forest.dto.PageParam;
import com.forest.dto.Result;
import com.forest.entity.Knowledge;
import com.forest.entity.User;
import com.forest.service.KnowledgeService;
import com.forest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(PageParam pageParam,
                                             @RequestParam(required = false) String category) {
        Page<Knowledge> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        if (pageParam.getKeyword() != null && !pageParam.getKeyword().isEmpty()) {
            wrapper.like(Knowledge::getTitle, pageParam.getKeyword());
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Knowledge::getCategory, category);
        }
        wrapper.eq(Knowledge::getStatus, 1);
        wrapper.orderByDesc(Knowledge::getCreatedAt);
        Page<Knowledge> result = knowledgeService.page(page, wrapper);

        result.getRecords().forEach(k -> {
            if (k.getCreatedBy() != null) {
                User user = userService.getById(k.getCreatedBy());
                if (user != null) k.setCreatorName(user.getRealName());
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/detail/{id}")
    public Result<Knowledge> getDetail(@PathVariable Integer id) {
        Knowledge knowledge = knowledgeService.getById(id);
        if (knowledge == null) return Result.error("文章不存在");
        // 增加浏览量
        knowledge.incrementViewCount();
        knowledgeService.updateById(knowledge);
        return Result.success(knowledge);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Knowledge knowledge,
                             @RequestAttribute("userId") Integer userId,
                             @RequestAttribute("roleCode") String roleCode) {
        if (!"ADMIN".equals(roleCode)) {
            return Result.error("仅管理员可发布知识库文章");
        }
        knowledge.setCreatedBy(userId);
        knowledge.setViewCount(0);
        knowledge.setStatus(1);
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeService.save(knowledge);
        return Result.success("发布成功", null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Knowledge knowledge,
                                @RequestAttribute("roleCode") String roleCode) {
        if (!"ADMIN".equals(roleCode)) {
            return Result.error("仅管理员可编辑知识库文章");
        }
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeService.updateById(knowledge);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id,
                                @RequestAttribute("roleCode") String roleCode) {
        if (!"ADMIN".equals(roleCode)) {
            return Result.error("仅管理员可删除知识库文章");
        }
        knowledgeService.removeById(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        List<Knowledge> all = knowledgeService.list();
        List<String> categories = all.stream()
                .map(Knowledge::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return Result.success(categories);
    }
}
