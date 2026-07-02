package com.zootdungeon.items;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.artifacts.Artifact;
import com.zootdungeon.items.food.Food;
import com.zootdungeon.items.potions.Potion;
import com.zootdungeon.items.rings.Ring;
import com.zootdungeon.items.scrolls.Scroll;
import com.zootdungeon.items.stones.Runestone;
import com.zootdungeon.items.trinkets.Trinket;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.base.MeleeWeapon;
import com.zootdungeon.items.weapon.base.MissileWeapon;
import com.zootdungeon.plants.Plant;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

// ============================================================
// 新版 Loot 系统
//
// 架构：
//   LootRegistry          ← 统一入口
//   ├── LootTable         ← 顶层掉落表（按 id 注册）
//   │   └── LootPool[]    ← 奖池列表
//   │       └── LootEntry[]   ← 条目列表
//   │           ├── ItemEntry
//   │           ├── CategoryPoolEntry    ← Generator.Category 的替代
//   │           ├── TableEntry           ← 递归子表
//   │           └── EmptyEntry
//   ├── CategoryPool      ← Generator.Category 的替代（Deck 机制内置）
//   ├── LootArgs          ← 每次抽取的上下文（只读快照）
//   └── LootHistory       ← 持久化状态（Deck 状态/种子/计数）
// ============================================================

public final class LootRegistry {

    // ============================================================
    // 注册表
    // ============================================================

    public static final Map<String, LootTable> TABLES = new LinkedHashMap<>();
    public static final Map<String, CategoryPool> CATEGORIES = new LinkedHashMap<>();

    // 全局 Artifact 去重集合（不依赖 LootHistory，可独立于 Dungeon 使用）
    private static final Set<String> extractedArtifacts = new HashSet<>();

    // ============================================================
    // 注册 API
    // ============================================================

    public static void register(String id, LootTable table) {
        if (id == null || table == null) return;
        table.id = id;
        TABLES.put(id, table);
    }

    public static CategoryPool category(String key, Class<?> superClass, Supplier<Item> make) {
        return CATEGORIES.computeIfAbsent(key, k -> new CategoryPool(k, superClass, make));
    }

    // 预定义常用 Category
    public static CategoryPool WEAPON()   { return category("WEAPON", MeleeWeapon.class, null); }
    public static CategoryPool ARMOR()    { return category("ARMOR", com.zootdungeon.items.armor.Armor.class, null); }
    public static CategoryPool MISSILE()  { return category("MISSILE", MissileWeapon.class, null); }
    public static CategoryPool WAND()     { return category("WAND", Wand.class, null); }
    public static CategoryPool RING()     { return category("RING", Ring.class, null); }
    public static CategoryPool ARTIFACT() { return category("ARTIFACT", Artifact.class, null); }
    public static CategoryPool POTION()    { return category("POTION", Potion.class, null); }
    public static CategoryPool SCROLL()   { return category("SCROLL", Scroll.class, null); }
    public static CategoryPool SEED()      { return category("SEED", Plant.Seed.class, null); }
    public static CategoryPool STONE()     { return category("STONE", Runestone.class, null); }
    public static CategoryPool FOOD()      { return category("FOOD", Food.class, null); }
    public static CategoryPool TRINKET()   { return category("TRINKET", Trinket.class, null); }
    public static CategoryPool GOLD()      { return category("GOLD", com.zootdungeon.items.material.Gold.class, () -> new com.zootdungeon.items.material.Gold().random()); }

    // Tier 级别（用于 WEP_T1-T5 / MIS_T1-T5 / ARM_T1-T5）
    public static CategoryPool WEP_TIER(int tier) { return category("WEP_T" + tier, MeleeWeapon.class, null); }
    public static CategoryPool MIS_TIER(int tier) { return category("MIS_T" + tier, MissileWeapon.class, null); }
    public static CategoryPool ARM_TIER(int tier) { return category("ARM_T" + tier, com.zootdungeon.items.armor.Armor.class, null); }

    // ============================================================
    // 物品注册（从 Generator 迁移）
    // ============================================================

    static {
        // 基础类别初始化
        CategoryPool potionPool   = POTION();
        CategoryPool scrollPool   = SCROLL();
        CategoryPool seedPool     = SEED();
        CategoryPool stonePool    = STONE();
        CategoryPool wandPool     = WAND();
        CategoryPool ringPool     = RING();
        CategoryPool artifactPool = ARTIFACT();
        CategoryPool armorPool    = ARMOR();
        CategoryPool foodPool     = FOOD();
        CategoryPool trinketPool  = TRINKET();

        // 武器层级
        CategoryPool[] wepTierPools = new CategoryPool[6];
        for (int t = 1; t <= 5; t++) wepTierPools[t] = WEP_TIER(t);

        // 远程武器层级
        CategoryPool[] misTierPools = new CategoryPool[6];
        for (int t = 1; t <= 5; t++) misTierPools[t] = MIS_TIER(t);

        // 防具层级
        CategoryPool[] armTierPools = new CategoryPool[6];
        for (int t = 1; t <= 5; t++) armTierPools[t] = ARM_TIER(t);

        // ========== 药水 ==========
        potionPool.register(com.zootdungeon.items.potions.PotionOfStrength.class, 0f, 0f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfHealing.class, 3f, 3f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfMindVision.class, 2f, 2f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfFrost.class, 1f, 2f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfLiquidFlame.class, 2f, 1f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfToxicGas.class, 1f, 2f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfHaste.class, 1f, 1f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfInvisibility.class, 1f, 1f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfLevitation.class, 1f, 1f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfParalyticGas.class, 1f, 1f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfPurity.class, 1f, 1f);
        potionPool.register(com.zootdungeon.items.potions.PotionOfExperience.class, 1f, 0f);

        // ========== 种子 ==========
        seedPool.register(com.zootdungeon.plants.Rotberry.Seed.class, 0f);
        seedPool.register(com.zootdungeon.plants.Sungrass.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Fadeleaf.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Icecap.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Firebloom.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Sorrowmoss.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Swiftthistle.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Blindweed.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Stormvine.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Earthroot.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Mageroyal.Seed.class, 2f);
        seedPool.register(com.zootdungeon.plants.Starflower.Seed.class, 1f);

        // ========== 卷轴 ==========
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfUpgrade.class, 0f, 0f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfIdentify.class, 3f, 3f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfRemoveCurse.class, 2f, 2f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfMirrorImage.class, 1f, 2f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfRecharging.class, 2f, 1f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfTeleportation.class, 1f, 2f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfLullaby.class, 1f, 1f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfMagicMapping.class, 1f, 1f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfRage.class, 1f, 1f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfRetribution.class, 1f, 1f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfTerror.class, 1f, 1f);
        scrollPool.register(com.zootdungeon.items.scrolls.ScrollOfTransmutation.class, 1f, 0f);

