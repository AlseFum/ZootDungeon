# Window 尺寸与定位指南

本文档说明如何正确创建避免尺寸和定位错误的 Window。

## 核心概念

### Window 类结构

```
Window (extends Group)
├── chrome (NinePatch) - 窗口边框/装饰
├── blocker (PointerArea) - 点击外部关闭
├── shadow (ShadowBox) - 阴影效果
└── camera - 窗口内坐标系统的相机

protected int width, height       - 窗口尺寸
protected int xOffset, yOffset    - 窗口偏移
```

### Window 构造函数调用顺序

```
1. super() / super(width, height)     ← 初始化 chrome, blocker, shadow, camera
2. add(child)                         ← 添加子组件
3. resize(w, h)                       ← 设置尺寸并重新计算 camera 位置
4. child.setRect() / setPos()        ← 定位子组件（必须在 resize 之后）
```

## 屏幕尺寸适配

### 获取屏幕尺寸

使用 `PixelScene.uiCamera` 获取 UI 相机的实际尺寸：

```java
int screenW = (int) PixelScene.uiCamera.width;
int screenH = (int) PixelScene.uiCamera.height;
```

### 边缘留白常量

参考 `WndRhodesIslandTerminal`，定义边缘留白：

```java
private static final int SCREEN_EDGE_MARGIN = 24;
```

### 自适应窗口尺寸

根据屏幕尺寸计算窗口尺寸：

```java
private static final int DEFAULT_WIDTH = 180;
private static final int MIN_WIDTH = 140;
private static final int MAX_WIDTH = 240;

int maxByScreen = screenW - 2 * SCREEN_EDGE_MARGIN - chrome.marginHor();
int windowW;

if (screenW < 300) {
    // 窄屏：使用可用最大宽度
    windowW = Math.max(MIN_WIDTH, maxByScreen);
} else {
    // 正常屏：限制最大宽度
    windowW = Math.min(MAX_WIDTH, maxByScreen);
}
```

### 确保窗口在屏幕内

调用 `boundOffsetWithMargin()` 防止窗口超出屏幕边缘：

```java
boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
```

## 正确模式

### 模式 1: 简单窗口（无 ScrollPane）

```java
public class WndSimple extends Window {

    public WndSimple() {
        super();

        // 1. 添加内容到窗口（使用窗口坐标 0,0 开始）
        RenderedTextBlock text = PixelScene.renderTextBlock("Hello", 9);
        text.setPos(GAP, GAP);
        add(text);

        // 2. 计算所需高度
        int contentHeight = 50;

        // 3. 调用 resize 设置窗口尺寸
        resize(WIDTH, contentHeight);

        // 4. 子组件已在 0,0 坐标添加，无需额外定位
    }
}
```

### 模式 2: 带 ScrollPane 的窗口（正确顺序）

```java
public class WndWithScroll extends Window {

    private ScrollPane scrollPane;
    private Component content;

    public WndWithScroll() {
        super();

        // 1. 创建 content 和 scrollPane
        content = new Component();
        scrollPane = new ScrollPane(content);
        add(scrollPane);

        // 2. 构建内容（使用 content 坐标）
        float pos = GAP;
        for (int i = 0; i < 20; i++) {
            RedButton btn = new RedButton("Button " + i, 8) {
                @Override
                protected void onClick() { /* ... */ }
            };
            btn.setRect(GAP, pos, WIDTH - GAP * 2, 18);
            content.add(btn);
            pos += 20;
        }

        // 3. 设置 content 尺寸
        content.setSize(WIDTH, pos);

        // 4. 调用 resize 设置窗口尺寸（必须先调用！）
        int windowHeight = (int) Math.min(pos + chrome.marginTop() + chrome.marginBottom(), 400);
        resize(WIDTH, windowHeight);

        // 5. 最后设置 ScrollPane 位置（在 chrome 内部）
        float contentX = chrome.marginLeft();
        float contentY = chrome.marginTop();
        float contentW = width - chrome.marginHor();
        float contentH = height - chrome.marginVer();
        scrollPane.setRect(contentX, contentY, contentW, contentH);
    }
}
```

### 模式 3: WndKeyBindings 风格（参考实现）

