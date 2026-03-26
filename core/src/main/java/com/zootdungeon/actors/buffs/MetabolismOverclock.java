package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * Passive metabolism modifier: faster hunger gain and faster HP regeneration.
 */
public class MetabolismOverclock extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** Hunger gain multiplier (>1 = faster hunger). */
	public float hungerMult = 1.5f;

	/** HP regen speed multiplier (>1 = faster regen). */
	public float regenMult = 1.5f;

	@Override
	public int icon() {
		return BuffIndicator.WELL_FED;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.9f, 0.6f, 1f);
	}

	@Override
	public String desc() {
		int h = Math.round((hungerMult - 1f) * 100f);
		int r = Math.round((regenMult - 1f) * 100f);
		return "代谢超频：饥饿速率 +" + h + "%，回血速率 +" + r + "%";
	}
}

