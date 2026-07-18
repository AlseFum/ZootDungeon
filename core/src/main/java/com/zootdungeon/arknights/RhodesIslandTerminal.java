package com.zootdungeon.arknights;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.arknights.plugins.SkillSheet;
import com.zootdungeon.arknights.plugins.SkillSheet.SkillDef;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.artifacts.Artifact;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.TextureRegistry;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndRhodesIslandTerminal;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RhodesIslandTerminal extends Artifact {

	public static final String AC_OPEN = "OPEN";

	public static final int COST_CAP = 99;
	/** 固定技能槽位数。 */
	public static final int MAX_SKILL_SLOTS = 3;

	// 装在本终端的技能 id 集合（按安装顺序）。随物品本身 store/restore。
	private final Set<String> installedSkillIds = new LinkedHashSet<>();

	// 本终端当前持有的 COST。
	private int cost;

	/** 当前终端可安装的技能上限（写死为 {@link #MAX_SKILL_SLOTS}）。 */
	public static int maxSkillSlots( Hero hero ) {
		return MAX_SKILL_SLOTS;
	}

	/** 当前 COST。 */
	public int cost() {
		return cost;
	}

	/**
	 * 把 COST 直接设成 {@code v}（允许超出 cap，用于初始化）。
	 * @return 设入后的值
	 */
	public int setCost( int v ) {
		cost = Math.max(0, v);
		return cost;
	}

	/**
	 * 增加 COST，受 {@link #effectiveCostCap(Hero)} 限制。
	 * @return 实际增加量
	 */
	public int addCost( Hero hero, int delta ) {
		if (delta <= 0) return 0;
		int cap = effectiveCostCap(hero);
		int before = cost;
		cost = Math.min(cap, cost + delta);
		return cost - before;
	}

	/**
	 * 消耗 COST。
	 * @return 消耗成功
	 */
	public boolean spendCost( int amount ) {
		if (amount <= 0) return true;
		if (cost < amount) return false;
		cost -= amount;
		return true;
	}

	/**
	 * 找到 {@code hero} 身上（背包+装备）第一个 {@link RhodesIslandTerminal}。
	 * 多终端并存场景下：UI 流程应让玩家主动选择；这里只是兜底取一个。
	 */
	public static RhodesIslandTerminal ownedBy( Hero hero ) {
		if (hero == null) return null;
		for (Item it : hero.belongings) {
			if (it instanceof RhodesIslandTerminal t) return t;
		}
		return null;
	}

	/** 兜底：玩家身上无终端时返回 0，避免调用方到处判空。 */
	public static int costOrZero( Hero hero ) {
		RhodesIslandTerminal t = ownedBy(hero);
		return t != null ? t.cost() : 0;
	}

	/**
	 * 从背包中拿一份 {@link SkillRecord} 安装到终端：解析为对应 {@link Skill} 后加入本终端
	 * 已安装列表。安装成功消耗物品。
	 *
	 * @return 安装是否成功（已安装/槽位满/无效物品等都会返回 false）
	 */
	public boolean installSkillFromItem( Hero hero, Item item ) {
		if (hero == null || !(item instanceof SkillRecord si)) return false;
		SkillDef s = si.skill();
		if (s == null) return false;
		if (isInstalled(s)) {
			GLog.w(Messages.get(RhodesIslandTerminal.class, "skill_already_installed"));
			return false;
		}
		if (installedSkillIds.size() >= maxSkillSlots(hero)) {
			GLog.w(Messages.get(RhodesIslandTerminal.class, "skill_slot_full"));
			return false;
		}
		if (installedSkillIds.add(s.id)) {
			item.detach(hero.belongings.backpack);
			GLog.p(Messages.get(RhodesIslandTerminal.class, "skill_installed", s.name));
			return true;
		}
		return false;
	}

	/** 从本终端卸载一个已安装的技能。 */
	public boolean uninstallSkill( SkillDef skill ) {
		if (skill == null) return false;
		if (installedSkillIds.remove(skill.id)) {
			GLog.p(Messages.get(RhodesIslandTerminal.class, "skill_uninstalled", skill.name));
			return true;
		}
		return false;
	}

	/** 本终端已安装的技能列表（按安装顺序），跳过注册表中已不存在的 id。 */
	public List<SkillDef> installedSkills() {
		List<SkillDef> result = new ArrayList<>(installedSkillIds.size());
		for (String id : installedSkillIds) {
			SkillDef s = SkillSheet.get(id);
			if (s != null) result.add(s);
		}
		return result;
	}

	public boolean isInstalled( SkillDef skill ) {
		return skill != null && installedSkillIds.contains(skill.id);
	}

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
		RhodesIslandTerminal terminal = ownedBy(hero);
		if (terminal == null) return;
		int pool = terminal.cost();
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
		terminal.setCost(0);
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

	static {
		TextureRegistry.texture("sheet.cola.command_terminal", "cola/command_terminal.png")
				.setArea("rhodes_island_terminal", 0, 0, 32, 32);
	}

	{
		image = TextureRegistry.idByLabel("rhodes_island_terminal");
		defaultAction = AC_OPEN;
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

	public class TerminalBuff extends ArtifactBuff {

		public void gainCharge(float levelPortion) {
			if (cursed || target == null || !(target instanceof Hero)) return;
			Hero h = (Hero) target;
			int cap = RhodesIslandTerminal.effectiveCostCap(h);
			int before = cost;
			if (before >= cap) return;
			float gain = 2f * levelPortion;
			partialCharge += gain;
			while (partialCharge >= 1f && cost < cap) {
				partialCharge -= 1f;
				cost = Math.min(cap, cost + 1);
			}
		}
	}

	private static final String INSTALLED = "installed";
	private static final String COST = "cost";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( INSTALLED, installedSkillIds.toArray(new String[0]) );
		bundle.put( COST, cost );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		installedSkillIds.clear();
		String[] arr = bundle.getStringArray( INSTALLED );
		if (arr != null) {
			Collections.addAll(installedSkillIds, arr);
		}
		cost = bundle.getInt( COST );

		// 旧版本 cost 存在 Dungeon.cost 字段，本次重构后挪到本终端。Dungeon.restoreFromBundle
		// 先于本方法执行，把旧值暂存在 Dungeon.legacyCost。第一个恢复的终端拿走它并清零。
		if (cost == 0 && Dungeon.legacyCost > 0) {
			cost = Math.min(cost = Dungeon.legacyCost, COST_CAP);
			Dungeon.legacyCost = 0;
		}
	}
}
