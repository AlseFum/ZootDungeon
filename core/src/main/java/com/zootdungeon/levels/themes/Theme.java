package com.zootdungeon.levels.themes;

import com.zootdungeon.Dungeon;
import com.zootdungeon.levels.DebugLevel;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.DeadEndLevel;
import com.zootdungeon.levels.LastLevel;
import com.zootdungeon.levels.themes.caves.CavesBossLevel;
import com.zootdungeon.levels.themes.caves.CavesLevel;
import com.zootdungeon.levels.themes.city.CityBossLevel;
import com.zootdungeon.levels.themes.city.CityLevel;
import com.zootdungeon.levels.themes.device.DeviceSewerBossLevel;
import com.zootdungeon.levels.themes.device.DeviceSewerLevel;
import com.zootdungeon.levels.themes.halls.HallsBossLevel;
import com.zootdungeon.levels.themes.halls.HallsLevel;
import com.zootdungeon.levels.themes.prison.PrisonBossLevel;
import com.zootdungeon.levels.themes.prison.PrisonLevel;
import com.zootdungeon.levels.themes.sewer.SewerLevel;
import com.zootdungeon.levels.themes.sewer.SewerBossLevel;
import com.zootdungeon.actors.mobs.*;
import com.zootdungeon.arknights.MainTheme.HourOfAnAwakening.HourOfAnAwakeningTheme;
import com.zootdungeon.items.trinkets.RatSkull;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class Theme implements Bundlable {
    // Theme pack data.
    public Class<? extends Level> normalLevel;
    public Class<? extends Level> bossLevel;

    // Weight function: (depth, branch) -> weight (0 = unavailable, >0 = available).
    public BiFunction<Integer, Integer, Short> available;

    public Theme() {
        this.available = (depth, branch) -> (short) 1;
    }

    public Theme(Class<? extends Level> normalLevel, Class<? extends Level> bossLevel) {
        this.normalLevel = normalLevel;
        this.bossLevel = bossLevel;
        this.available = (depth, branch) -> (short) 1;
    }

    public Theme(Class<? extends Level> normalLevel, Class<? extends Level> bossLevel,
                     BiFunction<Integer, Integer, Short> available) {
        this.normalLevel = normalLevel;
        this.bossLevel = bossLevel;
        this.available = available != null ? available : (depth, branch) -> (short) 1;
    }

    public short getWeight(int depth, int branch) {
        return available.apply(depth, branch);
    }

    public boolean isAvailable(int depth, int branch) {
        return available.apply(depth, branch) > 0;
    }

    public Level getNormalLevel() {
        try {
            return normalLevel.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new DeadEndLevel();
        }
    }
    public Level getBossLevel() {
        try {
            return bossLevel.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new DeadEndLevel();
        }
    }

    /**
     * Called when the game enters this theme during floor transitions.
     * Override in custom themes if theme-scoped runtime hooks are needed.
     */
    public void enterTheme() {
        // no-op by default
    }

    /**
     * Called when the game leaves this theme during floor transitions.
     * Override in custom themes if theme-scoped runtime hooks are needed.
     */
    public void leaveTheme() {
        // no-op by default
    }

    // Bundle keys for serialization.
    private static final String NORMAL_LEVEL = "normal_level";
    private static final String BOSS_LEVEL = "boss_level";

    @Override
    public void storeInBundle(Bundle bundle) {
        if (normalLevel != null) {
            bundle.put(NORMAL_LEVEL, normalLevel.getName());
        }
        if (bossLevel != null) {
            bundle.put(BOSS_LEVEL, bossLevel.getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreFromBundle(Bundle bundle) {
        try {
            if (bundle.contains(NORMAL_LEVEL)) {
            String normalLevelName = bundle.getString(NORMAL_LEVEL);
            normalLevel = (Class<? extends Level>) Class.forName(normalLevelName);
            } else {
                normalLevel = SewerLevel.class;
            }
            
            if (bundle.contains(BOSS_LEVEL)) {
                String bossLevelName = bundle.getString(BOSS_LEVEL);
                bossLevel = (Class<? extends Level>) Class.forName(bossLevelName);
            } else {
                bossLevel = SewerBossLevel.class;
            }
        } catch (ClassNotFoundException e) {
            // Fallback to sewer theme if classes cannot be found.
            normalLevel = SewerLevel.class;
            bossLevel = SewerBossLevel.class;
        }
    }

    // -------------------------------------------------------------------------
    // Merged Theme System (ThemeBuilder + ThemeManager + ThemeSheet)
    // -------------------------------------------------------------------------

    public static final Map<String, Theme> themePacks = new HashMap<>();

    public static Theme SewerTheme;
    public static Theme CavesTheme;
    public static Theme CityTheme;
    public static Theme HallsTheme;
    public static Theme PrisonTheme;
    public static Theme DebugTheme;
    public static Theme DevTheme;

    private static String currentTheme = "sewer";

    static {
        SewerTheme = new Theme(
                SewerLevel.class, SewerBossLevel.class,
                (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 5) ? (short) 1 : (short) 0) {
            @Override
            public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
                ArrayList<Class<? extends Mob>> mobs;
                if (depth == 1) mobs = new ArrayList<>(Arrays.asList(Rat.class, Rat.class, Rat.class, Snake.class));
                else if (depth == 2) mobs = new ArrayList<>(Arrays.asList(Rat.class, Rat.class, Snake.class, Gnoll.class, Gnoll.class));
                else if (depth == 3) mobs = new ArrayList<>(Arrays.asList(Rat.class, Snake.class, Gnoll.class, Gnoll.class, Gnoll.class, Swarm.class, Crab.class));
                else mobs = new ArrayList<>(Arrays.asList(Gnoll.class, Swarm.class, Crab.class, Crab.class, Slime.class, Slime.class));
                addRareMobs(depth, mobs);
                swapMobAlts(mobs);
                Random.shuffle(mobs);
                return mobs;
            }
        };

        PrisonTheme = new Theme(
                PrisonLevel.class, PrisonBossLevel.class,
                (depth, branch) -> (branch == 0 && depth >= 6 && depth <= 10) ? (short) 1 : (short) 0) {
            @Override
            public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
                ArrayList<Class<? extends Mob>> mobs;
                if (depth == 6) mobs = new ArrayList<>(Arrays.asList(Skeleton.class, Skeleton.class, Skeleton.class, Thief.class, Swarm.class));
                else if (depth == 7) mobs = new ArrayList<>(Arrays.asList(Skeleton.class, Skeleton.class, Skeleton.class, Thief.class, DM100.class, Guard.class));
                else if (depth == 8) mobs = new ArrayList<>(Arrays.asList(Skeleton.class, Skeleton.class, Thief.class, DM100.class, DM100.class, Guard.class, Guard.class, Necromancer.class));
                else mobs = new ArrayList<>(Arrays.asList(Skeleton.class, Thief.class, DM100.class, DM100.class, Guard.class, Guard.class, Necromancer.class, Necromancer.class));
                addRareMobs(depth, mobs);
                swapMobAlts(mobs);
                Random.shuffle(mobs);
                return mobs;
            }
        };

        CavesTheme = new Theme(
                CavesLevel.class, CavesBossLevel.class,
                (depth, branch) -> (branch == 0 && depth >= 11 && depth <= 15) ? (short) 1 : (short) 0) {
            @Override
            public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
                ArrayList<Class<? extends Mob>> mobs;
                if (depth == 11) mobs = new ArrayList<>(Arrays.asList(Bat.class, Bat.class, Bat.class, Brute.class, Shaman.random()));
                else if (depth == 12) mobs = new ArrayList<>(Arrays.asList(Bat.class, Bat.class, Brute.class, Brute.class, Shaman.random(), Spinner.class));
                else if (depth == 13) mobs = new ArrayList<>(Arrays.asList(Bat.class, Brute.class, Brute.class, Shaman.random(), Shaman.random(), Spinner.class, Spinner.class, DM200.class));
                else mobs = new ArrayList<>(Arrays.asList(Bat.class, Brute.class, Shaman.random(), Shaman.random(), Spinner.class, Spinner.class, DM200.class, DM200.class));
                addRareMobs(depth, mobs);
                swapMobAlts(mobs);
                Random.shuffle(mobs);
                return mobs;
            }
        };

        CityTheme = new Theme(
                CityLevel.class, CityBossLevel.class,
                (depth, branch) -> (branch == 0 && depth >= 16 && depth <= 20) ? (short) 1 : (short) 0) {
            @Override
            public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
                ArrayList<Class<? extends Mob>> mobs;
                if (depth == 16) mobs = new ArrayList<>(Arrays.asList(Ghoul.class, Ghoul.class, Ghoul.class, Elemental.random(), Warlock.class));
                else if (depth == 17) mobs = new ArrayList<>(Arrays.asList(Ghoul.class, Elemental.random(), Elemental.random(), Warlock.class, Monk.class));
                else if (depth == 18) mobs = new ArrayList<>(Arrays.asList(Ghoul.class, Elemental.random(), Warlock.class, Warlock.class, Monk.class, Monk.class, Golem.class));
                else mobs = new ArrayList<>(Arrays.asList(Elemental.random(), Warlock.class, Warlock.class, Monk.class, Monk.class, Golem.class, Golem.class, Golem.class));
                addRareMobs(depth, mobs);
                swapMobAlts(mobs);
                Random.shuffle(mobs);
                return mobs;
            }
        };

        HallsTheme = new Theme(
                HallsLevel.class, HallsBossLevel.class,
                (depth, branch) -> (branch == 0 && depth >= 21 && depth <= 25) ? (short) 1 : (short) 0) {
            @Override
            public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
                ArrayList<Class<? extends Mob>> mobs;
                if (depth == 21) mobs = new ArrayList<>(Arrays.asList(Succubus.class, Succubus.class, Eye.class));
                else if (depth == 22) mobs = new ArrayList<>(Arrays.asList(Succubus.class, Eye.class));
                else if (depth == 23) mobs = new ArrayList<>(Arrays.asList(Succubus.class, Eye.class, Eye.class, Scorpio.class));
                else mobs = new ArrayList<>(Arrays.asList(Succubus.class, Eye.class, Eye.class, Scorpio.class, Scorpio.class, Scorpio.class));
                addRareMobs(depth, mobs);
                swapMobAlts(mobs);
                Random.shuffle(mobs);
                return mobs;
            }
        };

        DebugTheme = new Theme(DebugLevel.class, DebugLevel.class);

        DevTheme = new Theme(
                DeviceSewerLevel.class, DeviceSewerBossLevel.class,
                (depth, branch) -> (branch == 0 && depth >= 1 && depth <= 25) ? (short) 4 : (short) 0) {
            @Override
            public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
                if (depth >= 1 && depth <= 5) return SewerTheme.getMobRotation(depth, branch);
                if (depth >= 6 && depth <= 10) return PrisonTheme.getMobRotation(depth, branch);
                if (depth >= 11 && depth <= 15) return CavesTheme.getMobRotation(depth, branch);
                if (depth >= 16 && depth <= 20) return CityTheme.getMobRotation(depth, branch);
                if (depth >= 21 && depth <= 25) return HallsTheme.getMobRotation(depth, branch);
                return SewerTheme.getMobRotation(1, branch);
            }
        };

        registerDefaultThemes();
    }

    public static void registerDefaultThemes() {
        registerTheme("sewer", SewerTheme);
        registerTheme("prison", PrisonTheme);
        registerTheme("caves", CavesTheme);
        registerTheme("city", CityTheme);
        registerTheme("halls", HallsTheme);
        registerTheme("dev", DevTheme);
        HourOfAnAwakeningTheme.register();
    }

    public static void registerTheme(String name, Theme theme) {
        if (name == null || theme == null) return;
        themePacks.put(name, theme);
    }

    public static Theme getTheme(String name) {
        return themePacks.get(name);
    }

    public static boolean hasTheme(String name) {
        return themePacks.containsKey(name);
    }

    public static List<String> getThemeNames() {
        return new ArrayList<>(themePacks.keySet());
    }

    public static void setCurrentTheme(String name) {
        if (hasTheme(name)) currentTheme = name;
    }

    public static Theme getCurrentTheme() {
        Theme theme = getTheme(currentTheme);
        return theme != null ? theme : SewerTheme;
    }

    public static boolean bossLevel(int depth) {
        return depth == 5 || depth == 10 || depth == 15 || depth == 20 || depth == 25;
    }

    public static boolean shopOnLevel() {
        return shopOnLevel(Dungeon.depth);
    }

    public static boolean shopOnLevel(int depth) {
        return depth == 6 || depth == 11 || depth == 16;
    }

    public static Level createLevel(int depth) {
        return createLevel(depth, 0);
    }

    public static Level createLevel(int depth, int branch) {
        if (depth < -5) return new DeadEndLevel();
        if (depth > -5 && depth < 0) return new DebugLevel();
        if (depth >= 26) return new LastLevel();

        short maxWeight = 0;
        List<String> topThemes = new ArrayList<>();
        for (Map.Entry<String, Theme> entry : themePacks.entrySet()) {
            String themeName = entry.getKey();
            Theme pack = entry.getValue();
            short weight = pack.getWeight(depth, branch);
            if (weight <= 0) continue;

            if (weight > maxWeight) {
                maxWeight = weight;
                topThemes.clear();
                topThemes.add(themeName);
            } else if (weight == maxWeight) {
                topThemes.add(themeName);
            }
        }

        if (topThemes.isEmpty()) return new DeadEndLevel();

        String selectedTheme = Random.element(topThemes);
        setCurrentTheme(selectedTheme);
        Theme pack = getCurrentTheme();
        Level level = bossLevel(depth) ? pack.getBossLevel() : pack.getNormalLevel();
        if (depth == 5 && branch == 0) {
            GLog.w("theme debug: depth=5 theme=" + selectedTheme
                    + ", level=" + level.getClass().getSimpleName()
                    + ", candidates=" + topThemes);
        }
        return level;
    }

    public static void selectRandomTheme() {
        List<String> themes = getThemeNames();
        if (!themes.isEmpty()) setCurrentTheme(Random.element(themes));
    }

    public static Theme resolveForLevel(Level level) {
        if (level == null) return null;
        for (Theme pack : themePacks.values()) {
            if (pack.normalLevel != null && pack.normalLevel.isInstance(level)) return pack;
            if (pack.bossLevel != null && pack.bossLevel.isInstance(level)) return pack;
        }
        return null;
    }

    /**
     * 根据深度和分支选择主题（与 createLevel 的选取逻辑一致）。
     * 用于无 Level 上下文时（如 DistortionTrap）获取该深度对应的怪物轮换。
     */
    public static Theme getThemeForDepth(int depth, int branch) {
        if (depth < 1 || depth >= 26) return null;
        short maxWeight = 0;
        List<String> topThemes = new ArrayList<>();
        for (Map.Entry<String, Theme> entry : themePacks.entrySet()) {
            String themeName = entry.getKey();
            Theme pack = entry.getValue();
            short weight = pack.getWeight(depth, branch);
            if (weight <= 0) continue;
            if (weight > maxWeight) {
                maxWeight = weight;
                topThemes.clear();
                topThemes.add(themeName);
            } else if (weight == maxWeight) {
                topThemes.add(themeName);
            }
        }
        if (topThemes.isEmpty()) return null;
        return getTheme(Random.element(topThemes));
    }

    /**
     * 返回本主题在该深度/分支下的怪物轮换。
     * 子类可覆盖以自定义怪物池。
     * 默认按深度委托给对应区域主题（DebugTheme 等未覆盖时使用）。
     */
    public ArrayList<Class<? extends Mob>> getMobRotation(int depth, int branch) {
        if (depth >= 1 && depth <= 5) return SewerTheme.getMobRotation(depth, branch);
        if (depth >= 6 && depth <= 10) return PrisonTheme.getMobRotation(depth, branch);
        if (depth >= 11 && depth <= 15) return CavesTheme.getMobRotation(depth, branch);
        if (depth >= 16 && depth <= 20) return CityTheme.getMobRotation(depth, branch);
        if (depth >= 21 && depth <= 25) return HallsTheme.getMobRotation(depth, branch);
        return SewerTheme.getMobRotation(1, branch);
    }

    /**
     * 有概率向轮换中添加稀有怪物。子类可覆盖以自定义或禁用。
     */
    protected void addRareMobs(int depth, ArrayList<Class<? extends Mob>> rotation) {
        if (depth == 4 && Random.Float() < 0.025f) rotation.add(Thief.class);
        else if (depth == 9 && Random.Float() < 0.025f) rotation.add(Bat.class);
        else if (depth == 14 && Random.Float() < 0.025f) rotation.add(Ghoul.class);
        else if (depth == 19 && Random.Float() < 0.025f) rotation.add(Succubus.class);
    }

    /**
     * 有概率将普通怪物替换为变种。子类可覆盖以自定义或禁用。
     */
    protected void swapMobAlts(ArrayList<Class<? extends Mob>> rotation) {
        float altChance = 1 / 50f * RatSkull.exoticChanceMultiplier();
        for (int i = 0; i < rotation.size(); i++) {
            if (Random.Float() < altChance) {
                Class<? extends Mob> cl = rotation.get(i);
                if (cl == Rat.class) cl = Albino.class;
                else if (cl == Slime.class) cl = CausticSlime.class;
                else if (cl == Thief.class) cl = Bandit.class;
                else if (cl == Necromancer.class) cl = SpectralNecromancer.class;
                else if (cl == Brute.class) cl = ArmoredBrute.class;
                else if (cl == DM200.class) cl = DM201.class;
                else if (cl == Monk.class) cl = Senior.class;
                else if (cl == Scorpio.class) cl = Acidic.class;
                else continue;
                rotation.set(i, cl);
            }
        }
    }

    /** 从轮换中排除指定类型，空缺用 Gnoll 补位 */
    protected static ArrayList<Class<? extends Mob>> exclude(ArrayList<Class<? extends Mob>> rotation,
                                                              Class<? extends Mob>... exclude) {
        if (exclude == null || exclude.length == 0) return rotation;
        Set<Class<?>> set = new HashSet<>(Arrays.asList(exclude));
        rotation.removeIf(set::contains);
        while (rotation.isEmpty()) rotation.add(Gnoll.class);
        return rotation;
    }

    public static void onLeaveLevel(Level level) {
        Theme pack = resolveForLevel(level);
        if (pack != null) pack.leaveTheme();
    }

    public static void onEnterLevel(Level level) {
        Theme pack = resolveForLevel(level);
        if (pack != null) {
            for (Map.Entry<String, Theme> entry : themePacks.entrySet()) {
                if (entry.getValue() == pack) {
                    setCurrentTheme(entry.getKey());
                    break;
                }
            }
            pack.enterTheme();
        }
    }

    public static class Builder {
        private final Theme theme;

        public Builder() {
            theme = new Theme();
            theme.normalLevel = DeadEndLevel.class;
            theme.bossLevel = DeadEndLevel.class;
        }

        public Builder setNormalLevel(Class<? extends Level> levelClass) {
            theme.normalLevel = levelClass;
            return this;
        }

        public Builder setBossLevel(Class<? extends Level> levelClass) {
            theme.bossLevel = levelClass;
            return this;
        }

        public Builder setAvailable(BiFunction<Integer, Integer, Short> available) {
            theme.available = available != null ? available : (d, b) -> (short) 1;
            return this;
        }

        public Theme register(String themeName) {
            Theme.registerTheme(themeName, theme);
            return theme;
        }
    }
}
