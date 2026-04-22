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
import com.zootdungeon.arknights.misc.RhodesArmorPierceGauntlet;
import com.zootdungeon.arknights.misc.RhodesChargeGauntlet;
import com.zootdungeon.arknights.misc.RhodesCritGauntlet;
import com.zootdungeon.arknights.misc.RhodesDefBreakerGauntlet;
import com.zootdungeon.arknights.misc.RhodesDodgeGauntlet;
import com.zootdungeon.arknights.misc.RhodesKnockbackGauntlet;
import com.zootdungeon.arknights.misc.RhodesLockOnGauntlet;
import com.zootdungeon.arknights.RhodesStandardWeapons.RhodesStandardWeaponSupply;
import com.zootdungeon.arknights.MainTheme.SkullShattererWeapon;
import com.zootdungeon.arknights.firearms.BlackSteelGun;
import com.zootdungeon.arknights.firearms.IberianGun;
import com.zootdungeon.arknights.firearms.LateranGun;
import com.zootdungeon.items.weapon.firearms.FirearmBullet;
import com.zootdungeon.items.weapon.firearms.FirearmMagazine;
import com.zootdungeon.arknights.plugins.DefenseBoostPlugin;
import com.zootdungeon.arknights.plugins.MetabolismOverclockPlugin;
import com.zootdungeon.arknights.plugins.NextAttackCostRefundPlugin;
import com.zootdungeon.arknights.plugins.NextAttackDamageBoostPlugin;
import com.zootdungeon.arknights.plugins.OverclockedStrikesPlugin;
import com.zootdungeon.arknights.plugins.OverclockedStrikesPlusPlugin;
import com.zootdungeon.arknights.plugins.PullEnemyPlugin;
import com.zootdungeon.arknights.plugins.ReachBoostPlugin;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndGeneral;
import com.zootdungeon.items.cheat.CellEntityPlacer;
import com.zootdungeon.items.cheat.Codex;
import com.zootdungeon.items.cheat.DivineAnkh;
import com.zootdungeon.items.cheat.ItemRemover;
import com.zootdungeon.items.cheat.StackingBuffTester;
import com.zootdungeon.items.cheat.ItemEditor;
import com.zootdungeon.items.cheat.Panacea;
import com.zootdungeon.items.cheat.RedStone;
import com.zootdungeon.items.cheat.EventBusProbe;
import com.zootdungeon.items.cheat.ThrowingWeaponBox;
import com.zootdungeon.items.cheat.WandBox;
import com.zootdungeon.items.cheat.BombBox;
import com.zootdungeon.items.cheat.StoneOfDungeonTravel;
import com.zootdungeon.items.cheat.StoneOfLevelSelect;
import com.zootdungeon.items.TengusMask;
import com.zootdungeon.items.KingsCrown;

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
    private static final String CAT_RHODES_GAUNTLETS = "cat_rhodes_gauntlets";
    private static final String CAT_CELL_ENTITIES = "cat_cell_entities";
    private static final String CAT_BUFF_TESTS = "cat_buff_tests";

    private final Map<String, List<Supplier<Item>>> categories = new LinkedHashMap<>();

    public DebugSupply() {
        super();
        name = Messages.get(DebugSupply.class, "name");
        desc = Messages.get(DebugSupply.class, "desc");


        List<Supplier<Item>> stones = new ArrayList<>();
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

        List<Supplier<Item>> rhodesGauntlets = new ArrayList<>();
        rhodesGauntlets.add(() -> create(RhodesLockOnGauntlet.class, 1));
        rhodesGauntlets.add(() -> create(RhodesArmorPierceGauntlet.class, 1));
        rhodesGauntlets.add(() -> create(RhodesDodgeGauntlet.class, 1));
        rhodesGauntlets.add(() -> create(RhodesKnockbackGauntlet.class, 1));
        rhodesGauntlets.add(() -> create(RhodesCritGauntlet.class, 1));
        rhodesGauntlets.add(() -> create(RhodesChargeGauntlet.class, 1));
        rhodesGauntlets.add(() -> create(RhodesDefBreakerGauntlet.class, 1));
        categories.put(CAT_RHODES_GAUNTLETS, rhodesGauntlets);

        List<Supplier<Item>> cellEntities = new ArrayList<>();
        cellEntities.add(() -> create(CellEntityPlacer.class, 1));
        categories.put(CAT_CELL_ENTITIES, cellEntities);

        List<Supplier<Item>> buffTests = new ArrayList<>();
        buffTests.add(() -> create(StackingBuffTester.class, 1));
        categories.put(CAT_BUFF_TESTS, buffTests);

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
        p.line("3 把枪：A/B/C 三种装填机制，对应 Lateran / Iberian / BlackSteel。");

        p.hrow(r -> r.button("LateranGun", () -> grantItem(hero, () -> create(LateranGun.class, 1)))
                .button("IberianGun", () -> grantItem(hero, () -> create(IberianGun.class, 1)))
                .button("BlackSteelGun", () -> grantItem(hero, () -> create(BlackSteelGun.class, 1))));

        p.line("");
        p.line("弹匣：直接发到背包，供 C 类枪械更换。");
        p.hrow(r -> r.button("手枪弹匣", () -> grantItem(hero, DebugSupply::createCPistolMagazine))
                .button("霰弹弹匣", () -> grantItem(hero, DebugSupply::createCShotgunMagazine))
                .button("步枪弹匣", () -> grantItem(hero, DebugSupply::createCRifleMagazine)));
        p.hrow(r -> r.button("狙击弹匣", () -> grantItem(hero, DebugSupply::createCSniperMagazine)));

        p.line("");
        p.line("子弹：仅保留 3 种基础调试弹（直接发放物品）。燃烧弹会在命中格/碰撞格生成火焰。");
        p.hrow(r -> r.button("FMJ×50", () -> grantItem(hero, () -> FirearmBullet.Presets.fmj().quantity(50)))
                .button("鹿弹×50", () -> grantItem(hero, () -> FirearmBullet.Presets.buckshot().quantity(50)))
                .button("燃烧×50", () -> grantItem(hero, () -> FirearmBullet.Presets.incendiary().quantity(50))));
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
}
