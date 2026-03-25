package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.NextAttackCostRefund;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class NextAttackCostRefundPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "next_attack_refund_cost_by_damage";

	@Override
	public String name() {
		return "回费转换插件";
	}

	@Override
	public String desc() {
		return "主动：下一次攻击根据造成的伤害回复 COST。";
	}

	@Override
	public String pluginId() {
		return "plugin.next_attack_cost_refund";
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
		spec.name = "回费转换";
		spec.desc = "下一次攻击根据造成伤害回复 COST（默认 10%）。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 2;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		if (Dungeon.hero == null) return;
		NextAttackCostRefund buff = Buff.affect(Dungeon.hero, NextAttackCostRefund.class);
		buff.rate = 2f;
		GLog.p("下一次攻击将根据伤害回复 COST");
	}
}

