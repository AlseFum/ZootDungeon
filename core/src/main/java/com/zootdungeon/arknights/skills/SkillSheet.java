package com.zootdungeon.arknights.skills;

import com.zootdungeon.actors.buffs.Adrenaline;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.PowerStrike;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.utils.GLog;

import java.util.HashMap;
import java.util.Map;

public final class SkillSheet {
	
	// 注册的技能映射
	public static final Map<String, Skill> registeredSkills = new HashMap<>();
	
	// 技能1：占位符技能
	public static final Skill SKILL_1 = new Skill("skill_1", "base_skill_1", 0) {
		@Override
		public void execute(Hero hero) {
			GLog.i("执行技能 1");
			// TODO: 实现技能1的具体逻辑
		}
	};
	
	// 技能2：占位符技能
	public static final Skill SKILL_2 = new Skill("skill_2", "base_skill_2", 0) {
		@Override
		public void execute(Hero hero) {
			GLog.i("执行技能 2");
			// TODO: 实现技能2的具体逻辑
		}
	};

	// 攻击力强化α：给hero 15回合的Adrenaline buff，消耗4 cost
	public static final Skill ATTACK_UP_ALPHA = new Skill("attack_up_alpha", "attack_up", 4) {
		@Override
		public void execute(Hero hero) {
			RhodesIslandTerminal terminal = hero.belongings.getItem(RhodesIslandTerminal.class);
			
			Buff.affect(hero, Adrenaline.class, 15f);
		}
	};
	
	// 下一次攻击强化：给hero下一次攻击提升30%伤害的buff，攻击后失效
	public static final Skill NEXT_ATTACK_BOOST = new Skill("next_attack_boost", "attack_boost", 0) {
		@Override
		public void execute(Hero hero) {
			Buff.affect(hero, PowerStrike.class);
			GLog.p("下一次攻击伤害提升30%");
		}
	};
	//@@ skill_sheet_def
	// 静态初始化块，注册所有技能
	static {
		register(SKILL_1);
		register(SKILL_2);
		register(ATTACK_UP_ALPHA);
		register(NEXT_ATTACK_BOOST);
	//@@ skill_sheet_static
	//@@ skill_sheet_static+++
	//@@ skill_sheet_static---
	}
	
	/**
	 * 注册一个技能
	 * 
	 * @param skill 要注册的技能
	 * @return 注册的技能
	 */
	public static Skill register(Skill skill) {
		registeredSkills.put(skill.id, skill);
		return skill;
	}
	
	/**
	 * 根据ID获取技能
	 * 
	 * @param id 技能ID
	 * @return 对应的技能，如果不存在则返回null
	 */
	public static Skill get(String id) {
		return registeredSkills.get(id);
	}
	
	/**
	 * 获取所有注册的技能
	 * 
	 * @return 技能数组
	 */
	public static Skill[] values() {
		return registeredSkills.values().toArray(new Skill[0]);
	}
	
	// 私有构造函数，防止实例化
	private SkillSheet() {
		throw new AssertionError("SkillSheet is a utility class and should not be instantiated");
	}
}

