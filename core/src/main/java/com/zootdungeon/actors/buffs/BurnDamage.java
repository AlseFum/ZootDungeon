package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.utils.GLog;

/**
 * 灼燃损伤：爆发时造成大量火焰伤害 + 点燃 + 地面起火。
 * SPLASH_TRIGGER：火焰范围扩大，燃烧更久。
 */
public class BurnDamage extends ElementalDamage {

	private float burnDuration = 5f;
	private int fireRadius = 2;

	@Override
	protected void enhanceBurst(int talentPts) {
		burnDuration = 5f + talentPts * 3f;
		fireRadius = 2 + talentPts;
	}

	@Override
	protected void onBurst(int damage) {
		int finalDmg = Math.round(damage * 1.2f);
		target.damage(finalDmg, this);
		GameScene.add(Blob.seed(target.pos, fireRadius, Fire.class));
		Buff.affect(target, Burning.class).reignite(target, burnDuration);
		if (target.sprite != null) {
			target.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(finalDmg));
		}
		GLog.n("burring");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", accumulation, THRESHOLD);
	}

	@Override
	public void tintIcon(com.watabou.noosa.Image icon) {
		float p = Math.min(1f, (float)accumulation / THRESHOLD);
		icon.hardlight(1f, 0.3f * (1f - p * 0.5f), 0f);
	}
}