# 云RAG使用说明

## 🎯 功能概述

云RAG功能允许你查询云端知识库并基于查询结果进行AI对话，提供更准确和相关的回答。

## 📋 可用方法

### 1. 基础云RAG查询
```java
// 查询云RAG知识库，返回相关文档
List<Document> documents = loveApp.doChatYunRag("谁是加炜");
```

**特点**：
- 直接返回查询到的文档列表
- 包含文档内容和元数据
- 适合需要查看原始文档的场景

### 2. 云RAG对话
```java
// 基于云RAG查询结果进行AI对话
String answer = loveApp.doChatWithYunRag("谁是加炜？", chatId);
```

**特点**：
- 先查询云RAG获取相关文档
- 将文档内容作为上下文提供给AI
- 返回基于知识库的AI回答
- 支持多轮对话

## 🔧 配置要求

### 1. API Key配置
确保在 `application.yml` 中配置了正确的API Key：
```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key-here
```

### 2. 知识库配置
确保云RAG知识库已创建并配置：
- 知识库名称：`恋爱大师知识库`
- 确保知识库中有相关数据

## 📝 使用示例

### 测试用例
```java
@Test
void doChatYunRag() {
    // 基础查询
    List<Document> documents = loveApp.doChatYunRag("谁是加炜");
    Assertions.assertNotNull(documents);
    System.out.println("云RAG查询结果数量: " + documents.size());
    
    for (int i = 0; i < documents.size(); i++) {
        Document doc = documents.get(i);
        System.out.println("文档 " + (i + 1) + ": " + doc.getContent());
        System.out.println("元数据: " + doc.getMetadata());
    }
}

@Test
void doChatWithYunRag() {
    // 云RAG对话
    String chatId = UUID.randomUUID().toString();
    String message = "谁是加炜？";
    String answer = loveApp.doChatWithYunRag(message, chatId);
    Assertions.assertNotNull(answer);
    System.out.println("云RAG对话回答: " + answer);
}
```

### 实际使用
```java
@Resource
private LoveApp loveApp;

public void example() {
    String chatId = UUID.randomUUID().toString();
    
    // 方式1：只查询文档
    List<Document> docs = loveApp.doChatYunRag("恋爱技巧");
    System.out.println("找到 " + docs.size() + " 条相关文档");
    
    // 方式2：基于文档进行AI对话
    String answer = loveApp.doChatWithYunRag("如何维持长期关系？", chatId);
    System.out.println("AI回答: " + answer);
}
```

## 🔍 工作流程

### 云RAG对话流程
1. **查询云RAG**：根据用户问题查询云端知识库
2. **获取文档**：返回相关的文档列表
3. **构建上下文**：将文档内容整合到提示词中
4. **AI回答**：基于文档上下文生成回答
5. **返回结果**：返回AI生成的回答

### 日志输出
```
云RAG查询到 3 条文档
文档 1: 加炜是程序员...
元数据: {source=knowledge_base, type=person}
文档 2: 加炜擅长Spring AI...
元数据: {source=knowledge_base, type=skill}
云RAG对话结果: 根据知识库信息，加炜是一位程序员...
```

## ⚠️ 注意事项

1. **网络依赖**：需要网络连接访问云端知识库
2. **API限制**：注意API调用频率和配额限制
3. **知识库内容**：确保知识库中有相关数据
4. **错误处理**：网络异常时会记录错误日志

## 🚀 优势

1. **准确性**：基于真实知识库数据回答
2. **实时性**：可以查询最新的云端数据
3. **可扩展性**：知识库可以随时更新
4. **灵活性**：支持多种查询和对话方式

## 🔧 故障排除

### 常见问题

1. **查询结果为空**：
   - 检查知识库名称是否正确
   - 确认知识库中有相关数据
   - 检查API Key是否有效

2. **网络连接问题**：
   - 检查网络连接
   - 确认API服务可用性

3. **API Key错误**：
   - 检查配置文件中的API Key
   - 确认API Key权限

现在你可以使用云RAG功能了！🎉
