package com.zootdungeon.items.weapon.ammo;

import com.zootdungeon.sprites.SpriteRegistry;

public class ExplosiveAmmo extends Ammo {
    {
        image = SpriteRegistry.byLabel("explosive_ammo");
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
