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

package com.zootdungeon.items.wands;

import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.CorrosiveGas;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.buffs.Amok;
import com.zootdungeon.actors.buffs.Barrier;
import com.zootdungeon.actors.buffs.Blindness;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Charm;
import com.zootdungeon.actors.buffs.Chill;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.Poison;
import com.zootdungeon.actors.buffs.Roots;
import com.zootdungeon.actors.buffs.SoulMark;
import com.zootdungeon.actors.buffs.Frost;
import com.zootdungeon.actors.buffs.Ooze;
import com.zootdungeon.actors.buffs.Paralysis;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Effects;
import com.zootdungeon.effects.FloatingText;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.effects.Pushing;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.BloodParticle;
import com.zootdungeon.effects.particles.CorrosionParticle;
import com.zootdungeon.effects.particles.PurpleParticle;
import com.zootdungeon.effects.particles.RainbowParticle;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mimic;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.weapon.enchantments.Blazing;
import com.zootdungeon.items.weapon.enchantments.Elastic;
import com.zootdungeon.items.weapon.enchantments.Shocking;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import com.zootdungeon.levels.features.Door;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.plants.Sungrass;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.tiles.DungeonTilemap;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

/**
 * Static utility class containing all wand special effects as pure functions.
 */
public class WandEffects {

	//**********************************************************************************
	//*** Section 1: Knockback & Blast Wave (from WandOfBlastWave)
	//**********************************************************************************

	public static class Knockback {}

	/**
	 * Throws a character along a trajectory, with optional collision damage and door closing.
	 */
	public static void knockback(final Char ch, final Ballistica trajectory, int power,
	                             boolean closeDoors, boolean collideDmg, Object cause){
		if (ch.properties().contains(Char.Property.BOSS)) {
			power = (power+1)/2;
		}

		int dist = Math.min(trajectory.dist, power);

		boolean collided = dist == trajectory.dist;

		if (dist <= 0
				|| ch.rooted
				|| ch.properties().contains(Char.Property.IMMOVABLE)) return;

		//large characters cannot be moved into non-open space
		if (Char.hasProp(ch, Char.Property.LARGE)) {
			for (int i = 1; i <= dist; i++) {
				if (!Dungeon.level.openSpace[trajectory.path.get(i)]){
					dist = i-1;
					collided = true;
					break;
				}
			}
		}

		if (Actor.findChar(trajectory.path.get(dist)) != null){
			dist--;
			collided = true;
		}

		if (dist < 0) return;

		final int newPos = trajectory.path.get(dist);

		if (newPos == ch.pos) return;

		final int finalDist = dist;
		final boolean finalCollided = collided && collideDmg;
		final int initialpos = ch.pos;

		Actor.add(new Pushing(ch, ch.pos, newPos, new Callback() {
			public void call() {
				if (initialpos != ch.pos || Actor.findChar(newPos) != null) {
					//something caused movement or added chars before pushing resolved, cancel to be safe.
					ch.sprite.place(ch.pos);
					return;
				}
				int oldPos = ch.pos;
				ch.pos = newPos;
				if (finalCollided && ch.isActive()) {
					ch.damage(Random.NormalIntRange(finalDist, 2*finalDist), new Knockback());
					if (ch.isActive()) {
						Paralysis.prolong(ch, Paralysis.class, 1 + finalDist/2f);
					} else if (ch == Dungeon.hero){
						if (cause instanceof WandOfBlastWave){
							Badges.validateDeathFromFriendlyMagic();
						}
						GLog.n(Messages.get(WandOfBlastWave.class, "knockback_ondeath"));
						Dungeon.fail(cause);
					}
				}
				if (closeDoors && Dungeon.level.map[oldPos] == Terrain.OPEN_DOOR){
					Door.leave(oldPos);
				}
				Dungeon.level.occupyCell(ch);
				if (ch == Dungeon.hero){
					Dungeon.observe();
					GameScene.updateFog();
				} else if (Dungeon.level.heroFOV[initialpos] != Dungeon.level.heroFOV[newPos]){
					Dungeon.observe();
				}
			}
		}));
	}

	//**********************************************************************************
	//*** Section 1b: BlastWave Visual Effect
	//**********************************************************************************

	public static class BlastWaveEffect extends Image {

		private static final float TIME_TO_FADE = 0.2f;

		private float time;
		private float size;

		public BlastWaveEffect(){
			super(Effects.get(Effects.Type.RIPPLE));
			origin.set(width / 2, height / 2);
		}

