package com.zootdungeon.items.stones;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.messages.Messages;

public class StoneOfDeath extends Runestone {
    {
        image = ItemSpriteSheet.STONE_HOLDER;
    }
    
    @Override
    protected void activate(int cell) {
        Char ch = Actor.findChar(cell);
        if (ch != null && ch.isAlive()) {
            ch.die(this);
        }
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