```java
public WndKeyBindings(Boolean controller) {
    super();

    // ... 添加标题、分隔线等 ...

    bindingsList = new Component();
    ScrollPane scrollingList = new ScrollPane(bindingsList) { ... };
    add(scrollingList);

    // 在 bindingsList 中添加项目
    int y = 0;
    for (GameAction action : actionList) {
        BindingItem item = new BindingItem(action);
        item.setRect(0, y, WIDTH, BindingItem.HEIGHT);
        bindingsList.addToBack(item);
        y += item.height();
    }
    bindingsList.setSize(WIDTH, y + 1);

    // 计算窗口高度
    int windowHeight = Math.min(
        BTN_HEIGHT * 3 + 3 + BindingItem.HEIGHT * listItems.size(),
        PixelScene.uiCamera.height - 20
    );

    // resize 在添加完所有内容之后
    resize(WIDTH, windowHeight);

    // 在 resize 之后添加按钮并定位
    RedButton btnDefaults = new RedButton(...);
    btnDefaults.setRect(0, height - BTN_HEIGHT * 2 - 1, WIDTH, BTN_HEIGHT);
    add(btnDefaults);

    // ScrollPane 定位在最后
    scrollingList.setRect(0, BTN_HEIGHT + 1, WIDTH, btnDefaults.top() - BTN_HEIGHT - 1);
}
```

### 模式 4: 响应式 ScrollPane 窗口（完整示例）

这是最完善的窗口模式，同时考虑屏幕尺寸和自适应布局。

核心要点：
- 使用 `super()` 而非 `super(WIDTH, HEIGHT)`
- 使用 `PixelScene.uiCamera.width/height` 获取实际屏幕尺寸
- 根据屏幕宽度自适应窗口宽度（窄屏用满宽度，正常屏限制最大宽度）
- 计算 `maxByScreen` 考虑屏幕边缘留白 `SCREEN_EDGE_MARGIN`
- resize 之后调用 `boundOffsetWithMargin()` 确保窗口在屏幕范围内
- buildUI 方法接收 windowW 参数，使布局元素使用正确的宽度

```java
public class WndResponsive extends Window {

    private static final int SCREEN_EDGE_MARGIN = 24;
    private static final int GAP = 2;

    private static final int DEFAULT_WIDTH = 180;
    private static final int MIN_WIDTH = 140;
    private static final int MAX_WIDTH = 240;

    private ScrollPane scrollPane;
    private Component content;

    public WndResponsive() {
        super();

        // 1. 获取屏幕尺寸
        int screenW = (int) PixelScene.uiCamera.width;
        int screenH = (int) PixelScene.uiCamera.height;

        // 2. 创建 content 和 scrollPane
        content = new Component();
        scrollPane = new ScrollPane(content);
        add(scrollPane);

        // 3. 根据屏幕尺寸计算窗口宽度
        int maxByScreen = screenW - 2 * SCREEN_EDGE_MARGIN - chrome.marginHor();
        int windowW = screenW < 300
            ? Math.max(MIN_WIDTH, maxByScreen)
            : Math.min(MAX_WIDTH, maxByScreen);

        // 4. 构建内容（传递计算后的宽度）
        float pos = buildUI(windowW);
        content.setSize(windowW, pos);

        // 5. 计算窗口高度（考虑屏幕高度限制）
        int maxHByScreen = screenH - 2 * SCREEN_EDGE_MARGIN - chrome.marginVer();
        int windowH = Math.min((int) pos + chrome.marginTop() + chrome.marginBottom(), maxHByScreen);

        // 6. resize + 定位
        resize(windowW, windowH);
        scrollPane.setRect(chrome.marginLeft(), chrome.marginTop(),
                width - chrome.marginHor(), height - chrome.marginVer());

        // 7. 确保窗口在屏幕范围内
        boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
    }

    // buildUI 接收 windowW 参数
    private float buildUI(int windowW) {
        float pos = GAP;

        // 使用 windowW 计算按钮宽度等
        RenderedTextBlock title = PixelScene.renderTextBlock("Title", 9);
        title.setPos(GAP, pos);
        title.maxWidth(windowW - GAP * 2);
        add(title);
        pos = title.bottom() + GAP * 2;

        // ... 添加更多内容 ...

        return pos;
    }
}
```

## 常见错误

