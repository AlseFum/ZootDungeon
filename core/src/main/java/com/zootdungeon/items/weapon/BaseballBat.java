package com.zootdungeon.items.weapon;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.base.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.TextureRegistry;

import java.util.ArrayList;

public class BaseballBat extends MeleeWeapon {

    public static final String AC_THROW_BASEBALL = "THROW_BASEBALL";

    {
        image = TextureRegistry.once("cuora_baseballbat","cola/cuora_baseballbat.png",0,0,32,32);
        hitSound = Assets.Sounds.HIT_CRUSH;
        tier = 1;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (findBaseball(hero) != null) {
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
                baseball.markBatSourced();
                baseball.execute(hero, Item.AC_THROW);
            }
            return;
        }
        super.execute(hero, action);
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
}
