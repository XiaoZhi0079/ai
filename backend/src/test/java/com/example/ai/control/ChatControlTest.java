package com.example.ai.control;

import com.example.ai.enums.ChatMode;
import com.example.ai.pojo.ChatEntity;
import com.example.ai.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatControlTest {

    @Test
    void chatStreamReturnsServerSentEvents() {
        ChatService chatService = mock(ChatService.class);
        ChatControl controller = new ChatControl(chatService);
        ChatEntity entity = new ChatEntity();
        entity.setChatId("chat-1");
        entity.setChatMode(ChatMode.DIRECT);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(chatService.streamChat(entity, null)).thenReturn(Flux.just("你", "好"));

        ServerSentEvent<String> first = controller.chatStream(entity, request).blockFirst();

        assertEquals("你", first.data());
    }
}
