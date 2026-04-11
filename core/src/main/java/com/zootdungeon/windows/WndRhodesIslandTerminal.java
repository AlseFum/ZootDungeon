package com.zootdungeon.windows;

import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Belongings;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.ScrollPane;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.PointerArea;

public class WndRhodesIslandTerminal extends Window {

	/** 横屏或宽窗口下的首选宽度；竖屏时改为贴齐可用宽度 */
	private static final int WIDTH_MAX = 176;
	/** 最窄宽度：需容纳被动行「名称 + 信息 + 数值」三列 */
	private static final int LAYOUT_MIN_W = 104;
	/** 窗口整体相对 UI 视口边缘至少留白（与 {@link #boundOffsetWithMargin} 一致） */
	private static final int SCREEN_EDGE_MARGIN = 24;
	private static final int GAP = 2;
	private static final int MARGIN = 3;
	private static final int BTN_HEIGHT = 16;
	private static final int TAB_BTN_H = 14;
	private static final int MIN_VIEW_H = 96;

	private static final int BTN_INFO_W = 26;
	private static final int BTN_ACTIVATE_W = 36;
	private static final int VALUE_W = 32;
	private static final int BTN_UNINSTALL_W = 32;
	private static final int BTN_TOGGLE_W = 32;
	private static final int BTN_EXIT_W = 34;
	private static final int INFO_FONT = 6;
	private static final int INFO_MAX_H = 32;
	private static final int CUSTOM_COMPONENT_H = 56;
	private static final int EMPTY_TEXT_H = 14;

	private static WndRhodesIslandTerminal openInstance;

	/** 当前布局宽度（随 {@link PixelScene#uiCamera} 在竖屏下变窄） */
	private int layoutWidth = WIDTH_MAX;

	private final RhodesIslandTerminal terminal;
	private IconTitle title;
	private RedButton tabActiveBtn;
	private RedButton tabPassiveBtn;
	private RedButton tabPluginBtn;
	private RedButton btnExit;
	private ScrollPane contentPane;
	private TabType currentTab = TabType.ACTIVE;

	private enum TabType {
		ACTIVE, PASSIVE, PLUGINS
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		// GameScene.show() 可能会给新窗口继承 offset；带 ScrollPane 的窗口需要在 offset 后刷新布局
		relayout();
	}

	@Override
	public void boundOffsetWithMargin(int margin) {
		super.boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
	}

	public WndRhodesIslandTerminal(RhodesIslandTerminal terminal) {
		this.terminal = terminal;
		if (openInstance != null && openInstance.parent != null) {
			openInstance.hide();
		}
		openInstance = this;
		buildWindow();
	}

