package com.zootdungeon.items.cheat;

import com.zootdungeon.actors.mobs.Rat;
import com.zootdungeon.items.stones.Runestone;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
public class StoneOfDummy extends Runestone {
    {
        image = ItemSpriteSheet.STONE_HOLDER;
    }
    
    @Override
    protected void activate(int cell) {
        Rat rat = new Rat() {
            @Override
            protected boolean act() {
                spend(TICK);
                return true; // Prevent movement by only spending time
            }
            {
                defenseSkill=0;
            }
        };
        rat.pos = cell;
        rat.state = rat.PASSIVE;
        rat.HT=114514;
        rat.HP=114514;
        GameScene.add(rat);
    }
    
    @Override
    public String name() {
        return "Dummy魔石";
    }
    
    @Override
    public String desc() {
        return "这块石头会生成一个血量为114514的老鼠，它什么都不会做";
    }
} 