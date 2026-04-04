package com.zootdungeon.items.loot;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.TerminalPlugin;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.LootRegistry;
import com.watabou.utils.Random;

/**
 * ReservedOp 天赋 {@link Talent#RESERVED_OP_PLUGIN_SCAVENGE}：由 {@link com.zootdungeon.items.LootRegistry}
 * 在击杀掉落结算前按需挂到怪物上，再通过 {@link LootRegistry.ExtraLootBuff} 统一分发。
 */
public class ReservedOpTerminalPluginExtraLoot extends LootRegistry.ExtraLootBuff {

	@Override
	public void onMobLootRollComplete(Mob mob) {
		try {
			if (Dungeon.hero == null || !Dungeon.hero.isAlive()) return;
			if (Dungeon.level == null || mob.pos < 0) return;
			if (Random.Float() >= 0.06f * Dungeon.hero.pointsInTalent(Talent.RESERVED_OP_PLUGIN_SCAVENGE)) return;
			TerminalPlugin plugin = RhodesIslandTerminal.createRandomLootPlugin();
			if (plugin != null) {
				plugin.identify();
				Dungeon.level.drop(plugin, mob.pos).sprite.drop();
			}
		} finally {
			detach();
		}
	}
}
