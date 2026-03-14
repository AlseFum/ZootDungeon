/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zootdungeon.windows;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Belongings;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.arknights.skills.Skill;
import com.zootdungeon.arknights.skills.SkillSheet;
import com.zootdungeon.items.Item;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.ui.Component;

public class WndRhodesIslandTerminal extends WndTabbed {

	private static final int WIDTH        = 140;
	private static final int BTN_HEIGHT   = 18;
	private static final int GAP          = 2;
	private static final int TAB_HEIGHT   = 25;
	private static final int PLUGIN_ICON       = 16;
	private static final int INFO_FONT         = 6;
	private static final int INFO_MAX_H        = 32;
	/** 自定义显示组件的最大高度（插件子类 createDisplayComponent 返回的组件） */
	private static final int CUSTOM_COMPONENT_H = 56;

	/** 当前打开的终端窗口，用于安装插件后刷新插件面板 */
	private static WndRhodesIslandTerminal openInstance;

	private static final int BTN_INFO_W = 32;
	private static final int BTN_ACTIVATE_W = 40;
	private static final int VALUE_W = 36;

	private final RhodesIslandTerminal terminal;
	private Component activePanel;
	private PassivePanel passivePanel;
	private PluginPanel pluginPanel;
	private float contentTop;
	private int activeContentHeight;

