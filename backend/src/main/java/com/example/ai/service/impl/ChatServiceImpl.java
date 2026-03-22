package com.example.ai.service.impl;

import com.example.ai.Factory.ChatClientFactory;
import com.example.ai.enums.ChatMode;
import com.example.ai.pojo.ChatEntity;
import com.example.ai.pojo.ImagesResponse;
import com.example.ai.pojo.SearchResult;
import com.example.ai.repository.ChatHistoryRepository;
import com.example.ai.service.ChatService;
import com.example.ai.service.DocumentService;
import com.example.ai.service.SearXngService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    // 优化命名：chatClientMap 更直观

    private final ChatClientFactory clientFactory;
    private final ChatHistoryRepository chatHistoryRepository;
    private final DocumentService documentService;
    private final SearXngService searXngService;
    private final ChatMemory chatMemory;

    @Value("${prompt.RAG_PROMPT_TEMPLATE}")
    private String ragPromptTemplateStr;

    @Value("${prompt.INTERNET_SEARCH_PROMPT_TEMPLATE}")
    private String internetSearchPromptTemplateStr;

    @Value("${ai.chat.memory.max-image-messages:3}")
    private Integer maxImageMessages;

    @Override
    public String chat(ChatEntity chatEntity, Long userId) {

        //获取对话ID
        String chatId = chatEntity.getChatId();
        //获取对话模型
        String modelName = chatEntity.getModel();
        String userInput = chatEntity.getUserInput();

        // 取用户第一条消息的前50字符作为 title
        String title = null;
        if (userInput != null && !userInput.isBlank()) {
            title = userInput.length() > 50 ? userInput.substring(0, 50) : userInput;
        }

        // 记录历史（含 userId 和 title，仅首次写入生效）
        chatHistoryRepository.save(chatId, chatEntity.getChatMode().name(), userId, title);

        ChatClient chatClient = clientFactory.getClient(modelName);

        // 1. 根据模式构建消息列表
        List<ImagesResponse> images = (chatEntity.getImageFiles());
        List<Message> baseMessages = buildMessagesByMode(chatEntity, images, userId);
        List<Message> mergedMessages = appendHistory(chatId, baseMessages);
        Prompt promptWithHistory = new Prompt(mergedMessages);

        // 2. 调用 AI
        String reply = chatClient.prompt(promptWithHistory)
                /*.tools(new DateTimeTools())*/ // 如果 DateTimeTools 无状态，建议注册为 Bean 注入，避免每次 new
                .call()
                .content();
        saveToMemory(chatId, userInput, reply, images);
        return reply;
    }

    private List<Message> appendHistory(String chatId, List<Message> baseMessages) {
        if (!StringUtils.hasText(chatId)) {
            return baseMessages;
        }
        List<Message> history = chatMemory.get(chatId);
        if (CollectionUtils.isEmpty(history)) {
            return baseMessages;
        }
        List<Message> merged = new ArrayList<>(history.size() + baseMessages.size());
        merged.addAll(history);
        merged.addAll(baseMessages);
        return limitImageMessages(merged);
    }

    private void saveToMemory(String chatId, String userInput, String reply, List<ImagesResponse> images) {
        if (!StringUtils.hasText(chatId)) {
            return;
        }
        if (StringUtils.hasText(userInput) || (images != null && !images.isEmpty())) {
            chatMemory.add(chatId, buildUserMessage(userInput, images));
        }
        if (StringUtils.hasText(reply)) {
            chatMemory.add(chatId, new AssistantMessage(reply));
        }
    }

    private Message buildUserMessage(String text, List<ImagesResponse> images) {
        String safeText = StringUtils.hasText(text) ? text : "";
        if (images == null || images.isEmpty()) {
            return new UserMessage(safeText);
        }
        List<Media> mediaList = new ArrayList<>();
        for (ImagesResponse image : images) {
            if (!StringUtils.hasText(image.getImageUrl())) continue;
            MimeType mimeType = resolveMimeType(image.getMimeType());
            mediaList.add(new Media(mimeType, URI.create(image.getImageUrl())));
        }
        if (mediaList.isEmpty()) {
            return new UserMessage(safeText);
        }
        return UserMessage.builder().text(safeText).media(mediaList).build();
    }

    private MimeType resolveMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
        try {
            return MimeTypeUtils.parseMimeType(mimeType);
        } catch (Exception ex) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
    }

    private List<Message> limitImageMessages(List<Message> messages) {
        if (messages.isEmpty()) {
            return messages;
        }
        int limit = maxImageMessages == null ? 0 : maxImageMessages;
        if (limit < 0) {
            return messages;
        }
        int remaining = limit;
        List<Message> reversed = new ArrayList<>(messages.size());
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (hasMedia(message)) {
                if (remaining > 0) {
                    remaining--;
                    reversed.add(message);
                } else {
                    reversed.add(stripMedia(message));
                }
            } else {
                reversed.add(message);
            }
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private boolean hasMedia(Message message) {
        if (!(message instanceof MediaContent mediaContent)) {
            return false;
        }
        return mediaContent.getMedia() != null && !mediaContent.getMedia().isEmpty();
    }

    private Message stripMedia(Message message) {
        String text = message.getText() == null ? "" : message.getText();
        return switch (message.getMessageType()) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            case SYSTEM, TOOL -> new SystemMessage(text);
        };
    }

    /**
     * 根据聊天模式构建基础 Prompt
     */
    private List<Message> buildMessagesByMode(ChatEntity chatEntity, List<ImagesResponse> images, Long userId) {

        //获取用户姓名
        String userName = chatEntity.getUserName();
        //获取用户输入
        String userInput = chatEntity.getUserInput();
        //获取对话模式
        ChatMode chatMode = chatEntity.getChatMode();

        switch (chatMode) {
            case INTERNET_SEARCH:
                return createInternetSearchMessages(userName, userInput, images);
            case KNOWLEDGE_BASE:
                return createKnowledgeBaseMessages(userId, userInput, images);
            case DIRECT:
                return createDirectMessages(userName, userInput, images);
            default:
                throw new IllegalArgumentException("不支持的聊天模式: " + chatMode);
        }
    }

    /**
     * 知识库 Prompt
     */
    private List<Message> createKnowledgeBaseMessages(Long userId, String userInput, List<ImagesResponse> images) {
        log.info("【用户: {}】使用【知识库模式】", userId);
        List<Document> relatedDocs = documentService.doSearch(userInput, userId);

        String context = CollectionUtils.isEmpty(relatedDocs) ?
                "没有找到相关的知识库信息。" :
                relatedDocs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));

        String promptText = renderTemplate(ragPromptTemplateStr, context, userInput);
        return List.of(buildUserMessage(promptText, images));
    }

    /**
     * 联网搜索 Prompt
     */
    private List<Message> createInternetSearchMessages(String userId, String userInput, List<ImagesResponse> images) {
        log.info("【用户: {}】使用【联网搜索模式】", userId);
        List<SearchResult> searchResults = searXngService.search(userInput);

        String context = CollectionUtils.isEmpty(searchResults) ?
                "未能获取到有效的网络搜索结果。" :
                searchResults.stream()
                        .map(r -> String.format("【标题】: %s\n【摘要】: %s\n【链接】: %s", r.getTitle(), r.getContent(), r.getUrl()))
                        .collect(Collectors.joining("\n\n---\n\n"));

        String promptText = renderTemplate(internetSearchPromptTemplateStr, context, userInput);
        return List.of(buildUserMessage(promptText, images));
    }

    /**
     * 基础 Prompt
     */
    private List<Message> createDirectMessages(String userId, String userInput, List<ImagesResponse> images) {
        log.info("【用户: {}】使用【普通模式】", userId);
        return List.of(buildUserMessage(userInput, images));
    }

    private String renderTemplate(String template, String context, String question) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        String result = template.replace("{context}", context == null ? "" : context);
        result = result.replace("{question}", question == null ? "" : question);
        return result;
    }

}
