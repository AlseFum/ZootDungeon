# 代码生成与任务管线系统 - 完整指南

本系统包含两层：
- **底层（IO 层）**: `io.js` - 文件操作原子函数（insertContent, removeContent, generateFromTemplate, deleteGeneratedFile）
- **上层（Pipeline 层）**: `pipeline.js` - 声明式任务编排（task / job / pipeline 模型）

---

## 📋 架构概览

```
┌─────────────────────────────────────┐
│  你的业务代码                        │
│  (声明式创建 task / job / pipeline)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  pipeline.js                         │
│  - createJob()                       │
│  - createPipeline()                  │
│  - 依赖拓扑排序 + 执行控制           │
│  - insert 内容合并                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  io.js                               │
│  - insertContent()                   │
│  - removeContent()                   │
│  - generateFromTemplate()            │
│  - deleteGeneratedFile()             │
│  - scanDirectory()                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  文件系统                             │
│  (模板 / 标记 / 生成文件)             │
└─────────────────────────────────────┘
```

---

## 第一部分：底层 IO API

### 1. 核心概念

#### 1.1 行内标记 `//@@identifier`

用于标记可以插入内容的位置，标记必须单独占一行或在行尾。

**插入前：**
```java
public class MyClass {
    //@@init
}
```

**插入后：**
```java
public class MyClass {
    //@@init
    //@@init+++
    // 插入的内容在这里
    //@@init---
}
```

#### 1.2 模板文件 `*.tpl` 或 `*.template`

模板文件包含 `$$` 占位符和 `//@@$$` 标记，当从模板生成文件时，所有 `$$` 会被替换为具体的输出文件名。

**模板文件示例：** `food.tpl`
```java
package com.zootdungeon.items.food;

public class $$-classname extends Food {
    //@@$$-init
    
    public void execute() {
        //@@$$-execute
    }
}
```

生成 `Apple.java` 时：
- `$$-classname` → `Apple-classname`
- `//@@$$-init` → `//@@Apple-init`

#### 1.3 缓存文件 `script/cache.txt`

自动记录所有操作信息，包括：
- 所有插入操作
- 模板文件的映射关系
- 已生成的文件列表
- 所有的标记位置

### 2. IO API 函数

#### 2.1 insertContent(filePath, identifier, content)

在指定文件的标记位置插入内容。多次对同一 `(filePath, identifier)` 的调用会被在 pipeline 层面合并。

**参数：**
- `filePath` {string} - 目标文件路径
- `identifier` {string} - 标记标识符
- `content` {string} - 要插入的内容

**返回：** `Promise<boolean>` - 是否成功

**特点：**
- 一个标记只能插入一次（单次调用），重复会被阻止
- 自动添加 `//@@identifier+++` 和 `//@@identifier---` 标记
- 在 pipeline 执行时，同一标记的多次插入会先合并再统一写入
- 自动更新缓存记录

**示例：**
```javascript
import { insertContent } from './io.js';

const result = await insertContent(
    'core/src/main/java/com/zootdungeon/Assets.java',
    'food-assets',
    'public static final String APPLE = "items/food/apple.png";'
);
```

---

#### 2.2 removeContent(filePath, identifier)

删除 `//@@identifier+++` 到 `//@@identifier---` 之间的所有内容，标记本身保留。

**参数：**
- `filePath` {string} - 目标文件路径
- `identifier` {string} - 标记标识符

**返回：** `Promise<boolean>` - 是否成功

**特点：**
- 只删除插入的内容
- 保留原始标记点
- 更新缓存记录

**示例：**
```javascript
import { removeContent } from './io.js';

const result = await removeContent(
    './core/src/main/java/com/zootdungeon/Assets.java',
    'food-assets'
);
```

---

#### 2.3 generateFromTemplate(templatePath, outputName, config)

从模板文件生成新文件，并将配置内容插入到指定的标记位置。

**参数：**
- `templatePath` {string} - 模板文件路径
- `outputName` {string} - 输出文件名（不含后缀）
- `config` {Object} - 配置对象，键值对应标记名后缀

