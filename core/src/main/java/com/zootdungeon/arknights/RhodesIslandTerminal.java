package com.zootdungeon.arknights;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.buffs.Barrier;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.arknights.plugins.DefenseBoostPlugin;
import com.zootdungeon.arknights.plugins.MetabolismOverclockPlugin;
import com.zootdungeon.arknights.plugins.NextAttackCostRefundPlugin;
import com.zootdungeon.arknights.plugins.NextAttackDamageBoostPlugin;
import com.zootdungeon.arknights.plugins.PullEnemyPlugin;
import com.zootdungeon.arknights.plugins.ReachBoostPlugin;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.LootRegistry;
import com.zootdungeon.items.artifacts.Artifact;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.AtomBundle;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndRhodesIslandTerminal;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collection;

import com.watabou.utils.Random;

public class RhodesIslandTerminal extends Artifact {

	public static final String AC_OPEN = "OPEN";

	public static final int COST_CAP = 99;
	public static final int DEFAULT_MAX_PLUGINS = 3;

	/** Base cap plus {@link Talent#RESERVED_OP_COST_MASTERY}. */
	public static int effectiveCostCap( Hero hero ) {
		int base = COST_CAP;
		if (hero == null) return base;
		int p = hero.pointsInTalent(Talent.RESERVED_OP_COST_MASTERY);
		return base + Math.max(0, p) * 15;
	}

	/**
	 * {@link Talent#RESERVED_OP_COST_SURGE}: spend all current COST to recharge wands and charge-based artifacts
	 * proportionally to their max charge capacity.
	 */
	public static void surgeAllCostIntoMagicalGear( Hero hero ) {
		if (hero == null || hero.pointsInTalent(Talent.RESERVED_OP_COST_SURGE) <= 0) return;
		int pool = Dungeon.cost;
		if (pool <= 0) {
			GLog.i(Messages.get(RhodesIslandTerminal.class, "surge_no_cost"));
			return;
		}
		ArrayList<Wand> wands = new ArrayList<>();
		ArrayList<Artifact> arts = new ArrayList<>();
		for (Item it : hero.belongings) {
			if (it instanceof Wand w) {
				wands.add(w);
			} else if (it instanceof Artifact a && !(a instanceof RhodesIslandTerminal) && a.chargeCap > 0) {
				arts.add(a);
			}
		}
		float totalW = 0f;
		for (Wand w : wands) totalW += Math.max(1, w.maxCharges);
		for (Artifact a : arts) totalW += Math.max(1, a.chargeCap);
		if (totalW <= 0f) {
			GLog.w(Messages.get(RhodesIslandTerminal.class, "surge_nothing"));
			return;
		}
		Dungeon.cost = 0;
		for (Wand w : wands) {
			float share = pool * (Math.max(1, w.maxCharges) / totalW);
			w.gainCharge(share);
		}
		for (Artifact a : arts) {
			float share = pool * (Math.max(1, a.chargeCap) / totalW);
			a.charge(hero, share);
		}
		GLog.p(Messages.get(RhodesIslandTerminal.class, "surge_done", pool));
	}

	@SuppressWarnings("unchecked")
	private static final Class<? extends TerminalPlugin>[] PLUGIN_LOOT_POOL = new Class[]{
			ReachBoostPlugin.class,
			NextAttackDamageBoostPlugin.class,
			NextAttackCostRefundPlugin.class,
			DefenseBoostPlugin.class,
			PullEnemyPlugin.class,
			MetabolismOverclockPlugin.class
	};

	public static TerminalPlugin createRandomLootPlugin() {
		return Reflection.newInstance(Random.element(PLUGIN_LOOT_POOL));
	}

	private static final String PLUGINS = "plugins";
	private static final String MAX_PLUGINS = "max_plugins";
	private static final String SLOT_COUNT = "slot_count";
	private static final String SLOT_PLUGIN = "slot_plugin";
	private static final String SLOT_ENABLED = "slot_enabled";
	private static final String SLOT_MODE = "slot_mode";
	private static final String SLOT_STATE = "slot_state";
	private static final String RUNTIME_ACTOR_ID = "runtime_actor_id";

