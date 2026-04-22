package com.zootdungeon.actors.buffs;

import com.watabou.utils.Bundle;
import com.zootdungeon.actors.Char;
import com.zootdungeon.messages.Messages;

/**
 * 可叠层触发型 buff 框架。
 * <p>
 * 语义：
 * <ul>
 *   <li>每次通过 {@link #tryAddStacks(int)} 加入叠层；</li>
 *   <li>当累计叠层 {@code >= threshold()} 时，对附着单位调用 {@link #onTrigger(Char)}，
 *       随后把叠层清零并在同一单位身上附着 {@link #cooldownBuffClass()} 指定的
 *       {@link TriggerCooldownBuff}（持续 {@link #cooldownDuration()} 回合）；</li>
 *   <li>在配对的 cooldown buff 仍然存在时，{@link #tryAddStacks(int)} 直接被拒绝，
 *       叠层也不会增长。</li>
 * </ul>
 * <p>
 * 默认 {@link #act()} 立刻把自己 {@code deactivate()} —— 本类不依赖时间流逝，
 * 只作为附着在角色身上的计数容器。子类若要做叠层衰减等时间逻辑，请覆写 {@code act()}
 * 自行处理。
 */
public abstract class StackingTriggerBuff extends Buff {

    /** 当前累计叠层。对外只读，通过 {@link #tryAddStacks(int)} / {@link #setStacks(int)} 写入。 */
    protected int stacks = 0;

    /** 触发所需叠层数。 */
    public abstract int threshold();

    /** 触发后将附着在目标上的冷却 buff 类型。 */
    public abstract Class<? extends TriggerCooldownBuff> cooldownBuffClass();

    /** 冷却 buff 的持续回合数。默认 5，子类按需覆写。 */
    public float cooldownDuration() {
        return 5f;
    }

    /**
     * 叠层达到阈值时，对附着单位造成的效果。由子类实现。
     * 在本函数返回后，{@link #tryAddStacks(int)} 会自动清空叠层并附加冷却 buff。
     */
    public abstract void onTrigger(Char target);

    /** 因处于冷却中而被拒绝叠层时的回调。默认 no-op，子类可覆写用于提示 / 日志。 */
    protected void onBlockedByCooldown(int blockedAmount) {
    }

    // ==== 外部 API ====

    /**
     * 尝试增加叠层。
     *
     * @param amount 要增加的层数（>= 1 才会生效）。
     * @return 真正叠上的层数。若处于冷却中返回 0。
     */
    public final int tryAddStacks(int amount) {
        if (amount <= 0 || target == null) {
            return 0;
        }
        if (isOnCooldown()) {
            onBlockedByCooldown(amount);
            return 0;
        }

        stacks += amount;
        int applied = amount;

        if (stacks >= threshold()) {
            Char keptTarget = target;
            try {
                onTrigger(keptTarget);
            } finally {
                stacks = 0;
                applyCooldown(keptTarget);
            }
        }

        return applied;
    }

    /** 当前是否处于冷却（配对 {@link TriggerCooldownBuff} 尚存在）。 */
    public final boolean isOnCooldown() {
        return target != null && target.buff(cooldownBuffClass()) != null;
    }

    /** 读取当前叠层。 */
    public final int stacks() {
        return stacks;
    }

    /** 直接写入叠层（如需从存档恢复到特定值、或由子类调试时使用），不会触发。 */
    public final void setStacks(int value) {
        stacks = Math.max(0, value);
    }

    /** 强制清空叠层（不触发、不施加冷却）。 */
    public final void clearStacks() {
        stacks = 0;
    }

    // ==== 内部逻辑 ====

    private void applyCooldown(Char t) {
        if (t == null) return;
        Class<? extends TriggerCooldownBuff> cd = cooldownBuffClass();
        if (cd == null) return;
        float dur = Math.max(0f, cooldownDuration());
        if (dur <= 0f) return;
        Buff.affect(t, cd, dur);
    }

    // ==== Actor ====

    /**
     * 默认不随时间演化：附着后立刻 deactivate，只作为状态容器存在，
     * 直到被 {@link #tryAddStacks(int)} 驱动或被外部 {@link #detach()}。
     */
    @Override
    public boolean act() {
        deactivate();
        return true;
    }

    // ==== UI 默认实现 ====

    @Override
    public String iconTextDisplay() {
        return Integer.toString(stacks);
    }

    @Override
    public float iconFadePercent() {
        int t = threshold();
        if (t <= 0) return 0f;
        return Math.max(0f, 1f - (stacks / (float) t));
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", stacks, threshold(), dispTurns(cooldownDuration()));
    }

    // ==== Bundle ====

    private static final String STACKS = "stacks";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STACKS, stacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        stacks = bundle.getInt(STACKS);
    }
}
