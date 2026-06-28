package com.zootdungeon.items.cheat;

import java.util.ArrayList;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.items.Item;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.scenes.CellSelector;

import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndIconGrid;
import com.zootdungeon.items.stones.StoneOfAugmentation;
import com.zootdungeon.items.stones.StoneOfEnchantment;
import com.zootdungeon.items.stones.StoneOfBlast;
import com.zootdungeon.items.stones.StoneOfDisarming;
import com.zootdungeon.items.stones.StoneOfFear;
import com.zootdungeon.items.stones.StoneOfFlock;
import com.zootdungeon.items.stones.StoneOfIntuition;
import com.zootdungeon.items.stones.StoneOfDetectMagic;
import com.zootdungeon.items.stones.StoneOfShock;
import com.zootdungeon.items.stones.StoneOfDeepSleep;
import com.zootdungeon.items.stones.StoneOfClairvoyance;
import com.zootdungeon.items.stones.StoneOfBlink;
import com.zootdungeon.items.stones.StoneOfAggression;
import com.zootdungeon.items.stones.Runestone;
import com.zootdungeon.items.stones.InventoryStone;
import com.watabou.utils.Reflection;

public class RedStone extends Item {

    private Class<? extends Runestone> selectedStone;
    public static final String AC_SELECT = "SELECT";
    public static final String AC_USE = "USE";
    public static final String AC_THROW = "THROW";
    public static final String AC_GENERATE = "GENERATE";
    
    {
        image = ItemSpriteSheet.STONE_HOLDER;
        defaultAction = AC_SELECT;
        stackable = true;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SELECT)) {
            hero.sprite.operate(hero.pos);

            // 使用Builder模式创建图标网格窗口
            WndIconGrid.Builder builder = new WndIconGrid.Builder()
                    .setTitle(Messages.get(RedStone.class, "title"))
                    .setColumns(5);
            addStoneItem(builder, ItemSpriteSheet.STONE_AUGMENTATION, StoneOfAugmentation.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_ENCHANT,       StoneOfEnchantment.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_BLAST,         StoneOfBlast.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_DETECT,        StoneOfDisarming.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_FEAR,          StoneOfFear.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_FLOCK,         StoneOfFlock.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_INTUITION,     StoneOfIntuition.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_DETECT,        StoneOfDetectMagic.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_SHOCK,         StoneOfShock.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_SLEEP,         StoneOfDeepSleep.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_CLAIRVOYANCE,  StoneOfClairvoyance.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_BLINK,         StoneOfBlink.class);
            addStoneItem(builder, ItemSpriteSheet.STONE_AGGRESSION,    StoneOfAggression.class);

            // 显示窗口
            GameScene.show(builder.build());
        } else if (action.equals(AC_USE)) {
            if (selectedStone == null) {
                GLog.w("请先选择要使用的符文石！");
            } else {
                Runestone stone = Reflection.newInstance(selectedStone);
                stone.curUser = hero;
                curUser = hero;
                curItem = stone;
                if (stone instanceof InventoryStone) {
                    // 对于背包符文石，使用directActivate方法
                    ((InventoryStone)stone).directActivate();
                } else {
                    // 对于需要选择目标的符文石，显示目标选择界面
                    GameScene.selectCell(new CellSelector.Listener() {
                        @Override
                        public void onSelect(Integer target) {
                            if (target != null) {
                                stone.onThrow(target);
                            }
                        }
                        @Override
                        public String prompt() {
                            return "选择使用目标位置";
                        }
                    });
                }
            }
        } else if (action.equals(AC_THROW)) {
            if (selectedStone == null) {
                GLog.w("请先选择要投掷的符文石！");
            } else {
                Runestone stone = Reflection.newInstance(selectedStone);
                stone.curUser = hero;
                curUser = hero;
                curItem = this;
                GameScene.selectCell(new CellSelector.Listener() {
                    @Override
                    public void onSelect(Integer target) {
                        if (target != null) {
                            stone.cast(hero, target);
                        }
                    }
                    @Override
                    public String prompt() {
                        return "选择投掷目标位置";
                    }
                });
            }
        } else if (action.equals(AC_GENERATE)) {
            if (selectedStone == null) {
                GLog.w("请先选择要生成的符文石！");
            } else {
                Runestone stone = Reflection.newInstance(selectedStone);
                stone.identify();
                if (stone.collect(hero.belongings.backpack)) {
                    GLog.p("生成了 " + stone.name() + "！");
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
        actions.add(AC_USE);
        actions.add(AC_THROW);
        actions.add(AC_GENERATE);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SELECT)) {
            return Messages.get(this, "ac_select");
        } else if (action.equals(AC_USE)) {
            return Messages.get(this, "ac_use");
        } else if (action.equals(AC_THROW)) {
            return Messages.get(this, "ac_throw");
        } else if (action.equals(AC_GENERATE)) {
            return Messages.get(this, "ac_generate");
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        String name = Messages.get(this, "name");
        if (selectedStone != null) {
            name += " (" + Reflection.newInstance(selectedStone).name() + ")";
        }
        return name;
    }

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc");
        if (selectedStone != null) {
            desc += "\n\n" + Messages.get(this, "current") + Reflection.newInstance(selectedStone).name();
            desc += "\n" + Reflection.newInstance(selectedStone).desc();
        }
        return desc;
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

    private <T extends Runestone> void addStoneItem(WndIconGrid.Builder builder, int icon, Class<T> cls) {
        T stone = Reflection.newInstance(cls);
        String desc = stone.name() + "：" + stone.desc();
        builder.addItem(new ItemSprite(icon), desc, () -> {
            selectedStone = cls;
        });
    }
}
