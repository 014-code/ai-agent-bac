@echo off
echo 清理聊天记忆序列化文件...
if exist "chat-memory" (
    rmdir /s /q "chat-memory"
    echo 已完全删除 chat-memory 目录
) else (
    echo chat-memory 目录不存在
)
echo 清理完成！
pause
