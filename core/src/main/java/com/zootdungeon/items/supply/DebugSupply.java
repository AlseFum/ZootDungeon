package com.zootdungeon.items.supply;

import com.zootdungeon.Dungeon;
import com.zootdungeon.items.cheat.Codex;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.items.cheat.ItemRemover;
import com.zootdungeon.items.cheat.Panacea;
import com.zootdungeon.items.cheat.RedStone;
import com.zootdungeon.items.cheat.AllItemsBox;
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
import com.zootdungeon.items.stones.StoneOfLevelSelect;
import com.zootdungeon.items.stones.StoneOfSummoning;
import com.zootdungeon.items.weapon.blockWeapon.*;
import com.zootdungeon.items.TengusMask;
import com.zootdungeon.items.KingsCrown;
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
        put_in(StoneOfSummoning.class, 300);
        put_in(StoneOfLevelSelect.class, 50);
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
        put_in(AllItemsBox.class, 1);
        put_in(AssassinSupply.class,1);
        put_in(TengusMask.class, 1);
        put_in(KingsCrown.class, 1);
        onOpen = () -> {
            Dungeon.energy+=10086;
            return null;
        };
        put_in(Katana.class);
        put_in(Rapier.class);
        put_in(RoundShield.class);
        put_in(Greatshield.class)
;
    put_in(Quarterstaff.class);}
}
