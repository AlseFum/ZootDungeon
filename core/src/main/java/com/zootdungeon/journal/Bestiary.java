/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zootdungeon.journal;

import com.zootdungeon.Badges;
import com.zootdungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.zootdungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.zootdungeon.actors.hero.abilities.rogue.ShadowClone;
import com.zootdungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.zootdungeon.actors.mobs.Acidic;
import com.zootdungeon.actors.mobs.Albino;
import com.zootdungeon.actors.mobs.ArmoredBrute;
import com.zootdungeon.actors.mobs.ArmoredStatue;
import com.zootdungeon.actors.mobs.Bandit;
import com.zootdungeon.actors.mobs.Bat;
import com.zootdungeon.actors.mobs.Bee;
import com.zootdungeon.actors.mobs.Brute;
import com.zootdungeon.actors.mobs.CausticSlime;
import com.zootdungeon.actors.mobs.Crab;
import com.zootdungeon.actors.mobs.CrystalGuardian;
import com.zootdungeon.actors.mobs.CrystalMimic;
import com.zootdungeon.actors.mobs.CrystalSpire;
import com.zootdungeon.actors.mobs.CrystalWisp;
import com.zootdungeon.actors.mobs.DM100;
import com.zootdungeon.actors.mobs.DM200;
import com.zootdungeon.actors.mobs.DM201;
import com.zootdungeon.actors.mobs.DM300;
import com.zootdungeon.actors.mobs.DemonSpawner;
import com.zootdungeon.actors.mobs.DwarfKing;
import com.zootdungeon.actors.mobs.EbonyMimic;
import com.zootdungeon.actors.mobs.Elemental;
import com.zootdungeon.actors.mobs.Eye;
import com.zootdungeon.actors.mobs.FetidRat;
import com.zootdungeon.actors.mobs.Ghoul;
import com.zootdungeon.actors.mobs.Gnoll;
import com.zootdungeon.actors.mobs.GnollGeomancer;
import com.zootdungeon.actors.mobs.GnollGuard;
import com.zootdungeon.actors.mobs.GnollSapper;
import com.zootdungeon.actors.mobs.GnollTrickster;
import com.zootdungeon.actors.mobs.GoldenMimic;
import com.zootdungeon.actors.mobs.Golem;
import com.zootdungeon.actors.mobs.Goo;
import com.zootdungeon.actors.mobs.GreatCrab;
import com.zootdungeon.actors.mobs.Guard;
import com.zootdungeon.actors.mobs.Mimic;
import com.zootdungeon.actors.mobs.Monk;
import com.zootdungeon.actors.mobs.Necromancer;
import com.zootdungeon.actors.mobs.PhantomPiranha;
import com.zootdungeon.actors.mobs.Piranha;
import com.zootdungeon.actors.mobs.Pylon;
import com.zootdungeon.actors.mobs.Rat;
import com.zootdungeon.actors.mobs.RipperDemon;
import com.zootdungeon.actors.mobs.RotHeart;
import com.zootdungeon.actors.mobs.RotLasher;
import com.zootdungeon.actors.mobs.Scorpio;
import com.zootdungeon.actors.mobs.Senior;
import com.zootdungeon.actors.mobs.Shaman;
import com.zootdungeon.actors.mobs.Skeleton;
import com.zootdungeon.actors.mobs.Slime;
import com.zootdungeon.actors.mobs.Snake;
import com.zootdungeon.actors.mobs.SpectralNecromancer;
import com.zootdungeon.actors.mobs.Spinner;
import com.zootdungeon.actors.mobs.Statue;
import com.zootdungeon.actors.mobs.Succubus;
import com.zootdungeon.actors.mobs.Swarm;
import com.zootdungeon.actors.mobs.Tengu;
import com.zootdungeon.actors.mobs.Thief;
import com.zootdungeon.actors.mobs.TormentedSpirit;
import com.zootdungeon.actors.mobs.Warlock;
import com.zootdungeon.actors.mobs.Wraith;
import com.zootdungeon.actors.mobs.YogDzewa;
import com.zootdungeon.actors.mobs.YogFist;
import com.zootdungeon.actors.mobs.npcs.Blacksmith;
import com.zootdungeon.actors.mobs.npcs.Ghost;
import com.zootdungeon.actors.mobs.npcs.Imp;
import com.zootdungeon.actors.mobs.npcs.MirrorImage;
import com.zootdungeon.actors.mobs.npcs.PrismaticImage;
import com.zootdungeon.actors.mobs.npcs.RatKing;
import com.zootdungeon.actors.mobs.npcs.Sheep;
import com.zootdungeon.actors.mobs.npcs.Shopkeeper;
import com.zootdungeon.actors.mobs.npcs.Wandmaker;
import com.zootdungeon.items.artifacts.DriedRose;
import com.zootdungeon.items.quest.CorpseDust;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.items.wands.WandOfRegrowth;
import com.zootdungeon.items.wands.WandOfWarding;
import com.zootdungeon.levels.rooms.special.SentryRoom;
import com.zootdungeon.levels.traps.AlarmTrap;
import com.zootdungeon.levels.traps.BlazingTrap;
import com.zootdungeon.levels.traps.BurningTrap;
import com.zootdungeon.levels.traps.ChillingTrap;
import com.zootdungeon.levels.traps.ConfusionTrap;
import com.zootdungeon.levels.traps.CorrosionTrap;
import com.zootdungeon.levels.traps.CursingTrap;
import com.zootdungeon.levels.traps.DisarmingTrap;
import com.zootdungeon.levels.traps.DisintegrationTrap;
import com.zootdungeon.levels.traps.DistortionTrap;
import com.zootdungeon.levels.traps.ExplosiveTrap;
import com.zootdungeon.levels.traps.FlashingTrap;
import com.zootdungeon.levels.traps.FlockTrap;
import com.zootdungeon.levels.traps.FrostTrap;
import com.zootdungeon.levels.traps.GatewayTrap;
import com.zootdungeon.levels.traps.GeyserTrap;
import com.zootdungeon.levels.traps.GnollRockfallTrap;
import com.zootdungeon.levels.traps.GrimTrap;
import com.zootdungeon.levels.traps.GrippingTrap;
import com.zootdungeon.levels.traps.GuardianTrap;
import com.zootdungeon.levels.traps.OozeTrap;
import com.zootdungeon.levels.traps.PitfallTrap;
import com.zootdungeon.levels.traps.PoisonDartTrap;
import com.zootdungeon.levels.traps.RockfallTrap;
import com.zootdungeon.levels.traps.ShockingTrap;
import com.zootdungeon.levels.traps.StormTrap;
import com.zootdungeon.levels.traps.SummoningTrap;
import com.zootdungeon.levels.traps.TeleportationTrap;
import com.zootdungeon.levels.traps.TenguDartTrap;
import com.zootdungeon.levels.traps.ToxicTrap;
import com.zootdungeon.levels.traps.WarpingTrap;
import com.zootdungeon.levels.traps.WeakeningTrap;
import com.zootdungeon.levels.traps.WornDartTrap;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.plants.BlandfruitBush;
import com.zootdungeon.plants.Blindweed;
import com.zootdungeon.plants.Earthroot;
import com.zootdungeon.plants.Fadeleaf;
import com.zootdungeon.plants.Firebloom;
import com.zootdungeon.plants.Icecap;
import com.zootdungeon.plants.Mageroyal;
import com.zootdungeon.plants.Rotberry;
import com.zootdungeon.plants.Sorrowmoss;
import com.zootdungeon.plants.Starflower;
import com.zootdungeon.plants.Stormvine;
import com.zootdungeon.plants.Sungrass;
import com.zootdungeon.plants.Swiftthistle;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

