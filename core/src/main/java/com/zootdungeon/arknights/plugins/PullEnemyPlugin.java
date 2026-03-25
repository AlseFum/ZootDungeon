package com.zootdungeon.arknights.plugins;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class PullEnemyPlugin extends TerminalPlugin {

	private static final String ACTIVE_ID = "pull_enemy_to_hero";

	@Override
	public String name() {
		return "牵引装置插件";
	}

	@Override
	public String desc() {
		return "主动：把一个敌人拉到你身边。";
	}

	@Override
	public String pluginId() {
		return "plugin.pull_enemy";
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
		spec.name = "牵引";
		spec.desc = "将最近的可见敌人拉到你身边的空位。";
		spec.mode = ActiveMode.INSTANT;
		spec.cost = 4;
		list.add(spec);
		return list;
	}

	@Override
	public void onActivate(RhodesIslandTerminal terminal, RhodesIslandTerminal.PluginSlot slot, ActiveSpec spec) {
		if (spec == null || !ACTIVE_ID.equals(spec.id)) return;
		if (Dungeon.hero == null) return;

		Mob best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob == null) continue;
			if (mob.alignment == Char.Alignment.ALLY) continue;
			if (!Dungeon.level.heroFOV[mob.pos]) continue;
			int d = Dungeon.level.distance(Dungeon.hero.pos, mob.pos);
			if (d < bestDist) {
				bestDist = d;
				best = mob;
			}
		}
		if (best == null) {
			GLog.w("没有可牵引的敌人");
			return;
		}

		int from = best.pos;
		int to = Dungeon.hero.pos;
		Ballistica line = Ballistica.of(from, to, Ballistica.STOP_TARGET);
		int pullPos = -1;
		for (int i = line.path.size() - 1; i >= 0; i--) {
			int cell = line.path.get(i);
			if (!Dungeon.level.adjacent(cell, Dungeon.hero.pos)) continue;
			if (!Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell]) continue;
			if (Actor.findChar(cell) != null) continue;
			if (best.properties().contains(Char.Property.LARGE) && !Dungeon.level.openSpace[cell]) continue;
			pullPos = cell;
			break;
		}

		if (pullPos == -1) {
			GLog.w("你身边没有空位可以牵引");
			return;
		}

		if (best.sprite != null) {
			best.sprite.move(from, pullPos);
		}
		best.move(pullPos, false);
		GLog.p("牵引成功");
	}
}

