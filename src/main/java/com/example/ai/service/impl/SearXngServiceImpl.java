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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // 添加Slf4j注解
public class SearXngServiceImpl implements SearXngService {

    @Value("${internet.websearch.searxng.url}")
    private String SEARXNG_URL;

    @Value("${internet.websearch.searxng.counts}")
    private Integer SEARXNG_COUNTS;

    private final OkHttpClient okHttpClient;


    public List<SearchResult> search(String query) {
        HttpUrl url = HttpUrl.get(SEARXNG_URL)
                .newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .build();

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
        // 注意：原有的 subList 和 parallelStream 结合可能有问题，如果 results 数量小于 SEARXNG_COUNTS
        // 先 limit 再 sorted 更安全高效
        return results.stream()
                .limit(SEARXNG_COUNTS)
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();
    }
}

