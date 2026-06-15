package com.zootdungeon.items.cheat;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.wands.WandOfCorrosion;
import com.zootdungeon.items.wands.WandOfCorruption;
import com.zootdungeon.items.wands.WandOfDisintegration;
import com.zootdungeon.items.wands.WandOfFireblast;
import com.zootdungeon.items.wands.WandOfFrost;
import com.zootdungeon.items.wands.WandOfLightning;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.wands.WandOfPrismaticLight;
import com.zootdungeon.items.wands.WandOfRegrowth;
import com.zootdungeon.items.wands.WandOfTransfusion;
import com.zootdungeon.items.wands.WandOfWarding;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndTabbedIconGrid;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class WandBox extends Item {

    private Class<? extends Wand> selectedWand;
    private Wand curWand; // 内置的法杖实例
    
    public static final String AC_SELECT = "SELECT";
    public static final String AC_GENERATE = "GENERATE";
    public static final String AC_ZAP = Wand.AC_ZAP;

    {
        image = ItemSpriteSheet.WAND_HOLDER;
        defaultAction = AC_ZAP;
        usesTargeting = true;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SELECT)) {
            hero.sprite.operate(hero.pos);

            // 使用WndTabbedIconGrid构建器创建分标签页窗口
            WndTabbedIconGrid.Builder builder = new WndTabbedIconGrid.Builder()
                    .setTitle("选择法杖")
                    .setColumns(4);
            
            // 添加法杖标签页
            builder.addTab("法杖", Icons.get(Icons.WAND_HOLSTER));
            
            // 添加法杖到第一个标签页（索引0）
            builder.addItemToTab(0,
                    createWandIcon(WandOfMagicMissile.class),
                    getWandDescription(WandOfMagicMissile.class),
                    () -> {
                        selectedWand = WandOfMagicMissile.class;
                        curWand = null; // 选择新的法杖类型时重置
                    }
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfFireblast.class),
                    getWandDescription(WandOfFireblast.class),
                    () -> {
                        selectedWand = WandOfFireblast.class;
                        curWand = null;
                    }
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfFrost.class),
                    getWandDescription(WandOfFrost.class),
                    () -> selectedWand = WandOfFrost.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfLightning.class),
                    getWandDescription(WandOfLightning.class),
                    () -> selectedWand = WandOfLightning.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfDisintegration.class),
                    getWandDescription(WandOfDisintegration.class),
                    () -> selectedWand = WandOfDisintegration.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfPrismaticLight.class),
                    getWandDescription(WandOfPrismaticLight.class),
                    () -> selectedWand = WandOfPrismaticLight.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfCorrosion.class),
                    getWandDescription(WandOfCorrosion.class),
                    () -> selectedWand = WandOfCorrosion.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfLivingEarth.class),
                    getWandDescription(WandOfLivingEarth.class),
                    () -> selectedWand = WandOfLivingEarth.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfBlastWave.class),
                    getWandDescription(WandOfBlastWave.class),
                    () -> selectedWand = WandOfBlastWave.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfCorruption.class),
                    getWandDescription(WandOfCorruption.class),
                    () -> selectedWand = WandOfCorruption.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfWarding.class),
                    getWandDescription(WandOfWarding.class),
                    () -> selectedWand = WandOfWarding.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfRegrowth.class),
                    getWandDescription(WandOfRegrowth.class),
                    () -> selectedWand = WandOfRegrowth.class
            );
            builder.addItemToTab(0,
                    createWandIcon(WandOfTransfusion.class),
                    getWandDescription(WandOfTransfusion.class),
                    () -> selectedWand = WandOfTransfusion.class
            );

            // 显示窗口
            GameScene.show(builder.build());
        } else if (action.equals(AC_GENERATE)) {
            if (selectedWand == null) {
                GLog.w("请先选择要生成的法杖！");
            } else {
                Wand wand = Reflection.newInstance(selectedWand);
                wand.identify();
                if (wand.collect(hero.belongings.backpack)) {
                    GLog.p("生成了 " + wand.name() + "！");
                } else {
                    GLog.w("背包已满，无法生成物品！");
                }
            }
        } else if (action.equals(AC_ZAP)) {
            if (selectedWand == null) {
                GLog.w("请先选择要使用的法杖！");
            } else {
                // 如果内置法杖不存在或类型不匹配，创建新的法杖
                if (curWand == null || curWand.getClass() != selectedWand) {
                    curWand = Reflection.newInstance(selectedWand);
                    curWand.identify();
                }
                
                // 每次发射前充能一次（确保有足够的充能）
                curWand.gainCharge(1f);
                
                // 调用法杖的 execute 方法，它会设置 curItem 和 curUser，并显示选择界面
                // 注意：不需要手动设置 curItem，因为 wand.execute 会调用 super.execute 来设置
                curWand.execute(hero, AC_ZAP);
            }
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SELECT);
        actions.add(AC_GENERATE);
        actions.add(AC_ZAP);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SELECT)) {
            return "选择法杖";
        } else if (action.equals(AC_GENERATE)) {
            return "生成法杖";
        } else if (action.equals(AC_ZAP)) {
            return "发射法杖";
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return "法杖箱";
    }

    @Override
    public String desc() {
        return "这是一个可以让你选择生成各种法杖的物品。";
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
    
    // 创建法杖图标的辅助方法
    private static Image createWandIcon(Class<? extends Wand> wandClass) {
        Wand wand = Reflection.newInstance(wandClass);
        return new ItemSprite(wand.image);
    }

    // 获取法杖描述的辅助方法
    private static String getWandDescription(Class<? extends Wand> wandClass) {
        String name = Messages.get(wandClass, "name");
        String desc = Messages.get(wandClass, "desc");
        
        // 检查是否找到了有效的文本
        if (name == null || name.equals(Messages.NO_TEXT_FOUND)) {
            name = wandClass.getSimpleName();
        }
        if (desc == null || desc.equals(Messages.NO_TEXT_FOUND)) {
            desc = "";
        }
        
        // 如果描述太长，可以截取第一句话
        if (!desc.isEmpty() && desc.length() > 100) {
            int firstPeriod = desc.indexOf('。');
            int firstPeriodEn = desc.indexOf('.');
            int cutPoint = firstPeriod > 0 ? firstPeriod : (firstPeriodEn > 0 ? firstPeriodEn : Math.min(100, desc.length()));
            desc = desc.substring(0, cutPoint);
        }
        
        return desc.isEmpty() ? name : (name + "：" + desc);
    }
}

