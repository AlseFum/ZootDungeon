# 代码生成系统 - 完整指南

一个强大的异步代码生成和文件管理系统，支持模板生成、内容插入、删除等功能。

## 📋 核心概念

### 1. 行内标记 `//@@identifier`

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

### 2. 模板文件 `*.tpl` 或 `*.template`

模板文件包含 `//@@$$` 标记，代表输出文件名的标识符。当从模板生成文件时，`$$` 会被替换为具体的文件名。

**模板文件示例：** `food.tpl`
```java
package com.zootdungeon.items.food;

public class //@@$$-classname extends Food {
    {
        //@@$$-init
    }
    
    public void execute() {
        //@@$$-execute
    }
}
```

生成 `Apple.java` 时，`//@@$$` 变为 `//@@Apple`。

### 3. 缓存文件 `cache.text`

自动记录所有的操作信息：
- 所有插入操作
- 模板文件的映射关系
- 已生成的文件列表
- 所有的标记位置

## ✨ 主要功能

### 1. insertContent - 插入内容

在指定文件的标记位置插入内容。

```javascript
const { insertContent } = require('./scan.js');

const result = await insertContent(
    './core/src/main/java/com/zootdungeon/Assets.java',
    'morepng',
    'public static final String MOREPNG = "environment/custom_tiles/morepng.png";'
);

console.log(result); // true 或 false
```

**特点：**
- ✅ 一个标记只能插入一次，重复插入会被阻止
- ✅ 自动添加 `//@@identifier+++` 和 `//@@identifier---` 标记
- ✅ 更新缓存记录

### 2. removeContent - 删除已插入的内容

删除 `//@@identifier+++` 到 `//@@identifier---` 之间的所有内容，标记本身保留。

```javascript
const { removeContent } = require('./scan.js');

const result = await removeContent(
    './Assets.java',
    'morepng'
);

console.log(result); // true 或 false
```

**特点：**
- ✅ 只删除插入的内容
- ✅ 保留原始标记点
- ✅ 更新缓存记录

### 3. generateFromTemplate - 从模板生成文件

从模板文件生成新文件，并将配置内容插入到指定的标记位置。

```javascript
const { generateFromTemplate } = require('./scan.js');

const filePath = await generateFromTemplate(
    './core/src/main/java/com/zootdungeon/items/food/food.tpl',
    'Bellagio',  // 输出文件名（不含后缀）
    {
        'imports': '',
        'docclass': 'Bellagio 类 - 特殊食物',
        'classname': 'Bellagio',
        'init': 'name = "Bellagio";\nimage = ItemSpriteSheet.FOOD;',
        'body': '',
        'execute': 'hero.HP(Math.min(hero.HP() + 20, hero.HT()));',
        'info': '"美味的佳肴"'
    }
);

console.log(filePath); // 生成的文件路径或 null
```

**工作流程：**
1. 读取模板文件
2. 将 `//@@$$` 替换为文件名（如 `//@@Bellagio`）
3. 创建输出文件
4. 将配置内容插入到对应的标记位置
5. 更新缓存记录

**config 对象的键值组合：**
- 模板中的 `//@@$$-init` 生成 `//@@Bellagio-init`
- config 中的 `init: 'content'` 插入到 `//@@Bellagio-init` 位置

### 4. deleteGeneratedFile - 删除生成的文件

删除通过系统生成的文件，并清理缓存中的所有相关记录。

```javascript
const { deleteGeneratedFile } = require('./scan.js');

const result = await deleteGeneratedFile(
    './core/src/main/java/com/zootdungeon/items/food/Bellagio.java'
);

console.log(result); // true 或 false
```

**安全机制：**
- ✅ 验证文件是否存在
- ✅ 验证文件是否在缓存中被记录为生成文件
- ✅ 只有通过系统生成的文件才能删除
- ✅ 删除文件的同时清理缓存中的所有相关记录和标记

### 5. scanDirectory - 扫描目录

递归扫描指定目录，找出所有的标记、模板文件和已插入的内容范围。

```javascript
const { scanDirectory } = require('./scan.js');

const results = await scanDirectory(process.cwd());

console.log('模板文件:', results.templates);
console.log('标记点:', results.markers);
console.log('已插入内容:', results.insertedRanges);
```

**扫描结果包含：**
- `templates` - 所有 `*.tpl` 和 `*.template` 文件
- `markers` - 所有 `//@@identifier` 标记及其位置
- `insertedRanges` - 所有 `//@@identifier+++` 到 `//@@identifier---` 的范围

## 📝 标记格式规则

### ✅ 有效的标记

```java
//@@myId                    // 基本标记
//@@food-list              // 带连字符
//@@item_123               // 带下划线
//@@$$                      // 模板中使用
//@@Bellagio-init           // 生成文件中的组合标记
```

### ❌ 无效的标记

```java
//@@myId+++                // 已插入内容的起始标记（自动生成）
//@@myId---                // 已插入内容的结束标记（自动生成）
// @@myId                  // 中间有空格
//@@                       // 缺少标识符
```

## 💾 缓存管理

### 缓存文件结构

```json
{
  "insertions": [
    {
      "file": "path/to/file",
      "identifier": "myId",
      "content": "inserted content",
      "timestamp": "2025-11-16T10:00:00.000Z"
    }
  ],
  "templates": {
    "path/to/template.tpl": [
      {
        "generatedFile": "path/to/Generated.java",
        "identifier": "Generated",
        "config": { ... }
      }
    ]
  },
  "markers": {
    "identifier1": [
      {
        "file": "path/to/file",
        "line": 100
      }
    ]
  }
}
```

### 使用缓存

