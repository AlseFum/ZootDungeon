package com.zootdungeon.actors.blobs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.BlobEmitter;
import com.zootdungeon.effects.particles.ElmoParticle;
import com.zootdungeon.effects.particles.FlameParticle;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 脉冲烈焰区域 — 静态 Blob，不会蔓延。
 * <p>
 * 以 3×3 或更小的块状生成，周期性地在激活/熄灭状态间切换。
 * 激活状态时对踩踏其上的角色（敌我双方）造成伤害。
 * 熄灭状态时无效果。
 */
public class PulseBlob extends Blob {

    /** 激活持续回合数。 */
    private static final int ACTIVE_TICKS = 3;

    /** 熄灭持续回合数。 */
    private static final int INACTIVE_TICKS = 3;

    /** 当前状态计时器。 */
    private int timer = 0;

    /** 是否为激活状态。 */
    private boolean active = true;

    @Override
    protected void evolve() {
        timer++;
        int threshold = active ? ACTIVE_TICKS : INACTIVE_TICKS;

        if (timer >= threshold) {
            timer = 0;
            active = !active;
        }

        // 不蔓延：保持 volume
        System.arraycopy(cur, 0, off, 0, cur.length);

        if (active) {
            for (int i = 0; i < cur.length; i++) {
                if (cur[i] > 0) {
                    Char ch = Actor.findChar(i);
                    if (ch != null && ch.isAlive()) {
                        int dmg = 2 + Dungeon.scalingDepth() / 2;
                        ch.damage(dmg, this);
                        if (ch.sprite != null) {
                            ch.sprite.showStatus(CharSprite.NEGATIVE,
                                    Messages.get(this, "status", dmg));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void use(BlobEmitter emitter) {
        super.use(emitter);
        // 粒子效果跟随激活状态
        if (active) {
            emitter.pour(FlameParticle.FACTORY, 0.08f);
        } else {
            emitter.pour(ElmoParticle.FACTORY, 0.03f);
        }
    }

    @Override
    public String tileDesc() {
        return Messages.get(this, "desc");
    }
}
