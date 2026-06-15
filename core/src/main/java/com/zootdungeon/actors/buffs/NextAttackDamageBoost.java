package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class NextAttackDamageBoost extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** damage multiplier for next physical attack */
	public float multiplier = 1.4f;

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.7f, 0.3f);
	}

	@Override
	public String desc() {
		int pct = Math.round((multiplier - 1f) * 100f);
		return "下一次攻击伤害 +" + pct + "%";
	}
}

