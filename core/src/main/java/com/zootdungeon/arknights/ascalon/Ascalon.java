/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zootdungeon.arknights.ascalon;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.SmokeScreen;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class Ascalon extends AmbushWeapon {
    
    static {
        SpriteRegistry.registerItemTexture("cola/ascalon_weapon.png",64)
                .label("ascalon_execution");
    }
    
    {
        image = SpriteRegistry.itemByName("ascalon_execution");
        tier = 0;
        bones = false;
        ambushRate=1.2f;
    }
    @Override
    public String name(){
        return "“复仇者”";
    }

    @Override
    public String desc(){
        return "在阿斯卡纶第一次为军事委员会完成任务后，由特雷西斯亲手赠送，特蕾西娅为她安装的第一把武器。";
    }
    
    @Override
    public int damageRoll(Char owner) {
        if (owner instanceof Hero) {
            Hero hero = (Hero)owner;
            Char enemy = hero.enemy();
            if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
                // 检查是否在暗夜迷雾中
                float currentAmbushRate = ambushRate;
                if (Blob.volumeAt(hero.pos, SmokeScreen.class) > 0 
                        || (enemy != null && Blob.volumeAt(enemy.pos, SmokeScreen.class) > 0)) {
                    currentAmbushRate = 2.4f;
                }
                
                // surprise hit: 偏向高端的基础伤害，再加上 exSTR 骰
                int lvl = buffedLvl();
                int mn = min(lvl);
                int mx = max(lvl);
                int diff = mx - mn;

                // 基础部分：将 [mn, mx] 区间压缩到 [mn+currentAmbushRate*diff, mx]
                int biasedMin = mn + Math.round(diff * currentAmbushRate);
                int biasedMax = mx + Math.round(diff * currentAmbushRate);
                if (biasedMin > biasedMax) biasedMin = biasedMax;

                // 用 Dice 表达该区间并掷出一次
                int span = biasedMax - biasedMin;
                com.zootdungeon.utils.Dice base = span > 0
                        ? com.zootdungeon.utils.Dice.of(new com.zootdungeon.utils.Dice.Die(1, span + 1), biasedMin - 1)
                        : com.zootdungeon.utils.Dice.of(biasedMin);

                int damage = augment.damageFactor(base.rollTotalGame());

                // 追加 exSTR 骰
                damage += exSTRDice(hero).rollTotalGame();
                return damage;
            }
        }
        return super.damageRoll(owner);
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);
        
        // 如果攻击者是Hero，应用AscalonWound buff
        if (attacker instanceof Hero && defender.isAlive()) {
            Hero hero = (Hero) attacker;
            // 在攻击时判断是否在迷雾中
            boolean inFog = Blob.volumeAt(hero.pos, SmokeScreen.class) > 0 
                    || Blob.volumeAt(defender.pos, SmokeScreen.class) > 0;
            
            Wound existingWound = defender.buff(Wound.class);
            Wound wound;
            if (existingWound != null) {
                wound = existingWound;
            } else {
                wound = new Wound();
                wound.attachTo(defender);
            }
            // 如果buff刚被创建或剩余时间小于DURATION，则延长到DURATION
            float duration = Wound.DURATION * defender.resist(Wound.class);
            wound.extend(duration);
            // 设置hero引用、攻击时的攻击力和迷雾状态
            float attackDamage = hero.damageRoll();
            wound.set(hero, attackDamage, inFog);
        }
        
        return damage;
    }
    
    public static class Wound extends Buff {

        public static final float DURATION = 25f;
        
        private Hero hero;
        private float heroAttackDamage; // 存储攻击时的hero攻击力
        private int lastHP; // 记录上次的HP，用于检测伤害
        private float left = DURATION; // 剩余持续时间
        private boolean inFogWhenAttacked; // 攻击时是否在迷雾中
        
        {
            type = buffType.NEGATIVE;
            announced = true;
        }
        
        private static final String HERO = "hero";
        private static final String HERO_ATTACK_DAMAGE = "heroAttackDamage";
        private static final String LAST_HP = "lastHP";
        private static final String LEFT = "left";
        private static final String IN_FOG_WHEN_ATTACKED = "inFogWhenAttacked";
        
        @Override
        public boolean attachTo(Char target) {
            if (super.attachTo(target)) {
                lastHP = target.HP;
                return true;
            }
            return false;
        }
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(HERO_ATTACK_DAMAGE, heroAttackDamage);
            bundle.put(LAST_HP, lastHP);
            bundle.put(LEFT, left);
            bundle.put(IN_FOG_WHEN_ATTACKED, inFogWhenAttacked);
            // Hero不能序列化，所以需要重新获取
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            heroAttackDamage = bundle.getFloat(HERO_ATTACK_DAMAGE);
            lastHP = bundle.getInt(LAST_HP);
            if (bundle.contains(LEFT)) {
                left = bundle.getFloat(LEFT);
            } else {
                left = DURATION;
            }
            inFogWhenAttacked = bundle.getBoolean(IN_FOG_WHEN_ATTACKED);
            hero = Dungeon.hero; // 恢复时重新获取hero引用
        }
        
        public void set(Hero hero, float heroAttackDamage, boolean inFog) {
            this.hero = hero;
            this.heroAttackDamage = heroAttackDamage;
            this.inFogWhenAttacked = inFog;
        }
        
        public void extend(float duration) {
            // 延长持续时间
            left = Math.max(left, duration);
        }
        
        @Override
        public int icon() {
            return BuffIndicator.BLEEDING;
        }
        
        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.8f, 0.2f, 0.2f);
        }
        
        @Override
        public float iconFadePercent() {
            return Math.max(0, left / DURATION);
        }
        
        @Override
        public String iconTextDisplay() {
            return Integer.toString((int)left);
        }
        
        // 降低移动速度18%
        public float speedFactor() {
            return 0.82f;
        }
        
        @Override
        public boolean act() {
            if (target.isAlive() && left > 0) {
                // 每回合受到hero当前攻击力的伤害（攻击时在迷雾中则20%，否则10%）
                if (hero != null && hero.isAlive()) {
                    // 获取hero当前攻击力
                    float currentAttackDamage = hero.damageRoll();
                    float damagePercent = inFogWhenAttacked ? 0.2f : 0.1f;
                    int damage = Math.max(1, Math.round(currentAttackDamage * damagePercent));
                    
                    // 记录伤害前的HP
                    int hpBeforeDamage = target.HP;
                    target.damage(damage, this);
                    
                    // 如果攻击时在迷雾中，每次受到伤害都治疗hero
                    if (inFogWhenAttacked && target.HP < hpBeforeDamage && hero.isAlive()) {
                        // 计算治疗量：敌人损失HP的10%
                        int hpLost = hpBeforeDamage - target.HP;
                        if (hpLost > 0) {
                            int healAmount = Math.round(hpLost * 0.1f);
                            if (healAmount > 0) {
                                hero.HP = Math.min(hero.HP + healAmount, hero.HT);
                                if (hero.sprite != null && hero.HP < hero.HT) {
                                    hero.sprite.emitter().burst(com.zootdungeon.effects.Speck.factory(com.zootdungeon.effects.Speck.HEALING), 1);
                                }
                            }
                        }
                    }
                    
                    // 检查是否有其他伤害（HP变化不是由我们造成的）
                    if (target.HP < lastHP && target.HP >= 0) {
                        int totalDamage = lastHP - target.HP;
                        // 如果总伤害大于我们造成的伤害，说明有其他伤害源
                        if (inFogWhenAttacked && totalDamage > damage && hero.isAlive()) {
                            // 治疗hero：额外伤害的10%
                            int extraDamage = totalDamage - damage;
                            int healAmount = Math.round(extraDamage * 0.1f);
                            if (healAmount > 0) {
                                hero.HP = Math.min(hero.HP + healAmount, hero.HT);
                                if (hero.sprite != null && hero.HP < hero.HT) {
                                    hero.sprite.emitter().burst(com.zootdungeon.effects.Speck.factory(com.zootdungeon.effects.Speck.HEALING), 1);
                                }
                            }
                        }
                    }
                    
                    // 更新lastHP
                    lastHP = target.HP;
                    
                    if (!target.isAlive() && target instanceof Mob) {
                        // 敌人持有buff死亡时，hero恢复10%生命
                        int healAmount = Math.round(hero.HT * 0.1f);
                        hero.HP = Math.min(hero.HP + healAmount, hero.HT);
                        if (hero.sprite != null && hero.HP < hero.HT) {
                            hero.sprite.emitter().burst(com.zootdungeon.effects.Speck.factory(com.zootdungeon.effects.Speck.HEALING), 1);
                        }
                        GLog.p(Messages.get(this, "heal", healAmount));
                    }
                }
                
                left -= TICK;
                spend(TICK);
            } else {
                detach();
            }
            
            return true;
        }
        
        @Override
        public String desc() {
            return Messages.get(this, "desc", dispTurns(left));
        }
    }
}

