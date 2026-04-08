package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.OverclockedStrikes;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class OverclockedStrikesPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "overclocked_strikes";

	@Override
	public String name() {
		return "超频连击插件";
	}

	@Override
	public String desc() {
		return "主动：一段时间内攻击次数 +2，但伤害 -20%。";
	}

	@Override
	public String pluginId() {
		return "plugin.overclocked_strikes";
	}

	@Override
	public List<PassiveEntry> passiveEntries(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot) {
		return new ArrayList<>();
	}

	@Override
	public List<ActiveSpec> activeSpecs(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot) {
		ArrayList<ActiveSpec> list = new ArrayList<>();
		ActiveSpec spec = new ActiveSpec();
		spec.id = ACTIVE_ID;
		spec.name = "超频连击";
		spec.desc = "在 20 回合内攻击次数 +2，伤害 -20%。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 4;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		if (Dungeon.hero == null) return;
		OverclockedStrikes b = Buff.affect(Dungeon.hero, OverclockedStrikes.class);
		b.bonusHits = 2;
		b.damageMult = 0.8f;
		b.set(20);
		GLog.p("超频连击启动：攻击次数 +2，伤害 -20%");
	}
}

