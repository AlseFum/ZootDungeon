package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.material.Gold;
import com.watabou.utils.Random;

/**
 * 碎骨战室内较弱小怪：碎骨或任意一只同类真实看见英雄后，同层所有此类怪在 AI 上都视为看见英雄。
 */
public class ShattererSquadMob extends Mob {

	{
		spriteClass = Infantry.InfantrySprite.class;

		HP = HT = 8;
		defenseSkill = 3;
		EXP = 2;
		maxLvl = 6;
		loot = Gold.class;
		lootChance = 0.25f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(1, 4);
	}

	@Override
	public int attackSkill( Char target ) {
		return 9;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1);
	}

	@Override
	protected void afterFieldOfViewUpdated() {
		super.afterFieldOfViewUpdated();
		if ( !(Dungeon.level instanceof HourOfAnAwakeningBossLevel) ) {
			return;
		}
		HourOfAnAwakeningBossLevel lvl = (HourOfAnAwakeningBossLevel) Dungeon.level;
		Hero h = Dungeon.hero;
		if ( !h.isAlive() ) {
			return;
		}
		boolean reallySee = fieldOfView[h.pos] && h.invisible <= 0;
		if ( reallySee ) {
			lvl.onShattererSquadSpottedHero();
		}
		if ( lvl.areShattererSquadLinkedOnHero() && h.invisible <= 0 ) {
			fieldOfView[h.pos] = true;
		}
	}
}
