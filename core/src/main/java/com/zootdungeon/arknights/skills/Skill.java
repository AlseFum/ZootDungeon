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
	
	public void execute(Hero hero) {
		// TODO: 实现技能执行逻辑
	}
}