        // ========== 石头 ==========
        stonePool.register(com.zootdungeon.items.stones.StoneOfEnchantment.class, 0f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfIntuition.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfDetectMagic.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfFlock.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfShock.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfBlink.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfDeepSleep.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfClairvoyance.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfAggression.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfBlast.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfFear.class, 2f);
        stonePool.register(com.zootdungeon.items.stones.StoneOfAugmentation.class, 0f);

        // ========== 法杖 ==========
        wandPool.register(com.zootdungeon.items.wands.WandOfMagicMissile.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfLightning.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfDisintegration.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfFireblast.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfCorrosion.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfBlastWave.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfLivingEarth.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfFrost.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfPrismaticLight.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfWarding.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfTransfusion.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfCorruption.class, 3f);
        wandPool.register(com.zootdungeon.items.wands.WandOfRegrowth.class, 3f);

        // ========== T1 武器 ==========
        wepTierPools[1].register(com.zootdungeon.items.weapon.WornShortsword.class, 2f);
        wepTierPools[1].register(com.zootdungeon.items.weapon.MagesStaff.class, 0f);
        wepTierPools[1].register(com.zootdungeon.items.weapon.Dagger.class, 2f);
        wepTierPools[1].register(com.zootdungeon.items.weapon.Gloves.class, 2f);
        wepTierPools[1].register(com.zootdungeon.items.weapon.Rapier.class, 2f);
        wepTierPools[1].register(com.zootdungeon.items.weapon.Cudgel.class, 2f);

        // ========== T2 武器 ==========
        wepTierPools[2].register(com.zootdungeon.items.weapon.Shortsword.class, 2f);
        wepTierPools[2].register(com.zootdungeon.items.weapon.HandAxe.class, 2f);
        wepTierPools[2].register(com.zootdungeon.items.weapon.Spear.class, 2f);
        wepTierPools[2].register(com.zootdungeon.items.weapon.Quarterstaff.class, 2f);
        wepTierPools[2].register(com.zootdungeon.items.weapon.Dirk.class, 2f);
        wepTierPools[2].register(com.zootdungeon.items.weapon.Sickle.class, 2f);

        // ========== T3 武器 ==========
        wepTierPools[3].register(com.zootdungeon.items.weapon.Sword.class, 2f);
        wepTierPools[3].register(com.zootdungeon.items.weapon.Mace.class, 2f);
        wepTierPools[3].register(com.zootdungeon.items.weapon.Scimitar.class, 2f);
        wepTierPools[3].register(com.zootdungeon.items.weapon.RoundShield.class, 2f);
        wepTierPools[3].register(com.zootdungeon.items.weapon.Sai.class, 2f);
        wepTierPools[3].register(com.zootdungeon.items.weapon.Whip.class, 2f);

        // ========== T4 武器 ==========
        wepTierPools[4].register(com.zootdungeon.items.weapon.Longsword.class, 2f);
        wepTierPools[4].register(com.zootdungeon.items.weapon.BattleAxe.class, 2f);
        wepTierPools[4].register(com.zootdungeon.items.weapon.Flail.class, 2f);
        wepTierPools[4].register(com.zootdungeon.items.weapon.RunicBlade.class, 2f);
        wepTierPools[4].register(com.zootdungeon.items.weapon.AssassinsBlade.class, 2f);
        wepTierPools[4].register(com.zootdungeon.items.weapon.Crossbow.class, 2f);
        wepTierPools[4].register(com.zootdungeon.items.weapon.Katana.class, 2f);

        // ========== T5 武器 ==========
        wepTierPools[5].register(com.zootdungeon.items.weapon.Greatsword.class, 2f);
        wepTierPools[5].register(com.zootdungeon.items.weapon.WarHammer.class, 2f);
        wepTierPools[5].register(com.zootdungeon.items.weapon.Glaive.class, 2f);
        wepTierPools[5].register(com.zootdungeon.items.weapon.Greataxe.class, 2f);
        wepTierPools[5].register(com.zootdungeon.items.weapon.Greatshield.class, 2f);
        wepTierPools[5].register(com.zootdungeon.items.weapon.Gauntlet.class, 2f);
        wepTierPools[5].register(com.zootdungeon.items.weapon.WarScythe.class, 2f);

        // ========== 防具（按 Tier 注册，深度过滤） ==========
        // T1 (depth 0-4): ClothArmor
        armTierPools[1].register(com.zootdungeon.items.armor.ClothArmor.class, 2f);

        // T2 (depth 0-9): LeatherArmor
        armTierPools[2].register(com.zootdungeon.items.armor.LeatherArmor.class, 2f);

