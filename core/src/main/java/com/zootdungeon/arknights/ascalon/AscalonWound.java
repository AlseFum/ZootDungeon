package com.zootdungeon.arknights.ascalon;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class AscalonWound extends Buff {

	public static final float DURATION = 25f;

	private static final float DOT_MULT_NO_FOG = 0.10f;
	private static final float DOT_MULT_IN_FOG = 0.20f;
	/** 叠层时，在基础伤害倍率上最多再增加的量（边际递减趋近该值）。 */
	private static final float STACK_DOT_EXTRA_CAP = 0.12f;
	private static final float MAX_DOT_MULT = 0.40f;

	/** 每跳 DoT 吸血、击杀奶的基础比例；叠层时额外提升，见 {@link #STACK_STEAL_EXTRA_CAP}。 */
	private static final float LIFE_STEAL_FROM_DOT_RATE = 0.10f;
	private static final float KILL_HEAL_MAX_HP_RATE = 0.10f;
	private static final float STACK_STEAL_EXTRA_CAP = 0.08f;
	private static final float MAX_LIFE_STEAL = 0.40f;
	private static final float MAX_KILL_HEAL_RATE = 0.40f;

	private static final float STACK_CURVE = 0.45f;

	public Char caster;
	/** 每跳对目标造成的固定伤害（施加时由攻击骰与是否在雾中一次性算出）。 */
	public int damagePerTick;
	/** 第几次调用 {@link #set(Char, float, boolean)}（含首次），用于叠层加成。 */
	public int stackCount;
	private float left = DURATION;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	private static final String CASTER_ID = "caster_id";
	private static final String DAMAGE_PER_TICK = "damagePerTick";
	private static final String LEFT = "left";
	private static final String HERO_ATTACK_DAMAGE_LEGACY = "heroAttackDamage";
	private static final String IN_FOG_WHEN_ATTACKED = "inFogWhenAttacked";
	private static final String STACK_COUNT = "stackCount";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CASTER_ID, caster != null ? caster.id() : -1);
		bundle.put(DAMAGE_PER_TICK, damagePerTick);
		bundle.put(STACK_COUNT, stackCount);
		bundle.put(LEFT, left);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(CASTER_ID)) {
			int id = bundle.getInt(CASTER_ID);
			Actor a = id >= 0 ? Actor.findById(id) : null;
			caster = a instanceof Char ? (Char) a : null;
		} else {
			caster = Dungeon.hero;
		}
		if (bundle.contains(DAMAGE_PER_TICK)) {
			damagePerTick = bundle.getInt(DAMAGE_PER_TICK);
		} else {
			float oldSnap = bundle.getFloat(HERO_ATTACK_DAMAGE_LEGACY);
			boolean fog = bundle.getBoolean(IN_FOG_WHEN_ATTACKED);
			float mult = fog ? DOT_MULT_IN_FOG : DOT_MULT_NO_FOG;
			damagePerTick = Math.max(1, Math.round(oldSnap * mult));
		}
		if (bundle.contains(STACK_COUNT)) {
			stackCount = bundle.getInt(STACK_COUNT);
		} else {
			stackCount = 1;
		}
		if (bundle.contains(LEFT)) {
			left = bundle.getFloat(LEFT);
		} else {
			left = DURATION;
		}
	}

	/**
	 * 施加或刷新创伤：延长持续时间，按本次快照叠层并更新每跳伤害（取历史与本次较大者）。
	 *
	 * @param attackSnapshot 本击用于推算 DoT 的强度（常用 {@code proc} 里本击伤害）
	 */
	public static void applyFrom(Char caster, Char target, float attackSnapshot, boolean inFog) {
		if (caster == null || !target.isAlive()) {
			return;
		}
		AscalonWound w = target.buff(AscalonWound.class);
		if (w == null) {
			w = new AscalonWound();
			if (!w.attachTo(target)) {
				return;
			}
		}
		float duration = DURATION * target.resist(AscalonWound.class);
		w.extend(duration);
		w.set(caster, Math.max(1f, attackSnapshot), inFog);
	}

	/**
	 * @param attackSnapshot 施加当次用于计算每跳伤害的攻击骰（或等价数值）
	 * @param inFog 仅影响每跳伤害系数，不影响是否吸血
	 */
	public void set(Char caster, float attackSnapshot, boolean inFog) {
		this.caster = caster;
		stackCount++;
		float base = inFog ? DOT_MULT_IN_FOG : DOT_MULT_NO_FOG;
		float mult = base + diminishingExtra(stackCount, STACK_DOT_EXTRA_CAP);
		mult = Math.min(mult, MAX_DOT_MULT);
		int next = Math.max(1, Math.round(attackSnapshot * mult));
		this.damagePerTick = Math.max(this.damagePerTick, next);
	}

	/** 第 n 次叠层时额外量 = cap * (1 - 1/(1 + k*(n-1)))，n=1 时为 0，之后边际递减。 */
	private static float diminishingExtra(int stacks, float cap) {
		return cap * (1f - 1f / (1f + STACK_CURVE * Math.max(0, stacks - 1)));
	}

	public void extend(float duration) {
		left = Math.max(left, duration);
	}

	@Override
	public int icon() {
		return BuffIndicator.BLEEDING;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.8f, 0.2f, 0.2f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, left / DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) left);
	}

	public float speedFactor() {
		return 0.82f;
	}

	@Override
	public boolean act() {
		if (!target.isAlive() || left <= 0) {
			detach();
			return true;
		}

		int hpBefore = target.HP;
		target.damage(damagePerTick, this);

		if (caster != null && caster.isAlive() && target.HP < hpBefore) {
			int lost = hpBefore - target.HP;
			float steal = Math.min(
					LIFE_STEAL_FROM_DOT_RATE + diminishingExtra(stackCount, STACK_STEAL_EXTRA_CAP),
					MAX_LIFE_STEAL);
			healCaster(Math.round(lost * steal));
		}

		if (!target.isAlive() && target instanceof Mob && caster != null && caster.isAlive()) {
			float killRate = Math.min(
					KILL_HEAL_MAX_HP_RATE + diminishingExtra(stackCount, STACK_STEAL_EXTRA_CAP),
					MAX_KILL_HEAL_RATE);
			int heal = Math.round(caster.HT * killRate);
			if (heal > 0) {
				healCaster(heal);
				GLog.p(Messages.get(this, "heal", heal));
			}
		}

		left -= TICK;
		spend(TICK);
		return true;
	}

	private void healCaster(int amount) {
		if (amount <= 0 || caster == null || !caster.isAlive()) {
			return;
		}
		caster.HP = Math.min(caster.HP + amount, caster.HT);
		if (caster.sprite != null && caster.HP < caster.HT) {
			caster.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns(left));
	}
}
