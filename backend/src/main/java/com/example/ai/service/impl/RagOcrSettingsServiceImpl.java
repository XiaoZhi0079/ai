package com.example.ai.service.impl;

import com.example.ai.mapper.RagOcrSettingsMapper;
import com.example.ai.pojo.RagOcrRequestConfig;
import com.example.ai.pojo.RagOcrUserSettings;
import com.example.ai.service.RagOcrSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RagOcrSettingsServiceImpl implements RagOcrSettingsService {

    private final RagOcrSettingsMapper ragOcrSettingsMapper;

    @Override
    public RagOcrRequestConfig getUserSettings(Integer userId) {
        if (userId == null) {
            return emptyConfig();
        }
        RagOcrUserSettings settings = ragOcrSettingsMapper.selectByUserId(userId);
        if (settings == null) {
            return emptyConfig();
        }
        return toConfig(settings);
    }

    @Override
    public RagOcrRequestConfig saveUserSettings(Integer userId, RagOcrRequestConfig requestConfig) {
        RagOcrUserSettings settings = new RagOcrUserSettings();
        settings.setUserId(userId);
        settings.setBaseUrl(normalize(requestConfig.getBaseUrl()));
        settings.setApiKey(normalize(requestConfig.getApiKey()));
        settings.setModel(normalize(requestConfig.getModel()));

        RagOcrUserSettings existing = ragOcrSettingsMapper.selectByUserId(userId);
        if (existing == null) {
            ragOcrSettingsMapper.insert(settings);
        } else {
            ragOcrSettingsMapper.updateByUserId(settings);
        }
        return toConfig(settings);
    }

    private RagOcrRequestConfig toConfig(RagOcrUserSettings settings) {
        RagOcrRequestConfig config = new RagOcrRequestConfig();
        config.setBaseUrl(settings.getBaseUrl());
        config.setApiKey(settings.getApiKey());
        config.setModel(settings.getModel());
        return config;
    }

    private RagOcrRequestConfig emptyConfig() {
        return new RagOcrRequestConfig();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
