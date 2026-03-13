package com.zootdungeon.arknights.MainTheme;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.sprites.GnollSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class SkullShatterer extends Mob {

    private final SkullShattererWeapon weapon = new SkullShattererWeapon();

    {
        spriteClass = Sprite.class;
        HP = HT = 28;
        defenseSkill = 8;
        EXP = 6;
        maxLvl = 14;
        loot = Gold.class;
        lootChance = 0.4f;
        weapon.setMode(SkullShattererWeapon.Mode.RANGED);
    }

    public SkullShattererWeapon getWeapon() { return weapon; }

    @Override
    public int damageRoll() {
        return weapon.damageRoll(this);
    }

    @Override
    public int attackSkill(Char target) {
        return 14;
    }

    @Override
    public int drRoll() {
        return super.drRoll() + Random.NormalIntRange(0, 4);
    }

    private boolean hasAllyInNeighbour8() {
        for (int i : PathFinder.NEIGHBOURS8) {
            int cell = pos + i;
            if (cell < 0 || cell >= Dungeon.level.length()) continue;
            Char ch = Actor.findChar(cell);
            if (ch != null && ch.alignment == Alignment.ALLY) return true;
        }
        return false;
    }

    public void onZapComplete() {
        weapon.onZapComplete(this);
        spend(TICK);
        next();
    }

    private static final String WEAPON = "weapon";

    @Override
    public void storeInBundle(com.watabou.utils.Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(WEAPON, weapon);
    }

    @Override
    public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(WEAPON))
            weapon.restoreFromBundle(bundle.getBundle(WEAPON));
    }

    private class Hunting extends Mob.Hunting {
        @Override
        public boolean act(boolean enemyInFOV, boolean justAlerted) {
            if (!enemyInFOV || enemy == null) {
                return super.act(enemyInFOV, justAlerted);
            }
            enemySeen = true;
            target = enemy.pos;
            weapon.tickCooldown();

            if (weapon.isReleaseTurn()) {
                int cell = weapon.getGrenadeTargetCell();
                if (cell == -1) {
                    spend(TICK);
                    return true;
                }
                if (sprite != null && (sprite.visible || (Actor.findChar(cell) != null && Actor.findChar(cell).sprite.visible))) {
                    sprite.zap(cell);
                    return false;
                }
                weapon.doGrenadeAt(SkullShatterer.this, cell);
                weapon.clearGrenadeState();
                spend(TICK);
                return true;
            }

            if (weapon.canFireRanged() && !hasAllyInNeighbour8() && !Dungeon.level.adjacent(pos, enemy.pos)) {
                weapon.startAim(SkullShatterer.this, enemy.pos);
                if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
                    sprite.zap(enemy.pos);
                    return false;
                }
                weapon.finishAimPhase();
                spend(TICK);
                return true;
            }

            return super.act(enemyInFOV, justAlerted);
        }
    }

    {
        HUNTING = new Hunting();
    }

    public static class Sprite extends GnollSprite {
        @Override
        public void zap(int cell) {
            super.zap(cell);
            MagicMissile.boltFromChar(parent, MagicMissile.FIRE_CONE, this, cell,
                    (Callback) () -> {
                        if (ch instanceof SkullShatterer) ((SkullShatterer) ch).onZapComplete();
                    });
        }
    }
}
