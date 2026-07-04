package com.zootdungeon.items.weapon.missiles;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.BlazeHeatBuff;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.weapon.base.MissileWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class CircularSawBlade extends MissileWeapon {

	{
		image = ItemSpriteSheet.THROWING_SPIKE; // TODO: replace with saw blade sprite
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.15f;

		bones = false;

		tier = 2;
		baseUses = 3;
		sticky = false;
	}

	@Override
	protected void onThrow(int cell) {
		super.onThrow(cell);

		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == curUser) return;

		if (curUser instanceof Hero) {
			Hero hero = (Hero) curUser;
			if (hero.hasTalent(Talent.BLAZE_SAW_MASTERY)) {
				int pts = hero.pointsInTalent(Talent.BLAZE_SAW_MASTERY);

				// Extra hit: 33%/67%/100% per point
				if (enemy.isAlive() && Random.Float() < pts / 3f) {
					enemy.damage(damageRoll(hero), hero);
				}

				// Level 3: explosion
				if (pts >= 3) {
					com.zootdungeon.items.bombs.Bomb.ConjuredBomb bomb =
						new com.zootdungeon.items.bombs.Bomb.ConjuredBomb();
					bomb.explode(cell);
					if (enemy.isAlive()) {
						Buff.affect(enemy, Burning.class).reignite(enemy, 4f);
					}
				}
			}

			// Gain heat on hit
			if (hero.subClass == com.zootdungeon.actors.hero.HeroSubClass.BLAZE) {
				BlazeHeatBuff heat = Buff.affect(hero, BlazeHeatBuff.class);
				heat.countUp(1);
			}
		}
	}

	@Override
	public int value() {
		return 30 * quantity;
	}
}