		public void reset(int pos, float size) {
			revive();

			x = (pos % Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - width) / 2;
			y = (pos / Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - height) / 2;

			time = TIME_TO_FADE;
			this.size = size;
		}

		@Override
		public void update() {
			super.update();

			if ((time -= Game.elapsed) <= 0) {
				kill();
			} else {
				float p = time / TIME_TO_FADE;
				alpha(p);
				scale.y = scale.x = (1-p)*size;
			}
		}

		}

	//**********************************************************************************
	//*** blastWave Factory Methods
	//**********************************************************************************

	/**
	 * Creates a blast wave visual effect at the given position with default radius (3).
	 */
	public static void blastWave(int pos) {
		blastWave(pos, 3);
	}

	/**
	 * Creates a blast wave visual effect at the given position with the specified radius.
	 */
	public static void blastWave(int pos, float radius) {
		Group parent = Dungeon.hero.sprite.parent;
		BlastWaveEffect b = (BlastWaveEffect) parent.recycle(BlastWaveEffect.class);
		parent.bringToFront(b);
		b.reset(pos, radius);
	}

	//**********************************************************************************
	//*** Section 2: Enchantment Wrappers for On-Hit Effects
	//**********************************************************************************

	public static class BlastWaveEnchantment extends Elastic {
		@Override
		protected float procChanceMultiplier(Char attacker) {
			return Wand.procChanceMultiplier(attacker);
		}
	}

	public static class FireBlastEnchantment extends Blazing {
		@Override
		protected float procChanceMultiplier(Char attacker) {
			return Wand.procChanceMultiplier(attacker);
		}
	}

	public static class LightningEnchantment extends Shocking {
		@Override
		protected float procChanceMultiplier(Char attacker) {
			return Wand.procChanceMultiplier(attacker);
		}
	}


	//**********************************************************************************
	//*** Section 2b: On-Hit Melee Proc Methods
	//**********************************************************************************

	// --- BlastWave onHit handler ---

	public static void onHitBlastWave(MagesStaff staff, Char attacker, Char defender, int damage) {
		com.zootdungeon.actors.hero.Talent.EmpoweredStrikeTracker tracker =
				attacker.buff(com.zootdungeon.actors.hero.Talent.EmpoweredStrikeTracker.class);

		if (tracker != null){
			tracker.delayedDetach = true;
		}

		Actor.add(new Actor() {
			{
				actPriority = VFX_PRIO+9;
			}

			@Override
			protected boolean act() {
				Actor.remove(this);
				if (defender.isAlive()) {
					new BlastWaveEnchantment().proc(staff, attacker, defender, damage);
				}
				if (tracker != null) tracker.detach();
				return true;
			}
		});
	}
	// --- Frost ---

	public static void applyFrostOnHit(Char defender, int wandLevel, float procChanceMultiplier) {
		Chill chill = defender.buff(Chill.class);
		if (chill != null) {
			float procChance = ((int)Math.floor(chill.cooldown()) - 1)/9f;
			procChance *= procChanceMultiplier;
			if (Random.Float() < procChance) {
				float powerMulti = Math.max(1f, procChance);
				new Buff() {
					{ actPriority = VFX_PRIO; }
					public boolean act() {
						Buff.affect(target, Frost.class, Math.round(Frost.DURATION * powerMulti));
						return super.act();
					}
				}.attachTo(defender);
			}
		}
	}

	// --- Corrosion ---

	public static void applyCorrosionOnHit(Char defender, int wandLevel, float procChanceMultiplier) {
		int level = Math.max(0, wandLevel);
		float procChance = (level+1f)/(level+3f) * procChanceMultiplier;
		if (Random.Float() < procChance) {
			float powerMulti = Math.max(1f, procChance);
			Buff.affect(defender, Ooze.class).set(Ooze.DURATION * powerMulti);
			CellEmitter.center(defender.pos).burst(CorrosionParticle.SPLASH, 5);
		}
	}

	// --- Corruption ---

	public static void applyCorruptionOnHit(Char defender, int wandLevel, float procChanceMultiplier) {
		int level = Math.max(0, wandLevel);
		float procChance = (level+1f)/(level+6f) * procChanceMultiplier;
		if (Random.Float() < procChance) {
			float powerMulti = Math.max(1f, procChance);
			Buff.prolong(defender, Amok.class, Math.round((4+level*2) * powerMulti));
		}
	}

	// --- Regrowth ---

