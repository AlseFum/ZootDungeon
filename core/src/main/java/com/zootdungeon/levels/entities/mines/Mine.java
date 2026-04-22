package com.zootdungeon.levels.entities.mines;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.levels.entities.CellEntity;

/**
 * 地雷族 {@link CellEntity} 的共同基类。
 *
 * <h3>约定</h3>
 * <ul>
 *   <li><b>阵营</b>：所有地雷都视为玩家阵营布置的道具，因此只有
 *       {@link Char.Alignment#ENEMY} 的 {@link Char} 才能触发（通过
 *       {@link #isEnemy(Char)} 判断）。这样即使英雄/宠物踩到也不会自爆。</li>
 *   <li><b>单次触发</b>：{@link #detonate()} 自带 {@link #detonated} 幂等保护，
 *       被连锁引爆、同回合多源触发时不会重复结算。</li>
 *   <li><b>视觉 / 音效</b>：默认播放 {@link WandOfBlastWave.BlastWave}
 *       + {@link Assets.Sounds#BLAST}，子类不需要再重复播放。</li>
 *   <li><b>移除</b>：{@link #detonate()} 结算完毕后自动
 *       {@link #despawn()}，子类只需实现 {@link #onDetonate()}。</li>
 * </ul>
 */
public abstract class Mine extends CellEntity {

    /** 已经被引爆（避免连锁/并发导致的重复结算）。持久化以便存读档时保持一致。 */
    protected boolean detonated = false;

    /**
     * 核心入口：触发爆炸。外部（踩中、连锁引爆、远程触发）统一调用这个方法。
     * 重复调用是安全 no-op。
     */
    public final void detonate() {
        if (detonated) {
            return;
        }
        detonated = true;
        playBlastVisual();
        onDetonate();
        despawn();
    }

    /** 子类实现：地雷的具体效果。调用时 {@link #pos} 仍指向爆炸中心。 */
    protected abstract void onDetonate();

    /**
     * 爆炸视觉。默认使用 {@link WandOfBlastWave.BlastWave#blast(int, float)}
     * + 低音量爆炸音效。子类可覆盖以调整半径或禁用（返回前 super 不要调用）。
     */
    protected void playBlastVisual() {
        if (Dungeon.level == null) {
            return;
        }
        WandOfBlastWave.BlastWave.blast(pos, blastVisualRadius());
        Sample.INSTANCE.play(Assets.Sounds.BLAST);
    }

    /** {@link #playBlastVisual()} 用的特效半径（仅视觉，不影响实际伤害范围）。 */
    protected float blastVisualRadius() {
        return 2f;
    }

    /** 地雷只对「敌方」角色生效；英雄/盟友踩上去不触发。 */
    protected static boolean isEnemy(Char ch) {
        return ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY;
    }

    // ==== 持久化 ====

    private static final String DETONATED = "detonated";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(DETONATED, detonated);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        detonated = bundle.getBoolean(DETONATED);
    }
}
