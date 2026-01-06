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

package com.zootdungeon.arknights.misc;

import com.zootdungeon.items.supply.Supply;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;

public class RhodesStandardWeaponSupply extends Supply {
    {
        name = Messages.get(this, "name");
        desc = Messages.get(this, "desc");
        image = SpriteRegistry.itemByName("arksupply");
    }
    public RhodesStandardWeaponSupply() {
        super();
        
        // 添加所有标准武器
        put_in(RhodesStandardSword.class, 1);
        put_in(RhodesStandardShield.class, 1);
        put_in(RhodesStandardBow.class, 1);
        put_in(RhodesStandardWand.class, 1);
        // 弓需要箭
        put_in(RhodesStandardBow.Arrow.class, 50);
    }
}