	public int maxPlugins = DEFAULT_MAX_PLUGINS;
	public ArrayList<PluginSlot> slots = new ArrayList<>();
	public int runtimeActorId = -1;

	static {
		SpriteRegistry.texture("sheet.cola.command_terminal", "cola/command_terminal.png")
				.grid(32, 32)
				.label("rhodes_island_terminal");
	}

	{
		image = SpriteRegistry.itemByName("rhodes_island_terminal");
		defaultAction = AC_OPEN;
	}

	public ArrayList<TerminalPlugin> getInstalledPlugins() {
		ArrayList<TerminalPlugin> result = new ArrayList<>();
		for (PluginSlot slot : slots) {
			if (slot != null && slot.plugin != null) result.add(slot.plugin);
		}
		return result;
	}

	public boolean canInstallMorePlugins() {
		return slots.size() < maxPlugins;
	}

	public int slotCount() {
		return slots.size();
	}

	public PluginSlot getSlot(int index) {
		if (index < 0 || index >= slots.size()) return null;
		return slots.get(index);
	}

	public PluginSlot findSlotByPlugin(TerminalPlugin plugin) {
		for (PluginSlot slot : slots) {
			if (slot != null && slot.plugin == plugin) return slot;
		}
		return null;
	}

	public AtomBundle getOrCreatePluginStorage(TerminalPlugin plugin) {
		PluginSlot slot = findSlotByPlugin(plugin);
		if (slot == null) return null;
		if (slot.state == null) slot.state = new AtomBundle();
		return slot.state;
	}

	public boolean installPlugin(TerminalPlugin plugin, Hero hero) {
		if (plugin == null || !canInstallMorePlugins()) return false;
		if (!hero.belongings.backpack.contains(plugin)) return false;
		plugin.detach(hero.belongings.backpack);
		PluginSlot slot = new PluginSlot();
		slot.plugin = plugin;
		slot.state = new AtomBundle();
		slot.enabled = true;
		slot.activeModeState = "normal";
		slots.add(slot);
		plugin.onInstall(this, slot);
		if (slot.enabled) plugin.onEnable(this, slot);
		ensureRuntimeActor();
		return true;
	}

	public TerminalPlugin uninstallPlugin(int index, Hero hero) {
		if (index < 0 || index >= slots.size()) return null;
		PluginSlot slot = slots.get(index);
		TerminalPlugin plugin = slot != null ? slot.plugin : null;
		if (plugin != null && slot != null) {
			if (slot.enabled) plugin.onDisable(this, slot);
			plugin.onUninstall(this, slot);
		}
		slots.remove(index);
		if (plugin != null && hero.belongings.backpack != null) {
			if (!plugin.collect(hero.belongings.backpack)) {
				Dungeon.level.drop(plugin, hero.pos);
			}
		}
		return plugin;
	}

	public float getCostRegenMultiplier() {
		float mult = 1f;
		for (PluginSlot slot : slots) {
			if (!canSlotRun(slot)) continue;
			mult *= slot.plugin.costRegenMultiplier(this, slot);
		}
		return mult;
	}

	public void enableSlot(int index) {
		PluginSlot slot = getSlot(index);
		if (slot == null || slot.plugin == null || slot.enabled) return;
		slot.enabled = true;
		slot.plugin.onEnable(this, slot);
	}

	public void disableSlot(int index) {
		PluginSlot slot = getSlot(index);
		if (slot == null || slot.plugin == null || !slot.enabled) return;
		slot.plugin.onDisable(this, slot);
		slot.enabled = false;
	}

	public boolean canSlotRun(PluginSlot slot) {
		if (slot == null || slot.plugin == null || !slot.enabled) return false;
		String state = slot.activeModeState;
		return state == null || (!"jammed".equals(state) && !"cursed".equals(state));
	}

