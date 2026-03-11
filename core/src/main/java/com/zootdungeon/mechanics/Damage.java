package com.zootdungeon.mechanics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.*;
import com.zootdungeon.actors.buffs.PowerStrike;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.zootdungeon.actors.hero.abilities.rogue.DeathMark;
import com.zootdungeon.actors.hero.abilities.warrior.Endure;
import com.zootdungeon.actors.hero.spells.AuraOfProtection;
import com.zootdungeon.actors.hero.spells.BeamingRay;
import com.zootdungeon.actors.hero.spells.GuidingLight;
import com.zootdungeon.actors.hero.spells.LifeLinkSpell;
import com.zootdungeon.actors.mobs.Brute;
import com.zootdungeon.actors.mobs.YogDzewa;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.armor.glyphs.Viscosity;
import com.zootdungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.zootdungeon.items.stones.StoneOfAggression;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;


public class Damage {

    private enum DamageType {
        PHYSICAL,
        MAGIC,
        FIRE,
        FROST,
        SHOCK,
        POISON,
        CORROSION,
        HUNGER,
        TRUE
    }

    private enum DamageForm {
        DIRECT,
        RETALIATION,
        ENVIRONMENT,
        ADDITIONAL,
        DOT
    }

    private enum DamageFlag {
        IGNORE_ARMOR,
        CANNOT_CRIT,
        NO_INTERRUPT,
        NO_FEEDBACK
    }

    // Type shortcuts for external callers: Damage.PHYSICAL style.
    public static final Object PHYSICAL = DamageType.PHYSICAL;
    public static final Object MAGIC = DamageType.MAGIC;
    public static final Object FIRE = DamageType.FIRE;
    public static final Object FROST = DamageType.FROST;
    public static final Object SHOCK = DamageType.SHOCK;
    public static final Object POISON = DamageType.POISON;
    public static final Object CORROSION = DamageType.CORROSION;
    public static final Object HUNGER = DamageType.HUNGER;
    public static final Object TRUE = DamageType.TRUE;

    public static final Object DIRECT = DamageForm.DIRECT;
    public static final Object RETALIATION = DamageForm.RETALIATION;
    public static final Object ENVIRONMENT = DamageForm.ENVIRONMENT;
    public static final Object ADDITIONAL = DamageForm.ADDITIONAL;
    public static final Object DOT = DamageForm.DOT;

    public static final Object IGNORE_ARMOR = DamageFlag.IGNORE_ARMOR;
    public static final Object CANNOT_CRIT = DamageFlag.CANNOT_CRIT;
    public static final Object NO_INTERRUPT = DamageFlag.NO_INTERRUPT;
    public static final Object NO_FEEDBACK = DamageFlag.NO_FEEDBACK;

    public static enum Interrupt {
        Invulnerable,
        Parry,
        Dodge,
        NotFound,
        Else;
    }

    public static class PhysicalResult {

        public boolean result;      // 攻击是否成功
        public int damage;          // 造成的伤害
        public Interrupt interrupt; // 攻击是否被中断
        public boolean visible;     // 战斗是否可见

        public PhysicalResult(boolean result, int damage, Interrupt interrupt, boolean visible) {
            this.result = result;
            this.damage = damage;
            this.interrupt = interrupt;
            this.visible = visible;
        }
    }

    public static class DamageResult {
        public int requestedAmount;
        public int appliedAmount;
        public int effectiveDamage;
        public int shieldAbsorbed;
        public boolean targetDied;
        public final List<BiConsumer<DamageContext, DamageResult>> appliedEffects = new ArrayList<>();
        public final List<Object> feedback = new ArrayList<>();
    }

    public static class DamageContext {
        public final Char from;
        public final Char to;
        public final Object damageType;
        public final Object damageForm;
        public final float amount;
        public final Object way;
        public final Set<Object> flags;

        public float baseAmount;
        public float mitigatedAmount;
        public int appliedAmount;
        public int effectiveDamage;
        public int shieldAbsorbed;

