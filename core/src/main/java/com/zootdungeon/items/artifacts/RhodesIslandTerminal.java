package com.zootdungeon.items.artifacts;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndRhodesIslandTerminal;

import java.util.ArrayList;

public class RhodesIslandTerminal extends Artifact  {
    
    public static final String AC_OPEN = "OPEN";
    
    static {
        SpriteRegistry.registerItemTexture("cola/command_terminal.png", 32)
                .label("rhodes_island_terminal");
    }

    {
        image = SpriteRegistry.itemByName("rhodes_island_terminal");
        defaultAction = AC_OPEN;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_OPEN);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_OPEN)) {
            GameScene.show(new WndRhodesIslandTerminal(this));
        }
    }

    @Override
    protected ArtifactBuff passiveBuff() {
        return new TerminalBuff();
    }

    public class TerminalBuff extends ArtifactBuff {
        // 占位符 Buff，目前不做任何事情
    }
}
