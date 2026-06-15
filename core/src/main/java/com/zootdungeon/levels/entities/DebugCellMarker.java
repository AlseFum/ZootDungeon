package com.zootdungeon.levels.entities;

import com.watabou.utils.Bundle;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.levels.Level;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.utils.GLog;

/**
 * 调试用具体 {@link CellEntity}：放在地上显示一个垃圾桶贴图，
 * 每次有角色踩上或飞过都会向游戏日志打印一次事件，并累计计数。
 * <p>
 * 除了用于演示 {@link CellEntity} 的接入方式，也便于手动测试
 * {@link Level#occupyCell(Char)} 对地面实体的 trigger 流程。
 */
public class DebugCellMarker extends CellEntity {

    public int stepCount = 0;
    public int flyCount = 0;

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return DebugCellMarkerSprite.class;
    }

    @Override
    public void onSpawn(Level level) {
        super.onSpawn(level);
        if (sprite != null) {
            sprite.fadeIn();
        }
    }

    @Override
    public void onStep(Char who) {
        stepCount++;
        String whoName = (who != null) ? who.name() : "?";
        if (who instanceof Hero) {
            GLog.i(Messages.get(this, "step_hero", whoName, stepCount));
        } else {
            GLog.i(Messages.get(this, "step_other", whoName, stepCount));
        }
    }

    @Override
    public void onFlyOver(Char who) {
        flyCount++;
        String whoName = (who != null) ? who.name() : "?";
        GLog.i(Messages.get(this, "fly_over", whoName, flyCount));
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", stepCount, flyCount);
    }

    private static final String STEP_COUNT = "stepCount";
    private static final String FLY_COUNT = "flyCount";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STEP_COUNT, stepCount);
        bundle.put(FLY_COUNT, flyCount);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        stepCount = bundle.getInt(STEP_COUNT);
        flyCount = bundle.getInt(FLY_COUNT);
    }
}
