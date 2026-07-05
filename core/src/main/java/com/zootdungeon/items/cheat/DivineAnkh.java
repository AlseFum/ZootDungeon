package com.zootdungeon.items.cheat;

import com.zootdungeon.items.Ankh;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.TextureRegistry;
public class DivineAnkh extends Ankh {
    {
        image=TextureRegistry.once("cheat:divine_ankh","cola/totem_of_undying.png",0,0,32,32);
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    public boolean isBlessed() {
        return true;
    }
}
