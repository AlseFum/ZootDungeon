package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;

/** Temporary buff: next attack deals bonus assassination damage.  MISERY feature. */
public class ShadowStrikeBuff extends FlavourBuff {
	{
		type = buffType.POSITIVE;
	}
	@Override
	public int icon() { return BuffIndicator.WEAPON; }
}