	public void activatePluginAction(int slotIndex, String actionId) {
		PluginSlot slot = getSlot(slotIndex);
		if (slot == null || !canSlotRun(slot)) return;
		for (TerminalPlugin.ActiveSpec spec : slot.plugin.activeSpecs(this, slot)) {
			if (spec == null || spec.id == null || !spec.id.equals(actionId)) continue;
			if (!slot.plugin.canActivate(this, slot, spec)) return;
			if (Dungeon.cost < spec.cost) return;
			Dungeon.cost -= spec.cost;
			slot.plugin.onActivate(this, slot, spec);
			if (Dungeon.hero != null && Dungeon.hero.pointsInTalent(Talent.RESERVED_OP_COMMAND_SHIELD) > 0) {
				int sh = Random.Int(2) + 1;
				Buff.affect(Dungeon.hero, Barrier.class).incShield(sh);
			}
			return;
		}
	}

	public void deactivatePluginAction(int slotIndex, String actionId) {
		PluginSlot slot = getSlot(slotIndex);
		if (slot == null || slot.plugin == null) return;
		for (TerminalPlugin.ActiveSpec spec : slot.plugin.activeSpecs(this, slot)) {
			if (spec != null && actionId.equals(spec.id)) {
				slot.plugin.onDeactivate(this, slot, spec);
				return;
			}
		}
	}

	public void consumePluginCharge(int slotIndex, String actionId) {
		PluginSlot slot = getSlot(slotIndex);
		if (slot == null || slot.plugin == null) return;
		for (TerminalPlugin.ActiveSpec spec : slot.plugin.activeSpecs(this, slot)) {
			if (spec != null && actionId.equals(spec.id)) {
				slot.plugin.onConsumeCharge(this, slot, spec);
				return;
			}
		}
	}

	public TerminalRuntimeActor ensureRuntimeActor() {
		if (runtimeActorId != -1) {
			Actor existing = Actor.findById(runtimeActorId);
			if (existing instanceof TerminalRuntimeActor tra) {
				tra.bind(this);
				return tra;
			}
		}
		TerminalRuntimeActor actor = new TerminalRuntimeActor();
		actor.bind(this);
		Actor.add(actor);
		runtimeActorId = actor.id();
		return actor;
	}

	public void bindRuntimeActor(TerminalRuntimeActor actor) {
		if (actor == null) return;
		actor.bind(this);
		runtimeActorId = actor.id();
	}

	public void onInstall(Hero hero) {
		for (PluginSlot slot : slots) {
			if (slot != null && slot.plugin != null) slot.plugin.onInstall(this, slot);
		}
	}

	public void onLoad() {
		ensureRuntimeActor();
		for (PluginSlot slot : slots) {
			if (slot == null || slot.plugin == null) continue;
			slot.plugin.onLoad(this, slot);
			if (slot.enabled) slot.plugin.onEnable(this, slot);
		}
	}

	public void onEnable() {
		ensureRuntimeActor();
		for (PluginSlot slot : slots) {
			if (slot != null && slot.plugin != null && slot.enabled) slot.plugin.onEnable(this, slot);
		}
	}

	public void onDisable() {
		for (PluginSlot slot : slots) {
			if (slot != null && slot.plugin != null && slot.enabled) slot.plugin.onDisable(this, slot);
		}
	}

