package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.blobs.ToxicGas;
import com.zootdungeon.actors.buffs.Barrier;
import com.zootdungeon.actors.buffs.Burning;
import com.zootdungeon.actors.buffs.Chill;
import com.zootdungeon.actors.buffs.Corrosion;
import com.zootdungeon.actors.buffs.Degrade;
import com.zootdungeon.actors.buffs.Paralysis;
import com.zootdungeon.actors.buffs.Roots;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.FloatingText;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.SparkParticle;
import com.zootdungeon.items.wands.DamageWand;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.wands.WandOfCorrosion;
import com.zootdungeon.items.wands.WandOfCorruption;
import com.zootdungeon.items.wands.WandOfDisintegration;
import com.zootdungeon.items.wands.WandOfFireblast;
import com.zootdungeon.items.wands.WandOfFrost;
import com.zootdungeon.items.wands.WandOfLightning;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.wands.WandOfPrismaticLight;
import com.zootdungeon.items.wands.WandOfRegrowth;
import com.zootdungeon.items.wands.WandOfTransfusion;
import com.zootdungeon.items.wands.WandOfWarding;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * PITH class feature: remembers the last wand used to modify the next wand's effect.
 */
public class PithWandMemory extends Buff {

	{
		type = buffType.POSITIVE;
	}

	// ========== 联动效果枚举 ==========
	public enum ComboEffect {
		NONE,
		STEAM_BURST,      // 蒸汽爆发：目标周围 3×3 溅射 40% 伤害
		SUPERCONDUCT,     // 超导：连锁至附近额外 2 个敌人
		WILDFIRE,         // 野火：点燃目标及相邻可燃格
		TOXIC_CLOUD,      // 毒气：在目标格生成毒气
		PARTICLE_CASCADE, // 粒子加速：目标减甲 2 回合
		SENTRY_VOLLEY,    // 哨卫齐射：额外发射 2 枚魔法飞弹
		SHATTER,          // 碎石：目标眩晕 1 回合
		BLOOD_SHIELD,     // 血盾：伤害的 30% 转为护盾
		HOLY_JUDGMENT,    // 圣裁：对恶魔/不死额外追加 80% 伤害
		FROSTFIRE,        // 霜火：目标同时燃烧 + 减速
		PLASMA,           // 等离子：点燃目标
		CORROSIVE_VINES,  // 腐蚀藤蔓：缠绕目标 3 回合 + 腐蚀
		ROOT,             // 冻土：定身目标 2 回合
		OVERCHARGE,       // 过载：返还 1 充能
	}

	public static class ComboEntry {
		public final float powerLevel;
		public final ComboEffect effect;
		public ComboEntry(float powerLevel, ComboEffect effect) {
			this.powerLevel = powerLevel;
			this.effect = effect;
		}
	}

	// ========== 联动查找（switch 函数代替 Map 表）==========

	private static final ComboEntry NONE_ENTRY = new ComboEntry(1.0f, ComboEffect.NONE);

	/**
	 * 查找联动条目。使用实例中记忆的法杖信息。
	 * 两连通过 switch 函数查找，三连预留扩展点。
	 * 无联动返回 ComboEntry(1.0, NONE)。
	 */
	public ComboEntry getCombo(Class<? extends Wand> current) {
		if (current == null) return NONE_ENTRY;

		// Try 3-length combo: (first, second) → current (reserved for future)
		if (first != null && second != null) {
			ComboEntry entry = getCombo3(first.wandClass, second.wandClass, current);
			if (entry != null && entry.effect != ComboEffect.NONE) return entry;
		}

		// Fall back to 2-length combo: second → current
		Class<? extends Wand> prev = getMemorized();
		if (prev == null || prev == current) return NONE_ENTRY;
		return getCombo2(prev, current);
	}

	/** 一站式联动：查找 + 派发。Wand.java 中直接调用此方法。 */
	public void applyComboEffect(Hero hero, Wand wand, int cell) {
		ComboEntry combo = getCombo(wand.getClass());
		if (combo.effect == ComboEffect.NONE) return;
		PithWandMemory.applyComboEffect(hero, wand, combo, cell);
	}

	// ========== 完全释放（FULL_DISCHARGE）==========

	private boolean dischargeMode = false;
	private int dischargeCharges = 0;

