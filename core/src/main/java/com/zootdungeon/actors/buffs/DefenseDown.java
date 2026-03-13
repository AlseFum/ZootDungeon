package com.zootdungeon.actors.buffs;

import com.zootdungeon.ui.BuffIndicator;

/**
 * 独立减防 debuff：使 defenseSkill 减半，可被碎颅者榴弹等任意来源施加。
 */
public class DefenseDown extends FlavourBuff {

    public static final float DURATION = 6f;

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    @Override
    public int icon() {
        return BuffIndicator.VULNERABLE;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
