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

package com.zootdungeon.items.scrolls;

import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Roots;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.npcs.MirrorImage;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.EnergyParticle;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.items.EquipableItem;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.rings.Ring;
import com.zootdungeon.items.trinkets.ShardOfOblivion;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.levels.RegularLevel;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.rooms.secret.SecretRoom;
import com.zootdungeon.levels.rooms.special.SpecialRoom;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Static utility class containing all scroll special effects as pure functions.
 */
public class ScrollEffects {

	//**********************************************************************************
	//*** Section 1: Teleportation (from ScrollOfTeleportation)
	//**********************************************************************************

	public static boolean teleportToLocation(Char ch, int pos){
		PathFinder.buildDistanceMap(pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
		if (PathFinder.distance[ch.pos] == Integer.MAX_VALUE
				|| (!Dungeon.level.passable[pos] && !Dungeon.level.avoid[pos])
				|| (Actor.findChar(pos) != null && Actor.findChar(pos) != ch)){
			if (ch == Dungeon.hero){
				GLog.w( Messages.get(ScrollOfTeleportation.class, "cant_reach") );
			}
			return false;
		}

		appear( ch, pos );
		Dungeon.level.occupyCell( ch );
		Buff.detach(ch, Roots.class);
		if (ch == Dungeon.hero) {
			Dungeon.observe();
			GameScene.updateFog();
		}
		return true;
	}

	public static boolean teleportChar( Char ch ) {
		return teleportChar( ch, ScrollOfTeleportation.class );
	}

	public static boolean teleportChar( Char ch, Class source ) {

		if (!(Dungeon.level instanceof RegularLevel)){
			return teleportInNonRegularLevel( ch, false );
		}

		if (Char.hasProp(ch, Char.Property.IMMOVABLE) || ch.isImmune(source)){
			GLog.w( Messages.get(ScrollOfTeleportation.class, "no_tele") );
			return false;
		}

		int count = 20;
		int pos;
		do {
			pos = Dungeon.level.randomRespawnCell( ch );
			if (count-- <= 0) {
				break;
			}
		} while (pos == -1 || Dungeon.level.secret[pos]);

		if (pos == -1) {

			GLog.w( Messages.get(ScrollOfTeleportation.class, "no_tele") );
			return false;

		} else {

			appear( ch, pos );
			Dungeon.level.occupyCell( ch );
			Buff.detach(ch, Roots.class);

			if (ch == Dungeon.hero) {
				GLog.i( Messages.get(ScrollOfTeleportation.class, "tele") );

				Dungeon.observe();
				GameScene.updateFog();
				Dungeon.hero.interrupt();
			}
			return true;

		}
	}

	public static boolean teleportPreferringUnseen( Hero hero ){

		if (!(Dungeon.level instanceof RegularLevel)){
			return teleportInNonRegularLevel( hero, true );
		}

		RegularLevel level = (RegularLevel) Dungeon.level;
		ArrayList<Integer> candidates = new ArrayList<>();

		for (Room r : level.rooms()){
			if (r instanceof SpecialRoom){
				int terr;
				boolean locked = false;
				for (Point p : r.getPoints()){
					terr = level.map[level.pointToCell(p)];
					if (terr == Terrain.LOCKED_DOOR || terr == Terrain.CRYSTAL_DOOR || terr == Terrain.BARRICADE){
						locked = true;
						break;
					}
				}
				if (locked){
					continue;
				}
			}

			int cell;
			for (Point p : r.charPlaceablePoints(level)){
				cell = level.pointToCell(p);
				if (level.passable[cell] && !level.visited[cell] && !level.secret[cell] && Actor.findChar(cell) == null){
					candidates.add(cell);
				}
			}
		}

		if (candidates.isEmpty()){
			return teleportChar( hero );
		} else {
			int pos = Random.element(candidates);
			boolean secretDoor = false;
			int doorPos = -1;
			if (level.room(pos) instanceof SpecialRoom){
				SpecialRoom room = (SpecialRoom) level.room(pos);
				if (room.entrance() != null){
					doorPos = level.pointToCell(room.entrance());
					for (int i : PathFinder.NEIGHBOURS8){
						if (!room.inside(level.cellToPoint(doorPos + i))
								&& level.passable[doorPos + i]
								&& Actor.findChar(doorPos + i) == null){
							secretDoor = room instanceof SecretRoom;
							pos = doorPos + i;
							break;
						}
					}
				}
			}
			GLog.i( Messages.get(ScrollOfTeleportation.class, "tele") );
			appear( hero, pos );
			Dungeon.level.occupyCell( hero );
			Buff.detach(hero, Roots.class);
			if (secretDoor && level.map[doorPos] == Terrain.SECRET_DOOR){
				Sample.INSTANCE.play( Assets.Sounds.SECRET );
				int oldValue = Dungeon.level.map[doorPos];
				GameScene.discoverTile( doorPos, oldValue );
				Dungeon.level.discover( doorPos );
				discover( doorPos );
			}
			Dungeon.observe();
			GameScene.updateFog();
			return true;
		}

	}

	//teleports to a random pathable location on the floor
	//prefers not seen(optional) > not visible > visible
	static boolean teleportInNonRegularLevel(Char ch, boolean preferNotSeen ){

		if (Char.hasProp(ch, Char.Property.IMMOVABLE)){
			GLog.w( Messages.get(ScrollOfTeleportation.class, "no_tele") );
			return false;
		}

		ArrayList<Integer> visibleValid = new ArrayList<>();
		ArrayList<Integer> notVisibleValid = new ArrayList<>();
		ArrayList<Integer> notSeenValid = new ArrayList<>();

		boolean[] passable = Dungeon.level.passable;

		if (Char.hasProp(ch, Char.Property.LARGE)){
			passable = BArray.and(passable, Dungeon.level.openSpace, null);
		}

		PathFinder.buildDistanceMap(ch.pos, passable);

		for (int i = 0; i < Dungeon.level.length(); i++){
			if (PathFinder.distance[i] < Integer.MAX_VALUE
					&& !Dungeon.level.secret[i]
					&& Actor.findChar(i) == null){
				if (preferNotSeen && !Dungeon.level.visited[i]){
					notSeenValid.add(i);
				} else if (Dungeon.level.heroFOV[i]){
					visibleValid.add(i);
				} else {
					notVisibleValid.add(i);
				}
			}
		}

		int pos;

		if (!notSeenValid.isEmpty()){
			pos = Random.element(notSeenValid);
		} else if (!notVisibleValid.isEmpty()){
			pos = Random.element(notVisibleValid);
		} else if (!visibleValid.isEmpty()){
			pos = Random.element(visibleValid);
		} else {
			GLog.w( Messages.get(ScrollOfTeleportation.class, "no_tele") );
			return false;
		}

		appear( ch, pos );
		Dungeon.level.occupyCell( ch );

		Buff.detach(ch, Roots.class);

		if (ch == Dungeon.hero) {
			GLog.i( Messages.get(ScrollOfTeleportation.class, "tele") );

			Dungeon.observe();
			GameScene.updateFog();
			Dungeon.hero.interrupt();
		}

		return true;

	}

	public static void appear( Char ch, int pos ) {

		ch.sprite.interruptMotion();

		if (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[ch.pos]){
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		}

		if (Dungeon.level.heroFOV[ch.pos] && ch != Dungeon.hero ) {
			CellEmitter.get(ch.pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);
		}

		ch.move( pos, false );
		if (ch.pos == pos) {
			ch.sprite.interruptMotion();
			ch.sprite.place(pos);
		}

		if (ch.invisible == 0) {
			ch.sprite.alpha( 0 );
			ch.sprite.parent.add( new AlphaTweener( ch.sprite, 1, 0.4f ) );
		}

		if (Dungeon.level.heroFOV[pos] || ch == Dungeon.hero ) {
			ch.sprite.emitter().start(Speck.factory(Speck.LIGHT), 0.2f, 3);
		}
	}

	//just plays the VFX for teleporting, without any position changes, does re-press cells though
	public static void appearVFX( Char ch ){
		if (Dungeon.level.heroFOV[ch.pos]){
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		}

		Dungeon.level.occupyCell(ch);

		if (ch.invisible == 0) {
			ch.sprite.alpha( 0 );
			ch.sprite.parent.add( new AlphaTweener( ch.sprite, 1, 0.4f ) );
		}

		if (Dungeon.level.heroFOV[ch.pos]) {
			ch.sprite.emitter().start(Speck.factory(Speck.LIGHT), 0.2f, 3);
		}
	}

	//**********************************************************************************
	//*** Section 2: Recharging (from ScrollOfRecharging)
	//**********************************************************************************

	public static void charge( Char user ) {
		if (user.sprite != null) {
			Emitter e = user.sprite.centerEmitter();
			if (e != null) e.burst(EnergyParticle.FACTORY, 15);
		}
	}

	//**********************************************************************************
	//*** Section 3: Magic Mapping (from ScrollOfMagicMapping)
	//**********************************************************************************

	public static void discover( int cell ) {
		CellEmitter.get( cell ).start( Speck.factory( Speck.DISCOVER ), 0.1f, 4 );
	}

	//**********************************************************************************
	//*** Section 4: Upgrade (from ScrollOfUpgrade)
	//**********************************************************************************

	public static void upgradeVFX( Hero hero ) {
		hero.sprite.emitter().start( Speck.factory( Speck.UP ), 0.2f, 3 );
	}

	public static void weakenCurseVFX( Hero hero ){
		GLog.p( Messages.get(ScrollOfUpgrade.class, "weaken_curse") );
		hero.sprite.emitter().start( ShadowParticle.UP, 0.05f, 5 );
	}

	public static void removeCurseVFX( Hero hero ){
		GLog.p( Messages.get(ScrollOfUpgrade.class, "remove_curse") );
		hero.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
		Badges.validateClericUnlock();
	}

	//**********************************************************************************
	//*** Section 5: RemoveCurse (from ScrollOfRemoveCurse)
	//**********************************************************************************

	public static boolean uncursable( Item item ){
		if (item.isEquipped(Dungeon.hero) && Dungeon.hero.buff(com.zootdungeon.actors.buffs.Degrade.class) != null) {
			return true;
		} if ((item instanceof EquipableItem || item instanceof Wand) && ((!item.isIdentified() && !item.cursedKnown) || item.cursed)){
			return true;
		} else if (item instanceof Weapon){
			return ((Weapon)item).hasCurseEnchant();
		} else if (item instanceof Armor){
			return ((Armor)item).hasCurseGlyph();
		} else {
			return false;
		}
	}

	public static boolean uncurse( Hero hero, Item... items ) {

		boolean procced = false;
		for (Item item : items) {
			if (item != null) {
				item.cursedKnown = true;
				if (item.cursed) {
					procced = true;
					item.cursed = false;
				}
			}
			if (item instanceof Weapon){
				Weapon w = (Weapon) item;
				if (w.hasCurseEnchant()){
					w.enchant(null);
					procced = true;
				}
			}
			if (item instanceof Armor){
				Armor a = (Armor) item;
				if (a.hasCurseGlyph()){
					a.inscribe(null);
					procced = true;
				}
			}
			if (item instanceof Wand){
				((Wand) item).updateLevel();
			}
		}

		if (procced) {
			if (hero != null) {
				hero.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10);
				hero.updateHT(false); //for ring of might
				Item.updateQuickslot();
			}

			Badges.validateClericUnlock();
		}

		return procced;
	}

