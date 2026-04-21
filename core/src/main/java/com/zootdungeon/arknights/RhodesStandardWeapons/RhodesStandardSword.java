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

package com.zootdungeon.arknights.RhodesStandardWeapons;

import com.zootdungeon.Assets;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.sprites.SpriteRegistry;

public class RhodesStandardSword extends MeleeWeapon {

    static {
        SpriteRegistry.texture("mod:rhodes_standard_sword", "cola/rhodes_sword.png")
                .grid(32, 32)
                .label("rhodes_standard_sword");
    }

    {
        image = SpriteRegistry.byLabel("rhodes_standard_sword");
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.0f;
        
        tier = 0;
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }
}

