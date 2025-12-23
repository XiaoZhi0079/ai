//package com.example.ai.pojo;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import org.springframework.ai.chat.messages.MessageType;
//
//
//import java.time.LocalDateTime;
//
//@Data
//@Table(name = "springai")
//public class MessagePojo {
//
//  @Id
//  @GeneratedValue(strategy = GenerationType.UUID)
//  private Long id;
//
//  @Column(name = "conversation_id", length = 36, nullable = false)
//  private String conversationId;
//
//  @Column(name = "MessageType")
//  private MessageType type;
//
//  @Column(columnDefinition = "TEXT",name="content")
//  private String content;
//
//  @Column(name = "updateTime")
//  private LocalDateTime updateTime;
//
//  @Column(name = "createTime")
//  private LocalDateTime careteTime;
//
//
//  // 如果将来要加 media 字段（图片、视频等链接或元数据），可以加：
//  @Column(name = "media_url")
//  private String mediaUrl;
//
//  // 或者保存 JSON / 元数据字符串
//  @Column(name = "media_meta", columnDefinition = "TEXT")
//  private String mediaMeta;
//
//}
