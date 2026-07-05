package com.zootdungeon.actors.buffs;

import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.Entity;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.blobs.ToxicGas;
import com.zootdungeon.actors.buffs.Barrier;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.actors.buffs.Chill;
import com.zootdungeon.actors.buffs.Corrosion;
import com.zootdungeon.actors.buffs.Degrade;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Paralysis;
import com.zootdungeon.actors.buffs.Roots;
import com.zootdungeon.actors.entities.CellEntitySprite;
import com.zootdungeon.actors.entities.mines.MineSprite;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.SparkParticle;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.wands.WandOfCorrosion;
import com.zootdungeon.items.wands.WandOfCorruption;
import com.zootdungeon.items.wands.WandOfDisintegration;
import com.zootdungeon.items.wands.WandOfFireblast;
import com.zootdungeon.items.wands.WandOfFrost;
import com.zootdungeon.items.wands.WandOfLightning;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.wands.WandOfPrismaticLight;
import com.zootdungeon.items.wands.WandOfRegrowth;
import com.zootdungeon.items.wands.WandOfTransfusion;
import com.zootdungeon.items.wands.WandOfWarding;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.utils.GLog;

/**
 * LOGOS subclass effects: rune drawing and sentinel summoning.
 */
public class LogosEffects {

	// ========== 符文 ==========

