package com.zootdungeon.items.bags;

import com.zootdungeon.items.Item;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.items.RedStone;
import com.zootdungeon.items.Codex;
import com.zootdungeon.items.Panacea;

import com.zootdungeon.items.ItemRemover;
import com.zootdungeon.items.supply.Supply;
import com.zootdungeon.items.supply.testitem;
public class DebugBag extends Bag {

    {
        image = ItemSpriteSheet.BACKPACK;
        unique = true;
    }

    @Override
    public boolean canHold(Item item) {
        return item instanceof DivineAnkh
            || item instanceof RedStone
            || item instanceof Codex
            || item instanceof Panacea
            || item instanceof Supply
            || item instanceof ItemRemover
            || item instanceof testitem
            ; 
    }

    @Override
    public int capacity() {
        return 30; // 弹药和枪械的容量限制
    }

    @Override
    public String name() {
        return "调试包";
    }

    @Override
    public String desc() {
        return "";
    }
} 