	private void buildWindow() {
		title = new IconTitle(new ItemSprite(terminal), Messages.get(RhodesIslandTerminal.class, "name"));
		add(title);

		tabActiveBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "tab_active"), 7) {
			@Override
			protected void onClick() {
				switchTab(TabType.ACTIVE);
			}
		};
		add(tabActiveBtn);

		tabPassiveBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "tab_passive"), 7) {
			@Override
			protected void onClick() {
				switchTab(TabType.PASSIVE);
			}
		};
		add(tabPassiveBtn);

		tabPluginBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "tab_plugins"), 7) {
			@Override
			protected void onClick() {
				switchTab(TabType.PLUGINS);
			}
		};
		add(tabPluginBtn);

		btnExit = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_exit"), 7) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		add(btnExit);

		syncLayoutWidth();
		relayout();
		switchTab(TabType.ACTIVE);
	}

	/** 按当前 UI 相机尺寸更新窗口宽度（竖屏窄宽时用满宽度，避免固定 176 溢出） */
	private int maxInnerWidth() {
		return Math.max(LAYOUT_MIN_W,
				(int) PixelScene.uiCamera.width - 2 * SCREEN_EDGE_MARGIN - chrome.marginHor());
	}

	private int maxInnerHeight() {
		return (int) PixelScene.uiCamera.height - 2 * SCREEN_EDGE_MARGIN - chrome.marginVer();
	}

	private void syncLayoutWidth() {
		int screenW = (int) PixelScene.uiCamera.width;
		int screenH = (int) PixelScene.uiCamera.height;
		int maxByScreen = maxInnerWidth();
		boolean portrait = screenH > screenW;
		if (portrait) {
			layoutWidth = maxByScreen;
		} else {
			layoutWidth = Math.min(WIDTH_MAX, maxByScreen);
		}
		layoutWidth = Math.max(LAYOUT_MIN_W, Math.min(layoutWidth, maxByScreen));
	}

	private void relayout() {
		syncLayoutWidth();
		float y = MARGIN;
		float titleW = layoutWidth - BTN_EXIT_W - GAP;
		title.setRect(0, y, titleW, 0);
		btnExit.setRect(titleW + GAP, y, BTN_EXIT_W, TAB_BTN_H);
		y = Math.max(title.bottom(), btnExit.bottom()) + GAP;

		if (layoutWidth >= 155) {
			float firstW = (float) Math.floor(layoutWidth * 0.333f) - GAP;
			float secondW = (float) Math.floor(layoutWidth * 0.333f) - GAP;
			float thirdW = layoutWidth - firstW - secondW - GAP * 2;
			tabActiveBtn.setRect(0, y, firstW, TAB_BTN_H);
			tabPassiveBtn.setRect(tabActiveBtn.right() + GAP, y, secondW, TAB_BTN_H);
			tabPluginBtn.setRect(tabPassiveBtn.right() + GAP, y, thirdW, TAB_BTN_H);
			y = tabActiveBtn.bottom() + GAP;
		} else {
			float half = (layoutWidth - GAP) / 2f;
			tabActiveBtn.setRect(0, y, half, TAB_BTN_H);
			tabPassiveBtn.setRect(tabActiveBtn.right() + GAP, y, layoutWidth - half - GAP, TAB_BTN_H);
			y = tabActiveBtn.bottom() + GAP;
			tabPluginBtn.setRect(0, y, layoutWidth, TAB_BTN_H);
			y = tabPluginBtn.bottom() + GAP;
		}

		int viewH = getAvailableViewHeight(y);
		if (contentPane != null) {
			// clamp window height to screen, then clamp content height accordingly
			int maxWindowHeight = maxInnerHeight();
			int finalH = (int) (y + viewH + MARGIN);
			if (finalH > maxWindowHeight) {
				finalH = maxWindowHeight;
			}
			viewH = Math.max(0, finalH - (int) y - MARGIN);
			contentPane.setRect(0, y, layoutWidth, viewH);
			resize(layoutWidth, finalH);
			boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
			return;
		}
		int maxWindowHeight = maxInnerHeight();
		int finalH = (int) (y + viewH + MARGIN);
		if (finalH > maxWindowHeight) finalH = maxWindowHeight;
		resize(layoutWidth, finalH);
		boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
	}

	private int getAvailableViewHeight(float contentTop) {
		int maxWindowHeight = maxInnerHeight();
		int bySpace = (int) (maxWindowHeight - contentTop - MARGIN);
		return Math.max(MIN_VIEW_H, bySpace);
	}

	private void switchTab(TabType tab) {
		currentTab = tab;
		if (contentPane != null) {
			contentPane.destroy();
			remove(contentPane);
			contentPane = null;
		}

		syncLayoutWidth();
		Component content = buildTabContent(tab);
		contentPane = new InteractiveScrollPane(content);
		contentPane.scrollTo(0, 0);
		add(contentPane);
		relayout();
		refreshTabButtons();
	}

	private void refreshTabButtons() {
		tabActiveBtn.enable(currentTab != TabType.ACTIVE);
		tabPassiveBtn.enable(currentTab != TabType.PASSIVE);
		tabPluginBtn.enable(currentTab != TabType.PLUGINS);
	}

	public void refreshPlugins() {
		switchTab(currentTab);
	}

	private Component buildTabContent(TabType tab) {
		switch (tab) {
			case PASSIVE:
				return buildPassiveContent();
			case PLUGINS:
				return buildPluginContent();
			case ACTIVE:
			default:
				return buildActiveContent();
		}
	}

	private Component buildActiveContent() {
		Component root = new Component();
		ColumnLayout col = new ColumnLayout(root, 0, 0, layoutWidth, GAP);
		boolean hasActiveEntry = false;

		RenderedTextBlock costLabel = PixelScene.renderTextBlock(
				Messages.get(RhodesIslandTerminal.class, "cost_display", Dungeon.cost,
						RhodesIslandTerminal.effectiveCostCap(Dungeon.hero)), 8);
		costLabel.maxWidth(layoutWidth);
		col.addAutoHeight(costLabel);

		if (Dungeon.hero != null && Dungeon.hero.pointsInTalent(Talent.RESERVED_OP_COST_SURGE) > 0) {
			RedButton surgeBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_cost_surge"), 8) {
				@Override
				protected void onClick() {
					RhodesIslandTerminal.surgeAllCostIntoMagicalGear(Dungeon.hero);
					refreshPlugins();
				}
			};
			col.addFixedHeight(surgeBtn, BTN_HEIGHT);
		}

		for (int i = 0; i < terminal.slotCount(); i++) {
			final int slotIndex = i;
			RhodesIslandTerminal.PluginSlot slot = terminal.getSlot(i);
			if (slot == null || slot.plugin == null) continue;
			for (TerminalPlugin.ActiveSpec spec : slot.plugin.activeSpecs(terminal, slot)) {
				if (spec == null) continue;
				final TerminalPlugin.ActiveSpec active = spec;
				String rowTitle = "[S" + (slotIndex + 1) + "] " + (active.name != null ? active.name : active.id);
				if (active.cost > 0) {
					rowTitle += " (" + Messages.get(RhodesIslandTerminal.class, "cost_use", active.cost) + ")";
				}
				boolean canActivate = slot.enabled && Dungeon.cost >= active.cost && slot.plugin.canActivate(terminal, slot, active);
				hasActiveEntry = true;
				addActionRow(root, col, rowTitle, active.desc != null ? active.desc : "", active.cost, canActivate, () -> {
					if (Dungeon.cost < active.cost) {
						GLog.w(Messages.get(RhodesIslandTerminal.class, "cost_not_enough", active.cost));
						return;
					}
					terminal.activatePluginAction(slotIndex, active.id);
					hide();
				});
			}
		}
		if (!hasActiveEntry) {
			RenderedTextBlock empty = PixelScene.renderTextBlock("No active actions from installed plugins.", 8);
			empty.maxWidth(layoutWidth);
			empty.setRect(0, col.y, layoutWidth, EMPTY_TEXT_H);
			root.add(empty);
			col.y = empty.bottom() + GAP;
		}

		root.setSize(layoutWidth, col.bottom());
		return root;
	}

	private Component buildPassiveContent() {
		Component root = new Component();
		ColumnLayout col = new ColumnLayout(root, 0, 0, layoutWidth, GAP);
		int nameW = layoutWidth - BTN_INFO_W - VALUE_W - GAP * 2;

		for (RhodesIslandTerminal.PluginSlot slot : terminal.slots) {
			if (slot == null || slot.plugin == null) continue;
			for (TerminalPlugin.PassiveEntry entry : slot.plugin.passiveEntries(terminal, slot)) {
				if (entry == null || !entry.available) continue;
				float rowY = col.y;
				String name = entry.name != null ? entry.name : "";
				String desc = entry.desc != null ? entry.desc : "";
				String value = entry.valueText != null ? entry.valueText : "";

				RenderedTextBlock nameBlock = PixelScene.renderTextBlock(name, 8);
				nameBlock.maxWidth(nameW);
				nameBlock.setRect(0, rowY, nameW, 12);
				root.add(nameBlock);

				RedButton infoBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_info"), 7) {
					@Override
					protected void onClick() {
						GameScene.show(new WndMessage(desc));
					}
				};
				infoBtn.setRect(layoutWidth - BTN_INFO_W - VALUE_W - GAP, rowY, BTN_INFO_W, BTN_HEIGHT);
				root.add(infoBtn);

				RenderedTextBlock valueBlock = PixelScene.renderTextBlock(value, 8);
				valueBlock.setRect(layoutWidth - VALUE_W, rowY + 4, VALUE_W, 12);
				root.add(valueBlock);

				col.y = Math.max(nameBlock.bottom(), infoBtn.bottom()) + GAP;
			}
		}

		if (col.y <= 0.001f) {
			RenderedTextBlock empty = PixelScene.renderTextBlock(Messages.get(RhodesIslandTerminal.class, "passive_empty"), 8);
			empty.maxWidth(layoutWidth);
			col.addAutoHeight(empty);
		}

		root.setSize(layoutWidth, col.bottom());
		return root;
	}

	private Component buildPluginContent() {
		Component root = new Component();
		ColumnLayout col = new ColumnLayout(root, 0, 0, layoutWidth, GAP);
		Hero hero = Dungeon.hero;
		boolean hasPlugin = false;

		RenderedTextBlock pluginsTitle = PixelScene.renderTextBlock(Messages.get(RhodesIslandTerminal.class, "plugins_title"), 8);
		pluginsTitle.maxWidth(layoutWidth);
		col.addAutoHeight(pluginsTitle);
		RenderedTextBlock slotsInfo = PixelScene.renderTextBlock("Slots: " + terminal.slots.size() + "/" + terminal.maxPlugins, 7);
		slotsInfo.maxWidth(layoutWidth);
		col.addAutoHeight(slotsInfo);

		int rightButtons = BTN_UNINSTALL_W + GAP + BTN_TOGGLE_W;
		int nameW = layoutWidth - rightButtons - GAP;

		for (int i = 0; i < terminal.slots.size(); i++) {
			final int index = i;
			RhodesIslandTerminal.PluginSlot slot = terminal.slots.get(i);
			if (slot == null || slot.plugin == null) continue;
			TerminalPlugin plugin = slot.plugin;
			hasPlugin = true;

			float rowY = col.y;
			String pluginName = plugin.pluginName(terminal, slot);
			if (pluginName == null || pluginName.isEmpty()) pluginName = plugin.name();
			if (!slot.enabled) pluginName += " [" + Messages.get(RhodesIslandTerminal.class, "slot_off") + "]";

			RenderedTextBlock nameBlock = PixelScene.renderTextBlock(pluginName, 8);
			nameBlock.maxWidth(nameW);
			nameBlock.setRect(0, rowY + 2, nameW, 12);
			root.add(nameBlock);

			FlowLayout buttons = new FlowLayout(layoutWidth - rightButtons, rowY, rightButtons + GAP, GAP);
			RedButton toggle = new RedButton(slot.enabled
					? Messages.get(RhodesIslandTerminal.class, "btn_disable")
					: Messages.get(RhodesIslandTerminal.class, "btn_enable")) {
				@Override
				protected void onClick() {
					if (slot.enabled) terminal.disableSlot(index);
					else terminal.enableSlot(index);
					refreshPlugins();
				}
			};
			buttons.add(toggle, BTN_TOGGLE_W, BTN_HEIGHT);

			RedButton uninstall = new RedButton(Messages.get(RhodesIslandTerminal.class, "uninstall")) {
				@Override
				protected void onClick() {
					terminal.uninstallPlugin(index, hero);
					refreshPlugins();
				}
			};
			buttons.add(uninstall, BTN_UNINSTALL_W, BTN_HEIGHT);
			buttons.commit(root);

			col.y = Math.max(nameBlock.bottom(), rowY + BTN_HEIGHT) + GAP;

			Component custom = plugin.createDisplayComponent(terminal);
			if (custom != null) {
				custom.setRect(0, col.y, layoutWidth, CUSTOM_COMPONENT_H);
				root.add(custom);
				col.y = custom.bottom() + GAP;
			} else {
				String infoText = plugin.pluginDesc(terminal, slot);
				if (infoText == null || infoText.isEmpty()) infoText = plugin.desc();
				if (infoText != null && !infoText.isEmpty()) {
					RenderedTextBlock info = PixelScene.renderTextBlock(infoText, INFO_FONT);
					info.maxWidth(layoutWidth);
					info.setRect(0, col.y, layoutWidth, INFO_MAX_H);
					root.add(info);
					col.y = info.bottom() + GAP;
				}
			}
		}
		if (!hasPlugin) {
			RenderedTextBlock empty = PixelScene.renderTextBlock("No plugins installed.", 8);
			empty.maxWidth(layoutWidth);
			empty.setRect(0, col.y, layoutWidth, EMPTY_TEXT_H);
			root.add(empty);
			col.y = empty.bottom() + GAP;
		}

		if (terminal.canInstallMorePlugins()) {
			RedButton installBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "install_plugin")) {
				@Override
				protected void onClick() {
					openPluginSelector();
				}
			};
			col.addFixedHeight(installBtn, BTN_HEIGHT);
		}

		root.setSize(layoutWidth, col.bottom());
		return root;
	}

	private void addActionRow(Component root, ColumnLayout col, String titleText, String desc, int cost, boolean enabled, Runnable onActivate) {
		float rowY = col.y;
		int titleW = layoutWidth - BTN_INFO_W - BTN_ACTIVATE_W - GAP * 2;

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(titleText, 8);
		titleBlock.maxWidth(titleW);
		titleBlock.setRect(0, rowY, titleW, 12);
		root.add(titleBlock);

		RedButton infoBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_info"), 7) {
			@Override
			protected void onClick() {
				GameScene.show(new WndMessage(desc));
			}
		};
		infoBtn.setRect(layoutWidth - BTN_INFO_W - BTN_ACTIVATE_W - GAP, rowY, BTN_INFO_W, BTN_HEIGHT);
		root.add(infoBtn);

		RedButton activateBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_activate"), 7) {
			@Override
			protected void onClick() {
				onActivate.run();
			}
		};
		activateBtn.setRect(layoutWidth - BTN_ACTIVATE_W, rowY, BTN_ACTIVATE_W, BTN_HEIGHT);
		activateBtn.enable(enabled && Dungeon.cost >= cost);
		root.add(activateBtn);

		col.y = Math.max(titleBlock.bottom(), activateBtn.bottom()) + GAP;
	}

	private void openPluginSelector() {
		WndBag.ItemSelector selector = new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(RhodesIslandTerminal.class, "install_prompt");
			}

			@Override
			public Class<? extends com.zootdungeon.items.bags.Bag> preferredBag() {
				return Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable(Item item) {
				return item instanceof TerminalPlugin;
			}

			@Override
			public void onSelect(Item item) {
				if (item instanceof TerminalPlugin plugin && Dungeon.hero != null) {
					RhodesIslandTerminal term = Dungeon.hero.belongings.getItem(RhodesIslandTerminal.class);
					if (term != null && term.canInstallMorePlugins()) {
						term.installPlugin(plugin, Dungeon.hero);
						GLog.p(Messages.get(RhodesIslandTerminal.class, "plugin_installed"));
						if (openInstance != null) openInstance.refreshPlugins();
					}
				}
			}
		};
		GameScene.selectItem(selector);
	}

	@Override
	public void destroy() {
		super.destroy();
		if (openInstance == this) openInstance = null;
	}

	/**
	 * ScrollPane 的默认滚动热区会 block pointer 输入，导致滚动区域里的 Button 点不到。
	 * 这里把滚动热区改成 NEVER_BLOCK，让按钮仍然能收到点击事件，同时保留滚动能力。
	 */
	private static class InteractiveScrollPane extends ScrollPane {
		public InteractiveScrollPane(Component content) {
			super(content);
			// ScrollPane.createChildren() 里会创建 controller，这里直接调整它的 blockLevel
			if (controller != null) {
				controller.blockLevel = PointerArea.NEVER_BLOCK;
			}
		}
	}

	private static class ColumnLayout {
		public final Component parent;
		public final float x;
		public float y;
		public final float width;
		public final float gap;

		public ColumnLayout(Component parent, float x, float y, float width, float gap) {
			this.parent = parent;
			this.x = x;
			this.y = y;
			this.width = width;
			this.gap = gap;
		}

		public void addAutoHeight(Component c) {
			c.setRect(x, y, width, 0);
			parent.add(c);
			y = c.bottom() + gap;
		}

		public void addFixedHeight(Component c, float height) {
			c.setRect(x, y, width, height);
			parent.add(c);
			y = c.bottom() + gap;
		}

		public int bottom() {
			return (int) Math.ceil(y);
		}
	}

	private static class FlowLayout {
		private final float x;
		private final float y;
		private final float width;
		private final float gap;
		private float cursor;
		private float maxBottom;
		private final ArrayList<Node> items = new ArrayList<>();

		private static class Node {
			Component component;
			float width;
			float height;

			Node(Component component, float width, float height) {
				this.component = component;
				this.width = width;
				this.height = height;
			}
		}

		public FlowLayout(float x, float y, float width, float gap) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.gap = gap;
			this.cursor = x;
			this.maxBottom = y;
		}

		public void add(Component c, float itemWidth, float itemHeight) {
			items.add(new Node(c, itemWidth, itemHeight));
		}

		public void commit(Component parent) {
			for (Node node : items) {
				node.component.setRect(cursor, y, node.width, node.height);
				parent.add(node.component);
				cursor = node.component.right() + gap;
				maxBottom = Math.max(maxBottom, node.component.bottom());
			}
		}

		public void commitByRatio(Component parent) {
			cursor = x;
			for (int i = 0; i < items.size(); i++) {
				Node node = items.get(i);
				float ratioWidth;
				if (i < items.size() - 1) {
					ratioWidth = (float) Math.floor(width * node.width) - gap;
				} else {
					ratioWidth = x + width - cursor;
				}
				node.component.setRect(cursor, y, ratioWidth, node.height);
				parent.add(node.component);
				cursor = node.component.right() + gap;
				maxBottom = Math.max(maxBottom, node.component.bottom());
			}
		}

		public float bottom() {
			return maxBottom;
		}
	}
}