	/** 开始完全释放：保存充能数。由 Wand.execute(AC_DISCHARGE) 调用。 */
	public void startFullDischarge(Wand wand) {
		dischargeMode = true;
		dischargeCharges = wand.curCharges;
	}

	/** 完全释放伤害倍率。damageRoll() 中调用，>1 表示在充放状态。 */
	public float fullDischargePower() {
		if (!dischargeMode || dischargeCharges <= 0) return 1f;
		Hero hero = (Hero) target;
		int pts = hero.pointsInTalent(Talent.PITH_FULL_DISCHARGE);
		if (pts <= 0) return 1f;
		return 1f + dischargeCharges * (0.15f + 0.1f * pts);
	}

	/** 消耗充能：true=释放（耗全部），false=正常（耗1点）。wandUsed() 调用。 */
	public boolean consumeCharges() {
		if (!dischargeMode) return false;
		dischargeMode = false;
		dischargeCharges = 0;
		return true;
	}

	/** 三连查找（预留）。返回 null 表示无匹配，回退到两连。 */
	private static ComboEntry getCombo3(Class<? extends Wand> first, Class<? extends Wand> second, Class<? extends Wand> current) {
		// TODO: define 3-length combos here
		return null;
	}

	/** 两连查找：prev → current 的联动条目 */
	private static ComboEntry getCombo2(Class<? extends Wand> prev, Class<? extends Wand> current) {
		if (prev == WandOfFireblast.class) {
			if (current == WandOfFrost.class)         return new ComboEntry(1.50f, ComboEffect.STEAM_BURST);
			if (current == WandOfRegrowth.class)       return new ComboEntry(1.60f, ComboEffect.WILDFIRE);
			if (current == WandOfCorrosion.class)      return new ComboEntry(1.40f, ComboEffect.TOXIC_CLOUD);
			if (current == WandOfMagicMissile.class)   return new ComboEntry(1.25f, ComboEffect.PLASMA);
			if (current == WandOfLightning.class)      return new ComboEntry(1.20f, ComboEffect.PLASMA);
			return NONE_ENTRY;
		}
		if (prev == WandOfFrost.class) {
			if (current == WandOfLightning.class)      return new ComboEntry(1.60f, ComboEffect.SUPERCONDUCT);
			if (current == WandOfFireblast.class)      return new ComboEntry(1.40f, ComboEffect.FROSTFIRE);
			if (current == WandOfLivingEarth.class)    return new ComboEntry(1.40f, ComboEffect.ROOT);
			return NONE_ENTRY;
		}
		if (prev == WandOfLightning.class) {
			if (current == WandOfDisintegration.class)    return new ComboEntry(1.50f, ComboEffect.PARTICLE_CASCADE);
			if (current == WandOfFrost.class)             return new ComboEntry(1.40f, ComboEffect.SUPERCONDUCT);
			return NONE_ENTRY;
		}
		if (prev == WandOfDisintegration.class) {
			if (current == WandOfMagicMissile.class)   return new ComboEntry(1.40f, ComboEffect.PARTICLE_CASCADE);
			return NONE_ENTRY;
		}
		if (prev == WandOfMagicMissile.class) {
			if (current == WandOfWarding.class)          return new ComboEntry(1.40f, ComboEffect.SENTRY_VOLLEY);
			if (current == WandOfFireblast.class)        return new ComboEntry(1.30f, ComboEffect.PLASMA);
			return NONE_ENTRY;
		}
		if (prev == WandOfCorruption.class) {
			if (current == WandOfPrismaticLight.class) return new ComboEntry(1.40f, ComboEffect.HOLY_JUDGMENT);
			if (current == WandOfTransfusion.class)    return new ComboEntry(1.50f, ComboEffect.BLOOD_SHIELD);
			return NONE_ENTRY;
		}
		if (prev == WandOfBlastWave.class) {
			if (current == WandOfLivingEarth.class)    return new ComboEntry(1.50f, ComboEffect.SHATTER);
			return NONE_ENTRY;
		}
		if (prev == WandOfPrismaticLight.class) {
			if (current == WandOfCorruption.class)     return new ComboEntry(1.50f, ComboEffect.HOLY_JUDGMENT);
			if (current == WandOfTransfusion.class)    return new ComboEntry(1.40f, ComboEffect.BLOOD_SHIELD);
			return NONE_ENTRY;
		}
		if (prev == WandOfRegrowth.class) {
			if (current == WandOfFireblast.class)      return new ComboEntry(1.60f, ComboEffect.WILDFIRE);
			if (current == WandOfCorrosion.class)      return new ComboEntry(1.40f, ComboEffect.CORROSIVE_VINES);
			if (current == WandOfLivingEarth.class)    return new ComboEntry(1.35f, ComboEffect.ROOT);
			return NONE_ENTRY;
		}
		if (prev == WandOfCorrosion.class) {
			if (current == WandOfFireblast.class)      return new ComboEntry(1.40f, ComboEffect.TOXIC_CLOUD);
			if (current == WandOfRegrowth.class)       return new ComboEntry(1.35f, ComboEffect.CORROSIVE_VINES);
			return NONE_ENTRY;
		}
		if (prev == WandOfTransfusion.class) {
			if (current == WandOfCorruption.class)     return new ComboEntry(1.50f, ComboEffect.BLOOD_SHIELD);
			if (current == WandOfWarding.class)        return new ComboEntry(1.40f, ComboEffect.BLOOD_SHIELD);
			return NONE_ENTRY;
		}
		if (prev == WandOfLivingEarth.class) {
			if (current == WandOfFrost.class)          return new ComboEntry(1.40f, ComboEffect.ROOT);
			if (current == WandOfBlastWave.class)      return new ComboEntry(1.30f, ComboEffect.SHATTER);
			return NONE_ENTRY;
		}
		if (prev == WandOfWarding.class) {
			if (current == WandOfMagicMissile.class)   return new ComboEntry(1.50f, ComboEffect.SENTRY_VOLLEY);
			if (current == WandOfLightning.class)      return new ComboEntry(1.35f, ComboEffect.OVERCHARGE);
			return NONE_ENTRY;
		}
		return NONE_ENTRY;
	}

