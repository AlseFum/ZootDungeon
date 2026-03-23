/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zootdungeon.items.cheat;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.scrolls.ScrollOfTeleportation;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.InterlevelScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

public class StoneOfDungeonTravel extends Item {
    
    {
        image = ItemSpriteSheet.STONE_SHOCK;
        icon = ItemSpriteSheet.Icons.SYMBOL_DEBUG;
        stackable = false;
        unique = true;
        defaultAction = AC_TRAVEL;
    }
    
    public static final String AC_TRAVEL = "TRAVEL";
    
    private int lastFloor = -1;
    private boolean activated = false;
    
    @Override
    public java.util.ArrayList<String> actions(Hero hero) {
        java.util.ArrayList<String> actions = super.actions(hero);
        actions.add(AC_TRAVEL);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_TRAVEL.equals(action)) {
            return "楼层旅行";
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (AC_TRAVEL.equals(action)) {
            activate(hero);
        }
    }
    
    private void activate(Hero hero) {
        if (!Dungeon.interfloorTeleportAllowed()) {
            GLog.w(Messages.get(ScrollOfTeleportation.class, "no_tele"));
            return;
        }
        
        activated = true;
        
        GameScene.show(new WndOptions(
                "楼层旅行工具",
                "你想前往哪里？",
                "上升一层",
                "下降一层",
                "上升五层", 
                "下降五层"
        ) {
            @Override
            protected void onSelect(int index) {
                if (index < 0) return;
                
                lastFloor = Dungeon.depth;
                int targetDepth = Dungeon.depth;
                
                switch (index) {
                    case 0: // 上升一层
                        targetDepth = Math.max(1, Dungeon.depth - 1);
                        break;
                    case 1: // 下降一层
                        targetDepth = Math.min(40, Dungeon.depth + 1);
                        break;
                    case 2: // 上升五层
                        targetDepth = Math.max(1, Dungeon.depth - 5);
                        break;
                    case 3: // 下降五层
                        targetDepth = Math.min(40, Dungeon.depth + 5);
                        break;
                }
                
                if (targetDepth != Dungeon.depth) {
                    Buff.affect(hero, Invisibility.class, 2f);
                    InterlevelScene.mode = InterlevelScene.Mode.RETURN;
                    InterlevelScene.returnDepth = targetDepth;
                    InterlevelScene.returnBranch = Dungeon.branch;
                    InterlevelScene.returnPos = -1;
                    Game.switchScene(InterlevelScene.class);
                    
                    CellEmitter.get(hero.pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);
                    Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
                }
            }
        });
    }
    
    private static final String LAST_FLOOR = "last_floor";
    private static final String ACTIVATED = "activated";
    
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(LAST_FLOOR, lastFloor);
        bundle.put(ACTIVATED, activated);
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        lastFloor = bundle.getInt(LAST_FLOOR);
        activated = bundle.getBoolean(ACTIVATED);
    }
    
    @Override
    public int value() {
        return 0;
    }
    
    @Override
    public String name() {
        return "楼层旅行工具";
    }

    @Override
    public String desc() {
        return "调试工具，可快速在楼层间移动（上下1层或5层）。";
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }
} 