package com.mashang.bac.web.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.*;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        
        // 注册 Spring AI 相关类
        kryo.register(Media.class);
        kryo.register(ArrayList.class);
        kryo.register(UserMessage.class);
        kryo.register(AssistantMessage.class);
        kryo.register(SystemMessage.class);
    }

    public FileBasedChatMemory(@Value("${app.chat-memory.dir:./chat-memory}") String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, Message message) {
        ChatMemory.super.add(conversationId, message);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> allMessages = getOrCreateConversation(conversationId);
        return allMessages.stream()
                .skip(Math.max(0, allMessages.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                List<Message> rawMessages = kryo.readObject(input, ArrayList.class);
                // 过滤掉 null 元素
                if (rawMessages != null) {
                    messages = rawMessages.stream()
                            .filter(msg -> msg != null)
                            .collect(java.util.stream.Collectors.toList());
                }
            } catch (Exception e) {
                // 如果序列化失败，删除旧文件并重新开始
                System.err.println("序列化读取失败，清理旧文件: " + e.getMessage());
                file.delete();
                messages = new ArrayList<>();
            }
        }
        return messages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            // 过滤掉 null 元素
            List<Message> filteredMessages = messages.stream()
                    .filter(msg -> msg != null)
                    .collect(java.util.stream.Collectors.toList());
            kryo.writeObject(output, filteredMessages);
        } catch (Exception e) {
            System.err.println("序列化写入失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
