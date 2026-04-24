package com.zootdungeon.arknights.MainTheme;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.DefenseDown;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Vulnerable;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.BlobEmitter;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.levels.Level;
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

    /**
     * Plants a charged grenade at the target cell. After 1 turn it detonates.
     * @param owner The char who owns/controls the weapon.
     * @param cell  The center cell for the 2x2 blast.
     * @return The planted blob, or null if failed.
     */
    public Blob plantGrenadeAt(Char owner, int cell) {
        if (Dungeon.level == null) return null;
        int sd = Dungeon.scalingDepth();
        int dmg = Random.NormalIntRange(6 + sd, 11 + sd + sd / 2);
        // 2x2 area: seed center and one adjacent cell so detonation hits 2x2
        SkullShattererWeapon.GrenadeWarningBlob.plant(Dungeon.level, cell, dmg, owner);
        return Dungeon.level.blobs.get(GrenadeWarningBlob.class);
    }

    /**
     * Immediate detonation helper (used by player weapon).
     */
    public void doGrenadeAt(Level level, Char owner, int cell) {
        PathFinder.buildDistanceMap(cell, BArray.not(level.solid, null), 1);
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
                    sw.plantGrenadeAt(h, targetCell);
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

    /**
     * 榴弹蓄力警告 Blob：
     * 种植时 amount=2（存活 2 个 act 回合），
     * 第 2 个回合结束时在中心格触发 2x2 范围爆炸，然后自动清除。
     */
    public static class GrenadeWarningBlob extends Blob {

        private int storedDamage;
        private int grenadeOwnerId = -1;

        public static final int LIFETIME = 2;

        public static void plant(Level level, int cell, int damage, Char owner) {
            GrenadeWarningBlob blob = (GrenadeWarningBlob) level.blobs.get(GrenadeWarningBlob.class);
            if (blob == null) {
                blob = new GrenadeWarningBlob();
                level.blobs.put(GrenadeWarningBlob.class, blob);
            }
            blob.storedDamage = damage;
            blob.grenadeOwnerId = owner.id();
            blob.seed(level, cell, LIFETIME);
            if (blob.cur == null) {
                blob.cur = new int[level.length()];
                blob.off = new int[level.length()];
            }
            Actor.add(blob);

            if (level == Dungeon.level && level.heroFOV[cell]) {
                CellEmitter.get(cell).burst(BlastParticle.FACTORY, 6);
            }
        }

        @Override
        protected void evolve() {
            int cell;
            for (int i = area.left - 1; i <= area.right; i++) {
                for (int j = area.top - 1; j <= area.bottom; j++) {
                    cell = i + j * Dungeon.level.width();
                    if (cur[cell] > 0) {
                        off[cell] = cur[cell] - 1;
                        volume += off[cell];
                        if (off[cell] <= 0) {
                            detonate(i + j * Dungeon.level.width());
                        }
                    }
                }
            }
        }

        private void detonate(int centerCell) {
            Char owner = (Char) Actor.findById(grenadeOwnerId);
            if (owner == null) owner = Dungeon.hero;

            int width = Dungeon.level.width();
            ArrayList<Integer> affectedCells = new ArrayList<>();
            affectedCells.add(centerCell);
            if (centerCell % width > 0) affectedCells.add(centerCell - 1);
            if (centerCell % width < width - 1) affectedCells.add(centerCell + 1);
            if (centerCell >= width) affectedCells.add(centerCell - width);
            if (centerCell < Dungeon.level.length() - width) affectedCells.add(centerCell + width);

            for (int cell : affectedCells) {
                if (cell < 0 || cell >= Dungeon.level.length()) continue;
                if (Dungeon.level.solid[cell]) continue;

                Char ch = Actor.findChar(cell);
                if (ch != null && (owner == null || ch != owner)) {
                    int dmg = storedDamage - ch.drRoll();
                    if (dmg > 0) ch.damage(dmg, owner);
                    Buff.affect(ch, ShatterDebuff.class, ShatterDebuff.DURATION);
                }
            }

            if (Dungeon.level.heroFOV[centerCell]) {
                CellEmitter.center(centerCell).burst(BlastParticle.FACTORY, 12);
            }
        }

        @Override
        public void use(BlobEmitter emitter) {
            super.use(emitter);
            emitter.pour(BlastParticle.FACTORY, 0.08f);
        }

        @Override
        public String tileDesc() {
            return "";
        }
    }
}
