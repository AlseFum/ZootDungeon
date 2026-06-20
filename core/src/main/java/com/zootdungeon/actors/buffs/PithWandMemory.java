package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * PITH class feature: remembers the last wand used to modify the next wand's effect.
 * Cleared after the second wand use.
 */
public class PithWandMemory extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	/** The class name of the memorized wand */
	private Class<? extends Wand> memorizedWand = null;
	/** The first wand in a 3-chain (for talent 4 level 2+) */
	private Class<? extends Wand> firstWand = null;
	private Class<? extends Wand> secondWand = null;

	public Class<? extends Wand> getMemorized() {
		return memorizedWand;
	}

	public Class<? extends Wand> getFirstWand() {
		return firstWand;
	}

	public Class<? extends Wand> getSecondWand() {
		return secondWand;
	}

	/** Memorize a wand. If there's already one memorized, shifts to 3-chain if applicable. */
	public void memorize(Wand wand) {
		Hero hero = (Hero) target;
		if (hero.pointsInTalent(com.zootdungeon.actors.hero.Talent.PITH_CHAIN_STRONGER) >= 2) {
			// 3-chain mode
			if (secondWand != null && firstWand != null) {
				// Third wand in chain: cycle - third becomes first of next
				if (hero.pointsInTalent(com.zootdungeon.actors.hero.Talent.PITH_CHAIN_STRONGER) >= 3) {
					Class<? extends Wand> third = wand.getClass();
					firstWand = third;
					secondWand = null;
				} else {
					firstWand = null;
					secondWand = null;
				}
			} else if (firstWand != null && secondWand == null) {
				secondWand = wand.getClass();
			} else {
				firstWand = memorizedWand;
				secondWand = null;
			}
		}
		memorizedWand = wand.getClass();
		BuffIndicator.refreshHero();
	}

	/** Clear memory (called after second wand use in basic mode) */
	public void clearMemory() {
		if (firstWand == null && secondWand == null) {
			memorizedWand = null;
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.5f, 0f, 1f);
	}

	@Override
	public String desc() {
		if (memorizedWand == null) return Messages.get(this, "desc_empty");
		String name = Messages.get(memorizedWand, "name");
		if (firstWand != null && secondWand != null) {
			return Messages.get(this, "desc_chain", name,
				Messages.get(firstWand, "name"), Messages.get(secondWand, "name"));
		}
		if (firstWand != null) {
			return Messages.get(this, "desc_pair", name,
				Messages.get(firstWand, "name"));
		}
		return Messages.get(this, "desc_one", name);
	}

	private static final String MEMORIZED = "memorized";
	private static final String FIRST     = "first";
	private static final String SECOND    = "second";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MEMORIZED, memorizedWand != null ? memorizedWand.getName() : "");
		bundle.put(FIRST, firstWand != null ? firstWand.getName() : "");
		bundle.put(SECOND, secondWand != null ? secondWand.getName() : "");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			String s = bundle.getString(MEMORIZED);
			if (!s.isEmpty()) memorizedWand = (Class<? extends Wand>) Class.forName(s);
			s = bundle.getString(FIRST);
			if (!s.isEmpty()) firstWand = (Class<? extends Wand>) Class.forName(s);
			s = bundle.getString(SECOND);
			if (!s.isEmpty()) secondWand = (Class<? extends Wand>) Class.forName(s);
		} catch (ClassNotFoundException e) {
			memorizedWand = null;
			firstWand = null;
			secondWand = null;
		}
	}
}
