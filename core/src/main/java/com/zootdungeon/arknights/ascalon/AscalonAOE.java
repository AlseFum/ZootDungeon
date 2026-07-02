package com.zootdungeon.arknights.ascalon;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.SmokeScreen;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.base.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.utils.Select;
import com.watabou.utils.PathFinder;

public class AscalonAOE extends MeleeWeapon {
    
    static {
        // setArea(label, gx, gy) 按网格格索引；要 64×64 一帧需先 grid(64,64) 再 setArea(label,0,0)。
        TextureRegistry.texture("sheet.cola.ascalon_weapon", "cola/ascalon_weapon.png")
                .setArea("ascalon_aoe", 0, 0, 64, 64);
    }
    
    {
        image = TextureRegistry.idByLabel("ascalon_aoe");
        tier = 1;
        bones = false;
        RCH = 3;
        usesTargeting = false;
    }
    
    private static final int AOE_RADIUS = 3; // 范围半径
    private static final float AOE_DAMAGE_MULT = 0.3f; // 范围伤害倍率（30%）
    private static final float EVASION_BONUS = 1.5f; // 闪避加成（50%）
    
    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }
    
    @Override
    public boolean doEquip(Hero hero) {
        if (super.doEquip(hero)) {
            // 装备时添加闪避加成 buff
            Buff.affect(hero, EvasionBonus.class);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (super.doUnequip(hero, collect, single)) {
            // 卸下时移除闪避加成 buff
            Buff.detach(hero, EvasionBonus.class);
            return true;
        }
        return false;
    }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);

        // Only apply AscalonWound for the primary target of a regular attack.
        // AOE attacks are handled by duelistAbility which sets applyingAOEAbility=true,
        // applies AscalonWound itself in the callback, and resets the flag.
        if (attacker instanceof Hero && defender.isAlive() && !applyingAOEAbility) {
            Hero hero = (Hero) attacker;
            boolean inFog = Blob.volumeAt(hero.pos, SmokeScreen.class) > 0
                    || Blob.volumeAt(defender.pos, SmokeScreen.class) > 0;
            AscalonWound.applyFrom(hero, defender, damage, inFog);
        }

        return damage;
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        if (target == null) return;

        // If target is the hero's own position, use all adjacent enemies
        // Otherwise, build distance map from target cell
        if (target != hero.pos) {
            PathFinder.buildDistanceMap(target, Dungeon.level.passable, AOE_RADIUS);
        }

        java.util.Set<Char> targets = Select.chars().all()
                .except(Select.chars().of(hero))
                .except(Select.chars().ally())
                .that(ch -> {
                    // Must be alive and in hero's FOV
                    if (!ch.isAlive() || !Dungeon.level.heroFOV[ch.pos]) return false;
                    // If targeting own position: adjacent to hero
                    if (target == hero.pos) {
                        return Dungeon.level.adjacent(hero.pos, ch.pos);
                    }
                    // Otherwise: within AOE radius of target cell
                    return ch.pos >= 0 && ch.pos < PathFinder.distance.length
                            && PathFinder.distance[ch.pos] <= AOE_RADIUS
                            && PathFinder.distance[ch.pos] > 0;
                })
                .query();

        if (targets.isEmpty()) {
            GLog.w(Messages.get(this, "ability_no_target"));
            hero.belongings.abilityWeapon = null;
            return;
        }

        beforeAbilityUsed(hero, null);
        applyingAOEAbility = true;

        final java.util.Set<Char> finalTargets = targets;
        final int[] hitCount = {0};
        for (Char enemy : finalTargets) {
            hero.sprite.attack(enemy.pos, new com.watabou.utils.Callback() {
                @Override
                public void call() {
                    hero.attack(enemy, 1f, 0f, Char.INFINITE_ACCURACY);

                    // Apply AscalonWound to AOE targets using AOE damage.
                    int aoeDamage = Math.max(1, Math.round(hero.damageRoll() * AOE_DAMAGE_MULT));
                    boolean inFog = Blob.volumeAt(hero.pos, SmokeScreen.class) > 0
                            || Blob.volumeAt(enemy.pos, SmokeScreen.class) > 0;
                    AscalonWound.applyFrom(hero, enemy, aoeDamage, inFog);

                    hitCount[0]++;
                    if (hitCount[0] == finalTargets.size()) {
                        applyingAOEAbility = false;
                        Invisibility.dispel();
                        hero.spendAndNext(hero.attackDelay());
                        afterAbilityUsed(hero);
                    }
                }
            });
        }
    }

    @Override
    public String abilityInfo() {
        int avgDmg = (min(0) + max(0)) / 2;
        int aoeDmg = Math.max(1, Math.round(avgDmg * AOE_DAMAGE_MULT));
        return Messages.get(this, "ability_desc", min(0), max(0), aoeDmg, AOE_RADIUS);
    }
    
    // 闪避加成 buff
    public static class EvasionBonus extends FlavourBuff {
        
        {
            type = buffType.POSITIVE;
            announced = false; // 不显示在 buff 栏
        }
        
        @Override
        public int icon() {
            return BuffIndicator.DUEL_EVASIVE;
        }
    }
    
    // 静态方法：获取闪避倍率
    public static float evasionMultiplier(Char target) {
        if (target.buff(EvasionBonus.class) != null) {
            return EVASION_BONUS;
        }
        return 1f;
    }

    // 防止在 AOE 技能攻击中重复触发 proc 内的 AscalonWound 逻辑
    private static boolean applyingAOEAbility = false;
    public static boolean isApplyingAOEAbility() {
        return applyingAOEAbility;
    }
}