        // T3 (depth 5-14): MailArmor
        armTierPools[3].register(com.zootdungeon.items.armor.MailArmor.class, 2f);

        // T4 (depth 10-19): ScaleArmor
        armTierPools[4].register(com.zootdungeon.items.armor.ScaleArmor.class, 2f);

        // T5 (depth 15+): PlateArmor
        armTierPools[5].register(com.zootdungeon.items.armor.PlateArmor.class, 2f);

        // 职业防具（掉落权重为 0，不随机生成）
        armorPool.register(com.zootdungeon.items.armor.WarriorArmor.class, 0f);
        armorPool.register(com.zootdungeon.items.armor.MageArmor.class, 0f);
        armorPool.register(com.zootdungeon.items.armor.RogueArmor.class, 0f);
        armorPool.register(com.zootdungeon.items.armor.HuntressArmor.class, 0f);
        armorPool.register(com.zootdungeon.items.armor.DuelistArmor.class, 0f);
        armorPool.register(com.zootdungeon.items.armor.ClericArmor.class, 0f);

        // ========== T1 远程武器 ==========
        misTierPools[1].register(com.zootdungeon.items.weapon.missiles.ThrowingStone.class, 3f);
        misTierPools[1].register(com.zootdungeon.items.weapon.missiles.ThrowingKnife.class, 3f);
        misTierPools[1].register(com.zootdungeon.items.weapon.missiles.ThrowingSpike.class, 3f);
        misTierPools[1].register(com.zootdungeon.items.weapon.darts.Dart.class, 0f);

        // ========== T2 远程武器 ==========
        misTierPools[2].register(com.zootdungeon.items.weapon.missiles.FishingSpear.class, 3f);
        misTierPools[2].register(com.zootdungeon.items.weapon.missiles.ThrowingClub.class, 3f);
        misTierPools[2].register(com.zootdungeon.items.weapon.missiles.Shuriken.class, 3f);

        // ========== T3 远程武器 ==========
        misTierPools[3].register(com.zootdungeon.items.weapon.missiles.ThrowingSpear.class, 3f);
        misTierPools[3].register(com.zootdungeon.items.weapon.missiles.Kunai.class, 3f);
        misTierPools[3].register(com.zootdungeon.items.weapon.missiles.Bolas.class, 3f);

        // ========== T4 远程武器 ==========
        misTierPools[4].register(com.zootdungeon.items.weapon.missiles.Javelin.class, 3f);
        misTierPools[4].register(com.zootdungeon.items.weapon.missiles.Tomahawk.class, 3f);
        misTierPools[4].register(com.zootdungeon.items.weapon.missiles.HeavyBoomerang.class, 3f);

        // ========== T5 远程武器 ==========
        misTierPools[5].register(com.zootdungeon.items.weapon.missiles.Trident.class, 3f);
        misTierPools[5].register(com.zootdungeon.items.weapon.missiles.ThrowingHammer.class, 3f);
        misTierPools[5].register(com.zootdungeon.items.weapon.missiles.ForceCube.class, 3f);

        // ========== 食物 ==========
        foodPool.register(com.zootdungeon.items.food.Food.class, 4f);
        foodPool.register(com.zootdungeon.items.food.Pasty.class, 1f);
        foodPool.register(com.zootdungeon.items.food.MysteryMeat.class, 0f);

        // ========== 戒指 ==========
        ringPool.register(com.zootdungeon.items.rings.RingOfAccuracy.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfArcana.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfElements.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfEnergy.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfEvasion.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfForce.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfFuror.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfHaste.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfMight.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfSharpshooting.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfTenacity.class, 3f);
        ringPool.register(com.zootdungeon.items.rings.RingOfWealth.class, 3f);

        // ========== 神器 ==========
        artifactPool.register(com.zootdungeon.items.artifacts.AlchemistsToolkit.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.ChaliceOfBlood.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.CloakOfShadows.class, 0f);
        artifactPool.register(com.zootdungeon.items.artifacts.DriedRose.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.EtherealChains.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.HolyTome.class, 0f);
        artifactPool.register(com.zootdungeon.items.artifacts.HornOfPlenty.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.MasterThievesArmband.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.SandalsOfNature.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.TalismanOfForesight.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.TimekeepersHourglass.class, 1f);
        artifactPool.register(com.zootdungeon.items.artifacts.UnstableSpellbook.class, 1f);

        // ========== 饰品 ==========
        trinketPool.register(com.zootdungeon.items.trinkets.ExoticCrystals.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.RatSkull.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.ParchmentScrap.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.PetrifiedSeed.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.MossyClump.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.DimensionalSundial.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.ThirteenLeafClover.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.TrapMechanism.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.MimicTooth.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.WondrousResin.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.EyeOfNewt.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.SaltCube.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.VialOfBlood.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.ShardOfOblivion.class, 1f);
        trinketPool.register(com.zootdungeon.items.trinkets.ChaoticCenser.class, 1f);

        // ========== 金币 ==========
        GOLD().register(com.zootdungeon.items.material.Gold.class, 1f);

        // ========== 测试用 LootTables（使用 CategoryPoolEntry） ==========
        register("debug:common_rewards",
            new LootTable()
                .pool(new LootPool()
                    .rolls(3)
                    .bonusRolls(0.5f)
                    .add(new CategoryPoolEntry(3, potionPool))
                    .add(new CategoryPoolEntry(3, scrollPool))
                    .add(new CategoryPoolEntry(2, seedPool))
                    .add(new CategoryPoolEntry(1, foodPool))));

        register("debug:rare_rewards",
            new LootTable()
                .pool(new LootPool()
                    .rolls(2)
                    .bonusRolls(0.3f)
                    .add(new CategoryPoolEntry(2, ringPool))
                    .add(new CategoryPoolEntry(2, wandPool))
                    .add(new CategoryPoolEntry(3, ARMOR()))  // WEAPON 特殊处理
                    .add(new CategoryPoolEntry(2, armorPool))));

