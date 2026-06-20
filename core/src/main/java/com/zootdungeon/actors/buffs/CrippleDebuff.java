package com.zootdungeon.actors.buffs;

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
}
