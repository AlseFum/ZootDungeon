package com.zootdungeon.arknights.firearms;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.firearms.FirearmBullet;
import com.zootdungeon.items.weapon.firearms.FirearmMagazine;
import com.zootdungeon.items.weapon.firearms.FirearmWeapon;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Laterano-style firearm: switchable magazine with a special round mode.
 * This is a standalone weapon (not debug-supply inner class).
 */
public class LateranGun extends FirearmWeapon implements FirearmWeapon.FirearmTraitShotgun {

    protected FirearmMagazine.SwitchMagazine mag = new FirearmMagazine.SwitchMagazine();

    {
        tier = 2;
        maxRange = 6;
        DLY = 1.15f;
        gunDamageMult = 1.0f;

        mag.defaultBullet = FirearmBullet.Presets.buckshot();
        mag.specialBullet = FirearmBullet.Presets.incendiary();
        magazine = mag;
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (magazine instanceof FirearmMagazine.SwitchMagazine) {
            mag = (FirearmMagazine.SwitchMagazine) magazine;
        } else {
            mag = new FirearmMagazine.SwitchMagazine();
            mag.defaultBullet = FirearmBullet.Presets.buckshot();
            mag.specialBullet = FirearmBullet.Presets.incendiary();
            magazine = mag;
        }
    }

    @Override
    public int pelletCount() {
        return 7;
    }

    public void chargeSpecialAmmo(int amount, boolean enableMode) {
        if (!(magazine instanceof FirearmMagazine.SwitchMagazine)) return;
        FirearmMagazine.SwitchMagazine sm = (FirearmMagazine.SwitchMagazine) magazine;
        sm.setSpecialAmmo(Math.max(0, amount));
        if (enableMode) sm.setSpecialMode(true);
    }

    @Override
    public String name() {
        return "LateranGun";
    }

    @Override
    public String desc() {
        return "拉特兰制式霰弹枪。常规为霰弹，特殊弹切换为燃烧弹。";
    }

    /** Consumable: charges the equipped {@link LateranGun} with special ammo and enables special mode. */
    public static class SpecialCharge extends Item {

        public static final String AC_CHARGE = "CHARGE";

        private static final int CHARGE_TO = 30;

        {
            image = ItemSpriteSheet.STONE_HOLDER;
            stackable = true;
            defaultAction = AC_CHARGE;
        }

        @Override
        public ArrayList<String> actions(Hero hero) {
            ArrayList<String> actions = super.actions(hero);
            actions.add(AC_CHARGE);
            return actions;
        }

        @Override
        public String actionName(String action, Hero hero) {
            if (AC_CHARGE.equals(action)) return "充能特弹";
            return super.actionName(action, hero);
        }

        @Override
        public void execute(Hero hero, String action) {
            if (AC_CHARGE.equals(action)) {
                if (!(hero.belongings.weapon() instanceof LateranGun)) {
                    GLog.w("未装备 LateranGun。");
                    return;
                }

                LateranGun gun = (LateranGun) hero.belongings.weapon();
                gun.chargeSpecialAmmo(CHARGE_TO, true);
                GLog.i("LateranGun 特弹已充能至 " + CHARGE_TO + "，并已启用特殊模式。");

                detach(hero.belongings.backpack);
                hero.spendAndNext(0.5f);
                return;
            }
            super.execute(hero, action);
        }

        @Override
        public String name() {
            return "Lateran 特弹充能";
        }

        @Override
        public String desc() {
            return "消耗后将已装备的 LateranGun 特弹填充并启用特殊模式。";
        }

        @Override
        public boolean isUpgradable() {
            return false;
        }

        @Override
        public boolean isIdentified() {
            return true;
        }
    }
}

