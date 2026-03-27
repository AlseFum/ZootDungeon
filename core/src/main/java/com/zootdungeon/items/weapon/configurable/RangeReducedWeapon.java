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

package com.zootdungeon.items.weapon.configurable;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class RangeReducedWeapon extends MeleeWeapon {
    
    // 伤害衰减倍率：1格外的攻击伤害会乘以这个值
    public float reduceRate = 0.8f;
    
    // 最大攻击范围：最多可以攻击4格外的敌人
    private static final int MAX_RANGE = 4;
    private static final String REDUCE_RATE = "reduceRate";
    private static final String RANGE = "rch";
    
    {
        image = ItemSpriteSheet.SPEAR;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.0f;
        
        tier = 2;
        RCH = MAX_RANGE; // 设置最大攻击范围为4格
    }
    
    @Override
    public String name() {
        return Messages.get(this, "name");
    }
    
    @Override
    public String info() {
        return Messages.get(this, "desc", MAX_RANGE, (int)(reduceRate * 100));
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        // 先调用父类的 proc 方法处理基础逻辑
        damage = super.proc(attacker, defender, damage);
        
        // 计算攻击者到目标的距离
        int distance = Dungeon.level.distance(attacker.pos, defender.pos);
        
        // 如果距离大于1格，应用伤害衰减
        if (distance > 1) {
            // 每增加1格距离，伤害乘以 reduceRate
            // 例如：2格距离 = reduceRate^1, 3格距离 = reduceRate^2, 4格距离 = reduceRate^3
            float distanceMultiplier = (float) Math.pow(reduceRate, distance - 1);
            damage = Math.round(damage * distanceMultiplier);
        }
        
        return damage;
    }

    public RangeReducedWeapon randomize() {
        tier = Random.IntRange(1, 5);
        level(Random.IntRange(0, 3));
        reduceRate = Random.Float(0.65f, 0.9f);
        RCH = Random.IntRange(2, MAX_RANGE);
        return this;
    }

    @Override
    public Item random() {
        return randomize();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(REDUCE_RATE, reduceRate);
        bundle.put(RANGE, RCH);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(REDUCE_RATE)) reduceRate = bundle.getFloat(REDUCE_RATE);
        if (bundle.contains(RANGE)) RCH = bundle.getInt(RANGE);
    }
}