	public static void drawRune(Hero hero, Wand wand, int cell) {
		if (wand.curCharges <= 0) {
			GLog.w(Messages.get(LogosEffects.class, "no_charge"));
			return;
		}
		wand.curCharges--;
		Item.updateQuickslot();

		int pts = hero.hasTalent(Talent.LOGOS_RUNE_POWER)
				? hero.pointsInTalent(Talent.LOGOS_RUNE_POWER) : 0;

		RuneType type = wandToRuneType(wand);
		RuneEntity rune = new RuneEntity(type, pts, cell);
		Dungeon.level.addCellEntity(rune, cell);
		CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 5 + pts);
		GLog.p(Messages.get(LogosEffects.class, type.name().toLowerCase()));
	}

	private static RuneType wandToRuneType(Wand wand) {
		if (wand instanceof WandOfFireblast)      return RuneType.FIRE;
		if (wand instanceof WandOfFrost)          return RuneType.FROST;
		if (wand instanceof WandOfLightning)      return RuneType.LIGHTNING;
		if (wand instanceof WandOfDisintegration) return RuneType.DISINTEGRATION;
		if (wand instanceof WandOfMagicMissile)   return RuneType.MAGIC_MISSILE;
		if (wand instanceof WandOfCorruption)     return RuneType.CORRUPTION;
		if (wand instanceof WandOfBlastWave)      return RuneType.BLAST_WAVE;
		if (wand instanceof WandOfPrismaticLight) return RuneType.PRISMATIC;
		if (wand instanceof WandOfRegrowth)       return RuneType.REGROWTH;
		if (wand instanceof WandOfCorrosion)      return RuneType.CORROSION;
		if (wand instanceof WandOfTransfusion)    return RuneType.TRANSFUSION;
		if (wand instanceof WandOfLivingEarth)    return RuneType.LIVING_EARTH;
		if (wand instanceof WandOfWarding)        return RuneType.WARDING;
		return RuneType.GENERIC;
	}

	// ========== 哨卫 ==========

	public static WandSentinel summonSentinel(Hero hero, Wand wand) {
		if (wand.curCharges < 2) {
			GLog.w(Messages.get(LogosEffects.class, "no_charge_sentinel"));
			return null;
		}
		wand.curCharges -= 2;
		Item.updateQuickslot();

		int pts = hero.pointsInTalent(Talent.LOGOS_WAND_SENTINEL);
		WandSentinel sentinel = new WandSentinel();
		sentinel.setup(hero, wand, pts);
		GameScene.add(sentinel);
		Buff.prolong(hero, WandSentinel.SentinelTracker.class, sentinel.maxLifeTurns);
		GLog.p(Messages.get(WandSentinel.class, "summoned"));
		return sentinel;
	}

	// ========== 枚举 ==========

	public enum RuneType {
		FIRE, FROST, LIGHTNING, DISINTEGRATION, MAGIC_MISSILE,
		CORRUPTION, BLAST_WAVE, PRISMATIC, REGROWTH, CORROSION,
		TRANSFUSION, LIVING_EARTH, WARDING, GENERIC
	}

	// ==================== 内部类：符文地面实体 ====================

	public static class RuneEntity extends Entity {

		public RuneType type = RuneType.GENERIC;
		public int powerLevel = 0;
		public int lifeTurns = 15;
		private boolean triggered = false;

		public RuneEntity() {
			super();
		}

		public RuneEntity(RuneType type, int powerLevel, int pos) {
			this.type = type;
			this.powerLevel = powerLevel;
			this.pos = pos;
			this.lifeTurns = 15 + powerLevel * 5;
			try {
				this.sprite = new RuneEntitySprite(type);
				this.sprite.link(this);
				this.sprite.place(pos);
			} catch (Exception ignored) {}
		}

		@Override public Class<? extends CellEntitySprite> spriteClass() { return RuneEntitySprite.class; }

		@Override
		protected boolean act() {
			lifeTurns--;
			if (lifeTurns <= 0) { despawn(); return true; }
			spend(TICK);
			return true;
		}

		@Override
		public void onStep(Char who) {
			if (triggered || who == null || !who.isAlive()) return;
			if (who.alignment == Char.Alignment.ENEMY) {
				triggered = true;
				trigger(who);
				despawn();
			}
		}

		@Override public void onFlyOver(Char who) {}

		private void trigger(Char victim) {
			CellEmitter.center(pos).burst(Speck.factory(Speck.LIGHT), 10);
			Sample.INSTANCE.play(Assets.Sounds.ZAP);
			switch (type) {
				case FIRE:          fire(victim); break;
				case FROST:         frost(victim); break;
				case LIGHTNING:     lightning(victim); break;
				case DISINTEGRATION: dis(victim); break;
				case MAGIC_MISSILE: missile(victim); break;
				case CORRUPTION:    corrupt(victim); break;
				case BLAST_WAVE:    blast(victim); break;
				case PRISMATIC:     prism(victim); break;
				case REGROWTH:      regrowth(); break;
				case CORROSION:     corrode(victim); break;
				case TRANSFUSION:   heal(); break;
				case LIVING_EARTH:  earth(victim); break;
				case WARDING:       wardTrigger(victim); break;
				default:            generic(victim); break;
			}
		}

		private void fire(Char v) {
			for (int n : PathFinder.NEIGHBOURS9) {
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
					ch.damage(6 + 3 * powerLevel, this);
					Buff.affect(ch, Burning.class).reignite(ch, 4f + 2f * powerLevel);
				}
			}
			GameScene.add(Blob.seed(pos, 3 + powerLevel, Fire.class));
		}
		private void frost(Char v) {
			for (int n : PathFinder.NEIGHBOURS9) {
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
					ch.damage(4 + 2 * powerLevel, this);
					Buff.prolong(ch, Chill.class, 5f + 2f * powerLevel);
				}
			}
		}
		private void lightning(Char v) {
			for (int i = 0, n = 0; i < PathFinder.NEIGHBOURS8.length && n < 2 + powerLevel; i++) {
				Char ch = Actor.findChar(pos + PathFinder.NEIGHBOURS8[i]);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
					ch.damage(Random.NormalIntRange(6, 12 + 4 * powerLevel), this);
					ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
					ch.sprite.flash(); n++;
				}
			}
		}
		private void dis(Char v) { v.damage(10 + 5 * powerLevel, this); Buff.affect(v, Degrade.class, Degrade.DURATION); }
		private void missile(Char v) {
			v.damage(8 + 3 * powerLevel, this);
			for (int n : PathFinder.NEIGHBOURS8) {
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) ch.damage(3 + powerLevel, this);
			}
		}
		private void corrupt(Char v) { v.damage(5 + 2 * powerLevel, this); Buff.affect(v, Degrade.class, Degrade.DURATION); }
		private void blast(Char v) {
			for (int n : PathFinder.NEIGHBOURS8) {
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
					ch.damage(6 + 3 * powerLevel, this);
					Buff.prolong(ch, Paralysis.class, 1f);
				}
			}
		}
		private void prism(Char v) {
			int dmg = 8 + 4 * powerLevel;
			if (Char.hasProp(v, Char.Property.UNDEAD) || Char.hasProp(v, Char.Property.DEMONIC)) dmg = Math.round(dmg * 1.5f);
			v.damage(dmg, this);
		}
		private void regrowth() {
			Hero h = Dungeon.hero;
			if (h != null && h.isAlive()) {
				int hh = 5 + 3 * powerLevel;
				h.HP = Math.min(h.HT, h.HP + hh);
				if (h.sprite != null) h.sprite.showStatus(CharSprite.POSITIVE, "+" + hh + "HP");
			}
			for (int n : PathFinder.NEIGHBOURS9) {
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) Buff.prolong(ch, Roots.class, 2f + powerLevel);
			}
		}
		private void corrode(Char v) {
			GameScene.add(Blob.seed(pos, 30 + 20 * powerLevel, ToxicGas.class));
			Buff.affect(v, Corrosion.class).set(5f + 2f * powerLevel, 6 + 2 * powerLevel);
		}
		private void heal() {
			Hero h = Dungeon.hero;
			if (h != null && h.isAlive()) {
				int hh = 4 + 3 * powerLevel;
				h.HP = Math.min(h.HT, h.HP + hh);
				if (h.sprite != null) h.sprite.showStatus(CharSprite.POSITIVE, "+" + hh + "HP");
				Buff.affect(h, Barrier.class).setShield(3 + 2 * powerLevel);
			}
		}
		private void earth(Char v) {
			Hero h = Dungeon.hero;
			if (h != null && h.isAlive()) Buff.affect(h, Barrier.class).setShield(8 + 4 * powerLevel);
			for (int n : PathFinder.NEIGHBOURS9) {
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) Buff.prolong(ch, Roots.class, 3f + powerLevel);
			}
		}
		private void wardTrigger(Char v) {
			Hero h = Dungeon.hero;
			if (h != null && h.isAlive()) Buff.affect(h, Barrier.class).setShield(10 + 5 * powerLevel);
			v.damage(6 + 3 * powerLevel, this);
		}
		private void generic(Char v) { v.damage(6 + 2 * powerLevel, this); }

		@Override public String desc() { return Messages.get(this, "desc", type.name().toLowerCase()); }

		private static final String RUNE_TYPE="rune_type", POWER_LVL="power_lvl", LIFE="life", TRIGGERED="triggered";
		@Override public void storeInBundle(Bundle b) {
			super.storeInBundle(b);
			b.put(RUNE_TYPE, type.ordinal()); b.put(POWER_LVL, powerLevel);
			b.put(LIFE, lifeTurns); b.put(TRIGGERED, triggered);
		}
		@Override public void restoreFromBundle(Bundle b) {
			super.restoreFromBundle(b);
			type = RuneType.values()[b.getInt(RUNE_TYPE)];
			powerLevel = b.getInt(POWER_LVL); lifeTurns = b.getInt(LIFE);
			triggered = b.getBoolean(TRIGGERED);
		}
	}

	// ==================== 内部类：符文精灵 ====================

	public static class RuneEntitySprite extends MineSprite {
		private static final String TEX = "cola/trashbin.png";
		public RuneEntitySprite() { this(RuneType.GENERIC); }
		public RuneEntitySprite(RuneType type) {
			super();
			texture(TEX);
			TextureFilm film = new TextureFilm(TEX, 16, 16);
			idle = new Animation(4, true); idle.frames(film, 0, 1, 0, 2);
			place = new Animation(3, false); place.frames(film, 0, 1, 2);
			disarm = new Animation(4, false); disarm.frames(film, 2, 1, 0);
			detonate = new Animation(2, false); detonate.frames(film, 3);
			play(idle);
			switch (type) {
				case FIRE: hardlight(0xFF4444); break; case FROST: hardlight(0x44AAFF); break;
				case LIGHTNING: hardlight(0xFFFF44); break; case DISINTEGRATION: hardlight(0xAA44FF); break;
				case MAGIC_MISSILE: hardlight(0x88FFFF); break; case CORRUPTION: hardlight(0x884488); break;
				case BLAST_WAVE: hardlight(0xFFAA44); break; case PRISMATIC: hardlight(0xFFFFFF); break;
				case REGROWTH: hardlight(0x44FF44); break; case CORROSION: hardlight(0x88AA44); break;
				case TRANSFUSION: hardlight(0xFF88AA); break; case LIVING_EARTH: hardlight(0x886644); break;
				case WARDING: hardlight(0x4488FF); break; default: hardlight(0xAAAAAA); break;
			}
		}
		@Override public int baseColor() { return 0xFF4488FF; }
	}

	// ==================== 内部类：法杖哨卫 ====================

	public static class WandSentinel extends com.zootdungeon.actors.mobs.Mob {

		private int lifeTurns = 0;
		public int maxLifeTurns = 20;
		public Class<? extends Wand> wandClass = null;
		public int powerLevel = 0;

		{
			spriteClass = WandSentinelSprite.class;
			HP = HT = 30; defenseSkill = 8; EXP = 0; maxLvl = -2;
			alignment = Alignment.ALLY;
			properties.add(Property.IMMOVABLE);
		}

		public void setup(Hero hero, Wand wand, int talentPts) {
			HT = 20 + 10 * talentPts;  HP = HT;
			defenseSkill = 4 + 4 * talentPts;
			maxLifeTurns = 10 + 15 * talentPts;  lifeTurns = maxLifeTurns;
			this.wandClass = wand.getClass();
			this.powerLevel = talentPts;
			pos = hero.pos;
			if (Dungeon.level.findMob(pos) != null && Dungeon.level.findMob(pos) != this) {
				for (int n : PathFinder.NEIGHBOURS8) {
					int c = hero.pos + n;
					if (Dungeon.level.passable[c] && Dungeon.level.findMob(c) == null) { pos = c; break; }
				}
			}
		}

		@Override protected boolean act() {
			if (--lifeTurns <= 0 || Dungeon.hero == null || !Dungeon.hero.isAlive()) { destroy(); sprite.die(); return true; }
			if (Dungeon.level.distance(pos, Dungeon.hero.pos) > 999) {
				int step = Dungeon.findStep(this, Dungeon.hero.pos, Dungeon.level.passable, Dungeon.level.heroFOV, true);
				if (step != -1 && step != pos) { move(step); spend(TICK); return true; }
			}
			// Use standard mob AI: pick enemy, move, attack
				boolean enemyInFOV = false;
				enemy = chooseEnemy();
			if (enemyInFOV) {
					if (Dungeon.level.distance(pos, enemy.pos) <= 1 && canAttack(enemy)) {
						spend(TICK);
						if (attack(enemy)) {}
						return true;
					} else {
						int step = Dungeon.findStep(this, enemy.pos, Dungeon.level.passable, Dungeon.level.heroFOV, true);
						if (step != -1) { move(step); spend(TICK); return true; }
					}
				}

				// Follow hero when no enemy
				if (Dungeon.level.distance(pos, Dungeon.hero.pos) > 2) {
					int step = Dungeon.findStep(this, Dungeon.hero.pos, Dungeon.level.passable, Dungeon.level.heroFOV, true);
					if (step != -1 && step != pos) { move(step); spend(TICK); return true; }
				}

				spend(TICK);
				return true;
		}

		@Override public int damageRoll() { return Random.NormalIntRange(4 + 2 * powerLevel, 8 + 3 * powerLevel); }
		@Override public int attackSkill(Char t) { return 12; }
		@Override public int drRoll() { return Random.NormalIntRange(0, 2); }
		@Override public boolean reset() { return false; }
		@Override public String description() { return Messages.get(this, "desc", lifeTurns, maxLifeTurns); }

		public static class SentinelTracker extends FlavourBuff {
			{ type = Buff.buffType.POSITIVE; }
		}
	}

	// ==================== 内部类：哨卫精灵 ====================

	public static class WandSentinelSprite extends com.zootdungeon.sprites.MobSprite {

		public WandSentinelSprite() {
			super();
			TextureFilm film = textureWithFallback("cola/wand_sentinel.png", Assets.Sprites.STATUE, 12, 15);
			idle = new Animation(2, true); idle.frames(film, 0, 1, 0, 2);
			run = new Animation(4, true); run.frames(film, 0, 1, 2, 1);
			attack = new Animation(8, false); attack.frames(film, 0, 3, 4, 0);
			die = new Animation(8, false); die.frames(film, 0);
			zap = attack.clone();
			play(idle);
		}

		@Override public int blood() { return 0xFF4488FF; }
	}
}
