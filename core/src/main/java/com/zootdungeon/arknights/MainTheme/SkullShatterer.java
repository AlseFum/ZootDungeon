package com.zootdungeon.arknights.MainTheme;

import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.MainTheme.HourOfAnAwakening.HourOfAnAwakeningBossLevel;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.items.keys.SkeletonKey;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.ui.BossHealthBar;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class SkullShatterer extends Mob {

    static {
        // SpriteRegistry no longer manages mob textures.
        SpriteRegistry.texture("mod:skull_shatterer", "cola/skullshatter.png");
    }

    private final SkullShattererWeapon weapon = new SkullShattererWeapon();
    private float speechCooldown = 0f;

    {
        spriteClass = Sprite.class;
        HP = HT = 24;
        defenseSkill = 7;
        EXP = 5;
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
        return 12;
    }

    @Override
    public int drRoll() {
        return super.drRoll() + Random.NormalIntRange(0, 3);
    }

    @Override
    public void die(Object cause) {
        super.die(cause);
        sayOnce(Messages.get(this, "defeated"), true);
        Dungeon.level.unseal();
        GameScene.bossSlain();
        Dungeon.level.drop(new SkeletonKey(Dungeon.depth), pos).sprite.drop();
        Badges.validateBossSlain();
    }

    @Override
    public void notice() {
        super.notice();
        sayOnce(Messages.get(this, "notice"), true);
        if (!BossHealthBar.isAssigned()) {
            BossHealthBar.assignBoss(this);
            Dungeon.level.seal();
        }
    }

    @Override
    protected void afterFieldOfViewUpdated() {
        super.afterFieldOfViewUpdated();
        if (!(Dungeon.level instanceof HourOfAnAwakeningBossLevel)) return;
        Hero h = Dungeon.hero;
        if (!h.isAlive() || h.invisible > 0) return;
        if (fieldOfView[h.pos]) {
            ((HourOfAnAwakeningBossLevel) Dungeon.level).onShattererSquadSpottedHero();
        }
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

    public void onZapComplete(int cell) {
        sayOnce(Messages.get(this, "grenade"), false);
        weapon.doGrenadeAt(this, cell);
        weapon.clearGrenadeState();
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
            if (speechCooldown > 0) speechCooldown -= TICK;
            if (!enemyInFOV || enemy == null) {
                return super.act(enemyInFOV, justAlerted);
            }
            enemySeen = true;
            target = enemy.pos;
            weapon.tickCooldown();

            if (HP > 0 && HT > 0 && HP <= HT / 3) {
                sayOnce("别想逃。", false);
            }

            if (weapon.canFireRanged() && !hasAllyInNeighbour8() && !Dungeon.level.adjacent(pos, enemy.pos)) {
                sayOnce("锁定。", false);
                weapon.doGrenadeAt(SkullShatterer.this, enemy.pos);
                weapon.clearGrenadeState();
                spend(TICK);
                return true;
            }

            return super.act(enemyInFOV, justAlerted);
        }
    }

    {
        HUNTING = new Hunting();
    }

    public static class Sprite extends MobSprite {
        public Sprite() {
            super();

            TextureFilm frames = textureWithFallback("mod:skull_shatterer", Assets.Sprites.GNOLL, 64,64);
            scale.set(0.28f);
            idle = new Animation(2, true);
            idle.frames(frames, 0);

            run = new Animation(12, true);
            run.frames(frames, 0);

            attack = new Animation(12, false);
            attack.frames(frames, 0);

            die = new Animation(12, false);
            die.frames(frames, 0);

            zap = attack.clone();

            play(idle);
        }

        @Override
        public void zap(int cell) {
            super.zap(cell);
            MagicMissile.boltFromChar(parent, MagicMissile.FIRE_CONE, this, cell,
                    (Callback) () -> {
                        if (ch instanceof SkullShatterer) ((SkullShatterer) ch).onZapComplete(cell);
                    });
        }
    }

    private void sayOnce(String line, boolean important) {
        if (line == null || line.isEmpty()) return;
        if (!important && speechCooldown > 0f) return;
        // cooldown avoids spamming when boss is repeatedly acting/triggering.
        speechCooldown = important ? 4f : 2f;
        if (sprite != null) {
            sprite.showStatus(CharSprite.NEUTRAL, line);
        }
        // Also log it so players on small screens don't miss it.
        GLog.n(Messages.format("[碎骨] %s", line));
    }
}
