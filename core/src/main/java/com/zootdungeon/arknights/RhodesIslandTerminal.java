package com.zootdungeon.arknights;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.artifacts.Artifact;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.windows.WndRhodesIslandTerminal;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;

public class RhodesIslandTerminal extends Artifact {

	public static final String AC_OPEN = "OPEN";

	public static final int COST_CAP = 99;
	public static final int MAX_PLUGINS = 3;

	private static final String PLUGINS = "plugins";
	private static final String PLUGIN_STORAGE = "plugin_storage";

	/** 已安装的插件（从背包安装后存放在此） */
	private ArrayList<TerminalPlugin> installedPlugins = new ArrayList<>();
	/** 与 installedPlugins 一一对应的插件存储，供插件读写状态 */
	private ArrayList<Bundle> pluginStorageData = new ArrayList<>();

	static {
		SpriteRegistry.registerItemTexture("cola/command_terminal.png", 32)
				.label("rhodes_island_terminal");
	}

	{
		image = SpriteRegistry.itemByName("rhodes_island_terminal");
		defaultAction = AC_OPEN;
	}

	/** 获取已安装的插件列表（只读） */
	public ArrayList<TerminalPlugin> getInstalledPlugins() {
		return installedPlugins;
	}

	/** 是否还能安装更多插件 */
	public boolean canInstallMorePlugins() {
		return installedPlugins.size() < MAX_PLUGINS;
	}

	/** 获取或创建指定插件的存储 Bundle，供插件读写状态。按 installedPlugins 中的索引一一对应。 */
	public Bundle getOrCreatePluginStorage(TerminalPlugin plugin) {
		int idx = installedPlugins.indexOf(plugin);
		if (idx < 0) return null;
		while (pluginStorageData.size() <= idx) {
			pluginStorageData.add(new Bundle());
		}
		return pluginStorageData.get(idx);
	}

	/** 安装插件：从背包中移除该物品并加入终端。成功返回 true。安装成功后调用 plugin.mount(this)。 */
	public boolean installPlugin(TerminalPlugin plugin, Hero hero) {
		if (plugin == null || !canInstallMorePlugins()) return false;
		if (!hero.belongings.backpack.contains(plugin)) return false;
		plugin.detach(hero.belongings.backpack);
		installedPlugins.add(plugin);
		pluginStorageData.add(new Bundle());
		plugin.mount(this);
		return true;
	}

	/** 卸载插件：从终端移除并放回英雄背包。卸载前调用 plugin.onUnmount(this)。成功返回被卸载的插件。 */
	public TerminalPlugin uninstallPlugin(int index, Hero hero) {
		if (index < 0 || index >= installedPlugins.size()) return null;
		TerminalPlugin plugin = installedPlugins.get(index);
		plugin.onUnmount(this);
		installedPlugins.remove(index);
		if (index < pluginStorageData.size()) {
			pluginStorageData.remove(index);
		}
		if (plugin != null && hero.belongings.backpack != null) {
			if (!plugin.collect(hero.belongings.backpack)) {
				Dungeon.level.drop(plugin, hero.pos);
			}
		}
		return plugin;
	}

	/** 计算当前部署费用恢复倍率（基础 1.0 + 各插件加成） */
	public float getCostRegenMultiplier() {
		float mult = 1f;
		for (TerminalPlugin p : installedPlugins) {
			mult *= p.costRegenMultiplier();
		}
		return mult;
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
		bundle.put(PLUGINS, new ArrayList<>(installedPlugins));
		while (pluginStorageData.size() < installedPlugins.size()) {
			pluginStorageData.add(new Bundle());
		}
		for (int i = 0; i < pluginStorageData.size(); i++) {
			bundle.put(PLUGIN_STORAGE + "_" + i, pluginStorageData.get(i));
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		Collection<?> col = bundle.getCollection(PLUGINS);
		installedPlugins = new ArrayList<>();
		for (Object o : col) {
			if (o instanceof TerminalPlugin) {
				installedPlugins.add((TerminalPlugin) o);
			}
		}
		pluginStorageData = new ArrayList<>();
		for (int i = 0; i < installedPlugins.size(); i++) {
			String key = PLUGIN_STORAGE + "_" + i;
			if (bundle.contains(key)) {
				pluginStorageData.add(bundle.getBundle(key));
			} else {
				pluginStorageData.add(new Bundle());
			}
		}
	}

	public class TerminalBuff extends ArtifactBuff {

		public void gainCharge(float levelPortion) {
			if (cursed || target == null || !(target instanceof Hero)) return;
			int cap = RhodesIslandTerminal.COST_CAP;
			if (Dungeon.cost >= cap) return;
			float gain = 2f * levelPortion * getCostRegenMultiplier();
			partialCharge += gain;
			while (partialCharge >= 1f && Dungeon.cost < cap) {
				partialCharge -= 1f;
				Dungeon.cost = Math.min(cap, Dungeon.cost + 1);
			}
		}
	}
}
