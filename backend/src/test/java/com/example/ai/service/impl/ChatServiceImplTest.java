package com.example.ai.service.impl;

import com.example.ai.Factory.ChatClientFactory;
import com.example.ai.enums.ChatMode;
import com.example.ai.pojo.ChatEntity;
import com.example.ai.repository.ChatHistoryRepository;
import com.example.ai.service.DocumentService;
import com.example.ai.service.SearXngService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.StreamResponseSpec;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatClientFactory chatClientFactory;

    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private SearXngService searXngService;

    @Mock
    private ChatMemory chatMemory;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(
                chatClientFactory,
                chatHistoryRepository,
                documentService,
                searXngService,
                chatMemory
        );
        ReflectionTestUtils.setField(service, "ragPromptTemplateStr", "{context}\n{question}");
        ReflectionTestUtils.setField(service, "internetSearchPromptTemplateStr", "{context}\n{question}");
        ReflectionTestUtils.setField(service, "maxImageMessages", 3);
    }

    @Test
    void streamChatReturnsChunksAndPersistsCombinedReply() {
        ChatEntity entity = new ChatEntity();
        entity.setChatId("chat-1");
        entity.setModel("test-model");
        entity.setUserInput("你好");
        entity.setUserName("alice");
        entity.setChatMode(ChatMode.DIRECT);

        ChatClient client = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        StreamResponseSpec streamSpec = mock(StreamResponseSpec.class);

        when(chatClientFactory.getClient("test-model")).thenReturn(client);
        when(client.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("你", "好"));
        when(chatMemory.get("chat-1")).thenReturn(List.of());

        List<String> chunks = service.streamChat(entity, 7L).collectList().block();

        assertIterableEquals(List.of("你", "好"), chunks);
        verify(chatHistoryRepository).save("chat-1", "DIRECT", 7L, "你好");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(chatMemory).add(eq("chat-1"), messageCaptor.capture());
        verify(chatMemory).add(eq("chat-1"), messageCaptor.capture());
        List<Message> savedMessages = messageCaptor.getAllValues();

        assertEquals(UserMessage.class, savedMessages.get(0).getClass());
        assertEquals("你好", savedMessages.get(0).getText());
        assertEquals(AssistantMessage.class, savedMessages.get(1).getClass());
        assertEquals("你好", savedMessages.get(1).getText());
    }
}
