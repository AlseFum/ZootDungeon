package com.zootdungeon.items.supply;

import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.ScrollPane;
import com.zootdungeon.ui.Window;
import com.zootdungeon.ui.RedButton;
import com.watabou.noosa.ui.Component;

/**
 * 界面测试工具：用于快速弹出窗口验证自己写的 UI 组件。
 *
 * <pre>
 * UI_Test.line("Hello");
 * UI_Test.row("Value", 42);
 * </pre>
 */
public class UI_Test {

    private static final int WIDTH = 140;
    private static final int MARGIN = 4;
    private static final int GAP = 2;

    private UI_Test() {}

    /**
     * 简单文本窗口。
     */
    public static void line(String text) {
        GameScene.show(new SimpleTestWnd(text));
    }

    /**
     * 简单文本窗口，多行。
     */
    public static void show(String title, String... lines) {
        GameScene.show(new SimpleTestWnd(title, lines));
    }

    /**
     * 标签值行。
     */
    public static void row(String label, Object value) {
        GameScene.show(new SimpleTestWnd(label + ": " + value));
    }

    /**
     * 显示自定义测试窗口。
     */
    public static void show(TestWindow wnd) {
        GameScene.show(wnd);
    }

    /**
     * 简单文本窗口。
     */
    public static class SimpleTestWnd extends Window {

        public SimpleTestWnd(String singleLine) {
            this(null, singleLine != null ? new String[]{singleLine} : new String[]{});
        }

        public SimpleTestWnd(String title, String... lines) {
            super();

            int colW = WIDTH - MARGIN * 2;

            // 1. 创建内容组件
            Component content = new Component();

            // 2. 创建 ScrollPane（参数是内容组件）
            ScrollPane pane = new ScrollPane(content);
            add(pane);
            pane.setRect(0, 0, WIDTH, 100); // pane 尺寸

            // 3. 标题（相对于 content 左上角）
            if (title != null && !title.isEmpty()) {
                RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 9);
                titleBlock.hardlight(TITLE_COLOR);
                titleBlock.maxWidth(colW);
                titleBlock.setPos(0, 0);
                content.add(titleBlock);
            }

            // 4. 内容行
            float y = title != null && !title.isEmpty() ? GAP + 14 : 0;
            for (String line : lines) {
                RenderedTextBlock block = PixelScene.renderTextBlock(line, 6);
                block.maxWidth(colW);
                block.setPos(0, y);
                content.add(block);
                y = block.bottom() + GAP;
            }

            // 5. 设置 content 尺寸
            content.setRect(0, 0, colW, y);

            // 6. 设置 pane 高度
            pane.setRect(0, 0, WIDTH, Math.min((int) y + MARGIN, (int) PixelScene.uiCamera.height - 20));

            resize(WIDTH, (int) pane.bottom());
        }
    }
}
