package com.zootdungeon.ui;

import com.zootdungeon.CDSettings;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.windows.WndConsole;
import com.watabou.noosa.Image;

public class ConsoleButton extends IconButton {

    public ConsoleButton(){
        super();
        Image ic = Icons.get(Icons.INFO);
        ic.scale.set(PixelScene.align(0.8f));
        icon(ic);
    }

    @Override
    protected void onClick() {
        if (CDSettings.devConsole()){
            GameScene.show(new WndConsole());
        }
    }
}


