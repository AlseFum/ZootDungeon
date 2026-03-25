package com.zootdungeon.arknights.plugins;

import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.NextAttackReachBoost;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class ReachBoostPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "next_attack_rch_plus_2";

	@Override
	public String name() {
		return "RCH扩展插件";
	}

	@Override
	public String desc() {
		return "提供主动技能：使下一次攻击的 RCH +2。";
	}

	@Override
	public String pluginId() {
		return "plugin.reach_boost";
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
		spec.name = "RCH强化";
		spec.desc = "使下一次攻击的 RCH +2。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 2;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		NextAttackReachBoost buff = Buff.affect(com.zootdungeon.Dungeon.hero, NextAttackReachBoost.class);
		buff.bonusReach = 2;
		GLog.p("下一次攻击 RCH +2");
	}
}
