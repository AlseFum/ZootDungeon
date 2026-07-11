package com.zootdungeon.items.wands;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.items.ItemEffects;
import com.zootdungeon.items.weapon.MagesStaff;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

/** Eyjafjalla's wand: fires a fireball that damages and ignites the area around the impact. */
public class WandOfEyjafjalla extends DamageWand {

	{
		image = 18;
		collisionProperties = Ballistica.STOP_SOLID | Ballistica.STOP_TARGET;
	}

	@Override
	public int min(int lvl) {
		return 3 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 10 + 3 * lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {
		int cell = bolt.collisionPos;

		Char ch = Actor.findChar(cell);
		if (ch != null) {
			wandProc(ch, 1);
			ch.damage(damageRoll(), this);
			if (ch.isAlive()) {
				Buff.affect(ch, Burning.class).reignite(ch);
			}
		}

		// ignite impact + 3x3 area
		if (!Dungeon.level.solid[cell]) {
			GameScene.add(Blob.seed(cell, 5, Fire.class));
		}
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = cell + n;
			if (Dungeon.level.insideMap(c) && !Dungeon.level.solid[c]) {
				GameScene.add(Blob.seed(c, 3, Fire.class));
				Char nearby = Actor.findChar(c);
				if (nearby != null && nearby != ch) {
					Buff.affect(nearby, Burning.class).reignite(nearby);
				}
			}
		}

		// open doors
		for (int n : PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (Dungeon.level.insideMap(c) && Dungeon.level.map[c] == Terrain.DOOR) {
				Level.set(c, Terrain.OPEN_DOOR);
				GameScene.updateMap(c);
			}
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		new ItemEffects.FireBlastEnchantment().proc(staff, attacker, defender, damage);
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(curUser.sprite.parent,
				MagicMissile.FIRE,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xEE6622);
		particle.am = 0.5f;
		particle.setLifespan(0.6f);
		particle.acc.set(0, -40);
		particle.setSize(0f, 3f);
		particle.shuffleXY(1.5f);
	}
}
