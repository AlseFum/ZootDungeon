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

package com.zootdungeon.items.wands;
import com.zootdungeon.items.ItemEffects;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.PithWandMemory;
import com.zootdungeon.actors.buffs.WandEmpower;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;

//for wands that directly damage a target
//wands with AOE or circumstantial direct damage count here (e.g. fireblast, transfusion), but wands with indirect damage do not (e.g. corrosion)
public abstract class DamageWand extends Wand{

	public int min(){
		return min(buffedLvl());
	}

	public abstract int min(int lvl);

	public int max(){
		return max(buffedLvl());
	}

	public abstract int max(int lvl);

	public int damageRoll(){
		return damageRoll(buffedLvl());
	}

	public int damageRoll(int lvl){
		int dmg = Hero.heroDamageIntRange(min(lvl), max(lvl));
		WandEmpower emp = Dungeon.hero.buff(WandEmpower.class);
		if (emp != null){
			dmg += emp.dmgBoost;
			emp.left--;
			if (emp.left <= 0) {
				emp.detach();
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.75f, 1.2f);
		}
		// PITH: 联动法术按表增伤，CHAIN_STRONGER 额外增强倍率
		if (Dungeon.hero.subClass == HeroSubClass.PITH) {
			PithWandMemory mem = Dungeon.hero.buff(PithWandMemory.class);
			if (mem != null && mem.getMemorized() != null) {
				PithWandMemory.ComboEntry combo = mem.getCombo(this.getClass());
				float mult = combo.powerLevel;
				if (Dungeon.hero.hasTalent(Talent.PITH_CHAIN_STRONGER)) {
					mult += 0.1f * Dungeon.hero.pointsInTalent(Talent.PITH_CHAIN_STRONGER);
				}
				dmg = Math.round(dmg * mult);
			}
		}
		// PITH FULL_DISCHARGE: 通过 PithWandMemory 计算完全释放增伤
		if (Dungeon.hero.subClass == HeroSubClass.PITH) {
			PithWandMemory mem = Dungeon.hero.buff(PithWandMemory.class);
			if (mem != null) {
				float dischargeMult = mem.fullDischargePower();
				if (dischargeMult > 1f) {
					dmg = Math.round(dmg * dischargeMult);
				}
			}
		}
		return dmg;
	}

	@Override
	public String statsDesc() {
		if (levelKnown)
			return Messages.get(this, "stats_desc", min(), max());
		else
			return Messages.get(this, "stats_desc", min(0), max(0));
	}

	@Override
	public String upgradeStat1(int level) {
		return min(level) + "-" + max(level);
	}
}
