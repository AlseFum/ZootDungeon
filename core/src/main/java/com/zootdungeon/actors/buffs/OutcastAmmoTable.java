package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.potions.PotionOfLiquidFlame;
import com.zootdungeon.items.potions.PotionOfFrost;
import com.zootdungeon.items.potions.PotionOfToxicGas;
import com.zootdungeon.items.scrolls.ScrollOfRecharging;
import com.zootdungeon.items.stones.*;
import com.zootdungeon.items.wands.Wand;

/**
 * OUTCAST talent: special ammo table.
 * Consume items to add special effects to the next shot.
 *
 * EXAMPLE TABLE (expand as needed):
 *   Firebloom seed     → next shot applies Burning (4 turns) + bonus fire damage
 *   Sorrowmoss seed    → next shot applies Poison (6 turns)
 *   Icecap seed        → next shot applies Chill (5 turns)
 *   Liquid Flame potion → next shot explodes in 3x3 AOE fire
 *   Frost potion       → next shot freezes target (2 turns)
 *   Toxic Gas potion   → next shot applies Corrosion
 *   Wand charge (any)  → next shot deals extra magic damage = wandLevel * 3
 */
public class OutcastAmmoTable {

	/** Returns ammo effect description, or null if item not valid fuel */
	public static String getEffectDescription(Item item, Hero hero) {
		if (item instanceof Wand && ((Wand) item).curCharges > 0) {
			Wand w = (Wand) item;
			return "Magic Shot: +" + (w.buffedLvl() * 3) + " bonus damage";
		}
		// Seeds
		String clsName = item.getClass().getSimpleName();
		switch (clsName) {
			case "Firebloom$Seed":  return "Flame Shot: applies Burning (4 turns), +3 fire damage";
			case "Sorrowmoss$Seed": return "Toxic Shot: applies Poison (6 turns)";
			case "Icecap$Seed":     return "Frost Shot: applies Chill (5 turns)";
			case "Blindweed$Seed":  return "Blind Shot: applies Blindness (3 turns)";
			case "Stormvine$Seed":  return "Shock Shot: applies Vertigo (4 turns)";
		}
		// Potions
		if (item instanceof PotionOfLiquidFlame) return "Inferno Shot: 3x3 fire AOE at target";
		if (item instanceof PotionOfFrost)       return "Freeze Shot: freezes target for 2 turns";
		if (item instanceof PotionOfToxicGas)    return "Gas Shot: applies Corrosion at target";
		// Scrolls
		if (item instanceof ScrollOfRecharging)  return "Energy Shot: recharges 2 wand charges on hit";
		// Stones
		if (item instanceof StoneOfBlink)       return "Blink Shot: teleport to target after hit";
		if (item instanceof StoneOfAggression)  return "Aggro Shot: provokes target for 5 turns";
		if (item instanceof StoneOfFlock)       return "Flock Shot: spawns 4 sheep around target";

		return null; // not valid fuel
	}

	/** Apply the ammo effect. Returns extra damage dealt. */
	public static int applyEffect(Item item, Hero hero, Char target) {
		String clsName = item.getClass().getSimpleName();
		int extraDmg = 0;

		switch (clsName) {
			case "Firebloom$Seed":
				Buff.affect(target, Burning.class).reignite(target, 4f);
				extraDmg = 3;
				break;
			case "Sorrowmoss$Seed":
				Buff.affect(target, Poison.class).extend(6f);
				break;
			case "Icecap$Seed":
				Buff.prolong(target, Chill.class, 5f);
				break;
			case "Blindweed$Seed":
				Buff.prolong(target, Blindness.class, 3f);
				break;
			case "Stormvine$Seed":
				Buff.prolong(target, Vertigo.class, 4f);
				break;
		}
		if (item instanceof PotionOfLiquidFlame) {
			for (Char ch : com.zootdungeon.actors.Actor.chars()) {
				if (ch.alignment == Char.Alignment.ENEMY
						&& com.zootdungeon.Dungeon.level.distance(target.pos, ch.pos) <= 1) {
					Buff.affect(ch, Burning.class).reignite(ch, 4f);
					ch.damage(5, hero);
				}
			}
			extraDmg = 5;
		}
		if (item instanceof PotionOfFrost) {
			Buff.prolong(target, Chill.class, 10f);
			extraDmg = 3;
		}
		if (item instanceof PotionOfToxicGas) {
			Buff.affect(target, Corrosion.class).set(5f, 3);
		}
		if (item instanceof ScrollOfRecharging) {
			for (Item i : hero.belongings.backpack.items) {
				if (i instanceof Wand) ((Wand) i).gainCharge(2f);
			}
		}
		if (item instanceof Wand) {
			Wand w = (Wand) item;
			extraDmg = w.buffedLvl() * 3;
		}
		item.detach(hero.belongings.backpack);
		return extraDmg;
	}
}
