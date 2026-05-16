package com.zootdungeon.arknights.RhodesStandardWeapons;

import com.zootdungeon.Assets;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.sprites.TextureRegistry;

public class RhodesStandardSword extends MeleeWeapon {

    static {
        TextureRegistry.texture("mod:rhodes_standard_sword", "cola/rhodes_sword.png")
                .setArea("rhodes_standard_sword", 0, 0, 32, 32);
    }

    {
        image = TextureRegistry.idByLabel("rhodes_standard_sword");
        hitSound = Assets.Sounds.HIT_SLASH;
        tier = 1;
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

