package com.example.ai.enums;



public enum ChatMode {

    /**
     * 直接对话模式
     */
    DIRECT,

    /**
     * 基于已上传文档的知识库模式 (RAG)
     */
    KNOWLEDGE_BASE,

    /**
     * 联网搜索模式
     */
    INTERNET_SEARCH

}
