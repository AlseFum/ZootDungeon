package com.zootdungeon.items.artifacts;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.MagicImmune;
import com.zootdungeon.actors.buffs.Preparation;
import com.zootdungeon.actors.buffs.Regeneration;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.bags.Bag;
import com.zootdungeon.items.rings.RingOfEnergy;
import com.zootdungeon.journal.Catalog;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class RhodesIslandTerminal extends Artifact  {
    static {
        SpriteRegistry.registerItemTexture("cola/command_terminal.png", 32)
                .label("rhodes_island_terminal");
    }

    {
        image = SpriteRegistry.itemByName("rhodes_island_terminal");
    }

    @Override
    protected ArtifactBuff passiveBuff() {
        return new TerminalBuff();
    }

    public class TerminalBuff extends ArtifactBuff {
        // 占位符 Buff，目前不做任何事情
    }
}