**返回：** `Promise<string|null>` - 生成的文件路径或 null

**工作流程：**
1. 读取模板文件
2. 全局替换所有 `$$` 为 `outputName`
3. 创建输出文件
4. 根据 `config` 对象的键，生成 `outputName-key` 标记名，并插入对应内容
5. 更新缓存记录

**示例：**
```javascript
import { generateFromTemplate } from './io.js';

const filePath = await generateFromTemplate(
    'core/src/main/java/com/zootdungeon/items/food/food.tpl',
    'Apple',
    {
        'init': 'name = "Apple";\nimage = ItemSpriteSheet.FOOD;',
        'execute': 'hero.HP(Math.min(hero.HP() + 15, hero.HT()));',
        'info': '"A fresh red apple"'
    }
);
// 生成 Apple.java，并在 //@@Apple-init、//@@Apple-execute、//@@Apple-info 处插入内容
```

---

#### 2.4 deleteGeneratedFile(filePath)

删除通过系统生成的文件，并清理缓存中的所有相关记录。

**参数：**
- `filePath` {string} - 生成的文件路径

**返回：** `Promise<boolean>` - 是否成功

**安全机制：**
- 验证文件是否存在
- 验证文件是否在缓存中被记录为生成文件
- 只有通过系统生成的文件才能删除
- 删除文件的同时清理缓存中的所有相关记录和标记

**示例：**
```javascript
import { deleteGeneratedFile } from './io.js';

const result = await deleteGeneratedFile(
    'core/src/main/java/com/zootdungeon/items/food/Apple.java'
);
```

---

#### 2.5 scanDirectory(directory)

递归扫描指定目录，找出所有的模板、标记和已插入的内容范围。

**参数：**
- `directory` {string} - 要扫描的目录路径

**返回：** `Promise<Object>` - 扫描结果

**结果对象：**
```javascript
{
  templates: [                    // 模板文件列表
    'path/to/file.tpl',
    'path/to/template.template'
  ],
  markers: [                      // 标记点列表
    {
      file: 'path/to/file',
      line: 100,
      identifier: 'myId',
      type: 'marker'
    }
  ],
  insertedRanges: [               // 已插入内容范围
    {
      file: 'path/to/file',
      identifier: 'myId',
      startLine: 100,
      endLine: 110,
      contentLines: 10
    }
  ]
}
```

**示例：**
```javascript
import { scanDirectory } from './io.js';

const results = await scanDirectory(process.cwd());
console.log('模板文件:', results.templates);
console.log('标记点:', results.markers);
console.log('已插入内容:', results.insertedRanges);
```

---

### 3. 标记格式规则

#### ✅ 有效的标记
```java
//@@myId                    // 基本标记
//@@food-list              // 带连字符
//@@item_123               // 带下划线
//@@$$                      // 模板中使用
//@@Apple-init             // 生成文件中的组合标记
$$-classname               // 模板中的 $$ 占位符
```

#### ❌ 无效的标记
```java
//@@myId+++                // 已插入内容的起始标记（自动生成）
//@@myId---                // 已插入内容的结束标记（自动生成）
// @@myId                  // 中间有空格
//@@                       // 缺少标识符
```

---

## 第二部分：上层 Pipeline API

### 1. 核心概念

- **op（原子操作）**：对 `io.js` 的一次调用，用数组表示：
  - `['insert',  fileSpec, identifier, content]`
  - `['remove',  fileSpec, identifier]`
  - `['generate', templateSpec, outputName, config]`
  - `['delete',  fileSpec]`
  - `['custom',  name, asyncHandler]`

- **task（任务）**：一串按顺序执行的 op

- **job（作业）**：一组可以并行执行的 task，带依赖和路径映射

- **pipeline（管线）**：多个 job 的集合，负责：
  - 扫描工程找出所有模板和标记
  - 校验 id 和依赖
  - 解析 `fileSpec` / `templateSpec`
  - 根据 job 依赖做拓扑排序
  - 按 job 顺序执行（job 内 task 可串行或并行）
  - **合并 insert 操作**：同一 `(file, identifier)` 的多次 insert 会合并成一个操作

