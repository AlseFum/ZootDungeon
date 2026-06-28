package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.potions.Potion;
import com.zootdungeon.items.potions.PotionOfExperience;
import com.zootdungeon.items.potions.PotionOfFrost;
import com.zootdungeon.items.potions.PotionOfHaste;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.items.potions.PotionOfInvisibility;
import com.zootdungeon.items.potions.PotionOfLevitation;
import com.zootdungeon.items.potions.PotionOfLiquidFlame;
import com.zootdungeon.items.potions.PotionOfMindVision;
import com.zootdungeon.items.potions.PotionOfParalyticGas;
import com.zootdungeon.items.potions.PotionOfPurity;
import com.zootdungeon.items.potions.PotionOfStrength;
import com.zootdungeon.items.potions.PotionOfToxicGas;
import com.zootdungeon.items.potions.brews.AquaBrew;
import com.zootdungeon.items.potions.brews.BlizzardBrew;
import com.zootdungeon.items.potions.brews.CausticBrew;
import com.zootdungeon.items.potions.brews.InfernalBrew;
import com.zootdungeon.items.potions.brews.ShockingBrew;
import com.zootdungeon.items.potions.brews.UnstableBrew;
import com.zootdungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.zootdungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.zootdungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.zootdungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.zootdungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.zootdungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.zootdungeon.items.potions.elixirs.ElixirOfMight;
import com.zootdungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.zootdungeon.items.potions.exotic.PotionOfCleansing;
import com.zootdungeon.items.potions.exotic.PotionOfCorrosiveGas;
import com.zootdungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.zootdungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.zootdungeon.items.potions.exotic.PotionOfEarthenArmor;
import com.zootdungeon.items.potions.exotic.PotionOfMagicalSight;
import com.zootdungeon.items.potions.exotic.PotionOfMastery;
import com.zootdungeon.items.potions.exotic.PotionOfShielding;
import com.zootdungeon.items.potions.exotic.PotionOfShroudingFog;
import com.zootdungeon.items.potions.exotic.PotionOfSnapFreeze;
import com.zootdungeon.items.potions.exotic.PotionOfStamina;
import com.zootdungeon.items.potions.exotic.PotionOfStormClouds;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndTabbedIconGrid;
import com.watabou.noosa.Image;
import com.zootdungeon.Assets;
import com.watabou.utils.Reflection;

public class Panacea extends Item {

    private Class<? extends Potion> selectedPotion;
    public static final String AC_SELECT = "SELECT";
    public static final String AC_DRINK = "DRINK";
    public static final String AC_GENERATE = "GENERATE";

