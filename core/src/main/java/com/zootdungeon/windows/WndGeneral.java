package com.zootdungeon.windows;

import com.zootdungeon.Assets;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.Button;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.ScrollPane;
import com.zootdungeon.ui.Window;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <!-- TOC: Column segments + {@link Builder#hrow} | multi-tab {@link Builder#tab} -->
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
 *
 * // 多标签：仅使用 {@link #tab} 填入的页面；根级 {@link #line} 等在存在 tab 时忽略
 * WndGeneral.make()
 *     .title("多页")
 *     .tab("甲", p -> p.line("第一页"))
 *     .tab("乙", p -> p.option("动作", () -> {}))
 *     .show();
 *
 * // 横向一排（默认自上而下为 Column；{@link Builder#hrow} / {@link PaneBuilder#hrow} 插入一行多列）
 * WndGeneral.make()
 *     .line("上面独占一行")
 *     .hrow(r -> r.line("左").button("右", () -> {}))
 *     .show();
 *
 * // 开关（不自动关窗；{@link PaneBuilder#switchRow} / {@link Builder#switchRow}）
 * WndGeneral.make()
 *     .title("选项")
 *     .switchRow("调试绘制", false, on -> applyDebugDraw(on))
 *     .show();
 * </pre>
 */
public class WndGeneral extends Window {

	/** 有滚动列表时非空；需在加入窗口后再 {@link ScrollPane#setRect}，否则 {@link ScrollPane#layout} 会因无 parent/camera 提前返回，子相机保持 1×1，列表不显示 */
	public ScrollPane scrollPane;

	public static final int WIDTH = 140;
	public static final int MARGIN = 4;
	public static final int GAP = 2;
	public static final int BTN_HEIGHT = 18;
	public static final int TAB_BTN_H = 14;
	/** 单行排 tab 时每个按钮的目标最小宽度；再窄则改为两行均分 */
	public static final int TAB_BTN_MIN_W = 26;
	/** 内容超过此高度时启用滚动，且不超过屏幕 */
	public static final int MAX_CONTENT_HEIGHT = 180;

	/** 实际窗口内容宽度，不超过屏幕；用于横向滚动与居中 */
	public int layoutWidth = WIDTH;

	public boolean tabbedMode;
	public RenderedTextBlock tabbedTitleBlock;
	public ArrayList<RedButton> tabButtons;
	public ArrayList<TabSpec> tabSpecs;
	public int currentTab;

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

	public static int computeLayoutWidth() {
		Camera cam = PixelScene.uiCamera.visible ? PixelScene.uiCamera : Camera.main;
		int screenW = (int) cam.width;
		int edge = 16;
		return Math.max(80, Math.min(WIDTH, screenW - edge));
	}

	public static int computeMaxWindowHeight() {
		Camera cam = PixelScene.uiCamera.visible ? PixelScene.uiCamera : Camera.main;
		return Math.max(80, (int) cam.height - 20);
	}

	private void applyWindowBounds() {
		boundOffsetWithMargin(3);
	}

	private WndGeneral(Builder b) {
		super();
		if (!b.tabs.isEmpty()) {
			initTabbed(b);
		} else {
			initSinglePane(b);
		}
	}

	private void initSinglePane(Builder b) {
		tabbedMode = false;
		layoutWidth = computeLayoutWidth();
		float y = MARGIN;
		int maxWindowH = computeMaxWindowHeight();

		if (b.title != null && !b.title.isEmpty()) {
			RenderedTextBlock titleBlock = PixelScene.renderTextBlock(b.title, 9);
			titleBlock.hardlight(TITLE_COLOR);
			titleBlock.maxWidth(layoutWidth - MARGIN * 2);
			titleBlock.setPos((layoutWidth - titleBlock.width()) / 2f, y);
			add(titleBlock);
			y = titleBlock.bottom() + GAP;
		}

		Component body = buildBodyComponent(b.segments, b.buttonText, b.onButton, layoutWidth);
		float bodyY = body.height();
		float bodyW = body.width();

		int availableForBody = maxWindowH - (int) y - MARGIN;
		availableForBody = Math.max(40, availableForBody);

		boolean needVScroll = bodyY > availableForBody || bodyY > MAX_CONTENT_HEIGHT;
		boolean needHScroll = bodyW > layoutWidth + 0.5f;
		boolean needScroll = (needVScroll || needHScroll) && bodyY > 0;

		if (needScroll) {
			int scrollH;
			if (needVScroll) {
				scrollH = (int) Math.min(bodyY, Math.max(40, Math.min(MAX_CONTENT_HEIGHT, availableForBody)));
			} else {
				scrollH = (int) Math.min(Math.max(bodyY, 40), availableForBody);
			}
			scrollPane = new InteractiveScrollPane(body);
			add(scrollPane);
			scrollPane.setRect(0, (int) y, layoutWidth, scrollH);
			y += scrollPane.height() + MARGIN;
		} else {
			add(body);
			body.setPos(0, y);
			y = body.bottom() + MARGIN;
		}

		int finalH = (int) y;
		finalH = Math.min(finalH, maxWindowH);
		resize(layoutWidth, finalH);
		applyWindowBounds();
	}

	private void initTabbed(Builder b) {
		tabbedMode = true;
		layoutWidth = computeLayoutWidth();
		tabSpecs = new ArrayList<>(b.tabs);
		tabButtons = new ArrayList<>();
		float y = MARGIN;

		if (b.title != null && !b.title.isEmpty()) {
			tabbedTitleBlock = PixelScene.renderTextBlock(b.title, 9);
			tabbedTitleBlock.hardlight(TITLE_COLOR);
			tabbedTitleBlock.maxWidth(layoutWidth - MARGIN * 2);
			tabbedTitleBlock.setPos((layoutWidth - tabbedTitleBlock.width()) / 2f, y);
			add(tabbedTitleBlock);
			y = tabbedTitleBlock.bottom() + GAP;
		}

		int n = tabSpecs.size();
		for (int i = 0; i < n; i++) {
			final int idx = i;
			RedButton btn = new RedButton(tabSpecs.get(i).label, 7) {
				@Override
				protected void onClick() {
					switchTab(idx);
				}
			};
			add(btn);
			tabButtons.add(btn);
		}

		switchTab(0);
	}

	/** 在单行 y 上排 tabButtons[from..to)，等分宽度；返回该行底边 y（不含行间距） */
	private float layoutTabButtonRow(int from, int to, float rowY) {
		int count = to - from;
		if (count <= 0) {
			return rowY;
		}
		float x = 0;
		for (int i = from; i < to; i++) {
			RedButton btn = tabButtons.get(i);
			int idxInRow = i - from;
			float w = (idxInRow == count - 1)
					? layoutWidth - x
					: (float) Math.floor((layoutWidth - (count - 1) * GAP) / (float) count);
			btn.setRect(x, rowY, w, TAB_BTN_H);
			x = btn.right() + GAP;
		}
		return rowY + TAB_BTN_H;
	}

	private void relayoutTabbed() {
		if (!tabbedMode || tabButtons == null || tabSpecs == null) {
			return;
		}
		layoutWidth = computeLayoutWidth();
		float y = MARGIN;
		int maxWindowH = computeMaxWindowHeight();

		if (tabbedTitleBlock != null) {
			tabbedTitleBlock.setPos((layoutWidth - tabbedTitleBlock.width()) / 2f, y);
			y = tabbedTitleBlock.bottom() + GAP;
		}

		int n = tabButtons.size();
		if (n > 0) {
			int maxFitOneRow = Math.max(1, (layoutWidth + GAP) / (TAB_BTN_MIN_W + GAP));
			boolean twoRows = n > maxFitOneRow;
			if (!twoRows) {
				float x = 0;
				for (int i = 0; i < n; i++) {
					RedButton btn = tabButtons.get(i);
					float w = (i == n - 1)
							? layoutWidth - x
							: (float) Math.floor((layoutWidth - (n - 1) * GAP) / (float) n);
					btn.setRect(x, y, w, TAB_BTN_H);
					x = btn.right() + GAP;
				}
				y = tabButtons.get(0).bottom() + GAP;
			} else {
				int row0 = (n + 1) / 2;
				y = layoutTabButtonRow(0, row0, y);
				y += GAP;
				y = layoutTabButtonRow(row0, n, y);
				y += GAP;
			}
		}

		if (scrollPane != null) {
			Component body = scrollPane.content();
			float bodyY = body.height();
			float bodyW = body.width();

			int availableForBody = maxWindowH - (int) y - MARGIN;
			availableForBody = Math.max(40, availableForBody);

			boolean needVScroll = bodyY > availableForBody || bodyY > MAX_CONTENT_HEIGHT;
			boolean needHScroll = bodyW > layoutWidth + 0.5f;

			int scrollH;
			if ((needVScroll || needHScroll) && bodyY > 0) {
				if (needVScroll) {
					scrollH = (int) Math.min(bodyY, Math.max(40, Math.min(MAX_CONTENT_HEIGHT, availableForBody)));
				} else {
					scrollH = (int) Math.min(Math.max(bodyY, 40), availableForBody);
				}
			} else {
				int h = bodyY > 0 ? (int) bodyY : 40;
				h = Math.min(h, availableForBody);
				scrollH = Math.max(h, 1);
			}
			scrollPane.setRect(0, (int) y, layoutWidth, scrollH);
			y += scrollPane.height() + MARGIN;
		}

		int finalH = (int) y;
		finalH = Math.min(finalH, maxWindowH);
		resize(layoutWidth, finalH);
	}

	private void switchTab(int index) {
		if (!tabbedMode || tabSpecs == null || index < 0 || index >= tabSpecs.size()) {
			return;
		}
		currentTab = index;
		if (scrollPane != null) {
			scrollPane.destroy();
			remove(scrollPane);
			scrollPane = null;
		}

		PaneBuilder pane = tabSpecs.get(index).pane;
		Component body = buildBodyComponent(pane.segments, pane.buttonText, pane.onButton, layoutWidth);
		scrollPane = new InteractiveScrollPane(body);
		scrollPane.scrollTo(0, 0);
		add(scrollPane);

		for (int i = 0; i < tabButtons.size(); i++) {
			tabButtons.get(i).enable(i != currentTab);
		}

		relayoutTabbed();
		applyWindowBounds();
	}

	private Component buildBodyComponent(
			List<PaneSegment> segments,
			String buttonText,
			Runnable onButton,
			int layoutW) {
		Component body = new Component();
		float bodyY = 0;
		int colW = Math.max(1, layoutW - MARGIN * 2);
		float maxRight = MARGIN;

		for (PaneSegment seg : segments) {
			if (seg instanceof TextLineSeg) {
				String text = ((TextLineSeg) seg).text;
				RenderedTextBlock block = PixelScene.renderTextBlock(text, 6);
				block.maxWidth(colW);
				block.setPos(MARGIN, bodyY);
				body.add(block);
				bodyY = block.bottom() + GAP;
				maxRight = Math.max(maxRight, block.right());
			} else if (seg instanceof SwitchSeg) {
				SwitchSeg ss = (SwitchSeg) seg;
				SwitchRow sw = new SwitchRow(ss.label, ss.initialOn, ss.onChange);
				sw.setRect(MARGIN, bodyY, colW, BTN_HEIGHT);
				body.add(sw);
				bodyY = sw.bottom() + GAP;
				maxRight = Math.max(maxRight, sw.right());
			} else if (seg instanceof OptionSeg) {
				Option opt = ((OptionSeg) seg).option;
				RedButton btn = new RedButton(opt.label) {
					@Override
					protected void onClick() {
						if (opt.onClick != null) opt.onClick.run();
						hide();
					}
				};
				btn.setRect(MARGIN, bodyY, colW, BTN_HEIGHT);
				body.add(btn);
				bodyY = btn.bottom() + GAP;
				maxRight = Math.max(maxRight, btn.right());
			} else if (seg instanceof HRowSeg) {
				List<HCell> cells = ((HRowSeg) seg).cells;
				int n = cells.size();
				if (n <= 0) {
					continue;
				}
				float innerLeft = MARGIN;
				float innerRight = layoutW - MARGIN;
				float innerW = innerRight - innerLeft;
				float totalGap = (n - 1) * GAP;
				float cellW = (innerW - totalGap) / (float) n;
				float x = innerLeft;
				float rowTop = bodyY;
				float maxBottom = rowTop;
				for (int i = 0; i < n; i++) {
					final HCell c = cells.get(i);
					float w = (i == n - 1) ? (innerRight - x) : (float) Math.floor(cellW);
					if (c.button) {
						final Runnable r = c.onClick;
						RedButton btn = new RedButton(c.text) {
							@Override
							protected void onClick() {
								if (r != null) r.run();
								hide();
							}
						};
						btn.setRect(x, rowTop, w, BTN_HEIGHT);
						body.add(btn);
						maxBottom = Math.max(maxBottom, btn.bottom());
						maxRight = Math.max(maxRight, btn.right());
					} else {
						RenderedTextBlock block = PixelScene.renderTextBlock(c.text, 6);
						block.maxWidth((int) w);
						block.setPos(x, rowTop);
						body.add(block);
						maxBottom = Math.max(maxBottom, block.bottom());
						maxRight = Math.max(maxRight, block.right());
					}
					x += w + GAP;
				}
				bodyY = maxBottom + GAP;
			}
		}

		if (buttonText != null) {
			RedButton btn = new RedButton(buttonText) {
				@Override
				protected void onClick() {
					if (onButton != null) onButton.run();
					hide();
				}
			};
			btn.setRect(MARGIN, bodyY, colW, BTN_HEIGHT);
			body.add(btn);
			bodyY = btn.bottom() + MARGIN;
			maxRight = Math.max(maxRight, btn.right());
		} else if (bodyY > 0) {
			bodyY += MARGIN;
		}

		int bodyW = Math.max(layoutW, (int) Math.ceil(maxRight));
		body.setSize(bodyW, (int) bodyY);
		return body;
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (tabbedMode) {
			relayoutTabbed();
		} else if (scrollPane != null) {
			scrollPane.setPos(scrollPane.left(), scrollPane.top());
		}
	}

	/**
	 * 默认 {@link ScrollPane} 的滚动热区会吞掉指针事件，列表里的 {@link RedButton} 无法点击。
	 * 与 {@link com.zootdungeon.arknights.RhodesIslandTerminal} 中做法一致：{@code NEVER_BLOCK} 让事件穿透到子控件，仍可通过拖拽/滚轮滚动。
	 */
	private static class InteractiveScrollPane extends ScrollPane {
		InteractiveScrollPane(Component content) {
			super(content);
			if (controller != null) {
				controller.blockLevel = PointerArea.NEVER_BLOCK;
			}
		}
	}

	/** 仅用于本窗口的开关行：左侧标签，右侧滑轨 + 滑块。 */
	private static class SwitchRow extends Button {

		private static final int TRACK_W = 28;
		private static final int TRACK_H = 10;
		private static final int THUMB = 8;

		private final RenderedTextBlock label;
		private final ColorBlock track;
		private final ColorBlock thumb;
		private final Consumer<Boolean> onChange;
		private boolean on;

		SwitchRow(String labelText, boolean initialOn, Consumer<Boolean> onChange) {
			this.on = initialOn;
			this.onChange = onChange;
			label = PixelScene.renderTextBlock(labelText != null ? labelText : "", 6);
			add(label);
			track = new ColorBlock(1, 1, 0xFF3a3a3a);
			add(track);
			thumb = new ColorBlock(THUMB, THUMB, 0xFFc8c8c8);
			add(thumb);
		}

		void setOn(boolean value) {
			if (on != value) {
				on = value;
				layout();
				if (onChange != null) {
					onChange.accept(on);
				}
			}
		}

		@Override
		protected void layout() {
			super.layout();

			float h = height;
			label.maxWidth(Math.max(1, (int) (width - TRACK_W - 6)));
			label.setPos(x, y + (h - label.height()) / 2f);
			PixelScene.align(label);

			float trackX = x + width - TRACK_W - 1;
			float trackY = y + (h - TRACK_H) / 2f;
			track.size(TRACK_W, TRACK_H);
			track.x = trackX;
			track.y = trackY;

			float thumbY = y + (h - THUMB) / 2f;
			float thumbX = on ? (trackX + TRACK_W - THUMB - 1) : (trackX + 1);
			thumb.x = thumbX;
			thumb.y = thumbY;

			track.resetColor();
			thumb.resetColor();
			if (on) {
				track.hardlight(0.42f, 0.72f, 0.46f);
			}
		}

		@Override
		protected void onClick() {
			Sample.INSTANCE.play(Assets.Sounds.CLICK);
			setOn(!on);
		}
	}

	public interface PaneSegment {
	}

	public static class TextLineSeg implements PaneSegment {
		public final String text;

		public TextLineSeg(String text) {
			this.text = text;
		}
	}

	public static class OptionSeg implements PaneSegment {
		public final Option option;

		public OptionSeg(Option option) {
			this.option = option;
		}
	}

	public static class SwitchSeg implements PaneSegment {
		public final String label;
		public final boolean initialOn;
		public final Consumer<Boolean> onChange;

		public SwitchSeg(String label, boolean initialOn, Consumer<Boolean> onChange) {
			this.label = label;
			this.initialOn = initialOn;
			this.onChange = onChange;
		}
	}

	public static class HRowSeg implements PaneSegment {
		public final List<HCell> cells;

		public HRowSeg(List<HCell> cells) {
			this.cells = new ArrayList<>(cells);
		}
	}

	public static class HCell {
		public final boolean button;
		public final String text;
		public final Runnable onClick;

		public HCell(boolean button, String text, Runnable onClick) {
			this.button = button;
			this.text = text;
			this.onClick = onClick;
		}

		public static HCell text(String t) {
			return new HCell(false, t != null ? t : "", null);
		}

		public static HCell button(String label, Runnable onClick) {
			return new HCell(true, label, onClick);
		}
	}

	private static class Option {
		final String label;
		final Runnable onClick;
		Option(String label, Runnable onClick) { this.label = label; this.onClick = onClick; }
	}

	public static class TabSpec {
		public final String label;
		public final PaneBuilder pane;

		public TabSpec(String label, PaneBuilder pane) {
			this.label = label;
			this.pane = pane;
		}
	}

	public static class PaneBuilder {
		public final List<PaneSegment> segments = new ArrayList<>();
		public String buttonText;
		public Runnable onButton;

		public PaneBuilder row(String label, Object value) {
			segments.add(new TextLineSeg(label + ": " + (value != null ? value : "null")));
			return this;
		}

		public PaneBuilder line(String text) {
			segments.add(new TextLineSeg(text != null ? text : ""));
			return this;
		}

		public PaneBuilder option(String label, Runnable onClick) {
			segments.add(new OptionSeg(new Option(label, onClick)));
			return this;
		}

		/** 开关行：点击切换，不关闭窗口；{@code onChange} 可为 null */
		public PaneBuilder switchRow(String label, boolean initialOn, Consumer<Boolean> onChange) {
			segments.add(new SwitchSeg(label, initialOn, onChange));
			return this;
		}

		public PaneBuilder hrow(Consumer<HRowBuilder> fill) {
			HRowBuilder hb = new HRowBuilder();
			fill.accept(hb);
			segments.add(new HRowSeg(hb.cells));
			return this;
		}

		public PaneBuilder button(String text, Runnable onClick) {
			this.buttonText = text;
			this.onButton = onClick;
			return this;
		}
	}

	public static class HRowBuilder {
		public final ArrayList<HCell> cells = new ArrayList<>();

		public HRowBuilder line(String text) {
			cells.add(HCell.text(text));
			return this;
		}

		public HRowBuilder button(String label, Runnable onClick) {
			cells.add(HCell.button(label, onClick));
			return this;
		}
	}

	public static class Builder {
		public String title;
		public final List<PaneSegment> segments = new ArrayList<>();
		public String buttonText;
		public Runnable onButton;
		public final List<TabSpec> tabs = new ArrayList<>();

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		/** 添加一行：标签 + 值，显示为 "label: value" */
		public Builder row(String label, Object value) {
			segments.add(new TextLineSeg(label + ": " + (value != null ? value : "null")));
			return this;
		}

		/** 添加一行纯文本 */
		public Builder line(String text) {
			segments.add(new TextLineSeg(text != null ? text : ""));
			return this;
		}

		/** 添加可选项，点击后执行 callback 并关闭窗口 */
		public Builder option(String label, Runnable onClick) {
			segments.add(new OptionSeg(new Option(label, onClick)));
			return this;
		}

		/** 开关行：点击切换，不关闭窗口；{@code onChange} 可为 null */
		public Builder switchRow(String label, boolean initialOn, Consumer<Boolean> onChange) {
			segments.add(new SwitchSeg(label, initialOn, onChange));
			return this;
		}

		/** 横向一排单元格（与 {@link #line} 等穿插时仍按顺序向下排，整体为 Column） */
		public Builder hrow(Consumer<HRowBuilder> fill) {
			HRowBuilder hb = new HRowBuilder();
			fill.accept(hb);
			segments.add(new HRowSeg(hb.cells));
			return this;
		}

		/** 添加底部按钮，点击后执行 callback 并关闭窗口 */
		public Builder button(String text, Runnable onClick) {
			this.buttonText = text;
			this.onButton = onClick;
			return this;
		}

		/**
		 * 增加一个标签页；与 {@link PaneBuilder} 描述该页内容。若调用了本方法，构建时只展示各 tab，根级 {@link #line} 等不生效。
		 */
		public Builder tab(String label, Consumer<PaneBuilder> fill) {
			PaneBuilder p = new PaneBuilder();
			fill.accept(p);
			tabs.add(new TabSpec(label, p));
			return this;
		}

		public void show() {
			GameScene.show(new WndGeneral(this));
		}
	}
}
