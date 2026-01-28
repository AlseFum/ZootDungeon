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
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.PathFinder;

public class AscalonAOE extends MeleeWeapon {
    
    static {
        SpriteRegistry.registerItemTexture("cola/ascalon_weapon.png",64)
                .label("ascalon_aoe");
    }
    
    {
        image = SpriteRegistry.itemByName("ascalon_aoe");
        tier = 0;
        bones = false;
        RCH=3;
    }
    
    private static final int AOE_RADIUS = 3; // 范围半径
    private static final float AOE_DAMAGE_MULT = 0.3f; // 范围伤害倍率（30%）
    private static final float EVASION_BONUS = 1.5f; // 闪避加成（50%）
    
    @Override
    public String name(){
        return "“复仇者·范围”";
    }

    @Override
    public String desc(){
        return "在阿斯卡纶第一次为军事委员会完成任务后，由特雷西斯亲手赠送，特蕾西娅为她安装的第一把武器。\n\n" +
               "这把武器的每次攻击都会对大范围内的敌人造成伤害并附加持续伤害效果。装备时提供50%的闪避加成。";
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
        
        // 每次攻击时触发范围伤害
        if (attacker instanceof Hero && defender.isAlive()) {
            Hero hero = (Hero) attacker;
            // 获取范围内的所有位置
            boolean[] affected = new boolean[Dungeon.level.length()];
            PathFinder.buildDistanceMap(defender.pos, Dungeon.level.passable, AOE_RADIUS);
            
            for (int i = 0; i < Dungeon.level.length(); i++) {
                if (PathFinder.distance[i] <= AOE_RADIUS && PathFinder.distance[i] > 0) {
                    affected[i] = true;
                }
            }
            
            // 对范围内的所有敌人轮流 proc
            for (int pos = 0; pos < affected.length; pos++) {
                if (affected[pos]) {
                    Char ch = Actor.findChar(pos);
                    if (ch != null && ch != hero && ch != defender && ch.alignment != Char.Alignment.ALLY && ch.isAlive()) {
                        // 计算 AOE 伤害（30%的原始伤害）
                        int aoeDamage = Math.max(1, Math.round(damage * AOE_DAMAGE_MULT));
                        
                        // 对每个敌人调用 proc，让武器和附魔效果正常处理
                        proc(hero, ch, aoeDamage);
                        
                        // 附加 Wound buff
                        boolean inFog = com.zootdungeon.actors.blobs.Blob.volumeAt(hero.pos, com.zootdungeon.actors.blobs.SmokeScreen.class) > 0 
                                || com.zootdungeon.actors.blobs.Blob.volumeAt(ch.pos, com.zootdungeon.actors.blobs.SmokeScreen.class) > 0;
                        
                        AscalonWound existingWound = ch.buff(AscalonWound.class);
                        AscalonWound wound;
                        if (existingWound != null) {
                            wound = existingWound;
                        } else {
                            wound = new AscalonWound();
                            wound.attachTo(ch);
                        }
                        float duration = AscalonWound.DURATION * ch.resist(AscalonWound.class);
                        wound.extend(duration);
                        float attackDamage = hero.damageRoll();
                        wound.set(hero, attackDamage, inFog);
                    }
                }
            }
        }
        
        return damage;
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
}
