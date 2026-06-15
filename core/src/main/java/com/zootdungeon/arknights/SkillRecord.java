package com.zootdungeon.arknights;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.arknights.plugins.SkillSheet;
import com.zootdungeon.arknights.plugins.SkillSheet.SkillDef;
import com.zootdungeon.items.Item;
import com.zootdungeon.sprites.TextureRegistry;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * 可安装到罗德岛终端的"技能档案"物品。在背包中使用时会调用
 * {@link RhodesIslandTerminal#installSkillFromItem}，把对应 {@link SkillDef} 加入已安装列表。
 */
public class SkillRecord extends Item {

	private static final String SKILL_ID = "skill_id";

	public static final String AC_INSTALL = "INSTALL";

	static {
		TextureRegistry.texture("sheet.cola.mod_unlock_token", "cola/skill_record_1.png")
				.grid(32, 32)
				.label("mod_unlock_token");
	}

	private String skillId;

	public SkillRecord() {
		this(SkillSheet.SKILL_1);
	}

	public SkillRecord(SkillDef skill) {
		this.skillId = skill != null ? skill.id : null;
		image = TextureRegistry.idByLabel("mod_unlock_token");
		levelKnown = true;
		cursedKnown = true;
	}

	/** 关联的 {@link SkillDef}（可能为 null，例如存档中保存的 id 已不存在）。 */
	public SkillDef skill() {
		return skillId != null ? SkillSheet.get(skillId) : null;
	}

	public String skillId() {
		return skillId;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_INSTALL);
		return actions;
	}

	@Override
	public String defaultAction() {
		return AC_INSTALL;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (AC_INSTALL.equals(action)) {
			List<RhodesIslandTerminal> terminals = new ArrayList<>();
			for (Item it : hero.belongings) {
				if (it instanceof RhodesIslandTerminal t) terminals.add(t);
			}
			if (terminals.isEmpty()) {
				com.zootdungeon.utils.GLog.w(
					com.zootdungeon.messages.Messages.get(SkillRecord.class, "no_terminal"));
				return;
			}
			if (terminals.size() == 1) {
				terminals.get(0).installSkillFromItem(hero, this);
				return;
			}
			openTerminalSelector(hero, terminals);
		}
	}

	private void openTerminalSelector( Hero hero, List<RhodesIslandTerminal> terminals ) {
		com.zootdungeon.windows.WndBag.ItemSelector sel = new com.zootdungeon.windows.WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return com.zootdungeon.messages.Messages.get(SkillRecord.class, "choose_terminal_prompt");
			}

			@Override
			public Class<? extends com.zootdungeon.items.bags.Bag> preferredBag() {
				return com.zootdungeon.actors.hero.Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable(Item item) {
				return item instanceof RhodesIslandTerminal;
			}

			@Override
			public void onSelect(Item item) {
				if (item instanceof RhodesIslandTerminal t) {
					t.installSkillFromItem(hero, SkillRecord.this);
				}
			}
		};
		com.zootdungeon.scenes.GameScene.selectItem(sel);
	}

	@Override
	public String name() {
		SkillDef s = skill();
		if (s == null) return "Skill Record (unknown)";
		return "技能档案：" + s.name;
	}

	@Override
	public String info() {
		SkillDef s = skill();
		StringBuilder sb = new StringBuilder();
		if (s != null) {
			sb.append("对应技能：").append(s.name);
			if (s.cost > 0) sb.append("\n消耗：").append(s.cost).append(" COST");
			if (s.desc != null && !s.desc.isEmpty()) {
				sb.append("\n\n").append(s.desc);
			}
		} else {
			sb.append("未识别的技能档案。");
		}
		return sb.toString();
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 40;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SKILL_ID, skillId);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		skillId = bundle.getString(SKILL_ID);
	}
}
