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

package com.zootdungeon.items.artifacts;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.sprites.SpriteRegistry;

import java.util.ArrayList;

/**
 * 可安装到罗德岛终端上的插件物品。
 * 插件是单独的物品，可在终端的插件窗口中安装/卸载。
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
