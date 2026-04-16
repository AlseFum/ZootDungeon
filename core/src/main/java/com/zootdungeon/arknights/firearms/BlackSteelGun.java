package com.zootdungeon.arknights.firearms;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.firearms.FirearmBullet;
import com.zootdungeon.items.weapon.firearms.FirearmMagazine;
import com.zootdungeon.items.weapon.firearms.FirearmWeapon;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.windows.WndBag;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * BlackSteel-style firearm: detachable magazine swap + eject magazine action.
 */
public class BlackSteelGun extends FirearmWeapon implements FirearmWeapon.FirearmTraitPistol {

    private static final String AC_EJECT_MAG = "EJECT_MAG";

    protected FirearmMagazine.SwapMagazine mag = new FirearmMagazine.SwapMagazine();

    {
        tier = 2;
        maxRange = 7;
        DLY = 0.94f;
        gunDamageMult = 0.95f;

        mag.activeMagazine = new FirearmMagazine("c_pistol_mag", FirearmBullet.Presets.fmj(), 8, 8);
        magazine = mag;
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (magazine instanceof FirearmMagazine.SwapMagazine) {
            mag = (FirearmMagazine.SwapMagazine) magazine;
        } else {
            mag = new FirearmMagazine.SwapMagazine();
            mag.activeMagazine = new FirearmMagazine("c_pistol_mag", FirearmBullet.Presets.fmj(), 8, 8);
            magazine = mag;
        }
    }

    @Override
    protected String reloadActionName() {
        return "换弹匣";
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            actions.add(AC_EJECT_MAG);
        }
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_EJECT_MAG.equals(action)) return "退弹匣";
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        if (AC_EJECT_MAG.equals(action)) {
            ejectMagazine(hero);
            return;
        }
        super.execute(hero, action);
    }

    private void ejectMagazine(Hero hero) {
        if (mag.activeMagazine == null) {
            com.zootdungeon.utils.GLog.w("当前没有装入弹匣。");
            return;
        }
        FirearmMagazine out = mag.activeMagazine;
        mag.activeMagazine = null;
        if (!out.collect(hero.belongings.backpack)) {
            Dungeon.level.drop(out, hero.pos).sprite.drop(hero.pos);
            com.zootdungeon.utils.GLog.w("背包已满，弹匣掉落在地。");
        } else {
            com.zootdungeon.utils.GLog.i("已退出弹匣。");
        }
        hero.spendAndNext(0.5f);
    }

    @Override
    protected void reload(Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override
            public String textPrompt() {
                return "选择要换上的弹匣";
            }

            @Override
            public boolean itemSelectable(Item item) {
                return item != null && item.getClass() == FirearmMagazine.class;
            }

            @Override
            public void onSelect(Item item) {
                if (!(item instanceof FirearmMagazine)) return;
                FirearmMagazine selected = (FirearmMagazine) item.detachAll(hero.belongings.backpack);
                FirearmMagazine old = mag.activeMagazine;
                mag.activeMagazine = selected;
                if (old != null) old.collect(hero.belongings.backpack);
                com.zootdungeon.utils.GLog.i("已更换弹匣。");
                hero.spendAndNext(0.8f);
            }
        });
    }

    @Override
    public String name() {
        return "BlackSteelGun";
    }

    @Override
    public String desc() {
        return "黑钢制式。以弹匣为单位换弹/退弹匣。";
    }
}

