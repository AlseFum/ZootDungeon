package com.zootdungeon.traits;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Electricity;
import com.zootdungeon.actors.buffs.*;
import com.zootdungeon.actors.mobs.Elemental;
import com.zootdungeon.actors.blobs.ToxicGas;
import com.zootdungeon.items.armor.glyphs.Potential;
import com.zootdungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.zootdungeon.items.scrolls.ScrollOfRetribution;
import com.zootdungeon.items.wands.WandOfFireblast;
import com.zootdungeon.items.wands.WandOfFrost;
import com.zootdungeon.items.wands.WandOfLightning;
import com.zootdungeon.items.weapon.enchantments.Blazing;
import com.zootdungeon.items.weapon.enchantments.Grim;
import com.zootdungeon.items.weapon.enchantments.Shocking;
import com.zootdungeon.items.weapon.missiles.darts.ShockingDart;
import com.zootdungeon.levels.traps.GrimTrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bridges between legacy Char.Property and new trait ids,
 * and registers built-in trait definitions corresponding to those properties.
 *
 * This enables incremental migration: old property checks continue to work,
 * while new code can use traits directly.
 */
public final class PropertyTraitBridge {

	private static final Map<String, Char.Property> TRAIT_TO_PROPERTY = new HashMap<>();

	static {
		// Register built-in traits that correspond to old properties
		registerPropertyTrait("boss", Char.Property.BOSS,
				setOf(Grim.class, GrimTrap.class, ScrollOfRetribution.class, ScrollOfPsionicBlast.class),
				setOf(AllyBuff.class, Dread.class));

		registerPropertyTrait("miniboss", Char.Property.MINIBOSS,
				setOf(), setOf(AllyBuff.class, Dread.class));

		registerPropertyTrait("boss_minion", Char.Property.BOSS_MINION, setOf(), setOf());
		registerPropertyTrait("undead", Char.Property.UNDEAD, setOf(), setOf());
		registerPropertyTrait("demonic", Char.Property.DEMONIC, setOf(), setOf());

		registerPropertyTrait("inorganic", Char.Property.INORGANIC,
				setOf(), setOf(Bleeding.class, ToxicGas.class, Poison.class));

		registerPropertyTrait("fiery", Char.Property.FIERY,
				setOf(WandOfFireblast.class, Elemental.FireElemental.class),
				setOf(Burning.class, Blazing.class));

		registerPropertyTrait("icy", Char.Property.ICY,
				setOf(WandOfFrost.class, Elemental.FrostElemental.class),
				setOf(Frost.class, Chill.class));

		registerPropertyTrait("acidic", Char.Property.ACIDIC,
				setOf(Corrosion.class),
				setOf(Ooze.class));

		registerPropertyTrait("electric", Char.Property.ELECTRIC,
				setOf(WandOfLightning.class, Shocking.class, Potential.class,
						Electricity.class, ShockingDart.class, Elemental.ShockElemental.class),
				setOf());

		registerPropertyTrait("large", Char.Property.LARGE, setOf(), setOf());

		registerPropertyTrait("immovable", Char.Property.IMMOVABLE,
				setOf(), setOf(Vertigo.class));

		registerPropertyTrait("static", Char.Property.STATIC,
				setOf(),
				setOf(AllyBuff.class, Dread.class, Terror.class, Amok.class, Charm.class, Sleep.class,
						Paralysis.class, Frost.class, Chill.class, Slow.class, Speed.class));
	}

	private PropertyTraitBridge() {}

	private static <T> Set<Class<?>> setOf(Class<?>... classes) {
		HashSet<Class<?>> s = new HashSet<>();
		for (Class<?> c : classes) s.add(c);
		return s;
	}

	private static void registerPropertyTrait(String traitId, Char.Property property,
	                                         Set<Class<?>> defaultResists, Set<Class<?>> defaultImmunes) {
		TRAIT_TO_PROPERTY.put(traitId, property);

		TraitDefinition.Builder b = TraitDefinition.define(traitId);
		for (Class<?> c : defaultResists) b.defaultResistance(c);
		for (Class<?> c : defaultImmunes) b.defaultImmunity(c);
		// Register with registry for dynamic availability
		TraitRegistry.registerOrUpdate(b.build());
	}

	public static Char.Property propertyForTrait(String traitId) {
		return TRAIT_TO_PROPERTY.get(traitId);
	}
}