	//**********************************************************************************
	//*** Section 6: Identify (from ScrollOfIdentify)
	//**********************************************************************************

	public static void IDItem( Item item ){
		if (ShardOfOblivion.passiveIDDisabled()) {
			if (item instanceof Weapon){
				((Weapon) item).setIDReady();
				GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				return;
			} else if (item instanceof Armor){
				((Armor) item).setIDReady();
				GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				return;
			} else if (item instanceof Ring){
				((Ring) item).setIDReady();
				GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				return;
			} else if (item instanceof Wand){
				((Wand) item).setIDReady();
				GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), item.name());
				return;
			}
		}

		item.identify();
		GLog.i(Messages.get(ScrollOfIdentify.class, "it_is", item.title()));
		Badges.validateItemLevelAquired( item );
	}

	//**********************************************************************************
	//*** Section 7: MirrorImage (from ScrollOfMirrorImage)
	//**********************************************************************************

	public static int spawnImages( Hero hero, int nImages ){
		return spawnImages( hero, hero.pos, nImages);
	}

	//returns the number of images spawned
	public static int spawnImages( Hero hero, int pos, int nImages ){

		ArrayList<Integer> respawnPoints = new ArrayList<>();

		for (int i = 0; i < PathFinder.NEIGHBOURS9.length; i++) {
			int p = pos + PathFinder.NEIGHBOURS9[i];
			if (Actor.findChar( p ) == null && Dungeon.level.passable[p]) {
				respawnPoints.add( p );
			}
		}

		int spawned = 0;
		while (nImages > 0 && !respawnPoints.isEmpty()) {
			int index = Random.index( respawnPoints );

			MirrorImage mob = new MirrorImage();
			mob.duplicate( hero );
			GameScene.add( mob );
			ScrollEffects.appear( mob, respawnPoints.get( index ) );

			respawnPoints.remove( index );
			nImages--;
			spawned++;
		}

		return spawned;
	}

}