	/**
	 * 派发联动效果。Called from Wand.applyComboEffect() after onZap().
	 */
	public static void applyComboEffect(Hero hero, Wand wand, ComboEntry combo, int cell) {
		switch (combo.effect) {
			case STEAM_BURST:      steamBurst(hero, wand, cell);          break;
			case SUPERCONDUCT:     superConduct(hero, wand, cell);        break;
			case WILDFIRE:         wildfire(hero, wand, cell);            break;
			case TOXIC_CLOUD:      toxicCloud(hero, wand, cell);          break;
			case PARTICLE_CASCADE: particleCascade(hero, wand, cell);     break;
			case SENTRY_VOLLEY:    sentryVolley(hero, wand, cell);        break;
			case SHATTER:          shatter(hero, wand, cell);             break;
			case BLOOD_SHIELD:     bloodShield(hero, wand, combo, cell);  break;
			case FROSTFIRE:        frostfire(hero, wand, cell);           break;
			case PLASMA:           plasma(hero, wand, cell);              break;
			case CORROSIVE_VINES:  corrosiveVines(hero, wand, cell);      break;
			case ROOT:             root(hero, wand, cell);                break;
			case OVERCHARGE:       overcharge(hero, wand);                break;
			case HOLY_JUDGMENT:    holyJudgment(hero, wand, cell);        break;
			case NONE: break;
		}
	}

	// ========== 各效果独立方法 ==========

