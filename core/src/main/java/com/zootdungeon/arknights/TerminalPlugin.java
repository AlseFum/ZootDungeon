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

package com.zootdungeon.arknights;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * 可安装到罗德岛终端上的插件物品。
 * 插件是单独的物品，可在终端的插件窗口中安装/卸载。
 * <p>
 * 子类可重写 {@link #createDisplayComponent(RhodesIslandTerminal)} 以在终端插件面板中
 * 显示自定义 UI 组件；返回 null 时使用默认显示（仅描述文字）。
 */
public class TerminalPlugin extends Item {

	static {
		// 使用与终端相同的贴图作为占位，可后续替换为 cola/terminal_plugin.png
		SpriteRegistry.registerItemTexture("cola/command_terminal.png", 32)
				.label("terminal_plugin");
	}

	{
		image = SpriteRegistry.itemByName("terminal_plugin");
		levelKnown = true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	/**
	 * 插件对终端部署费用恢复速度的加成倍数。1.0 为无加成。
	 */
	public float costRegenMultiplier() {
		return 1f + level() * 0.1f;
	}

	/**
	 * 若插件需要在终端窗口中使用自定义组件显示，可重写此方法并返回该组件。
	 * 终端会在插件名称行下方、卸载按钮右侧预留区域，并调用 {@link Component#setRect(float, float, float, float)}
	 * 设置位置与尺寸，子类只需创建并返回组件即可。
	 *
	 * @param terminal 当前终端，可用于与终端交互或读取状态
	 * @return 自定义显示组件，或 null 时使用默认显示（仅描述文字）
	 */
	public Component createDisplayComponent(RhodesIslandTerminal terminal) {
		return null;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		return actions;
	}

	@Override
	public int value() {
		return 40;
	}
}
