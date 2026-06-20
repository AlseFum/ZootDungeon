package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * SCOUT talent: increased loot drops (equivalent to Ring of Wealth effect).
 * Applied automatically when the hero has the SCOUT_BETTER_LOOT talent.
 */
public class ScoutLootBuff extends FlavourBuff {
	{
		type = buffType.POSITIVE;
	}

	/** Returns the drop chance multiplier for SCOUT's better loot talent. */
	public static float dropChanceMultiplier(Hero hero) {
		if (hero.subClass == HeroSubClass.SCOUT && hero.hasTalent(Talent.SCOUT_BETTER_LOOT)) {
			int pts = hero.pointsInTalent(Talent.SCOUT_BETTER_LOOT);
			return 1f + 0.3f * pts; // +30%/+60%/+90%
		}
		return 1f;
	}

	@Override public int icon() { return BuffIndicator.NONE; }
	@Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.85f, 0f); }
	@Override public String desc() { return Messages.get(this, "desc"); }
}
