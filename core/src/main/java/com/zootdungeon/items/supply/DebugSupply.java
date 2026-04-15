package com.zootdungeon.items.supply;

import com.zootdungeon.arknights.ascalon.AscalonAmbush;
import com.zootdungeon.arknights.ascalon.AscalonAOE;
import com.zootdungeon.items.weapon.configurable.StateSwitchWeapon;
import com.zootdungeon.items.weapon.configurable.InstantMechWeapon;
import com.zootdungeon.items.weapon.configurable.MomentumWeapon;
import com.zootdungeon.items.weapon.configurable.PropertyHuntingWeapon;
import com.zootdungeon.arknights.gitano.GitanoCard;
import com.zootdungeon.items.weapon.configurable.RangeReducedWeapon;
import com.zootdungeon.arknights.phantom.PhantomKnife;
import com.zootdungeon.items.weapon.configurable.TransferMechWeapon;
import com.zootdungeon.arknights.tragodia.TragodiaWand;
import com.zootdungeon.arknights.necrass.NecrassCard;
import com.zootdungeon.items.weapon.chakram.Chakram;
import com.zootdungeon.items.weapon.configurable.TwinBlade;
import com.zootdungeon.arknights.misc.DeployablewCrossBow;
import com.zootdungeon.arknights.misc.NearRangeCrossBow;
import com.zootdungeon.arknights.misc.RhodesStandardWeaponSupply;
import com.zootdungeon.arknights.MainTheme.SkullShattererWeapon;
import com.zootdungeon.items.weapon.firearms.FirearmBullet;
import com.zootdungeon.items.weapon.firearms.FirearmMagazine;
import com.zootdungeon.items.weapon.firearms.FirearmWeapon;
import com.zootdungeon.arknights.plugins.DefenseBoostPlugin;
import com.zootdungeon.arknights.plugins.MetabolismOverclockPlugin;
import com.zootdungeon.arknights.plugins.NextAttackCostRefundPlugin;
import com.zootdungeon.arknights.plugins.NextAttackDamageBoostPlugin;
import com.zootdungeon.arknights.plugins.OverclockedStrikesPlugin;
import com.zootdungeon.arknights.plugins.OverclockedStrikesPlusPlugin;
import com.zootdungeon.arknights.plugins.PullEnemyPlugin;
import com.zootdungeon.arknights.plugins.ReachBoostPlugin;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndGeneral;
import com.zootdungeon.windows.WndBag;
import com.zootdungeon.items.cheat.Codex;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.items.cheat.ItemRemover;
import com.zootdungeon.items.cheat.ItemEditor;
import com.zootdungeon.items.cheat.Panacea;
import com.zootdungeon.items.cheat.RedStone;
import com.zootdungeon.items.cheat.EventBusProbe;
import com.zootdungeon.items.cheat.ThrowingWeaponBox;
import com.zootdungeon.items.cheat.WandBox;
import com.zootdungeon.items.cheat.BombBox;
import com.zootdungeon.items.cheat.StoneOfDungeonTravel;
import com.zootdungeon.items.cheat.StoneOfLevelSelect;
import com.zootdungeon.items.cheat.StoneOfSpawn;
import com.zootdungeon.items.TengusMask;
import com.zootdungeon.items.KingsCrown;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DebugSupply extends Supply {
    static {
        SpriteRegistry.texture("sheet.cola.debug_bag", "cola/debug_bag.png")
                    .setXY("debug_bag", 0, 0, 32, 32);
    }
    {
        
        image = SpriteRegistry.byLabel("debug_bag");
    }

    // private static final String CAT_POTIONS = "cat_potions";
    private static final String CAT_STONES = "cat_stones";
    private static final String CAT_STANDARD_SUPPLY = "cat_standard_supply";
    private static final String CAT_CHEAT = "cat_cheat";
    private static final String CAT_WEAPONS = "cat_weapons";
    private static final String CAT_FIREARMS = "cat_firearms";
    private static final String CAT_PLUGINS = "cat_plugins";

    private final Map<String, List<Supplier<Item>>> categories = new LinkedHashMap<>();

    public DebugSupply() {
        super();
        name = Messages.get(DebugSupply.class, "name");
        desc = Messages.get(DebugSupply.class, "desc");


        List<Supplier<Item>> stones = new ArrayList<>();
        stones.add(() -> create(StoneOfSpawn.class, 300));
        stones.add(() -> create(StoneOfLevelSelect.class, 50));
        categories.put(CAT_STONES, stones);

        List<Supplier<Item>> standardSupply = new ArrayList<>();
        standardSupply.add(() -> create(RhodesStandardWeaponSupply.class, 1));
        categories.put(CAT_STANDARD_SUPPLY, standardSupply);

        List<Supplier<Item>> cheat = new ArrayList<>();
        cheat.add(() -> create(DivineAnkh.class, 1));
        cheat.add(() -> create(ItemRemover.class));
        cheat.add(() -> create(ItemEditor.class, 1));
        cheat.add(() -> create(StoneOfDungeonTravel.class, 1));
        cheat.add(() -> create(Panacea.class, 1));
        cheat.add(() -> create(Codex.class, 1));
        cheat.add(() -> create(RedStone.class, 1));
        cheat.add(() -> create(EventBusProbe.class, 1));
        cheat.add(() -> create(ThrowingWeaponBox.class, 1));
        cheat.add(() -> create(WandBox.class, 1));
        cheat.add(() -> create(BombBox.class, 1));
        cheat.add(() -> create(TengusMask.class, 1));
        cheat.add(() -> create(KingsCrown.class, 1));
        categories.put(CAT_CHEAT, cheat);

        List<Supplier<Item>> plugins = new ArrayList<>();
        // ReservedOp 随机赠送/终端掉落池中的插件（调试用直接发放）
        plugins.add(() -> create(ReachBoostPlugin.class, 1));
        plugins.add(() -> create(NextAttackDamageBoostPlugin.class, 1));
        plugins.add(() -> create(NextAttackCostRefundPlugin.class, 1));
        plugins.add(() -> create(DefenseBoostPlugin.class, 1));
        plugins.add(() -> create(PullEnemyPlugin.class, 1));
        plugins.add(() -> create(MetabolismOverclockPlugin.class, 1));
        plugins.add(() -> create(OverclockedStrikesPlugin.class, 1));
        plugins.add(() -> create(OverclockedStrikesPlusPlugin.class, 1));
        categories.put(CAT_PLUGINS, plugins);

        List<Supplier<Item>> weapons = new ArrayList<>();
        weapons.add(() -> create(AscalonAmbush.class, 1));
        weapons.add(() -> create(AscalonAOE.class, 1));
        weapons.add(() -> create(StateSwitchWeapon.class, 1));
        weapons.add(() -> create(PhantomKnife.class, 1));
        weapons.add(() -> create(TragodiaWand.class, 1));
        weapons.add(() -> create(RangeReducedWeapon.class, 1));
        weapons.add(() -> create(MomentumWeapon.class, 1));
        weapons.add(() -> create(PropertyHuntingWeapon.class, 1));
        weapons.add(() -> create(GitanoCard.class, 50));
        weapons.add(() -> create(NecrassCard.class, 50));
        weapons.add(() -> create(TwinBlade.class, 5));
        weapons.add(() -> create(InstantMechWeapon.class, 1));
        weapons.add(() -> create(TransferMechWeapon.class, 1));
        weapons.add(() -> create(SkullShattererWeapon.class, 1));
        weapons.add(() -> create(Chakram.class, 1));
        weapons.add(() -> create(NearRangeCrossBow.class, 1));
        weapons.add(() -> create(DeployablewCrossBow.class, 1));
        categories.put(CAT_WEAPONS, weapons);

        // Firearms: one tab, 4 rows x 3 columns (type rows, A/B/C columns)
        categories.put(CAT_FIREARMS, List.of()); // placeholder; rendered by a custom pane
    }

    private static Item create(Class<? extends Item> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private static Item create(Class<? extends Item> clazz, int qty) {
        try {
            return clazz.getConstructor().newInstance().quantity(qty);
        } catch (Exception e) {
            return null;
        }
    }

    private static FirearmMagazine createCPistolMagazine() {
        return new FirearmMagazine("c_pistol_mag", FirearmBullet.Presets.fmj(), 8, 8);
    }

    private static FirearmMagazine createCShotgunMagazine() {
        return new FirearmMagazine("c_shot_mag", FirearmBullet.Presets.buckshot(), 4, 4);
    }

    private static FirearmMagazine createCRifleMagazine() {
        return new FirearmMagazine("c_rifle_mag", FirearmBullet.Presets.fmj(), 30, 30);
    }

    private static FirearmMagazine createCSniperMagazine() {
        return new FirearmMagazine("c_snipe_mag", FirearmBullet.Presets.incendiary(), 5, 5);
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_OPEN)) {
            showCategorySelection(hero);
        } else {
            super.execute(hero, action);
        }
    }
    @Override
    public String defaultAction(){
        return AC_OPEN;
    }

    /** Root: one {@link WndGeneral} with {@link WndGeneral.Builder#tab} per category. */
    private void showCategorySelection(Hero hero) {
        WndGeneral.Builder b = WndGeneral.make().title(Messages.get(DebugSupply.class, "name"));
        for (String catKey : categories.keySet()) {
            final String key = catKey;
            if (CAT_CHEAT.equals(key)) {
                b.tab(Messages.get(DebugSupply.class, key), p -> fillCheatTab(p, hero));
            } else if (CAT_FIREARMS.equals(key)) {
                b.tab(Messages.get(DebugSupply.class, key), p -> fillFirearmsTab(p, hero));
            } else {
                b.tab(Messages.get(DebugSupply.class, key), p -> fillItemTab(p, hero, key));
            }
        }
        b.show();
    }

    private void fillFirearmsTab(WndGeneral.PaneBuilder p, Hero hero) {
        p.line(Messages.get(DebugSupply.class, "tab_hint"));
        p.line("4 行 × 3 列：每行一种枪型（手枪/霰弹/步枪/狙击），每列对应 A/B/C。");

        // Row 1: Pistols
        p.hrow(r -> r.button("A-手枪", () -> grantItem(hero, () -> create(A_Pistol.class, 1)))
                .button("B-手枪", () -> grantItem(hero, () -> create(B_Pistol.class, 1)))
                .button("C-手枪", () -> grantItem(hero, () -> create(C_Pistol.class, 1))));

        // Row 2: Shotguns
        p.hrow(r -> r.button("A-霰弹", () -> grantItem(hero, () -> create(A_Shotgun.class, 1)))
                .button("B-霰弹", () -> grantItem(hero, () -> create(B_Shotgun.class, 1)))
                .button("C-霰弹", () -> grantItem(hero, () -> create(C_Shotgun.class, 1))));

        // Row 3: Rifles
        p.hrow(r -> r.button("A-步枪", () -> grantItem(hero, () -> create(A_Rifle.class, 1)))
                .button("B-步枪", () -> grantItem(hero, () -> create(B_Rifle.class, 1)))
                .button("C-步枪", () -> grantItem(hero, () -> create(C_Rifle.class, 1))));

        // Row 4: Snipers
        p.hrow(r -> r.button("A-狙击", () -> grantItem(hero, () -> create(A_Sniper.class, 1)))
                .button("B-狙击", () -> grantItem(hero, () -> create(B_Sniper.class, 1)))
                .button("C-狙击", () -> grantItem(hero, () -> create(C_Sniper.class, 1))));

        p.line("");
        p.line("弹匣：直接发到背包，供 C 类枪械更换。");
        p.hrow(r -> r.button("手枪弹匣", () -> grantItem(hero, DebugSupply::createCPistolMagazine))
                .button("霰弹弹匣", () -> grantItem(hero, DebugSupply::createCShotgunMagazine))
                .button("步枪弹匣", () -> grantItem(hero, DebugSupply::createCRifleMagazine)));
        p.hrow(r -> r.button("狙击弹匣", () -> grantItem(hero, DebugSupply::createCSniperMagazine)));

        p.line("");
        p.line("子弹：仅保留 3 种基础调试弹。燃烧弹会在命中格/碰撞格生成火焰。");
        p.hrow(r -> r.button("FMJ", () -> applyBulletToEquipped(hero, FirearmBullet.Presets.fmj()))
                .button("鹿弹", () -> applyBulletToEquipped(hero, FirearmBullet.Presets.buckshot()))
                .button("燃烧", () -> applyBulletToEquipped(hero, FirearmBullet.Presets.incendiary())));

        // A-type special controls
        p.line("");
        p.line("A 型：特殊弹开关/补充。");
        p.hrow(r -> r.button("A特弹+10", () -> addASpecialAmmo(hero, 10))
                .button("A特弹+50", () -> addASpecialAmmo(hero, 50))
                .button("A特弹开关", () -> toggleASpecialMode(hero)));
    }

    private static FirearmWeapon equippedFirearm(Hero hero) {
        if (hero == null || hero.belongings == null) return null;
        Item w = hero.belongings.weapon();
        if (w instanceof FirearmWeapon) return (FirearmWeapon) w;
        return null;
    }

    private static void applyBulletToEquipped(Hero hero, FirearmBullet bullet) {
        FirearmWeapon fw = equippedFirearm(hero);
        if (fw == null) {
            com.zootdungeon.utils.GLog.w("未装备枪械。");
            return;
        }
        if (fw instanceof DebugBulletLoader) {
            if (!((DebugBulletLoader) fw).applyDebugBullet(bullet)) {
                com.zootdungeon.utils.GLog.w("该枪型当前无法装入这发子弹。");
            }
            return;
        }
        com.zootdungeon.utils.GLog.w("该枪型不支持此装弹操作。");
    }

    private static void addASpecialAmmo(Hero hero, int add) {
        FirearmWeapon fw = equippedFirearm(hero);
        if (!(fw instanceof DebugASpecialControl)) {
            com.zootdungeon.utils.GLog.w("未装备 A 型枪械。");
            return;
        }
        DebugASpecialControl a = (DebugASpecialControl) fw;
        a.addSpecialAmmo(Math.max(0, add));

        com.zootdungeon.utils.GLog.i("A 型特殊弹: " + a.specialAmmo());
    }

    private static void toggleASpecialMode(Hero hero) {
        FirearmWeapon fw = equippedFirearm(hero);
        if (!(fw instanceof DebugASpecialControl)) {
            com.zootdungeon.utils.GLog.w("未装备 A 型枪械。");
            return;
        }
        DebugASpecialControl a = (DebugASpecialControl) fw;
        a.setSpecialMode(!a.specialMode());
        com.zootdungeon.utils.GLog.i("A 型特殊模式: " + (a.specialMode() ? "启用" : "关闭"));
    }

    private void fillItemTab(WndGeneral.PaneBuilder p, Hero hero, String categoryKey) {
        p.line(Messages.get(DebugSupply.class, "tab_hint"));
        appendItemOptions(p, hero, categoryKey);
    }

    private void fillCheatTab(WndGeneral.PaneBuilder p, Hero hero) {
        p.line(Messages.get(DebugSupply.class, "cheat_tab_line"));
        p.option(Messages.get(DebugSupply.class, "grant_all_cheat"), () -> grantAllInCategory(hero, CAT_CHEAT));
        p.line(Messages.get(DebugSupply.class, "cheat_items_line"));
        appendItemOptions(p, hero, CAT_CHEAT);
    }

    private void appendItemOptions(WndGeneral.PaneBuilder p, Hero hero, String categoryKey) {
        List<Supplier<Item>> items = categories.get(categoryKey);
        if (items == null) {
            return;
        }
        for (Supplier<Item> supplier : items) {
            final Supplier<Item> sup = supplier;
            Item sample = sup.get();
            String label = sample != null ? sample.name() : "?";
            p.option(label, () -> grantItem(hero, sup));
        }
    }

    private void grantAllInCategory(Hero hero, String categoryKey) {
        List<Supplier<Item>> items = categories.get(categoryKey);
        if (items == null || items.isEmpty()) return;
        for (Supplier<Item> supplier : items) {
            Item item = supplier.get();
            if (item != null) {
                item.identify();
                if (item.collect()) {
                    tryEquipWeapon(hero, item);
                }
            }
        }
        if (onOpen != null) onOpen.get();
    }

    private void grantItem(Hero hero, Supplier<Item> supplier) {
        Item item = supplier.get();
        if (item != null) {
            item.identify();
            if (item.collect()) {
                tryEquipWeapon(hero, item);
            }
        }
        if (onOpen != null) {
            onOpen.get();
        }
    }

    /** 调试发放的武器默认装进背包；对可装备武器再穿上，物品栏武器槽才会显示图标。 */
    private static void tryEquipWeapon(Hero hero, Item item) {
        if (!(item instanceof KindOfWeapon)) return;
        KindOfWeapon kw = (KindOfWeapon) item;
        if (hero.belongings.contains(kw) && kw.quantity() > 0) {
            kw.doEquip(hero);
            return;
        }
        for (Item it : hero.belongings.backpack.items) {
            if (it != null && kw.getClass() == it.getClass() && it instanceof KindOfWeapon && it.quantity() > 0) {
                ((KindOfWeapon) it).doEquip(hero);
                return;
            }
        }
    }

    private interface DebugBulletLoader {
        boolean applyDebugBullet(FirearmBullet bullet);
    }

    private interface DebugASpecialControl {
        void addSpecialAmmo(int add);
        int specialAmmo();
        void setSpecialMode(boolean enabled);
        boolean specialMode();
    }

    private static abstract class DebugAFirearm extends FirearmWeapon implements DebugBulletLoader, DebugASpecialControl {
        protected FirearmMagazine.SwitchMagazine mag = new FirearmMagazine.SwitchMagazine();
        {
            magazine = mag;
        }
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (magazine instanceof FirearmMagazine.SwitchMagazine) mag = (FirearmMagazine.SwitchMagazine) magazine;
            else {
                mag = new FirearmMagazine.SwitchMagazine();
                magazine = mag;
            }
        }
        @Override public boolean applyDebugBullet(FirearmBullet bullet) {
            if (bullet == null) return false;
            mag.setSpecialBullet(bullet);
            mag.setSpecialMode(true);
            com.zootdungeon.utils.GLog.i("已设置 A 型特殊弹为: " + bullet.displayName);
            return true;
        }
        @Override public void addSpecialAmmo(int add) { mag.addSpecialAmmo(add); }
        @Override public int specialAmmo() { return mag.specialAmmo(); }
        @Override public void setSpecialMode(boolean enabled) { mag.setSpecialMode(enabled); }
        @Override public boolean specialMode() { return mag.specialMode(); }
    }

    private static abstract class DebugBFirearm extends FirearmWeapon implements DebugBulletLoader {
        protected FirearmMagazine.QueueMagazine mag = new FirearmMagazine.QueueMagazine();
        {
            magazine = mag;
        }
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (magazine instanceof FirearmMagazine.QueueMagazine) mag = (FirearmMagazine.QueueMagazine) magazine;
            else {
                mag = new FirearmMagazine.QueueMagazine();
                magazine = mag;
            }
        }
        @Override protected String reloadActionName() { return "装弹"; }
        @Override public boolean applyDebugBullet(FirearmBullet bullet) {
            boolean ok = mag.loadRound(bullet);
            if (!ok) com.zootdungeon.utils.GLog.w("B 型队列已满。");
            return ok;
        }
    }

    private static abstract class DebugCFirearm extends FirearmWeapon implements DebugBulletLoader {
        private static final String AC_EJECT_MAG = "EJECT_MAG";

        protected FirearmMagazine.SwapMagazine mag = new FirearmMagazine.SwapMagazine();

        {
            magazine = mag;
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (magazine instanceof FirearmMagazine.SwapMagazine) mag = (FirearmMagazine.SwapMagazine) magazine;
            else {
                mag = new FirearmMagazine.SwapMagazine();
                magazine = mag;
            }
        }

        @Override
        protected String reloadActionName() {
            return "换弹匣";
        }

        @Override
        public ArrayList<String> actions(Hero hero) {
            ArrayList<String> actions = super.actions(hero);
            if (isEquipped(hero)) {
                actions.add(AC_EJECT_MAG);
            }
            return actions;
        }

        @Override
        public String actionName(String action, Hero hero) {
            if (AC_EJECT_MAG.equals(action)) return "退弹匣";
            return super.actionName(action, hero);
        }

        @Override
        public void execute(Hero hero, String action) {
            if (AC_EJECT_MAG.equals(action)) {
                ejectMagazine(hero);
                return;
            }
            super.execute(hero, action);
        }

        private void ejectMagazine(Hero hero) {
            if (mag.activeMagazine == null) {
                com.zootdungeon.utils.GLog.w("当前没有装入弹匣。");
                return;
            }
            FirearmMagazine out = mag.activeMagazine;
            mag.activeMagazine = null;
            if (!out.collect(hero.belongings.backpack)) {
                Dungeon.level.drop(out, hero.pos).sprite.drop(hero.pos);
                com.zootdungeon.utils.GLog.w("背包已满，弹匣掉落在地。");
            } else {
                com.zootdungeon.utils.GLog.i("已退出弹匣。");
            }
            hero.spendAndNext(0.5f);
        }

        @Override
        protected void reload(Hero hero) {
            GameScene.selectItem(new WndBag.ItemSelector() {
                @Override
                public String textPrompt() {
                    return "选择要换上的弹匣";
                }

                @Override
                public boolean itemSelectable(Item item) {
                    return item != null && item.getClass() == FirearmMagazine.class;
                }

                @Override
                public void onSelect(Item item) {
                    if (!(item instanceof FirearmMagazine)) return;
                    FirearmMagazine selected = (FirearmMagazine) item.detachAll(hero.belongings.backpack);
                    FirearmMagazine old = mag.activeMagazine;
                    mag.activeMagazine = selected;
                    if (old != null) old.collect(hero.belongings.backpack);
                    com.zootdungeon.utils.GLog.i("已更换弹匣。");
                    hero.spendAndNext(0.8f);
                }
            });
        }

        @Override
        public boolean applyDebugBullet(FirearmBullet bullet) {
            if (mag.activeMagazine == null) return false;
            boolean ok = mag.activeMagazine.loadRound(bullet);
            if (!ok) {
                com.zootdungeon.utils.GLog.w("当前弹匣已满，或其中已有不同类型的子弹。");
            }
            return ok;
        }
    }

    // Debug-only: concrete guns are colocated here. A/B/C differences are magazine models only.

    public static class A_Pistol extends DebugAFirearm implements FirearmWeapon.FirearmTraitPistol {
        {
            tier = 1;
            maxRange = 7;
            DLY = 0.95f;
            gunDamageMult = 0.9f;
            mag.defaultBullet = FirearmBullet.Presets.fmj();
            mag.specialBullet = FirearmBullet.Presets.incendiary();
        }
        @Override public String name() { return "A-手枪"; }
        @Override public String desc() { return "常规输出偏弱；特殊弹药用于爆发。手枪近战更强，远距离衰减明显。"; }
    }

    public static class A_Shotgun extends DebugAFirearm implements FirearmWeapon.FirearmTraitShotgun {
        {
            tier = 1;
            maxRange = 6;
            DLY = 1.15f;
            gunDamageMult = 1.0f;
            mag.defaultBullet = FirearmBullet.Presets.buckshot();
            mag.specialBullet = FirearmBullet.Presets.incendiary();
        }
        @Override public int pelletCount() { return 7; }
        @Override public String name() { return "A-霰弹枪"; }
        @Override public String desc() { return "近距离范围大并带推击；可对门开火并伤及门后目标；弹丸可按子弹规则反弹。"; }
    }

    public static class A_Rifle extends DebugAFirearm {
        {
            tier = 3;
            maxRange = 9;
            DLY = 1.05f;
            gunDamageMult = 1.05f;
            burstCount = 3;
            autoCount = 5;
            triggerMode = TriggerMode.BURST;
            mag.defaultBullet = FirearmBullet.Presets.fmj();
            mag.specialBullet = FirearmBullet.Presets.incendiary();
        }
        @Override public String name() { return "A-步枪"; }
        @Override public String desc() { return "可单发/三发/连发（取决于模式）。"; }
    }

    public static class A_Sniper extends DebugAFirearm implements FirearmWeapon.FirearmTraitAimable {
        {
            tier = 4;
            maxRange = 12;
            DLY = 1.25f;
            gunDamageMult = 1.15f;
            mag.defaultBullet = FirearmBullet.Presets.fmj();
            mag.specialBullet = FirearmBullet.Presets.incendiary();
        }
        @Override public int aimCap() { return 6; }
        @Override public float aimDamagePerLayer() { return 0.25f; }
        @Override public float aimAccuracyPerLayer() { return 0.2f; }
        @Override public boolean keepHalfAimOnMove() { return true; }
        @Override public String name() { return "A-狙击枪"; }
        @Override public String desc() { return "提供瞄准动作；移动会清零/减半瞄准层；开火一次消耗全部瞄准层，瞄得越久越强越准。"; }
    }

    public static class B_Pistol extends DebugBFirearm implements FirearmWeapon.FirearmTraitPistol {
        {
            tier = 1;
            maxRange = 7;
            DLY = 0.9f;
            gunDamageMult = 0.9f;
            mag.queueCapacity = 6;
            mag.reloadTemplate = FirearmBullet.Presets.fmj();
        }
        @Override public String name() { return "B-手枪"; }
        @Override public String desc() { return "FIFO 队列装填，小容量频繁装弹；可以混装不同子弹。"; }
    }

    public static class B_Shotgun extends DebugBFirearm implements FirearmWeapon.FirearmTraitShotgun {
        {
            tier = 2;
            maxRange = 6;
            DLY = 1.1f;
            gunDamageMult = 1.0f;
            mag.queueCapacity = 3;
            mag.reloadTemplate = FirearmBullet.Presets.buckshot();
        }
        @Override public int pelletCount() { return 7; }
        @Override public String name() { return "B-霰弹枪"; }
        @Override public String desc() { return "队列装填霰弹，容量小；每发弹丸独立轨迹，反弹次数看子弹。"; }
    }

    public static class B_Rifle extends DebugBFirearm {
        {
            tier = 3;
            maxRange = 9;
            DLY = 1.0f;
            gunDamageMult = 1.05f;
            mag.queueCapacity = 9;
            burstCount = 3;
            autoCount = 5;
            triggerMode = TriggerMode.BURST;
            mag.reloadTemplate = FirearmBullet.Presets.fmj();
        }
        @Override public String name() { return "B-步枪"; }
        @Override public String desc() { return "队列装填步枪，可切换三发/连发。"; }
    }

    public static class B_Sniper extends DebugBFirearm implements FirearmWeapon.FirearmTraitAimable {
        {
            tier = 4;
            maxRange = 12;
            DLY = 1.2f;
            gunDamageMult = 1.15f;
            mag.queueCapacity = 4;
            mag.reloadTemplate = FirearmBullet.Presets.incendiary();
        }
        @Override public int aimCap() { return 6; }
        @Override public float aimDamagePerLayer() { return 0.25f; }
        @Override public float aimAccuracyPerLayer() { return 0.2f; }
        @Override public boolean keepHalfAimOnMove() { return true; }
        @Override public String name() { return "B-狙击枪"; }
        @Override public String desc() { return "瞄准叠层后开火消耗全部层数；队列装填小容量，适合精确射击与子弹混装测试。"; }
    }

    public static class C_Pistol extends DebugCFirearm implements FirearmWeapon.FirearmTraitPistol {
        {
            tier = 1;
            maxRange = 7;
            DLY = 0.92f;
            gunDamageMult = 0.9f;
            mag.activeMagazine = new FirearmMagazine("c_pistol_mag", FirearmBullet.Presets.fmj(), 8, 8);
        }
        @Override public String name() { return "C-手枪"; }
        @Override public String desc() { return "弹匣即时切换；弹匣内单一弹种。"; }
    }

    public static class C_Shotgun extends DebugCFirearm implements FirearmWeapon.FirearmTraitShotgun {
        {
            tier = 2;
            maxRange = 6;
            DLY = 1.12f;
            gunDamageMult = 1.0f;
            mag.activeMagazine = new FirearmMagazine("c_shot_mag", FirearmBullet.Presets.buckshot(), 4, 4);
        }
        @Override public int pelletCount() { return 8; }
        @Override public String name() { return "C-霰弹枪"; }
        @Override public String desc() { return "弹匣即时切换霰弹；弹丸可按子弹规则反弹并可破门伤及门后。"; }
    }

    public static class C_Rifle extends DebugCFirearm {
        {
            tier = 3;
            maxRange = 9;
            DLY = 1.02f;
            gunDamageMult = 1.05f;
            burstCount = 3;
            autoCount = 5;
            triggerMode = TriggerMode.BURST;
            mag.activeMagazine = new FirearmMagazine("c_rifle_mag", FirearmBullet.Presets.fmj(), 30, 30);
        }
        @Override public String name() { return "C-步枪"; }
        @Override public String desc() { return "以弹匣为单位换弹并可即时切换；弹匣内不混弹。"; }
    }

    public static class C_Sniper extends DebugCFirearm implements FirearmWeapon.FirearmTraitAimable {
        {
            tier = 4;
            maxRange = 12;
            DLY = 1.22f;
            gunDamageMult = 1.15f;
            mag.activeMagazine = new FirearmMagazine("c_snipe_mag", FirearmBullet.Presets.incendiary(), 5, 5);
        }
        @Override public int aimCap() { return 6; }
        @Override public float aimDamagePerLayer() { return 0.25f; }
        @Override public float aimAccuracyPerLayer() { return 0.2f; }
        @Override public boolean keepHalfAimOnMove() { return true; }
        @Override public String name() { return "C-狙击枪"; }
        @Override public String desc() { return "瞄准叠层越久越强越准；弹匣即时切换；弹匣内不混弹。"; }
    }
}
