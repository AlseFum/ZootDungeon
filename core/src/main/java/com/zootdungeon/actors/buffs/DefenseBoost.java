package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class DefenseBoost extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** mitigated damage multiplier = (1 - reduction) */
	public float reduction = 0.3f;

	@Override
	public int icon() {
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.6f, 0.8f, 1f);
	}

	@Override
	public String desc() {
		int pct = Math.round(reduction * 100f);
		return "防御力提升（受到伤害 -" + pct + "%），剩余 " + dispTurns() + " 回合";
	}
}

