package com.zootdungeon.arknights.RhodesStandardWeapons;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.supply.Supply;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.windows.WndItemSelect;

import java.util.ArrayList;
import java.util.List;

public class RhodesStandardWeaponSupply extends Supply {

    private List<Item> pendingItems = new ArrayList<>();
    static{
        TextureRegistry.texture("sheet.cola.rhodes_standard_weapon_supply", "cola/arksupply.png").setArea("arksupply", 0, 0, 64, 64);
    }
    {
        name = Messages.get(this, "name");
        desc = Messages.get(this, "desc");
        image = TextureRegistry.idByLabel("arksupply");
    }

    public RhodesStandardWeaponSupply() {
        super();

        put_in(RhodesStandardSword.class, 1);
        put_in(RhodesStandardHammer.class, 1);
        put_in(RhodesStandardShield.class, 1);
        put_in(RhodesStandardBow.class, 1);
        put_in(RhodesStandardBow.Arrow.class, 50);
    }

    @Override
    public void open(Hero hero) {
        // 先收集所有物品的实例
        pendingItems.clear();
        for (java.util.function.Supplier<Item> supply : supplies) {
            Item item = supply.get();
            if (item != null) {
                pendingItems.add(item);
            }
        }

        // 显示选择窗口
        GameScene.show(new WndItemSelect(
                name(),
                pendingItems,
                selectedItem -> {
                    // 选择后发放物品并删除 Supply
                    if (selectedItem != null) {
                        selectedItem.identify().collect();
                    }
                    // 从背包中移除
                    this.detach(hero.belongings.backpack);
                }
        ));
    }
}
