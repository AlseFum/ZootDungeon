package com.zootdungeon.items.weapon.ammo;

import com.zootdungeon.items.material.LiquidMetal;
import com.zootdungeon.items.Recipe;
import com.zootdungeon.items.Item;
import java.util.ArrayList;

public class LiquidMetalToAmmoRecipe extends Recipe.SimpleRecipe {
    {
        inputs = new Class[]{LiquidMetal.class};
        inQuantity = new int[]{1};
        cost = 0;
        output = Ammo.class;
        outQuantity = 1;
    }

    @Override
    public boolean testIngredients(ArrayList<Item> ingredients) {
        // 只允许1个液态金属
        return super.testIngredients(ingredients);
    }
} 