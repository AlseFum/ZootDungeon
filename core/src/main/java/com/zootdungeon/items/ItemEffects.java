package com.zootdungeon.items;

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
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.actors.buffs.Charm;
import com.zootdungeon.actors.buffs.Chill;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.Frost;
import com.zootdungeon.actors.buffs.Ooze;
import com.zootdungeon.actors.buffs.Paralysis;
import com.zootdungeon.actors.buffs.Poison;
import com.zootdungeon.actors.buffs.Roots;
import com.zootdungeon.actors.buffs.SoulMark;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mimic;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.actors.mobs.npcs.MirrorImage;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Effects;
import com.zootdungeon.effects.FloatingText;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.effects.Pushing;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.BloodParticle;
import com.zootdungeon.effects.particles.CorrosionParticle;
import com.zootdungeon.effects.particles.EnergyParticle;
import com.zootdungeon.effects.particles.PurpleParticle;
import com.zootdungeon.effects.particles.RainbowParticle;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.rings.Ring;
import com.zootdungeon.items.trinkets.ShardOfOblivion;
import com.zootdungeon.items.wands.DamageWand;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.wands.WandOfCorruption;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.wands.WandOfTransfusion;
import com.zootdungeon.items.wands.WandOfWarding;
import com.zootdungeon.items.weapon.base.Weapon;
import com.zootdungeon.items.weapon.enchantments.Blazing;
import com.zootdungeon.items.weapon.enchantments.Elastic;
import com.zootdungeon.items.weapon.enchantments.Shocking;
import com.zootdungeon.items.weapon.MagesStaff;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.RegularLevel;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.features.Door;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.rooms.secret.SecretRoom;
import com.zootdungeon.levels.rooms.special.SpecialRoom;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.plants.Sungrass;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.tiles.DungeonTilemap;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Static utility class containing all item special effects as pure functions.
 * Covers effects shared across wands, scrolls, and other items.
 */
public class ItemEffects {

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
					discover(cell);

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

	//**********************************************************************************
	//*** Section 4: Teleportation (from ScrollOfTeleportation)
	//**********************************************************************************

	public static boolean teleportToLocation(Char ch, int pos){
		PathFinder.buildDistanceMap(pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
		if (PathFinder.distance[ch.pos] == Integer.MAX_VALUE
				|| (!Dungeon.level.passable[pos] && !Dungeon.level.avoid[pos])
				|| (Actor.findChar(pos) != null && Actor.findChar(pos) != ch)){
			if (ch == Dungeon.hero){
				GLog.w( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "cant_reach") );
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
		return teleportChar( ch, com.zootdungeon.items.scrolls.ScrollOfTeleportation.class );
	}

	public static boolean teleportChar( Char ch, Class source ) {

		if (!(Dungeon.level instanceof RegularLevel)){
			return teleportInNonRegularLevel( ch, false );
		}

		if (Char.hasProp(ch, Char.Property.IMMOVABLE) || ch.isImmune(source)){
			GLog.w( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "no_tele") );
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

			GLog.w( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "no_tele") );
			return false;

		} else {

			appear( ch, pos );
			Dungeon.level.occupyCell( ch );
			Buff.detach(ch, Roots.class);

			if (ch == Dungeon.hero) {
				GLog.i( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "tele") );

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
			GLog.i( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "tele") );
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
			GLog.w( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "no_tele") );
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
			GLog.w( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "no_tele") );
			return false;
		}

		appear( ch, pos );
		Dungeon.level.occupyCell( ch );

		Buff.detach(ch, Roots.class);

		if (ch == Dungeon.hero) {
			GLog.i( Messages.get(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, "tele") );

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
	//*** Section 5: Recharging (from ScrollOfRecharging)
	//**********************************************************************************

	public static void charge( Char user ) {
		if (user.sprite != null) {
			Emitter e = user.sprite.centerEmitter();
			if (e != null) e.burst(EnergyParticle.FACTORY, 15);
		}
	}

	//**********************************************************************************
	//*** Section 6: Magic Mapping / Discover (from ScrollOfMagicMapping)
	//**********************************************************************************

	public static void discover( int cell ) {
		CellEmitter.get( cell ).start( Speck.factory( Speck.DISCOVER ), 0.1f, 4 );
	}

	//**********************************************************************************
	//*** Section 7: Upgrade / Curse (from ScrollOfUpgrade)
	//**********************************************************************************

	public static void upgradeVFX( Hero hero ) {
		hero.sprite.emitter().start( Speck.factory( Speck.UP ), 0.2f, 3 );
	}

	public static void weakenCurseVFX( Hero hero ){
		GLog.p( Messages.get(com.zootdungeon.items.scrolls.ScrollOfUpgrade.class, "weaken_curse") );
		hero.sprite.emitter().start( ShadowParticle.UP, 0.05f, 5 );
	}

	public static void removeCurseVFX( Hero hero ){
		GLog.p( Messages.get(com.zootdungeon.items.scrolls.ScrollOfUpgrade.class, "remove_curse") );
		hero.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
		Badges.validateClericUnlock();
	}

	//**********************************************************************************
	//*** Section 8: RemoveCurse (from ScrollOfRemoveCurse)
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
	//*** Section 9: Identify (from ScrollOfIdentify)
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
		GLog.i(Messages.get(com.zootdungeon.items.scrolls.ScrollOfIdentify.class, "it_is", item.title()));
		Badges.validateItemLevelAquired( item );
	}

	//**********************************************************************************
	//*** Section 10: MirrorImage (from ScrollOfMirrorImage)
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
			ItemEffects.appear( mob, respawnPoints.get( index ) );

			respawnPoints.remove( index );
			nImages--;
			spawned++;
		}

		return spawned;
	}

}