	public static void applyRegrowthOnHit(Char attacker, Char defender, int damage, int wandLevel, float procChanceMultiplier) {
		boolean grass = false;
		int terr = Dungeon.level.map[attacker.pos];
		if (terr == Terrain.GRASS || terr == Terrain.HIGH_GRASS || terr == Terrain.FURROWED_GRASS) grass = true;
		terr = Dungeon.level.map[defender.pos];
		if (terr == Terrain.GRASS || terr == Terrain.HIGH_GRASS || terr == Terrain.FURROWED_GRASS) grass = true;
		if (grass) {
			int level = Math.max(0, wandLevel);
			int healing = Math.round(damage * (level + 2f) / (level + 6f) / 2f);
			healing = Math.round(healing * procChanceMultiplier);
			Buff.affect(attacker, Sungrass.Health.class).boost(healing);
		}
	}

	// --- Transfusion ---

	public static void applyTransfusionOnHit(WandOfTransfusion wand, Char attacker, Char defender, int wandLevel, float procChanceMultiplier) {
		if (defender.buff(Charm.class) != null && defender.buff(Charm.class).object == attacker.id()){
			wand.freeCharge = true;
			int shieldToGive = Math.round((2*(5 + wandLevel))*procChanceMultiplier);
			Buff.affect(attacker, Barrier.class).setShield(shieldToGive);
			attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
			GLog.p(Messages.get(WandOfTransfusion.class, "charged"));
			attacker.sprite.emitter().burst(BloodParticle.BURST, 20);
		}
	}

	// --- Warding ---

	public static void applyWardingOnHit(int wandLevel, float procChanceMultiplier) {
		int level = Math.max(0, wandLevel);
		float procChance = (level+1f)/(level+5f) * procChanceMultiplier;
		if (Random.Float() < procChance) {
			float powerMulti = Math.max(1f, procChance);
			for (Char ch : Actor.chars()){
				if (ch instanceof WandOfWarding.Ward){
					((WandOfWarding.Ward) ch).wandHeal(wandLevel, powerMulti);
					ch.sprite.emitter().burst(MagicMissile.WardParticle.UP, ((WandOfWarding.Ward) ch).tier);
				}
			}
		}
	}

	// --- LivingEarth ---

