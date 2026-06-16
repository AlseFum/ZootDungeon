package com.zootdungeon.actors.entities.mines;

import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.tweeners.ScaleTweener;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.PointF;

/**
 * 地雷族的视觉基类，定义标准动画状态机。
 *
 * <h3>动画状态</h3>
 * <pre>
 *   place() → idle() ←→ disarm()
 *                ↓
 *           detonate() → kill()
 * </pre>
 *
 * <h3>子类职责</h3>
 * 子类需在构造器中完成以下工作：
 * <ol>
 *   <li>调用 {@link #texture(String)} 设置贴图</li>
 *   <li>构造 {@link TextureFilm} 并用其初始化四个动画：
 *       {@link #idle}、{@link #place}、{@link #disarm}、{@link #detonate}</li>
 *   <li>调用 {@link #hardlight(int)} 设置常态着色</li>
 * </ol>
 *
 * <h3>Camera shake</h3>
 * {@link #detonate()} 默认带屏幕震动，幅度由 {@link #shakeMagnitude()} 控制。
 * 若子类不希望震动（如感应雷的闪光效果），可覆盖返回 0。
 */
public class MineSprite extends com.zootdungeon.actors.entities.CellEntitySprite implements com.watabou.noosa.MovieClip.Listener {

    // ===== 动画定义 =====

    /** 待机动画。子类覆盖。 */
    protected Animation idle;

    /** 放置动画。子类覆盖。 */
    protected Animation place;

    /** 解除动画。子类覆盖。 */
    protected Animation disarm;

    /** 引爆动画。子类覆盖。 */
    protected Animation detonate;

    // ===== 状态 =====

    protected enum State {
        IDLE, PLACING, DISARMING, DETONATING
    }

    protected State state = State.IDLE;

    /** 动画进行中禁止切换。 */
    protected boolean locked = false;

    // ===== 构造 =====

    public MineSprite() {
        super();
        listener = this;
    }

    // ===== 动画播放方法 =====

    /** 播放待机动画。循环。 */
    public void idle() {
        if (locked) return;
        if (state == State.IDLE || state == State.PLACING) return;
        if (idle != null) play(idle);
        state = State.IDLE;
    }

    /** 播放放置动画。动画完成后切换到待机。 */
    public void place() {
        if (locked) return;
        if (state == State.PLACING) return;
        if (place != null) play(place);
        state = State.PLACING;
    }

    /** 播放解除动画。动画完成后移除 sprite。 */
    public void disarm() {
        if (locked) return;
        state = State.DISARMING;
        locked = true;
        if (disarm != null) {
            play(disarm);
        } else {
            playDisarmFallback();
        }
    }

    /** 播放引爆动画。动画完成后移除 sprite 并触发屏幕震动。 */
    public void detonate() {
        if (state == State.DETONATING) return;
        state = State.DETONATING;
        locked = true;

        float shakeMag = shakeMagnitude();
        if (shakeMag > 0 && camera() != null) {
            camera().shake(shakeMag, detonateDuration() * 1.5f);
        }

        if (detonate != null) {
            play(detonate);
        } else {
            playDetonateFallback();
        }
    }

    // ===== MovieClip.Listener =====

    @Override
    public void onComplete(Animation anim) {
        if (anim == place) {
            state = State.IDLE;
            if (idle != null) play(idle);
        } else if (anim == disarm) {
            killAndErase();
        } else if (anim == detonate) {
            killAndErase();
        }
    }

    // ===== 降级动画（Tweener 模拟，无帧图时使用） =====

    protected void playDisarmFallback() {
        if (parent == null) {
            killAndErase();
            return;
        }
        float duration = disarmDuration();
        parent.add(new ScaleTweener(this, new PointF(0, 0), duration) {
            @Override
            protected void onComplete() {
                killAndErase();
            }
        });
        parent.add(new AlphaTweener(this, 0f, duration));
    }

    protected void playDetonateFallback() {
        if (parent == null) {
            killAndErase();
            return;
        }
        float duration = detonateDuration();
        float scaleTo = detonateScaleTo();
        int flashColor = detonateColor();

        parent.add(new ScaleTweener(this, new PointF(scaleTo, scaleTo), duration * 0.4f));

        if (flashColor != 0) {
            tint(flashColor, 1f);
            parent.add(new Delayer(0.05f) {
                @Override
                public void onComplete() {
                    resetColor();
                }
            });
        }

        parent.add(new Delayer(duration) {
            @Override
            public void onComplete() {
                killAndErase();
            }
        });
    }

    // ===== 子类可覆盖的参数 =====

    /** 引爆时屏幕震动幅度。0 表示不震动。默认 6f。 */
    protected float shakeMagnitude() {
        return 6f;
    }

    /** 引爆动画持续时间。默认 0.25f。 */
    protected float detonateDuration() {
        return 0.25f;
    }

    /** 引爆动画目标缩放。默认 2.5f。 */
    protected float detonateScaleTo() {
        return 2.5f;
    }

    /** 引爆动画颜色叠加。0 表示不叠加。默认 0xFFFFFF（白色闪白）。 */
    protected int detonateColor() {
        return 0xFFFFFF;
    }

    /** 解除动画持续时间。默认 0.3f。 */
    protected float disarmDuration() {
        return 0.3f;
    }

    /** 常态着色值。子类覆盖以改变默认着色（构造函数中调用 hardlight 生效）。默认 0xFFFFFF。 */
    protected int baseColor() {
        return 0xFFFFFF;
    }
}
