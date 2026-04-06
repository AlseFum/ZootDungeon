package com.zootdungeon.items.cheat;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.bags.Bag;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.items.supply.Supply;

public class DebugBag extends Bag {
    static {
        SpriteRegistry.texture("sheet.cola.debug_bag", "cola/debug_bag.png")
                .grid(32, 32)
                .label("debug_bag");
    }
    
    {
        image = SpriteRegistry.itemByName("debug_bag");
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
            || item instanceof ThrowingWeaponBox
            || item instanceof WandBox
            || item instanceof BombBox
            || item instanceof ItemEditor
            || item instanceof StoneOfDungeonTravel
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