    {
        image = ItemSpriteSheet.POTION_HOLDER;
        defaultAction = AC_DRINK;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SELECT)) {
            hero.sprite.operate(hero.pos);

            // 使用WndTabbedIconGrid构建器创建分标签页窗口
            WndTabbedIconGrid.Builder builder = new WndTabbedIconGrid.Builder()
                    .setTitle(Messages.get(Panacea.class, "title"))
                    .setColumns(4);
            
            // 添加标准药剂标签页
            builder.addTab(Messages.get(Panacea.class, "tab_standard"), Icons.get(Icons.POTION_BANDOLIER));
            
            // 添加标准药剂到第一个标签页（索引0）
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfStrength.class),
                    potionDesc(PotionOfStrength.class),
                    () -> selectedPotion = PotionOfStrength.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfHealing.class),
                    potionDesc(PotionOfHealing.class),
                    () -> selectedPotion = PotionOfHealing.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfMindVision.class),
                    potionDesc(PotionOfMindVision.class),
                    () -> selectedPotion = PotionOfMindVision.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfFrost.class),
                    potionDesc(PotionOfFrost.class),
                    () -> selectedPotion = PotionOfFrost.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfLiquidFlame.class),
                    potionDesc(PotionOfLiquidFlame.class),
                    () -> selectedPotion = PotionOfLiquidFlame.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfToxicGas.class),
                    potionDesc(PotionOfToxicGas.class),
                    () -> selectedPotion = PotionOfToxicGas.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfHaste.class),
                    potionDesc(PotionOfHaste.class),
                    () -> selectedPotion = PotionOfHaste.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfInvisibility.class),
                    potionDesc(PotionOfInvisibility.class),
                    () -> selectedPotion = PotionOfInvisibility.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfLevitation.class),
                    potionDesc(PotionOfLevitation.class),
                    () -> selectedPotion = PotionOfLevitation.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfParalyticGas.class),
                    potionDesc(PotionOfParalyticGas.class),
                    () -> selectedPotion = PotionOfParalyticGas.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfPurity.class),
                    potionDesc(PotionOfPurity.class),
                    () -> selectedPotion = PotionOfPurity.class
            );
            builder.addItemToTab(0,
                    createPotionIcon(PotionOfExperience.class),
                    potionDesc(PotionOfExperience.class),
                    () -> selectedPotion = PotionOfExperience.class
            );
            
            // 添加异域药剂标签页
            builder.addTab(Messages.get(Panacea.class, "tab_exotic"), Icons.get(Icons.ALCHEMY));
            
            // 添加异域药剂到第二个标签页（索引1）
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfMastery.class),
                    potionDesc(PotionOfMastery.class),
                    () -> selectedPotion = PotionOfMastery.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfShielding.class),
                    potionDesc(PotionOfShielding.class),
                    () -> selectedPotion = PotionOfShielding.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfMagicalSight.class),
                    potionDesc(PotionOfMagicalSight.class),
                    () -> selectedPotion = PotionOfMagicalSight.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfSnapFreeze.class),
                    potionDesc(PotionOfSnapFreeze.class),
                    () -> selectedPotion = PotionOfSnapFreeze.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfDragonsBreath.class),
                    potionDesc(PotionOfDragonsBreath.class),
                    () -> selectedPotion = PotionOfDragonsBreath.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfCorrosiveGas.class),
                    potionDesc(PotionOfCorrosiveGas.class),
                    () -> selectedPotion = PotionOfCorrosiveGas.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfStamina.class),
                    potionDesc(PotionOfStamina.class),
                    () -> selectedPotion = PotionOfStamina.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfShroudingFog.class),
                    potionDesc(PotionOfShroudingFog.class),
                    () -> selectedPotion = PotionOfShroudingFog.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfStormClouds.class),
                    potionDesc(PotionOfStormClouds.class),
                    () -> selectedPotion = PotionOfStormClouds.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfEarthenArmor.class),
                    potionDesc(PotionOfEarthenArmor.class),
                    () -> selectedPotion = PotionOfEarthenArmor.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfCleansing.class),
                    potionDesc(PotionOfCleansing.class),
                    () -> selectedPotion = PotionOfCleansing.class
            );
            builder.addItemToTab(1,
                    createPotionIcon(PotionOfDivineInspiration.class),
                    potionDesc(PotionOfDivineInspiration.class),
                    () -> selectedPotion = PotionOfDivineInspiration.class
            );
            
            // 添加酿造药剂标签页
            builder.addTab(Messages.get(Panacea.class, "tab_brews"), Icons.get(Icons.WAND_HOLSTER));
            
            // 添加酿造药剂到第三个标签页（索引2）
            builder.addItemToTab(2,
                    createPotionIcon(InfernalBrew.class),
                    potionDesc(InfernalBrew.class),
                    () -> selectedPotion = InfernalBrew.class
            );
            builder.addItemToTab(2,
                    createPotionIcon(BlizzardBrew.class),
                    potionDesc(BlizzardBrew.class),
                    () -> selectedPotion = BlizzardBrew.class
            );
            builder.addItemToTab(2,
                    createPotionIcon(ShockingBrew.class),
                    potionDesc(ShockingBrew.class),
                    () -> selectedPotion = ShockingBrew.class
            );
            builder.addItemToTab(2,
                    createPotionIcon(CausticBrew.class),
                    potionDesc(CausticBrew.class),
                    () -> selectedPotion = CausticBrew.class
            );
            builder.addItemToTab(2,
                    createPotionIcon(AquaBrew.class),
                    potionDesc(AquaBrew.class),
                    () -> selectedPotion = AquaBrew.class
            );
            builder.addItemToTab(2,
                    createPotionIcon(UnstableBrew.class),
                    potionDesc(UnstableBrew.class),
                    () -> selectedPotion = UnstableBrew.class
            );
            
            // 添加药剂精华标签页
            builder.addTab(Messages.get(Panacea.class, "tab_elixirs"), Icons.get(Icons.SEED_POUCH));
            
            // 添加药剂精华到第四个标签页（索引3）
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfHoneyedHealing.class),
                    potionDesc(ElixirOfHoneyedHealing.class),
                    () -> selectedPotion = ElixirOfHoneyedHealing.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfAquaticRejuvenation.class),
                    potionDesc(ElixirOfAquaticRejuvenation.class),
                    () -> selectedPotion = ElixirOfAquaticRejuvenation.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfMight.class),
                    potionDesc(ElixirOfMight.class),
                    () -> selectedPotion = ElixirOfMight.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfDragonsBlood.class),
                    potionDesc(ElixirOfDragonsBlood.class),
                    () -> selectedPotion = ElixirOfDragonsBlood.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfToxicEssence.class),
                    potionDesc(ElixirOfToxicEssence.class),
                    () -> selectedPotion = ElixirOfToxicEssence.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfIcyTouch.class),
                    potionDesc(ElixirOfIcyTouch.class),
                    () -> selectedPotion = ElixirOfIcyTouch.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfArcaneArmor.class),
                    potionDesc(ElixirOfArcaneArmor.class),
                    () -> selectedPotion = ElixirOfArcaneArmor.class
            );
            builder.addItemToTab(3,
                    createPotionIcon(ElixirOfFeatherFall.class),
                    potionDesc(ElixirOfFeatherFall.class),
                    () -> selectedPotion = ElixirOfFeatherFall.class
            );

            // 显示窗口
            GameScene.show(builder.build());
        } else if (action.equals(AC_DRINK)) {
            if (selectedPotion == null) {
                GLog.w("请先选择要饮用的药剂！");
            } else {
                Potion potion = Reflection.newInstance(selectedPotion);
                potion.curUser = hero;
                potion.apply(hero);
            }
        } else if (action.equals(AC_GENERATE)) {
            if (selectedPotion == null) {
                GLog.w("请先选择要生成的药剂！");
            } else {
                Potion potion = Reflection.newInstance(selectedPotion);
                potion.identify();
                if (potion.collect(hero.belongings.backpack)) {
                    GLog.p("生成了 " + potion.name() + "！");
                } else {
                    GLog.w("背包已满，无法生成物品！");
                }
            }
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SELECT);
        actions.add(AC_DRINK);
        actions.add(AC_GENERATE);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SELECT)) {
            return Messages.get(this, "ac_select");
        } else if (action.equals(AC_DRINK)) {
            return Messages.get(this, "ac_drink");
        } else if (action.equals(AC_GENERATE)) {
            return Messages.get(this, "ac_generate");
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

    @Override
    public int value() {
        return 1000;
    }
    
    // 创建药剂图标的辅助方法
    private static Image createPotionIcon(Class<? extends Potion> potionClass) {
        try {
            Potion potion = Reflection.newInstance(potionClass);
            if (potion.icon != -1) {
                return new Image(Assets.getTexture(Assets.Sprites.ITEM_ICONS), ItemSpriteSheet.Icons.film.get(potion.icon));
            } else {
                // 如果没有icon，使用sprite作为fallback
                return new ItemSprite(potion.image);
            }
        } catch (Exception e) {
            // 发生异常时使用默认图标
            return new ItemSprite(ItemSpriteSheet.POTION_HOLDER);
        }
    }

    private static <T extends Potion> String potionDesc(Class<T> cls) {
        T p = Reflection.newInstance(cls);
        return p.name() + "：" + p.desc();
    }
}
