package com.zootdungeon.windows;

import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.ScrollPane;
import com.zootdungeon.ui.Window;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用表单窗口，便于开发时快速展示结构化信息。
 * <p>
 * 用法示例：
 * <pre>
 * // 简单：标题 + 多行文本
 * WndGeneral.show("调试", "深度: " + Dungeon.depth, "COST: " + Dungeon.cost);
 *
 * // 构建器：标签-值行 + 按钮
 * WndGeneral.make()
 *     .title("状态")
 *     .row("深度", Dungeon.depth)
 *     .row("分支", Dungeon.branch)
 *     .button("确定", () -> {})
 *     .show();
 *
 * // 可选项列表（支持滚动）
 * WndGeneral.make()
 *     .title("选择分类")
 *     .option("药水卷轴", () -> {})
 *     .option("石头", () -> {})
 *     .show();
 * </pre>
 */
public class WndGeneral extends Window {

	private static final int WIDTH = 140;
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final int BTN_HEIGHT = 18;
	/** 内容超过此高度时启用滚动，且不超过屏幕 */
	private static final int MAX_CONTENT_HEIGHT = 180;

	/** 简单展示：标题 + 多行文本，每行一个字符串 */
	public static void show(String title, String... lines) {
		Builder b = make().title(title);
		for (String line : lines) {
			b.line(line);
		}
		b.show();
	}

	public static Builder make() {
		return new Builder();
	}

	private WndGeneral(Builder b) {
		super();
		float y = MARGIN;

		if (b.title != null && !b.title.isEmpty()) {
			RenderedTextBlock titleBlock = PixelScene.renderTextBlock(b.title, 9);
			titleBlock.hardlight(TITLE_COLOR);
			titleBlock.maxWidth(WIDTH - MARGIN * 2);
			titleBlock.setPos(MARGIN, y);
			add(titleBlock);
			y = titleBlock.bottom() + GAP;
		}

		Component body = new Component();
		float bodyY = 0;

		for (Row row : b.rows) {
			RenderedTextBlock block = PixelScene.renderTextBlock(row.text, 6);
			block.maxWidth(WIDTH - MARGIN * 2);
			block.setPos(MARGIN, bodyY);
			body.add(block);
			bodyY = block.bottom() + GAP;
		}

		for (Option opt : b.options) {
			RedButton btn = new RedButton(opt.label) {
				@Override
				protected void onClick() {
					if (opt.onClick != null) opt.onClick.run();
					hide();
				}
			};
			btn.setRect(MARGIN, bodyY, WIDTH - MARGIN * 2, BTN_HEIGHT);
			body.add(btn);
			bodyY = btn.bottom() + GAP;
		}

		if (b.buttonText != null) {
			RedButton btn = new RedButton(b.buttonText) {
				@Override
				protected void onClick() {
					if (b.onButton != null) b.onButton.run();
					hide();
				}
			};
			btn.setRect(MARGIN, bodyY, WIDTH - MARGIN * 2, BTN_HEIGHT);
			body.add(btn);
			bodyY = btn.bottom() + MARGIN;
		} else if (bodyY > 0) {
			bodyY += MARGIN;
		}

		body.setSize(WIDTH, (int) bodyY);

		if (bodyY > MAX_CONTENT_HEIGHT) {
			int scrollH = Math.min((int) bodyY, MAX_CONTENT_HEIGHT);
			int maxScrollH = Math.max(100, (int) (PixelScene.uiCamera.height - 80));
			if (scrollH > maxScrollH) scrollH = maxScrollH;
			ScrollPane scroll = new ScrollPane(body);
			scroll.setRect(0, (int) y, WIDTH, scrollH);
			add(scroll);
			y += scroll.height() + MARGIN;
		} else {
			body.setPos(0, y);
			add(body);
			y = body.bottom() + MARGIN;
		}

		int finalH = (int) y;
		if (finalH > PixelScene.uiCamera.height - 20) {
			finalH = PixelScene.uiCamera.height - 20;
		}
		resize(WIDTH, finalH);
	}

	private static class Row {
		final String text;
		Row(String text) { this.text = text; }
	}

	private static class Option {
		final String label;
		final Runnable onClick;
		Option(String label, Runnable onClick) { this.label = label; this.onClick = onClick; }
	}

	public static class Builder {
		private String title;
		private final List<Row> rows = new ArrayList<>();
		private final List<Option> options = new ArrayList<>();
		private String buttonText;
		private Runnable onButton;

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		/** 添加一行：标签 + 值，显示为 "label: value" */
		public Builder row(String label, Object value) {
			rows.add(new Row(label + ": " + (value != null ? value : "null")));
			return this;
		}

		/** 添加一行纯文本 */
		public Builder line(String text) {
			rows.add(new Row(text != null ? text : ""));
			return this;
		}

		/** 添加可选项，点击后执行 callback 并关闭窗口 */
		public Builder option(String label, Runnable onClick) {
			options.add(new Option(label, onClick));
			return this;
		}

		/** 添加底部按钮，点击后执行 callback 并关闭窗口 */
		public Builder button(String text, Runnable onClick) {
			this.buttonText = text;
			this.onButton = onClick;
			return this;
		}

		public void show() {
			GameScene.show(new WndGeneral(this));
		}
	}
}
