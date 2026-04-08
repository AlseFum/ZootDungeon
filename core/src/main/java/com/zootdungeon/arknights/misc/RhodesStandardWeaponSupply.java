package com.zootdungeon.arknights.misc;

import com.zootdungeon.items.supply.Supply;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;

public class RhodesStandardWeaponSupply extends Supply {
    {
        name = Messages.get(this, "name");
        desc = Messages.get(this, "desc");
        image = SpriteRegistry.byLabel("arksupply");
    }
    public RhodesStandardWeaponSupply() {
        super();
        
        put_in(RhodesStandardSword.class, 1);
        put_in(RhodesStandardShield.class, 1);
        put_in(RhodesStandardBow.class, 1);
        put_in(RhodesStandardWand.class, 1);
        put_in(RhodesStandardBow.Arrow.class, 50);
    }
}

