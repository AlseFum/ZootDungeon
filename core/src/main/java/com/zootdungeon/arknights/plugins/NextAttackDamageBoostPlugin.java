package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.NextAttackDamageBoost;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class NextAttackDamageBoostPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "next_attack_damage_plus_40";

	@Override
	public String name() {
		return "火力增幅插件";
	}

	@Override
	public String desc() {
		return "主动：下一次攻击力增加 40%。";
	}

	@Override
	public String pluginId() {
		return "plugin.next_attack_damage_boost";
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
		spec.name = "火力增幅";
		spec.desc = "下一次攻击伤害 +40%。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 3;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		if (Dungeon.hero == null) return;
		NextAttackDamageBoost buff = Buff.affect(Dungeon.hero, NextAttackDamageBoost.class);
		buff.multiplier = 1.4f;
		GLog.p("下一次攻击伤害 +40%");
	}
}

