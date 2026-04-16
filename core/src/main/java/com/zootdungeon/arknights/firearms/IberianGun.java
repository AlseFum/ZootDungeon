package com.zootdungeon.arknights.firearms;

import com.zootdungeon.items.weapon.firearms.FirearmBullet;
import com.zootdungeon.items.weapon.firearms.FirearmMagazine;
import com.zootdungeon.items.weapon.firearms.FirearmWeapon;
import com.watabou.utils.Bundle;

/**
 * Iberia-style firearm: queue-loaded magazine (round-by-round).
 */
public class IberianGun extends FirearmWeapon implements FirearmWeapon.FirearmTraitAimable, FirearmWeapon.FirearmTraitPistol {

    protected FirearmMagazine.QueueMagazine mag = new FirearmMagazine.QueueMagazine();

    {
        tier = 1;
        maxRange = 12;
        DLY = 1.22f;
        gunDamageMult = 1.15f;

        mag.queueCapacity = 4;
        mag.reloadTemplate = FirearmBullet.Presets.incendiary();
        magazine = mag;
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (magazine instanceof FirearmMagazine.QueueMagazine) {
            mag = (FirearmMagazine.QueueMagazine) magazine;
        } else {
            mag = new FirearmMagazine.QueueMagazine();
            mag.queueCapacity = 4;
            mag.reloadTemplate = FirearmBullet.Presets.incendiary();
            magazine = mag;
        }
    }

    @Override
    protected String reloadActionName() {
        return "装弹";
    }

    @Override
    public int aimCap() {
        return 6;
    }

    @Override
    public float aimDamagePerLayer() {
        return 0.25f;
    }

    @Override
    public float aimAccuracyPerLayer() {
        return 0.2f;
    }

    @Override
    public boolean keepHalfAimOnMove() {
        return true;
    }

    @Override
    public String name() {
        return "IberianGun";
    }

    @Override
    public String desc() {
        return "伊比利亚制式狙击手枪。提供瞄准动作；队列装填，小容量便于测试。";
    }
}