### 2. 特殊 fileSpec / templateSpec

#### 2.1 fileSpec

- `'thisfile'`  
  使用当前 op 的 `identifier`，在扫描结果中找到 **唯一** 的标记位置所在文件。

  ```js
  ['insert', 'thisfile', 'foodregistry', 'new Apple(...),']
  ```

- `'filewithin:markerName'`  
  使用指定的 `markerName`，在扫描结果中找到 **唯一** 的文件。

- 路径映射 + 普通字符串  
  - 先查 `job.paths[fileSpec]`
  - 再查 `pipeline.paths[fileSpec]`
  - 找不到就原样返回

#### 2.2 templateSpec

- `'tpl:food'`  
  会被转换为文件名 `food.tpl`，然后在扫描出的模板列表中按 basename 匹配（要求唯一）。

  ```js
  ['generate', 'tpl:food', 'Apple', { hunger: 10, name: 'Apple' }]
  ```

- `'food.tpl'`  
  直接按文件名匹配。

- 显式路径  
  含有 `/` 或 `\` 或以 `.` 开头的字符串，直接视为路径。

---

### 3. API 使用

#### 3.1 createJob(id, initialTaskDefs?, options?)

创建一个 job builder。

**示例：**
```javascript
import { createJob } from './pipeline.js';

const job = createJob('food', [], {
  parallel: false,
  paths: {
    assets: 'core/src/main/java/com/zootdungeon/Assets.java'
  }
});

job
  .and(taskDef1)
  .and(() => taskDef2)
  .after('validate');
```

**参数：**
- `id` {string} - job 标识符
- `initialTaskDefs` - 初始 task 定义数组（可选）
- `options` {Object} - 选项（可选）
  - `parallel` {boolean} - job 内 task 是否并行执行（默认 `false`）
  - `paths` {Object} - 本 job 的路径映射表

**方法：**
- `.and(taskDef)` - 添加一个 task
- `.after(jobId | job)` - 指定依赖关系
- `.withPaths(pathMap)` - 设置路径映射

---

#### 3.2 createPipeline(...jobsOrBuildersOrOptions)

创建 pipeline。

**示例：**
```javascript
import { createJob, createPipeline } from './pipeline.js';

function createFood(name, hunger) {
  return [
    ['generate', 'tpl:food', name, { hunger, name }],
    ['insert', 'thisfile', 'foodregistry', `new ${name}(${hunger}),`]
  ];
}

const validateJob = createJob('validate')
  .and([
    ['custom', 'validate-templates', async () => {
      console.log('checking templates...');
    }]
  ]);

const foodJob = createJob('food')
  .and(() => createFood('GoldenApple', 120))
  .and(() => createFood('SilverApple', 80))
  .after(validateJob);

const pipeline = createPipeline(
  validateJob,
  foodJob,
  { name: 'food-pipeline' }
);

