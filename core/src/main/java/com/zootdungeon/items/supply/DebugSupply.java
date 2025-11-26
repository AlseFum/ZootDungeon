package com.zootdungeon.items.supply;

import com.zootdungeon.Dungeon;
import com.zootdungeon.items.Codex;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.items.ItemRemover;
import com.zootdungeon.items.Panacea;
import com.zootdungeon.items.RedStone;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.items.potions.PotionOfInvisibility;
import com.zootdungeon.items.potions.PotionOfStrength;
import com.zootdungeon.items.potions.exotic.PotionOfShroudingFog;
import com.zootdungeon.items.scrolls.ScrollOfIdentify;
import com.zootdungeon.items.scrolls.ScrollOfUpgrade;
import com.zootdungeon.items.stones.StoneOfDeath;
import com.zootdungeon.items.stones.StoneOfDummy;
import com.zootdungeon.items.stones.StoneOfDungeonTravel;
import com.zootdungeon.items.stones.StoneOfGeneration;
// import com.coladungeon.items.weapon.SummonerStaff;

public class DebugSupply extends Supply {
    public DebugSupply() {
        super();
        name = "Debug Supply";
        desc = "A supply for debugging purposes.";
        // put_in(SummonerStaff.class, 100);
        //put_in(Torch.class, 100);
        

        put_in(PotionOfHealing.class, 100);
        put_in(ScrollOfIdentify.class, 100);
        put_in(PotionOfStrength.class, 100);
        put_in(StoneOfGeneration.class, 300);
        put_in(StoneOfDungeonTravel.class, 300);
        put_in(ScrollOfUpgrade.class, 100);
        put_in(StoneOfDummy.class, 300);
        put_in(StoneOfDeath.class, 300);
        put_in(PotionOfInvisibility.class, 500);
        
        put_in(PotionOfShroudingFog.class, 200);
        put_in(DivineAnkh.class, 1);
        
        
        put_in(ItemRemover.class);
        put_in(Panacea.class, 1);
        put_in(Codex.class, 1);
        put_in(RedStone.class, 1);
        onOpen = () -> {
            Dungeon.energy+=10086;
            return null;
        };
    }
}