	public void onUninstall(Hero hero) {
		for (int i = slots.size() - 1; i >= 0; i--) {
			uninstallPlugin(i, hero);
		}
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_OPEN);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_OPEN)) {
			GameScene.show(new WndRhodesIslandTerminal(this));
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new TerminalBuff();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		ArrayList<TerminalPlugin> plugins = getInstalledPlugins();
		bundle.put(PLUGINS, plugins);
		bundle.put(MAX_PLUGINS, maxPlugins);
		bundle.put(SLOT_COUNT, slots.size());
		for (int i = 0; i < slots.size(); i++) {
			PluginSlot slot = slots.get(i);
			if (slot == null || slot.plugin == null) continue;
			String p = "slot_" + i + "_";
			bundle.put(p + SLOT_PLUGIN, slot.plugin.getClass().getName());
			bundle.put(p + SLOT_ENABLED, slot.enabled ? 1 : 0);
			bundle.put(p + SLOT_MODE, slot.activeModeState != null ? slot.activeModeState : "normal");
			bundle.put(p + SLOT_STATE, slot.state != null ? slot.state.toString() : new AtomBundle().toString());
		}
		bundle.put(RUNTIME_ACTOR_ID, runtimeActorId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		Collection<?> col = bundle.getCollection(PLUGINS);
		ArrayList<TerminalPlugin> fallbackPlugins = new ArrayList<>();
		for (Object o : col) {
			if (o instanceof TerminalPlugin) {
				fallbackPlugins.add((TerminalPlugin) o);
			}
		}
		maxPlugins = bundle.contains(MAX_PLUGINS) ? bundle.getInt(MAX_PLUGINS) : DEFAULT_MAX_PLUGINS;
		slots = new ArrayList<>();
		int slotCount = bundle.contains(SLOT_COUNT) ? bundle.getInt(SLOT_COUNT) : fallbackPlugins.size();
		for (int i = 0; i < slotCount; i++) {
			String p = "slot_" + i + "_";
			PluginSlot slot = new PluginSlot();
			if (bundle.contains(p + SLOT_PLUGIN)) {
				Object obj = null;
				try {
					Class<?> cls = Class.forName(bundle.getString(p + SLOT_PLUGIN));
					obj = cls.getDeclaredConstructor().newInstance();
				} catch (Exception ignored) {
				}
				if (obj instanceof TerminalPlugin tp) {
					slot.plugin = tp;
				}
			}
			if (slot.plugin == null && i < fallbackPlugins.size()) {
				slot.plugin = fallbackPlugins.get(i);
			}
			slot.enabled = !bundle.contains(p + SLOT_ENABLED) || bundle.getInt(p + SLOT_ENABLED) != 0;
			slot.activeModeState = bundle.contains(p + SLOT_MODE) ? bundle.getString(p + SLOT_MODE) : "normal";
			slot.state = new AtomBundle();
			if (bundle.contains(p + SLOT_STATE)) {
				String raw = bundle.getString(p + SLOT_STATE);
				if (raw != null && !raw.isEmpty()) {
					try {
						slot.state = AtomBundle.fromString(raw);
					} catch (Exception ignored) {
						slot.state = new AtomBundle();
					}
				}
			}
			if (slot.plugin != null) slots.add(slot);
		}
		runtimeActorId = bundle.contains(RUNTIME_ACTOR_ID) ? bundle.getInt(RUNTIME_ACTOR_ID) : -1;
		onLoad();
	}

	public class TerminalBuff extends ArtifactBuff {

		public void gainCharge(float levelPortion) {
			if (cursed || target == null || !(target instanceof Hero)) return;
			Hero h = (Hero) target;
			int cap = RhodesIslandTerminal.effectiveCostCap(h);
			if (Dungeon.cost >= cap) return;
			float gain = 2f * levelPortion * getCostRegenMultiplier();
			partialCharge += gain;
			while (partialCharge >= 1f && Dungeon.cost < cap) {
				partialCharge -= 1f;
				Dungeon.cost = Math.min(cap, Dungeon.cost + 1);
			}
		}
	}

	public static class PluginSlot {
		public TerminalPlugin plugin;
		public AtomBundle state = new AtomBundle();
		public boolean enabled = true;
		public String activeModeState = "normal";
	}

	public static class TerminalRuntimeActor extends Actor {

		public RhodesIslandTerminal terminal;

		public void bind(RhodesIslandTerminal terminal) {
			this.terminal = terminal;
		}

		@Override
		protected boolean act() {
			if (terminal != null) {
				for (PluginSlot slot : terminal.slots) {
					if (!terminal.canSlotRun(slot)) continue;
					for (TerminalPlugin.ActiveSpec spec : slot.plugin.activeSpecs(terminal, slot)) {
						slot.plugin.onRuntimeTick(terminal, slot, spec);
					}
					slot.plugin.onPassiveTick(terminal, slot);
				}
			}
			spend(TICK);
			return true;
		}
	}

	/**
	 * {@link Talent#RESERVED_OP_PLUGIN_SCAVENGE}：由 {@link LootRegistry} 在击杀掉落结算前挂到怪物上，经 {@link LootRegistry.ExtraLootBuff} 分发。
	 */
	public static class ReservedOpTerminalPluginExtraLoot extends LootRegistry.ExtraLootBuff {

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
}
