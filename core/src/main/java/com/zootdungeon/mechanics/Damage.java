package com.zootdungeon.mechanics;

import java.util.ArrayList;

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
        int effective = defender.defenseProc(attacker, Math.round(baseDmg));
        if (effective >= 0) {
            effective = Math.max(effective - dr, 0);
            Viscosity.ViscosityTracker viscosity = defender.buff(Viscosity.ViscosityTracker.class);
            if (viscosity != null) {
                effective = viscosity.deferDamage(effective);
                viscosity.detach();
            }
            if (defender.buff(Vulnerable.class) != null) effective *= 1.33f;
            effective = attacker.attackProc(defender, effective);
        }
        if (playHitSound && visibleFight && (effective > 0 || !defender.blockSound(Random.Float(0.96f, 1.05f)))) {
            attacker.hitSound(Random.Float(0.87f, 1.15f));
        }
        if (!defender.isAlive()) {
            return effective;
        }
        defender.damage(effective, attacker);
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
}
