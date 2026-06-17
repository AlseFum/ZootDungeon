package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * BLAZE subclass Heat buff. Stacks increase natural regen speed and hunger rate.
 */
public class BlazeHeatBuff extends CounterBuff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	@Override
	public int icon() { return BuffIndicator.FIRE; }

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.4f, 0f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) count());
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", count());
	}

	/**
	 * Regen speed multiplier from heat stacks. Each stack gives ~5% faster regen.
	 */
	public float regenMultiplier() {
		return 1f + count() * 0.05f;
	}

	/**
	 * Hunger speed multiplier from heat stacks. Each stack gives ~3% faster hunger.
	 */
	public float hungerMultiplier() {
		return 1f + count() * 0.03f;
	}
}
