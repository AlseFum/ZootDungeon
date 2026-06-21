package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.hero.Hero;
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
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * PITH class feature: remembers the last wand used to modify the next wand's effect.
 * Cleared after the second wand use.
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
		HOLY_JUDGMENT,    // 圣裁：对恶魔/不死 +80% 伤害（damageRoll 中处理）
		FROSTFIRE,        // 霜火：目标同时燃烧 + 减速
		PLASMA,           // 等离子：点燃目标
		CORROSIVE_VINES,  // 腐蚀藤蔓：缠绕目标 3 回合 + 腐蚀
		ROOT,             // 冻土：定身目标 2 回合
		OVERCHARGE,       // 过载：返还 1 充能
	}

	public static class ComboEntry {
		public final float dmgMulti;
		public final ComboEffect effect;
		public ComboEntry(float dmgMulti, ComboEffect effect) {
			this.dmgMulti = dmgMulti;
			this.effect = effect;
		}
	}

	// ========== 联动效果表 ==========
	// key = 上一根法杖类, value = {当前法杖类 → ComboEntry(dmg倍率, 效果类型)}
	private static final Map<Class<? extends Wand>, Map<Class<? extends Wand>, ComboEntry>> COMBO_TABLE = new HashMap<>();

	static {
		// Fireblast(火) → 其他
		Map<Class<? extends Wand>, ComboEntry> fireMap = new HashMap<>();
		fireMap.put(WandOfFrost.class,           new ComboEntry(1.50f, ComboEffect.STEAM_BURST));
		fireMap.put(WandOfRegrowth.class,         new ComboEntry(1.60f, ComboEffect.WILDFIRE));
		fireMap.put(WandOfCorrosion.class,        new ComboEntry(1.40f, ComboEffect.TOXIC_CLOUD));
		fireMap.put(WandOfBlastWave.class,        new ComboEntry(1.30f, ComboEffect.NONE));
		fireMap.put(WandOfMagicMissile.class,     new ComboEntry(1.25f, ComboEffect.PLASMA));
		fireMap.put(WandOfLightning.class,        new ComboEntry(1.20f, ComboEffect.PLASMA));
		fireMap.put(WandOfDisintegration.class,   new ComboEntry(1.20f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfFireblast.class, fireMap);

		// Frost(冰) → 其他
		Map<Class<? extends Wand>, ComboEntry> frostMap = new HashMap<>();
		frostMap.put(WandOfLightning.class,        new ComboEntry(1.60f, ComboEffect.SUPERCONDUCT));
		frostMap.put(WandOfFireblast.class,        new ComboEntry(1.40f, ComboEffect.FROSTFIRE));
		frostMap.put(WandOfLivingEarth.class,       new ComboEntry(1.40f, ComboEffect.ROOT));
		frostMap.put(WandOfBlastWave.class,        new ComboEntry(1.30f, ComboEffect.NONE));
		frostMap.put(WandOfMagicMissile.class,     new ComboEntry(1.20f, ComboEffect.NONE));
		frostMap.put(WandOfDisintegration.class,   new ComboEntry(1.20f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfFrost.class, frostMap);

		// Lightning(电) → 其他
		Map<Class<? extends Wand>, ComboEntry> lightningMap = new HashMap<>();
		lightningMap.put(WandOfDisintegration.class, new ComboEntry(1.50f, ComboEffect.PARTICLE_CASCADE));
		lightningMap.put(WandOfFrost.class,          new ComboEntry(1.40f, ComboEffect.SUPERCONDUCT));
		lightningMap.put(WandOfMagicMissile.class,    new ComboEntry(1.30f, ComboEffect.NONE));
		lightningMap.put(WandOfPrismaticLight.class,  new ComboEntry(1.30f, ComboEffect.NONE));
		lightningMap.put(WandOfBlastWave.class,       new ComboEntry(1.25f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfLightning.class, lightningMap);

		// Disintegration(解离) → 其他
		Map<Class<? extends Wand>, ComboEntry> disMap = new HashMap<>();
		disMap.put(WandOfMagicMissile.class,      new ComboEntry(1.40f, ComboEffect.PARTICLE_CASCADE));
		disMap.put(WandOfLightning.class,         new ComboEntry(1.30f, ComboEffect.NONE));
		disMap.put(WandOfFireblast.class,         new ComboEntry(1.30f, ComboEffect.NONE));
		disMap.put(WandOfBlastWave.class,         new ComboEntry(1.25f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfDisintegration.class, disMap);

		// MagicMissile(飞弹) → 其他
		Map<Class<? extends Wand>, ComboEntry> mmMap = new HashMap<>();
		mmMap.put(WandOfFireblast.class,         new ComboEntry(1.30f, ComboEffect.PLASMA));
		mmMap.put(WandOfFrost.class,             new ComboEntry(1.30f, ComboEffect.NONE));
		mmMap.put(WandOfLightning.class,         new ComboEntry(1.30f, ComboEffect.NONE));
		mmMap.put(WandOfDisintegration.class,    new ComboEntry(1.25f, ComboEffect.NONE));
		mmMap.put(WandOfCorrosion.class,         new ComboEntry(1.25f, ComboEffect.NONE));
		mmMap.put(WandOfPrismaticLight.class,    new ComboEntry(1.20f, ComboEffect.NONE));
		mmMap.put(WandOfWarding.class,           new ComboEntry(1.40f, ComboEffect.SENTRY_VOLLEY));
		COMBO_TABLE.put(WandOfMagicMissile.class, mmMap);

		// Corruption(腐化) → 其他
		Map<Class<? extends Wand>, ComboEntry> corrMap = new HashMap<>();
		corrMap.put(WandOfPrismaticLight.class,  new ComboEntry(1.40f, ComboEffect.HOLY_JUDGMENT));
		corrMap.put(WandOfTransfusion.class,     new ComboEntry(1.50f, ComboEffect.BLOOD_SHIELD));
		corrMap.put(WandOfMagicMissile.class,    new ComboEntry(1.20f, ComboEffect.NONE));
		corrMap.put(WandOfFireblast.class,       new ComboEntry(1.25f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfCorruption.class, corrMap);

		// BlastWave(冲击波) → 其他
		Map<Class<? extends Wand>, ComboEntry> bwMap = new HashMap<>();
		bwMap.put(WandOfLivingEarth.class,        new ComboEntry(1.50f, ComboEffect.SHATTER));
		bwMap.put(WandOfFrost.class,             new ComboEntry(1.35f, ComboEffect.NONE));
		bwMap.put(WandOfFireblast.class,         new ComboEntry(1.30f, ComboEffect.NONE));
		bwMap.put(WandOfMagicMissile.class,      new ComboEntry(1.25f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfBlastWave.class, bwMap);

		// PrismaticLight(棱光) → 其他
		Map<Class<? extends Wand>, ComboEntry> plMap = new HashMap<>();
		plMap.put(WandOfCorruption.class,        new ComboEntry(1.50f, ComboEffect.HOLY_JUDGMENT));
		plMap.put(WandOfDisintegration.class,    new ComboEntry(1.35f, ComboEffect.NONE));
		plMap.put(WandOfLightning.class,         new ComboEntry(1.30f, ComboEffect.NONE));
		plMap.put(WandOfMagicMissile.class,      new ComboEntry(1.25f, ComboEffect.NONE));
		plMap.put(WandOfTransfusion.class,       new ComboEntry(1.40f, ComboEffect.BLOOD_SHIELD));
		COMBO_TABLE.put(WandOfPrismaticLight.class, plMap);

		// Regrowth(生长) → 其他
		Map<Class<? extends Wand>, ComboEntry> regMap = new HashMap<>();
		regMap.put(WandOfFireblast.class,        new ComboEntry(1.60f, ComboEffect.WILDFIRE));
		regMap.put(WandOfCorrosion.class,        new ComboEntry(1.40f, ComboEffect.CORROSIVE_VINES));
		regMap.put(WandOfLivingEarth.class,       new ComboEntry(1.35f, ComboEffect.ROOT));
		regMap.put(WandOfFrost.class,            new ComboEntry(1.30f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfRegrowth.class, regMap);

		// Corrosion(腐蚀) → 其他
		Map<Class<? extends Wand>, ComboEntry> coroMap = new HashMap<>();
		coroMap.put(WandOfFireblast.class,       new ComboEntry(1.40f, ComboEffect.TOXIC_CLOUD));
		coroMap.put(WandOfRegrowth.class,        new ComboEntry(1.35f, ComboEffect.CORROSIVE_VINES));
		coroMap.put(WandOfMagicMissile.class,    new ComboEntry(1.25f, ComboEffect.NONE));
		coroMap.put(WandOfBlastWave.class,       new ComboEntry(1.25f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfCorrosion.class, coroMap);

		// Transfusion(输血) → 其他
		Map<Class<? extends Wand>, ComboEntry> transMap = new HashMap<>();
		transMap.put(WandOfCorruption.class,     new ComboEntry(1.50f, ComboEffect.BLOOD_SHIELD));
		transMap.put(WandOfWarding.class,        new ComboEntry(1.40f, ComboEffect.BLOOD_SHIELD));
		transMap.put(WandOfMagicMissile.class,   new ComboEntry(1.30f, ComboEffect.NONE));
		transMap.put(WandOfPrismaticLight.class, new ComboEntry(1.35f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfTransfusion.class, transMap);

		// LivingEarth(大地) → 其他
		Map<Class<? extends Wand>, ComboEntry> leMap = new HashMap<>();
		leMap.put(WandOfFrost.class,            new ComboEntry(1.40f, ComboEffect.ROOT));
		leMap.put(WandOfFireblast.class,        new ComboEntry(1.35f, ComboEffect.NONE));
		leMap.put(WandOfBlastWave.class,        new ComboEntry(1.30f, ComboEffect.SHATTER));
		leMap.put(WandOfMagicMissile.class,     new ComboEntry(1.20f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfLivingEarth.class, leMap);

		// Warding(哨卫) → 其他
		Map<Class<? extends Wand>, ComboEntry> wardMap = new HashMap<>();
		wardMap.put(WandOfMagicMissile.class,    new ComboEntry(1.50f, ComboEffect.SENTRY_VOLLEY));
		wardMap.put(WandOfLightning.class,       new ComboEntry(1.35f, ComboEffect.OVERCHARGE));
		wardMap.put(WandOfFireblast.class,       new ComboEntry(1.30f, ComboEffect.NONE));
		wardMap.put(WandOfPrismaticLight.class,  new ComboEntry(1.30f, ComboEffect.NONE));
		COMBO_TABLE.put(WandOfWarding.class, wardMap);
	}

	/**
	 * 查找联动条目。无联动返回 ComboEntry(1.0, NONE)。
	 */
	public static ComboEntry getCombo(Class<? extends Wand> previous, Class<? extends Wand> current) {
		if (previous == null || current == null || previous == current) return new ComboEntry(1.0f, ComboEffect.NONE);
		Map<Class<? extends Wand>, ComboEntry> inner = COMBO_TABLE.get(previous);
		if (inner == null) return new ComboEntry(1.0f, ComboEffect.NONE);
		ComboEntry entry = inner.get(current);
		return entry != null ? entry : new ComboEntry(1.0f, ComboEffect.NONE);
	}

	/** The class name of the memorized wand */
	private Class<? extends Wand> memorizedWand = null;
	/** The first wand in a 3-chain (for talent 4 level 2+) */
	private Class<? extends Wand> firstWand = null;
	private Class<? extends Wand> secondWand = null;

	public Class<? extends Wand> getMemorized() {
		return memorizedWand;
	}

	public Class<? extends Wand> getFirstWand() {
		return firstWand;
	}

	public Class<? extends Wand> getSecondWand() {
		return secondWand;
	}

	/** Memorize a wand. If there's already one memorized, shifts to 3-chain if applicable. */
	public void memorize(Wand wand) {
		Hero hero = (Hero) target;
		if (hero.pointsInTalent(com.zootdungeon.actors.hero.Talent.PITH_CHAIN_STRONGER) >= 2) {
			// 3-chain mode
			if (secondWand != null && firstWand != null) {
				// Third wand in chain: cycle - third becomes first of next
				if (hero.pointsInTalent(com.zootdungeon.actors.hero.Talent.PITH_CHAIN_STRONGER) >= 3) {
					Class<? extends Wand> third = wand.getClass();
					firstWand = third;
					secondWand = null;
				} else {
					firstWand = null;
					secondWand = null;
				}
			} else if (firstWand != null && secondWand == null) {
				secondWand = wand.getClass();
			} else {
				firstWand = memorizedWand;
				secondWand = null;
			}
		}
		memorizedWand = wand.getClass();
		BuffIndicator.refreshHero();
	}

	/** Clear memory (called after second wand use in basic mode) */
	public void clearMemory() {
		if (firstWand == null && secondWand == null) {
			memorizedWand = null;
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
		if (memorizedWand == null) return Messages.get(this, "desc_empty");
		String name = Messages.get(memorizedWand, "name");
		if (firstWand != null && secondWand != null) {
			return Messages.get(this, "desc_chain", name,
				Messages.get(firstWand, "name"), Messages.get(secondWand, "name"));
		}
		if (firstWand != null) {
			return Messages.get(this, "desc_pair", name,
				Messages.get(firstWand, "name"));
		}
		return Messages.get(this, "desc_one", name);
	}

	private static final String MEMORIZED = "memorized";
	private static final String FIRST     = "first";
	private static final String SECOND    = "second";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MEMORIZED, memorizedWand != null ? memorizedWand.getName() : "");
		bundle.put(FIRST, firstWand != null ? firstWand.getName() : "");
		bundle.put(SECOND, secondWand != null ? secondWand.getName() : "");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			String s = bundle.getString(MEMORIZED);
			if (!s.isEmpty()) memorizedWand = (Class<? extends Wand>) Class.forName(s);
			s = bundle.getString(FIRST);
			if (!s.isEmpty()) firstWand = (Class<? extends Wand>) Class.forName(s);
			s = bundle.getString(SECOND);
			if (!s.isEmpty()) secondWand = (Class<? extends Wand>) Class.forName(s);
		} catch (ClassNotFoundException e) {
			memorizedWand = null;
			firstWand = null;
			secondWand = null;
		}
	}
}
