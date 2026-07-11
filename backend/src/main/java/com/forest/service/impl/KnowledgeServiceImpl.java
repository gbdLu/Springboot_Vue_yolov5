package com.forest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forest.entity.Knowledge;
import com.forest.mapper.KnowledgeMapper;
import com.forest.service.KnowledgeService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge>
        implements KnowledgeService {
}
