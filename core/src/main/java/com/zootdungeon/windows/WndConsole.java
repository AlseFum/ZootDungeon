package com.zootdungeon.windows;

import com.zootdungeon.commands.BuiltinCommands;
import com.zootdungeon.commands.CommandDispatcher;

public class WndConsole extends WndTextInput {

    public WndConsole(){
        super("命令控制台", "输入命令并回车。示例：help、echo hello", "", 256, false, "执行", null);
        BuiltinCommands.installDefaults();
    }

    @Override
    public void onSelect(boolean positive, String text){
        if (positive && text != null && !text.trim().isEmpty()){
            CommandDispatcher.dispatch(text.trim());
        }
    }
}


