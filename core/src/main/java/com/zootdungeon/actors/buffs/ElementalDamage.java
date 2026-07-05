package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 元素损伤（Elemental Damage）基类。
 * <p>
 * 挂在敌人身上积累损伤值，满 {@link #THRESHOLD} 时触发爆发。
 * 子类实现 {@link #onBurst(int)} 定义具体爆发效果。
 * MANTRA 天赋逻辑通过 {@link MantraDeclarationBuff#onElementalBurst} 接入。
 */
public abstract class ElementalDamage extends Buff {

	public static final int THRESHOLD = 100;

	protected int accumulation = 0;

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	/** 有效阈值（受 ALLY_TRIGGER Lv3 影响可降低） */
	public int effectiveThreshold() {
		int t = THRESHOLD;
		Hero hero = (Hero) Dungeon.hero;
		if (hero != null && hero.hasTalent(Talent.MANTRA_ALLY_TRIGGER)
				&& hero.pointsInTalent(Talent.MANTRA_ALLY_TRIGGER) >= 3) {
			t = 80;
		}
		return t;
	}

	/** 叠加损伤 */
	public void add(int amount) {
		accumulation += amount;
		if (accumulation >= effectiveThreshold()) {
			detonate();
		}
	}

	/** 直接加损伤，独立触发爆发检测 */
	public void addDirect(int amount) {
		accumulation += amount;
		if (accumulation >= effectiveThreshold()) {
			detonate();
		}
	}

	/** 子类实现：损伤爆发时的具体效果。damage 已包含爆发伤害。 */
	protected abstract void onBurst(int damage);

	/** SPLASH_TRIGGER 天赋加成：子类可在此增强爆发效果（范围/持续/减益等）。 */
	protected void enhanceBurst(int talentPts) {}

	/** 损伤爆发 */
	private void detonate() {
		Hero hero = (Hero) Dungeon.hero;
		if (hero == null || !hero.isAlive()) return;

		int baseDmg = 10 + accumulation;

		// SPLASH_TRIGGER 天赋：各子类自定义增强效果
		if (hero.hasTalent(Talent.MANTRA_SPLASH_TRIGGER)) {
			enhanceBurst(hero.pointsInTalent(Talent.MANTRA_SPLASH_TRIGGER));
		}

		// 宣告标记 → 伤害 +50%
		MantraDeclarationBuff decl = target.buff(MantraDeclarationBuff.class);
		int dmg = decl != null ? Math.round(baseDmg * 1.5f) : baseDmg;

		// MANTRA 天赋处理（溅射、延长宣告）
		MantraDeclarationBuff.onElementalBurst(target, this, dmg);

		// 子类具体爆发效果
		onBurst(dmg);

		accumulation = 0;
		detach();
	}

	@Override
	public boolean act() {
		int decay = decayPerTurn();
		if (decay > 0) {
			accumulation = Math.max(0, accumulation - decay);
		}
		if (accumulation <= 0) {
			detach();
			return true;
		}
		spend(TICK);
		return true;
	}

	/** 每回合衰减量。子类可覆盖（如某种损伤需要自然衰减时）。 */
	protected int decayPerTurn() {
		return 0;
	}

	public int getAccumulation() { return accumulation; }

	@Override
	public int icon() { return BuffIndicator.CORRUPT; }

	@Override
	public abstract String desc();

	@Override
	public abstract void tintIcon(com.watabou.noosa.Image icon);

	private static final String ACCUM = "accumulation";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ACCUM, accumulation);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		accumulation = bundle.getInt(ACCUM);
	}
}
