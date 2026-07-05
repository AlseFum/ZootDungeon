package com.zootdungeon.actors.hero;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.base.Weapon;
import com.zootdungeon.items.wands.Wand;

/**
 * ROSMONTIS 投掷系统。
 * <p>
 * 统一处理所有投掷行为：
 * <ul>
 *   <li>近战武器：走 weapon.proc() 触发附魔效果，受 ENHANCED_THROWN 等级加成</li>
 *   <li>Wand（普通法杖）：暂未实现，留空</li>
 * </ul>
 * <p>
 * 受天赋影响：
 * <ul>
 *   <li>ROSMONTIS_THROW_MELEE（天赋5）：解锁投掷，决定伤害倍率 50/100/150%</li>
 *   <li>ROSMONTIS_ENHANCED_THROWN（天赋4）：提升投掷结算等级 +1/+2/+3</li>
 * </ul>
 */
public class RosmontisThrow {

	/**
	 * ROSMONTIS 投掷物品到指定格子。
	 * 根据物品类型分派到对应的投掷逻辑。
	 *
	 * @param hero 投掷者
	 * @param item 被投掷的物品（武器 / 法杖等）
	 * @param cell 目标格子
	 */
	public static void throwItem(Hero hero, Item item, int cell) {
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero) return;

		if (item instanceof Weapon) {
			throwWeapon(hero, (Weapon) item, enemy);
		} else if (item instanceof Wand) {
			throwWand(hero, (Wand) item, enemy);
		}
		// 其他物品类型暂不支持投掷
	}

	/**
	 * 投掷近战武器。
	 * 走武器自身的 proc 链（含附魔/特殊效果），受天赋4/5影响。
	 */
	private static void throwWeapon(Hero hero, Weapon weapon, Char enemy) {
		int meleePts = hero.pointsInTalent(Talent.ROSMONTIS_THROW_MELEE);
		if (meleePts <= 0) return;

		float[] dmgMult = {0f, 0.5f, 1.0f, 1.5f};

		// 天赋4：ENHANCED_THROWN 提升结算等级
		int enhancedPts = hero.pointsInTalent(Talent.ROSMONTIS_ENHANCED_THROWN);
		int originalLevel = weapon.level();
		if (enhancedPts > 0) {
			weapon.level(originalLevel + enhancedPts);
		}

		int dmg = Math.round(weapon.damageRoll(hero) * dmgMult[meleePts]);
		dmg = weapon.proc(hero, enemy, dmg);
		enemy.damage(dmg, hero);

		// 恢复武器等级
		if (enhancedPts > 0) {
			weapon.level(originalLevel);
		}

		hero.spendAndNext(hero.attackDelay());
	}

	/**
	 * 投掷法杖（非 MagesStaff 的普通 Wand）。
	 * <p>
	 * TODO: 效果待定。可能的方案：
	 * <ul>
	 *   <li>元素爆炸：落地处产生对应元素 AOE</li>
	 *   <li>消耗充能：每点充能造成对应元素伤害</li>
	 * </ul>
	 */
	private static void throwWand(Hero hero, Wand wand, Char enemy) {
		// TODO: 投掷法杖效果未实现
	}
}
