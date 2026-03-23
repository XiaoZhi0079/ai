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
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Coordinates chat requests, prompt assembly, and memory persistence.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    // Core dependencies used to build prompts and fetch context.
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
        // Read the request payload first so the later steps stay simple.
        String chatId = chatEntity.getChatId();
        String modelName = chatEntity.getModel();
        String userInput = chatEntity.getUserInput();

        // Use the beginning of the first user message as the conversation title.
        String title = null;
        if (userInput != null && !userInput.isBlank()) {
            title = userInput.length() > 50 ? userInput.substring(0, 50) : userInput;
        }

        // Save metadata for sidebar history.
        chatHistoryRepository.save(chatId, chatEntity.getChatMode().name(), userId, title);

        ChatClient chatClient = clientFactory.getClient(modelName);

        // Build the current-round messages and merge them with memory.
        List<ImagesResponse> images = chatEntity.getImageFiles();
        List<Message> baseMessages = buildMessagesByMode(chatEntity, images, userId);
        List<Message> mergedMessages = appendHistory(chatId, baseMessages);
        Prompt promptWithHistory = new Prompt(mergedMessages);

        // Send the final prompt to the selected model.
        String reply = chatClient.prompt(promptWithHistory)
                /*.tools(new DateTimeTools())*/ // Prefer dependency injection if tools become stateful.
                .call()
                .content();
        saveToMemory(chatId, userInput, reply, images);
        return reply;
    }

    private List<Message> appendHistory(String chatId, List<Message> baseMessages) {
        if (!StringUtils.hasText(chatId)) {
            return baseMessages;
        }
        // Memory is optional; blank history means only the current round is sent.
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
        // Store both sides of the conversation so follow-up questions keep context.
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

        // Only valid image URLs are attached to the multimodal user message.
        List<Media> mediaList = new ArrayList<>();
        for (ImagesResponse image : images) {
            if (!StringUtils.hasText(image.getImageUrl())) {
                continue;
            }
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

        // Keep the most recent image messages and downgrade older ones to plain text.
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
     * Create the prompt messages for the selected chat mode.
     */
    private List<Message> buildMessagesByMode(ChatEntity chatEntity, List<ImagesResponse> images, Long userId) {
        String userName = chatEntity.getUserName();
        String userInput = chatEntity.getUserInput();
        ChatMode chatMode = chatEntity.getChatMode();

        return switch (chatMode) {
            case INTERNET_SEARCH -> createInternetSearchMessages(userName, userInput, images);
            case KNOWLEDGE_BASE -> createKnowledgeBaseMessages(userId, userInput, images);
            case DIRECT -> createDirectMessages(userName, userInput, images);
            default -> throw new IllegalArgumentException("Unsupported chat mode: " + chatMode);
        };
    }

    /**
     * Build a prompt that includes retrieved knowledge-base context.
     */
    private List<Message> createKnowledgeBaseMessages(Long userId, String userInput, List<ImagesResponse> images) {
        log.info("User {} is using knowledge-base mode", userId);
        List<Document> relatedDocs = documentService.doSearch(userInput, userId);

        String context = CollectionUtils.isEmpty(relatedDocs)
                ? "No relevant knowledge-base content was found."
                : relatedDocs.stream().map(this::formatKnowledgeContext).collect(Collectors.joining("\n---\n"));

        String promptText = renderTemplate(ragPromptTemplateStr, context, userInput);
        return List.of(buildUserMessage(promptText, images));
    }

    /**
     * Build a prompt that includes internet search results.
     */
    private List<Message> createInternetSearchMessages(String userId, String userInput, List<ImagesResponse> images) {
        log.info("User {} is using internet-search mode", userId);
        List<SearchResult> searchResults = searXngService.search(userInput);

        String context = CollectionUtils.isEmpty(searchResults)
                ? "No valid internet search result was returned."
                : searchResults.stream()
                .map(result -> String.format("[Title] %s\n[Snippet] %s\n[Link] %s",
                        result.getTitle(),
                        result.getContent(),
                        result.getUrl()))
                .collect(Collectors.joining("\n\n---\n\n"));

        String promptText = renderTemplate(internetSearchPromptTemplateStr, context, userInput);
        return List.of(buildUserMessage(promptText, images));
    }

    /**
     * Build a prompt for direct chat without external context.
     */
    private List<Message> createDirectMessages(String userId, String userInput, List<ImagesResponse> images) {
        log.info("User {} is using direct mode", userId);
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

    private String formatKnowledgeContext(Document document) {
        String fileName = document.getMetadata() == null ? null : String.valueOf(document.getMetadata().get("fileName"));
        if (!StringUtils.hasText(fileName)) {
            return document.getText();
        }
        return "[来源文件] " + fileName + "\n" + document.getText();
    }
}
