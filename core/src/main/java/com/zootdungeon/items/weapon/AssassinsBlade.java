package com.zootdungeon.items.weapon;
import com.zootdungeon.items.weapon.base.AmbushWeapon;

import com.zootdungeon.Assets;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class AssassinsBlade extends AmbushWeapon {

	{
		image = ItemSpriteSheet.ASSASSINS_BLADE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		tier = 4;
        ambushRate=0.50f;
	}
}