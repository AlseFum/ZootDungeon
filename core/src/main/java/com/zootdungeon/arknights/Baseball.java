package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.weapon.base.MissileWeapon;
import com.zootdungeon.sprites.TextureRegistry;

public class Baseball extends MissileWeapon {

    private boolean batSourced = false;

    {
        image = TextureRegistry.once("cuora_baseball","cola/cuora_baseball.png",0,0,32,32);
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.2f;

        tier = 1;
        baseUses = 10;
        sticky = false;
        bones = true;
    }

    public void markBatSourced() {
        this.batSourced = true;
    }

    @Override
    public int min(int lvl) {
        return 1 * tier + lvl;                      // 比标准 MissileWeapon 的 2*tier+lvl 更低
    }

    @Override
    public int max(int lvl) {
        return 2 * tier + lvl;                      // 比标准 MissileWeapon 的 5*tier+2*lvl 更低
    }

    @Override
    public int damageRoll(Char owner) {
        int damage = super.damageRoll(owner);
        if (batSourced) {
            damage *= 3;                            // 被球棒打出时伤害三倍
        }
        return damage;
    }

    @Override
    public float accuracyFactor(Char owner, Char target) {
        if (batSourced) {
            return 2.5f;                            // 被球棒打出时更不容易闪避
        }
        return super.accuracyFactor(owner, target);
    }

    @Override
    protected void onThrow(int cell) {
        super.onThrow(cell);
        batSourced = false;
    }
}
