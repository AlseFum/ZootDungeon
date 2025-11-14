package com.zootdungeon.commands;

import com.zootdungeon.utils.GLog;

public interface Command {
    String name();
    String usage();
    void execute(String[] args) throws Exception;
    default void print(String text){ GLog.i(text); }
}


