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
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
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

    public enum Mode { MELEE, RANGED }

    {
        image = ItemSpriteSheet.SWORD;
        tier = 3;
        bones = false;
    }

    private Mode mode = Mode.MELEE;
    private int grenadeCooldown = 0;
    private boolean aimingPhase;
    private int grenadeTargetCell = -1;
    private static final int GRENADE_COOLDOWN_TURNS = 8;

    public static final String AC_SWITCH = "SWITCH";

    public Mode mode() { return mode; }
    public void setMode(Mode m) { mode = m; }

    public void tickCooldown() {
        if (grenadeCooldown > 0) grenadeCooldown--;
    }

    public boolean canFireRanged() {
        return mode == Mode.RANGED && grenadeCooldown <= 0;
    }

    /** 上一回合已瞄准，本回合应发射 */
    public boolean isReleaseTurn() {
        return grenadeTargetCell != -1 && !aimingPhase;
    }

    public int getGrenadeTargetCell() { return grenadeTargetCell; }

    /** 发射后清除目标并进入冷却（供无动画时调用，或由 onZapComplete 内部使用） */
    public void clearGrenadeState() {
        grenadeTargetCell = -1;
        grenadeCooldown = GRENADE_COOLDOWN_TURNS;
    }

    /** 无动画时结束瞄准阶段 */
    public void finishAimPhase() { aimingPhase = false; }

    /** 开始瞄准（下一回合发射） */
    public void startAim(Char owner, int cell) {
        grenadeTargetCell = cell;
        aimingPhase = true;
    }

    /** zap 动画结束时由持有者调用；若为瞄准结束则只结束回合，若为发射则造成 AOE 并进入冷却 */
    public void onZapComplete(Char owner) {
        if (aimingPhase) {
            aimingPhase = false;
            return;
        }
        if (grenadeTargetCell != -1) {
            Sample.INSTANCE.play(Assets.Sounds.BLAST);
            doGrenadeAt(owner, grenadeTargetCell);
            grenadeTargetCell = -1;
            grenadeCooldown = GRENADE_COOLDOWN_TURNS;
        }
    }

    public void doGrenadeAt(Char owner, int cell) {
        PathFinder.buildDistanceMap(cell, BArray.not(Dungeon.level.solid, null), 2);
        int dmg = Random.NormalIntRange(8 + Dungeon.scalingDepth(), 14 + Dungeon.scalingDepth() * 2);
        for (int i = 0; i < PathFinder.distance.length; i++) {
            if (PathFinder.distance[i] == Integer.MAX_VALUE) continue;
            Char ch = Actor.findChar(i);
            if (ch != null && ch != owner) {
                int actualDmg = dmg - ch.drRoll();
                if (actualDmg > 0) {
                    ch.damage(actualDmg, owner);
                    Buff.affect(ch, ShatterDebuff.class, ShatterDebuff.DURATION);
                }
            }
        }
        CellEmitter.center(cell).burst(BlastParticle.FACTORY, 12);
        GameScene.flash(0xFF6600);
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
        return mode == Mode.RANGED ? "碎颅者(远程)" : "碎颅者(近程)";
    }

    @Override
    public String desc() {
        if (mode == Mode.RANGED) {
            return "可切换近程/远程。远程模式：瞄准一回合后发射榴弹，对落点周围造成范围伤害，被命中的单位短时间内防御减半，冷却 " + GRENADE_COOLDOWN_TURNS + " 回合。";
        }
        return "可切换近程/远程。近程模式：普通近战攻击。";
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) actions.add(AC_SWITCH);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SWITCH)) {
            mode = mode == Mode.MELEE ? Mode.RANGED : Mode.MELEE;
            GLog.p(mode == Mode.RANGED ? "切换为远程模式" : "切换为近程模式");
            updateQuickslot();
        } else {
            super.execute(hero, action);
        }
    }

    private static final String MODE = "mode";
    private static final String GRENADE_CD = "grenade_cd";
    private static final String GRENADE_TARGET = "grenade_target";
    private static final String AIMING_PHASE = "aiming_phase";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(MODE, mode);
        bundle.put(GRENADE_CD, grenadeCooldown);
        bundle.put(GRENADE_TARGET, grenadeTargetCell);
        bundle.put(AIMING_PHASE, aimingPhase);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(MODE)) mode = bundle.getEnum(MODE, Mode.class);
        grenadeCooldown = bundle.getInt(GRENADE_CD);
        grenadeTargetCell = bundle.getInt(GRENADE_TARGET);
        aimingPhase = bundle.getBoolean(AIMING_PHASE);
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
}
