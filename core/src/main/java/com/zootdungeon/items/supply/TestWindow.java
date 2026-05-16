package com.zootdungeon.items.supply;

import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/**
 * 测试窗口基类。
 * 继承此类来创建自己的测试窗口。
 *
 * <pre>
 * public class MyTestWnd extends TestWindow {
 *     public MyTestWnd() {
 *         super("我的测试");
 *         // 添加内容...
 *     }
 * }
 * </pre>
 */
public abstract class TestWindow extends Window {

    public static final int WIDTH = 140;
    public static final int MARGIN = 4;
    public static final int GAP = 2;

    protected float y;
    protected int colWidth;

    public TestWindow() {
        this(null);
    }

    public TestWindow(String title) {
        super();
        colWidth = WIDTH - MARGIN * 2;
        y = MARGIN;

        if (title != null && !title.isEmpty()) {
            RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 9);
            titleBlock.hardlight(TITLE_COLOR);
            titleBlock.maxWidth(colWidth);
            titleBlock.setPos((WIDTH - titleBlock.width()) / 2f, y);
            add(titleBlock);
            y = titleBlock.bottom() + GAP;
        }

        createBody();

        int h = (int) Math.max(y + MARGIN, 40);
        resize(WIDTH, Math.min(h, PixelScene.uiCamera.height - 20));
    }

    /**
     * 在此方法中添加窗口内容。
     * 可用方法：
     * - line(text)      添加文本行
     * - add(Component)  添加组件
     * - getNextY()      获取下一个元素 Y 坐标
     */
    protected abstract void createBody();

    /**
     * 添加一行文本。
     */
    protected void line(String text) {
        RenderedTextBlock block = PixelScene.renderTextBlock(text, 6);
        block.maxWidth(colWidth);
        block.setPos(MARGIN, y);
        add(block);
        y = block.bottom() + GAP;
    }

    /**
     * 添加组件到内容区，自动对齐到当前 Y 坐标。
     */
    protected void addContent(Component c) {
        c.setPos(MARGIN, y);
        add(c);
        y = c.bottom() + GAP;
    }

    /**
     * 获取下一个元素的 Y 坐标（不含间距）。
     */
    protected float getNextY() {
        return y;
    }

    /**
     * 获取内容区宽度。
     */
    protected int getColWidth() {
        return colWidth;
    }
}
