package com.zootdungeon.actors.blobs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.buffs.ShadowStrikeBuff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.BlobEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.ShadowBlobParticle;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.watabou.utils.Random;

/**
 * MISERY class feature: shadow cells cover dungeon tiles as a persistent blob.
 * Generated once per level, never fades. Enemies less likely to detect MISERY
 * inside shadow cells. MISERY can teleport to shadow cells at HP cost.
 */
public class MiseryShadowBlob extends Blob {

	{
		actPriority = HERO_PRIO;
	}

	/** One-time generation: cover the level with shadow cells (~25% of passable tiles). */
	public static void generateForLevel() {
		if (Dungeon.level == null) return;
		MiseryShadowBlob blob = new MiseryShadowBlob();
		Dungeon.level.blobs.put(MiseryShadowBlob.class, blob);
		for (int cell = 0; cell < Dungeon.level.length(); cell++) {
			if (Dungeon.level.passable[cell] && Random.Float() < 0.25f) {
				blob.seed(Dungeon.level, cell, Random.Int(3, 8));
			}
		}
		GameScene.add(blob);
	}

	@Override
	protected void evolve() {
		// Shadows persist — no decay
		int cell;
		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				off[cell] = cur[cell];
				volume += off[cell];
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.pour(ShadowBlobParticle.FACTORY, 0.15f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	/** Check if a cell has shadow. */
	public static boolean isShadowCell(int cell) {
		if (Dungeon.level == null) return false;
		MiseryShadowBlob blob = (MiseryShadowBlob) Dungeon.level.blobs.get(MiseryShadowBlob.class);
		return blob != null && blob.cur != null && blob.cur[cell] > 0;
	}

	/** Seed a shadow cell with the given intensity (for talent-created shadows). */
	public void addShadowCell(int cell, int amount) {
		seed(Dungeon.level, cell, amount);
	}

	/** Teleport hero to a shadow cell. Costs HP. */
	public void teleportTo(Hero hero, int cell) {
		if (cur == null || cur[cell] <= 0) return;

		int dist = Dungeon.level.distance(hero.pos, cell);
		float hpFraction = Math.min(0.5f, dist * 0.03f);

		if (hero.hasTalent(Talent.MISERY_SHADOW_TELEPORT)) {
			int pts = hero.pointsInTalent(Talent.MISERY_SHADOW_TELEPORT);
			hpFraction *= (1f - pts * 0.15f);
		}

		int hpCost = Math.max(1, Math.round(hero.HP * hpFraction));
		if (hero.HP <= hpCost) return;
		hero.HP -= hpCost;
		hero.sprite.emitter().burst(Speck.factory(Speck.WOOL), 10);
		hero.pos = cell;
		hero.sprite.place(cell);
		hero.sprite.showStatus(CharSprite.NEUTRAL, Integer.toString(hpCost));
		Dungeon.observe();
		GameScene.updateFog(cell, 2);

		Buff.affect(hero, ShadowStrikeBuff.class, 5f);

		if (hero.hasTalent(Talent.MISERY_SHADOW_TELEPORT)
				&& hero.pointsInTalent(Talent.MISERY_SHADOW_TELEPORT) >= 3) {
			Buff.affect(hero, Invisibility.class, 3f);
		}
	}

	/** Stealth multiplier: enemies less likely to detect MISERY in shadow cells. */
	public static float stealthMultiplier(Char ch) {
		if (!(ch instanceof Hero)) return 1f;
		if (Dungeon.level == null) return 1f;
		MiseryShadowBlob blob = (MiseryShadowBlob) Dungeon.level.blobs.get(MiseryShadowBlob.class);
		if (blob != null && blob.cur != null && blob.cur[ch.pos] > 0) {
			return 0.5f;
		}
		return 1f;
	}

	/** Find the nearest shadow cell. Returns -1 if none exist. */
	public int findNearestShadow(int fromCell) {
		if (cur == null || volume == 0) return -1;
		int nearest = -1;
		int minDist = Integer.MAX_VALUE;
		for (int cell = 0; cell < Dungeon.level.length(); cell++) {
			if (cur[cell] > 0) {
				int dist = Dungeon.level.distance(fromCell, cell);
				if (dist < minDist) {
					minDist = dist;
					nearest = cell;
				}
			}
		}
		return nearest;
	}

	/** SOUL_REAP talent: ambush kills increase loot drops. */
	public static float soulReapDropChance(Hero hero, Mob mob) {
		if (hero.subClass == HeroSubClass.MISERY
				&& hero.hasTalent(Talent.MISERY_SOUL_REAP)
				&& mob.surprisedBy(hero)) {
			int pts = hero.pointsInTalent(Talent.MISERY_SOUL_REAP);
			return 1f + 0.15f * pts;
		}
		return 1f;
	}
}
