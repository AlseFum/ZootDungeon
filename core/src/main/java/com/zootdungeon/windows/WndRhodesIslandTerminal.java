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
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.ui.Component;

public class WndRhodesIslandTerminal extends WndTabbed {

	private static final int WIDTH        = 120;
	private static final int BTN_HEIGHT   = 20;
	private static final int GAP          = 2;
	private static final int TAB_HEIGHT   = 25;
	private static final int PLUGIN_ICON       = 16;
	private static final int INFO_FONT         = 6;
	private static final int INFO_MAX_H        = 32;
	/** 自定义显示组件的最大高度（插件子类 createDisplayComponent 返回的组件） */
	private static final int CUSTOM_COMPONENT_H = 56;

	/** 当前打开的终端窗口，用于安装插件后刷新插件面板 */
	private static WndRhodesIslandTerminal openInstance;

	private final RhodesIslandTerminal terminal;
	private Component skillPanel;
	private PluginPanel pluginPanel;
	private float contentTop;
	private int skillContentHeight;

	public WndRhodesIslandTerminal(RhodesIslandTerminal terminal) {
		super();
		openInstance = this;
		this.terminal = terminal;

		// 窗口标题：终端图标 + 名称
		IconTitle title = new IconTitle(new ItemSprite(terminal), Messages.titleCase(terminal.name()));
		title.setRect(0, 0, WIDTH, 0);
		add(title);
		contentTop = title.bottom() + GAP;

		int skillContentHeight = 0;

		// 技能面板（第二个标签页）
		skillPanel = new Component();
		float y = 0;
		RenderedTextBlock costLabel = PixelScene.renderTextBlock(
				Messages.get(RhodesIslandTerminal.class, "cost_display", Dungeon.cost, RhodesIslandTerminal.COST_CAP), 8);
		costLabel.maxWidth(WIDTH);
		costLabel.setRect(0, y, WIDTH, 12);
		skillPanel.add(costLabel);
		y = costLabel.bottom() + GAP;

		for (Skill skill : SkillSheet.values()) {
			final Skill finalSkill = skill;
			String buttonText = skill.id;
			if (skill.cost > 0) {
				buttonText += " (" + Messages.get(RhodesIslandTerminal.class, "cost_use", skill.cost) + ")";
			}
			RedButton btn = new RedButton(buttonText) {
				@Override
				protected void onClick() {
					if (Dungeon.cost < finalSkill.cost) {
						GLog.w(Messages.get(RhodesIslandTerminal.class, "cost_not_enough", finalSkill.cost));
						return;
					}
					Dungeon.cost -= finalSkill.cost;
					finalSkill.execute(Dungeon.hero);
					hide();
				}
			};
			btn.setRect(0, y, WIDTH, BTN_HEIGHT);
			if (Dungeon.cost < skill.cost) btn.enable(false);
			skillPanel.add(btn);
			y = btn.bottom() + GAP;
		}
		this.skillContentHeight = (int) y;
		skillPanel.setRect(0, contentTop, WIDTH, skillContentHeight);
		add(skillPanel);
		skillPanel.active = skillPanel.visible = false;

		// 插件面板（主界面，默认显示；高度按内容动态收缩）
		pluginPanel = new PluginPanel(terminal);
		add(pluginPanel);
		pluginPanel.refresh();
		float pluginContentHeight = pluginPanel.getContentHeight();
		pluginPanel.setRect(0, contentTop, WIDTH, pluginContentHeight);

		// 内容区高度 = 标题下沿 + 面板内容；标签条由 WndTabbed 放在 height 处并再占 tabHeight()
		int contentHeight = (int) Math.max(skillContentHeight, pluginContentHeight);
		resize(WIDTH, (int) (contentTop + contentHeight));

		// 标签：先插件、后技能（插件为主界面）
		add(new IconTab(new ItemSprite(new TerminalPlugin())) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				pluginPanel.active = pluginPanel.visible = value;
				if (value) pluginPanel.refresh();
			}
		});
		add(new IconTab(new ItemSprite(terminal)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				skillPanel.active = skillPanel.visible = value;
			}
		});

		layoutTabs();
		select(0); // 默认选中插件页
	}

	@Override
	public void destroy() {
		super.destroy();
		if (openInstance == this) openInstance = null;
	}

	/** 刷新插件标签页内容（安装/卸载后由 ItemSelector 调用），并动态收缩面板与窗口高度 */
	public void refreshPlugins() {
		if (pluginPanel == null) return;
		pluginPanel.refresh();
		float ph = pluginPanel.getContentHeight();
		pluginPanel.setRect(0, contentTop, WIDTH, ph);
		resize(WIDTH, (int) (contentTop + Math.max(skillContentHeight, ph)));
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
			RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(RhodesIslandTerminal.class, "plugins_title"), 8);
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

				RedButton uninstall = new RedButton(Messages.get(RhodesIslandTerminal.class, "uninstall")) {
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
				RedButton installBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "install_plugin")) {
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
				if (item != null && item instanceof TerminalPlugin && Dungeon.hero != null) {
					RhodesIslandTerminal term = Dungeon.hero.belongings.getItem(RhodesIslandTerminal.class);
					if (term != null && term.canInstallMorePlugins()) {
						term.installPlugin((TerminalPlugin) item, Dungeon.hero);
						GLog.p(Messages.get(RhodesIslandTerminal.class, "plugin_installed"));
						if (openInstance != null) openInstance.refreshPlugins();
					}
				}
			}
		};
	}
}
