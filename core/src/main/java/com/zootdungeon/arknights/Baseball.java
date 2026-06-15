package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class Baseball extends MissileWeapon {

    private int batBonus = 0;

    {
        image = ItemSpriteSheet.THROWING_CLUB;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.2f;

        tier = 1;
        baseUses = 10;
        sticky = false;
        bones = true;
    }

    @Override
    public int buffedLvl() {
        return super.buffedLvl() + batBonus;
    }

    public void setBatBonus(int bonus) {
        this.batBonus = bonus;
    }

    @Override
    public int min(int lvl) {
        return 2 * tier + lvl;
    }

    @Override
    public int max(int lvl) {
        return 2 * tier + 5 * lvl;
    }

    @Override
    protected void onThrow(int cell) {
        batBonus = 0;
        super.onThrow(cell);
        this.batBonus = 0;
    }
}