	/** 蒸汽爆发：目标周围 3×3 溅射 40% 伤害 */
	public static void steamBurst(Hero hero, Wand wand, int cell) {
		if (!(wand instanceof DamageWand)) return;
		int splashDmg = Math.round(((DamageWand) wand).damageRoll() * 0.4f);
		for (int n : PathFinder.NEIGHBOURS9) {
			Char ch = Actor.findChar(cell + n);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
				ch.damage(splashDmg, wand);
			}
		}
		CellEmitter.center(cell).burst(Speck.factory(Speck.STEAM), 5);
	}

	/** 超导：连锁至附近额外 2 个敌人 */
	public static void superConduct(Hero hero, Wand wand, int cell) {
		for (int i = 0, bounced = 0; i < PathFinder.NEIGHBOURS8.length && bounced < 2; i++) {
			int n = cell + PathFinder.NEIGHBOURS8[i];
			Char ch = Actor.findChar(n);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
				ch.damage(Random.NormalIntRange(4, 12), wand);
				ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				ch.sprite.flash();
				bounced++;
			}
		}
	}

	/** 野火：点燃目标及相邻可燃格 */
	public static void wildfire(Hero hero, Wand wand, int cell) {
		GameScene.add(Blob.seed(cell, 2, Fire.class));
		for (int n : PathFinder.NEIGHBOURS8) {
			if (Random.Float() < 0.5f) {
				GameScene.add(Blob.seed(cell + n, 2, Fire.class));
			}
		}
	}

	/** 毒气：在目标格生成毒气 */
	public static void toxicCloud(Hero hero, Wand wand, int cell) {
		GameScene.add(Blob.seed(cell, 50, ToxicGas.class));
	}

	/** 粒子加速：目标减甲 2 回合 */
	public static void particleCascade(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target != null && target.isAlive()) {
			Buff.affect(target, Degrade.class, Degrade.DURATION);
		}
	}

	/** 哨卫齐射：额外发射 2 枚魔法飞弹 */
	public static void sentryVolley(Hero hero, Wand wand, int cell) {
		for (int i = 0; i < 2; i++) {
			int t = cell;
			Char ch = Actor.findChar(t);
			if (ch == null || ch.alignment == Char.Alignment.ALLY) {
				for (Char c : Actor.chars()) {
					if (c.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[c.pos]) {
						t = c.pos;
						ch = c;
						break;
					}
				}
			}
			if (ch != null && ch.isAlive()) {
				ch.damage(Random.NormalIntRange(2, 8), wand);
				hero.sprite.zap(ch.pos);
			}
		}
	}

	/** 碎石：目标眩晕 1 回合 */
	public static void shatter(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target != null && target.isAlive()) {
			Buff.prolong(target, Paralysis.class, 1f);
		}
	}

	/** 血盾：伤害的 30% 转为护盾 */
	public static void bloodShield(Hero hero, Wand wand, ComboEntry combo, int cell) {
		if (!(wand instanceof DamageWand)) return;
		int estDmg = ((DamageWand) wand).damageRoll();
		int shield = Math.round(estDmg * combo.powerLevel * 0.3f);
		if (shield > 0) {
			Buff.affect(hero, Barrier.class).setShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
	}

	/** 霜火：目标同时燃烧 + 减速 */
	public static void frostfire(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target != null && target.isAlive()) {
			Buff.affect(target, Burning.class).reignite(target, 5f);
			Buff.prolong(target, Chill.class, 5f);
		}
	}

	/** 等离子：点燃目标 */
	public static void plasma(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target != null && target.isAlive()) {
			Buff.affect(target, Burning.class).reignite(target, 5f);
		}
	}

	/** 腐蚀藤蔓：缠绕目标 3 回合 + 腐蚀 */
	public static void corrosiveVines(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target != null && target.isAlive()) {
			Buff.prolong(target, Roots.class, 3f);
			Buff.affect(target, Corrosion.class).set(5f, 6);
		}
	}

	/** 冻土：定身目标 2 回合 */
	public static void root(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target != null && target.isAlive()) {
			Buff.prolong(target, Roots.class, 2f);
		}
	}

	/** 过载：返还 1 充能 */
	public static void overcharge(Hero hero, Wand wand) {
		wand.gainCharge(1f);
		if (hero.sprite != null) {
			hero.sprite.showStatus(CharSprite.POSITIVE, "+1⚡");
		}
	}

	/** 圣裁：对恶魔/不死额外追加 80% 伤害 */
	public static void holyJudgment(Hero hero, Wand wand, int cell) {
		Char target = Actor.findChar(cell);
		if (target == null || !target.isAlive()) return;
		if (Char.hasProp(target, Char.Property.UNDEAD) || Char.hasProp(target, Char.Property.DEMONIC)) {
			if (wand instanceof DamageWand) {
				int bonus = Math.round(((DamageWand) wand).damageRoll() * 0.8f);
				target.damage(bonus, wand);
			}
		}
	}

	// ========== 法杖记忆（含等级）==========

	/** 记录一根法杖的类和等级 */
	private static class WandRecord {
		final Class<? extends Wand> wandClass;
		final int level;
		WandRecord(Class<? extends Wand> wandClass, int level) {
			this.wandClass = wandClass;
			this.level = level;
		}
	}

	private WandRecord memorized = null;
	private WandRecord first = null;
	private WandRecord second = null;

	/** 上一根使用的法杖类 */
	public Class<? extends Wand> getMemorized() {
		return memorized != null ? memorized.wandClass : null;
	}

	/** 上一根使用法杖的等级 */
	public int getMemorizedLevel() {
		return memorized != null ? memorized.level : 0;
	}

	/** 三连中第一根法杖类 */
	public Class<? extends Wand> getFirstWand() {
		return first != null ? first.wandClass : null;
	}

	/** 三连中第一根法杖等级 */
	public int getFirstWandLevel() {
		return first != null ? first.level : 0;
	}

	/** 三连中第二根法杖类 */
	public Class<? extends Wand> getSecondWand() {
		return second != null ? second.wandClass : null;
	}

	/** 三连中第二根法杖等级 */
	public int getSecondWandLevel() {
		return second != null ? second.level : 0;
	}

	/** Memorize a wand with its level. Shifts to 3-chain if applicable. */
	public void memorize(Wand wand) {
		Hero hero = (Hero) target;
		WandRecord rec = new WandRecord(wand.getClass(), wand.buffedLvl());

		if (hero.pointsInTalent(com.zootdungeon.actors.hero.Talent.PITH_CHAIN_STRONGER) >= 2) {
			// 3-chain mode
			if (second != null && first != null) {
				if (hero.pointsInTalent(com.zootdungeon.actors.hero.Talent.PITH_CHAIN_STRONGER) >= 3) {
					first = rec;
					second = null;
				} else {
					first = null;
					second = null;
				}
			} else if (first != null && second == null) {
				second = rec;
			} else {
				first = memorized;
				second = null;
			}
		}
		memorized = rec;
		BuffIndicator.refreshHero();
	}

	/** Clear memory (called after second wand use in basic mode) */
	public void clearMemory() {
		if (first == null && second == null) {
			memorized = null;
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.5f, 0f, 1f);
	}

	@Override
	public String desc() {
		if (memorized == null) return Messages.get(this, "desc_empty");
		String name = Messages.get(memorized.wandClass, "name");
		if (first != null && second != null) {
			return Messages.get(this, "desc_chain", name,
				Messages.get(first.wandClass, "name"), Messages.get(second.wandClass, "name"));
		}
		if (first != null) {
			return Messages.get(this, "desc_pair", name,
				Messages.get(first.wandClass, "name"));
		}
		return Messages.get(this, "desc_one", name);
	}

	private static final String MEMORIZED_CLASS = "memorized_class";
	private static final String MEMORIZED_LVL   = "memorized_lvl";
	private static final String FIRST_CLASS     = "first_class";
	private static final String FIRST_LVL       = "first_lvl";
	private static final String SECOND_CLASS    = "second_class";
	private static final String SECOND_LVL      = "second_lvl";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MEMORIZED_CLASS, memorized != null ? memorized.wandClass.getName() : "");
		bundle.put(MEMORIZED_LVL,   memorized != null ? memorized.level : 0);
		bundle.put(FIRST_CLASS,     first != null ? first.wandClass.getName() : "");
		bundle.put(FIRST_LVL,       first != null ? first.level : 0);
		bundle.put(SECOND_CLASS,    second != null ? second.wandClass.getName() : "");
		bundle.put(SECOND_LVL,      second != null ? second.level : 0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			String s = bundle.getString(MEMORIZED_CLASS);
			if (!s.isEmpty()) {
				int lvl = bundle.getInt(MEMORIZED_LVL);
				memorized = new WandRecord((Class<? extends Wand>) Class.forName(s), lvl);
			}
			s = bundle.getString(FIRST_CLASS);
			if (!s.isEmpty()) {
				int lvl = bundle.getInt(FIRST_LVL);
				first = new WandRecord((Class<? extends Wand>) Class.forName(s), lvl);
			}
			s = bundle.getString(SECOND_CLASS);
			if (!s.isEmpty()) {
				int lvl = bundle.getInt(SECOND_LVL);
				second = new WandRecord((Class<? extends Wand>) Class.forName(s), lvl);
			}
		} catch (ClassNotFoundException e) {
			memorized = null;
			first = null;
			second = null;
		}
	}
}
