package com.zootdungeon.arknights.misc;

import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.DefenseDown;
import com.zootdungeon.actors.buffs.DodgeBoostBuff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.buffs.Vertigo;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 罗德岛·拳套：
 * 一把武器整合了多种效果，可通过 ItemEditor 独立启用/禁用每个效果。
 * <p>
 * 支持的效果：
 * - 穿甲 : 每次命中按比例「无视」目标防御
 * - 蓄能 : 攻击/闪避累积蓄能，释放高伤害一击
 * - 暴击 : 命中概率提升本次攻击伤害
 * - 破甲 : 命中叠加破甲Debuff，后续命中无视更多防御
 * - 闪避 : 提升闪避率，闪避成功后免费反击
 * - 冲击 : 命中概率击退敌人并施加控制debuff
 * - 锁定 : 连续命中同一目标时叠加伤害加成
 */
public class RhodesGauntlet extends FastWeapon implements DodgeBoostBuff.Source {

    // ===== Config System for ItemEditor =====
    private final Map<String, Object> config = new HashMap<>();

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT;
        hitSoundPitch = 1.15f;

        tier = 0;
        DLY = 0.5f;

        defaultAction = AC_RELEASE;
    }

    // ===== Config Keys =====
    public static final String PIERCE_ENABLED = "pierceEnabled";
    public static final String PIERCE_RATE = "pierceRate";
    public static final String CRIT_ENABLED = "critEnabled";
    public static final String CRIT_CHANCE = "critChance";
    public static final String CRIT_MULT = "critMult";
    public static final String DODGE_ENABLED = "dodgeEnabled";
    public static final String DODGE_BONUS = "dodgeBonus";
    public static final String KNOCKBACK_ENABLED = "knockbackEnabled";
    public static final String KNOCKBACK_CHANCE = "knockbackChance";
    public static final String KNOCKBACK_POWER = "knockbackPower";
    public static final String DEFBREAKER_ENABLED = "defBreakerEnabled";
    public static final String DR_REDUCTION = "drReduction";
    public static final String MAX_STACKS = "maxStacks";
    public static final String CHARGE_ENABLED = "chargeEnabled";
    public static final String CHARGE_MULT = "chargeMult";
    public static final String LOCKON_ENABLED = "lockOnEnabled";
    public static final String LOCKON_BONUS = "lockOnBonus";

    public static final String AC_RELEASE = "RELEASE";
    public static final String AC_UNLOCK = "UNLOCK";
    public static final String AC_LOCK = "LOCK";

    // ===== Instance state =====
    private int lockOnTargetId = -1;
    private int lockOnStacks = 0;
    private boolean targetLocked = false;

    // ===== Config getters/setters for ItemEditor =====
    public Map<String, Object> getConfig() {
        config.clear();
        config.put(PIERCE_ENABLED, isPierceEnabled());
        config.put(PIERCE_RATE, getPierceRate());
        config.put(CRIT_ENABLED, isCritEnabled());
        config.put(CRIT_CHANCE, getCritChance());
        config.put(CRIT_MULT, getCritMultiplier());
        config.put(DODGE_ENABLED, isDodgeEnabled());
        config.put(DODGE_BONUS, getDodgeBonus());
        config.put(KNOCKBACK_ENABLED, isKnockbackEnabled());
        config.put(KNOCKBACK_CHANCE, getKnockbackChance());
        config.put(KNOCKBACK_POWER, getKnockbackPower());
        config.put(DEFBREAKER_ENABLED, isDefBreakerEnabled());
        config.put(DR_REDUCTION, getDrReductionPerStack());
        config.put(MAX_STACKS, getMaxStacks());
        config.put(CHARGE_ENABLED, isChargeEnabled());
        config.put(CHARGE_MULT, getBonusPerStack());
        config.put(LOCKON_ENABLED, isLockOnEnabled());
        config.put(LOCKON_BONUS, getLockOnBonus());
        return config;
    }

    public void setConfig(String key, Object value) {
        if (PIERCE_ENABLED.equals(key)) {
            config.put(PIERCE_ENABLED, value);
        } else if (PIERCE_RATE.equals(key)) {
            config.put(PIERCE_RATE, value);
        } else if (CRIT_ENABLED.equals(key)) {
            config.put(CRIT_ENABLED, value);
        } else if (CRIT_CHANCE.equals(key)) {
            config.put(CRIT_CHANCE, value);
        } else if (CRIT_MULT.equals(key)) {
            config.put(CRIT_MULT, value);
        } else if (DODGE_ENABLED.equals(key)) {
            config.put(DODGE_ENABLED, value);
        } else if (DODGE_BONUS.equals(key)) {
            config.put(DODGE_BONUS, value);
        } else if (KNOCKBACK_ENABLED.equals(key)) {
            config.put(KNOCKBACK_ENABLED, value);
        } else if (KNOCKBACK_CHANCE.equals(key)) {
            config.put(KNOCKBACK_CHANCE, value);
        } else if (KNOCKBACK_POWER.equals(key)) {
            config.put(KNOCKBACK_POWER, value);
        } else if (DEFBREAKER_ENABLED.equals(key)) {
            config.put(DEFBREAKER_ENABLED, value);
        } else if (DR_REDUCTION.equals(key)) {
            config.put(DR_REDUCTION, value);
        } else if (MAX_STACKS.equals(key)) {
            config.put(MAX_STACKS, value);
        } else if (CHARGE_ENABLED.equals(key)) {
            config.put(CHARGE_ENABLED, value);
        } else if (CHARGE_MULT.equals(key)) {
            config.put(CHARGE_MULT, value);
        } else if (LOCKON_ENABLED.equals(key)) {
            config.put(LOCKON_ENABLED, value);
        } else if (LOCKON_BONUS.equals(key)) {
            config.put(LOCKON_BONUS, value);
        }
    }

    // ===== Effect Enabled Checks =====
    private boolean isPierceEnabled() {
        Object val = config.get(PIERCE_ENABLED);
        return val instanceof Boolean ? (Boolean) val : true;
    }

    private boolean isCritEnabled() {
        Object val = config.get(CRIT_ENABLED);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    private boolean isDodgeEnabled() {
        Object val = config.get(DODGE_ENABLED);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    private boolean isKnockbackEnabled() {
        Object val = config.get(KNOCKBACK_ENABLED);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    private boolean isDefBreakerEnabled() {
        Object val = config.get(DEFBREAKER_ENABLED);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    private boolean isChargeEnabled() {
        Object val = config.get(CHARGE_ENABLED);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    private boolean isLockOnEnabled() {
        Object val = config.get(LOCKON_ENABLED);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    // ===== Effect Calculations =====
    private float getPierceRate() {
        Object override = config.get(PIERCE_RATE);
        if (override instanceof Float) return (Float) override;
        float p = 0.15f + 0.03f * Math.max(0, tier - 1) + 0.01f * buffedLvl();
        return Math.min(0.75f, Math.max(0f, p));
    }

    private float getCritChance() {
        Object override = config.get(CRIT_CHANCE);
        if (override instanceof Float) return (Float) override;
        float c = 0.15f + 0.04f * Math.max(0, tier - 1) + 0.015f * buffedLvl();
        return Math.min(0.75f, c);
    }

    private float getCritMultiplier() {
        Object override = config.get(CRIT_MULT);
        if (override instanceof Float) return (Float) override;
        float m = 1.75f + 0.10f * Math.max(0, tier - 1) + 0.05f * buffedLvl();
        return Math.min(3.0f, m);
    }

    private float getDodgeBonus() {
        Object override = config.get(DODGE_BONUS);
        if (override instanceof Float) return (Float) override;
        float bonus = 0.30f + 0.05f * Math.max(0, tier - 1) + 0.02f * buffedLvl();
        return Math.min(1.20f, bonus);
    }

    private float getKnockbackChance() {
        Object override = config.get(KNOCKBACK_CHANCE);
        if (override instanceof Float) return (Float) override;
        float c = 0.20f + 0.05f * Math.max(0, tier - 1) + 0.02f * buffedLvl();
        return Math.min(0.70f, c);
    }

    private int getKnockbackPower() {
        Object override = config.get(KNOCKBACK_POWER);
        if (override instanceof Integer) return (Integer) override;
        return Math.max(1, 1 + tier / 2 + buffedLvl() / 4);
    }

    private int getDrReductionPerStack() {
        Object override = config.get(DR_REDUCTION);
        if (override instanceof Integer) return (Integer) override;
        return Math.max(1, Math.round(1f + Math.max(0, tier - 1) + 0.5f * buffedLvl()));
    }

    private int getMaxStacks() {
        Object override = config.get(MAX_STACKS);
        if (override instanceof Integer) return (Integer) override;
        return Math.min(12, 4 + Math.max(0, tier - 1) + buffedLvl() / 2);
    }

    private float getBonusPerStack() {
        Object override = config.get(CHARGE_MULT);
        if (override instanceof Float) return (Float) override;
        return Math.min(0.25f, 0.10f + 0.01f * Math.max(0, tier - 1) + 0.005f * buffedLvl());
    }

    private float getLockOnBonus() {
        Object override = config.get(LOCKON_BONUS);
        if (override instanceof Float) return (Float) override;
        return 0.10f + 0.05f * Math.max(0, tier - 1);
    }

    // ===== Main proc logic =====
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        int dmg = damage;

        // Effect: Pierce - 穿甲
        if (isPierceEnabled()) {
            int dr = computeDrLikeDamageSystem(attacker, defender);
            int bonus = Math.round(dr * getPierceRate());
            dmg += bonus;
        }

        // Effect: Crit - 暴击
        if (isCritEnabled()) {
            dmg = applyCrit(attacker, defender, dmg);
        }

        // Effect: DefBreaker - 破甲
        if (isDefBreakerEnabled()) {
            dmg = applyDefBreaker(attacker, defender, dmg);
        }

        // Effect: LockOn - 锁定
        if (isLockOnEnabled()) {
            dmg = applyLockOn(attacker, defender, dmg);
        }

        // Effect: Knockback - 冲击
        if (isKnockbackEnabled()) {
            dmg = applyKnockback(attacker, defender, dmg);
        }

        // Effect: Charge - 蓄能 (只在攻击时累积)
        if (isChargeEnabled() && attacker instanceof Hero) {
            Buff.affect((Hero) attacker, ChargeStacks.class).addStack();
        }

        return super.proc(attacker, defender, dmg);
    }

    private int computeDrLikeDamageSystem(Char attacker, Char defender) {
        if (defender == null) return 0;
        int dr = Math.round(defender.drRoll() * com.zootdungeon.actors.buffs.AscensionChallenge.statModifier(defender));
        if (!defender.buffs(DefenseDown.class).isEmpty()) {
            dr /= 2;
        }
        return Math.max(dr, 0);
    }

    private int applyCrit(Char attacker, Char defender, int dmg) {
        if (defender != null && defender.isAlive() && Random.Float() < getCritChance()) {
            dmg = Math.round(dmg * getCritMultiplier());
            if (defender.sprite != null) {
                defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "crit_status"));
                defender.sprite.flash();
            }
            Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.95f);
        }
        return dmg;
    }

    private int applyDefBreaker(Char attacker, Char defender, int dmg) {
        if (defender == null || !defender.isAlive()) return dmg;
        ArmorShatter existing = defender.buff(ArmorShatter.class);
        if (existing != null && existing.totalReduction() > 0) {
            dmg = dmg + existing.totalReduction();
        }
        ArmorShatter shatter = Buff.affect(defender, ArmorShatter.class);
        shatter.addStack(getMaxStacks(), getDrReductionPerStack(), getDebuffDuration());
        return dmg;
    }

    private float getDebuffDuration() {
        return 6f + tier + buffedLvl() / 3f;
    }

    private int applyLockOn(Char attacker, Char defender, int dmg) {
        if (defender == null) {
            lockOnTargetId = -1;
            lockOnStacks = 0;
            return dmg;
        }
        int defId = defender.id();
        if (defId != lockOnTargetId) {
            lockOnTargetId = defId;
            lockOnStacks = 0;
        }
        float mult = 1f + lockOnStacks * getLockOnBonus();
        int boosted = Math.round(dmg * mult);
        lockOnStacks++;
        return boosted;
    }

    private int applyKnockback(Char attacker, Char defender, int dmg) {
        if (defender == null || !defender.isAlive()) return dmg;
        if (defender.properties().contains(Char.Property.IMMOVABLE)) {
            if (Random.Float() < getKnockbackChance()) {
                Buff.prolong(defender, Cripple.class, getDisableDuration());
                Buff.prolong(defender, Vertigo.class, getDisableDuration());
            }
            return dmg;
        }
        if (Random.Float() < getKnockbackChance()) {
            int from = attacker != null ? attacker.pos : defender.pos;
            int direction = defender.pos - from;
            if (direction == 0) {
                direction = com.watabou.utils.PathFinder.NEIGHBOURS8[Random.Int(8)];
            }
            int targetCell = defender.pos + direction;
            Ballistica trajectory = new Ballistica(defender.pos, targetCell, Ballistica.MAGIC_BOLT);
            WandOfBlastWave.throwChar(defender, trajectory, getKnockbackPower(), false, true, this);
            if (defender.isAlive()) {
                float dur = getDisableDuration();
                Buff.prolong(defender, Cripple.class, dur);
                Buff.prolong(defender, Vertigo.class, dur);
            }
            if (Dungeon.level != null) Dungeon.observe();
        }
        return dmg;
    }

    private float getDisableDuration() {
        return 2f + tier / 2f + buffedLvl() / 6f;
    }

    // ===== Dodge Effect Implementation =====
    @Override
    public float dodgeBuffMultiplier() {
        if (!isDodgeEnabled()) return 1f;
        return 1f + getDodgeBonus();
    }

    @Override
    public boolean stillWieldedBy(Hero hero) {
        if (hero == null) return false;
        KindOfWeapon main = hero.belongings.weapon();
        KindOfWeapon off = hero.belongings.secondWep();
        return main == this || off == this;
    }

    @Override
    public void activate(Char ch) {
        super.activate(ch);
        if (ch instanceof Hero && isDodgeEnabled()) {
            DodgeBoostBuff buff = Buff.affect(ch, DodgeBoostBuff.class);
            buff.setSource(this);
        }
    }

    public void onHeroDodged(Hero hero, Char attacker) {
        if (hero == null || attacker == null) return;
        if (!hero.isAlive() || !attacker.isAlive()) return;
        if (!hero.canAttack(attacker)) return;
        if (hero.buff(CounterTracker.class) != null) return;
        if (!isDodgeEnabled()) return;

        CounterTracker tracker = Buff.affect(hero, CounterTracker.class);
        tracker.enemy = attacker;
    }

    // ===== Charge Effect - Hero Dodged Callback =====
    public void onHeroDodged(Hero hero) {
        if (hero == null || !hero.isAlive()) return;
        if (!isChargeEnabled()) return;
        Buff.affect(hero, ChargeStacks.class).addStack();
    }

    // ===== Charge Release Logic =====
    public int releaseCharges(Hero hero, Char enemy) {
        if (hero == null || enemy == null) return 0;
        if (!hero.isAlive() || !enemy.isAlive()) return 0;
        if (!hero.canAttack(enemy)) return 0;

        ChargeStacks stacks = hero.buff(ChargeStacks.class);
        int used = stacks == null ? 0 : stacks.count;
        if (stacks != null) stacks.detach();

        AttackIndicator.target(enemy);
        float mult = 1f + used * getBonusPerStack();
        boolean hit = hero.attack(enemy, mult, 0, Char.INFINITE_ACCURACY);
        if (hit) {
            if (enemy.sprite != null) {
                enemy.sprite.showStatus(CharSprite.NEGATIVE,
                        Messages.get(RhodesGauntlet.class, "release_status", used));
                enemy.sprite.flash();
            }
            Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
        }
        Invisibility.dispel();
        return used;
    }

    // ===== Item Actions =====
    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            if (isChargeEnabled()) {
                actions.add(AC_RELEASE);
            }
            if (isLockOnEnabled()) {
                if (targetLocked) {
                    actions.add(AC_UNLOCK);
                } else {
                    actions.add(AC_LOCK);
                }
            }
        }
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (AC_RELEASE.equals(action)) {
            executeChargeRelease(hero);
        } else if (AC_LOCK.equals(action)) {
            executeLockOn(hero, true);
        } else if (AC_UNLOCK.equals(action)) {
            executeLockOn(hero, false);
        }
    }

    private void executeChargeRelease(Hero hero) {
        if (!isEquipped(hero)) {
            GLog.w(Messages.get(this, "release_need_equip"));
            usesTargeting = false;
            return;
        }
        ChargeStacks stacks = hero.buff(ChargeStacks.class);
        if (stacks == null || stacks.count <= 0) {
            GLog.w(Messages.get(this, "release_no_charge"));
            usesTargeting = false;
            return;
        }
        curUser = hero;
        usesTargeting = true;
        GameScene.selectCell(releaseTarget);
    }

    private void executeLockOn(Hero hero, boolean lock) {
        if (!isEquipped(hero)) {
            GLog.w(Messages.get(this, "lock_need_equip"));
            usesTargeting = false;
            return;
        }
        if (!isLockOnEnabled()) {
            GLog.w(Messages.get(this, "lock_wrong_mode"));
            usesTargeting = false;
            return;
        }
        targetLocked = lock;
        GLog.p(Messages.get(this, lock ? "lock_applied" : "lock_removed"));
        usesTargeting = false;
    }

    private final CellSelector.Listener releaseTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target == null) return;
            final Hero hero = curUser;
            if (hero == null) return;

            final Char enemy = Actor.findChar(target);
            if (enemy == null || enemy == hero || !Dungeon.level.heroFOV[target]) {
                GLog.w(Messages.get(RhodesGauntlet.class, "release_no_target"));
                return;
            }
            if (!hero.canAttack(enemy)) {
                GLog.w(Messages.get(RhodesGauntlet.class, "release_out_of_range"));
                return;
            }

            hero.sprite.attack(enemy.pos, new Callback() {
                @Override
                public void call() {
                    releaseCharges(hero, enemy);
                    hero.spendAndNext(hero.attackDelay());
                }
            });
        }

        @Override
        public String prompt() {
            return Messages.get(RhodesGauntlet.class, "release_prompt");
        }
    };

    // ===== Duelist Ability =====
    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        if (isChargeEnabled()) {
            executeDuelistChargeAbility(hero, target);
        } else {
            super.duelistAbility(hero, target);
        }
    }

    private void executeDuelistChargeAbility(final Hero hero, Integer target) {
        if (target == null) {
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }
        final Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }
        hero.belongings.abilityWeapon = this;
        if (!hero.canAttack(enemy)) {
            GLog.w(Messages.get(this, "ability_target_range"));
            hero.belongings.abilityWeapon = null;
            return;
        }
        hero.belongings.abilityWeapon = null;

        hero.sprite.attack(enemy.pos, new Callback() {
            @Override
            public void call() {
                beforeAbilityUsed(hero, enemy);
                releaseCharges(hero, enemy);
                if (!enemy.isAlive()) onAbilityKill(hero, enemy);
                hero.spendAndNext(hero.attackDelay());
                afterAbilityUsed(hero);
            }
        });
    }

    @Override
    public String abilityInfo() {
        if (isChargeEnabled()) {
            int pctPerStack = Math.round(getBonusPerStack() * 100f);
            if (levelKnown) {
                return Messages.get(this, "ability_desc", pctPerStack);
            } else {
                return Messages.get(this, "typical_ability_desc");
            }
        } else if (isPierceEnabled()) {
            int pct = Math.round(getPierceRate() * 100f);
            return Messages.get(this, "armor_pierce_ability_desc", pct);
        } else if (isCritEnabled()) {
            int pct = Math.round(getCritChance() * 100f);
            int mult = Math.round(getCritMultiplier() * 100f);
            return Messages.get(this, "crit_ability_desc", pct, mult);
        }
        return super.abilityInfo();
    }

    @Override
    public String upgradeAbilityStat(int level) {
        if (isChargeEnabled()) {
            float base = 0.10f + 0.01f * Math.max(0, tier - 1) + 0.005f * level;
            int pct = Math.round(Math.min(0.25f, base) * 100f);
            return Messages.get(this, "upgrade_ability_stat", pct);
        }
        return super.upgradeAbilityStat(level);
    }

    @Override
    public String desc() {
        StringBuilder sb = new StringBuilder();
        sb.append(Messages.get(this, "desc_header"));
        sb.append("\n\n");

        if (isPierceEnabled()) {
            sb.append(Messages.get(this, "desc_pierce", Math.round(getPierceRate() * 100f)));
            sb.append("\n\n");
        }
        if (isCritEnabled()) {
            sb.append(Messages.get(this, "desc_crit",
                    Math.round(getCritChance() * 100f),
                    Math.round(getCritMultiplier() * 100f)));
            sb.append("\n\n");
        }
        if (isDefBreakerEnabled()) {
            sb.append(Messages.get(this, "desc_defbreaker",
                    getMaxStacks(),
                    getDrReductionPerStack(),
                    (int) getDebuffDuration()));
            sb.append("\n\n");
        }
        if (isDodgeEnabled()) {
            sb.append(Messages.get(this, "desc_dodge", Math.round(getDodgeBonus() * 100f)));
            sb.append("\n\n");
        }
        if (isKnockbackEnabled()) {
            sb.append(Messages.get(this, "desc_knockback",
                    Math.round(getKnockbackChance() * 100f),
                    getKnockbackPower(),
                    (int) getDisableDuration()));
            sb.append("\n\n");
        }
        if (isChargeEnabled()) {
            int pct = Math.round(getBonusPerStack() * 100f);
            sb.append(Messages.get(this, "desc_charge", pct));
            sb.append("\n\n");
        }
        if (isLockOnEnabled()) {
            sb.append(Messages.get(this, "desc_lockon", Math.round(getLockOnBonus() * 100f)));
            sb.append("\n\n");
        }

        if (!isPierceEnabled() && !isCritEnabled() && !isDefBreakerEnabled()
                && !isDodgeEnabled() && !isKnockbackEnabled() && !isChargeEnabled() && !isLockOnEnabled()) {
            sb.append(Messages.get(this, "desc_no_effects"));
        }

        return sb.toString().trim();
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("lockon_target_id", lockOnTargetId);
        bundle.put("lockon_stacks", lockOnStacks);
        bundle.put("target_locked", targetLocked);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        lockOnTargetId = bundle.getInt("lockon_target_id");
        lockOnStacks = bundle.getInt("lockon_stacks");
        targetLocked = bundle.getBoolean("target_locked");
    }

    // ===== Inner Buffs =====

    /**
     * 蓄能层数 Buff
     */
    public static class ChargeStacks extends Buff {
        {
            type = buffType.POSITIVE;
            revivePersists = true;
        }

        public int count = 0;

        public void addStack() {
            count++;
            BuffIndicator.refreshHero();
        }

        @Override
        public boolean act() {
            spend(TICK);
            return true;
        }

        @Override
        public int icon() {
            if (Dungeon.hero != null) {
                KindOfWeapon w1 = Dungeon.hero.belongings.weapon();
                KindOfWeapon w2 = Dungeon.hero.belongings.secondWep();
                if (w1 instanceof RhodesGauntlet || w2 instanceof RhodesGauntlet) {
                    return BuffIndicator.COMBO;
                }
            }
            return BuffIndicator.NONE;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.2f, 0.8f, 1.0f);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(count);
        }

        @Override
        public String desc() {
            return Messages.get(ChargeStacks.class, "desc", count);
        }

        private static final String COUNT = "rhodes_charge_count";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(COUNT, count);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            count = bundle.getInt(COUNT);
        }
    }

    /**
     * 破甲 Debuff
     */
    public static class ArmorShatter extends Buff {
        {
            type = buffType.NEGATIVE;
            announced = false;
        }

        public int stacks = 0;
        public int perStackDr = 1;
        private float remaining = 0f;

        public void addStack(int maxStacks, int dr, float duration) {
            if (stacks < maxStacks) stacks++;
            perStackDr = Math.max(perStackDr, dr);
            remaining = Math.max(remaining, duration);
            BuffIndicator.refreshHero();
        }

        public int totalReduction() {
            return stacks * perStackDr;
        }

        @Override
        public boolean act() {
            remaining -= TICK;
            spend(TICK);
            if (remaining <= 0f) {
                detach();
            }
            return true;
        }

        @Override
        public int icon() {
            return BuffIndicator.VULNERABLE;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.9f, 0.55f, 0.1f);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(stacks);
        }

        @Override
        public float iconFadePercent() {
            float full = 10f;
            return Math.max(0, (full - remaining) / full);
        }

        @Override
        public String desc() {
            return Messages.get(ArmorShatter.class, "desc", stacks, totalReduction(), (int) Math.ceil(remaining));
        }

        private static final String STACKS = "armorshatter_stacks";
        private static final String PER = "armorshatter_per";
        private static final String REMAIN = "armorshatter_remain";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(STACKS, stacks);
            bundle.put(PER, perStackDr);
            bundle.put(REMAIN, remaining);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            stacks = bundle.getInt(STACKS);
            perStackDr = bundle.getInt(PER);
            remaining = bundle.getFloat(REMAIN);
        }
    }

    /**
     * 反击追踪 Buff
     */
    public static class CounterTracker extends Buff {
        { actPriority = VFX_PRIO; }

        public Char enemy;

        @Override
        public boolean act() {
            if (enemy == null || !enemy.isAlive() || !target.isAlive() || !(target instanceof Hero)) {
                detach();
                return true;
            }
            final Hero hero = (Hero) target;
            if (!hero.canAttack(enemy)) {
                detach();
                return true;
            }
            final Char en = enemy;
            final CounterTracker self = this;
            hero.sprite.attack(en.pos, new Callback() {
                @Override
                public void call() {
                    hero.attack(en, 1f, 0, Char.INFINITE_ACCURACY);
                    Invisibility.dispel();
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 1.15f);
                    self.next();
                }
            });
            detach();
            return false;
        }
    }
}
