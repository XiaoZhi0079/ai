package com.example.ai.service.impl;

import com.example.ai.entity.OperationLog;
import com.example.ai.mapper.OperationLogMapper;
import com.example.ai.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void log(String operator, String action) {
        OperationLog log = new OperationLog();
        log.setOperator(operator == null || operator.isBlank() ? "unknown" : operator);
        log.setAction(action == null ? "" : action);
        operationLogMapper.insert(log);
    }
}
