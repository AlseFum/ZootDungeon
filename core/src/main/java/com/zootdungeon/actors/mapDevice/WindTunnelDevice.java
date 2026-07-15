package com.zootdungeon.actors.mapDevice;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.ItemEffects;
import com.zootdungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;

/**
 * 定向吹风装置：具有方向，周期性将范围内的单位朝指定方向推送。
 *
 * <ul>
 *   <li>方向存储为 0–3 整数，表示：0=北, 1=东, 2=南, 3=西。</li>
 *   <li>每 {@link #INTERVAL} 回合脉冲一次，对范围内所有可移动单位执行
 *       {@link ItemEffects#knockback}，推送距离 = {@link #PUSH_POWER}。</li>
 *   <li>推送轨迹由 {@link Ballistica} 从目标位置沿装置方向延伸计算。</li>
 *   <li>通过 {@link #setDirection(int)} 可在运行时切换方向。</li>
 * </ul>
 */
public class WindTunnelDevice extends MapDevice {

    public static final int DIR_NORTH = 0;
    public static final int DIR_EAST = 1;
    public static final int DIR_SOUTH = 2;
    public static final int DIR_WEST = 3;

    public static final String[] DIR_NAMES = {"North", "East", "South", "West"};

    private static final int RADIUS = 4;
    private static final int INTERVAL = 5;
    private static final int PUSH_POWER = 3;

    private int direction = DIR_NORTH;
    private int cooldown = 0;

    {
        spriteClass = MapDevice.Sprite.class;
        HP = HT = 20;
        properties.add(Property.IMMOVABLE);
        properties.add(Property.INORGANIC);
        properties.add(Property.STATIC);
    }

    public WindTunnelDevice() {
        super();
    }

    /** 获取当前方向 (0=北, 1=东, 2=南, 3=西) */
    public int getDirection() {
        return direction;
    }

    /** 设置方向 (0=北, 1=东, 2=南, 3=西)，循环取模 */
    public void setDirection(int dir) {
        direction = dir & 3; // 等价于 dir % 4
    }

    /** 方向循环：北→东→南→西→北 */
    public void cycleDirection() {
        direction = (direction + 1) & 3;
    }

    @Override
    protected boolean act() {
        if (cooldown <= 0) {
            pushUnits();
            cooldown = INTERVAL;
        } else {
            cooldown--;
        }
        spend(TICK);
        return true;
    }

    private void pushUnits() {
        int dx = dirToDx(direction);
        int dy = dirToDy(direction);

        for (Char ch : Actor.chars()) {
            if (ch == this || !ch.isAlive()) continue;
            if (Dungeon.level.distance(pos, ch.pos) > RADIUS) continue;
            if (!isInPushDirection(ch, dx, dy)) continue;

            // 从目标位置沿装置方向延伸 PUSH_POWER+1 格作为 Ballistica 终点
            int chX = ch.pos % Dungeon.level.width();
            int chY = ch.pos / Dungeon.level.width();
            int targetX = chX + dx * (PUSH_POWER + 1);
            int targetY = chY + dy * (PUSH_POWER + 1);
            targetX = Math.max(0, Math.min(Dungeon.level.width() - 1, targetX));
            targetY = Math.max(0, Math.min(Dungeon.level.height() - 1, targetY));
            int targetCell = targetX + targetY * Dungeon.level.width();

            Ballistica trajectory = new Ballistica(ch.pos, targetCell, Ballistica.PROJECTILE);
            ItemEffects.knockback(ch, trajectory, PUSH_POWER, false, false, this);
        }
    }

    /** 检查单位是否在装置的推送方向（锥形区，约 90° 开口） */
    private boolean isInPushDirection(Char ch, int dx, int dy) {
        int dxc = (ch.pos % Dungeon.level.width()) - (pos % Dungeon.level.width());
        int dyc = (ch.pos / Dungeon.level.width()) - (pos / Dungeon.level.width());
        // 对于纯四方向，锥形判断：在方向轴上的投影绝对值要大于垂直轴
        if (dx != 0) return dxc * dx > 0 && Math.abs(dxc) >= Math.abs(dyc);
        if (dy != 0) return dyc * dy > 0 && Math.abs(dyc) >= Math.abs(dxc);
        return false;
    }

    private static int dirToDx(int dir) {
        switch (dir) {
            case DIR_EAST:  return 1;
            case DIR_WEST:  return -1;
            default:        return 0;
        }
    }

    private static int dirToDy(int dir) {
        switch (dir) {
            case DIR_SOUTH: return 1;
            case DIR_NORTH: return -1;
            default:        return 0;
        }
    }

    @Override
    public void receiveDamage(Object src) {
        // 无特殊反应
    }

    private static final String DIRECTION = "direction";
    private static final String COOLDOWN = "cooldown";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(DIRECTION, direction);
        bundle.put(COOLDOWN, cooldown);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        direction = bundle.getInt(DIRECTION);
        cooldown = bundle.getInt(COOLDOWN);
    }
}
