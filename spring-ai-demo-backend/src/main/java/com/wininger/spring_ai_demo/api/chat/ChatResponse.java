package com.wininger.spring_ai_demo.api.chat;

import java.util.Date;

public record ChatResponse(
        String prompt,
        String response,
        String model,
        int conversationId,
        Date requestTimeStartTime,
        Date requestEndTime
) {}
