package com.zootdungeon.arknights.MainTheme;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.DefenseDown;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Vulnerable;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.utils.Select;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 可切换模式：近程 = 近战，远程 = 按冷却发射榴弹（瞄准一回合后发射）。
 */
public class SkullShattererWeapon extends MeleeWeapon {

    static {
        // gunweapon.png 每行 14 格，第二行第10列 = index 1*14+9 = 23
        SpriteRegistry.texture("sheet.cola.gunweapon", "cola/gunweapon.png")
                .grid(16, 16)
                .span(23).label("shatteredweapon");
    }

    public enum Mode { MELEE, RANGED }

    {
        image = SpriteRegistry.byLabel("shatteredweapon");
        tier = 3;
        bones = false;
        usesTargeting = true;
    }

    private Mode mode = Mode.MELEE;
    private int grenadeCooldown = 0;
    private static final int GRENADE_COOLDOWN_TURNS = 8;

    public static final String AC_SWITCH = "SWITCH";
    public static final String AC_GRENADE = "GRENADE";

    public Mode mode() { return mode; }
    public void setMode(Mode m) { mode = m; }

    public void tickCooldown() {
        if (grenadeCooldown > 0) grenadeCooldown--;
    }

    public boolean canFireRanged() {
        return mode == Mode.RANGED && grenadeCooldown <= 0;
    }

    public void clearGrenadeState() {
        grenadeCooldown = GRENADE_COOLDOWN_TURNS;
    }

    public void doGrenadeAt(Char owner, int cell) {
        PathFinder.buildDistanceMap(cell, BArray.not(Dungeon.level.solid, null), 2);
        int sd = Dungeon.scalingDepth();
        int dmg = Random.NormalIntRange(6 + sd, 11 + sd + sd / 2);
        for (Char ch : Select.chars().all()
                .at(Select.place().all().that(i -> PathFinder.distance[i] != Integer.MAX_VALUE))
                .except(Select.chars().of(owner))
                .query()) {
            int actualDmg = dmg - ch.drRoll();
            if (actualDmg > 0) ch.damage(actualDmg, owner);
            Buff.affect(ch, ShatterDebuff.class, ShatterDebuff.DURATION);
        }
        CellEmitter.center(cell).burst(BlastParticle.FACTORY, 12);
    }

    private static SkullShattererWeapon weaponOf(Hero h) {
        KindOfWeapon w = h.belongings.weapon();
        if (w instanceof SkullShattererWeapon) return (SkullShattererWeapon) w;
        w = h.belongings.secondWep();
        return w instanceof SkullShattererWeapon ? (SkullShattererWeapon) w : null;
    }

    @Override
    public int STRReq(int lvl) {
        return 10;
    }

