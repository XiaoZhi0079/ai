package com.example.ai.pojo;

import lombok.Data;

@Data
public class PythonOcrResponse {

    private Boolean success;
    private String text;
    private String markdown;
}
