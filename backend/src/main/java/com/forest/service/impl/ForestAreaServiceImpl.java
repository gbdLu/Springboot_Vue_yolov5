package com.forest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forest.entity.ForestArea;
import com.forest.mapper.ForestAreaMapper;
import com.forest.service.ForestAreaService;
import org.springframework.stereotype.Service;

@Service
public class ForestAreaServiceImpl extends ServiceImpl<ForestAreaMapper, ForestArea>
        implements ForestAreaService {
}
