package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class NextAttackCostRefund extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** refund rate: cost gained = floor(damage * rate) */
	public float rate = 0.1f;

	@Override
	public int icon() {
		return BuffIndicator.RECHARGING;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.4f, 1f, 0.6f);
	}

	@Override
	public String desc() {
		int pct = Math.round(rate * 100f);
		return "下一次攻击根据造成的伤害回复 COST（约 " + pct + "%）";
	}
}

