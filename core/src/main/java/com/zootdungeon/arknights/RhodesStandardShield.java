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

package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class RhodesStandardShield extends MeleeWeapon {
    
    {
        image = ItemSpriteSheet.ROUND_SHIELD;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.0f;
        
        tier = 0;
    }
    
    // 格挡值：基础3，每级+1
    public int drBase = 3;
    public int drPerLevel = 1;
    
    @Override
    public String name() {
        return "罗德岛标准盾牌";
    }
    
    @Override
    public String info() {
        String info = "一面标准的盾牌，可以提供一定的格挡能力。";
        info += "\n\n这把武器可以格挡 0-" + DRMax() + " 点伤害。格挡值随升级提升。";
        return info;
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        // 盾牌伤害较低
        return Math.round(3f * (tier + 1)) + lvl * (tier - 1);
    }
    
    @Override
    public int defenseFactor(Char owner) {
        return DRMax();
    }
    
    public int DRMax() {
        return DRMax(buffedLvl());
    }
    
    public int DRMax(int lvl) {
        return drBase + drPerLevel * lvl;
    }
}

