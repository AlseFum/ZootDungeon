package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.buffs.Chill;
import com.zootdungeon.actors.buffs.Degrade;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 凋亡损伤：爆发时减甲 + 减速。
 */
public class WitherDamage extends ElementalDamage {

	@Override
	protected void onBurst(int damage) {
		target.damage(damage, this);
		Buff.affect(target, Degrade.class, Degrade.DURATION);
		Buff.prolong(target, Chill.class, 5f);
		if (target.sprite != null) {
			target.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(damage));
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", accumulation, THRESHOLD);
	}

	@Override
	public void tintIcon(com.watabou.noosa.Image icon) {
		icon.hardlight(0.6f, 0f, 0.8f);
	}
}
