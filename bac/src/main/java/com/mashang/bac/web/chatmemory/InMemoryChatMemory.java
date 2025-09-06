package com.mashang.bac.web.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存聊天记忆实现 - 临时解决方案
 */
@Component
public class InMemoryChatMemory implements ChatMemory {

    private final ConcurrentMap<String, List<Message>> conversations = new ConcurrentHashMap<>();

    @Override
    public void add(String conversationId, Message message) {
        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(message);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messages = conversations.getOrDefault(conversationId, new ArrayList<>());
        return messages.stream()
                .skip(Math.max(0, messages.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        conversations.remove(conversationId);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
    }
}
