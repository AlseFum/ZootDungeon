package com.zootdungeon.actors.buffs;

import com.zootdungeon.messages.Messages;

/**
 * {@link StackingTriggerBuff} 对应的「冷却」侧：一个纯时间型 {@link FlavourBuff}。
 * <p>
 * 语义：当 StackingTriggerBuff 触发效果后会清空叠层，然后把对应的
 * {@code TriggerCooldownBuff} 附着到同一单位上。只要该冷却 buff 仍在，
 * 配对的 {@link StackingTriggerBuff#tryAddStacks(int)} 就不会再让叠层增长。
 * <p>
 * 本类只是提供一个带默认 {@code desc()} / icon 文本的基类；子类通常
 * 只需要在类上加 {@code icon()} 覆写、在 {@code .properties} 里加 name/desc。
 */
public abstract class TriggerCooldownBuff extends FlavourBuff {

    {
        type = buffType.NEUTRAL;
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", dispTurns());
    }
}
