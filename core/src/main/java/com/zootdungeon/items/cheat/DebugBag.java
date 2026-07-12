package com.zootdungeon.items.cheat;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.bags.Bag;
import com.zootdungeon.messages.Messages;
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
        // cheat 包下的所有调试工具 + DebugSupply
        if (item instanceof DebugSupply) return true;
        Package pkg = item.getClass().getPackage();
        return pkg != null && pkg.getName().equals("com.zootdungeon.items.cheat");
    }

    @Override
    public int capacity() {
        return 30; // 弹药和枪械的容量限制
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }
} 