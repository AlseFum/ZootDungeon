package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.OverclockedStrikes;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class OverclockedStrikesPlusPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "overclocked_strikes_plus";

	@Override
	public String name() {
		return "超频连击插件+";
	}

	@Override
	public String desc() {
		return "主动：一段时间内攻击次数 +4，但伤害 -40%。";
	}

	@Override
	public String pluginId() {
		return "plugin.overclocked_strikes_plus";
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
		spec.name = "超频连击+";
		spec.desc = "在 20 回合内攻击次数 +4，伤害 -40%。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 6;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		if (Dungeon.hero == null) return;
		OverclockedStrikes b = Buff.affect(Dungeon.hero, OverclockedStrikes.class);
		b.bonusHits = 4;
		b.damageMult = 0.6f;
		b.set(20);
		GLog.p("超频连击+启动：攻击次数 +4，伤害 -40%");
	}
}

