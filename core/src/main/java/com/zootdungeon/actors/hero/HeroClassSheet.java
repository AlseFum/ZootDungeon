package com.zootdungeon.actors.hero;

import java.util.LinkedHashMap;
import java.util.Map;

import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.abilities.cleric.AscendedForm;
import com.zootdungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.zootdungeon.actors.hero.abilities.cleric.Trinity;
import com.zootdungeon.actors.hero.abilities.duelist.Challenge;
import com.zootdungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.zootdungeon.actors.hero.abilities.duelist.Feint;
import com.zootdungeon.actors.hero.abilities.huntress.NaturesPower;
import com.zootdungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.zootdungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.zootdungeon.actors.hero.abilities.mage.ElementalBlast;
import com.zootdungeon.actors.hero.abilities.mage.WarpBeacon;
import com.zootdungeon.actors.hero.abilities.mage.WildMagic;
import com.zootdungeon.actors.hero.abilities.rogue.DeathMark;
import com.zootdungeon.actors.hero.abilities.rogue.ShadowClone;
import com.zootdungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.zootdungeon.actors.hero.abilities.warrior.Endure;
import com.zootdungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.zootdungeon.actors.hero.abilities.warrior.Shockwave;
import com.zootdungeon.items.weapon.MelanthaSword;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.SkillRecord;
import com.zootdungeon.arknights.plugins.SkillSheet;
import com.zootdungeon.items.BrokenSeal;
import com.zootdungeon.items.GuardModal;
import com.zootdungeon.items.Waterskin;
import com.zootdungeon.items.armor.ClothArmor;
import com.zootdungeon.items.artifacts.CloakOfShadows;
import com.zootdungeon.items.artifacts.HolyTome;
import com.zootdungeon.items.cheat.DebugBag;
import com.zootdungeon.items.bags.PotionBandolier;
import com.zootdungeon.items.bags.VelvetPouch;
import com.zootdungeon.items.food.Food;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.items.potions.PotionOfInvisibility;
import com.zootdungeon.items.potions.PotionOfLiquidFlame;
import com.zootdungeon.items.potions.PotionOfMindVision;
import com.zootdungeon.items.potions.PotionOfPurity;
import com.zootdungeon.items.potions.PotionOfStrength;
import com.zootdungeon.items.scrolls.ScrollOfLullaby;
import com.zootdungeon.items.scrolls.ScrollOfMagicMapping;
import com.zootdungeon.items.scrolls.ScrollOfMirrorImage;
import com.zootdungeon.items.scrolls.ScrollOfRage;
import com.zootdungeon.items.scrolls.ScrollOfRemoveCurse;
import com.zootdungeon.items.scrolls.ScrollOfUpgrade;
import com.zootdungeon.items.supply.DebugSupply;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.weapon.RhodesStandardBow;
import com.zootdungeon.items.weapon.SpiritBow;
import com.zootdungeon.items.weapon.Cudgel;
import com.zootdungeon.items.weapon.Dagger;
import com.zootdungeon.items.weapon.Gloves;
import com.zootdungeon.items.weapon.MagesStaff;
import com.zootdungeon.items.weapon.Rapier;
import com.zootdungeon.items.weapon.WornShortsword;
import com.zootdungeon.items.weapon.missiles.ThrowingKnife;
import com.zootdungeon.items.weapon.missiles.ThrowingSpike;
import com.zootdungeon.items.weapon.missiles.ThrowingStone;
import com.zootdungeon.journal.Catalog;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.ui.Icons;

public final class HeroClassSheet {

    public static final Map<String, HeroClass> registeredClasses = new LinkedHashMap<>();

    // 标准职业
    public static final HeroClass WARRIOR = registerStandardClass("warrior")
            .subClasses(HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR)
            .spritesheet(Assets.Sprites.WARRIOR)
            .splashArt(Assets.Splashes.WARRIOR)
            .abilities(new HeroicLeap(), new Shockwave(), new Endure())
            .masteryBadge(Badges.Badge.MASTERY_WARRIOR)
            .unlocked(true)
            .icons(
                new ItemSprite(ItemSpriteSheet.SEAL),
                new ItemSprite(ItemSpriteSheet.WORN_SHORTSWORD),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {

                // 基础武器
                (hero.belongings.weapon = new WornShortsword()).identify();

                // 投掷石头
                ThrowingStone stones = new ThrowingStone();
                stones.quantity(3).collect();
                Dungeon.quickslot.setSlot(0, stones);

                // 破损印记
                if (hero.belongings.armor != null) {
                    hero.belongings.armor.affixSeal(new BrokenSeal());
                    Catalog.setSeen(BrokenSeal.class);
                }

                // 自动识别
                new PotionOfHealing().identify();
                new ScrollOfRage().identify();
            })
            .register();

