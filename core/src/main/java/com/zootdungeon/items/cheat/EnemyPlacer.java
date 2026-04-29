package com.zootdungeon.items.cheat;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.actors.mobs.Acidic;
import com.zootdungeon.actors.mobs.ArmoredBrute;
import com.zootdungeon.actors.mobs.Bandit;
import com.zootdungeon.actors.mobs.Bat;
import com.zootdungeon.actors.mobs.Bee;
import com.zootdungeon.actors.mobs.Brute;
import com.zootdungeon.actors.mobs.CausticSlime;
import com.zootdungeon.actors.mobs.Crab;
import com.zootdungeon.actors.mobs.CrystalMimic;
import com.zootdungeon.actors.mobs.CrystalWisp;
import com.zootdungeon.actors.mobs.Device;
import com.zootdungeon.actors.mobs.DM100;
import com.zootdungeon.actors.mobs.DM200;
import com.zootdungeon.actors.mobs.DM201;
import com.zootdungeon.actors.mobs.DM300;
import com.zootdungeon.actors.mobs.DwarfKing;
import com.zootdungeon.actors.mobs.EbonyMimic;
import com.zootdungeon.actors.mobs.Elemental;
import com.zootdungeon.actors.mobs.Eye;
import com.zootdungeon.actors.mobs.FetidRat;
import com.zootdungeon.actors.mobs.FungalCore;
import com.zootdungeon.actors.mobs.FungalSentry;
import com.zootdungeon.actors.mobs.Ghoul;
import com.zootdungeon.actors.mobs.Gnoll;
import com.zootdungeon.actors.mobs.GnollGeomancer;
import com.zootdungeon.actors.mobs.GnollGuard;
import com.zootdungeon.actors.mobs.GnollSapper;
import com.zootdungeon.actors.mobs.GnollTrickster;
import com.zootdungeon.actors.mobs.Golem;
import com.zootdungeon.actors.mobs.Goo;
import com.zootdungeon.actors.mobs.GoldenMimic;
import com.zootdungeon.actors.mobs.GreatCrab;
import com.zootdungeon.actors.mobs.Guard;
import com.zootdungeon.actors.mobs.Hound;
import com.zootdungeon.actors.mobs.Monk;
import com.zootdungeon.actors.mobs.Necromancer;
import com.zootdungeon.actors.mobs.PhantomPiranha;
import com.zootdungeon.actors.mobs.Piranha;
import com.zootdungeon.actors.mobs.Pylon;
import com.zootdungeon.actors.mobs.Rat;
import com.zootdungeon.actors.mobs.RipperDemon;
import com.zootdungeon.actors.mobs.RotHeart;
import com.zootdungeon.actors.mobs.RotLasher;
import com.zootdungeon.actors.mobs.Scorpio;
import com.zootdungeon.actors.mobs.Senior;
import com.zootdungeon.actors.mobs.Shaman;
import com.zootdungeon.actors.mobs.Skeleton;
import com.zootdungeon.actors.mobs.Slime;
import com.zootdungeon.actors.mobs.Snake;
import com.zootdungeon.actors.mobs.Spinner;
import com.zootdungeon.actors.mobs.Swarm;
import com.zootdungeon.actors.mobs.Succubus;
import com.zootdungeon.actors.mobs.Tengu;
import com.zootdungeon.actors.mobs.Thief;
import com.zootdungeon.actors.mobs.TormentedSpirit;
import com.zootdungeon.actors.mobs.Warlock;
import com.zootdungeon.actors.mobs.Wraith;
import com.zootdungeon.actors.mobs.YogDzewa;
import com.zootdungeon.actors.mobs.YogFist;
import com.zootdungeon.actors.mobs.Yokai;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndGeneral;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 调试工具：选择任意 {@link Mob} 类型并在指定格子生成。
 * <p>
 * 默认动作 {@link #AC_SPAWN} 打开一个选择窗口，按难度分级（普通 / 精英 / Boss / 特殊），
 * 选中后通过 {@link CellSelector} 选择放置位置，以 PASSIVE 状态生成。
 */
public class EnemyPlacer extends Item {

    public static final String AC_SPAWN = "SPAWN";

    public enum Tier {
        NORMAL("tier_normal"),
        ELITE("tier_elite"),
        BOSS("tier_boss"),
        SPECIAL("tier_special");

        public final String msgKey;

        Tier(String msgKey) {
            this.msgKey = msgKey;
        }
    }

    private static final Map<Tier, List<Class<? extends Mob>>> MOB_BY_TIER = new TreeMap<>();

    static {
        // === 普通（Weak）：地下城前中期常见杂兵，HP 5~50，等级 5~15 ===
        MOB_BY_TIER.put(Tier.NORMAL, List.of(
                Rat.class,
                Snake.class,
                Bat.class,
                Bee.class,
                Slime.class,
                Swarm.class,
                Crab.class,
                Gnoll.class,
                Thief.class,
                Skeleton.class,
                Wraith.class,
                TormentedSpirit.class,
                FetidRat.class,
                Bandit.class,
                Shaman.class,
                Spinner.class,
                Scorpio.class,
                CrystalWisp.class,
                Yokai.class
        ));

        // === 精英（Elite）：后期/高难度关卡敌人，HP 50~120，等级 16~26 ===
        MOB_BY_TIER.put(Tier.ELITE, List.of(
                GreatCrab.class,
                Brute.class,
                ArmoredBrute.class,
                GnollGuard.class,
                GnollTrickster.class,
                GnollSapper.class,
                GnollGeomancer.class,
                Guard.class,
                Succubus.class,
                Warlock.class,
                Elemental.class,
                Monk.class,
                Senior.class,
                DM100.class,
                DM200.class,
                DM201.class,
                DM300.class,
                Goo.class,
                Tengu.class,
                Hound.class,
                RipperDemon.class,
                Acidic.class,
                Piranha.class,
                PhantomPiranha.class,
                Eye.class,
                Necromancer.class,
                Golem.class,
                Ghoul.class,
                RotLasher.class,
                FungalSentry.class,
                CausticSlime.class
        ));

        // === Boss ===
        MOB_BY_TIER.put(Tier.BOSS, List.of(
                DwarfKing.class,
                YogFist.class,
                YogDzewa.class,
                RotHeart.class,
                FungalCore.class,
                CrystalMimic.class,
                EbonyMimic.class,
                GoldenMimic.class,
                Pylon.class,
                Device.class
        ));

        // === 特殊 ===
        MOB_BY_TIER.put(Tier.SPECIAL, List.of(
        ));
    }

    {
        image = SpriteRegistry.byLabel("debug_bag");
        stackable = false;
        unique = true;
        defaultAction = AC_SPAWN;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SPAWN);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_SPAWN)) {
            showMobSelection();
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SPAWN)) {
            return Messages.get(this, "ac_spawn");
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    private void showMobSelection() {
        WndGeneral.Builder b = WndGeneral.make()
                .title(Messages.get(this, "window_title"));

        for (Tier tier : Tier.values()) {
            String tierLabel = Messages.get(this, tier.msgKey);
            List<Class<? extends Mob>> mobs = MOB_BY_TIER.get(tier);
            b.tab(tierLabel, pane -> {
                pane.line(Messages.get(this, "tier_hint", tierLabel));
                for (Class<? extends Mob> cls : mobs) {
                    String name = cls.getSimpleName();
                    pane.option(name, () -> {
                        GameScene.selectCell(spawnListener(cls));
                    });
                }
            });
        }

        b.show();
    }

    private CellSelector.Listener spawnListener(final Class<? extends Mob> mobClass) {
        return new CellSelector.Listener() {
            @Override
            public void onSelect(Integer cell) {
                if (cell == null || cell < 0 || Dungeon.level == null) {
                    return;
                }
                if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.insideMap(cell)) {
                    GLog.w(Messages.get(EnemyPlacer.class, "invalid_cell"));
                    return;
                }
                if (Actor.findChar(cell) != null) {
                    GLog.w(Messages.get(EnemyPlacer.class, "cell_occupied"));
                    return;
                }
                if (!Dungeon.level.passable[cell]) {
                    GLog.w(Messages.get(EnemyPlacer.class, "cell_not_passable"));
                    return;
                }

                Mob mob;
                try {
                    mob = Reflection.newInstance(mobClass);
                } catch (Throwable t) {
                    GLog.w(Messages.get(EnemyPlacer.class, "spawn_failed"));
                    return;
                }
                if (mob == null) {
                    GLog.w(Messages.get(EnemyPlacer.class, "spawn_failed"));
                    return;
                }

                mob.pos = cell;
                mob.state = mob.PASSIVE;
                Dungeon.level.occupyCell(mob);
                if (mob.isAlive()) {
                    GameScene.add(mob);
                    GLog.p(Messages.get(EnemyPlacer.class, "spawned", mob.name(), cell));
                } else {
                    GLog.w(Messages.get(EnemyPlacer.class, "killed_by_effect", mob.name()));
                }
            }

            @Override
            public String prompt() {
                return Messages.get(EnemyPlacer.class, "prompt_spawn");
            }
        };
    }
}
