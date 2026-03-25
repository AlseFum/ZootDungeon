package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.DefenseBoost;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class DefenseBoostPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "defense_boost_duration";

	@Override
	public String name() {
		return "防护强化插件";
	}

	@Override
	public String desc() {
		return "主动：一段时间内防御力增加。";
	}

	@Override
	public String pluginId() {
		return "plugin.defense_boost";
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
		spec.name = "防护强化";
		spec.desc = "20 回合内受到伤害 -30%。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 3;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		if (Dungeon.hero == null) return;
		DefenseBoost buff = Buff.affect(Dungeon.hero, DefenseBoost.class, 20f);
		buff.reduction = 0.3f;
		GLog.p("防护强化：20 回合内受到伤害 -30%");
	}
}

