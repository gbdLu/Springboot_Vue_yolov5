package com.forest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.forest.entity.DetectionRecord;
import com.forest.mapper.DetectionRecordMapper;
import com.forest.service.DetectionService;
import org.springframework.stereotype.Service;

@Service
public class DetectionServiceImpl extends ServiceImpl<DetectionRecordMapper, DetectionRecord>
        implements DetectionService {
}