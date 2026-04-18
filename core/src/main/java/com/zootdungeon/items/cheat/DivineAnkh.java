package com.zootdungeon.items.cheat;

import com.zootdungeon.items.Ankh;
import com.zootdungeon.messages.Messages;

public class DivineAnkh extends Ankh {

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
