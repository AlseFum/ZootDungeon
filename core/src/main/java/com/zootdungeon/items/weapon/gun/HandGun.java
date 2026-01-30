package com.zootdungeon.items.weapon.gun;

import java.util.ArrayList;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.ammo.Ammo;
import com.zootdungeon.items.weapon.ammo.Cartridge;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class HandGun extends Gun {

    private static final String AC_QUICKDRAW = "quickdraw";

    {
        image = ItemSpriteSheet.CROSSBOW; // 临时图标，需要替换为手枪图标
        defaultAction = AC_FIRE;

        // 设置弹药相关属性
        maxAmmo = 12;
        ammo = maxAmmo;
        tier=1;
    }
    @Override
    protected void addSubActions(Hero hero, ArrayList<String> actions) {
        actions.add(AC_QUICKDRAW);
    }

    @Override
    protected void executeSubAction(Hero hero, String action) {
        if (action.equals(AC_QUICKDRAW)) {
            Weapon originalWeapon = (Weapon) hero.belongings.weapon;
            hero.belongings.weapon = this;
            GameScene.selectCell(new CellSelector.Listener() {
                @Override
                public void onSelect(Integer target) {
                    if (target != null) {
                        fire(target);
                    }
                    if (originalWeapon != null) {
                        hero.belongings.backpack.items.add(originalWeapon);
                    }
                }
                @Override
                public String prompt() {
                    return Messages.get(HandGun.this, "prompt_quickdraw");
                }
            });
        }
    }

    @Override
    public boolean canLoad(Ammo ammo) {
        return ammo != null && ammo.cartridge != null && ammo.cartridge.cartridgeType == Cartridge.CartridgeType.SMALL;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_QUICKDRAW)) {
            return Messages.get(this, "ac_quickdraw");
        }
        return super.actionName(action, hero);
    }

    @Override
    public int STRReq(int lvl) {
        return 10 + lvl; // 手枪力量需求较低
    }

    @Override
    public int min(int lvl) {
        return 3 + lvl; // 基础最小伤害3，每级+1
    }

    @Override
    public int max(int lvl) {
        return 6 + 3 * lvl; // 基础最大伤害6，每级+3
    }

    @Override
    public float getAmmoPowerMultiplier() {
        return (float) Math.pow(1.2f + 0.3f * level(),tier);
    }


    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        String cartridgeStr = cartridge != null ? cartridge.cartridgeType.name() : Messages.get(Gun.class, "no_cartridge");
        return Messages.get(this, "desc", cartridgeStr);
    }
}
