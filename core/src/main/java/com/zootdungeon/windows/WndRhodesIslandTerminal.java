package com.zootdungeon.windows;

import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Belongings;
import com.zootdungeon.actors.hero.Hero;
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

public class WndRhodesIslandTerminal extends Window {

	private static final int WIDTH = 176;
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
	private static final int INFO_FONT = 6;
	private static final int INFO_MAX_H = 32;
	private static final int CUSTOM_COMPONENT_H = 56;
	private static final int EMPTY_TEXT_H = 14;

	private static WndRhodesIslandTerminal openInstance;

	private final RhodesIslandTerminal terminal;
	private IconTitle title;
	private RedButton tabActiveBtn;
	private RedButton tabPassiveBtn;
	private RedButton tabPluginBtn;
	private ScrollPane contentPane;
	private TabType currentTab = TabType.ACTIVE;

	private enum TabType {
		ACTIVE, PASSIVE, PLUGINS
	}

	public WndRhodesIslandTerminal(RhodesIslandTerminal terminal) {
		this.terminal = terminal;
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

		relayout();
		switchTab(TabType.ACTIVE);
	}

	private void relayout() {
		float y = MARGIN;
		title.setRect(0, y, WIDTH, 0);
		y = title.bottom() + GAP;

		float firstW = (float) Math.floor(WIDTH * 0.333f) - GAP;
		float secondW = (float) Math.floor(WIDTH * 0.333f) - GAP;
		float thirdW = WIDTH - firstW - secondW - GAP * 2;
		tabActiveBtn.setRect(0, y, firstW, TAB_BTN_H);
		tabPassiveBtn.setRect(tabActiveBtn.right() + GAP, y, secondW, TAB_BTN_H);
		tabPluginBtn.setRect(tabPassiveBtn.right() + GAP, y, thirdW, TAB_BTN_H);
		y = tabActiveBtn.bottom() + GAP;

		int viewH = getAvailableViewHeight(y);
		if (contentPane != null) {
			contentPane.setRect(0, y, WIDTH, viewH);
		}
		resize(WIDTH, (int) (y + viewH + MARGIN));
	}

	private int getAvailableViewHeight(float contentTop) {
		int maxWindowHeight = (int) PixelScene.uiCamera.height - 12;
		int bySpace = (int) (maxWindowHeight - contentTop - MARGIN);
		return Math.max(MIN_VIEW_H, bySpace);
	}

	private void switchTab(TabType tab) {
		currentTab = tab;
		if (contentPane != null) {
			remove(contentPane);
			contentPane = null;
		}

		Component content = buildTabContent(tab);
		contentPane = new ScrollPane(content);
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
		ColumnLayout col = new ColumnLayout(root, 0, 0, WIDTH, GAP);
		boolean hasActiveEntry = false;

		RenderedTextBlock costLabel = PixelScene.renderTextBlock(
				Messages.get(RhodesIslandTerminal.class, "cost_display", Dungeon.cost, RhodesIslandTerminal.COST_CAP), 8);
		costLabel.maxWidth(WIDTH);
		col.addAutoHeight(costLabel);

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
			empty.maxWidth(WIDTH);
			empty.setRect(0, col.y, WIDTH, EMPTY_TEXT_H);
			root.add(empty);
			col.y = empty.bottom() + GAP;
		}

		root.setSize(WIDTH, col.bottom());
		return root;
	}

	private Component buildPassiveContent() {
		Component root = new Component();
		ColumnLayout col = new ColumnLayout(root, 0, 0, WIDTH, GAP);
		int nameW = WIDTH - BTN_INFO_W - VALUE_W - GAP * 2;

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
				infoBtn.setRect(WIDTH - BTN_INFO_W - VALUE_W - GAP, rowY, BTN_INFO_W, BTN_HEIGHT);
				root.add(infoBtn);

				RenderedTextBlock valueBlock = PixelScene.renderTextBlock(value, 8);
				valueBlock.setRect(WIDTH - VALUE_W, rowY + 4, VALUE_W, 12);
				root.add(valueBlock);

				col.y = Math.max(nameBlock.bottom(), infoBtn.bottom()) + GAP;
			}
		}

		if (col.y <= 0.001f) {
			RenderedTextBlock empty = PixelScene.renderTextBlock(Messages.get(RhodesIslandTerminal.class, "passive_empty"), 8);
			empty.maxWidth(WIDTH);
			col.addAutoHeight(empty);
		}

		root.setSize(WIDTH, col.bottom());
		return root;
	}

	private Component buildPluginContent() {
		Component root = new Component();
		ColumnLayout col = new ColumnLayout(root, 0, 0, WIDTH, GAP);
		Hero hero = Dungeon.hero;
		boolean hasPlugin = false;

		RenderedTextBlock pluginsTitle = PixelScene.renderTextBlock(Messages.get(RhodesIslandTerminal.class, "plugins_title"), 8);
		pluginsTitle.maxWidth(WIDTH);
		col.addAutoHeight(pluginsTitle);
		RenderedTextBlock slotsInfo = PixelScene.renderTextBlock("Slots: " + terminal.slots.size() + "/" + terminal.maxPlugins, 7);
		slotsInfo.maxWidth(WIDTH);
		col.addAutoHeight(slotsInfo);

		int rightButtons = BTN_UNINSTALL_W + GAP + BTN_TOGGLE_W;
		int nameW = WIDTH - rightButtons - GAP;

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

			FlowLayout buttons = new FlowLayout(WIDTH - rightButtons, rowY, rightButtons + GAP, GAP);
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
				custom.setRect(0, col.y, WIDTH, CUSTOM_COMPONENT_H);
				root.add(custom);
				col.y = custom.bottom() + GAP;
			} else {
				String infoText = plugin.pluginDesc(terminal, slot);
				if (infoText == null || infoText.isEmpty()) infoText = plugin.desc();
				if (infoText != null && !infoText.isEmpty()) {
					RenderedTextBlock info = PixelScene.renderTextBlock(infoText, INFO_FONT);
					info.maxWidth(WIDTH);
					info.setRect(0, col.y, WIDTH, INFO_MAX_H);
					root.add(info);
					col.y = info.bottom() + GAP;
				}
			}
		}
		if (!hasPlugin) {
			RenderedTextBlock empty = PixelScene.renderTextBlock("No plugins installed.", 8);
			empty.maxWidth(WIDTH);
			empty.setRect(0, col.y, WIDTH, EMPTY_TEXT_H);
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

		root.setSize(WIDTH, col.bottom());
		return root;
	}

	private void addActionRow(Component root, ColumnLayout col, String titleText, String desc, int cost, boolean enabled, Runnable onActivate) {
		float rowY = col.y;
		int titleW = WIDTH - BTN_INFO_W - BTN_ACTIVATE_W - GAP * 2;

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
		infoBtn.setRect(WIDTH - BTN_INFO_W - BTN_ACTIVATE_W - GAP, rowY, BTN_INFO_W, BTN_HEIGHT);
		root.add(infoBtn);

		RedButton activateBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_activate"), 7) {
			@Override
			protected void onClick() {
				onActivate.run();
			}
		};
		activateBtn.setRect(WIDTH - BTN_ACTIVATE_W, rowY, BTN_ACTIVATE_W, BTN_HEIGHT);
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
