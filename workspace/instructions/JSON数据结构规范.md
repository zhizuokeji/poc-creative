# JSON数据结构规范

## 概述

本文档定义了文章生成系统中各个阶段使用的统一JSON数据结构，确保数据格式的一致性和向后兼容性。

## 核心原则

1. **纯JSON格式**：所有大模型的输入和输出都必须是纯JSON格式，不允许使用markdown内嵌json的混合格式
2. **结构化章节**：从写作阶段开始，文章内容必须是结构化的章节列表
3. **渐进式完善**：各阶段的数据结构支持渐进式完善，后续阶段可以在前一阶段的基础上添加新字段
4. **向后兼容**：新增字段不影响现有字段的使用

## 统一的文章内容数据结构

### ArticleContent（文章内容）

```json
{
  "title": "文章标题",
  "description": "文章简介/摘要",
  "metadata": {
    "author": "作者姓名",
    "createdAt": "创建时间（ISO格式）",
    "estimatedReadingTime": 10,
    "wordCount": 2500,
    "tags": [
      "标签1",
      "标签2"
    ],
    "category": "文章分类",
    "language": "zh-CN"
  },
  "sections": [
    {
      "title": "章节标题（可以为空，如引言）",
      "content": "章节的完整文本内容",
      "imageDescription": "配图描述（从配图计划阶段开始有值）",
      "imagePath": "图片相对路径（从配图执行阶段开始有值）",
      "level": 1,
      "order": 1
    }
  ]
}
```

### 字段说明

#### 顶层字段

- `title`: 文章标题（必填）
- `description`: 文章简介或摘要（必填）
- `metadata`: 文章元数据（必填）
- `sections`: 章节列表（必填）

#### metadata字段

- `author`: 作者姓名（可选）
- `createdAt`: 创建时间，ISO 8601格式（可选）
- `estimatedReadingTime`: 预估阅读时间，单位分钟（可选）
- `wordCount`: 总字数（可选）
- `tags`: 标签数组（可选）
- `category`: 文章分类（可选）
- `language`: 语言代码（可选，默认zh-CN）

#### sections字段

- `title`: 章节标题（可选，引言等章节可能没有标题）
- `content`: 章节的完整文本内容（必填）
- `imageDescription`: 配图描述（可选，从配图计划阶段开始有值）
- `imagePath`: 图片相对路径（可选，从配图执行阶段开始有值）
- `level`: 标题级别，1-6（可选，默认为2）
- `order`: 章节顺序（可选，用于排序）

## 各阶段的数据结构

### 构思阶段输出

```json
{
  "title": "文章标题",
  "description": "文章简介",
  "structure": "文章结构类型（如：问题-分析-解决方案、总-分-总等）",
  "outline": {
    "introduction": {
      "title": "引言标题",
      "keyPoints": [
        "要点1",
        "要点2"
      ],
      "estimatedWords": 200,
      "purpose": "引言的作用和目标"
    },
    "mainSections": [
      {
        "title": "主要章节标题",
        "keyPoints": [
          "核心要点1",
          "要点2"
        ],
        "estimatedWords": 600,
        "importance": "high/medium/low",
        "subsections": [
          {
            "title": "子章节标题",
            "keyPoints": [
              "子要点1",
              "子要点2"
            ],
            "estimatedWords": 300
          }
        ]
      }
    ],
    "conclusion": {
      "title": "结论标题",
      "keyPoints": [
        "总结要点1",
        "要点2"
      ],
      "estimatedWords": 200,
      "purpose": "结论的作用"
    }
  },
  "totalEstimatedWords": 2500,
  "readingTime": "8-10分钟"
}
```

### 写作阶段输出

使用统一的ArticleContent结构，sections字段包含完整的文章内容：

```json
{
  "title": "文章标题",
  "description": "文章简介",
  "metadata": {
    "estimatedReadingTime": 10,
    "wordCount": 2500,
    "tags": [
      "标签1",
      "标签2"
    ],
    "category": "文章分类"
  },
  "sections": [
    {
      "title": "",
      "content": "引言的完整内容...",
      "level": 1,
      "order": 1
    },
    {
      "title": "第一章标题",
      "content": "第一章的完整内容...",
      "level": 2,
      "order": 2
    }
  ]
}
```

### 配图执行阶段输出

在写作阶段的基础上，为每个章节添加imagePath字段：

```json
{
  "title": "文章标题",
  "description": "文章简介",
  "metadata": {
    ...
  },
  "sections": [
    {
      "title": "",
      "content": "引言的完整内容...",
      "imageDescription": "展示主题概念的配图",
      "imagePath": "images/hero-image.jpg",
      "level": 1,
      "order": 1
    },
    {
      "title": "第一章标题",
      "content": "第一章的完整内容...",
      "imageDescription": "说明第一章要点的配图",
      "imagePath": "images/section1-image.jpg",
      "level": 2,
      "order": 2
    }
  ]
}
```

### 完成阶段输出

与配图执行阶段相同的结构，但内容经过最终优化：

```json
{
  "title": "最终文章标题",
  "description": "最终文章简介",
  "metadata": {
    "author": "作者姓名",
    "createdAt": "2025-06-28T10:30:00Z",
    "estimatedReadingTime": 10,
    "wordCount": 2500,
    "tags": [
      "标签1",
      "标签2"
    ],
    "category": "文章分类",
    "language": "zh-CN"
  },
  "sections": [
    {
      "title": "",
      "content": "最终优化的引言内容...",
      "imageDescription": "展示主题概念的配图",
      "imagePath": "images/hero-image.jpg",
      "level": 1,
      "order": 1
    }
  ]
}
```

## 测试代码数据处理

### YAML序列化/反序列化

测试代码中使用Jackson YAML进行数据处理：

```kotlin
// 序列化为YAML
val yamlMapper = ObjectMapper(YAMLFactory())
val yamlContent = yamlMapper.writeValueAsString(articleContent)

// 反序列化YAML
val articleContent = yamlMapper.readValue(yamlContent, ArticleContent::class.java)
```

### Markdown文件生成

根据JSON数据生成Markdown文件：

```kotlin
fun generateMarkdown(articleContent: ArticleContent): String {
    val markdown = StringBuilder()

    // 添加Front Matter
    markdown.append("---\n")
    markdown.append("title: \"${articleContent.title}\"\n")
    markdown.append("description: \"${articleContent.description}\"\n")
    // ... 其他元数据
    markdown.append("---\n\n")

    // 添加标题
    markdown.append("# ${articleContent.title}\n\n")

    // 添加章节
    articleContent.sections.forEach { section ->
        // 添加图片（如果有）
        if (!section.imagePath.isNullOrBlank()) {
            markdown.append("![${section.imageDescription ?: ""}](${section.imagePath})\n\n")
        }

        // 添加章节标题（如果有）
        if (!section.title.isNullOrBlank()) {
            val headerLevel = "#".repeat(section.level ?: 2)
            markdown.append("$headerLevel ${section.title}\n\n")
        }

        // 添加章节内容
        markdown.append("${section.content}\n\n")
    }

    return markdown.toString()
}
```

## 版本兼容性

### 向后兼容原则

1. **新增字段**：只能在现有结构基础上新增可选字段
2. **字段重命名**：不允许重命名现有字段
3. **字段删除**：不允许删除现有字段
4. **类型变更**：不允许改变现有字段的数据类型

### 版本标识

在需要时，可以在metadata中添加版本字段：

```json
{
  "metadata": {
    "schemaVersion": "1.0",
    ...
  }
}
```