### 错误 1: 在 resize 之前定位 ScrollPane

```java
// 错误：此时 chrome 未初始化，尺寸为 0
scrollPane.setRect(chrome.marginLeft(), chrome.marginTop(), width, height);
resize(WIDTH, 200);

// 正确：先 resize，再定位
resize(WIDTH, 200);
scrollPane.setRect(chrome.marginLeft(), chrome.marginTop(), width, height);
```

### 错误 2: 使用 super(WIDTH, 0) 导致 chrome 尺寸错误

```java
// 错误：height=0 导致 chrome 无法正确初始化
super(WIDTH, 0);

// 正确：使用无参构造函数
super();
resize(WIDTH, desiredHeight);
```

### 错误 3: 在构造函数中访问未创建的对象

```java
public MyButton() {
    super();
    // 错误：width/height 在 createChildren 之前为 0
    width = bg.width + 4;
    height = bg.height + 4;
}

// 正确：使用固定值或延迟设置
public MyButton() {
    super();
    width = 50;  // 固定值
    height = 30;
}
```

### 错误 4: ScrollPane 尺寸计算错误

```java
// 错误：使用内容高度作为窗口高度，未考虑 chrome 边距
resize(WIDTH, (int) contentHeight);

// 正确：加上 chrome 边距
int windowHeight = (int) (contentHeight + chrome.marginTop() + chrome.marginBottom());
resize(WIDTH, windowHeight);
```

### 错误 5: 固定窗口尺寸导致窄屏溢出

```java
// 错误：固定宽度，窄屏时可能超出屏幕
private static final int WIDTH = 180;

// 正确：根据屏幕尺寸自适应
int maxByScreen = screenW - 2 * MARGIN - chrome.marginHor();
int windowW = Math.min(MAX_WIDTH, maxByScreen);
```

## chrome 边距参考

Chrome 边框的边距可以通过以下方法获取：

- `chrome.marginLeft()` - 左边距
- `chrome.marginRight()` - 右边距
- `chrome.marginTop()` - 上边距
- `chrome.marginBottom()` - 下边距
- `chrome.marginHor()` - 左右边距之和
- `chrome.marginVer()` - 上下边距之和

## 调试技巧

1. **查看窗口是否正确居中**: Window 构造函数会自动将窗口居中到 `Game.width/height`

2. **使用 offset() 调整位置**:
   ```java
   // 将窗口向下偏移 50 像素
   offset(0, 50);
   ```

3. **检查 chrome 是否正确渲染**: 如果 chrome 显示异常，通常是 `resize()` 调用顺序问题

4. **日志输出调试**:
   ```java
   GLog.d("Window size: %d x %d, chrome margins: L=%d R=%d T=%d B=%d",
       width, height,
       chrome.marginLeft(), chrome.marginRight(),
       chrome.marginTop(), chrome.marginBottom());
   ```

5. **屏幕尺寸日志**:
   ```java
   GLog.d("Screen: %d x %d, Max window: %d x %d",
       screenW, screenH, maxByScreenW, maxByScreenH);
   ```

## 总结

### 构造函数调用顺序

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | `super()` | 初始化 Window 基本结构 |
| 2 | `add(child)` | 添加子组件到窗口 |
| 3 | `content.setSize()` | 设置内容尺寸 |
| 4 | `resize(w, h)` | **关键**：设置窗口尺寸，重新计算 camera |
| 5 | `scrollPane.setRect()` | 在 resize 之后定位 ScrollPane |

### 屏幕适配流程

```
1. 获取屏幕尺寸
   └─ PixelScene.uiCamera.width / height

2. 计算可用空间
   └─ maxByScreen = screenW - 2 * MARGIN - chrome.marginHor()

3. 确定窗口尺寸
   └─ windowW = 窄屏 ? 用满宽度 : 限制最大宽度

4. resize 窗口
   └─ resize(windowW, windowH)

5. 边界检查
   └─ boundOffsetWithMargin(MARGIN)
```

**核心原则**:
1. `resize()` 必须在任何依赖 `chrome` 或 `camera` 的操作之前调用
2. 窗口尺寸必须考虑实际屏幕尺寸，避免窄屏溢出
3. 始终调用 `boundOffsetWithMargin()` 确保窗口在屏幕内
