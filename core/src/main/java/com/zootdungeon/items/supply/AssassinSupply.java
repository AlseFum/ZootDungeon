package com.zootdungeon.items.supply;

import com.zootdungeon.items.artifacts.CloakOfShadows;

public class AssassinSupply extends Supply {
    public AssassinSupply() {
        super();
        name = "Assassin Supply";
        desc = "A supply for the assassin class.";
        
        // Add a level 10 ShadowCloak
        CloakOfShadows cloak = new CloakOfShadows();
        for (int i = 0; i < 10; i++) {
            cloak.upgrade();
        }
        put_in(cloak);
        
        // put_in(Dagger.class);
        // put_in(Dirk.class);
        // put_in(AssassinsBlade.class);
    }
}
