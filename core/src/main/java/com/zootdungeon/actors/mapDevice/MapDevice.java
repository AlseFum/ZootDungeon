package com.zootdungeon.actors.mapDevice;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.sprites.MobSprite;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
/**
 * MapDevice：可阻挡寻路的"特殊 mob"。
 *
 * 设计要点：
 * - 继承 {@link Mob} 让它能跟 Mob 一样被 Hero/Mob AI 视为可攻击目标（带
 *   CharHealthIndicator、AttackIndicator、Enemy selection 入口），但自身
 *   不移动/不主动攻击/damage 链走 {@link #receiveDamage(Object)} 而非 HP。
 * - 阻挡寻路：Mob 子类占格子的语义在 Actor.findChar 处理，MapDevice 自动继承。
 * - spriteClass 字段直接复用 {@link Mob#spriteClass}：子类在 instance block
 *   里赋 `spriteClass = MapDevice.Sprite.class;` 即可。之所以不能再声明
 *   同名字段：AttackIndicator.updateImage 通过 `lastTarget.spriteClass`
 *   （JVM 静态类型 Mob）取值，若本类重声明，会变成另一个 slot，导致两边
 *   写入不同字段。
 * - die(...) 走标准 Mob/Char 死亡链：destroy() + sprite.die()，由
 *   {@link com.zootdungeon.sprites.MobSprite#onComplete} 做 alpha fade。
 *   MapDevice 不会掉 EXP/loot（alignment == NEUTRAL）。
 * - act 默认无操作。
 *
 * <h2>默认 Sprite</h2>
 * {@link Sprite} 是与本类配套的占位 sprite：与 Pylon 一样的 1 帧 10×20 静态
 * 物件视觉（reuses Pylon 贴图），继承自 MobSprite（拿 {@code textureWithFallback}）。
 * 真实子类应另写自己的 sprite，并赋给 {@code spriteClass}。
 */
public abstract class MapDevice extends Mob {

    {
        alignment = Alignment.NEUTRAL;
        // 比 Mob 稍晚，避免扰动 Mob AI 循环顺序
        actPriority = MOB_PRIO + 1;
        // 不主动攻击 / 不会自动被 hero 视为目标；
        // 但 click attack 仍能命中（Hero 走 attackSkill / 寻路 1 格邻近的格子）。
        state = PASSIVE;
    }

    public MapDevice() {
        super();
        HP = HT = 1; // 子类必须在构造里重设
    }

    @Override
    protected boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int attackSkill(Char target) {
        return 0;
    }

    @Override
    public int defenseSkill(Char enemy) {
        return 0;
    }

    @Override
    public int damageRoll() {
        return 0;
    }

    @Override
    public int drRoll() {
        return 0;
    }

    /**
     * 走 Char.damage 链时直接 redirect 到 {@link #receiveDamage(Object)}：
     * MapDevice 不按 HP 承受伤害，而是把攻击事件转交给本方法（一般只让 charge +1）。
     */
    @Override
    public void damage(int dmg, Object src) {
        if (!isAlive()) {
            return;
        }
        if (src == null) {
            return;
        }
        receiveDamage(src);
    }

    /**
     * 接受伤害入口。默认无操作：具体 MapDevice 子类重写以累计 charge 或触发效果。
     *
     * @param src 伤害来源（攻击者、投射物、效果等）
     */
    public void receiveDamage(Object src) {
        // 默认无操作：子类重写
    }


    @Override
	public void restoreFromBundle( Bundle bundle ) {
		
		super.restoreFromBundle( bundle );
	}
    @Override
	public void storeInBundle( Bundle bundle ) {
		
		super.storeInBundle( bundle );
	}


    /**
     * 与 MapDevice 配套的占位 sprite。复用 Pylon 贴图（"贴在地上的物件"语义贴近），
     * 一帧 10×20，perspectiveRaise 与 Pylon 一致。
     *
     * <p>子类想要不同视觉，应另写 sprite class 并赋给 {@code spriteClass}；只有
     * 用默认占位时才需要引用本类。</p>
     */
    
    public static class Sprite extends MobSprite {

        private static final int FRAME_W = 10;
        private static final int FRAME_H = 20;

        public Sprite() {
            super();

            // 跟 Pylon 一致：pylon 同样是不想被升起来的"贴在地上"的物件。
            perspectiveRaise = 5 / 16f;
            renderShadow = false;

            // reuse Pylon 贴图；textureWithFallback 是 MobSprite 提供的 helper
            TextureFilm frames = textureWithFallback(
                    "mod:mapdevice",
                    Assets.Sprites.PYLON,
                    FRAME_W, FRAME_H);

            idle = new MovieClip.Animation(1, false);
            idle.frames(frames, 0);

            run = idle.clone();
            attack = idle.clone();
            operate = idle.clone();
            zap = idle.clone();
            // die 用 idle 同帧，MobSprite.onComplete 会做 alpha fade，无需新贴图。
            die = idle.clone();

            play(idle);
        }
    }
}



