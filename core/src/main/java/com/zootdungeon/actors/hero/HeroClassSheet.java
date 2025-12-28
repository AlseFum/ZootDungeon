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
import com.zootdungeon.items.BrokenSeal;
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
import com.zootdungeon.items.supply.GunSupply;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.weapon.SpiritBow;
import com.zootdungeon.items.weapon.accurateWeapon.Cudgel;
import com.zootdungeon.items.weapon.ambushWeapon.Dagger;
import com.zootdungeon.items.weapon.fastWeapon.Gloves;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.items.weapon.blockWeapon.Rapier;
import com.zootdungeon.items.weapon.melee.WornShortsword;
import com.zootdungeon.items.weapon.missiles.ThrowingKnife;
import com.zootdungeon.items.weapon.missiles.ThrowingSpike;
import com.zootdungeon.items.weapon.missiles.ThrowingStone;
import com.zootdungeon.journal.Catalog;
import com.zootdungeon.utils.EventBus;

public final class HeroClassSheet {

    public static final Map<String, HeroClass> registeredClasses = new LinkedHashMap<>();

    // 标准职业
    public static final HeroClass WARRIOR = registerStandardClass("warrior")
            .subClasses(HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR)
            .title("战士")
            .desc("战士是一个强大的近战职业，擅长使用各种武器和防具。\n\n战士的特点：\n- 高生命值和防御力\n- 可以使用所有武器和防具\n- 起始装备：短剑和破损印记\n- 可以自动识别治疗药水和升级卷轴")
            .shortDesc("战士是一个强大的近战职业，擅长使用各种武器和防具。")
            .spritesheet(Assets.Sprites.WARRIOR)
            .splashArt(Assets.Splashes.WARRIOR)
            .abilities(new HeroicLeap(), new Shockwave(), new Endure())
            .masteryBadge(Badges.Badge.MASTERY_WARRIOR)
            .unlocked(true)
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
            .title("法师")
            .desc("法师是一个强大的远程职业，擅长使用魔法和法杖。\n\n法师的特点：\n- 高魔法伤害\n- 起始装备：魔法法杖\n- 可以自动识别升级卷轴和力量药水")
            .shortDesc("法师是一个强大的远程职业，擅长使用魔法和法杖。")
            .spritesheet(Assets.Sprites.MAGE)
            .splashArt(Assets.Splashes.MAGE)
            .abilities(new ElementalBlast(), new WildMagic(), new WarpBeacon())
            .masteryBadge(Badges.Badge.MASTERY_MAGE)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE))
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
            .title("盗贼")
            .desc("盗贼是一个灵活的近战职业，擅长隐身和暗杀。\n\n盗贼的特点：\n- 高敏捷和暴击率\n- 起始装备：短剑和隐身斗篷\n- 可以探测陷阱和隐藏的门")
            .shortDesc("盗贼是一个灵活的近战职业，擅长隐身和暗杀。")
            .spritesheet(Assets.Sprites.ROGUE)
            .splashArt(Assets.Splashes.ROGUE)
            .abilities(new SmokeBomb(), new DeathMark(), new ShadowClone())
            .masteryBadge(Badges.Badge.MASTERY_ROGUE)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE))
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

                new com.zootdungeon.items.scrolls.ScrollOfAmbushRateModification().collect();
            })
            .register();

    public static final HeroClass HUNTRESS = registerStandardClass("huntress")
            .subClasses(HeroSubClass.SNIPER, HeroSubClass.WARDEN)
            .title("猎人")
            .desc("猎人是一个灵活的远程职业，擅长使用弓箭和陷阱。\n\n猎人的特点：\n- 高命中率和闪避率\n- 起始装备：弓箭\n- 可以在草丛中获得额外效果")
            .shortDesc("猎人是一个灵活的远程职业，擅长使用弓箭和陷阱。")
            .spritesheet(Assets.Sprites.HUNTRESS)
            .splashArt(Assets.Splashes.HUNTRESS)
            .abilities(new SpectralBlades(), new NaturesPower(), new SpiritHawk())
            .masteryBadge(Badges.Badge.MASTERY_HUNTRESS)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS))
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
            .title("决斗者")
            .desc("决斗者是一个专精的近战职业，擅长单挑和格斗。\n\n决斗者的特点：\n- 高单体伤害\n- 起始装备：细剑\n- 可以挑战敌人进行决斗")
            .shortDesc("决斗者是一个专精的近战职业，擅长单挑和格斗。")
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
            .title("牧师")
            .desc("牧师是一个支援型职业，擅长治疗和祝福。\n\n牧师的特点：\n- 高治疗能力\n- 起始装备：短剑和圣典\n- 可以使用神圣魔法")
            .shortDesc("牧师是一个支援型职业，擅长治疗和祝福。")
            .spritesheet(Assets.Sprites.CLERIC)
            .splashArt(Assets.Splashes.CLERIC)
            .abilities(new AscendedForm(), new Trinity(), new PowerOfMany())
            .masteryBadge(Badges.Badge.MASTERY_CLERIC)
            .unlocked(() -> Badges.isUnlocked(Badges.Badge.UNLOCK_CLERIC))
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
            .title("预备干员")
            .shortDesc("预备干员开局持有罗德岛终端，在精英化后可以选择多个分支。罗德岛终端可以消耗COST以执行各种战术操作。")
            .desc("预备干员开局持有罗德岛终端。终端可以消耗COST来：\n- 合成特殊物品\n- 部署作战投影\n- 安装战术装置\n- 还有更多...")
            .classTalentsTier1(
                Talent.IRON_STOMACH,
        Talent.LIQUID_WILLPOWER,
        Talent.RUNIC_TRANSFERENCE,
        Talent.LETHAL_MOMENTUM
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
                new com.zootdungeon.items.artifacts.RhodesIslandTerminal().identify().collect();

                new com.zootdungeon.items.scrolls.ScrollOfAmbushRateModification().collect();
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
        // 基础装备
        (hero.belongings.armor = new ClothArmor()).identify();
        new Food().identify().collect();
        new Waterskin().collect();
        //添加绒布包和药剂包
        VelvetPouch velvetPouch = new VelvetPouch();
        velvetPouch.identify().collect();

        PotionBandolier potionBandolier = new PotionBandolier();
        potionBandolier.identify().collect();

        new GunSupply().identify().collect();
        new DebugSupply().identify().collect();
        // new AssassinSupply().identify().collect();
        new DebugBag().identify().collect();

        // new TengusMask().identify().collect();
        // new KingsCrown().identify().collect();
        // 事件通知
        EventBus.fire("Hero:created", "hero", hero);
    }
};