```javascript
const { Cache } = require('./scan.js');

// 加载缓存
const cache = Cache.load();

// 查看插入操作
console.log(cache.insertions);

// 查看模板映射
console.log(cache.templates);

// 查看标记
console.log(cache.markers);

// 手动保存缓存
cache.save();
```

## 🔧 完整 API 参考

### insertContent(filePath, identifier, content)

**参数：**
- `filePath` {string} - 目标文件路径
- `identifier` {string} - 标记标识符
- `content` {string} - 要插入的内容

**返回：** `Promise<boolean>` - 是否成功

**异常情况：**
- 文件不存在
- 标记不存在
- 标记已被插入过（防止重复）

---

### removeContent(filePath, identifier)

**参数：**
- `filePath` {string} - 目标文件路径
- `identifier` {string} - 标记标识符

**返回：** `Promise<boolean>` - 是否成功

**异常情况：**
- 文件不存在
- 标记范围不存在

---

### generateFromTemplate(templatePath, outputName, config)

**参数：**
- `templatePath` {string} - 模板文件路径
- `outputName` {string} - 输出文件名（不含后缀和路径）
- `config` {Object} - 配置对象，键值对应模板中的标记

**返回：** `Promise<string|null>` - 生成的文件路径或 null

**配置对象示例：**
```javascript
{
    'imports': 'import com.example.*;',
    'classname': 'MyClass',
    'init': 'this.name = "test";',
    'execute': 'doSomething();'
}
```

---

### deleteGeneratedFile(filePath)

**参数：**
- `filePath` {string} - 生成的文件路径

**返回：** `Promise<boolean>` - 是否成功

**安全检查：**
- 验证文件存在
- 验证文件在缓存中被记录
- 验证文件是通过系统生成的

**删除操作：**
1. 删除物理文件
2. 清理缓存中的模板记录
3. 清理缓存中的相关标记
4. 保存更新后的缓存

---

### scanDirectory(directory)

**参数：**
- `directory` {string} - 要扫描的目录路径

**返回：** `Promise<Object>` - 扫描结果

**结果对象：**
```javascript
{
  templates: [              // 模板文件列表
    'path/to/file.tpl',
    'path/to/template.template'
  ],
  markers: [               // 标记点列表
    {
      file: 'path/to/file',
      line: 100,
      identifier: 'myId',
      type: 'marker'
    }
  ],
  insertedRanges: [        // 已插入内容范围
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

---

### Cache.load(cacheFile)

**参数：**
- `cacheFile` {string} - 缓存文件路径（默认: 'script/cache.text'）

**返回：** `Cache` - 缓存对象

---

### cache.save(cacheFile)

**参数：**
- `cacheFile` {string} - 缓存文件路径（默认: 'script/cache.text'）

**作用：** 将缓存保存到文件

## ⚠️ 重要注意事项

### 1. 标记唯一性

在同一文件中，相同的标记只能插入一次。重复尝试插入会被系统阻止：

```javascript
await insertContent('./file.java', 'myId', 'content1'); // ✅ 成功
await insertContent('./file.java', 'myId', 'content2'); // ❌ 失败：已插入
```

### 2. 模板文件中的 `$$` 替换

`//@@$$` 会被替换为输出文件名（不含后缀）：

```
生成 Apple.java 时：
  //@@$$        → //@@Apple
  //@@$$-init   → //@@Apple-init
  //@@$$-body   → //@@Apple-body
```

### 3. 配置对象的键值组合

config 对象的键会和输出文件名用连字符组合：

```javascript
generateFromTemplate(tpl, 'Apple', {
    init: 'xxx',      // 生成 //@@Apple-init
    body: 'yyy',      // 生成 //@@Apple-body
    execute: 'zzz'    // 生成 //@@Apple-execute
})
```

### 4. 路径分隔符处理

系统自动处理 Windows（`\`）和 Unix（`/`）的路径分隔符差异。

### 5. 异步操作

所有主要操作都是异步的，必须使用 `await` 或 `.then()` 来获取结果。

## 🚀 实际使用示例

### 场景 1：生成多个食物类

```javascript
const { generateFromTemplate } = require('./scan.js');

const foods = ['Apple', 'Bread', 'Meat'];

for (const food of foods) {
    await generateFromTemplate(
        './food.tpl',
        food,
        {
            classname: food,
            docclass: `${food} 食物类`,
            init: `name = "${food}";`,
            execute: `hero.HP(Math.min(hero.HP() + 10, hero.HT()));`,
            info: `"A delicious ${food}"`
        }
    );
}
```

### 场景 2：更新已生成的文件

```javascript
const { removeContent, insertContent } = require('./scan.js');

// 删除旧内容
await removeContent('./Apple.java', 'Apple-execute');

// 插入新内容
await insertContent(
    './Apple.java',
    'Apple-execute',
    'hero.HP(hero.HT()); // 完全恢复'
);
```

### 场景 3：清理不需要的生成文件

```javascript
const { deleteGeneratedFile } = require('./scan.js');

// 删除文件（包括缓存清理）
await deleteGeneratedFile('./Apple.java');
```

## 🎯 设计优势

✅ **完全异步** - 基于 Promise 的异步操作，高效非阻塞  
✅ **安全保护** - 防止重复插入、误删重要文件  
✅ **完整缓存** - 所有操作都被记录，可追踪和回退  
✅ **灵活标记** - 支持任意标记名称和组合  
✅ **模板复用** - 一个模板可生成多个文件  
✅ **自动清理** - 删除文件时自动清理缓存  
✅ **跨平台** - 自动处理路径分隔符差异

## 📚 更多帮助

所有函数都支持 async/await 语法，建议在异步函数中使用：

```javascript
async function main() {
    try {
        const result = await insertContent(...);
        console.log('成功:', result);
    } catch (error) {
        console.error('错误:', error);
    }
}

main();
```

