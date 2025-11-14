package com.zootdungeon.items.weapon.ammo;

import com.zootdungeon.sprites.ItemSpriteManager;

public class ExplosiveAmmo extends Ammo {
    {
        image = ItemSpriteManager.ByName("explosive_ammo");
    }
    public ExplosiveAmmo() {
        super();
        this.cartridge = new Cartridge(10, CartridgeEffect.Explosive);
    }
    @Override
    public String name() {
        return "爆炸弹药";
    }
    @Override
    public String desc() {
        return "爆炸弹药，可以造成爆炸伤害。";
    }
}
