package com.zootdungeon.actors.buffs;

public class OverclockedStrikes extends Buff {

	public int bonusHits = 2;
	public float damageMult = 0.8f;
	public int left = 0;

	{
		type = buffType.POSITIVE;
	}

	public OverclockedStrikes set(int duration) {
		left = Math.max(0, duration);
		return this;
	}

	@Override
	public boolean act() {
		if (left <= 0) {
			detach();
			return true;
		}
		left--;
		spend(TICK);
		return true;
	}

	public int bonusHitCount() {
		return Math.max(0, bonusHits);
	}

	public float damageMultiplier() {
		return damageMult <= 0 ? 1f : damageMult;
	}

	@Override
	public String desc() {
		return "攻击次数 +" + bonusHitCount() + "，伤害 -" + Math.round((1f - damageMultiplier()) * 100f) + "%（剩余 " + left + " 回合）";
	}
}