//contains all the game's various entities, mostly enemies, NPCS, and allies, but also traps and plants
public enum Bestiary {

	REGIONAL,
	BOSSES,
	UNIVERSAL,
	RARE,
	QUEST,
	NEUTRAL,
	ALLY,
	TRAP,
	PLANT;

	//tracks whether an entity has been encountered
	private final LinkedHashMap<Class<?>, Boolean> seen = new LinkedHashMap<>();
	//tracks enemy kills, trap activations, plant tramples, or just sets to 1 for seen on allies
	private final LinkedHashMap<Class<?>, Integer> encounterCount = new LinkedHashMap<>();

	//should only be used when initializing
	private void addEntities(Class<?>... classes ){
		for (Class<?> cls : classes){
			seen.put(cls, false);
			encounterCount.put(cls, 0);
		}
	}

	public Collection<Class<?>> entities(){
		return seen.keySet();
	}

	public String title(){
		return Messages.get(this, name() + ".title");
	}

	public int totalEntities(){
		return seen.size();
	}

	public int totalSeen(){
		int seenTotal = 0;
		for (boolean entitySeen : seen.values()){
			if (entitySeen) seenTotal++;
		}
		return seenTotal;
	}

	static {

		REGIONAL.addEntities(Rat.class, Snake.class, Gnoll.class, Swarm.class, Crab.class, Slime.class,
				Skeleton.class, Thief.class, DM100.class, Guard.class, Necromancer.class,
				Bat.class, Brute.class, Shaman.RedShaman.class, Shaman.BlueShaman.class, Shaman.PurpleShaman.class, Spinner.class, DM200.class,
				Ghoul.class, Elemental.FireElemental.class, Elemental.FrostElemental.class, Elemental.ShockElemental.class, Warlock.class, Monk.class, Golem.class,
				RipperDemon.class, DemonSpawner.class, Succubus.class, Eye.class, Scorpio.class);

		BOSSES.addEntities(Goo.class,
				Tengu.class,
				Pylon.class, DM300.class,
				DwarfKing.class,
				YogDzewa.Larva.class, YogFist.BurningFist.class, YogFist.SoiledFist.class, YogFist.RottingFist.class, YogFist.RustedFist.class,YogFist.BrightFist.class, YogFist.DarkFist.class, YogDzewa.class);

		UNIVERSAL.addEntities(Wraith.class, Piranha.class, Mimic.class, GoldenMimic.class, EbonyMimic.class, Statue.class, GuardianTrap.Guardian.class, SentryRoom.Sentry.class);

		RARE.addEntities(Albino.class, CausticSlime.class,
				Bandit.class, SpectralNecromancer.class,
				ArmoredBrute.class, DM201.class,
				Elemental.ChaosElemental.class, Senior.class,
				Acidic.class,
				TormentedSpirit.class, PhantomPiranha.class, CrystalMimic.class, ArmoredStatue.class);

		QUEST.addEntities(FetidRat.class, GnollTrickster.class, GreatCrab.class,
				Elemental.NewbornFireElemental.class, RotLasher.class, RotHeart.class,
				CrystalWisp.class, CrystalGuardian.class, CrystalSpire.class, GnollGuard.class, GnollSapper.class, GnollGeomancer.class);

		NEUTRAL.addEntities(Ghost.class, RatKing.class, Shopkeeper.class, Wandmaker.class, Blacksmith.class, Imp.class, Sheep.class, Bee.class);

		ALLY.addEntities(MirrorImage.class, PrismaticImage.class,
				DriedRose.GhostHero.class,
				WandOfWarding.Ward.class, WandOfWarding.Ward.WardSentry.class, WandOfLivingEarth.EarthGuardian.class,
				ShadowClone.ShadowAlly.class, SmokeBomb.NinjaLog.class, SpiritHawk.HawkAlly.class, PowerOfMany.LightAlly.class);

		TRAP.addEntities(WornDartTrap.class, PoisonDartTrap.class, DisintegrationTrap.class, GatewayTrap.class,
				ChillingTrap.class, BurningTrap.class, ShockingTrap.class, AlarmTrap.class, GrippingTrap.class, TeleportationTrap.class, OozeTrap.class,
				FrostTrap.class, BlazingTrap.class, StormTrap.class, GuardianTrap.class, FlashingTrap.class, WarpingTrap.class,
				ConfusionTrap.class, ToxicTrap.class, CorrosionTrap.class,
				FlockTrap.class, SummoningTrap.class, WeakeningTrap.class, CursingTrap.class,
				GeyserTrap.class, ExplosiveTrap.class, RockfallTrap.class, PitfallTrap.class,
				DistortionTrap.class, DisarmingTrap.class, GrimTrap.class);

		PLANT.addEntities(Rotberry.class, Sungrass.class, Fadeleaf.class, Icecap.class,
				Firebloom.class, Sorrowmoss.class, Swiftthistle.class, Blindweed.class,
				Stormvine.class, Earthroot.class, Mageroyal.class, Starflower.class,
				BlandfruitBush.class,
				WandOfRegrowth.Dewcatcher.class, WandOfRegrowth.Seedpod.class, WandOfRegrowth.Lotus.class);

	}

