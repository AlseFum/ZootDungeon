package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.AscensionChallenge;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.TextureRegistry;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

/**
 * 远程射击型 Boss —— 先驱者。
 *
 * 在 {@link #ATTACK_RANGE} 格内发现敌人时，会发射暗影能量远程攻击，
 * 命中时在目标脚下产生粒子效果。
 * 属性随当前楼层深度（{@link Dungeon#depth}）成长，适合中前期登场。
 *
 * 使用方式（在自定义楼层/房间中生成）：
 * <pre>{@code
 * SarkazCenturion boss = new SarkazCenturion();
 * boss.pos = cell;
 * GameScene.add(boss);
 * }</pre>
 */
public class SarkazCenturion extends Mob implements Callback {

    private static final float TIME_TO_ZAP = 1f;

    /** 远程攻击的最大距离（曼哈顿距离）。 */
    public static final int ATTACK_RANGE = 3;

    static {
        TextureRegistry.texture("mod:sarkazcenturion", "cola/Infantry.png");
    }

    {
        spriteClass = SarkazCenturionSprite.class;

        // 基础值；onAdd 中会根据楼层进一步调整
        HP = HT = 20;
        defenseSkill = 4;

        EXP = 6;
        maxLvl = 10;

        properties.add(Property.BOSS);

        lootChance = 0f;
    }

    /**
     * 随楼层成长属性，在 AscensionChallenge 修正前执行。
     */
    @Override
    protected void onAdd() {
        int depth = Math.max(1, Dungeon.depth);
        HT = 15 + 10 * (depth / 2 + 1);
        HP = HT;
        defenseSkill = 4 + depth;
        maxLvl = Math.min(30, 5 + depth * 2);
        EXP = 3 + depth;
        super.onAdd();
    }

    @Override
    public int damageRoll() {
        // 随楼层成长：2+depth ~ 4+depth*2
        return Random.NormalIntRange(2 + Dungeon.depth, 4 + Dungeon.depth * 2);
    }

    @Override
    public int attackSkill(Char target) {
        return 6 + Dungeon.depth * 2;
    }

    @Override
    public int drRoll() {
        return super.drRoll() + Random.NormalIntRange(0, 1 + Dungeon.depth / 3);
    }

    // ── 远近战判定 ────────────────────────────────────────

    @Override
    protected boolean canAttack(Char enemy) {
        if (super.canAttack(enemy)) {
            return true;
        }
        if (Dungeon.level.distance(pos, enemy.pos) > ATTACK_RANGE) {
            return false;
        }
        return new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos;
    }

    // ── 攻击选择 ────────────────────────────────────────

    @Override
    protected boolean doAttack(Char enemy) {
        // 贴身 / 远程视线受阻 → 近战
        if (Dungeon.level.adjacent(pos, enemy.pos)
                || new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos != enemy.pos) {
            return super.doAttack(enemy);
        }
        // 远程射击
        if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
            sprite.zap(enemy.pos);
            return false;
        } else {
            zap();
            return true;
        }
    }

    // ── 远程攻击 ────────────────────────────────────────

    public static class VoidBolt {}

    private void zap() {
        spend(TIME_TO_ZAP);
        Invisibility.dispel(this);

        Char enemy = this.enemy;
        if (hit(this, enemy, true)) {
            int dmg = damageRoll();
            dmg = Math.round(dmg * AscensionChallenge.statModifier(this));
            enemy.damage(dmg, new VoidBolt());

            // 命中时在目标脚下产生血红色粒子，竖直向上喷发
            Emitter e = GameScene.emitter();
            e.pos(DungeonTilemap.tileCenterToWorld(enemy.pos));
            e.burst(new Emitter.Factory() {
                @Override
                public void emit(Emitter emitter, int index, float x, float y) {
                    PixelParticle p = (PixelParticle) emitter.recycle(PixelParticle.Shrinking.class);
                    p.reset(x + Random.Float(-6, 6), y, 0xCC0000, 4, Random.Float(0.4f, 0.8f));
                    p.speed.set(0, -Random.Float(48, 80));
                    p.acc.set(0, +100);
                }
            }, 16);

            if (!enemy.isAlive() && enemy == Dungeon.hero) {
                Dungeon.fail(this);
            }
        } else {
            enemy.sprite.showStatus(CharSprite.NEUTRAL, enemy.defenseVerb());
        }
    }

    /** 动画完成后回调（由 Sprite 触发）。 */
    public void onZapComplete() {
        zap();
        next();
    }

    @Override
    public void call() {
        next();
    }

    // ── Sprite ────────────────────────────────────────

    public static class SarkazCenturionSprite extends MobSprite {

        public SarkazCenturionSprite() {
            super();
            scale.set(0.7f);
            boolean hasMod = TextureRegistry.the("mod:sarkazcenturion") != null;
            TextureFilm frames = hasMod
                    ? textureWithFallback("mod:sarkazcenturion", Assets.Sprites.RAT, 32, 32)
                    : textureWithFallback(null, Assets.Sprites.RAT, 16, 15);

            idle = new Animation(1, true);
            idle.frames(frames, hasMod ? 23 : 0);

            run = new Animation(10, true);
            if (hasMod) run.frames(frames, 0, 1, 2, 3, 4, 5, 6);
            else run.frames(frames, 0, 1, 2, 3);

            attack = new Animation(10, false);
            if (hasMod) attack.frames(frames, 8, 9, 10, 11, 12);
            else attack.frames(frames, 4, 5, 6);

            zap = attack.clone();

            die = new Animation(9, false);
            if (hasMod) die.frames(frames, 13, 14, 15, 16, 17, 18, 19, 20, 21);
            else die.frames(frames, 7, 8, 9);

            play(idle);
        }

        /**
         * 远程射击动画：播放 zap 动画后由 {@link #onComplete} 回调 Boss 本体。
         * 不发射弹道（MagicMissile），伤害由粒子代替视觉效果。
         */
        @Override
        public void zap(int cell) {
            super.zap(cell);
            Sample.INSTANCE.play(Assets.Sounds.ZAP);
        }

        @Override
        public void onComplete(Animation anim) {
            if (anim == zap) {
                idle();
                // zap 动画结束时通知 Boss 结算伤害
                if (ch instanceof SarkazCenturion) {
                    ((SarkazCenturion) ch).onZapComplete();
                }
            }
            super.onComplete(anim);
        }
    }
}
