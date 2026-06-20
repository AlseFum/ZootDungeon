package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * MISERY class feature: shadow cells appear on the level.
 * Enemies less likely to detect MISERY inside shadow cells.
 * MISERY can teleport to shadow cells at HP cost.
 */
public class MiseryShadowBlob extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		actPriority = HERO_PRIO;
	}

	private HashSet<Integer> shadowCells = new HashSet<>();

	@Override
	public boolean act() {
		// Periodically spawn new shadow cells
		if (com.watabou.utils.Random.Int(15) == 0 && Dungeon.level != null) {
			int cell = com.watabou.utils.Random.Int(Dungeon.level.length());
			if (Dungeon.level.passable[cell] && Dungeon.level.heroFOV[cell]) {
				shadowCells.add(cell);
				CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 2);
			}
		}
		spend(TICK);
		return true;
	}

	public boolean isShadowCell(int cell) {
		return shadowCells.contains(cell);
	}

	public void addShadowCell(int cell, int duration) {
		shadowCells.add(cell);
		CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 3);
	}

	/**
	 * Teleport to a shadow cell. Costs HP.
	 */
	public void teleportTo(Hero hero, int cell) {
		if (!shadowCells.contains(cell)) return;

		int dist = Dungeon.level.distance(hero.pos, cell);
		float hpFraction = Math.min(0.5f, dist * 0.03f);

		if (hero.hasTalent(Talent.MISERY_SHADOW_TELEPORT)) {
			int pts = hero.pointsInTalent(Talent.MISERY_SHADOW_TELEPORT);
			hpFraction *= (1f - pts * 0.15f);
		}

		int hpCost = Math.max(1, Math.round(hero.HP * hpFraction));
		if (hero.HP <= hpCost) return; // can't kill self
		hero.HP -= hpCost;
		hero.sprite.emitter().burst(Speck.factory(Speck.WOOL), 10);
		hero.pos = cell;
		hero.sprite.place(cell);
		hero.sprite.showStatus(CharSprite.NEUTRAL, Integer.toString(hpCost));
		Dungeon.observe();
		GameScene.updateFog(cell, 2);

		// Shadow strike bonus
		Buff.affect(hero, ShadowStrikeBuff.class, 5f);

		if (hero.hasTalent(Talent.MISERY_SHADOW_TELEPORT)
				&& hero.pointsInTalent(Talent.MISERY_SHADOW_TELEPORT) >= 3) {
			Buff.affect(hero, Invisibility.class, 3f);
		}
	}

	/**
	 * Stealth multiplier: enemies less likely to detect MISERY in shadow cells
	 */
	public static float stealthMultiplier(Char ch) {
		if (!(ch instanceof Hero)) return 1f;
		MiseryShadowBlob blob = ((Hero) ch).buff(MiseryShadowBlob.class);
		if (blob != null && blob.isShadowCell(ch.pos)) {
			return 0.5f;
		}
		return 1f;
	}

	@Override
	public int icon() { return BuffIndicator.NONE; }
	@Override
	public void tintIcon(Image icon) { icon.hardlight(0.2f, 0.1f, 0.3f); }
	@Override
	public String desc() {
		return Messages.get(this, "desc", shadowCells.size());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		int[] arr = new int[shadowCells.size()];
		int i = 0;
		for (int c : shadowCells) arr[i++] = c;
		bundle.put("cells", arr);
	}
	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		shadowCells.clear();
		int[] arr = bundle.getIntArray("cells");
		if (arr != null) for (int c : arr) shadowCells.add(c);
	}
}

/** Temporary buff: next attack deals bonus assassination damage */
class ShadowStrikeBuff extends FlavourBuff {
	{
		type = buffType.POSITIVE;
	}
	@Override
	public int icon() { return BuffIndicator.WEAPON; }
}
