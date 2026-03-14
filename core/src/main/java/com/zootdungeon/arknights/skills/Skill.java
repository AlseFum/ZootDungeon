package com.zootdungeon.arknights.skills;

import com.zootdungeon.actors.hero.Hero;

public class Skill
{
	public String id;
	public String base;
    public int cost=4;
	
	public Skill(String id, String base) {
		this.id = id;
		this.base = base;
		this.cost = 0;
	}
	
	public Skill(String id, String base, int cost) {
		this.id = id;
		this.base = base;
		this.cost = cost;
	}
	
	public String name() {
		switch (base) {
			case "attack_up": return "攻击力强化α";
			case "attack_boost": return "下一次攻击强化";
			case "base_skill_1": return "技能1";
			case "base_skill_2": return "技能2";
			default: return id;
		}
	}

	public String desc() {
		switch (base) {
			case "attack_up": return "获得15回合肾上腺素效果，提升攻击伤害。";
			case "attack_boost": return "下一次攻击伤害提升30%。";
			case "base_skill_1": return "占位技能。";
			case "base_skill_2": return "占位技能。";
			default: return "";
		}
	}

	public void execute(Hero hero) {
		// TODO: 实现技能执行逻辑
	}
}
