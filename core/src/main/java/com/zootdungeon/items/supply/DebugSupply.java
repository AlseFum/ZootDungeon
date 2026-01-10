package com.zootdungeon.items.supply;

import com.zootdungeon.arknights.ascalon.Ascalon;
import com.zootdungeon.arknights.MomentumWeapon;
import com.zootdungeon.arknights.PropertyHuntingWeapon;
import com.zootdungeon.arknights.RandomBuffCard;
import com.zootdungeon.arknights.RangeReducedWeapon;
import com.zootdungeon.arknights.SummoningAmbushWeapon;
import com.zootdungeon.arknights.WandOfPrisonCage;
import com.zootdungeon.arknights.SummoningThrowingWeapon;
import com.zootdungeon.arknights.misc.RhodesStandardWeaponSupply;

import com.zootdungeon.Dungeon;
import com.zootdungeon.items.cheat.Codex;
import com.zootdungeon.items.DivineAnkh;
import com.zootdungeon.items.cheat.ItemRemover;
import com.zootdungeon.items.cheat.ItemEditor;
import com.zootdungeon.items.cheat.Panacea;
import com.zootdungeon.items.cheat.RedStone;
import com.zootdungeon.items.cheat.ThrowingWeaponBox;
import com.zootdungeon.items.cheat.WandBox;
import com.zootdungeon.items.cheat.BombBox;
import com.zootdungeon.items.potions.PotionOfHealing;
import com.zootdungeon.items.potions.PotionOfInvisibility;
import com.zootdungeon.items.potions.PotionOfStrength;
import com.zootdungeon.items.potions.exotic.PotionOfShroudingFog;
import com.zootdungeon.items.scrolls.ScrollOfIdentify;
import com.zootdungeon.items.scrolls.ScrollOfUpgrade;
import com.zootdungeon.items.stones.StoneOfDeath;
import com.zootdungeon.items.cheat.StoneOfDummy;
import com.zootdungeon.items.cheat.StoneOfDungeonTravel;
import com.zootdungeon.items.cheat.StoneOfGeneration;
import com.zootdungeon.items.cheat.StoneOfLevelSelect;
import com.zootdungeon.items.cheat.StoneOfSummoning;
import com.zootdungeon.items.TengusMask;
import com.zootdungeon.items.KingsCrown;
import com.zootdungeon.plants.Swiftthistle;
// import com.coladungeon.items.weapon.SummonerStaff;

public class DebugSupply extends Supply {
    public DebugSupply() {
        super();
        name = "Debug Supply";
        desc = "A supply for debugging purposes.";
        

        put_in(PotionOfHealing.class, 20);
        put_in(ScrollOfIdentify.class, 20);
        put_in(PotionOfStrength.class, 20);
        

        put_in(StoneOfGeneration.class, 300);
        put_in(StoneOfDungeonTravel.class, 300);
        put_in(StoneOfSummoning.class, 300);
        put_in(StoneOfLevelSelect.class, 50);
        put_in(ScrollOfUpgrade.class, 100);
        put_in(StoneOfDummy.class, 300);
        put_in(StoneOfDeath.class, 300);

        put_in(DivineAnkh.class, 1);
        put_in(ItemRemover.class);
        put_in(ItemEditor.class, 1);
        put_in(Panacea.class, 1);
        put_in(Codex.class, 1);
        put_in(RedStone.class, 1);
        put_in(ThrowingWeaponBox.class, 1);
        put_in(WandBox.class, 1);
        put_in(BombBox.class, 1);
        put_in(AssassinSupply.class,1);
        put_in(TengusMask.class, 1);
        put_in(KingsCrown.class, 1);
        put_in(Ascalon.class, 1);
        put_in(SummoningAmbushWeapon.class, 1);
        put_in(WandOfPrisonCage.class, 1);
        put_in(RangeReducedWeapon.class, 1);
        put_in(MomentumWeapon.class, 1);
        put_in(PropertyHuntingWeapon.class, 1);
        put_in(RhodesStandardWeaponSupply.class, 1);
        put_in(SummoningThrowingWeapon.class, 50);
        put_in(Swiftthistle.Seed.class, 100);
        put_in(RandomBuffCard.class, 50);
        onOpen = () -> {
            Dungeon.energy+=800;
            return null;
        };
    }
}
