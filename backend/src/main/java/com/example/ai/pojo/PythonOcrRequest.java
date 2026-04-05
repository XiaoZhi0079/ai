package com.example.ai.pojo;

import lombok.Data;

@Data
public class PythonOcrRequest {

    private String file;
    private String fileName;
    private String contentType;
}
