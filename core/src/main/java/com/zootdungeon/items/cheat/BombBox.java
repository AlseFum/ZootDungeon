package com.zootdungeon.items.cheat;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.items.bombs.ArcaneBomb;
import com.zootdungeon.items.bombs.Bomb;
import com.zootdungeon.items.bombs.Firebomb;
import com.zootdungeon.items.bombs.FlashBangBomb;
import com.zootdungeon.items.bombs.FrostBomb;
import com.zootdungeon.items.bombs.HolyBomb;
import com.zootdungeon.items.bombs.Noisemaker;
import com.zootdungeon.items.bombs.RegrowthBomb;
import com.zootdungeon.items.bombs.ShrapnelBomb;
import com.zootdungeon.items.bombs.SmokeBomb;
import com.zootdungeon.items.bombs.WoollyBomb;
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

public class BombBox extends Item {

    private Class<? extends Bomb> selectedBomb;
    public static final String AC_SELECT = "SELECT";
    public static final String AC_GENERATE = "GENERATE";
    public static final String AC_LIGHTTHROW = "LIGHTTHROW";

    {
        image = ItemSpriteSheet.BOMB_HOLDER;
        defaultAction = AC_LIGHTTHROW;
        usesTargeting = true;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SELECT)) {
            hero.sprite.operate(hero.pos);

            // 使用WndTabbedIconGrid构建器创建分标签页窗口
            WndTabbedIconGrid.Builder builder = new WndTabbedIconGrid.Builder()
                    .setTitle("选择炸弹")
                    .setColumns(4);
            
            // 添加炸弹标签页
            builder.addTab("炸弹", Icons.get(Icons.WAND_HOLSTER));
            
            // 添加炸弹到第一个标签页（索引0）
            builder.addItemToTab(0,
                    createBombIcon(Bomb.class),
                    getBombDescription(Bomb.class),
                    () -> selectedBomb = Bomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(Firebomb.class),
                    getBombDescription(Firebomb.class),
                    () -> selectedBomb = Firebomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(FrostBomb.class),
                    getBombDescription(FrostBomb.class),
                    () -> selectedBomb = FrostBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(ShrapnelBomb.class),
                    getBombDescription(ShrapnelBomb.class),
                    () -> selectedBomb = ShrapnelBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(FlashBangBomb.class),
                    getBombDescription(FlashBangBomb.class),
                    () -> selectedBomb = FlashBangBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(SmokeBomb.class),
                    getBombDescription(SmokeBomb.class),
                    () -> selectedBomb = SmokeBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(Noisemaker.class),
                    getBombDescription(Noisemaker.class),
                    () -> selectedBomb = Noisemaker.class
            );
            builder.addItemToTab(0,
                    createBombIcon(RegrowthBomb.class),
                    getBombDescription(RegrowthBomb.class),
                    () -> selectedBomb = RegrowthBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(HolyBomb.class),
                    getBombDescription(HolyBomb.class),
                    () -> selectedBomb = HolyBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(ArcaneBomb.class),
                    getBombDescription(ArcaneBomb.class),
                    () -> selectedBomb = ArcaneBomb.class
            );
            builder.addItemToTab(0,
                    createBombIcon(WoollyBomb.class),
                    getBombDescription(WoollyBomb.class),
                    () -> selectedBomb = WoollyBomb.class
            );

            // 显示窗口
            GameScene.show(builder.build());
        } else if (action.equals(AC_GENERATE)) {
            if (selectedBomb == null) {
                GLog.w("请先选择要生成的炸弹！");
            } else {
                Bomb bomb = Reflection.newInstance(selectedBomb);
                bomb.identify();
                if (bomb.collect(hero.belongings.backpack)) {
                    GLog.p("生成了 " + bomb.name() + "！");
                } else {
                    GLog.w("背包已满，无法生成物品！");
                }
            }
        } else if (action.equals(AC_LIGHTTHROW)) {
            if (selectedBomb == null) {
                GLog.w("请先选择要扔出的炸弹！");
            } else {
                // 设置 shouldLightFuse 标志，然后调用父类的 execute 方法处理 AC_THROW
                shouldLightFuse = true;
                super.execute(hero, AC_THROW);
            }
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SELECT);
        actions.add(AC_GENERATE);
        actions.add(AC_LIGHTTHROW);
        return actions;
    }

    private boolean shouldLightFuse = false;

    @Override
    public void cast(final Hero user, final int dst) {
        if (selectedBomb == null) {
            // 如果没有选择炸弹，使用默认的扔出行为
            super.cast(user, dst);
            return;
        }
        
        // 生成选择的炸弹并扔出
        Bomb bomb = Reflection.newInstance(selectedBomb);
        bomb.identify();
        
        // 将炸弹添加到背包，然后调用它的 cast 方法
        if (bomb.collect(user.belongings.backpack)) {
            // 临时保存当前物品
            final Item originalItem = curItem;
            curItem = bomb;
            
            try {
                // 如果应该点燃引信，使用反射设置 Bomb 类的静态 lightingFuse 为 true
                if (shouldLightFuse) {
                    try {
                        java.lang.reflect.Field lightingFuseField = Bomb.class.getDeclaredField("lightingFuse");
                        lightingFuseField.setAccessible(true);
                        lightingFuseField.setBoolean(null, true);
                    } catch (Exception e) {
                        // 如果反射失败，继续但不设置标志
                    }
                }
                
                // 调用炸弹的 cast 方法（onThrow 会异步检查 lightingFuse 静态变量）
                // 注意：onThrow 是在回调中被异步调用的，所以我们不能在这里恢复 lightingFuse
                // 参考 Bomb 类的实现，lightingFuse 在 execute 中设置后，会在 onThrow 检查时仍然为 true
                bomb.cast(user, dst);
                
                // 注意：不在这里恢复 lightingFuse，因为 onThrow 是异步调用的
                // lightingFuse 会在下次 Bomb.execute 被调用时被重置（如果不是 AC_LIGHTTHROW）
            } finally {
                // 恢复 curItem 和 shouldLightFuse
                curItem = originalItem;
                shouldLightFuse = false;
            }
        } else {
            // 背包已满，使用默认行为
            super.cast(user, dst);
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SELECT)) {
            return "选择炸弹";
        } else if (action.equals(AC_GENERATE)) {
            return "生成炸弹";
        } else if (action.equals(AC_LIGHTTHROW)) {
            return "点燃并扔出";
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return "炸弹箱";
    }

    @Override
    public String desc() {
        return "这是一个可以让你选择生成各种炸弹的物品。";
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
    
    // 创建炸弹图标的辅助方法
    private static Image createBombIcon(Class<? extends Bomb> bombClass) {
        Bomb bomb = Reflection.newInstance(bombClass);
        return new ItemSprite(bomb.image);
    }

    // 获取炸弹描述的辅助方法
    private static String getBombDescription(Class<? extends Bomb> bombClass) {
        String name = Messages.get(bombClass, "name");
        String desc = Messages.get(bombClass, "desc");
        
        // 检查是否找到了有效的文本
        if (name == null || name.equals(Messages.NO_TEXT_FOUND)) {
            name = bombClass.getSimpleName();
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