    public static final HeroClass MAGE = registerStandardClass("mage")
            .subClasses(HeroSubClass.BATTLEMAGE, HeroSubClass.WARLOCK)
            .spritesheet(Assets.Sprites.MAGE)
            .splashArt(Assets.Splashes.MAGE)
            .abilities(new ElementalBlast(), new WildMagic(), new WarpBeacon())
            .masteryBadge(Badges.Badge.MASTERY_MAGE)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE))
            .icons(
                new ItemSprite(ItemSpriteSheet.MAGES_STAFF),
                new ItemSprite(ItemSpriteSheet.WAND_MAGIC_MISSILE),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {

                // 法师法杖
                MagesStaff staff = new MagesStaff(new WandOfMagicMissile());
                (hero.belongings.weapon = staff).identify();
                hero.belongings.weapon.activate(hero);

                // 快捷栏
                Dungeon.quickslot.setSlot(0, staff);

                // 自动识别
                new ScrollOfUpgrade().identify();
                new PotionOfLiquidFlame().identify();
            })
            .register();

    public static final HeroClass ROGUE = registerStandardClass("rogue")
            .subClasses(HeroSubClass.ASSASSIN, HeroSubClass.FREERUNNER)
            .spritesheet(Assets.Sprites.ROGUE)
            .splashArt(Assets.Splashes.ROGUE)
            .abilities(new SmokeBomb(), new DeathMark(), new ShadowClone())
            .masteryBadge(Badges.Badge.MASTERY_ROGUE)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE))
            .icons(
                new ItemSprite(ItemSpriteSheet.ARTIFACT_CLOAK),
                Icons.get(Icons.STAIRS),
                new ItemSprite(ItemSpriteSheet.DAGGER),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {

                // 基础武器
                (hero.belongings.weapon = new Dagger()).identify();

                // 隐身斗篷
                CloakOfShadows cloak = new CloakOfShadows();
                (hero.belongings.artifact = cloak).identify();
                hero.belongings.artifact.activate(hero);

                // 投掷飞刀
                ThrowingKnife knives = new ThrowingKnife();
                knives.quantity(3).collect();

                // 快捷栏
                Dungeon.quickslot.setSlot(0, cloak);
                Dungeon.quickslot.setSlot(1, knives);

                // 自动识别
                new ScrollOfMagicMapping().identify();
                new PotionOfInvisibility().identify();

            })
            .register();

    public static final HeroClass HUNTRESS = registerStandardClass("huntress")
            .subClasses(HeroSubClass.SNIPER, HeroSubClass.WARDEN)
            .spritesheet(Assets.Sprites.HUNTRESS)
            .splashArt(Assets.Splashes.HUNTRESS)
            .abilities(new SpectralBlades(), new NaturesPower(), new SpiritHawk())
            .masteryBadge(Badges.Badge.MASTERY_HUNTRESS)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS))
            .icons(
                new ItemSprite(ItemSpriteSheet.SPIRIT_BOW),
                Icons.GRASS.get(),
                new ItemSprite(ItemSpriteSheet.GLOVES),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {

                // 基础武器
                (hero.belongings.weapon = new Gloves()).identify();

                // 灵魂弓
                SpiritBow bow = new SpiritBow();
                bow.identify().collect();

                // 快捷栏
                Dungeon.quickslot.setSlot(0, bow);

                // 自动识别
                new PotionOfMindVision().identify();
                new ScrollOfLullaby().identify();
            })
            .register();

    public static final HeroClass DUELIST = registerStandardClass("duelist")
            .subClasses(HeroSubClass.CHAMPION, HeroSubClass.MONK)
            .spritesheet(Assets.Sprites.DUELIST)
            .splashArt(Assets.Splashes.DUELIST)
            .abilities(new Challenge(), new ElementalStrike(), new Feint())
            .masteryBadge(Badges.Badge.MASTERY_DUELIST)
            .unlocked(() -> {
                if (Badges.isUnlocked(Badges.Badge.UNLOCK_DUELIST)) {
                    return true;
                }
                int unlockedChars = 0;
                for (Badges.Badge b : Badges.Badge.values()) {
                    if (b.name().startsWith("UNLOCK_") && Badges.isUnlocked(b)) {
                        unlockedChars++;
                    }
                    if (unlockedChars >= 2) {
                        return true;
                    }
                }
                return false;
            })
            .icons(
                new ItemSprite(ItemSpriteSheet.RAPIER),
                new ItemSprite(ItemSpriteSheet.WAR_HAMMER),
                new ItemSprite(ItemSpriteSheet.THROWING_SPIKE),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {
                // 基础武器
                (hero.belongings.weapon = new Rapier()).identify();
                hero.belongings.weapon.activate(hero);

                // 投掷尖刺
                ThrowingSpike spikes = new ThrowingSpike();
                spikes.quantity(2).collect();

                // 快捷栏
                Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
                Dungeon.quickslot.setSlot(1, spikes);

                // 自动识别
                new PotionOfStrength().identify();
                new ScrollOfMirrorImage().identify();
            })
            .register();

    public static final HeroClass CLERIC = registerStandardClass("cleric")
            .subClasses(HeroSubClass.PRIEST, HeroSubClass.PALADIN)
            .spritesheet(Assets.Sprites.CLERIC)
            .splashArt(Assets.Splashes.CLERIC)
            .abilities(new AscendedForm(), new Trinity(), new PowerOfMany())
            .masteryBadge(Badges.Badge.MASTERY_CLERIC)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_CLERIC))
            .icons(
                new ItemSprite(ItemSpriteSheet.ARTIFACT_TOME),
                Icons.TALENT.get(),
                new ItemSprite(ItemSpriteSheet.CUDGEL),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {

                // 基础武器
                (hero.belongings.weapon = new Cudgel()).identify();
                hero.belongings.weapon.activate(hero);

                // 圣典
                HolyTome tome = new HolyTome();
                (hero.belongings.artifact = tome).identify();
                hero.belongings.artifact.activate(hero);

                // 快捷栏
                Dungeon.quickslot.setSlot(0, tome);

                // 自动识别
                new PotionOfPurity().identify();
                new ScrollOfRemoveCurse().identify();
            })
            .register();
    public static final HeroClass ReservedOp=registerStandardClass("reservedOp")
            .spritesheet("cola/guard.png")
            .spriteCellSize(22, 23)
            .splashArt("cola/guard_splashart.png")
            .classTalentsTier1(
                Talent.RESERVED_OP_APPRAISAL,
                Talent.RESERVED_OP_FIELD_RATION,
                Talent.RESERVED_OP_COMMAND_SHIELD
            )
            .classTalentsTier2(
                Talent.RESERVED_OP_RAPID_MEAL,
                Talent.RESERVED_OP_ALCHEMY_SUBSIDY,
                Talent.RESERVED_OP_COST_SURGE,
                Talent.RESERVED_OP_COST_MASTERY
            )
            .icons(
                new ItemSprite(TextureRegistry.idByLabel("rhodes_island_terminal"))
            )
            .initializer(hero -> {
                // 开局给个 Melantha 剑：tier=1 cleave 武器，有 dmgBoostBase=3，能打能升。
                // 配合 SkillRecord(SKILL_1) + 终端 cost=40，让玩家有事可做。
                new MelanthaSword().identify().collect();
                RhodesIslandTerminal terminal = new RhodesIslandTerminal();
                terminal.identify().collect();
                Dungeon.quickslot.setSlot(2, terminal);
                terminal.setCost(40);
                // 开局赠送 1 份技能档案
                new SkillRecord(SkillSheet.SKILL_1).collect();
            })
            .register();

    public static final HeroClass RESERVED_GUARD = registerStandardClass("reservedGuard")
            .subClasses(HeroSubClass.OP_SHARP, HeroSubClass.ACE, HeroSubClass.BLAZE)
            .spritesheet("cola/guard.png")
            .spriteCellSize(22, 23)
            .splashArt("cola/guard_splashart.png")
            .classTalentsTier1(
                Talent.RESERVED_GUARD_HEARTY_MEAL,
                Talent.RESERVED_GUARD_VETERANS_INTUITION,
                Talent.RESERVED_GUARD_PROVOKED_ANGER,
                Talent.RESERVED_GUARD_IRON_WILL
            )
            .classTalentsTier2(
                Talent.RESERVED_GUARD_IRON_STOMACH,
                Talent.RESERVED_GUARD_LIQUID_WILLPOWER,
                Talent.RESERVED_GUARD_RUNIC_TRANSFERENCE,
                Talent.RESERVED_GUARD_LETHAL_MOMENTUM,
                Talent.RESERVED_GUARD_IMPROVISED_PROJECTILES
            )
            .icons(
                new ItemSprite(ItemSpriteSheet.SEAL),
                new ItemSprite(ItemSpriteSheet.WORN_SHORTSWORD),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {
                // 基础武器
                (hero.belongings.weapon = new WornShortsword()).identify();

                // 守护印记 - 特色物品（可投掷触发单挑）
                if (hero.belongings.armor != null) {
                    hero.belongings.armor.affixSeal(new GuardModal());
                    Catalog.setSeen(GuardModal.class);
                }

                // 投掷石头
                ThrowingStone stones = new ThrowingStone();
                stones.quantity(3).collect();
                Dungeon.quickslot.setSlot(0, stones);

                // 自动识别
                new PotionOfHealing().identify();
                new ScrollOfRage().identify();
            })
            .register();

    public static final HeroClass RESERVED_CASTER = registerStandardClass("reservedCaster")
            .subClasses(HeroSubClass.PITH, HeroSubClass.LOGOS, HeroSubClass.MANTRA)
            .spritesheet("cola/guard.png")
            .spriteCellSize(22, 23)
            .splashArt("cola/guard_splashart.png")
            .classTalentsTier1(
                Talent.RESERVED_CASTER_EMPOWERING_MEAL,
                Talent.RESERVED_CASTER_SCHOLARS_INTUITION,
                Talent.RESERVED_CASTER_LINGERING_MAGIC,
                Talent.RESERVED_CASTER_BACKUP_BARRIER
            )
            .classTalentsTier2(
                Talent.RESERVED_CASTER_ENERGIZING_MEAL,
                Talent.RESERVED_CASTER_INSCRIBED_POWER,
                Talent.RESERVED_CASTER_WAND_PRESERVATION,
                Talent.RESERVED_CASTER_ARCANE_VISION,
                Talent.RESERVED_CASTER_SHIELD_BATTERY
            )
            .icons(
                new ItemSprite(ItemSpriteSheet.MAGES_STAFF),
                new ItemSprite(ItemSpriteSheet.WAND_MAGIC_MISSILE),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {
                // 法师法杖
                MagesStaff staff = new MagesStaff(new WandOfMagicMissile());
                (hero.belongings.weapon = staff).identify();
                hero.belongings.weapon.activate(hero);

                // 快捷栏
                Dungeon.quickslot.setSlot(0, staff);

                // 自动识别
                new ScrollOfUpgrade().identify();
                new PotionOfLiquidFlame().identify();
            })
            .register();

    public static final HeroClass RESERVED_SNIPER = registerStandardClass("reservedSniper")
            .subClasses(HeroSubClass.STORMEYE, HeroSubClass.ROSMONTIS, HeroSubClass.OUTCAST)
            .spritesheet("cola/guard.png")
            .spriteCellSize(22, 23)
            .splashArt("cola/guard_splashart.png")
            .classTalentsTier1(
                Talent.RESERVED_SNIPER_NATURES_BOUNTY,
                Talent.RESERVED_SNIPER_SURVIVALISTS_INTUITION,
                Talent.RESERVED_SNIPER_FOLLOWUP_STRIKE,
                Talent.RESERVED_SNIPER_NATURES_AID
            )
            .classTalentsTier2(
                Talent.RESERVED_SNIPER_INVIGORATING_MEAL,
                Talent.RESERVED_SNIPER_LIQUID_NATURE,
                Talent.RESERVED_SNIPER_REJUVENATING_STEPS,
                Talent.RESERVED_SNIPER_HEIGHTENED_SENSES,
                Talent.RESERVED_SNIPER_DURABLE_PROJECTILES
            )
            .icons(
                new ItemSprite(ItemSpriteSheet.SPIRIT_BOW),
                Icons.GRASS.get(),
                new ItemSprite(ItemSpriteSheet.GLOVES),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {
                // 基础武器
                (hero.belongings.weapon = new Gloves()).identify();

                // 罗德短弓
                RhodesStandardBow bow = new RhodesStandardBow();
                bow.identify().collect();

                // 快捷栏
                Dungeon.quickslot.setSlot(0, bow);

                // 自动识别
                new PotionOfMindVision().identify();
                new ScrollOfLullaby().identify();
            })
            .register();

    public static final HeroClass RESERVED_SPECIALIST = registerStandardClass("reservedSpecialist")
            .subClasses(HeroSubClass.MISERY, HeroSubClass.SCOUT, HeroSubClass.RADIAN)
            .spritesheet("cola/guard.png")
            .spriteCellSize(22, 23)
            .splashArt("cola/guard_splashart.png")
            .classTalentsTier1(
                Talent.RESERVED_SPECIALIST_CACHED_RATIONS,
                Talent.RESERVED_SPECIALIST_THIEFS_INTUITION,
                Talent.RESERVED_SPECIALIST_SUCKER_PUNCH,
                Talent.RESERVED_SPECIALIST_PROTECTIVE_SHADOWS
            )
            .classTalentsTier2(
                Talent.RESERVED_SPECIALIST_MYSTICAL_MEAL,
                Talent.RESERVED_SPECIALIST_INSCRIBED_STEALTH,
                Talent.RESERVED_SPECIALIST_WIDE_SEARCH,
                Talent.RESERVED_SPECIALIST_SILENT_STEPS,
                Talent.RESERVED_SPECIALIST_ROGUES_FORESIGHT
            )
            .icons(
                new ItemSprite(ItemSpriteSheet.ARTIFACT_CLOAK),
                Icons.get(Icons.STAIRS),
                new ItemSprite(ItemSpriteSheet.DAGGER),
                new ItemSprite(ItemSpriteSheet.SCROLL_ISAZ)
            )
            .initializer(hero -> {
                // 基础武器
                (hero.belongings.weapon = new Dagger()).identify();

                // 隐身斗篷
                CloakOfShadows cloak = new CloakOfShadows();
                (hero.belongings.artifact = cloak).identify();
                hero.belongings.artifact.activate(hero);

                // 投掷飞刀
                ThrowingKnife knives = new ThrowingKnife();
                knives.quantity(3).collect();

                // 快捷栏
                Dungeon.quickslot.setSlot(0, cloak);
                Dungeon.quickslot.setSlot(1, knives);

                // 自动识别
                new ScrollOfMagicMapping().identify();
                new PotionOfInvisibility().identify();
            })
            .register();

    // 私有构造函数，防止实例化
    private HeroClassSheet() {
        throw new AssertionError("HeroClassSheet is a utility class and should not be instantiated");
    }

    /**
     * 注册一个新的英雄职业
     *
     * @param heroClass 要注册的英雄职业
     * @return 注册的英雄职业
     */
    public static HeroClass register(HeroClass heroClass) {
        registeredClasses.put(heroClass.id(), heroClass);
        return heroClass;
    }

    /**
     * 创建并返回一个新的标准职业构建器
     *
     * @param id 职业ID
     * @return 职业构建器
     */
    public static HeroClassBuilder registerStandardClass(String id) {
        return HeroClass.builder(id);
    }

    /**
     * 获取所有注册的职业
     *
     * @return 职业数组
     */
    public static HeroClass[] values() {
        return registeredClasses.values().toArray(new HeroClass[0]);
    }

    /**
     * 根据ID获取职业
     *
     * @param id 职业ID
     * @return 对应的职业，如果不存在则返回null
     */
    public static HeroClass valueOf(String id) {
        return registeredClasses.get(id);
    }

    // 通用初始化
    public static void initCommon(Hero hero) {
        (hero.belongings.armor = new ClothArmor()).identify();
        new Food().identify().collect();
        new Waterskin().collect();
        VelvetPouch velvetPouch = new VelvetPouch();
        velvetPouch.identify().collect();

        PotionBandolier potionBandolier = new PotionBandolier();
        potionBandolier.identify().collect();
        new DebugBag().identify().collect();
        new DebugSupply().identify().collect();
        
    }
};
