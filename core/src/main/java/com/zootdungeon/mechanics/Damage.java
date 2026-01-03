package com.zootdungeon.mechanics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.zootdungeon.utils.Augment;
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

    public static PhysicalResult physical(Char attacker, Char defender, float dmgMulti, float dmgBonus, float accMulti) {
        /**
         * 1. "PhysicalDamage" - 触发时机：伤害倍率和加成应用前 - 参数：attacker, defender -
         * 返回：DamageAugment（可分组优先级，支持add/mul/pipe等多种修正类型） -
         * 用途：用于外部动态修改伤害（如装备、天赋、被动、环境等）
         *
         * 2. "PhysicalDamage:afterDefense" - 触发时机：防御处理后，最终伤害应用前 - 参数：attacker,
         * defender, effectiveDamage - 返回：DamageAugment -
         * 用途：用于外部对最终伤害做进一步修正（如反伤、特殊护盾、吸血等）
         *
         * 3. "PhysicalDamage:afterDefense:vanished" - 触发时机：防御后伤害为负或无效时 -
         * 参数：attacker, defender, effectiveDamage - 用途：用于处理伤害被完全抵消的特殊反馈
         *
         * 4. "PhysicalDamage:afterDefense:earlyKilled" - 触发时机：防御后目标已死亡 -
         * 参数：attacker, defender, finalEffectiveDamage - 用途：用于处理目标被提前击杀的特殊反馈
         *
         * 5. "PhysicalDamage:afterDamage" - 触发时机：最终伤害应用后 - 参数：attacker,
         * defender, effectiveDamage - 用途：用于外部监听伤害结果（如吸血、反击、连锁反应等）
         *
         * 6. "PhysicalDamage:afterDefense:afterAttackProc" -
         * 触发时机：attackProc后，最终伤害应用前 - 参数：attacker, defender, effectiveDamage -
         * 返回：DamageAugment - 用途：用于进一步修正attackProc后的伤害
         *
         */
        //#region hitOrNot
        if (defender == null) {
            return new PhysicalResult(false, 0, Interrupt.NotFound, false);
        }

        boolean visibleFight = Dungeon.level.heroFOV[attacker.pos]
                || Dungeon.level.heroFOV[defender.pos];
        if (defender.isInvulnerable(attacker.getClass())) {
            if (visibleFight) {
                defender.sprite.showStatus(CharSprite.POSITIVE,
                        Messages.get(defender, "invulnerable"));
                Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, Random.Float(0.96f, 1.05f));
            }
            return new PhysicalResult(false, 0, Interrupt.Invulnerable, visibleFight);
        }

        if (!Char.hit(attacker, defender, accMulti, false)) {
            defender.sprite.showStatus(CharSprite.NEUTRAL, "miss");
            return new PhysicalResult(false, 0, Interrupt.Dodge, visibleFight);
        }
        //#endregion
        //#region basic dr and dmg
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
        
        // 检查 PowerStrike buff（在方法开始处获取，以便后续使用）
        PowerStrike nextAttackBoost = attacker.buff(PowerStrike.class);
        
        float dmg;
        Preparation prep = attacker.buff(Preparation.class);
        if (prep != null) {
            dmg = prep.damageRoll(attacker);
            if (attacker == Dungeon.hero && Dungeon.hero.hasTalent(Talent.BOUNTY_HUNTER)) {
                Buff.affect(Dungeon.hero, Talent.BountyHunterTracker.class, 0.0f);
            }
        } else {
            dmg = attacker.damageRoll();
        }
        //#endregion
        //#region dmgAmplification
        // EventBus removed - empty priority groups
        Map<Short, ArrayList<Augment>> priorityGroups = new HashMap<>();
        dmg *= dmgMulti;
        
        // 应用 PowerStrike buff
        if (nextAttackBoost != null && !nextAttackBoost.used) {
            dmg *= nextAttackBoost.boostMultiplier;
        }
        
        // 处理priority 0的乘法修正
        ArrayList<Augment> priority0Augments = priorityGroups.get((short) 0);
        if (priority0Augments != null) {
            for (Augment augment : priority0Augments) {
                if (augment.type == Augment.Type.mul) {
                    dmg *= augment.value;
                }
            }
        }

        dmg += dmgBonus;
        // 处理priority 0的加法修正
        if (priority0Augments != null) {
            for (Augment augment : priority0Augments) {
                if (augment.type == Augment.Type.add) {
                    dmg += augment.value;
                }
            }
        }

        if (defender.buff(GuidingLight.Illuminated.class) != null) {
            defender.buff(GuidingLight.Illuminated.class).detach();
            if (attacker == Dungeon.hero && Dungeon.hero.hasTalent(Talent.SEARING_LIGHT)) {
                dmg += 2 + 2 * Dungeon.hero.pointsInTalent(Talent.SEARING_LIGHT);
            }
            if (attacker != Dungeon.hero && Dungeon.hero.subClass == HeroSubClass.PRIEST) {
                defender.damage(Dungeon.hero.lvl, GuidingLight.INSTANCE);
            }
        }

        Berserk berserk = attacker.buff(Berserk.class);
        if (berserk != null) {
            dmg = berserk.damageFactor(dmg);
        }

        if (attacker.buff(Fury.class) != null) {
            dmg *= 1.5f;
        }

        if (attacker.buff(PowerOfMany.PowerBuff.class) != null) {
            if (attacker.buff(BeamingRay.BeamingRayBoost.class) != null
                    && attacker.buff(BeamingRay.BeamingRayBoost.class).object == defender.id()) {
                dmg *= 1.3f + 0.05f * Dungeon.hero.pointsInTalent(Talent.BEAMING_RAY);
            } else {
                dmg *= 1.25f;
            }
        }

        for (ChampionEnemy buff : attacker.buffs(ChampionEnemy.class)) {
            dmg *= buff.meleeDamageFactor();
        }

        dmg *= AscensionChallenge.statModifier(attacker);

        Endure.EndureTracker endure = attacker.buff(Endure.EndureTracker.class);
        if (endure != null) {
            dmg = endure.damageFactor(dmg);
        }

        endure = defender.buff(Endure.EndureTracker.class);
        if (endure != null) {
            dmg = endure.adjustDamageTaken(dmg);
        }

        if (defender.buff(ScrollOfChallenge.ChallengeArena.class) != null) {
            dmg *= 0.67f;
        }

        if (Dungeon.hero.alignment == defender.alignment
                && Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
                && (Dungeon.level.distance(defender.pos, Dungeon.hero.pos) <= 2 || defender.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)) {
            dmg *= 0.925f - 0.075f * Dungeon.hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
        }

        if (defender.buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class) != null) {
            dmg *= 0.2f;
        }

        if (attacker.buff(Weakness.class) != null) {
            dmg *= 0.67f;
        }

        if (defender.buff(StoneOfAggression.Aggression.class) != null
                && defender.alignment == attacker.alignment
                && (Char.hasProp(defender, Char.Property.BOSS) || Char.hasProp(defender, Char.Property.MINIBOSS))) {
            dmg *= 0.5f;
            if (defender instanceof YogDzewa) {
                dmg *= 0.5f;
            }
        }

        // 处理priority > 0的修正
        for (Map.Entry<Short, ArrayList<Augment>> entry : priorityGroups.entrySet()) {
            if (entry.getKey() > 0) {
                for (Augment augment : entry.getValue()) {
                    dmg = switch (augment.type) {
                        case add -> dmg + augment.value;
                        case mul -> dmg * augment.value;
                        case pipe -> augment.pipe.pipe(dmg);
                    };
                }
            }
        }
        //#endregion
        //#region effectiveDamageAfterDefenseProc
        int effectiveDamage = defender.defenseProc(attacker, Math.round(dmg));
        int finalEffectiveDamage = effectiveDamage; // EventBus removed
        //#endregion
        if (finalEffectiveDamage >= 0) {
            finalEffectiveDamage = Math.max(finalEffectiveDamage - dr, 0);

            if (defender.buff(Viscosity.ViscosityTracker.class) != null) {
                finalEffectiveDamage = defender.buff(Viscosity.ViscosityTracker.class).deferDamage(finalEffectiveDamage);
                defender.buff(Viscosity.ViscosityTracker.class).detach();
            }

            if (defender.buff(Vulnerable.class) != null) {
                finalEffectiveDamage *= 1.33f;
            }

            finalEffectiveDamage = attacker.attackProc(defender, finalEffectiveDamage);
        } else {
            // EventBus removed
        }

        if (visibleFight) {
            if (finalEffectiveDamage > 0 || !defender.blockSound(Random.Float(0.96f, 1.05f))) {
                attacker.hitSound(Random.Float(0.87f, 1.15f));
            }
        }

        if (!defender.isAlive()) {
            // EventBus removed
            return new PhysicalResult(true, finalEffectiveDamage, Interrupt.Else, visibleFight);
        }
        
        defender.damage(finalEffectiveDamage, attacker);
        // EventBus removed
        
        // 移除 PowerStrike buff（如果已使用）
        if (nextAttackBoost != null && !nextAttackBoost.used) {
            nextAttackBoost.used = true;
            nextAttackBoost.detach();
        }
        
        if (attacker.buff(FireImbue.class) != null) {
            attacker.buff(FireImbue.class).proc(defender);
        }
        // if (attacker.buff(FrostImbue.class) != null) attacker.buff(FrostImbue.class).proc(defender);
        if (!defender.isAlive() && attacker instanceof Hero h) {
            KindOfWeapon weapon = h.belongings.attackingWeapon();
            if (weapon != null && weapon instanceof Weapon) {
                ((Weapon) weapon).onTargetDied(h, defender, finalEffectiveDamage);
            }
        }
        if (defender.isAlive() && defender.alignment != attacker.alignment && prep != null && prep.canKO(defender)) {
            defender.HP = 0;
            if (defender.buff(Brute.BruteRage.class) != null) {
                defender.buff(Brute.BruteRage.class).detach();
            }
            if (!defender.isAlive()) {
                defender.die(attacker);
            } else {
                defender.damage(-1, attacker);
                DeathMark.processFearTheReaper(defender);
            }
            if (defender.sprite != null) {
                defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Preparation.class, "assassinated"));
            }
        }

        Talent.CombinedLethalityAbilityTracker combinedLethality = attacker.buff(Talent.CombinedLethalityAbilityTracker.class);
        if (combinedLethality != null && attacker instanceof Hero && ((Hero) attacker).belongings.attackingWeapon() instanceof MeleeWeapon && combinedLethality.weapon != ((Hero) attacker).belongings.attackingWeapon()) {
            if (defender.isAlive() && defender.alignment != attacker.alignment && !Char.hasProp(defender, Char.Property.BOSS)
                    && !Char.hasProp(defender, Char.Property.MINIBOSS)
                    && (defender.HP / (float) defender.HT) <= 0.4f * ((Hero) attacker).pointsInTalent(Talent.COMBINED_LETHALITY) / 3f) {
                defender.HP = 0;
                if (defender.buff(Brute.BruteRage.class) != null) {
                    defender.buff(Brute.BruteRage.class).detach();
                }
                if (!defender.isAlive()) {
                    defender.die(attacker);
                } else {
                    defender.damage(-1, attacker);
                    DeathMark.processFearTheReaper(defender);
                }
                if (defender.sprite != null) {
                    defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Talent.CombinedLethalityAbilityTracker.class, "executed"));
                }
            }
        }

        return new PhysicalResult(true, finalEffectiveDamage, Interrupt.Else, visibleFight);
    }
}
