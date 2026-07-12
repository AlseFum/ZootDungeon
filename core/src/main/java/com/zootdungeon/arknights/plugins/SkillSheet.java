package com.zootdungeon.arknights.plugins;

import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.zootdungeon.actors.buffs.AttackUpBuff;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.PowerStrike;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局技能注册表。每个技能以 {@link SkillDef} 定义，通过 {@link #register(SkillDef)} 进入 {@link #registeredSkills}。
 * <p>
 * 也包含技能所需的 Buff 类作为内部类。
 */
public final class SkillSheet {

	/**
	 * 技能定义：纯数据 + 执行回调。替代原先的 {@code Skill} 类。
	 */
	public static class SkillDef {
		public final String id;
		public final String name;
		public final String desc;
		public final int cost;

		private final Runnable executor;

		public SkillDef(String id, String name, String desc, int cost, Runnable executor) {
			this.id = id;
			this.name = name;
			this.desc = desc;
			this.cost = cost;
			this.executor = executor;
		}

		public void execute(Hero hero) {
			if (executor != null) executor.run();
		}
	}

	public static final Map<String, SkillDef> registeredSkills = new HashMap<>();

	// 技能1：占位符技能
	public static final SkillDef SKILL_1 = new SkillDef("skill_1", "技能1", "占位技能。", 0,
		() -> GLog.i("执行技能 1"));

	// 技能2：占位符技能
	public static final SkillDef SKILL_2 = new SkillDef("skill_2", "技能2", "占位技能。", 0,
		() -> GLog.i("执行技能 2"));

	// 攻击力强化α：伤害×1.3，持续15回合，消耗4 cost
	public static final SkillDef ATTACK_UP_ALPHA = new SkillDef("attack_up_alpha", "攻击力强化α", "攻击伤害提升30%，持续15回合。", 4,
		() -> Buff.affect(com.zootdungeon.Dungeon.hero, AttackUpBuff.class, 15f).setMultiplier(1.3f));

	// 攻击力强化β：伤害×1.5，持续25回合，消耗6 cost
	public static final SkillDef ATTACK_UP_BETA = new SkillDef("attack_up_beta", "攻击力强化β", "攻击伤害提升50%，持续25回合。", 6,
		() -> Buff.affect(com.zootdungeon.Dungeon.hero, AttackUpBuff.class, 25f).setMultiplier(1.5f));

	// 攻击力强化γ：伤害×1.7，持续35回合，消耗8 cost
	public static final SkillDef ATTACK_UP_GAMMA = new SkillDef("attack_up_gamma", "攻击力强化γ", "攻击伤害提升70%，持续35回合。", 8,
		() -> Buff.affect(com.zootdungeon.Dungeon.hero, AttackUpBuff.class, 35f).setMultiplier(1.7f));

	// 下一次攻击强化：给hero下一次攻击提升30%伤害的buff，攻击后失效
	public static final SkillDef NEXT_ATTACK_BOOST = new SkillDef("next_attack_boost", "下一次攻击强化", "下一次攻击伤害提升30%。", 0,
		() -> {
			Buff.affect(com.zootdungeon.Dungeon.hero, PowerStrike.class);
			GLog.p("下一次攻击伤害提升30%");
		});

	// 淬毒射击：远程攻击附加 DoT，持续 15 回合
	public static final SkillDef RANGED_DOT = new SkillDef("ranged_dot", "淬毒射击",
		"远程攻击给目标附加 3 点/回合的持续伤害，持续 5 回合。技能持续 15 回合。", 3,
		() -> {
			Buff.affect(com.zootdungeon.Dungeon.hero, RangedDoTSkillBuff.class, 15f);
			GLog.p("远程攻击将附加持续伤害！");
		});

	static {
		register(SKILL_1);
		register(SKILL_2);
		register(ATTACK_UP_ALPHA);
		register(ATTACK_UP_BETA);
		register(ATTACK_UP_GAMMA);
		register(NEXT_ATTACK_BOOST);
		register(RANGED_DOT);
	}

	public static SkillDef register(SkillDef skill) {
		registeredSkills.put(skill.id, skill);
		return skill;
	}

	public static SkillDef get(String id) {
		return registeredSkills.get(id);
	}

	public static SkillDef[] values() {
		return registeredSkills.values().toArray(new SkillDef[0]);
	}

	private SkillSheet() {
		throw new AssertionError("SkillSheet is a utility class and should not be instantiated");
	}

	// ===== Buff 内部类 =====

	/**
	 * 英雄激活「淬毒射击」后的标记 buff。
	 * 持续期间内，远程攻击会给目标附加 {@link RangedDoTEnemyBuff}。
	 */
	public static class RangedDoTSkillBuff extends FlavourBuff {

		public int dotDamage = 3;
		public int dotTurns = 5;

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.POISON;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0f, 0.8f, 0.2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dotDamage, dotTurns, dispTurns());
		}
	}

	/**
	 * 远程攻击附着的持续伤害（DoT）。
	 * 每回合对目标造成一次伤害，持续若干回合后自动解除。
	 */
	public static class RangedDoTEnemyBuff extends Buff {

		private int damagePerTurn = 2;
		private int turnsLeft = 5;

		private static final String DAMAGE = "damagePerTurn";
		private static final String TURNS  = "turnsLeft";

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DAMAGE, damagePerTurn);
			bundle.put(TURNS, turnsLeft);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			damagePerTurn = bundle.getInt(DAMAGE);
			turnsLeft = bundle.getInt(TURNS);
		}

		public void set(int damage, int turns) {
			if (damage > this.damagePerTurn) {
				this.damagePerTurn = damage;
			}
			this.turnsLeft = Math.max(this.turnsLeft, turns);
		}

		@Override
		public boolean act() {
			if (target.isAlive()) {
				target.damage(damagePerTurn, this);
				if (target.sprite != null) {
					target.sprite.showStatus(CharSprite.NEGATIVE,
							Messages.get(this, "status", damagePerTurn));
				}
			}
			turnsLeft--;
			if (turnsLeft <= 0 || !target.isAlive()) {
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.POISON;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.2f, 0.8f, 0.2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", damagePerTurn, turnsLeft);
		}
	}
}
