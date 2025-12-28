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

package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class PowerStrike extends Buff {
	
	{
		type = buffType.POSITIVE;
		announced = true;
	}
	
	public float boostMultiplier = 1.3f; // 30% 伤害提升
	public boolean used = false; // 是否已使用
	
	public PowerStrike() {
		this(1.3f);
	}
	
	public PowerStrike(float multiplier) {
		this.boostMultiplier = multiplier;
	}
	
	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 1f, 0f); // 黄色
	}
	
	@Override
	public String desc() {
		return "下一次攻击伤害提升" + (int)((boostMultiplier - 1f) * 100) + "%";
	}
}