    @Override
    public int min(int lvl) {
        return 2 * (tier + 1) + lvl * tier;
    }

    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * tier;
    }

    @Override
    public String name() {
        return mode == Mode.RANGED
                ? Messages.get(this, "name_ranged")
                : Messages.get(this, "name_melee");
    }

    @Override
    public String desc() {
        if (mode == Mode.RANGED) {
            return Messages.get(this, "desc_ranged", GRENADE_COOLDOWN_TURNS);
        }
        return Messages.get(this, "desc_melee");
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            actions.add(AC_SWITCH);
            if (mode == Mode.RANGED) actions.add(AC_GRENADE);
        }
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SWITCH)) return Messages.get(this, "ac_switch");
        if (action.equals(AC_GRENADE)) return Messages.get(this, "ac_grenade");
        return super.actionName(action, hero);
    }

    @Override
    public String defaultAction() {
        if (Dungeon.hero != null && isEquipped(Dungeon.hero) && mode == Mode.RANGED) return AC_GRENADE;
        return super.defaultAction();
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_SWITCH)) {
            mode = mode == Mode.MELEE ? Mode.RANGED : Mode.MELEE;
            GLog.p(mode == Mode.RANGED
                    ? Messages.get(this, "msg_switch_ranged")
                    : Messages.get(this, "msg_switch_melee"));
            updateQuickslot();
            return;
        }
        if (action.equals(AC_GRENADE)) {
            if (!isEquipped(hero) || mode != Mode.RANGED) return;
            if (STRReq() > hero.STR()) {
                GLog.w(Messages.get(MeleeWeapon.class, "ability_low_str"));
                return;
            }
            if (!canFireRanged()) {
                GLog.w(Messages.get(this, "msg_grenade_cd", grenadeCooldown));
                return;
            }
            GameScene.selectCell(grenadeAimSelector);
            return;
        }
    }

    private final CellSelector.Listener grenadeAimSelector = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer cell) {
            if (cell != null && Dungeon.hero != null) {
                SkullShattererWeapon w = weaponOf(Dungeon.hero);
                if (w != null && w.canFireRanged()) {
                    ((GrenadeLaunchBuff) Buff.affect(Dungeon.hero, GrenadeLaunchBuff.class, 1f)).setCell(cell);
                    GLog.p(Messages.get(SkullShattererWeapon.this, "msg_aim_done"));
                    Dungeon.hero.spendAndNext(1f);
                    Item.updateQuickslot();
                }
            }
        }

        @Override
        public String prompt() {
            return Messages.get(SkullShattererWeapon.this, "prompt_aim");
        }
    };

    private static final String MODE = "mode";
    private static final String GRENADE_CD = "grenade_cd";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(MODE, mode);
        bundle.put(GRENADE_CD, grenadeCooldown);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(MODE)) mode = bundle.getEnum(MODE, Mode.class);
        grenadeCooldown = bundle.getInt(GRENADE_CD);
    }

    public static class ShatterDebuff extends FlavourBuff {
        public static final float DURATION = DefenseDown.DURATION;
        private static final Set<Class<? extends Buff>> CHILDREN =
                Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                        Vulnerable.class,
                        DefenseDown.class
                )));

        {
            type = buffType.NEGATIVE;
            announced = true;
        }

        @Override
        public Set<Class<? extends Buff>> reifiesTo() {
            return CHILDREN;
        }

        @Override
        public int icon() {
            return BuffIndicator.VULNERABLE;
        }

        @Override
        public float iconFadePercent() {
            return Math.max(0, (DURATION - visualcooldown()) / DURATION);
        }

        @Override
        public String name() {
            return "碎甲";
        }

        @Override
        public String desc() {
            return "目标陷入碎甲状态：更容易受到伤害，且护甲骰减半。";
        }
    }

    /** 瞄准后挂上，持续 1 回合，结束时自动发射榴弹。需比英雄先行动，否则同时间下英雄会先 ready() 导致卡顿。 */
    public static class GrenadeLaunchBuff extends FlavourBuff {
        {
            actPriority = Actor.HERO_PRIO + 1;
        }

        private int targetCell = -1;

        public GrenadeLaunchBuff setCell(int cell) {
            targetCell = cell;
            return this;
        }

        @Override
        public boolean act() {
            if (cooldown() > 0) {
                spend(Actor.TICK);
                return true;
            }
            if (target instanceof Hero && targetCell >= 0) {
                Hero h = (Hero) target;
                SkullShattererWeapon sw = weaponOf(h);
                if (sw != null) {
                    Sample.INSTANCE.play(Assets.Sounds.BLAST);
                    sw.doGrenadeAt(h, targetCell);
                    sw.clearGrenadeState();
                    Item.updateQuickslot();
                    h.spend(Actor.TICK);
                }
            }
            detach();
            return true;
        }

        @Override
        public int icon() {
            return BuffIndicator.NONE;
        }

        private static final String CELL = "cell";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(CELL, targetCell);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            targetCell = bundle.getInt(CELL);
        }
    }

    /** 装备碎颅者时挂在英雄身上，每回合只 tick 武器冷却，卸下时移除。 */
    public static class GrenadeCooldownBuff extends Buff {
        {
            actPriority = BUFF_PRIO;
        }

        @Override
        public boolean act() {
            if (target instanceof Hero) {
                SkullShattererWeapon w = weaponOf((Hero) target);
                if (w != null) w.tickCooldown();
            }
            spend(Actor.TICK);
            return true;
        }

        @Override
        public int icon() {
            return BuffIndicator.NONE;
        }
    }

    @Override
    public boolean doEquip(Hero hero) {
        if (super.doEquip(hero)) {
            Buff.affect(hero, GrenadeCooldownBuff.class);
            return true;
        }
        return false;
    }

    @Override
    public boolean equipSecondary(Hero hero) {
        if (super.equipSecondary(hero)) {
            Buff.affect(hero, GrenadeCooldownBuff.class);
            return true;
        }
        return false;
    }

    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        Buff.detach(hero, GrenadeCooldownBuff.class);
        return super.doUnequip(hero, collect, single);
    }
}
