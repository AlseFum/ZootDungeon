package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * MANTRA class feature: spend wand charges to make a "declaration".
 * When enemies in FOV perform the declared action, the buff triggers
 * a wand-specific effect on them.
 *
 * Basic trigger types: ATTACK (enemy attacks), MOVE (enemy moves), CAST (enemy uses magic).
 * Trigger actions cause the infused wand's effect to proc on the enemy.
 */
public class MantraDeclarationBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	public enum TriggerType {
		ATTACK,  // triggers when enemy attacks
		MOVE,    // triggers when enemy moves
		CAST     // triggers when enemy uses magic/abilities
	}

	private TriggerType triggerType;
	private Class<? extends Wand> infusedWand;
	private int wandLevel = 1;
	private int triggerCount = 0;

	/** Create a declaration. Costs 2 wand charges. */
	public static void declare(final Hero hero, TriggerType type) {
		Wand wandToUse = null;
		for (com.zootdungeon.items.Item item : hero.belongings.backpack.items) {
			if (item instanceof Wand && ((Wand) item).curCharges >= 2) {
				wandToUse = (Wand) item;
				break;
			}
		}
		if (wandToUse == null) {
			GLog.w(Messages.get(MantraDeclarationBuff.class, "no_wand"));
			return;
		}
		wandToUse.curCharges -= 2;

		MantraDeclarationBuff buff = Buff.affect(hero, MantraDeclarationBuff.class, 20f);
		buff.triggerType = type;
		buff.infusedWand = wandToUse.getClass();
		buff.wandLevel = wandToUse.buffedLvl();
		buff.triggerCount = 0;

		GLog.p(Messages.get(MantraDeclarationBuff.class, "declared",
				Messages.get(type, "name"), Messages.get(wandToUse, "name")));
		hero.spendAndNext(hero.attackDelay());
	}

	/** Called when an enemy does something that matches the trigger */
	public void onTrigger(Char enemy, Char target) {
		if (infusedWand == null || triggerType == null) return;
		try {
			Wand w = infusedWand.getDeclaredConstructor().newInstance();
			w.level(wandLevel);
			w.wandProc(enemy, 1);
			triggerCount++;

			// Talent 3: splash to nearby enemies
			Hero hero = (Hero) this.target;
			if (hero.hasTalent(Talent.MANTRA_SPLASH_TRIGGER)) {
				int pts = hero.pointsInTalent(Talent.MANTRA_SPLASH_TRIGGER);
				float[] splashChances = {0f, 0.3f, 0.5f, 0.7f};
				for (Char ch : Actor.chars()) {
					if (ch != enemy && ch.alignment == Char.Alignment.ENEMY
							&& ch.isAlive() && Dungeon.level.adjacent(enemy.pos, ch.pos)) {
						if (Random.Float() < splashChances[pts]) {
							w.wandProc(ch, 1);
						}
					}
				}
			}

			// Talent 5: may extend buff
			if (hero.hasTalent(Talent.MANTRA_ETERNAL_BUFF)) {
				int pts = hero.pointsInTalent(Talent.MANTRA_ETERNAL_BUFF);
				float[] extendChances = {0f, 0.2f, 0.35f, 0.6f};
				if (Random.Float() < extendChances[pts]) {
					this.spend(-5f); // extend by 5 turns
					GLog.p(Messages.get(this, "extended"));
				}
			}
		} catch (Exception e) {
			// wand class not instantiatable
		}
	}

	public TriggerType getTriggerType() { return triggerType; }

	@Override
	public int icon() { return BuffIndicator.MIND_VISION; }
	@Override
	public void tintIcon(Image icon) { icon.hardlight(1f, 0.3f, 1f); }

	@Override
	public String desc() {
		if (infusedWand == null) return Messages.get(this, "desc_empty");
		return Messages.get(this, "desc",
				Messages.get(triggerType, "name"),
				Messages.get(infusedWand, "name"),
				triggerCount, dispTurns());
	}

	private static final String TRIGGER_TYPE = "triggerType";
	private static final String INFUSED_WAND = "infusedWand";
	private static final String WAND_LEVEL    = "wandLevel";
	private static final String TRIGGER_COUNT = "triggerCount";

	@Override
	@SuppressWarnings("unchecked")
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TRIGGER_TYPE, triggerType != null ? triggerType.name() : "");
		bundle.put(INFUSED_WAND, infusedWand != null ? infusedWand.getName() : "");
		bundle.put(WAND_LEVEL, wandLevel);
		bundle.put(TRIGGER_COUNT, triggerCount);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		String s = bundle.getString(TRIGGER_TYPE);
		if (!s.isEmpty()) triggerType = TriggerType.valueOf(s);
		s = bundle.getString(INFUSED_WAND);
		try {
			if (!s.isEmpty()) infusedWand = (Class<? extends Wand>) Class.forName(s);
		} catch (ClassNotFoundException e) {
			infusedWand = null;
		}
		wandLevel = bundle.getInt(WAND_LEVEL);
		triggerCount = bundle.getInt(TRIGGER_COUNT);
	}
}
