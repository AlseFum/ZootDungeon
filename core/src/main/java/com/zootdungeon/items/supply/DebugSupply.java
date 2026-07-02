package com.zootdungeon.items.supply;

import com.zootdungeon.arknights.ascalon.AscalonAmbush;
import com.zootdungeon.arknights.ascalon.AscalonAOE;
import com.zootdungeon.items.weapon.base.StateSwitchWeapon;
import com.zootdungeon.items.weapon.BannerWeapon;
import com.zootdungeon.items.weapon.BerserkWeapon;
import com.zootdungeon.items.weapon.BlastWeapon;
import com.zootdungeon.items.weapon.BloodWeapon;
import com.zootdungeon.items.weapon.base.InstantMechWeapon;
import com.zootdungeon.items.weapon.base.MomentumWeapon;
import com.zootdungeon.items.weapon.base.PropertyHuntingWeapon;
import com.zootdungeon.arknights.gitano.GitanoCard;
import com.zootdungeon.items.weapon.base.RangeReducedWeapon;
import com.zootdungeon.arknights.phantom.PhantomKnife;
import com.zootdungeon.items.weapon.base.TransferMechWeapon;
import com.zootdungeon.arknights.tragodia.TragodiaWand;
import com.zootdungeon.arknights.WandOfEyjafjalla;
import com.zootdungeon.arknights.necrass.NecrassCard;
import com.zootdungeon.items.weapon.Chakram;
import com.zootdungeon.items.weapon.base.TwinBlade;
import com.zootdungeon.arknights.misc.DeployablewCrossBow;
import com.zootdungeon.arknights.misc.NearRangeCrossBow;
import com.zootdungeon.arknights.misc.RhodesGauntlet;
import com.zootdungeon.arknights.SkillRecord;
import com.zootdungeon.arknights.MelanthaSword;
import com.zootdungeon.arknights.BaseballBat;
import com.zootdungeon.arknights.Baseball;
import com.zootdungeon.arknights.firearms.BlackSteelGun;
import com.zootdungeon.arknights.firearms.IberianGun;
import com.zootdungeon.arknights.firearms.LateranGun;
import com.zootdungeon.items.weapon.firearms.FirearmBullet;
import com.zootdungeon.items.weapon.firearms.FirearmMagazine;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.KindOfWeapon;
import com.zootdungeon.items.keys.CrystalKey;
import com.zootdungeon.items.keys.GoldenKey;
import com.zootdungeon.items.keys.IronKey;
import com.zootdungeon.items.keys.Key;
import com.zootdungeon.items.keys.SkeletonKey;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.journal.Notes;
import com.zootdungeon.levels.Level;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.InterlevelScene;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.windows.WndGeneral;
import com.zootdungeon.windows.WndTestLoot;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.items.cheat.CellEntityPlacer;
import com.watabou.noosa.Game;
import com.zootdungeon.items.cheat.EnemyPlacer;
import com.zootdungeon.items.cheat.MinePlacer;
import com.zootdungeon.items.cheat.MapDevicePlacer;
import com.zootdungeon.items.cheat.Codex;
import com.zootdungeon.items.cheat.DivineAnkh;
import com.zootdungeon.items.cheat.ItemRemover;
import com.zootdungeon.items.cheat.ItemEditor;
import com.zootdungeon.items.cheat.Panacea;
import com.zootdungeon.items.cheat.RedStone;
import com.zootdungeon.items.cheat.EventBusProbe;
import com.zootdungeon.items.cheat.ThrowingWeaponBox;
import com.zootdungeon.items.cheat.WandBox;
import com.zootdungeon.items.cheat.BombBox;
import com.zootdungeon.items.cheat.LevelConsole;
import com.zootdungeon.items.TengusMask;
import com.zootdungeon.items.KingsCrown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DebugSupply extends Supply {
    static {
        TextureRegistry.texture("sheet.cola.debug_bag", "cola/debug_bag.png")
                    .setArea("debug_bag", 0, 0, 32, 32);
    }
    {
        
        image = TextureRegistry.idByLabel("debug_bag");
    }
    private static final String CAT_CHEAT = "cat_cheat";
    private static final String CAT_WEAPONS = "cat_weapons";
    private static final String CAT_FIREARMS = "cat_firearms";
    private static final String CAT_SKILLS = "cat_skills";
    private static final String CAT_TESTS = "cat_tests";

    private final Map<String, List<Supplier<Item>>> categories = new LinkedHashMap<>();

    public DebugSupply() {
        super();
        name = Messages.get(DebugSupply.class, "name");
        desc = Messages.get(DebugSupply.class, "desc");


        List<Supplier<Item>> cheat = new ArrayList<>();
        cheat.add(() -> create(DivineAnkh.class, 1));
        cheat.add(() -> create(ItemRemover.class));
        cheat.add(() -> create(ItemEditor.class, 1));
        cheat.add(() -> create(LevelConsole.class, 1));
        cheat.add(() -> create(Panacea.class, 1));
        cheat.add(() -> create(Codex.class, 1));
        cheat.add(() -> create(RedStone.class, 1));
        cheat.add(() -> create(ThrowingWeaponBox.class, 1));
        cheat.add(() -> create(WandBox.class, 1));
        cheat.add(() -> create(BombBox.class, 1));
        cheat.add(() -> create(TengusMask.class, 1));
        cheat.add(() -> create(KingsCrown.class, 1));
        cheat.add(() -> create(MinePlacer.class, 1));
        cheat.add(() -> create(EnemyPlacer.class, 1));
        cheat.add(() -> create(MapDevicePlacer.class, 1));
        categories.put(CAT_CHEAT, cheat);

        List<Supplier<Item>> weapons = new ArrayList<>();
        weapons.add(() -> create(AscalonAmbush.class, 1));
        weapons.add(() -> create(AscalonAOE.class, 1));
        weapons.add(() -> create(StateSwitchWeapon.class, 1));
        weapons.add(() -> create(PhantomKnife.class, 1));
        weapons.add(() -> create(TragodiaWand.class, 1));
weapons.add(() -> create(WandOfEyjafjalla.class, 1));
        weapons.add(() -> create(RangeReducedWeapon.class, 1));
        weapons.add(() -> create(MomentumWeapon.class, 1));
        weapons.add(() -> create(PropertyHuntingWeapon.class, 1));
        weapons.add(() -> create(GitanoCard.class, 50));
        weapons.add(() -> create(NecrassCard.class, 50));
        weapons.add(() -> create(TwinBlade.class, 5));
        weapons.add(() -> create(InstantMechWeapon.class, 1));
        weapons.add(() -> create(TransferMechWeapon.class, 1));
        weapons.add(() -> create(MelanthaSword.class, 1));
        weapons.add(() -> create(BaseballBat.class, 1));
        weapons.add(() -> create(Baseball.class, 10));
        weapons.add(() -> create(Chakram.class, 1));
        weapons.add(() -> create(NearRangeCrossBow.class, 1));
        weapons.add(() -> create(DeployablewCrossBow.class, 1));
        weapons.add(() -> create(BlastWeapon.class, 1));
        weapons.add(() -> create(BloodWeapon.class, 1));
        weapons.add(() -> create(BannerWeapon.class, 1));
        weapons.add(() -> create(BerserkWeapon.class, 1));
        weapons.add(() -> createRhodesGauntlet());
        categories.put(CAT_WEAPONS, weapons);

        List<Supplier<Item>> tests = new ArrayList<>();
        tests.add(() -> create(CellEntityPlacer.class, 1));
        tests.add(() -> create(EventBusProbe.class, 1));
        categories.put(CAT_TESTS, tests);

        List<Supplier<Item>> skills = new ArrayList<>();
        skills.add(() -> new SkillRecord(com.zootdungeon.arknights.plugins.SkillSheet.SKILL_1));
        skills.add(() -> new SkillRecord(com.zootdungeon.arknights.plugins.SkillSheet.SKILL_2));
        skills.add(() -> new SkillRecord(com.zootdungeon.arknights.plugins.SkillSheet.ATTACK_UP_ALPHA));
        skills.add(() -> new SkillRecord(com.zootdungeon.arknights.plugins.SkillSheet.NEXT_ATTACK_BOOST));
        categories.put(CAT_SKILLS, skills);

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

    private static RhodesGauntlet createRhodesGauntlet() {
        return new RhodesGauntlet();
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
            } else if (CAT_TESTS.equals(key)) {
                b.tab(Messages.get(DebugSupply.class, key), p -> fillTestTab(p, hero));
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
        p.option("转职并升到20级", () -> {
            TengusMask mask = new TengusMask() {
                @Override
                public void choose(HeroSubClass way) {
                    super.choose(way);
                    hero.earnExp(99999, DebugSupply.class);
                    hero.HP = 1; 
                }
            };
            mask.identify();
            mask.execute(hero, "WEAR");
        });
        p.option(Messages.get(DebugSupply.class, "grant_all_cheat"), () -> grantAllInCategory(hero, CAT_CHEAT));
        p.option(Messages.get(DebugSupply.class, "kill_all_loot_all"), () -> killAllLootAll(hero));
        p.line(Messages.get(DebugSupply.class, "cheat_items_line"));
        appendItemOptions(p, hero, CAT_CHEAT);
    }

    private void fillTestTab(WndGeneral.PaneBuilder p, Hero hero) {
        p.option("WndGeneral 部件演示", () -> {
            WndGeneral.make()
                    .title("WndGeneral 部件测试")
                    .tab("行与开关", bp -> {
                        bp.line("===== 文本行 =====");
                        bp.line("普通文本行");
                        bp.row("带标签的值行", "当前值: 42");
                        bp.line("===== 开关（不自动关闭窗口）=====");
                        bp.switchRow("调试模式", false, on -> GLog.p("调试模式: " + on));
                        bp.switchRow("简化界面", true, on -> GLog.p("简化界面: " + on));
                    })
                    .tab("按钮与选项", bp -> {
                        bp.line("===== 选项（点击后关闭）=====");
                        bp.option("选项 A（关闭）", () -> GLog.p("点击了选项 A"));
                        bp.option("选项 B（关闭）", () -> GLog.p("点击了选项 B"));
                        bp.line("===== 底部按钮 =====");
                        bp.button("确定", () -> GLog.p("点击了确定按钮"));
                    })
                    .tab("横向布局", bp -> {
                        bp.line("===== 横向单元格 =====");
                        bp.hrow(r -> r.line("标签").line("值"));
                        bp.hrow(r -> r.line("A").line("B").line("C"));
                        bp.hrow(r -> r.button("左", () -> GLog.p("左"))
                                .button("中", () -> GLog.p("中"))
                                .button("右", () -> GLog.p("右")));
                    })
                    .tab("输入框", bp -> {
                        bp.line("===== 输入框（弹出 WndTextInput）=====");
                        bp.inputRow("输入名字", "默认", 20, t -> GLog.p("名字: " + t));
                        bp.inputRow("输入数值", "100", 10, t -> GLog.p("数值: " + t));
                        bp.line("点击按钮后会弹出文本输入窗口");
                    })
                    .tab("长内容滚动", bp -> {
                        bp.line("===== 滚动测试 =====");
                        for (int i = 0; i < 30; i++) {
                            bp.line("第 " + i + " 行，用于测试垂直滚动区域");
                        }
                    })
                    
                    .show();
        });
        p.option("Loot 系统测试", () -> GameScene.show(new WndTestLoot()));
        appendItemOptions(p, hero, CAT_TESTS);
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

    /**
     * 清场搜刮：反复执行「击杀敌人 → 收集钥匙 → 开门 → 开宝箱」直到无新进展，
     * 最后拾取所有掉落物并前往下一层。
     */
    private void killAllLootAll(Hero hero) {
        int killed = 0, keysCollected = 0, doorsOpened = 0, chestsOpened = 0, itemsCollected = 0;

        boolean changed;
        do {
            changed = false;

            // Phase 1: 击杀所有敌人（新区域可能藏着新敌人）
            int roundKilled = 0;
            for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) {
                if (mob.isAlive() && mob.alignment == Char.Alignment.ENEMY) {
                    mob.HP = 1;
                    mob.damage(99999, hero);
                    roundKilled++;
                }
            }
            if (roundKilled > 0) { killed += roundKilled; changed = true; }

            // Phase 2: 收集地上所有钥匙
            int roundKeys = 0;
            for (Heap heap : new ArrayList<>(Dungeon.level.heaps.valueList())) {
                if (heap == null || heap.items.isEmpty()) continue;
                java.util.List<Item> keyItems = new ArrayList<>();
                for (Item item : heap.items) {
                    if (item instanceof Key) {
                        keyItems.add(item);
                    }
                }
                for (Item key : keyItems) {
                    heap.items.remove(key);
                    if (((Key) key).doPickUp(hero, heap.pos)) {
                        roundKeys++;
                    }
                }
                if (heap.items.isEmpty()) {
                    heap.destroy();
                } else if (heap.sprite != null) {
                    heap.sprite.view(heap).place(heap.pos);
                }
            }
            if (roundKeys > 0) { keysCollected += roundKeys; changed = true; }

            // Phase 3: 使用钥匙开门（LOCKED_DOOR / CRYSTAL_DOOR / LOCKED_EXIT）
            int roundDoors = 0;
            for (int cell = 0; cell < Dungeon.level.length(); cell++) {
                int terrain = Dungeon.level.map[cell];
                if (terrain == Terrain.LOCKED_DOOR) {
                    if (Notes.keyCount(new IronKey(Dungeon.depth)) > 0) {
                        Notes.remove(new IronKey(Dungeon.depth));
                        Level.set(cell, Terrain.DOOR);
                        GameScene.updateMap(cell);
                        roundDoors++;
                    }
                } else if (terrain == Terrain.CRYSTAL_DOOR) {
                    if (Notes.keyCount(new CrystalKey(Dungeon.depth)) > 0) {
                        Notes.remove(new CrystalKey(Dungeon.depth));
                        Level.set(cell, Terrain.EMPTY);
                        GameScene.updateMap(cell);
                        roundDoors++;
                    }
                } else if (terrain == Terrain.LOCKED_EXIT) {
                    if (Notes.keyCount(new SkeletonKey(Dungeon.depth)) > 0) {
                        Notes.remove(new SkeletonKey(Dungeon.depth));
                        Level.set(cell, Terrain.UNLOCKED_EXIT);
                        GameScene.updateMap(cell);
                        roundDoors++;
                    }
                }
            }
            if (roundDoors > 0) { doorsOpened += roundDoors; changed = true; }

            // Phase 4: 使用钥匙开宝箱
            int roundChests = 0;
            for (Heap heap : new ArrayList<>(Dungeon.level.heaps.valueList())) {
                if (heap == null) continue;
                boolean opened = false;
                if (heap.type == Heap.Type.LOCKED_CHEST) {
                    if (Notes.keyCount(new GoldenKey(Dungeon.depth)) > 0) {
                        Notes.remove(new GoldenKey(Dungeon.depth));
                        heap.open(hero);
                        opened = true;
                    }
                } else if (heap.type == Heap.Type.CRYSTAL_CHEST) {
                    if (Notes.keyCount(new CrystalKey(Dungeon.depth)) > 0) {
                        Notes.remove(new CrystalKey(Dungeon.depth));
                        heap.open(hero);
                        opened = true;
                    }
                } else if (heap.type == Heap.Type.CHEST) {
                    heap.open(hero);
                    opened = true;
                }
                if (opened) roundChests++;
            }
            if (roundChests > 0) { chestsOpened += roundChests; changed = true; }

        } while (changed);

        // Phase 5: 拾取所有地面掉落物（使用 doPickUp 触发 Gold/EnergyCrystal/Dewdrop 等的 onPickUp）
        // 排除商店物品、未开启的宝箱/墓碑/骸骨等
        for (Heap heap : new ArrayList<>(Dungeon.level.heaps.valueList())) {
            if (heap == null || heap.items.isEmpty()) continue;
            if (heap.type == Heap.Type.FOR_SALE
                    || heap.type == Heap.Type.LOCKED_CHEST
                    || heap.type == Heap.Type.CRYSTAL_CHEST
                    || heap.type == Heap.Type.CHEST
                    || heap.type == Heap.Type.TOMB
                    || heap.type == Heap.Type.SKELETON
                    || heap.type == Heap.Type.REMAINS) continue;
            java.util.List<Item> pending = new ArrayList<>();
            Item item;
            while ((item = heap.pickUp()) != null) {
                pending.add(item);
            }
            for (Item it : pending) {
                it.doPickUp(hero, heap.pos);
                itemsCollected++;
            }
        }

        GLog.p(Messages.get(DebugSupply.class, "kill_all_loot_all_result",
                killed, keysCollected, doorsOpened, chestsOpened, itemsCollected));

        // 搜刮完毕，前往下一层
        Level.beforeTransition();
        InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
        Game.switchScene(InterlevelScene.class);
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
