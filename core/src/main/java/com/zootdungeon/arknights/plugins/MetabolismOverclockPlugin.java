package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.MetabolismOverclock;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;

import java.util.ArrayList;
import java.util.List;

public class MetabolismOverclockPlugin extends TerminalPlugin {

	@Override
	public String name() {
		return "代谢超频插件";
	}

	@Override
	public String desc() {
		return "被动：饥饿速率加快并且回血加快。";
	}

	@Override
	public String pluginId() {
		return "plugin.metabolism_overclock";
	}

	@Override
	public List<PassiveEntry> passiveEntries(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot) {
		ArrayList<PassiveEntry> list = new ArrayList<>();
		PassiveEntry e1 = new PassiveEntry();
		e1.id = "hunger";
		e1.name = "饥饿速率";
		e1.desc = "饥饿增长速度提升。";
		e1.valueText = "+50%";
		list.add(e1);

		PassiveEntry e2 = new PassiveEntry();
		e2.id = "regen";
		e2.name = "回血速率";
		e2.desc = "生命恢复速度提升。";
		e2.valueText = "+50%";
		list.add(e2);
		return list;
	}

	@Override
	public void onEnable(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot) {
		if (Dungeon.hero == null) return;
		MetabolismOverclock b = Buff.affect(Dungeon.hero, MetabolismOverclock.class);
		b.hungerMult = 1.5f;
		b.regenMult = 1.5f;
	}

	@Override
	public void onDisable(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot) {
		if (Dungeon.hero == null) return;
		Buff.detach(Dungeon.hero, MetabolismOverclock.class);
	}

	@Override
	public void onUninstall(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot) {
		onDisable(terminal, slot);
	}
}

