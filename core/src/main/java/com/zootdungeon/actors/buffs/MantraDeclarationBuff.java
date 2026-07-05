package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.wands.WandOfCorrosion;
import com.zootdungeon.items.wands.WandOfFireblast;
import com.zootdungeon.items.wands.WandOfFrost;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * MANTRA class feature: spend wand charges to make a "declaration".
 * When enemies in FOV perform the declared action, the buff triggers
 * a wand-specific effect on them.
 *
 * Basic trigger types: ATTACK (enemy attacks), MOVE (enemy moves), CAST (enemy uses magic).
 * Trigger actions cause the infused wand's effect to proc on the enemy.
 */
public class MantraDeclarationBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	public enum TriggerType {
		ATTACK,  // triggers when enemy attacks
		MOVE,    // triggers when enemy moves
		CAST     // triggers when enemy uses magic/abilities
	}

	private TriggerType triggerType;
	private Class<? extends Wand> infusedWand;
	private int wandLevel = 1;
	private int triggerCount = 0;

	/** Create a declaration. Costs 2 charges. Marks all enemies in FOV. */
	public static void declare(final Hero hero) {
		Wand wandToUse = null;
		for (com.zootdungeon.items.Item item : hero.belongings.backpack.items) {
			if (item instanceof Wand && ((Wand) item).curCharges >= 2) {
				wandToUse = (Wand) item;
				break;
			}
		}
		if (wandToUse == null) {
			GLog.w(Messages.get(MantraDeclarationBuff.class, "no_wand"));
			return;
		}

		// Store declared wand info on hero for ALLY_TRIGGER
		MantraDeclarationBuff heroBuff = Buff.affect(hero, MantraDeclarationBuff.class, 30f);
		heroBuff.infusedWand = wandToUse.getClass();
		heroBuff.wandLevel = wandToUse.buffedLvl();

		wandToUse.curCharges -= 2;

		int marked = 0;
		for (com.zootdungeon.actors.mobs.Mob mob : Dungeon.level.mobs) {
			if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()
					&& Dungeon.level.heroFOV[mob.pos]) {
				Buff.affect(mob, MantraDeclarationBuff.class, 30f);
				marked++;
			}
		}

		GLog.p(Messages.get(MantraDeclarationBuff.class, "declared", marked));
		hero.spendAndNext(hero.attackDelay());
	}

	/** ALLY_TRIGGER: 标记敌人移动时触发法杖效果 */
	public static void onEnemyMove(Char mob) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.hasTalent(Talent.MANTRA_ALLY_TRIGGER)) return;
		if (mob.buff(MantraDeclarationBuff.class) == null) return;
		MantraDeclarationBuff heroBuff = hero.buff(MantraDeclarationBuff.class);
		if (heroBuff == null || heroBuff.infusedWand == null) return;
		fireWandAt(heroBuff, mob);
	}

	/** ALLY_TRIGGER: 标记敌人攻击时触发法杖效果 */
	public static void onEnemyAttack(Char mob) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.hasTalent(Talent.MANTRA_ALLY_TRIGGER)
				|| hero.pointsInTalent(Talent.MANTRA_ALLY_TRIGGER) < 2) return;
		if (mob.buff(MantraDeclarationBuff.class) == null) return;
		MantraDeclarationBuff heroBuff = hero.buff(MantraDeclarationBuff.class);
		if (heroBuff == null || heroBuff.infusedWand == null) return;
		fireWandAt(heroBuff, mob);
	}

	private static void fireWandAt(MantraDeclarationBuff heroBuff, Char target) {
		try {
			Wand w = heroBuff.infusedWand.getDeclaredConstructor().newInstance();
			w.level(heroBuff.wandLevel);
			w.wandProc(target, 1);
		} catch (Exception e) {}
	}

	/** Called when an enemy does something that matches the trigger */
	public void onTrigger(Char enemy, Char target) {
		if (infusedWand == null || triggerType == null) return;
		try {
			Wand w = infusedWand.getDeclaredConstructor().newInstance();
			w.level(wandLevel);
			w.wandProc(enemy, 1);
			triggerCount++;

			// Talent 3: splash to nearby enemies
			Hero hero = (Hero) this.target;
			if (hero.hasTalent(Talent.MANTRA_SPLASH_TRIGGER)) {
				int pts = hero.pointsInTalent(Talent.MANTRA_SPLASH_TRIGGER);
				float[] splashChances = {0f, 0.3f, 0.5f, 0.7f};
				for (Char ch : Actor.chars()) {
					if (ch != enemy && ch.alignment == Char.Alignment.ENEMY
							&& ch.isAlive() && Dungeon.level.adjacent(enemy.pos, ch.pos)) {
						if (Random.Float() < splashChances[pts]) {
							w.wandProc(ch, 1);
						}
					}
				}
			}

			// Talent 5: may extend buff
			if (hero.hasTalent(Talent.MANTRA_ETERNAL_BUFF)) {
				int pts = hero.pointsInTalent(Talent.MANTRA_ETERNAL_BUFF);
				float[] extendChances = {0f, 0.2f, 0.35f, 0.6f};
				if (Random.Float() < extendChances[pts]) {
					this.spend(-5f); // extend by 5 turns
					GLog.p(Messages.get(this, "extended"));
				}
			}
		} catch (Exception e) {
			// wand class not instantiatable
		}
	}

	/** MANTRA 法杖命中时调用：叠加对应类型的元素损伤 */
	public static void onWandHit(Char target, int wandLevel, int chargesUsed, Wand wand) {
		Hero hero = Dungeon.hero;
		if (hero == null || wand == null) return;

		Class<? extends ElementalDamage> edClass = BurnDamage.class;
		if (wand instanceof WandOfFireblast)   {}
		else if (wand instanceof WandOfCorrosion)   edClass = CorrodeDamage.class;
		else if (wand instanceof WandOfFrost)       edClass = WitherDamage.class;
		else if (wand instanceof WandOfLivingEarth) edClass = WitherDamage.class;

		ElementalDamage mark = (ElementalDamage) Buff.affect(target, edClass);
		mark.add(wandLevel * 10 + chargesUsed * 5);
		BuffIndicator.refreshHero();
	}

	/**
	 * ElementalDamage 爆发时由基类调用，处理 MANTRA 天赋逻辑。
	 * @return 增强后的最终伤害
	 */
	/** 爆发时 MANTRA 天赋处理（溅射 + 延长宣告）。damage 已包含宣告加成。 */
	public static void onElementalBurst(Char target, ElementalDamage ed, int damage) {
		Hero hero = Dungeon.hero;
		if (hero == null) return;

		MantraDeclarationBuff decl = target.buff(MantraDeclarationBuff.class);

		// ETERNAL_BUFF Lv1：爆发延长该敌人宣告
		if (hero.hasTalent(Talent.MANTRA_ETERNAL_BUFF)
				&& hero.pointsInTalent(Talent.MANTRA_ETERNAL_BUFF) >= 1
				&& decl != null) {
			decl.extend(5f);
		}

		// ETERNAL_BUFF Lv2：爆发延长全部敌人宣告
		if (hero.hasTalent(Talent.MANTRA_ETERNAL_BUFF)
				&& hero.pointsInTalent(Talent.MANTRA_ETERNAL_BUFF) >= 2) {
			for (Char ch : Actor.chars()) {
				if (ch != target && ch.alignment == Char.Alignment.ENEMY) {
					MantraDeclarationBuff other = ch.buff(MantraDeclarationBuff.class);
					if (other != null) other.extend(3f);
				}
			}
		}

		// ETERNAL_BUFF Lv3：给其他敌人累积损伤
		if (hero.hasTalent(Talent.MANTRA_ETERNAL_BUFF)
				&& hero.pointsInTalent(Talent.MANTRA_ETERNAL_BUFF) >= 3) {
			int splashAccum = Math.round(damage * 0.3f);
			for (Char ch : Actor.chars()) {
				if (ch != target && ch.alignment == Char.Alignment.ENEMY) {
					ElementalDamage other = (ElementalDamage) Buff.affect(ch, ed.getClass());
					other.addDirect(splashAccum);
				}
			}
		}

		// SPLASH_TRIGGER Lv3：溅射相邻
		if (hero.hasTalent(Talent.MANTRA_SPLASH_TRIGGER)
				&& hero.pointsInTalent(Talent.MANTRA_SPLASH_TRIGGER) >= 3) {
			int splashDmg = Math.round(damage * 0.5f);
			for (int n : PathFinder.NEIGHBOURS8) {
				Char ch = Actor.findChar(target.pos + n);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
					Buff.affect(ch, ed.getClass()).addDirect(splashDmg);
				}
			}
		}
	}

	public TriggerType getTriggerType() { return triggerType; }
	public Class<? extends Wand> getWandClass() { return infusedWand; }
	public int getWandLevel() { return wandLevel; }
	public void extend(float turns) { spend(-turns); }

	@Override
	public int icon() { return BuffIndicator.MIND_VISION; }
	@Override
	public void tintIcon(Image icon) { icon.hardlight(1f, 0.3f, 1f); }

	@Override
	public String desc() {
		if (infusedWand == null) return Messages.get(this, "desc_empty");
		return Messages.get(this, "desc",
				Messages.get(triggerType, "name"),
				Messages.get(infusedWand, "name"),
				triggerCount, dispTurns());
	}

	private static final String TRIGGER_TYPE = "triggerType";
	private static final String INFUSED_WAND = "infusedWand";
	private static final String WAND_LEVEL    = "wandLevel";
	private static final String TRIGGER_COUNT = "triggerCount";

	@Override
	@SuppressWarnings("unchecked")
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TRIGGER_TYPE, triggerType != null ? triggerType.name() : "");
		bundle.put(INFUSED_WAND, infusedWand != null ? infusedWand.getName() : "");
		bundle.put(WAND_LEVEL, wandLevel);
		bundle.put(TRIGGER_COUNT, triggerCount);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		String s = bundle.getString(TRIGGER_TYPE);
		if (!s.isEmpty()) triggerType = TriggerType.valueOf(s);
		s = bundle.getString(INFUSED_WAND);
		try {
			if (!s.isEmpty()) infusedWand = (Class<? extends Wand>) Class.forName(s);
		} catch (ClassNotFoundException e) {
			infusedWand = null;
		}
		wandLevel = bundle.getInt(WAND_LEVEL);
		triggerCount = bundle.getInt(TRIGGER_COUNT);
	}
}