	public WndRhodesIslandTerminal(RhodesIslandTerminal terminal) {
		super();
		openInstance = this;
		this.terminal = terminal;

		// 窗口标题：终端图标 + 名称
		IconTitle title = new IconTitle(new ItemSprite(terminal), "罗德岛终端");
		title.setRect(0, 0, WIDTH, 0);
		add(title);
		contentTop = title.bottom() + GAP;

		// 主动 Tab：技能列表，每行 [名称] [说明] [激活]
		activePanel = new Component();
		float y = 0;
		RenderedTextBlock costLabel = PixelScene.renderTextBlock(
				"COST: " + Dungeon.cost + "/" + RhodesIslandTerminal.COST_CAP, 8);
		costLabel.maxWidth(WIDTH);
		costLabel.setRect(0, y, WIDTH, 12);
		activePanel.add(costLabel);
		y = costLabel.bottom() + GAP;

		for (Skill skill : SkillSheet.values()) {
			final Skill finalSkill = skill;
			float rowY = y;
			String nameText = skill.name();
			if (skill.cost > 0) {
				nameText += " (消耗 " + skill.cost + " cost)";
			}
			RenderedTextBlock nameBlock = PixelScene.renderTextBlock(nameText, 8);
			nameBlock.maxWidth(WIDTH - BTN_INFO_W - BTN_ACTIVATE_W - GAP * 2);
			nameBlock.setRect(0, rowY, WIDTH - BTN_INFO_W - BTN_ACTIVATE_W - GAP * 2, 12);
			activePanel.add(nameBlock);

			RedButton infoBtn = new RedButton("说明", 7) {
				@Override
				protected void onClick() {
					GameScene.show(new WndMessage(finalSkill.desc()));
				}
			};
			infoBtn.setRect(WIDTH - BTN_INFO_W - BTN_ACTIVATE_W - GAP, rowY, BTN_INFO_W, BTN_HEIGHT);
			activePanel.add(infoBtn);

			RedButton activateBtn = new RedButton("激活", 7) {
				@Override
				protected void onClick() {
					if (Dungeon.cost < finalSkill.cost) {
						GLog.w("cost不足，需要 " + finalSkill.cost + "。");
						return;
					}
					Dungeon.cost -= finalSkill.cost;
					finalSkill.execute(Dungeon.hero);
					hide();
				}
			};
			activateBtn.setRect(WIDTH - BTN_ACTIVATE_W, rowY, BTN_ACTIVATE_W, BTN_HEIGHT);
			if (Dungeon.cost < skill.cost) activateBtn.enable(false);
			activePanel.add(activateBtn);

			y = Math.max(nameBlock.bottom(), activateBtn.bottom()) + GAP;
		}
		activeContentHeight = (int) y;
		activePanel.setRect(0, contentTop, WIDTH, activeContentHeight);
		add(activePanel);
		activePanel.active = activePanel.visible = false;

		// 被动 Tab：仅显示已安装插件提供的统计
		passivePanel = new PassivePanel(terminal);
		add(passivePanel);
		passivePanel.refresh();
		float passiveContentHeight = passivePanel.getContentHeight();
		passivePanel.setRect(0, contentTop, WIDTH, passiveContentHeight);
		passivePanel.active = passivePanel.visible = false;

		// 插件面板
		pluginPanel = new PluginPanel(terminal);
		add(pluginPanel);
		pluginPanel.refresh();
		float pluginContentHeight = pluginPanel.getContentHeight();
		pluginPanel.setRect(0, contentTop, WIDTH, pluginContentHeight);

		int contentHeight = (int) Math.max(Math.max(activeContentHeight, passiveContentHeight), pluginContentHeight);
		resize(WIDTH, (int) (contentTop + contentHeight));

		// 标签：主动、被动、插件
		add(new IconTab(Icons.get(Icons.TALENT)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				activePanel.active = activePanel.visible = value;
			}
		});
		add(new IconTab(Icons.get(Icons.STATS)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				passivePanel.active = passivePanel.visible = value;
				if (value) passivePanel.refresh();
			}
		});
		add(new IconTab(new ItemSprite(new TerminalPlugin())) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				pluginPanel.active = pluginPanel.visible = value;
				if (value) {
					pluginPanel.refresh();
					for (TerminalPlugin p : terminal.getInstalledPlugins()) {
						p.onResume(terminal);
					}
				}
			}
		});

		layoutTabs();
		select(0); // 默认选中主动页
	}

	/** 刷新插件/被动标签页内容（安装/卸载后由 ItemSelector 调用），并动态收缩面板与窗口高度 */
	public void refreshPlugins() {
		if (pluginPanel == null) return;
		pluginPanel.refresh();
		if (passivePanel != null) passivePanel.refresh();
		float ph = pluginPanel.getContentHeight();
		float pah = passivePanel != null ? passivePanel.getContentHeight() : 0;
		pluginPanel.setRect(0, contentTop, WIDTH, ph);
		if (passivePanel != null) passivePanel.setRect(0, contentTop, WIDTH, pah);
		int maxH = (int) Math.max(Math.max(activeContentHeight, pah), ph);
		resize(WIDTH, (int) (contentTop + maxH));
	}

	@Override
	public void destroy() {
		for (TerminalPlugin p : terminal.getInstalledPlugins()) {
			p.onPause(terminal);
		}
		super.destroy();
		if (openInstance == this) openInstance = null;
	}

	private static class PassivePanel extends Component {
		private final RhodesIslandTerminal terminal;
		private float layoutY;
		private float contentHeight;

		PassivePanel(RhodesIslandTerminal terminal) {
			this.terminal = terminal;
		}

		float getContentHeight() {
			return contentHeight;
		}

		void refresh() {
			clear();
			layoutY = GAP;
			java.util.ArrayList<TerminalPlugin> plugins = terminal.getInstalledPlugins();
			int nameW = WIDTH - BTN_INFO_W - VALUE_W - GAP * 2;

			for (TerminalPlugin plugin : plugins) {
				for (String[] entry : plugin.getPassiveEntries(terminal)) {
					if (entry == null || entry.length < 3) continue;
					final String name = entry[0];
					final String desc = entry[1];
					String value = entry[2];
					float rowY = layoutY;

					RenderedTextBlock nameBlock = PixelScene.renderTextBlock(name, 8);
					nameBlock.maxWidth(nameW);
					nameBlock.setRect(0, rowY, nameW, 12);
					add(nameBlock);

					RedButton infoBtn = new RedButton("说明", 7) {
						@Override
						protected void onClick() {
							GameScene.show(new WndMessage(desc));
						}
					};
					infoBtn.setRect(WIDTH - BTN_INFO_W - VALUE_W - GAP, rowY, BTN_INFO_W, BTN_HEIGHT);
					add(infoBtn);

					RenderedTextBlock valueBlock = PixelScene.renderTextBlock(value, 8);
					valueBlock.setRect(WIDTH - VALUE_W, rowY + 4, VALUE_W, 12);
					add(valueBlock);

					layoutY = Math.max(nameBlock.bottom(), infoBtn.bottom()) + GAP;
				}
			}

			if (layoutY == GAP) {
				RenderedTextBlock empty = PixelScene.renderTextBlock("暂无被动统计", 8);
				empty.maxWidth(WIDTH);
				empty.setRect(0, layoutY, WIDTH, 14);
				add(empty);
				layoutY = empty.bottom() + GAP;
			}
			contentHeight = layoutY;
		}
	}

	private static class PluginPanel extends Component {
		private final RhodesIslandTerminal terminal;
		private float layoutY;
		/** refresh 后内容总高度，供父窗口动态设置面板与窗口高度 */
		private float contentHeight;

		PluginPanel(RhodesIslandTerminal terminal) {
			this.terminal = terminal;
		}

		float getContentHeight() {
			return contentHeight;
		}

		void refresh() {
			clear();
			layoutY = GAP;
			// 已安装插件标题
			RenderedTextBlock title = PixelScene.renderTextBlock("已安装插件", 8);
			title.maxWidth(WIDTH);
			title.setRect(0, layoutY, WIDTH, 14);
			add(title);
			layoutY = title.bottom() + GAP;

			Hero hero = Dungeon.hero;
			java.util.ArrayList<TerminalPlugin> plugins = terminal.getInstalledPlugins();
			int nameAreaWidth = WIDTH - (PLUGIN_ICON + GAP) - (48 + GAP); // 名称区域宽度，右侧留卸载按钮

			for (int i = 0; i < plugins.size(); i++) {
				final int index = i;
				TerminalPlugin plugin = plugins.get(i);
				float rowTop = layoutY;

				// 第一行：图标 + 名称 + 卸载按钮
				ItemSprite sprite = new ItemSprite(plugin);
				sprite.x = 0;
				sprite.y = rowTop;
				sprite.width = PLUGIN_ICON;
				sprite.height = PLUGIN_ICON;
				add(sprite);

				RenderedTextBlock name = PixelScene.renderTextBlock(plugin.name(), 8);
				name.maxWidth(nameAreaWidth);
				name.setRect(PLUGIN_ICON + GAP, rowTop + 2, nameAreaWidth, 12);
				add(name);

				RedButton uninstall = new RedButton("卸载") {
					@Override
					protected void onClick() {
						terminal.uninstallPlugin(index, hero);
						refresh();
					}
				};
				uninstall.setRect(WIDTH - 48, rowTop, 48, BTN_HEIGHT);
				add(uninstall);

				// 第二行：自定义显示组件 或 默认描述
				float contentTop = rowTop + BTN_HEIGHT + GAP;
				int contentWidth = WIDTH - (PLUGIN_ICON + GAP);
				Component custom = plugin.createDisplayComponent(terminal);
				if (custom != null) {
					custom.setRect(PLUGIN_ICON + GAP, contentTop, contentWidth, CUSTOM_COMPONENT_H);
					add(custom);
					layoutY = custom.bottom() + GAP;
				} else {
					String infoText = plugin.desc();
					if (infoText != null && !infoText.isEmpty()) {
						RenderedTextBlock info = PixelScene.renderTextBlock(infoText, INFO_FONT);
						info.maxWidth(contentWidth);
						info.setRect(PLUGIN_ICON + GAP, contentTop, contentWidth, INFO_MAX_H);
						add(info);
						layoutY = info.bottom() + GAP;
					} else {
						layoutY = contentTop + GAP;
					}
				}
			}

			if (terminal.canInstallMorePlugins()) {
				RedButton installBtn = new RedButton("安装插件") {
					@Override
					protected void onClick() {
						GameScene.selectItem(pluginSelector);
					}
				};
				installBtn.setRect(0, layoutY, WIDTH, BTN_HEIGHT);
				add(installBtn);
				layoutY = installBtn.bottom() + GAP;
			}
			contentHeight = layoutY;
		}

		private static final WndBag.ItemSelector pluginSelector = new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return "选择要安装的插件";
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
				if (item != null && item instanceof TerminalPlugin && Dungeon.hero != null) {
					RhodesIslandTerminal term = Dungeon.hero.belongings.getItem(RhodesIslandTerminal.class);
					if (term != null && term.canInstallMorePlugins()) {
						term.installPlugin((TerminalPlugin) item, Dungeon.hero);
						GLog.p("插件已安装。");
						if (openInstance != null) openInstance.refreshPlugins();
					}
				}
			}
		};
	}
}
