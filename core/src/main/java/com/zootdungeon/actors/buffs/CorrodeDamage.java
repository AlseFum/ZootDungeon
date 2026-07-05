package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.ToxicGas;
import com.zootdungeon.actors.buffs.Corrosion;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;

/**
 * 浊蚀损伤：爆发时腐蚀 + 毒气。
 * SPLASH_TRIGGER：毒气范围更大，腐蚀更强。
 */
public class CorrodeDamage extends ElementalDamage {

	private int gasRadius = 30;
	private float corrosionDur = 5f;
	private int corrosionLvl = 6;

	@Override
	protected void enhanceBurst(int talentPts) {
		gasRadius = 30 + talentPts * 20;
		corrosionDur = 5f + talentPts * 2f;
		corrosionLvl = 6 + talentPts * 2;
	}

	@Override
	protected void onBurst(int damage) {
		target.damage(damage, this);
		GameScene.add(Blob.seed(target.pos, gasRadius, ToxicGas.class));
		Buff.affect(target, Corrosion.class).set(corrosionDur, corrosionLvl);
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
		icon.hardlight(0.3f, 0.8f, 0f);
	}
}
