package com.zootdungeon.actors;

import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundle;
/**
 * 调试用的最小 MapDevice 占位。
 *
 * 用于在没有真实子类时验证 MapDevice 行为：
 * - 阻挡寻路（extends Char 的占格语义）
 * - damage 链会走 {@link #receiveDamage(Object)}，HP 不变
 * - act 期间每回合自增 charge
 * - 正常流程不死（HP 永远 1）
 */
public class DebugDummyMapDevice extends MapDevice {

    {
        spriteClass = MapDevice.Sprite.class;
    }

    public DebugDummyMapDevice() {
        super();
    }

    private int charge = 0;

    public int getCharge() {
        return charge;
    }
    @Override
	public void restoreFromBundle( Bundle bundle ) {
		
		super.restoreFromBundle( bundle );
        int restoreCharge=bundle.getInt("charge");
        if (restoreCharge > 0) {
            charge+=restoreCharge;
        }
	}
    @Override
	public void storeInBundle( Bundle bundle ) {
		
		super.storeInBundle( bundle );
        bundle.put("charge", charge);
	}
    @Override
    public void receiveDamage(Object src) {
        charge++;
        GLog.i("DebugDummyMapDevice hit by " + (src == null ? "null" : src.getClass().getSimpleName())
                + " -> charge=" + charge);
    }
}
