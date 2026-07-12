package com.zootdungeon.actors.blobs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.watabou.noosa.Image;
import com.zootdungeon.effects.BlobEmitter;
import com.zootdungeon.effects.particles.FlameParticle;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.BuffIndicator;

/**
 * 焦土区域 — 静态 Blob，不会蔓延。
 * <p>
 * 每回合对站在其上的角色（敌我双方）附加 {@link ScorchBuff}，
 * 该 buff 每回合造成伤害且伤害 +50%。
 */
public class ScorchBlob extends Blob {

    @Override
    protected void evolve() {
        // 不蔓延：保持原有 volume，不扩散到相邻格子
        int[] curCopy = cur.clone();

        for (int i = 0; i < curCopy.length; i++) {
            off[i] = curCopy[i]; // 保持原位不动
            if (curCopy[i] > 0) {
                Char ch = Actor.findChar(i);
                if (ch != null && ch.isAlive()) {
                    Buff.affect(ch, ScorchBuff.class).tick();
                }
            }
        }

        // volume 不变，area 不变
    }

    @Override
    public void use(BlobEmitter emitter) {
        super.use(emitter);
        emitter.pour(FlameParticle.FACTORY, 0.05f);
    }

    @Override
    public String tileDesc() {
        return Messages.get(this, "desc");
    }

    /**
     * 焦灼 buff：每回合造成一次伤害，伤害值为基础值 × 1.5。
     * <p>
     * 基础值随楼层递增：2 + depth / 2。
     */
    public static class ScorchBuff extends FlavourBuff {

        {
            type = buffType.NEUTRAL;
            announced = true;
        }

        /** 调用一次即造成一跳伤害（无视阵营）。 */
        public void tick() {
            if (target == null || !target.isAlive()) return;
            int baseDmg = 2 + Dungeon.scalingDepth() / 2;
            int dmg = Math.round(baseDmg * 1.5f); // +50%
            target.damage(dmg, this);
            if (target.sprite != null) {
                target.sprite.showStatus(CharSprite.NEGATIVE,
                        Messages.get(this, "status", dmg));
            }
        }

        @Override
        public int icon() {
            return BuffIndicator.FIRE;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(1f, 0.4f, 0f);
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc");
        }
    }
}
