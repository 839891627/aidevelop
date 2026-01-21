package com.example.aidevelop.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String id;
    private MessageRole role;
    private String content;
    private LocalDateTime timestamp;
    private String model;  // 使用的模型
}

