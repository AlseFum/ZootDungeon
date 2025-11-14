package com.zootdungeon.items.bags;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.trinkets.Trinket;
import com.zootdungeon.sprites.ItemSpriteSheet;
public class TrinketBag extends Bag {
    {
        image = ItemSpriteSheet.HOLSTER;
    }
    @Override
	public boolean canHold( Item item ) {
		if (item instanceof Trinket ){
			return super.canHold(item);
		} else {
			return false;
		}
	}

	public int capacity(){
		return 22;
	}
}
