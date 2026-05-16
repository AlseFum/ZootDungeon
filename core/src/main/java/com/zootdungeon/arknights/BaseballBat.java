package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

public class BaseballBat extends MeleeWeapon {

    public static final String AC_THROW_BASEBALL = "THROW_BASEBALL";

    {
        image = ItemSpriteSheet.THROWING_KNIFE;
        hitSound = Assets.Sounds.HIT_CRUSH;
        tier = 1;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (hasBaseball(hero)) {
            actions.add(AC_THROW_BASEBALL);
        }
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_THROW_BASEBALL)) {
            return Messages.get(this, "ac_throw_baseball");
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_THROW_BASEBALL)) {
            Baseball baseball = findBaseball(hero);
            if (baseball != null) {
                baseball.setBatBonus(level()+3);
                baseball.execute(hero, Item.AC_THROW);
            }
            return;
        }
        super.execute(hero, action);
    }

    private boolean hasBaseball(Hero hero) {
        return findBaseball(hero) != null;
    }

    private Baseball findBaseball(Hero hero) {
        if (hero.belongings.weapon() instanceof Baseball) {
            return (Baseball) hero.belongings.weapon();
        }
        for (Item i : hero.belongings.backpack) {
            if (i instanceof Baseball && i.quantity() > 0) {
                return (Baseball) i;
            }
        }
        return null;
    }

    @Override
    public int min(int lvl) {
        return tier + lvl;
    }

    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }

}
