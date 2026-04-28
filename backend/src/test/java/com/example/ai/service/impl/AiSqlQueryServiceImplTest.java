package com.example.ai.service.impl;

import com.example.ai.Factory.ChatClientFactory;
import com.example.ai.config.ChatModelProperties;
import com.example.ai.mapper.AiSqlQueryMapper;
import com.example.ai.pojo.AiSqlQueryResponse;
import com.example.ai.security.Role;
import com.example.ai.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSqlQueryServiceImplTest {

    @Mock
    private ChatClientFactory chatClientFactory;

    @Mock
    private AiSqlQueryMapper aiSqlQueryMapper;

    @Mock
    private OperationLogService operationLogService;

    private AiSqlQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        ChatModelProperties properties = new ChatModelProperties();
        ChatModelProperties.Platform platform = new ChatModelProperties.Platform();
        ChatModelProperties.Platform.Options option = new ChatModelProperties.Platform.Options();
        option.setModel("test-model");
        platform.setOptions(List.of(option));
        properties.setPlatforms(List.of(platform));

        service = new AiSqlQueryServiceImpl(
                chatClientFactory,
                properties,
                aiSqlQueryMapper,
                operationLogService,
                new ObjectMapper()
        );
    }

    @Test
    void studentCanQueryStudentRoleViews() {
        mockAiSql("SELECT t.phone FROM ai_student_course_list_view c JOIN ai_student_teacher_list_view t ON c.teacher_name = t.name WHERE c.course_name = '数据结构'");
        when(aiSqlQueryMapper.executeSelect(any())).thenReturn(List.of(Map.of("phone", "13811110002")));

        AiSqlQueryResponse response = service.query("数据结构老师电话是多少", null, 10, Role.STUDENT.name());

        assertEquals(1, response.getRowCount());
        assertEquals("13811110002", response.getRows().get(0).get("phone"));
    }

    @Test
    void teacherCanQueryTeacherRoleViews() {
        mockAiSql("SELECT s.guardian_phone FROM ai_teacher_student_list_view s WHERE s.name = '陈晨'");
        when(aiSqlQueryMapper.executeSelect(any())).thenReturn(List.of(Map.of("guardian_phone", "13800000001")));

        AiSqlQueryResponse response = service.query("陈晨家长电话是多少", null, 20, Role.TEACHER.name());

        assertEquals(1, response.getRowCount());
        assertEquals("13800000001", response.getRows().get(0).get("guardian_phone"));
    }

    @Test
    void studentCannotQueryTeacherRoleViews() {
        mockAiSql("SELECT * FROM ai_teacher_student_list_view");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.query("试着查教师视图", null, 10, Role.STUDENT.name())
        );

        assertEquals("Only allowed role views may be queried", ex.getMessage());
    }

    @Test
    void adminCanQueryRealTables() {
        mockAiSql("SELECT t.phone FROM teachers t WHERE t.name = '张伟'");
        when(aiSqlQueryMapper.executeSelect(any())).thenReturn(List.of(Map.of("phone", "13811110001")));

        AiSqlQueryResponse response = service.query("张伟老师电话是多少", null, 1, Role.ADMIN.name());

        assertEquals(1, response.getRowCount());
        assertEquals("13811110001", response.getRows().get(0).get("phone"));
    }

    private void mockAiSql(String sql) {
        ChatClient client = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClientFactory.getClient(eq("test-model"))).thenReturn(client);
        when(client.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("{\"sql\":\"" + sql.replace("\"", "\\\"") + "\"}");
    }
}
