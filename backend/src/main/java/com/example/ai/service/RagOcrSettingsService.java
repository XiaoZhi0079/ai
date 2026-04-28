package com.example.ai.service;

import com.example.ai.pojo.RagOcrRequestConfig;

public interface RagOcrSettingsService {

    RagOcrRequestConfig getUserSettings(Integer userId);

    RagOcrRequestConfig saveUserSettings(Integer userId, RagOcrRequestConfig requestConfig);
}