	//some mobs and traps have different internal classes in some cases, so need to convert here
	private static final HashMap<Class<?>, Class<?>> classConversions = new HashMap<>();
	static {
		classConversions.put(CorpseDust.DustWraith.class,      Wraith.class);

		classConversions.put(Necromancer.NecroSkeleton.class,  Skeleton.class);

		classConversions.put(TenguDartTrap.class,              PoisonDartTrap.class);
		classConversions.put(GnollRockfallTrap.class,          RockfallTrap.class);

		classConversions.put(DwarfKing.DKGhoul.class,          Ghoul.class);
		classConversions.put(DwarfKing.DKWarlock.class,        Warlock.class);
		classConversions.put(DwarfKing.DKMonk.class,           Monk.class);
		classConversions.put(DwarfKing.DKGolem.class,          Golem.class);

		classConversions.put(YogDzewa.YogRipper.class,         RipperDemon.class);
		classConversions.put(YogDzewa.YogEye.class,            Eye.class);
		classConversions.put(YogDzewa.YogScorpio.class,        Scorpio.class);
	}

	public static boolean isSeen(Class<?> cls){
		for (Bestiary cat : values()) {
			if (cat.seen.containsKey(cls)) {
				return cat.seen.get(cls);
			}
		}
		return false;
	}

