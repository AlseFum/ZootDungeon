package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/** MISERY talent: enemies in shadow blobs are crippled — reduced speed and evasion. */
public class CrippleDebuff extends FlavourBuff {
	{
		type = buffType.NEGATIVE;
		announced = true;
	}
	@Override public int icon() { return BuffIndicator.NONE; }
	@Override public void tintIcon(Image icon) { icon.hardlight(0.5f, 0.3f, 0.7f); }
	@Override public String toString() { return Messages.get(this, "name"); }
	@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }

	/** Reduce speed by 33% while crippled. */
	public float speedFactor() {
		return 0.67f;
	}

	/** Returns an evasion multiplier for a mob under CrippleDebuff (-30% evasion). */
	public static float evasionMultiplier(Mob mob) {
		CrippleDebuff debuff = mob.buff(CrippleDebuff.class);
		return debuff != null ? 0.7f : 1f;
	}
}
