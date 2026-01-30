package com.zootdungeon.items.weapon.gun;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.weapon.ammo.Ammo;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;

public class GrenadeLauncher extends Gun {

    {
        image = ItemSpriteSheet.CROSSBOW;
        defaultAction = AC_FIRE;
        maxAmmo = 6;
        ammo = maxAmmo;
        reloadTime = 2f;
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    public boolean canLoad(Ammo ammo) {
        return ammo != null && ammo.cartridge != null;
    }

    @Override
    public int STRReq(int lvl) {
        return 12 + lvl; // 较高的力量需求
    }

    @Override
    public int min(int lvl) {
        return 15 + 5 * lvl; // 较高的基础伤害
    }

    @Override
    public int max(int lvl) {
        return 25 + 10 * lvl; // 较高的最大伤害
    }

    @Override
    public HitResult[] fire_hits(Char shooter, int targetPos, int projectileType) {
        Ballistica trajectory = new Ballistica(shooter.pos, targetPos, Ballistica.STOP_TARGET);
        int hitPos = trajectory.collisionPos;
        return new HitResult[]{new HitResult(hitPos, Actor.findChar(hitPos))};
    }
}