	public static void applyLivingEarthOnHit(Char attacker, int damage, int wandLevel, float procChanceMultiplier) {
		WandOfLivingEarth.EarthGuardian guardian = null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof WandOfLivingEarth.EarthGuardian){
				guardian = (WandOfLivingEarth.EarthGuardian) m;
				break;
			}
		}
		int armor = Math.round(damage*0.33f*procChanceMultiplier);
		if (guardian != null){
			guardian.sprite.centerEmitter().burst(MagicMissile.EarthParticle.ATTRACT, 8 + wandLevel / 2);
			guardian.setInfo(Dungeon.hero, wandLevel, armor);
		} else {
			attacker.sprite.centerEmitter().burst(MagicMissile.EarthParticle.ATTRACT, 8 + wandLevel / 2);
			Buff.affect(attacker, WandOfLivingEarth.RockArmor.class).addArmor(wandLevel, armor);
		}
	}

	// --- PrismaticLight ---

	public static void applyPrismaticOnHit(Char defender, int wandLevel, float procChanceMultiplier) {
		Buff.prolong(defender, Cripple.class, Math.round((1+wandLevel)*procChanceMultiplier));
	}

	// --- MagicMissile ---

	public static void applyMagicMissileOnHit(Char attacker, Wand self, float procChanceMultiplier) {
		for (Wand.Charger c : attacker.buffs(Wand.Charger.class)){
			if (c.wand() != self){
				c.gainCharge(0.5f * procChanceMultiplier);
			}
		}
	}


	//**********************************************************************************
	//*** Section 3: On-Zap Core Effects
	//**********************************************************************************

	// --- Corrosion ---

	public static void deployCorrosiveGas(int cell, int level, Class<? extends Wand> source, Wand wand) {
		CorrosiveGas gas = Blob.seed(cell, 50 + 10 * level, CorrosiveGas.class);
		CellEmitter.get(cell).burst(Speck.factory(Speck.CORROSION), 10);
		gas.setStrength(2 + level, source);
		GameScene.add(gas);
		Sample.INSTANCE.play(Assets.Sounds.GAS);

		for (int i : PathFinder.NEIGHBOURS9) {
			Char ch = Actor.findChar(cell + i);
			if (ch != null) {
				wand.wandProc(ch, wand.chargesPerCast());
			}
		}

		if (Actor.findChar(cell) == null) {
			Dungeon.level.pressCell(cell);
		}
	}

	// --- Frost ---

	public static void applyFrostZap(Ballistica bolt, int level, Wand wand) {
		Heap heap = Dungeon.level.heaps.get(bolt.collisionPos);
		if (heap != null) {
			heap.freeze();
		}

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		if (fire != null && fire.volume > 0) {
			fire.clear(bolt.collisionPos);
		}

		com.zootdungeon.levels.rooms.special.MagicalFireRoom.EternalFire eternalFire =
				(com.zootdungeon.levels.rooms.special.MagicalFireRoom.EternalFire)Dungeon.level.blobs.get(
						com.zootdungeon.levels.rooms.special.MagicalFireRoom.EternalFire.class);
		if (eternalFire != null && eternalFire.volume > 0) {
			eternalFire.clear(bolt.collisionPos);
			if (bolt.path.size() > bolt.dist + 1) {
				eternalFire.clear(bolt.path.get(bolt.dist + 1));
			}
		}

		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null) {

			int damage = wand instanceof DamageWand ? ((DamageWand)wand).damageRoll() : 0;

			if (ch.buff(Frost.class) != null) {
				return;
			}
			if (ch.buff(Chill.class) != null) {
				float chillturns = Math.min(10, ch.buff(Chill.class).cooldown());
				damage = (int)Math.round(damage * Math.pow(0.9333f, chillturns));
			} else {
				ch.sprite.burst(0xFF99CCFF, level / 2 + 2);
			}

			wand.wandProc(ch, wand.chargesPerCast());
			ch.damage(damage, wand);

			if (ch.isAlive()) {
				if (Dungeon.level.water[ch.pos])
					Buff.affect(ch, Chill.class, 4 + level);
				else
					Buff.affect(ch, Chill.class, 2 + level);
			}
		} else {
			Dungeon.level.pressCell(bolt.collisionPos);
		}
	}

	// --- Warding ---

	public static void applyWardingZap(Ballistica bolt, int wandLevel, boolean wardAvailable) {
		int target = bolt.collisionPos;
		Char ch = Actor.findChar(target);
		if (ch != null && !(ch instanceof WandOfWarding.Ward)) {
			if (bolt.dist > 1) target = bolt.path.get(bolt.dist - 1);

			ch = Actor.findChar(target);
			if (ch != null && !(ch instanceof WandOfWarding.Ward)) {
				GLog.w(Messages.get(WandOfWarding.class, "bad_location"));
				Dungeon.level.pressCell(bolt.collisionPos);
				return;
			}
		}

		if (ch != null) {
			if (ch instanceof WandOfWarding.Ward) {
				if (wardAvailable) {
					((WandOfWarding.Ward) ch).upgrade(wandLevel);
				} else {
					((WandOfWarding.Ward) ch).wandHeal(wandLevel);
				}
				ch.sprite.emitter().burst(MagicMissile.WardParticle.UP, ((WandOfWarding.Ward) ch).tier);
			} else {
				GLog.w(Messages.get(WandOfWarding.class, "bad_location"));
				Dungeon.level.pressCell(target);
			}

		} else if (!Dungeon.level.passable[target]) {
			GLog.w(Messages.get(WandOfWarding.class, "bad_location"));
			Dungeon.level.pressCell(target);

		} else {
			WandOfWarding.Ward ward = new WandOfWarding.Ward();
			ward.pos = target;
			ward.wandLevel = wandLevel;
			GameScene.add(ward, 1f);
			Dungeon.level.occupyCell(ward);
			ward.sprite.emitter().burst(MagicMissile.WardParticle.UP, ward.tier);
			Dungeon.level.pressCell(target);
		}
	}

	// --- PrismaticLight ---

	public static boolean revealMapPrismatic(Ballistica beam) {
		boolean noticed = false;
		for (int c : beam.subPath(0, beam.dist)) {
			if (!Dungeon.level.insideMap(c)) {
				continue;
			}
			for (int n : PathFinder.NEIGHBOURS9) {
				int cell = c + n;

				if (Dungeon.level.discoverable[cell])
					Dungeon.level.mapped[cell] = true;

				int terr = Dungeon.level.map[cell];
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {

					Dungeon.level.discover(cell);

					GameScene.discoverTile(cell, terr);
					com.zootdungeon.items.scrolls.ScrollEffects.discover(cell);

					noticed = true;
				}
			}

			CellEmitter.center(c).burst(RainbowParticle.BURST, Random.IntRange(1, 2));
		}
		if (noticed)
			Sample.INSTANCE.play(Assets.Sounds.SECRET);

		GameScene.updateFog();
		return noticed;
	}

	public static void applyPrismaticDamage(Char target, int level, Wand wand) {
		int dmg = wand instanceof DamageWand ? ((DamageWand)wand).damageRoll() : 0;

		if (Random.Int(5 + level) >= 3) {
			Buff.prolong(target, Blindness.class, 2f + (level * 0.333f));
			target.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
		}

		if (target.properties().contains(Char.Property.DEMONIC) || target.properties().contains(Char.Property.UNDEAD)) {
			target.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10 + level);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);

			target.damage(Math.round(dmg * 1.333f), wand);
		} else {
			target.sprite.centerEmitter().burst(RainbowParticle.BURST, 10 + level);

			target.damage(dmg, wand);
		}
	}

	// --- Transfusion ---

	public static void transfusionAllyEffect(Char user, Char ally, int wandLevel, Wand wand, boolean freeCharge) {
		int selfDmg = Math.round(user.HT * 0.05f);

		int healing = selfDmg + 3 * wandLevel;
		int shielding = (ally.HP + healing) - ally.HT;
		if (shielding > 0) {
			healing -= shielding;
			Buff.affect(ally, Barrier.class).setShield(shielding);
		} else {
			shielding = 0;
		}

		ally.HP += healing;

		ally.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2 + wandLevel / 2);
		if (healing > 0) {
			ally.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);
		}
		if (shielding > 0) {
			ally.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shielding), FloatingText.SHIELDING);
		}

		if (!freeCharge) {
			user.damage(selfDmg, wand);
		}
	}

	public static void transfusionEnemyEffect(Char user, Char enemy, int wandLevel, Wand wand) {
		Buff.affect(user, Barrier.class).setShield((5 + wandLevel));
		user.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(5 + wandLevel), FloatingText.SHIELDING);

		if (!enemy.properties().contains(Char.Property.UNDEAD)) {
			Charm charm = Buff.affect(enemy, Charm.class, Charm.DURATION / 2f);
			charm.object = user.id();
			charm.ignoreHeroAllies = true;
			enemy.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 3);
		} else {
			int dmg = wand instanceof DamageWand ? ((DamageWand)wand).damageRoll() : 0;
			enemy.damage(dmg, wand);
			enemy.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10 + wandLevel);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
		}
	}

	// --- Corruption ---

	public static float calcCorruptionResist(Mob enemy) {
		float enemyResist;
		if (enemy instanceof Mimic || enemy instanceof com.zootdungeon.actors.mobs.Statue){
			enemyResist = 1 + Dungeon.depth;
		} else if (enemy instanceof com.zootdungeon.actors.mobs.Piranha || enemy instanceof com.zootdungeon.actors.mobs.Bee) {
			enemyResist = 1 + Dungeon.depth/2f;
		} else if (enemy instanceof com.zootdungeon.actors.mobs.Wraith) {
			enemyResist = (1f + Dungeon.scalingDepth()/4f) / 5f;
		} else if (enemy instanceof com.zootdungeon.actors.mobs.Swarm){
			enemyResist = 1 + com.zootdungeon.actors.buffs.AscensionChallenge.AscensionCorruptResist(enemy);
			if (enemyResist == 1) enemyResist = 1 + 3;
		} else {
			enemyResist = 1 + com.zootdungeon.actors.buffs.AscensionChallenge.AscensionCorruptResist(enemy);
		}

		enemyResist *= 1 + 4*Math.pow(enemy.HP/(float)enemy.HT, 2);

		for (Buff buff : enemy.buffs()){
			if (WandOfCorruption.MAJOR_DEBUFFS.containsKey(buff.getClass()))         enemyResist *= (1f-WandOfCorruption.MAJOR_DEBUFF_WEAKEN);
			else if (WandOfCorruption.MINOR_DEBUFFS.containsKey(buff.getClass()))    enemyResist *= (1f-WandOfCorruption.MINOR_DEBUFF_WEAKEN);
			else if (buff.type == Buff.buffType.NEGATIVE)           enemyResist *= (1f-WandOfCorruption.MINOR_DEBUFF_WEAKEN);
		}

		return enemyResist;
	}

	// --- MagicMissile ---

	public static void applyMagicCharge(Char user, int wandLevel, Wand wand) {
		for (Wand.Charger wandCharger : user.buffs(Wand.Charger.class)) {
			if (wandCharger.wand().buffedLvl() < wandLevel || user.buff(WandOfMagicMissile.MagicCharge.class) != null) {
				Buff.prolong(user, WandOfMagicMissile.MagicCharge.class, WandOfMagicMissile.MagicCharge.DURATION).setup(wand);
				break;
			}
		}
	}

}
