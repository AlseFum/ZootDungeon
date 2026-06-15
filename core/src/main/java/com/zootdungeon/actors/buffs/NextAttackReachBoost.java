package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class NextAttackReachBoost extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public int bonusReach = 2;

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.5f, 0.9f, 1f);
	}

	@Override
	public String desc() {
		return "下一次攻击的 RCH +" + bonusReach;
	}
}
