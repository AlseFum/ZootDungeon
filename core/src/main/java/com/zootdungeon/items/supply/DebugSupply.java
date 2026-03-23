package com.zootdungeon.items.supply;

import com.zootdungeon.arknights.ascalon.AscalonAmbush;
import com.zootdungeon.arknights.ascalon.AscalonAOE;
import com.zootdungeon.arknights.StateSwitchWeapon;
import com.zootdungeon.arknights.InstantMechWeapon;
import com.zootdungeon.arknights.MomentumWeapon;
import com.zootdungeon.arknights.PropertyHuntingWeapon;
import com.zootdungeon.arknights.gitano.GitanoCard;
import com.zootdungeon.arknights.RangeReducedWeapon;
import com.zootdungeon.arknights.SummoningAmbushWeapon;
import com.zootdungeon.arknights.TransferMechWeapon;
import com.zootdungeon.arknights.WandOfPrisonCage;
import com.zootdungeon.arknights.necrass.SummoningThrowingWeapon;
import com.zootdungeon.arknights.TwinBlade;
import com.zootdungeon.arknights.misc.RhodesStandardWeaponSupply;
import com.zootdungeon.arknights.MainTheme.SkullShattererWeapon;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndGeneral;
import com.zootdungeon.items.cheat.Codex;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.items.cheat.ItemRemover;
import com.zootdungeon.items.cheat.ItemEditor;
import com.zootdungeon.items.cheat.Panacea;
import com.zootdungeon.items.cheat.RedStone;
import com.zootdungeon.items.cheat.ThrowingWeaponBox;
import com.zootdungeon.items.cheat.WandBox;
import com.zootdungeon.items.cheat.BombBox;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.items.potions.PotionOfInvisibility;
import com.zootdungeon.items.potions.PotionOfStrength;
import com.zootdungeon.items.potions.exotic.PotionOfShroudingFog;
import com.zootdungeon.items.scrolls.ScrollOfIdentify;
import com.zootdungeon.items.scrolls.ScrollOfUpgrade;
import com.zootdungeon.items.cheat.StoneOfDungeonTravel;
import com.zootdungeon.items.cheat.StoneOfLevelSelect;
import com.zootdungeon.items.cheat.StoneOfSpawn;
import com.zootdungeon.items.TengusMask;
import com.zootdungeon.items.KingsCrown;
import com.zootdungeon.plants.Swiftthistle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DebugSupply extends Supply {
    {
        SpriteRegistry.registerItemTexture("cola/debug_bag.png", 32)
                .label("debug_bag");
        image = SpriteRegistry.itemByName("debug_bag");
    }

    private static final String CAT_POTIONS = "cat_potions";
    private static final String CAT_STONES = "cat_stones";
    private static final String CAT_CHEAT = "cat_cheat";
    private static final String CAT_WEAPONS = "cat_weapons";
    private static final String CAT_PLANTS = "cat_plants";

    private final Map<String, List<Supplier<Item>>> categories = new LinkedHashMap<>();

    public DebugSupply() {
        super();
        name = Messages.get(DebugSupply.class, "name");
        desc = Messages.get(DebugSupply.class, "desc");

        List<Supplier<Item>> potions = new ArrayList<>();
        potions.add(() -> create(PotionOfHealing.class, 20));
        potions.add(() -> create(ScrollOfIdentify.class, 20));
        potions.add(() -> create(PotionOfStrength.class, 20));
        potions.add(() -> create(PotionOfInvisibility.class, 20));
        potions.add(() -> create(PotionOfShroudingFog.class, 20));
        potions.add(() -> create(ScrollOfUpgrade.class, 100));
        categories.put(CAT_POTIONS, potions);

        List<Supplier<Item>> stones = new ArrayList<>();
        stones.add(() -> create(StoneOfSpawn.class, 300));
        stones.add(() -> create(StoneOfLevelSelect.class, 50));
        categories.put(CAT_STONES, stones);

        List<Supplier<Item>> cheat = new ArrayList<>();
        cheat.add(() -> create(DivineAnkh.class, 1));
        cheat.add(() -> create(ItemRemover.class));
        cheat.add(() -> create(ItemEditor.class, 1));
        cheat.add(() -> create(StoneOfDungeonTravel.class, 1));
        cheat.add(() -> create(Panacea.class, 1));
        cheat.add(() -> create(Codex.class, 1));
        cheat.add(() -> create(RedStone.class, 1));
        cheat.add(() -> create(ThrowingWeaponBox.class, 1));
        cheat.add(() -> create(WandBox.class, 1));
        cheat.add(() -> create(BombBox.class, 1));
        cheat.add(() -> create(TengusMask.class, 1));
        cheat.add(() -> create(KingsCrown.class, 1));
        categories.put(CAT_CHEAT, cheat);

        List<Supplier<Item>> weapons = new ArrayList<>();
        weapons.add(() -> create(AscalonAmbush.class, 1));
        weapons.add(() -> create(AscalonAOE.class, 1));
        weapons.add(() -> create(StateSwitchWeapon.class, 1));
        weapons.add(() -> create(SummoningAmbushWeapon.class, 1));
        weapons.add(() -> create(WandOfPrisonCage.class, 1));
        weapons.add(() -> create(RangeReducedWeapon.class, 1));
        weapons.add(() -> create(MomentumWeapon.class, 1));
        weapons.add(() -> create(PropertyHuntingWeapon.class, 1));
        weapons.add(() -> create(RhodesStandardWeaponSupply.class, 1));
        weapons.add(() -> create(SummoningThrowingWeapon.class, 50));
        weapons.add(() -> create(TwinBlade.class, 5));
        weapons.add(() -> create(InstantMechWeapon.class, 1));
        weapons.add(() -> create(TransferMechWeapon.class, 1));
        weapons.add(() -> create(SkullShattererWeapon.class, 1));
        categories.put(CAT_WEAPONS, weapons);

        List<Supplier<Item>> plants = new ArrayList<>();
        plants.add(() -> create(Swiftthistle.Seed.class, 100));
        plants.add(() -> create(GitanoCard.class, 50));
        categories.put(CAT_PLANTS, plants);
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

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals("启用")) {
            showCategorySelection(hero);
        } else {
            super.execute(hero, action);
        }
    }

    private void showCategorySelection(Hero hero) {
        WndGeneral.Builder b = WndGeneral.make().title(Messages.get(DebugSupply.class, "name"));
        for (String catKey : categories.keySet()) {
            final String key = catKey;
            if (CAT_CHEAT.equals(key)) {
                b.option(Messages.get(DebugSupply.class, key), () -> grantAllInCategory(hero, key));
            } else {
                b.option(Messages.get(DebugSupply.class, key), () -> showItemSelection(hero, key));
            }
        }
        b.show();
    }

    private void grantAllInCategory(Hero hero, String categoryKey) {
        List<Supplier<Item>> items = categories.get(categoryKey);
        if (items == null || items.isEmpty()) return;
        for (Supplier<Item> supplier : items) {
            Item item = supplier.get();
            if (item != null) item.identify().collect();
        }
        if (onOpen != null) onOpen.get();
    }

    private void showItemSelection(Hero hero, String categoryKey) {
        List<Supplier<Item>> items = categories.get(categoryKey);
        if (items == null || items.isEmpty()) return;

        WndGeneral.Builder b = WndGeneral.make()
                .title(Messages.get(DebugSupply.class, categoryKey));
        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            Item sample = items.get(i).get();
            String label = sample != null ? sample.name() : "?";
            b.option(label, () -> grantItem(hero, items.get(idx)));
        }
        b.show();
    }

    private void grantItem(Hero hero, Supplier<Item> supplier) {
        Item item = supplier.get();
        if (item != null) {
            item.identify().collect();
        }
        if (onOpen != null) {
            onOpen.get();
        }
        // 不调用 detach，物品保留在背包
    }
}
