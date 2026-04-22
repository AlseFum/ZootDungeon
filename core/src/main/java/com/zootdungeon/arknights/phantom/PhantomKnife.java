package com.zootdungeon.arknights.phantom;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.GhostSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Random;

public class PhantomKnife extends AmbushWeapon {

	private static final int PHANTOM_KNIFE_IMAGE;

	static {
		SpriteRegistry.texture("sheet.cola.phantom_knife", "cola/phantom_knife.png")
				.grid(64, 64)
				.label("phantom_knife");
		PHANTOM_KNIFE_IMAGE = SpriteRegistry.byLabel("phantom_knife");
	}

	private static final String CHARGE = "charge";
	private static final String CHARGE_CAP = "chargeCap";

	private int charge = 0;
	private int chargeCap = 10;

	{
		image = PHANTOM_KNIFE_IMAGE;
		tier = 1;
		bones = false;
		ambushRate = 0.5f;
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		StringBuilder sb = new StringBuilder(Messages.get(this, "desc_intro"));
		if (charge > 0) {
			sb.append("\n\n").append(Messages.get(this, "desc_charge", charge, chargeCap));
		}
		sb.append("\n\n").append(Messages.get(this, "desc_body"));
		return sb.toString();
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (attacker instanceof Hero) {
			Hero hero = (Hero) attacker;

			boolean isAmbush = defender instanceof Mob && ((Mob) defender).surprisedBy(hero);

			if (isAmbush) {
				boolean willKill = defender.HP <= damage;

				if (charge > 0 && !willKill) {
					trySummonAndAttack(hero, defender);
				}

				if (willKill && charge < chargeCap) {
					charge++;
					updateQuickslot();
					GLog.p(Messages.get(this, "msg_charge_up", charge, chargeCap));
				}
			}
		}

		return damage;
	}

	private void trySummonAndAttack(Hero hero, Char enemy) {
		int summonPos = findNearbyEmptyCell(enemy.pos);
		if (summonPos == -1) {
			return;
		}

		int basePower = tier * 2;
		int chargePower = Math.min(charge, chargeCap);
		int powerLevel = basePower + chargePower;

		SummonedMinion minion = new SummonedMinion(this, powerLevel, tier);
		minion.pos = summonPos;
		minion.state = minion.HUNTING;
		minion.setTarget(enemy.pos);

		GameScene.add(minion);

		CellEmitter.get(summonPos).burst(Speck.factory(Speck.STAR), 6);
		Sample.INSTANCE.play(Assets.Sounds.MELD);

		charge--;
		updateQuickslot();

		GLog.p(Messages.get(this, "msg_summoned", minion.name()));

		if (enemy.isAlive() && Actor.findChar(enemy.pos) == enemy) {
			minion.enemy = enemy;
			minion.enemySeen = true;
			minion.state = minion.HUNTING;
		}
	}

	private void refundCharge(int amount) {
		if (amount <= 0) return;
		int before = charge;
		charge = Math.min(chargeCap, charge + amount);
		if (charge != before) {
			updateQuickslot();
			GLog.p(Messages.get(this, "msg_charge_up", charge, chargeCap));
		}
	}

	private int findNearbyEmptyCell(int centerPos) {
		for (int offset : PathFinder.NEIGHBOURS8) {
			int pos = centerPos + offset;
			if (Dungeon.level.passable[pos] && Actor.findChar(pos) == null) {
				return pos;
			}
		}

		for (int offset : PathFinder.NEIGHBOURS8) {
			int pos = centerPos + offset * 2;
			if (pos >= 0 && pos < Dungeon.level.length()
					&& Dungeon.level.passable[pos]
					&& Actor.findChar(pos) == null) {
				return pos;
			}
		}

		return -1;
	}

	@Override
	public Item upgrade() {
		chargeCap += 2;
		if (chargeCap > 20) chargeCap = 20;
		return super.upgrade();
	}

	public PhantomKnife randomize() {
		tier = Random.IntRange(1, 5);
		level(Random.IntRange(0, 3));
		chargeCap = Random.IntRange(8, 20);
		charge = Random.IntRange(0, chargeCap);
		ambushRate = Random.Float(0.4f, 1.2f);
		return this;
	}

	@Override
	public Item random() {
		return randomize();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
		bundle.put(CHARGE_CAP, chargeCap);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
		chargeCap = bundle.getInt(CHARGE_CAP);
		if (chargeCap == 0) chargeCap = 10;
	}

	public static class SummonedMinion extends Mob {

		private PhantomKnife weapon;
		private int powerLevel;
		private int weaponTier;
		private int idleTurns = 0;
		private static final int MAX_IDLE_TURNS = 5;

		{
			spriteClass = GhostSprite.class;
			alignment = Alignment.ALLY;
			lootChance = 0f;
			EXP = 0;
			state = HUNTING;
		}

		/** For bundle restore only; fields are filled by {@link #restoreFromBundle}. */
		public SummonedMinion() {
			this(null, 1, 1);
		}

		public SummonedMinion(PhantomKnife weapon, int powerLevel, int weaponTier) {
			this.weapon = weapon;
			this.powerLevel = powerLevel;
			this.weaponTier = weaponTier;

			HP = HT = 10 + powerLevel * 2 + weaponTier * 3;
			defenseSkill = 2 + powerLevel + weaponTier;
			maxLvl = Math.max(1, (powerLevel + weaponTier + 1) / 2);
		}

		@Override
		public int damageRoll() {
			int base = Random.NormalIntRange(1, 3);
			return base + powerLevel + weaponTier;
		}

		@Override
		public int attackSkill(Char target) {
			return 8 + powerLevel * 2 + weaponTier * 2;
		}

		@Override
		public int drRoll() {
			return Random.NormalIntRange(0, (powerLevel + weaponTier) / 2);
		}

		@Override
		public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti, int hitCount) {
			boolean wasAlive = enemy != null && enemy.isAlive();
			boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti, hitCount);
			if (wasAlive && enemy != null && !enemy.isAlive() && enemy.alignment == Alignment.ENEMY) {
				if (weapon != null) weapon.refundCharge(1);
			}
			return result;
		}

		@Override
		public String name() {
			return Messages.get(SummonedMinion.class, "name", powerLevel, weaponTier);
		}

		@Override
		public String description() {
			return Messages.get(SummonedMinion.class, "desc");
		}

		@Override
		protected boolean act() {
			if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
				die(null);
				return true;
			}

			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
				fieldOfView = new boolean[Dungeon.level.length()];
			}

			Dungeon.level.updateFieldOfView(this, fieldOfView);

			if (enemy == null || !enemy.isAlive() || enemy == this) {
				enemy = findTargetInFOV();
				if (enemy != null) {
					enemySeen = true;
					idleTurns = 0;
				} else {
					idleTurns++;
					if (idleTurns >= MAX_IDLE_TURNS) {
						die(null);
						return true;
					}
					state = PASSIVE;
				}
			} else {
				idleTurns = 0;
			}

			return super.act();
		}

		private Char findTargetInFOV() {
			Char closest = null;
			int closestDist = Integer.MAX_VALUE;

			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.alignment == Alignment.ENEMY && mob.isAlive()
						&& fieldOfView[mob.pos] && mob.invisible <= 0) {
					int dist = Dungeon.level.distance(pos, mob.pos);
					if (dist < closestDist) {
						closest = mob;
						closestDist = dist;
					}
				}
			}

			return closest;
		}

		@Override
		public void die(Object cause) {
			super.die(cause);
		}

		public void setTarget(int cell) {
			target = cell;
		}

		@Override
		public Item createLoot() {
			return null;
		}
	}
}
