package com.example.ai.service.impl;

import cn.hutool.json.JSONUtil;

import com.example.ai.pojo.SearXNGResponse;
import com.example.ai.pojo.SearchResult;
import com.example.ai.service.SearXngService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // 添加Slf4j注解
public class SearXngServiceImpl implements SearXngService {

    @Value("${internet.websearch.searxng.url}")
    private String SEARXNG_URL;

    @Value("${internet.websearch.searxng.counts}")
    private Integer SEARXNG_COUNTS;

    @Value("${internet.websearch.searxng.safesearch:1}")
    private Integer SEARXNG_SAFESEARCH;

    @Value("${internet.websearch.searxng.max-content-length:300}")
    private Integer SEARXNG_MAX_CONTENT_LENGTH;

    @Value("${internet.websearch.searxng.blocked-domains:}")
    private String SEARXNG_BLOCKED_DOMAINS;

    @Value("${internet.websearch.searxng.blocked-keywords:}")
    private String SEARXNG_BLOCKED_KEYWORDS;

    private final OkHttpClient okHttpClient;


    public List<SearchResult> search(String query) {
        HttpUrl.Builder urlBuilder = HttpUrl.get(SEARXNG_URL)
                .newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json");
        if (SEARXNG_SAFESEARCH != null) {
            urlBuilder.addQueryParameter("safesearch", String.valueOf(SEARXNG_SAFESEARCH));
        }
        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        log.info("正在向 SearXNG 发起请求: {}", url);

        try (Response response = okHttpClient.newCall(request).execute()) {
            // --- 核心修改：提供详细的错误信息 ---
            if (!response.isSuccessful()) {
                String errorBody = "无法获取响应体";
                try (ResponseBody body = response.body()) {
                    if (body != null) {
                        errorBody = body.string();
                    }
                } catch (IOException e) {
                    log.error("读取SearXNG错误响应体失败", e);
                }
                // 抛出包含状态码和响应体的详细异常
                throw new RuntimeException(String.format(
                        "请求 SearXNG 失败。状态码: %d, URL: %s, 响应体: %s",
                        response.code(), url, errorBody
                ));
            }

            ResponseBody body = response.body();
            if (body != null) {
                String responseBody = body.string();
                // 增加一个日志，方便调试返回的JSON内容
                log.debug("SearXNG 响应内容: {}", responseBody);
                SearXNGResponse searXNGResponse = JSONUtil.toBean(responseBody, SearXNGResponse.class);
                if (searXNGResponse != null && searXNGResponse.getResults() != null) {
                    return dealResult(searXNGResponse.getResults());
                } else {
                    log.warn("SearXNG 返回的JSON无法解析或结果为空。响应: {}", responseBody);
                    return Collections.emptyList();
                }
            }

        } catch (IOException e) {
            // 对于网络连接层面的IO异常，也提供更详细的日志
            log.error("请求 SearXNG 发生网络IO异常, URL: {}", url, e);
            throw new RuntimeException("请求 SearXNG 发生网络IO异常", e);
        }

        return Collections.emptyList();
    }

    public List<SearchResult> dealResult(List<SearchResult> results) {
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        List<SearchResult> cleaned = sanitizeResults(results);
        if (cleaned.isEmpty()) {
            return Collections.emptyList();
        }
        // Note: limit + sorted is safer than subList with parallel streams.
        return cleaned.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(SEARXNG_COUNTS)
                .toList();
    }

    private List<SearchResult> sanitizeResults(List<SearchResult> results) {
        Set<String> blockedDomains = parseCsvToSet(SEARXNG_BLOCKED_DOMAINS);
        Set<String> blockedKeywords = parseCsvToSet(SEARXNG_BLOCKED_KEYWORDS);

        List<SearchResult> sanitized = new ArrayList<>(results.size());
        for (SearchResult r : results) {
            String title = cleanText(r.getTitle(), SEARXNG_MAX_CONTENT_LENGTH);
            String content = cleanText(r.getContent(), SEARXNG_MAX_CONTENT_LENGTH);
            String url = r.getUrl();
            SearchResult cleaned = new SearchResult(title, content, url, r.getScore());
            if (!isAllowed(cleaned, blockedDomains, blockedKeywords)) {
                continue;
            }
            if (!StringUtils.hasText(cleaned.getTitle()) && !StringUtils.hasText(cleaned.getContent())) {
                continue;
            }
            sanitized.add(cleaned);
        }
        return sanitized;
    }

    private boolean isAllowed(SearchResult result, Set<String> blockedDomains, Set<String> blockedKeywords) {
        String host = extractHost(result.getUrl());
        if (StringUtils.hasText(host)) {
            String hostLower = host.toLowerCase(Locale.ROOT);
            for (String blocked : blockedDomains) {
                if (blocked.isBlank()) continue;
                String blockedLower = blocked.toLowerCase(Locale.ROOT);
                if (hostLower.equals(blockedLower) || hostLower.endsWith("." + blockedLower)) {
                    return false;
                }
            }
        }

        if (!blockedKeywords.isEmpty()) {
            String combined = (safe(result.getTitle()) + " " + safe(result.getContent()) + " " + safe(result.getUrl()))
                    .toLowerCase(Locale.ROOT);
            for (String kw : blockedKeywords) {
                if (kw.isBlank()) continue;
                if (combined.contains(kw.toLowerCase(Locale.ROOT))) {
                    return false;
                }
            }
        }
        return true;
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (Exception ex) {
            return "";
        }
    }

    private String cleanText(String input, Integer maxLen) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        String cleaned = input.replaceAll("<[^>]*>", " ");
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        int limit = maxLen == null ? 0 : maxLen;
        if (limit > 0 && cleaned.length() > limit) {
            cleaned = cleaned.substring(0, limit) + "...";
        }
        return cleaned;
    }

    private Set<String> parseCsvToSet(String csv) {
        if (!StringUtils.hasText(csv)) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