	public static void setSeen(Class<?> cls){
		if (classConversions.containsKey(cls)){
			cls = classConversions.get(cls);
		}
		for (Bestiary cat : values()) {
			if (cat.seen.containsKey(cls) && !cat.seen.get(cls)) {
				cat.seen.put(cls, true);
				Journal.saveNeeded = true;
			}
		}
		Badges.validateCatalogBadges();
	}

	public static int encounterCount(Class<?> cls) {
		for (Bestiary cat : values()) {
			if (cat.encounterCount.containsKey(cls)) {
				return cat.encounterCount.get(cls);
			}
		}
		return 0;
	}

	//used primarily when bosses are killed and need to clean up their minions
	public static boolean skipCountingEncounters = false;

	public static void countEncounter(Class<?> cls){
		countEncounters(cls, 1);
	}

	public static void countEncounters(Class<?> cls, int encounters){
		if (skipCountingEncounters){
			return;
		}
		if (classConversions.containsKey(cls)){
			cls = classConversions.get(cls);
		}
		for (Bestiary cat : values()) {
			if (cat.encounterCount.containsKey(cls) && cat.encounterCount.get(cls) != Integer.MAX_VALUE){
				cat.encounterCount.put(cls, cat.encounterCount.get(cls)+encounters);
				if (cat.encounterCount.get(cls) < -1_000_000_000){ //to catch cases of overflow
					cat.encounterCount.put(cls, Integer.MAX_VALUE);
				}
				Journal.saveNeeded = true;
			}
		}
	}

	private static final String BESTIARY_CLASSES    = "bestiary_classes";
	private static final String BESTIARY_SEEN       = "bestiary_seen";
	private static final String BESTIARY_ENCOUNTERS = "bestiary_encounters";

	public static void store( Bundle bundle ){

		ArrayList<Class<?>> classes = new ArrayList<>();
		ArrayList<Boolean> seen = new ArrayList<>();
		ArrayList<Integer> encounters = new ArrayList<>();

		for (Bestiary cat : values()) {
			for (Class<?> entity : cat.entities()) {
				if (cat.seen.get(entity) || cat.encounterCount.get(entity) > 0){
					classes.add(entity);
					seen.add(cat.seen.get(entity));
					encounters.add(cat.encounterCount.get(entity));
				}
			}
		}

		Class<?>[] storeCls = new Class[classes.size()];
		boolean[] storeSeen = new boolean[seen.size()];
		int[] storeEncounters = new int[encounters.size()];

		for (int i = 0; i < storeCls.length; i++){
			storeCls[i] = classes.get(i);
			storeSeen[i] = seen.get(i);
			storeEncounters[i] = encounters.get(i);
		}

		bundle.put( BESTIARY_CLASSES, storeCls );
		bundle.put( BESTIARY_SEEN, storeSeen );
		bundle.put( BESTIARY_ENCOUNTERS, storeEncounters );

	}

	public static void restore( Bundle bundle ){

		if (bundle.contains(BESTIARY_CLASSES)
				&& bundle.contains(BESTIARY_SEEN)
				&& bundle.contains(BESTIARY_ENCOUNTERS)){
			Class<?>[] classes = bundle.getClassArray(BESTIARY_CLASSES);
			boolean[] seen = bundle.getBooleanArray(BESTIARY_SEEN);
			int[] encounters = bundle.getIntArray(BESTIARY_ENCOUNTERS);

			for (int i = 0; i < classes.length; i++){
				for (Bestiary cat : values()){
					if (cat.seen.containsKey(classes[i])){
						cat.seen.put(classes[i], seen[i]);
						cat.encounterCount.put(classes[i], encounters[i]);
					}
				}
			}
		}

	}

}