        register("debug:legendary_rewards",
            new LootTable()
                .pool(new LootPool()
                    .rolls(1)
                    .bonusRolls(0.1f)
                    .whenDepthMin(15)
                    .add(new CategoryPoolEntry(5, artifactPool))
                    .add(new CategoryPoolEntry(3, trinketPool))));

        register("debug:gold_rewards",
            new LootTable()
                .pool(new LootPool()
                    .rolls(1)
                    .add(new CategoryPoolEntry(10, GOLD()))));

        register("debug:misc_rewards",
            new LootTable()
                .pool(new LootPool()
                    .rolls(2)
                    .add(new CategoryPoolEntry(2, potionPool))
                    .add(new CategoryPoolEntry(2, scrollPool))
                    .add(new CategoryPoolEntry(1, foodPool))
                    .add(new CategoryPoolEntry(1, seedPool))
                    .add(new CategoryPoolEntry(1, stonePool))));
    }

    // ============================================================
    // 抽取 API
    // ============================================================

    private static final ThreadLocal<Deque<String>> ACTIVE_TABLES = ThreadLocal.withInitial(ArrayDeque::new);

    public static List<Item> roll(String tableId, LootArgs args) {
        if (tableId == null || args == null) return Collections.emptyList();
        LootTable table = TABLES.get(tableId);
        if (table == null) return Collections.emptyList();

        Deque<String> stack = ACTIVE_TABLES.get();
        if (!stack.offerFirst(tableId)) return Collections.emptyList(); // 防环

        try {
            return table.roll(args);
        } finally {
            stack.pollFirst();
        }
    }

    public static Item rollOne(String tableId, LootArgs args) {
        List<Item> items = roll(tableId, args);
        return items.isEmpty() ? null : items.get(0);
    }

    // 便捷方法
    public static List<Item> roll(String tableId) {
        return roll(tableId, LootArgs.create().depth(Dungeon.depth));
    }

    public static Item rollOne(String tableId) {
        return rollOne(tableId, LootArgs.create().depth(Dungeon.depth));
    }

    // ============================================================
    // 便捷生成方法（通过 Generator.Category）
    // ============================================================

    /**
     * 通过 Generator.Category 随机生成一个物品（使用 Deck 机制）
     */
    public static Item random(Generator.Category category) {
        if (category == null) return null;
        // ARMOR 委托给 Generator.randomArmor() 走深度过滤
        if (category == Generator.Category.ARMOR) return Generator.randomArmor();
        LootArgs args = LootArgs.create().depth(Dungeon.depth);
        List<Item> items = category.pool().roll(args, false);
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * 通过 Generator.Category 使用默认概率随机生成物品（不使用 Deck）
     */
    public static Item randomUsingDefaults(Generator.Category category) {
        if (category == null) return null;
        // ARMOR 委托给 Generator.randomArmor() 走深度过滤
        if (category == Generator.Category.ARMOR) return Generator.randomArmor(true);
        LootArgs args = LootArgs.create().depth(Dungeon.depth);
        List<Item> items = category.pool().roll(args, true);
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * 标记一个 Artifact 类为已抽取（强制去重）
     */
    public static void markExtractedArtifact(Class<? extends Artifact> artifact) {
        if (artifact != null) extractedArtifacts.add(artifact.getName());
    }

    /**
     * 重置指定 Artifact 类的抽取状态（恢复可抽取）
     */
    public static boolean resetArtifact(Class<? extends Artifact> artifact) {
        if (artifact == null) return false;
        return extractedArtifacts.remove(artifact.getName());
    }

    /**
     * 查询指定 Artifact 类是否已被抽取
     */
    public static boolean isArtifactExtracted(Class<? extends Artifact> artifact) {
        return artifact != null && extractedArtifacts.contains(artifact.getName());
    }

    // ============================================================
    // LootArgs - 抽取上下文
    // ============================================================

    public static class LootArgs {

        public int depth = 0;
        public int pos = -1;
        public String source = "";
        public int luck = 0;
        public int qualityBonus = 0;
        public int bonusRolls = 0;
        public int quantityBonusRate = 0;
        public int bonusRollX = 0;

        // 抽取计数
        public final AtomicInteger poolRollCount = new AtomicInteger(0);
        public final AtomicInteger tableRollCount = new AtomicInteger(0);
        public final AtomicInteger totalRollCount = new AtomicInteger(0);

        // 当前处理中的 entry/pool/table
        public LootEntry chosenEntry = null;
        public LootPool currentPool = null;
        public String currentTableId = null;

        // 持久化引用
        public LootHistory history = null;

        // 自定义数据
        public final Map<String, Object> data = new HashMap<>();

        public static LootArgs create() { return new LootArgs(); }

        public LootArgs depth(int v) { this.depth = v; return this; }
        public LootArgs pos(int v) { this.pos = v; return this; }
        public LootArgs source(String v) { this.source = v; return this; }
        public LootArgs luck(int v) { this.luck = v; return this; }
        public LootArgs qualityBonus(int v) { this.qualityBonus = v; return this; }
        public LootArgs bonusRolls(int v) { this.bonusRolls = v; return this; }
        public LootArgs quantityBonusRate(int v) { this.quantityBonusRate = v; return this; }
        public LootArgs bonusRollX(int v) { this.bonusRollX = v; return this; }
        public LootArgs history(LootHistory h) { this.history = h; return this; }

        public LootArgs put(String key, Object value) { this.data.put(key, value); return this; }
        public <T> T get(String key) { return (T) this.data.get(key); }
        public <T> T get(String key, T defaultValue) { return this.data.containsKey(key) ? (T) this.data.get(key) : defaultValue; }

        /** 从父上下文继承累积变量 */
        public void inheritFrom(LootArgs parent) {
            if (parent == null) return;
            this.luck = parent.luck;
            this.qualityBonus = parent.qualityBonus;
            this.bonusRolls = parent.bonusRolls;
            this.quantityBonusRate = parent.quantityBonusRate;
            this.bonusRollX = parent.bonusRollX;
            if (this.history == null) this.history = parent.history;
        }

        /** 获取只读快照 */
        public LootArgs snapshot() {
            LootArgs snap = new LootArgs();
            snap.depth = this.depth;
            snap.pos = this.pos;
            snap.source = this.source;
            snap.luck = this.luck;
            snap.qualityBonus = this.qualityBonus;
            snap.bonusRolls = this.bonusRolls;
            snap.quantityBonusRate = this.quantityBonusRate;
            snap.bonusRollX = this.bonusRollX;
            snap.history = this.history;
            snap.data.putAll(this.data);
            return snap;
        }
    }

    // ============================================================
    // LootHistory - 持久化状态
    // ============================================================

    public static class LootHistory {

        // key = CategoryPool.identityKey()
        private final Map<String, DeckState> decks = new LinkedHashMap<>();
        private final Set<String> extracted = new HashSet<>();
        private final Map<String, Object> session = new HashMap<>();

        public static class DeckState {
            public float[] probs;
            public float[] defaultProbs;
            public float[] defaultProbs2;
            public float[] defaultProbsTotal;
            public Long seed;
            public int dropped = 0;
            public boolean usingSecondDeck = false;

            public DeckState clone() {
                DeckState s = new DeckState();
                s.probs = this.probs != null ? this.probs.clone() : null;
                s.defaultProbs = this.defaultProbs != null ? this.defaultProbs.clone() : null;
                s.defaultProbs2 = this.defaultProbs2 != null ? this.defaultProbs2.clone() : null;
                s.defaultProbsTotal = this.defaultProbsTotal != null ? this.defaultProbsTotal.clone() : null;
                s.seed = this.seed;
                s.dropped = this.dropped;
                s.usingSecondDeck = this.usingSecondDeck;
                return s;
            }
        }

        public Map<String, DeckState> getDecks() { return decks; }

        public DeckState getOrCreate(String key, Supplier<DeckState> factory) {
            return decks.computeIfAbsent(key, k -> factory.get());
        }

        public DeckState get(String key) { return decks.get(key); }

        public boolean hasDeck(String key) { return decks.containsKey(key); }

        public void resetDeck(String key) {
            DeckState state = decks.get(key);
            if (state != null) {
                state.usingSecondDeck = !state.usingSecondDeck;
                state.probs = state.usingSecondDeck && state.defaultProbs2 != null
                    ? state.defaultProbs2.clone() : (state.defaultProbs != null ? state.defaultProbs.clone() : null);
                state.seed = Random.Long();
                state.dropped = 0;
            }
        }

        public void reset() {
            for (Map.Entry<String, DeckState> e : decks.entrySet()) {
                resetDeck(e.getKey());
            }
            extracted.clear();
        }

        public boolean isExtracted(String itemClassName) { return extracted.contains(itemClassName); }
        public void markExtracted(String itemClassName) { extracted.add(itemClassName); }

        public <T> T getSession(String key) { return (T) session.get(key); }
        public void putSession(String key, Object value) { session.put(key, value); }
        public void clearSession() { session.clear(); }

        // ---- 序列化 ----
        private static final String DECKS = "decks";
        private static final String EXTRACTED = "extracted";

        public void storeInBundle(Bundle bundle) {
            Bundle decksBundle = new Bundle();
            for (Map.Entry<String, DeckState> e : decks.entrySet()) {
                Bundle stateBundle = new Bundle();
                DeckState s = e.getValue();
                if (s.probs != null) stateBundle.put("probs", s.probs);
                if (s.defaultProbs != null) stateBundle.put("defaultProbs", s.defaultProbs);
                if (s.defaultProbs2 != null) stateBundle.put("defaultProbs2", s.defaultProbs2);
                if (s.defaultProbsTotal != null) stateBundle.put("defaultProbsTotal", s.defaultProbsTotal);
                if (s.seed != null) stateBundle.put("seed", s.seed);
                stateBundle.put("dropped", s.dropped);
                stateBundle.put("usingSecondDeck", s.usingSecondDeck);
                decksBundle.put(e.getKey(), stateBundle);
            }
            bundle.put(DECKS, decksBundle);
            String[] extractedArray = extracted.toArray(new String[0]);
            bundle.put(EXTRACTED, extractedArray);
        }

        public void restoreFromBundle(Bundle bundle) {
            if (bundle == null) return;
            decks.clear();
            extracted.clear();

            Bundle decksBundle = bundle.getBundle(DECKS);
            if (decksBundle != null) {
                for (String key : decksBundle.getKeys()) {
                    Bundle stateBundle = decksBundle.getBundle(key);
                    DeckState s = new DeckState();
                    s.probs = stateBundle.contains("probs") ? stateBundle.getFloatArray("probs") : null;
                    s.defaultProbs = stateBundle.contains("defaultProbs") ? stateBundle.getFloatArray("defaultProbs") : null;
                    s.defaultProbs2 = stateBundle.contains("defaultProbs2") ? stateBundle.getFloatArray("defaultProbs2") : null;
                    s.defaultProbsTotal = stateBundle.contains("defaultProbsTotal") ? stateBundle.getFloatArray("defaultProbsTotal") : null;
                    s.seed = stateBundle.contains("seed") ? stateBundle.getLong("seed") : null;
                    s.dropped = stateBundle.getInt("dropped");
                    s.usingSecondDeck = stateBundle.getBoolean("usingSecondDeck");
                    decks.put(key, s);
                }
            }

            if (bundle.contains(EXTRACTED)) {
                String[] extractedArray = bundle.getStringArray(EXTRACTED);
                if (extractedArray != null) {
                    for (String s : extractedArray) extracted.add(s);
                }
            }
        }
    }

    // ============================================================
    // LootTable - 顶层掉落表
    // ============================================================

    public static class LootTable {
        public String id;
        public final List<LootPool> pools = new ArrayList<>();

        public LootTable() {}

        public LootTable pool(LootPool pool) {
            if (pool != null) pools.add(pool);
            return this;
        }

        public List<Item> roll(LootArgs ctx) {
            List<Item> out = new ArrayList<>();
            for (LootPool pool : pools) {
                ctx.currentPool = pool;
                ctx.poolRollCount.set(0);
                out.addAll(pool.roll(ctx));
            }
            ctx.tableRollCount.incrementAndGet();
            return out;
        }
    }

    // ============================================================
    // LootPool - 奖池
    // ============================================================

    @FunctionalInterface
    public interface LootCondition {
        boolean test(LootArgs ctx);
    }

    public static class LootPool {
        public int rolls = 1;
        public float bonusRolls = 0f;
        public final List<LootCondition> conditions = new ArrayList<>();
        public final List<LootEntry> entries = new ArrayList<>();

        public LootPool rolls(int r) { this.rolls = Math.max(0, r); return this; }
        public LootPool bonusRolls(float f) { this.bonusRolls = Math.max(0f, f); return this; }

        public LootPool when(LootCondition c) { if (c != null) conditions.add(c); return this; }

        // 快捷条件
        public LootPool whenDepth(int min, int max) {
            return when(ctx -> ctx.depth >= min && ctx.depth <= max);
        }
        public LootPool whenDepthMin(int min) {
            return when(ctx -> ctx.depth >= min);
        }
        public LootPool whenSource(String... sources) {
            return when(ctx -> Arrays.asList(sources).contains(ctx.source));
        }
        public LootPool whenLuck(int minLuck) {
            return when(ctx -> ctx.luck >= minLuck);
        }
        public LootPool whenHistory(String key, Object expected) {
            return when(ctx -> Objects.equals(ctx.history != null ? ctx.history.getSession(key) : null, expected));
        }

        public LootPool add(LootEntry e) { if (e != null) entries.add(e); return this; }

        public List<Item> roll(LootArgs ctx) {
            List<Item> out = new ArrayList<>();
            if (!matches(ctx, conditions)) return out;

            int totalRolls = rolls + bonusRollCount(ctx);
            for (int i = 0; i < totalRolls; i++) {
                LootEntry chosen = chooseEntry(ctx);
                if (chosen != null) {
                    ctx.chosenEntry = chosen;
                    out.addAll(chosen.generate(ctx));
                    ctx.poolRollCount.incrementAndGet();
                    ctx.totalRollCount.incrementAndGet();
                }
            }
            return out;
        }

        public int bonusRollCount(LootArgs ctx) {
            if (bonusRolls <= 0f) return 0;
            int x = ctx.bonusRollX;
            double v = Math.max(0d, bonusRolls * (1 + x / 100.0));
            int whole = (int) v;
            double frac = v - whole;
            return whole + (frac > 0d && Random.Float() < frac ? 1 : 0);
        }

        public LootEntry chooseEntry(LootArgs ctx) {
            List<LootEntry> candidates = new ArrayList<>();
            int totalWeight = 0;

            for (LootEntry e : entries) {
                if (e.matches(ctx)) {
                    int w = e.weight(ctx);
                    if (w > 0) {
                        candidates.add(e);
                        totalWeight += w;
                    }
                }
            }

            if (candidates.isEmpty() || totalWeight <= 0) return null;
            int ticket = Random.Int(totalWeight);
            for (LootEntry e : candidates) {
                ticket -= e.weight(ctx);
                if (ticket < 0) return e;
            }
            return null;
        }

        public static boolean matches(LootArgs ctx, List<LootCondition> conditions) {
            for (LootCondition c : conditions) {
                if (c != null && !c.test(ctx)) return false;
            }
            return true;
        }
    }

    // ============================================================
    // LootEntry - 条目基类
    // ============================================================

    public interface LootEntry {
        boolean matches(LootArgs ctx);
        int weight(LootArgs ctx);
        List<Item> generate(LootArgs ctx);
    }

    @FunctionalInterface
    public interface LootFunction {
        Item apply(Item item, LootArgs ctx);
    }

    public abstract static class BaseEntry<T extends BaseEntry<T>> implements LootEntry {
        public int weight = 1;
        public int quality = 0;
        public final List<LootCondition> conditions = new ArrayList<>();
        public final List<LootFunction> functions = new ArrayList<>();

        public BaseEntry(int weight) { this.weight = Math.max(0, weight); }

        public T weight(int w) { this.weight = Math.max(0, w); return self(); }
        public T quality(int q) { this.quality = q; return self(); }
        public T when(LootCondition c) { if (c != null) conditions.add(c); return self(); }
        public T apply(LootFunction f) { if (f != null) functions.add(f); return self(); }

        // 快捷回调
        public T upgrade(int levels) {
            return apply((item, ctx) -> {
                if (item instanceof EquipableItem) ((EquipableItem) item).upgrade(levels);
                return item;
            });
        }

        public T cursed() {
            return apply((item, ctx) -> {
                if (item instanceof EquipableItem) ((EquipableItem) item).cursed = true;
                return item;
            });
        }

        @Override
        public boolean matches(LootArgs ctx) {
            for (LootCondition c : conditions) {
                if (c != null && !c.test(ctx)) return false;
            }
            return true;
        }

        @Override
        public int weight(LootArgs ctx) {
            if (weight <= 0) return 0;
            int luck = ctx.luck;
            int exp = ctx.quantityBonusRate;
            // 有效权重 = base + quality * (luck + exp^quality)
            int effectiveBonus = quality * (luck + (exp > 0 ? (int) Math.pow(quality, exp) : 0));
            return Math.max(0, weight + effectiveBonus);
        }

        @Override
        public List<Item> generate(LootArgs ctx) {
            List<Item> items = create(ctx);
            if (functions.isEmpty()) return items;
            List<Item> result = new ArrayList<>();
            for (Item item : items) {
                Item current = item;
                for (LootFunction f : functions) {
                    if (current == null) break;
                    current = f.apply(current, ctx);
                }
                if (current != null) result.add(current);
            }
            return result;
        }

        public abstract List<Item> create(LootArgs ctx);

        @SuppressWarnings("unchecked")
        protected T self() { return (T) this; }
    }

    // ============================================================
    // 条目类型
    // ============================================================

    // 直接物品
    public static class ItemEntry extends BaseEntry<ItemEntry> {
        public final Supplier<Item> supplier;

        public ItemEntry(int weight, Class<? extends Item> cls) {
            this(weight, () -> Reflection.newInstance(cls));
        }

        public ItemEntry(int weight, Supplier<Item> supplier) {
            super(weight);
            this.supplier = supplier;
        }

        @Override
        public List<Item> create(LootArgs ctx) {
            if (supplier == null) return Collections.emptyList();
            Item item = supplier.get();
            if (item == null) return Collections.emptyList();
            return Collections.singletonList(randomizeIfPossible(item));
        }
    }

    // CategoryPool 条目
    public static class CategoryPoolEntry extends BaseEntry<CategoryPoolEntry> {
        public final CategoryPool pool;
        public final boolean useDefaults;

        public CategoryPoolEntry(int weight, CategoryPool pool) {
            this(weight, pool, false);
        }

        public CategoryPoolEntry(int weight, CategoryPool pool, boolean useDefaults) {
            super(weight);
            this.pool = pool;
            this.useDefaults = useDefaults;
        }

        @Override
        public List<Item> create(LootArgs ctx) {
            if (pool == null) return Collections.emptyList();
            return pool.roll(ctx, useDefaults);
        }
    }

    // 子表递归
    public static class TableEntry extends BaseEntry<TableEntry> {
        public final String tableId;

        public TableEntry(int weight, String tableId) {
            super(weight);
            this.tableId = tableId;
        }

        @Override
        public List<Item> create(LootArgs ctx) {
            if (tableId == null) return Collections.emptyList();
            LootArgs subCtx = ctx.snapshot();
            subCtx.inheritFrom(ctx);
            subCtx.currentTableId = tableId;
            return roll(tableId, subCtx);
        }
    }

    // Generator.Category 条目
    public static class GeneratorCategoryEntry extends BaseEntry<GeneratorCategoryEntry> {
        public final Generator.Category category;

        public GeneratorCategoryEntry(int weight, Generator.Category category) {
            super(weight);
            this.category = category;
        }

        @Override
        public List<Item> create(LootArgs ctx) {
            if (category == null) return Collections.emptyList();
            Item item = Generator.randomUsingDefaults(category);
            if (item == null) return Collections.emptyList();
            return Collections.singletonList(item);
        }
    }

    // 空条目
    public static class EmptyEntry extends BaseEntry<EmptyEntry> {
        public EmptyEntry() { super(1); }
        public EmptyEntry(int weight) { super(weight); }

        @Override
        public List<Item> create(LootArgs ctx) {
            return Collections.emptyList();
        }
    }

    // ============================================================
    // CategoryPool - Generator.Category 的替代（Deck 机制内置）
    // ============================================================

    public static class CategoryPool {
        public final String key;
        public final Class<?> superClass;
        public final Supplier<Item> makeSupplier;

        public Class<?>[] classes = new Class[0];
        public float[] probs;
        public float[] defaultProbs;
        public float[] defaultProbs2;
        public float[] defaultProbsTotal;

        // Tier 支持
        private static final float[][] TIER_PROBS = {
            {0, 75, 20, 4, 1},
            {0, 25, 50, 20, 5},
            {0, 0, 40, 50, 10},
            {0, 0, 20, 40, 40},
            {0, 0, 0, 20, 80}
        };

        public CategoryPool(String key, Class<?> superClass, Supplier<Item> make) {
            this.key = key;
            this.superClass = superClass;
            this.makeSupplier = make;
            this.probs = new float[0];
        }

        public String identityKey() { return key; }

        // ---- 注册物品 ----
        public CategoryPool register(Class<? extends Item> cls, float prob1, float prob2) {
            for (int i = 0; i < classes.length; i++) {
                if (classes[i].equals(cls)) {
                    probs[i] = prob1;
                    if (defaultProbs != null) defaultProbs[i] = prob1;
                    if (defaultProbs2 != null) defaultProbs2[i] = prob2;
                    if (defaultProbsTotal != null) defaultProbsTotal[i] = prob1 + prob2;
                    return this;
                }
            }

            classes = Arrays.copyOf(classes, classes.length + 1);
            classes[classes.length - 1] = cls;

            probs = Arrays.copyOf(probs, probs.length + 1);
            probs[probs.length - 1] = prob1;

            if (defaultProbs == null) defaultProbs = new float[0];
            defaultProbs = Arrays.copyOf(defaultProbs, defaultProbs.length + 1);
            defaultProbs[defaultProbs.length - 1] = prob1;

            if (prob2 > 0 || defaultProbs2 != null) {
                if (defaultProbs2 == null) {
                    defaultProbs2 = new float[probs.length];
                    System.arraycopy(defaultProbs, 0, defaultProbs2, 0, defaultProbs.length - 1);
                } else {
                    defaultProbs2 = Arrays.copyOf(defaultProbs2, defaultProbs2.length + 1);
                }
                defaultProbs2[defaultProbs2.length - 1] = prob2;
            }

            if (defaultProbsTotal == null) defaultProbsTotal = new float[0];
            defaultProbsTotal = Arrays.copyOf(defaultProbsTotal, defaultProbsTotal.length + 1);
            defaultProbsTotal[defaultProbsTotal.length - 1] = prob1 + prob2;

            return this;
        }

        public CategoryPool register(Class<? extends Item> cls, float prob) {
            return register(cls, prob, 0f);
        }

        // ---- Deck 抽取 ----
        public List<Item> roll(LootArgs ctx, boolean useDefaults) {
            List<Item> out = new ArrayList<>();
            if (classes.length == 0) return out;

            LootHistory history = ctx.history != null ? ctx.history : new LootHistory();
            LootHistory.DeckState state = history.getDecks().computeIfAbsent(key, k -> {
                LootHistory.DeckState s = new LootHistory.DeckState();
                s.usingSecondDeck = Random.Int(2) == 0;
                s.probs = s.usingSecondDeck && defaultProbs2 != null
                    ? defaultProbs2.clone() : (defaultProbs != null ? defaultProbs.clone() : probs.clone());
                s.defaultProbs = defaultProbs != null ? defaultProbs.clone() : probs.clone();
                s.defaultProbs2 = defaultProbs2 != null ? defaultProbs2.clone() : null;
                s.seed = Random.Long();
                return s;
            });

            float[] deckProbs = state.probs;
            if (deckProbs == null || deckProbs.length == 0) {
                deckProbs = state.defaultProbs != null ? state.defaultProbs : new float[classes.length];
            }

            // 种子重放
            if (state.seed != null) {
                Random.pushGenerator(state.seed);
                for (int i = 0; i < state.dropped; i++) Random.Long();
            }

            int idx;
            if (useDefaults && defaultProbsTotal != null) {
                idx = Random.chances(defaultProbsTotal);
            } else if (useDefaults && defaultProbs != null) {
                idx = Random.chances(defaultProbs);
            } else {
                idx = Random.chances(deckProbs);
            }

            if (state.seed != null) Random.popGenerator();

            if (idx == -1) {
                // Deck 耗尽
                history.resetDeck(key);
                return out;
            }

            state.dropped++;
            deckProbs[idx]--;
            state.probs = deckProbs;

            try {
                Item item = Reflection.newInstance((Class<Item>) classes[idx]);
                if (item != null) {
                    item = item.random();
                    // Artifact 去重（同时检查 LootHistory 和静态集合）
                    if (Artifact.class.isAssignableFrom(classes[idx])
                            && (history.isExtracted(classes[idx].getName())
                                || extractedArtifacts.contains(classes[idx].getName()))) {
                        // 已抽取过，返回金币
                        out.add(new com.zootdungeon.items.material.Gold().random());
                    } else {
                        if (Artifact.class.isAssignableFrom(classes[idx])) {
                            history.markExtracted(classes[idx].getName());
                            extractedArtifacts.add(classes[idx].getName());
                        }
                        out.add(item);
                    }
                }
            } catch (Exception e) {
                if (makeSupplier != null) out.add(makeSupplier.get());
            }

            return out;
        }

        public List<Item> roll(LootArgs ctx) { return roll(ctx, false); }

        public void undoDrop(Class<?> cls) {
            for (int i = 0; i < classes.length; i++) {
                if (classes[i].equals(cls)) {
                    if (Dungeon.hero == null) return;
                    // 需要从历史记录中恢复
                    break;
                }
            }
        }

        /**
         * 强制标记指定类为已抽取（用于 Generator.removeArtifact 的等价实现）
         * 实际逻辑委托到 LootRegistry.extractedArtifacts
         */
        public void markExtracted(String className) {
            if (className != null) extractedArtifacts.add(className);
        }

        /**
         * 重置指定类的抽取状态
         */
        public boolean resetExtracted(String className) {
            if (className == null) return false;
            return extractedArtifacts.remove(className);
        }

        // ---- Tier 支持 ----
        public static int tierFromDepth(int depth) {
            return Math.min(depth / 5, TIER_PROBS.length - 1);
        }

        public static int randomTierFromDepth(int depth) {
            int floorSet = tierFromDepth(depth);
            return Random.chances(TIER_PROBS[floorSet]);
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private static Item randomizeIfPossible(Item item) {
        if (item == null) return null;
        try {
            java.lang.reflect.Method m = item.getClass().getMethod("randomize");
            Object out = m.invoke(item);
            if (out instanceof Item) return (Item) out;
        } catch (Exception ignored) {}
        return item;
    }

    // ============================================================
    // ExtraLootBuff 系统（保持原有功能）
    // ============================================================

    public static abstract class ExtraLootBuff extends Buff {
        {
            type = buffType.NEUTRAL;
            announced = false;
        }

        @Override
        public int icon() { return BuffIndicator.NONE; }

        public abstract void onMobLootRollComplete(Mob mob);
    }

    public static void afterMobKillLootRoll(Mob mob) {
        if (mob == null) return;
        dispatchExtraLootFromMob(mob);
    }

    public static void dispatchExtraLootFromMob(Mob mob) {
        if (mob == null) return;
        ArrayList<Buff> copy = new ArrayList<>(mob.buffs());
        for (Buff b : copy) {
            if (b instanceof ExtraLootBuff) {
                ((ExtraLootBuff) b).onMobLootRollComplete(mob);
            }
        }
    }
}
