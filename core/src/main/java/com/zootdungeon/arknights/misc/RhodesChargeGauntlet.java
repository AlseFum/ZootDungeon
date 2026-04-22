package com.zootdungeon.arknights.misc;

import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.weapon.fastWeapon.FastWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 罗德岛·蓄能拳套：
 *  - 每一次攻击、每一次成功闪避，都会为武器累积 1 层“蓄能”。
 *  - 层数无上限、不会自然衰减，仅在主动消耗时清零。
 *  - 任何职业都可以通过道具主动技能（{@link #AC_RELEASE}）手动释放蓄能；
 *    决斗家则额外通过 {@link #duelistAbility(Hero, Integer)} 使用，两者共用
 *    {@link #releaseCharges(Hero, Char)} 这条核心结算路径。
 */
public class RhodesChargeGauntlet extends FastWeapon {

    public static final String AC_RELEASE = "RELEASE";

    {
        image = ItemSpriteSheet.GAUNTLETS;
        hitSound = Assets.Sounds.HIT;
        hitSoundPitch = 1.2f;

        tier = 0;
        DLY = 0.5f;

        // 装备后快捷栏点击默认触发“释放蓄能”
        defaultAction = AC_RELEASE;
    }

    /** 每层提供的倍率加成：0.10 + tier*0.01 + lvl*0.005，上限 0.25/层。 */
    public float bonusPerStack() {
        return Math.min(0.25f, 0.10f + 0.01f * Math.max(0, tier - 1) + 0.005f * buffedLvl());
    }

    /** 攻击命中：+1 层。 */
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero) {
            Buff.affect((Hero) attacker, ChargeStacks.class).addStack();
        }
        return super.proc(attacker, defender, damage);
    }

    /** RhodesDodgeHooks 回调：闪避一次 +1 层。 */
    public void onHeroDodged(Hero hero) {
        if (hero == null || !hero.isAlive()) return;
        Buff.affect(hero, ChargeStacks.class).addStack();
    }

    // ==== 独立的释放函数 ====

    /**
     * 独立的「消耗全部蓄能发动一击」核心函数。
     * <p>负责：读取层数、清零 {@link ChargeStacks}、按层数放大倍率、执行一次保证命中的物理攻击、
     * 播放命中特效与状态文字、解除隐身。</p>
     * <p>不负责：sprite 攻击动画（由调用方 {@link Hero#sprite} 的 attack 回调驱动）、
     * {@link Hero#spendAndNext} 的行动时间结算、以及决斗家主动技能
     * {@link #beforeAbilityUsed(Hero, Char)} / {@link #afterAbilityUsed(Hero)} 钩子。</p>
     *
     * @return 本次实际消耗的层数；若目标无效或不可攻击则返回 0 且不消耗层数。
     */
    public int releaseCharges(Hero hero, Char enemy) {
        if (hero == null || enemy == null) return 0;
        if (!hero.isAlive() || !enemy.isAlive()) return 0;
        if (!hero.canAttack(enemy)) return 0;

        ChargeStacks stacks = hero.buff(ChargeStacks.class);
        int used = stacks == null ? 0 : stacks.count;
        if (stacks != null) stacks.detach();

        AttackIndicator.target(enemy);
        float mult = 1f + used * bonusPerStack();
        boolean hit = hero.attack(enemy, mult, 0, Char.INFINITE_ACCURACY);
        if (hit) {
            if (enemy.sprite != null) {
                enemy.sprite.showStatus(CharSprite.NEGATIVE,
                        Messages.get(RhodesChargeGauntlet.class, "release_status", used));
                enemy.sprite.flash();
            }
            Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
        }
        Invisibility.dispel();
        return used;
    }

    // ==== 道具主动动作（AC_RELEASE）====

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            actions.add(AC_RELEASE);
        }
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (AC_RELEASE.equals(action)) {
            if (!isEquipped(hero)) {
                GLog.w(Messages.get(this, "release_need_equip"));
                usesTargeting = false;
                return;
            }
            ChargeStacks stacks = hero.buff(ChargeStacks.class);
            if (stacks == null || stacks.count <= 0) {
                GLog.w(Messages.get(this, "release_no_charge"));
                usesTargeting = false;
                return;
            }
            curUser = hero;
            usesTargeting = true;
            GameScene.selectCell(releaseTarget);
        }
    }

    /** 选目标的 listener：拾取到相邻敌人即调用 {@link #releaseCharges(Hero, Char)}。 */
    private final CellSelector.Listener releaseTarget = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target == null) return;
            final Hero hero = curUser;
            if (hero == null) return;

            final Char enemy = Actor.findChar(target);
            if (enemy == null || enemy == hero || !Dungeon.level.heroFOV[target]) {
                GLog.w(Messages.get(RhodesChargeGauntlet.class, "release_no_target"));
                return;
            }
            if (!hero.canAttack(enemy)) {
                GLog.w(Messages.get(RhodesChargeGauntlet.class, "release_out_of_range"));
                return;
            }

            hero.sprite.attack(enemy.pos, new Callback() {
                @Override
                public void call() {
                    releaseCharges(hero, enemy);
                    hero.spendAndNext(hero.attackDelay());
                }
            });
        }

        @Override
        public String prompt() {
            return Messages.get(RhodesChargeGauntlet.class, "release_prompt");
        }
    };

    // ==== 决斗家主动技能（复用 releaseCharges）====

    @Override
    protected void duelistAbility(final Hero hero, Integer target) {
        if (target == null) {
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }
        final Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }
        hero.belongings.abilityWeapon = this;
        if (!hero.canAttack(enemy)) {
            GLog.w(Messages.get(this, "ability_target_range"));
            hero.belongings.abilityWeapon = null;
            return;
        }
        hero.belongings.abilityWeapon = null;

        hero.sprite.attack(enemy.pos, new Callback() {
            @Override
            public void call() {
                beforeAbilityUsed(hero, enemy);
                releaseCharges(hero, enemy);
                if (!enemy.isAlive()) onAbilityKill(hero, enemy);
                hero.spendAndNext(hero.attackDelay());
                afterAbilityUsed(hero);
            }
        });
    }

    @Override
    public String abilityInfo() {
        int pctPerStack = Math.round(bonusPerStack() * 100f);
        if (levelKnown) {
            return Messages.get(this, "ability_desc", pctPerStack);
        } else {
            return Messages.get(this, "typical_ability_desc");
        }
    }

    @Override
    public String upgradeAbilityStat(int level) {
        float base = 0.10f + 0.01f * Math.max(0, tier - 1) + 0.005f * level;
        int pct = Math.round(Math.min(0.25f, base) * 100f);
        return Messages.get(this, "upgrade_ability_stat", pct);
    }

    @Override
    public String desc() {
        int pct = Math.round(bonusPerStack() * 100f);
        return Messages.get(this, "desc", pct);
    }

    // ==== 蓄能层数 Buff ====

    public static class ChargeStacks extends Buff {
        {
            type = buffType.POSITIVE;
            revivePersists = true;
        }

        public int count = 0;

        public void addStack() {
            count++;
            BuffIndicator.refreshHero();
        }

        @Override
        public boolean act() {
            // 不衰减，但仍要“占用”一个 act 周期避免饿死调度
            spend(TICK);
            return true;
        }

        @Override
        public int icon() {
            KindOfWeapon w1 = Dungeon.hero.belongings.weapon();
            KindOfWeapon w2 = Dungeon.hero.belongings.secondWep();
            if (w1 instanceof RhodesChargeGauntlet || w2 instanceof RhodesChargeGauntlet) {
                return BuffIndicator.COMBO;
            }
            return BuffIndicator.NONE;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.2f, 0.8f, 1.0f);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(count);
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", count);
        }

        private static final String COUNT = "rhodes_charge_count";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(COUNT, count);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            count = bundle.getInt(COUNT);
        }
    }
}