        public DamageContext(Char from, Char to, Object damageType, Object damageForm, float amount, Object way, Set<Object> flags) {
            this.from = from;
            this.to = to;
            this.damageType = damageType;
            this.damageForm = damageForm;
            this.amount = amount;
            this.way = way;
            this.flags = flags == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(flags));
        }

        public static DamageContext of(Char from, Char to, Object damageType, Object damageForm, float amount, Object way, Set<Object> flags) {
            return new DamageContext(from, to, damageType, damageForm, amount, way, flags);
        }

        public static DamageContext of(Char from, Char to, Object damageType, Object damageForm, float amount, Object way) {
            return new DamageContext(from, to, damageType, damageForm, amount, way, Collections.emptySet());
        }

        public static DamageContext direct(Char from, Char to, Object damageType, float amount, Object way) {
            return of(from, to, damageType, DIRECT, amount, way);
        }

        public static DamageContext directPhysical(Char from, Char to, float amount, Object way) {
            return direct(from, to, PHYSICAL, amount, way);
        }

        public boolean hasFlag(Object flag) {
            return flags.contains(flag);
        }

        public Object sourceForApply() {
            if (way != null) return way;
            if (from != null) return from;
            return this;
        }
    }

    public static DamageResult process(DamageContext context) {
        return DamagePipeline.process(context);
    }

    public static DamageResult dot(Char to, Object type, float amount, Object way) {
        return process(DamageContext.of(null, to, type, DOT, amount, way));
    }

    public static DamageResult environment(Char to, Object type, float amount, Object way) {
        return process(DamageContext.of(null, to, type, ENVIRONMENT, amount, way));
    }

    public static DamageResult retaliation(Char from, Char to, Object type, float amount, Object way) {
        return process(DamageContext.of(from, to, type, RETALIATION, amount, way));
    }

    public static DamageResult additional(Char from, Char to, Object type, float amount, Object way) {
        return process(DamageContext.of(from, to, type, ADDITIONAL, amount, way));
    }

    /**
     * 单次物理伤害（命中一次，结算一次伤害）。
     * 多次伤害请使用 {@link #physical(Char, Char, float, float, float, int)}。
     */
    public static PhysicalResult physical(Char attacker, Char defender, float dmgMulti, float dmgBonus, float accMulti) {
        return physical(attacker, defender, dmgMulti, dmgBonus, accMulti, 1);
    }

    /**
     * 物理伤害，支持多次命中。
     * @param hitCount 命中次数（≥1）。一次命中判定，然后按同一基础伤害结算 hitCount 次（每次独立经过防御、DR、proc）。
     */
    public static PhysicalResult physical(Char attacker, Char defender, float dmgMulti, float dmgBonus, float accMulti, int hitCount) {
        if (hitCount < 1) hitCount = 1;

        if (defender == null) {
            return new PhysicalResult(false, 0, Interrupt.NotFound, false);
        }
        boolean visibleFight = Dungeon.level.heroFOV[attacker.pos] || Dungeon.level.heroFOV[defender.pos];
        if (defender.isInvulnerable(attacker.getClass())) {
            if (visibleFight) {
                defender.sprite.showStatus(CharSprite.POSITIVE, Messages.get(defender, "invulnerable"));
                Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, Random.Float(0.96f, 1.05f));
            }
            return new PhysicalResult(false, 0, Interrupt.Invulnerable, visibleFight);
        }
        if (!Char.hit(attacker, defender, accMulti, false)) {
            defender.sprite.showStatus(CharSprite.NEUTRAL, "miss");
            return new PhysicalResult(false, 0, Interrupt.Dodge, visibleFight);
        }

        int dr = computeDr(attacker, defender);
        BaseDamageResult base = computeBaseDamage(attacker, defender, dmgMulti, dmgBonus);
        DamageContext physicalCtx = DamageContext.directPhysical(attacker, defender, base.baseDmg, attacker);
        base.baseDmg = DamagePipeline.applyComputeAmplifiers(physicalCtx, base.baseDmg);
        if (!defender.isAlive()) {
            return new PhysicalResult(true, 0, Interrupt.Else, visibleFight);
        }

        int totalDamage = 0;
        for (int i = 0; i < hitCount; i++) {
            int dealt = applyOneHit(attacker, defender, base.baseDmg, dr, visibleFight, i == 0);
            totalDamage += dealt;
            if (!defender.isAlive()) break;
        }

        postDamageEffects(attacker, defender, totalDamage, base.prep, base.nextAttackBoost);
        return new PhysicalResult(true, totalDamage, Interrupt.Else, visibleFight);
    }

    private static int computeDr(Char attacker, Char defender) {
        int dr = Math.round(defender.drRoll() * AscensionChallenge.statModifier(defender));
        if (attacker instanceof Hero h) {
            if (h.belongings.attackingWeapon() instanceof MissileWeapon
                    && h.subClass == HeroSubClass.SNIPER
                    && !Dungeon.level.adjacent(h.pos, defender.pos)) {
                dr = 0;
            }
            if (h.buff(MonkEnergy.MonkAbility.UnarmedAbilityTracker.class) != null) {
                dr = 0;
            }
        }
        return dr;
    }

    private static class BaseDamageResult {
        float baseDmg;
        Preparation prep;
        PowerStrike nextAttackBoost;
    }

    private static BaseDamageResult computeBaseDamage(Char attacker, Char defender, float dmgMulti, float dmgBonus) {
        BaseDamageResult r = new BaseDamageResult();
        r.nextAttackBoost = attacker.buff(PowerStrike.class);
        r.prep = attacker.buff(Preparation.class);
        float dmg = r.prep != null ? r.prep.damageRoll(attacker) : attacker.damageRoll();
        if (r.prep != null && attacker == Dungeon.hero && Dungeon.hero.hasTalent(Talent.BOUNTY_HUNTER)) {
            Buff.affect(Dungeon.hero, Talent.BountyHunterTracker.class, 0.0f);
        }
        dmg *= dmgMulti;
        if (r.nextAttackBoost != null && !r.nextAttackBoost.used) {
            dmg *= r.nextAttackBoost.boostMultiplier;
        }
        dmg += dmgBonus;

        GuidingLight.Illuminated illuminated = defender.buff(GuidingLight.Illuminated.class);
        if (illuminated != null) {
            illuminated.detach();
            if (attacker == Dungeon.hero && Dungeon.hero.hasTalent(Talent.SEARING_LIGHT)) {
                dmg += 2 + 2 * Dungeon.hero.pointsInTalent(Talent.SEARING_LIGHT);
            }
            if (attacker != Dungeon.hero && Dungeon.hero.subClass == HeroSubClass.PRIEST) {
                defender.damage(Dungeon.hero.lvl, GuidingLight.INSTANCE);
            }
        }
        Berserk berserk = attacker.buff(Berserk.class);
        if (berserk != null) dmg = berserk.damageFactor(dmg);
        if (attacker.buff(Fury.class) != null) dmg *= 1.5f;
        PowerOfMany.PowerBuff powerOfMany = attacker.buff(PowerOfMany.PowerBuff.class);
        if (powerOfMany != null) {
            BeamingRay.BeamingRayBoost beamingRay = attacker.buff(BeamingRay.BeamingRayBoost.class);
            dmg *= (beamingRay != null && beamingRay.object == defender.id())
                    ? (1.3f + 0.05f * Dungeon.hero.pointsInTalent(Talent.BEAMING_RAY))
                    : 1.25f;
        }
        for (ChampionEnemy buff : attacker.buffs(ChampionEnemy.class)) {
            dmg *= buff.meleeDamageFactor();
        }
        dmg *= AscensionChallenge.statModifier(attacker);
        Endure.EndureTracker endure = attacker.buff(Endure.EndureTracker.class);
        if (endure != null) dmg = endure.damageFactor(dmg);
        endure = defender.buff(Endure.EndureTracker.class);
        if (endure != null) dmg = endure.adjustDamageTaken(dmg);
        if (defender.buff(ScrollOfChallenge.ChallengeArena.class) != null) dmg *= 0.67f;
        if (Dungeon.hero.alignment == defender.alignment
                && Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
                && (Dungeon.level.distance(defender.pos, Dungeon.hero.pos) <= 2 || defender.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)) {
            dmg *= 0.925f - 0.075f * Dungeon.hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
        }
        if (defender.buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class) != null) dmg *= 0.2f;
        if (attacker.buff(Weakness.class) != null) dmg *= 0.67f;
        if (defender.buff(StoneOfAggression.Aggression.class) != null
                && defender.alignment == attacker.alignment
                && (Char.hasProp(defender, Char.Property.BOSS) || Char.hasProp(defender, Char.Property.MINIBOSS))) {
            dmg *= 0.5f;
            if (defender instanceof YogDzewa) dmg *= 0.5f;
        }
        r.baseDmg = dmg;
        return r;
    }

    /** 单次伤害应用：防御 proc、DR、黏性、易伤、攻击 proc、音效、扣血。返回本段造成的伤害值。 */
    private static int applyOneHit(Char attacker, Char defender, float baseDmg, int dr, boolean visibleFight, boolean playHitSound) {
        DamageContext hitCtx = DamageContext.directPhysical(attacker, defender, baseDmg, attacker);
        int effective = defender.defenseProc(attacker, Math.round(baseDmg));
        if (effective >= 0) {
            if (!hitCtx.hasFlag(IGNORE_ARMOR)) {
                effective = Math.max(effective - dr, 0);
            }
            Viscosity.ViscosityTracker viscosity = defender.buff(Viscosity.ViscosityTracker.class);
            if (viscosity != null) {
                effective = viscosity.deferDamage(effective);
                viscosity.detach();
            }
            if (defender.buff(Vulnerable.class) != null) effective *= 1.33f;
            effective = attacker.attackProc(defender, effective);
            effective = Math.max(Math.round(DamagePipeline.applyMitigationAmplifiers(hitCtx, effective)), 0);
        }
        if (playHitSound && visibleFight && (effective > 0 || !defender.blockSound(Random.Float(0.96f, 1.05f)))) {
            attacker.hitSound(Random.Float(0.87f, 1.15f));
        }
        if (!defender.isAlive()) {
            return effective;
        }
        DamagePipeline.applyDamageAndEffects(hitCtx, effective, true);
        return effective;
    }

    private static void postDamageEffects(Char attacker, Char defender, int totalDamage, Preparation prep, PowerStrike nextAttackBoost) {
        if (nextAttackBoost != null && !nextAttackBoost.used) {
            nextAttackBoost.used = true;
            nextAttackBoost.detach();
        }
        FireImbue fireImbue = attacker.buff(FireImbue.class);
        if (fireImbue != null) fireImbue.proc(defender);
        if (!defender.isAlive() && attacker instanceof Hero h) {
            KindOfWeapon weapon = h.belongings.attackingWeapon();
            if (weapon != null && weapon instanceof Weapon) {
                ((Weapon) weapon).onTargetDied(h, defender, totalDamage);
            }
        }
        if (defender.isAlive() && defender.alignment != attacker.alignment && prep != null && prep.canKO(defender)) {
            applyExecuteKill(defender, attacker, Preparation.class, "assassinated");
        }
        Talent.CombinedLethalityAbilityTracker combinedLethality = attacker.buff(Talent.CombinedLethalityAbilityTracker.class);
        if (combinedLethality != null && attacker instanceof Hero hero
                && hero.belongings.attackingWeapon() instanceof MeleeWeapon
                && combinedLethality.weapon != hero.belongings.attackingWeapon()) {
            if (defender.isAlive() && defender.alignment != attacker.alignment
                    && !Char.hasProp(defender, Char.Property.BOSS) && !Char.hasProp(defender, Char.Property.MINIBOSS)
                    && (defender.HP / (float) defender.HT) <= 0.4f * hero.pointsInTalent(Talent.COMBINED_LETHALITY) / 3f) {
                applyExecuteKill(defender, attacker, Talent.CombinedLethalityAbilityTracker.class, "executed");
            }
        }
    }

    /** 处决类效果：HP 置 0、移除暴怒、结算死亡或伤害、显示状态文字 */
    private static void applyExecuteKill(Char defender, Char attacker, Class<?> messageClass, String statusKey) {
        defender.HP = 0;
        Brute.BruteRage rage = defender.buff(Brute.BruteRage.class);
        if (rage != null) rage.detach();
        if (!defender.isAlive()) {
            defender.die(attacker);
        } else {
            defender.damage(-1, attacker);
            DeathMark.processFearTheReaper(defender);
        }
        if (defender.sprite != null) {
            defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(messageClass, statusKey));
        }
    }

    private static final class DamagePipeline {
        private DamagePipeline() {}

        private static float applyComputeAmplifiers(DamageContext context, float amount) {
            context.baseAmount = amount;
            ArrayList<UnaryOperator<Float>> amps = com.zootdungeon.event.DamageEvents.DamageComputeRequested.of(context).collect();
            float out = amount;
            for (UnaryOperator<Float> amp : amps) {
                if (amp != null) out = amp.apply(out);
            }
            context.baseAmount = out;
            return out;
        }

        private static float applyMitigationAmplifiers(DamageContext context, float amount) {
            context.mitigatedAmount = amount;
            ArrayList<UnaryOperator<Float>> amps = com.zootdungeon.event.DamageEvents.DamageMitigationRequested.of(context).collect();
            float out = amount;
            for (UnaryOperator<Float> amp : amps) {
                if (amp != null) out = amp.apply(out);
            }
            context.mitigatedAmount = out;
            return out;
        }

        private static DamageResult applyDamageAndEffects(DamageContext context, int amount, boolean collectFeedback) {
            DamageResult result = new DamageResult();
            result.requestedAmount = Math.max(0, amount);

            ArrayList<BiConsumer<DamageContext, DamageResult>> effects =
                    com.zootdungeon.event.DamageEvents.DamageApplyRequested.of(context).collect();
            int preHP = context.to.HP + context.to.shielding();
            context.to.damage(result.requestedAmount, context.sourceForApply());
            int postHP = context.to.HP + context.to.shielding();

            result.appliedAmount = result.requestedAmount;
            result.effectiveDamage = Math.max(preHP - postHP, 0);
            result.shieldAbsorbed = Math.max(result.requestedAmount - result.effectiveDamage, 0);
            result.targetDied = !context.to.isAlive();

            context.appliedAmount = result.appliedAmount;
            context.effectiveDamage = result.effectiveDamage;
            context.shieldAbsorbed = result.shieldAbsorbed;

            for (BiConsumer<DamageContext, DamageResult> effect : effects) {
                if (effect != null) {
                    effect.accept(context, result);
                    result.appliedEffects.add(effect);
                }
            }

            com.zootdungeon.event.DamageEvents.DamageApplied.of(context, result).dispatch();
            if (collectFeedback && !context.hasFlag(NO_FEEDBACK)) {
                ArrayList<Object> feedbacks = com.zootdungeon.event.DamageEvents.DamageFeedbackRequested.of(context, result).collect();
                result.feedback.addAll(feedbacks);
            }
            return result;
        }

        private static DamageResult process(DamageContext context) {
            float base = applyComputeAmplifiers(context, context.amount);
            float mitigated = applyMitigationAmplifiers(context, base);
            int applied = Math.max(Math.round(mitigated), 0);
            return applyDamageAndEffects(context, applied, true);
        }
    }
}
