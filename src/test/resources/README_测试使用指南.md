# 文章生成器集成测试使用指南

## 概述

本测试套件提供了两种类型的测试：

1. **独立阶段测试**：支持人工干预和手动编辑的分步测试
2. **自动化完整流程测试**：无值守的端到端测试

## 环境配置

### 必需的环境变量

```bash
# 启用AI相关测试
export ENABLE_AI_TEST=true

# Pixabay API密钥（可选，用于图片搜索）
export PIXABAY_API_KEY=your_pixabay_api_key
```

### Google Cloud认证

确保已配置Google Cloud认证以使用Vertex AI Gemini：

```bash
# 设置应用默认凭据
gcloud auth application-default login

# 或设置服务账号密钥
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

## 独立阶段测试（支持人工干预）

### 测试方法列表

1. `步骤1_创意阶段测试` - 扩展用户想法
2. `步骤2_选题阶段测试` - 生成候选标题
3. `步骤3_构思阶段测试` - 生成文章大纲
4. `步骤4_写作阶段测试` - 生成完整文章
5. `步骤5_优化阶段测试` - 润色文章内容
6. `步骤6_配图规划阶段测试` - 设计配图方案
7. `步骤7_配图执行阶段测试` - 生成实际配图
8. `步骤8_完成阶段测试` - 生成最终文章

### 使用方式

#### 1. 顺序执行（推荐）

```bash
# 按顺序执行各个阶段
mvn test -Dtest=ArticleGeneratorIntegrationTest#"步骤1_创意阶段测试"
mvn test -Dtest=ArticleGeneratorIntegrationTest#"步骤2_选题阶段测试"
mvn test -Dtest=ArticleGeneratorIntegrationTest#"步骤3_构思阶段测试"
# ... 继续其他步骤
```

#### 2. 手动编辑中间结果

每个阶段执行后，会在 `workspace/generated/AI对日常生活的影响分析/` 目录下生成：

- `{阶段编号}. {阶段名称}.yaml` - 阶段结果数据
- `{阶段编号}. {阶段名称}.md` - Markdown格式内容（从构思阶段开始）

**手动编辑流程：**

1. 执行某个阶段测试
2. 查看生成的YAML和Markdown文件
3. 根据需要编辑YAML文件中的 `result` 字段
4. 执行下一个阶段测试（会自动使用编辑后的内容）

#### 3. 跳过某些阶段

如果某个阶段的YAML文件已存在，后续阶段会自动使用该结果，无需重新执行。

### 文件结构示例

```
workspace/generated/AI对日常生活的影响分析/
├── 1. 创意阶段.yaml
├── 2. 选题阶段.yaml
├── 3. 构思阶段.yaml
├── 3. 构思阶段.md
├── 4. 写作阶段.yaml
├── 4. 写作阶段.md
├── 5. 优化阶段.yaml
├── 5. 优化阶段.md
├── 6. 配图规划阶段.yaml
├── 6. 配图规划阶段.md
├── 7. 配图执行阶段.yaml
├── 7. 配图执行阶段.md
├── 8. 完成阶段.yaml
├── 8. 完成阶段.md
└── images/
    ├── image_hero-image.jpg
    └── image_section1-image.jpg
```

## 自动化完整流程测试

### 测试方法列表

1. `自动化测试_AI技术文章完整流程` - AI主题文章
2. `自动化测试_远程工作文章完整流程` - 远程工作主题
3. `自动化测试_环保主题文章完整流程` - 环保主题
4. `自动化测试_简单主题快速流程` - 简化参数测试

### 使用方式

```bash
# 执行单个自动化测试
mvn test -Dtest=ArticleGeneratorIntegrationTest#"自动化测试_AI技术文章完整流程"

# 执行所有自动化测试
mvn test -Dtest=ArticleGeneratorIntegrationTest#"自动化测试*"
```

### 特点

- 无需人工干预，自动执行完整的8个阶段
- 每个测试使用不同的初始参数
- 生成完整的工作流摘要文件 `workflow_summary.yaml`
- 适合验证系统的端到端功能

## YAML文件格式

### 阶段结果YAML格式

```yaml
stage: "创意阶段"
stageNumber: 1
timestamp: "2025-06-27T16:30:00"
result: |
  这里是阶段的输出内容...
metadata:
  sessionId: "session-123"
  executionTime: 5000
  inputLength: 100
  outputLength: 500
```

### 工作流摘要YAML格式

```yaml
articleTitle: "AI对日常生活的影响分析"
success: true
startTime: "2025-06-27T16:00:00"
endTime: "2025-06-27T16:15:00"
totalDuration: 15.0
articleFilePath: "/path/to/article.md"
imagesDirectory: "/path/to/images"
stageResults:
  CREATIVE:
    stage: "创意阶段"
    success: true
    duration: 3000
  # ... 其他阶段
```

## 配图功能说明

### 配图注释格式

在配图规划阶段，AI会生成如下格式的配图注释：

```html
<!--
配图点ID: hero-image
类型: photo
描述: 展示人工智能技术在生活中的应用
关键词: artificial intelligence, technology, daily life
风格: 现代科技风格，蓝色调
-->
```

### 配图执行过程

1. 解析配图注释
2. 使用关键词在Pixabay搜索图片
3. 下载最佳匹配的图片
4. 优化图片尺寸（适合移动端显示）
5. 生成相对路径的Markdown图片引用

## 故障排除

### 常见问题

1. **AI测试被跳过**
    - 确保设置了 `ENABLE_AI_TEST=true`

2. **Google Cloud认证失败**
    - 检查 `gcloud auth application-default login`
    - 验证项目ID配置

3. **图片下载失败**
    - 检查网络连接
    - 验证Pixabay API密钥
    - 查看日志中的具体错误信息

4. **前一阶段结果不存在**
    - 确保按顺序执行阶段测试
    - 检查YAML文件是否正确生成

### 日志查看

测试运行时会输出详细日志，包括：

- 阶段执行状态
- 文件生成路径
- 图片下载进度
- 错误信息

## 最佳实践

1. **首次使用**：先运行系统验证测试
2. **开发调试**：使用独立阶段测试，便于定位问题
3. **回归测试**：使用自动化完整流程测试
4. **内容优化**：手动编辑YAML文件来改进内容质量
5. **批量测试**：运行多个自动化测试来验证不同场景

## 扩展说明

### 添加新的测试场景

1. 在自动化测试部分添加新的测试方法
2. 定义不同的 `ArticleGenerationRequest` 参数
3. 调用 `executeCompleteWorkflow` 方法

### 自定义阶段处理

1. 修改对应的阶段执行辅助方法
2. 调整YAML保存和加载逻辑
3. 更新验证断言
