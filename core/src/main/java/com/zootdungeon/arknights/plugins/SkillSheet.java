package com.zootdungeon.arknights.plugins;

import com.zootdungeon.actors.buffs.AttackUpBuff;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.PowerStrike;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.utils.GLog;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局技能注册表。每个技能以 {@link SkillDef} 定义，通过 {@link #register(SkillDef)} 进入 {@link #registeredSkills}。
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

	static {
		register(SKILL_1);
		register(SKILL_2);
		register(ATTACK_UP_ALPHA);
		register(ATTACK_UP_BETA);
		register(ATTACK_UP_GAMMA);
		register(NEXT_ATTACK_BOOST);
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
}