await pipeline.compile();  // scan + 校验 + 解析
pipeline.print();          // 打印执行顺序
await pipeline.run();      // 执行
// await pipeline.run({ dryRun: true }); // 只打印，不执行 IO
```

**参数：**
- 若干个 job 或 job builder
- 最后一个可选参数是 `options` 对象

**方法：**
- `async compile(rootDir?)` - 扫描 + 校验 + 解析
- `print()` - 打印 job 执行顺序
- `async run({ dryRun }?)` - 执行 pipeline

---

### 4. 执行语义

#### 4.1 compile() 阶段

- 调用 `scanDirectory(rootDir)`
- 校验 job id / task id 唯一
- 校验 job 依赖存在且无循环
- 解析所有 `fileSpec` / `templateSpec`
- 根据 job 依赖做拓扑排序

#### 4.2 run() 阶段

- 按排序好的 job 顺序执行
- 若某 job 的依赖中存在失败 job，则该 job 标记为 skipped
- job 内：
  - 如果 `parallel: true`，task 并行执行；否则串行
  - 任一 op 抛错 ⇒ 当前 job 标记为 failed，后续 op / task 不再执行
- **insert 合并**：所有 job 执行完后，将同一 `(file, identifier)` 的多次 insert 合并成一个操作

#### 4.3 错误处理

- 任意一个 op 失败 ⇒ 当前 job 停止，依赖它的后续 job 被标记为 skipped
- 前面成功的 job 不受影响
- **无回退机制**（轻量设计）

---

## 完整示例

### 场景：生成多个食物类并注册资产

**1. 创建工具函数**

```javascript
// task-tools.js
export function createFood(name, hunger, info) {
  return [
    // 从模板生成食物类文件
    ['generate', 'tpl:food', name, { hunger, name, info }],
    
    // 在注册表中插入记录
    ['insert', 'thisfile', 'foodregistry', `new ${name}(${hunger}),`]
  ];
}
```

**2. 组织为 job 和 pipeline**

```javascript
// main.js
import { createJob, createPipeline } from './pipeline.js';
import { createFood } from './task-tools.js';

const validateJob = createJob('validate')
  .and([
    ['custom', 'validate-templates', async () => {
      console.log('✓ 验证模板...');
    }]
  ]);

const foodJob = createJob('foods')
  .and(() => createFood('GoldenApple', 120, 'Golden fruit'))
  .and(() => createFood('SilverApple', 80, 'Silver fruit'))
  .and(() => createFood('BronzeApple', 40, 'Bronze fruit'))
  .after(validateJob);

const pipeline = createPipeline(validateJob, foodJob, {
  name: 'food-generation'
});

await pipeline.compile();
pipeline.print();
await pipeline.run();

// 输出示例：
// 🚀 Job start: validate
// ✓ 验证模板...
// ✅ Job finish: validate
// 🚀 Job start: foods
// ... 生成 3 个食物类 ...
// 📝 开始合并写入插入内容...
// ✓ 已插入内容: //@@foodregistry (3 段合并)
// ✅ Job finish: foods
```

结果：`build/testpipeline/FoodRegistry.txt` 中的 `//@@foodregistry` 标记位置会一次性插入：
```text
new GoldenApple(120),
new SilverApple(80),
new BronzeApple(40),
```

---

## 重要概念补充

### 1. 为什么要分层设计？

- **IO 层** (`io.js`)：原子文件操作，可独立使用或被其他工具调用
- **Pipeline 层** (`pipeline.js`)：声明式编排，自动处理依赖、合并插入、路径映射等

### 2. Insert 内容合并的好处

- 不用手动编排多个 insert 的顺序
- 同一个标记位置的内容会自动收集并一次性插入
- 避免重复的 IO 调用

### 3. 最小化设计

- 只负责：扫描 + 校验 + 依赖排序 + IO 调度 + insert 合并
- 不包含：回退、时间调度、监控统计等复杂功能
- 适合轻量级的代码生成工作流

---

## 快速参考

| 操作 | API | 说明 |
|------|-----|------|
| 在文件中插入内容 | `insertContent(file, id, content)` | IO 层：单次插入 |
| 删除已插入内容 | `removeContent(file, id)` | IO 层 |
| 从模板生成文件 | `generateFromTemplate(tpl, name, config)` | IO 层 |
| 删除生成文件 | `deleteGeneratedFile(file)` | IO 层 |
| 扫描模板和标记 | `scanDirectory(dir)` | IO 层 |
| 创建作业 | `createJob(id, tasks?, options?)` | Pipeline 层 |
| 创建管线 | `createPipeline(...jobs, options?)` | Pipeline 层 |
| 编译检查 | `pipeline.compile()` | Pipeline 层 |
| 打印执行计划 | `pipeline.print()` | Pipeline 层 |
| 执行管线 | `pipeline.run({ dryRun }?)` | Pipeline 层 |

---

这套设计完全符合你的需求：**声明式 + 依赖排序 + 顺序执行 IO**，没有时间调度、统计、回退等复杂功能。
