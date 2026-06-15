package com.zootdungeon.actors.mobs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.mechanics.Damage;
import com.zootdungeon.sprites.PylonSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Device extends Mob {

    private static final int SP_MAX = 10;
    private static final int ATTACK_RANGE = 6;

    private int sp = 0;
    private boolean active = false;

    {
        spriteClass = PylonSprite.class;

        HT = HP = 1;
        EXP = 0;
        maxLvl = -2;

        alignment = Alignment.NEUTRAL;
        state = PASSIVE;

        properties.add(Property.IMMOVABLE);
        properties.add(Property.INORGANIC);
        properties.add(Property.STATIC);
    }

    @Override
    protected boolean act() {
        if (!active) {
            spend(TICK);
            return true;
        }

        ArrayList<Char> candidates = new ArrayList<>();
        for (Char ch : Actor.chars()) {
            if (ch == null || ch == this || !ch.isAlive()) continue;
            if (Dungeon.level.distance(pos, ch.pos) > ATTACK_RANGE) continue;
            Ballistica bolt = new Ballistica(pos, ch.pos, Ballistica.PROJECTILE);
            if (bolt.collisionPos != ch.pos) continue;
            candidates.add(ch);
        }

        if (!candidates.isEmpty()) {
            Char target = Random.element(candidates);
            int dmg = Random.NormalIntRange(3, 8);
            Damage.additional(this, target, Damage.SHOCK, dmg, this);
            if (target == Dungeon.hero && !target.isAlive()) {
                Dungeon.fail(this);
            }
        }

        spend(TICK);
        return true;
    }

    @Override
    public void beckon(int cell) {
        // immovable device does not respond to beckon.
    }

    @Override
    public void damage(int dmg, Object src) {
        if (dmg <= 0) return;
        sp += dmg;
        if (sp >= SP_MAX) {
            sp = SP_MAX;
            active = true;
        }
        if (sprite != null) sprite.flash();
    }

    @Override
    public void die(Object cause) {
        // Device never dies.
        HP = HT;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    private static final String SP = "sp";
    private static final String ACTIVE = "active";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SP, sp);
        bundle.put(ACTIVE, active);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        sp = bundle.getInt(SP);
        active = bundle.getBoolean(ACTIVE);
    }
}
