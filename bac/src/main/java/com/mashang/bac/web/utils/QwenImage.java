package com.mashang.bac.web.utils;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.mashang.bac.web.common.TestApiKey;
import com.mashang.bac.web.dto.DashScopeImageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里文生图sdk
 */
@Component
public class QwenImage {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public void call(String text) throws ApiException, NoApiKeyException, UploadFileException, IOException {

        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("text", text)
                )).build();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("watermark", true);
        parameters.put("prompt_extend", true);
        parameters.put("negative_prompt", "");
        parameters.put("size", "1328*1328");

        String effectiveKey = (apiKey == null || apiKey.isBlank())
                ? System.getenv("DASHSCOPE_API_KEY")
                : apiKey;

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(effectiveKey)
                .model("qwen-image")
                .messages(Collections.singletonList(userMessage))
                .parameters(parameters)
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println(JsonUtils.toJson(result));
    }

    /**
     * 生成图片并返回完整响应对象
     */
    public DashScopeImageResponse callAndReturn(String text) throws ApiException, NoApiKeyException, UploadFileException, IOException {
        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("text", text)
                )).build();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("watermark", true);
        parameters.put("prompt_extend", true);
        parameters.put("negative_prompt", "");
        parameters.put("size", "1328*1328");

        String effectiveKey = (apiKey == null || apiKey.isBlank())
                ? System.getenv("DASHSCOPE_API_KEY")
                : apiKey;

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(effectiveKey)
                .model("qwen-image")
                .messages(Collections.singletonList(userMessage))
                .parameters(parameters)
                .build();

        MultiModalConversationResult result = conv.call(param);
        String json = JsonUtils.toJson(result);
        return new ObjectMapper().readValue(json, DashScopeImageResponse.class);
    }

    /**
     * 生成图片并返回首张图片URL
     */
    public String callForFirstImageUrl(String text) throws ApiException, NoApiKeyException, UploadFileException, IOException {
        DashScopeImageResponse resp = callAndReturn(text);
        if (resp != null
                && resp.getOutput() != null
                && resp.getOutput().getChoices() != null
                && !resp.getOutput().getChoices().isEmpty()
                && resp.getOutput().getChoices().get(0).getMessage() != null
                && resp.getOutput().getChoices().get(0).getMessage().getContent() != null
                && !resp.getOutput().getChoices().get(0).getMessage().getContent().isEmpty()) {
            return resp.getOutput().getChoices().get(0).getMessage().getContent().get(0).getImage();
        }
        return null;
    }

}
