package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * LOGOS class feature: spend wand charges to draw runes on the floor.
 * Basic rune types: FIRE (damage enemies), FROST (slow/chill), WARD (shield allies).
 * Talent LOGOS_RUNE_POWER makes runes stronger.
 */
public class LogosRuneBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	public enum RuneType {
		FIRE,
		FROST,
		WARD
	}

	/** Draw a rune at the target cell. Costs 1 wand charge. */
	public static void drawRune(final Hero hero, final RuneType type, final int cell) {
		// Find a wand with charges to consume
		Wand wandToUse = null;
		for (com.zootdungeon.items.Item item : hero.belongings.backpack.items) {
			if (item instanceof Wand && ((Wand) item).curCharges > 0) {
				wandToUse = (Wand) item;
				break;
			}
		}
		if (wandToUse == null) {
			com.zootdungeon.utils.GLog.w(Messages.get(LogosRuneBuff.class, "no_wand"));
			return;
		}
		wandToUse.curCharges--;

		int pts = hero.hasTalent(Talent.LOGOS_RUNE_POWER)
				? hero.pointsInTalent(Talent.LOGOS_RUNE_POWER) : 0;
		float powerMult = 1f + pts * 0.5f;

		// Visual effect
		CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 3 + pts);

		// Apply rune effect
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.distance(cell, ch.pos) <= 1) {
				switch (type) {
					case FIRE:
						ch.damage(Math.round((5 + 3 * pts) * powerMult), hero);
						Buff.affect(ch, Burning.class).reignite(ch, 4f + 2f * pts);
						break;
					case FROST:
						ch.damage(Math.round((3 + 2 * pts) * powerMult), hero);
						Buff.prolong(ch, Chill.class, 5f + 2f * pts);
						break;
					case WARD:
						// Ward gives the hero barrier
						Barrier barrier = Buff.affect(hero, Barrier.class);
						barrier.setShield(Math.round((3 + 2 * pts) * powerMult));
						break;
				}
			}
		}
		hero.spendAndNext(hero.attackDelay());
	}

	@Override
	public int icon() { return BuffIndicator.NONE; }
	@Override
	public void tintIcon(Image icon) { icon.hardlight(0.2f, 0.5f, 1f); }
}
