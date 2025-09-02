package com.mashang.bac.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashScopeImageResponse {

    private String requestId;
    private Map<String, Object> usage;
    private Output output;

    @Data
    public static class Output {
        private List<Choice> choices;
    }

    @Data
    public static class Choice {
        @JsonProperty("finish_reason")
        private String finishReason;
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private List<Content> content;
    }

    @Data
    public static class Content {
        private String image;
    }
}


