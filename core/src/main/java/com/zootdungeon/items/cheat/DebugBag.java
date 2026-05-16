package com.zootdungeon.items.cheat;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.bags.Bag;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.items.supply.DebugSupply;

public class DebugBag extends Bag {
    static {
        TextureRegistry.texture("sheet.cola.debug_bag", "cola/debug_bag.png")
                .setArea("debug_bag", 0, 0, 32, 32);
    }
    
    {
        image = TextureRegistry.idByLabel("debug_bag");
        unique = true;
    }

    @Override
    public boolean canHold(Item item) {
        return item instanceof DivineAnkh
            || item instanceof RedStone
            || item instanceof Codex
            || item instanceof Panacea
            || item instanceof DebugSupply
            || item instanceof ItemRemover
            || item instanceof ThrowingWeaponBox
            || item instanceof WandBox
            || item instanceof BombBox
            || item instanceof ItemEditor
            || item instanceof LevelConsole
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