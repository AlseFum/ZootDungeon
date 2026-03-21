/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
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

package com.zootdungeon.arknights.gitano;

import com.zootdungeon.actors.buffs.*;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.SpellSprite;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class GitanoCard extends Item {

	public static final String AC_USE = "USE";

	static {
		SpriteRegistry.registerItemTexture("cola/gitano_card.png", 32)
				.label("gitano_card");
	}

	{
		image = SpriteRegistry.itemByName("gitano_card");
		stackable = true;
		defaultAction = AC_USE;
	}

	// 可随机给予的buff列表
	private static final ArrayList<BuffInfo> RANDOM_BUFFS = new ArrayList<>();

	static {
		// 添加各种buff及其持续时间
		RANDOM_BUFFS.add(new BuffInfo(Haste.class, Haste.DURATION));
		RANDOM_BUFFS.add(new BuffInfo(Invisibility.class, Invisibility.DURATION));
		RANDOM_BUFFS.add(new BuffInfo(Regeneration.class, 20f));
		RANDOM_BUFFS.add(new BuffInfo(Bless.class, Bless.DURATION));
		RANDOM_BUFFS.add(new BuffInfo(Barkskin.class, 20f));
		RANDOM_BUFFS.add(new BuffInfo(Adrenaline.class, Adrenaline.DURATION));
		RANDOM_BUFFS.add(new BuffInfo(WellFed.class, 300f));
		RANDOM_BUFFS.add(new BuffInfo(Recharging.class, Recharging.DURATION));
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_USE)) {
			use(hero);
		}
	}

	public void use(Hero hero) {
		// 随机选择一个buff
		BuffInfo buffInfo = Random.element(RANDOM_BUFFS);
		Class<? extends Buff> buffClass = buffInfo.buffClass;
		float duration = buffInfo.duration;

		// 应用buff - 根据类型使用不同的方法
		Buff buff;
		if (FlavourBuff.class.isAssignableFrom(buffClass)) {
			// 对于FlavourBuff，使用affect的重载方法（会自动设置持续时间）
			@SuppressWarnings("unchecked")
			Class<? extends FlavourBuff> flavourBuffClass = (Class<? extends FlavourBuff>) buffClass;
			buff = Buff.affect(hero, flavourBuffClass, duration);
		} else {
			// 对于其他Buff，使用affect
			buff = Buff.affect(hero, buffClass);
		}

		// 显示消息
		String buffName = Messages.get(buffClass, "name");
		GLog.p(Messages.get(this, "applied", buffName));

		// 显示特效
		SpellSprite.show(hero, SpellSprite.HASTE, 1, 1, 0);

		// 消耗卡牌
		quantity--;
		if (quantity <= 0) {
			detach(hero.belongings.backpack);
		} else {
			updateQuickslot();
		}
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String info() {
		return Messages.get(this, "desc");
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 30 * quantity;
	}

	// Buff信息类
	private static class BuffInfo {
		Class<? extends Buff> buffClass;
		float duration;

		BuffInfo(Class<? extends Buff> buffClass, float duration) {
			this.buffClass = buffClass;
			this.duration = duration;
		}
	}
}
