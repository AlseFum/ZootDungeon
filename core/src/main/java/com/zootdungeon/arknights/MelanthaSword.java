package com.zootdungeon.arknights;

import com.zootdungeon.items.weapon.base.CleaveWeapon;
import com.zootdungeon.sprites.TextureRegistry;

public class MelanthaSword extends CleaveWeapon {
    {
        image = TextureRegistry.once("melantha_sword", "cola/melantha_sword.png", 0, 0, 32, 32);
        tier = 1;
        dmgBoostBase = 2;
    }
}
