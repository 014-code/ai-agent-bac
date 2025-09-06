#!/bin/bash
echo "清理聊天记忆序列化文件..."
if [ -d "chat-memory" ]; then
    rm -rf chat-memory
    echo "已完全删除 chat-memory 目录"
else
    echo "chat-memory 目录不存在"
fi
echo "清理完成！"
