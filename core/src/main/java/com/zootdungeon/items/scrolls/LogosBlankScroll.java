package com.zootdungeon.items.scrolls;
import com.zootdungeon.items.ItemEffects;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndBag;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * LOGOS talent: Blank scroll created after using a scroll.
 * Can be infused with wand charges to act as a one-time extra wand zap.
 */
public class LogosBlankScroll extends Scroll {

	public static final String AC_INFUSE = "INFUSE";
	public static final String AC_ZAP    = "ZAP";

	private Class<? extends Wand> infusedWand = null;
	private int infusedCharges = 0;
	private int wandLevel = 1;

	{
		image = ItemSpriteSheet.SCROLL_ISAZ;
		defaultAction = AC_INFUSE;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (infusedWand != null && infusedCharges > 0) {
			actions.add(AC_ZAP);
		}
		if (infusedWand == null) {
			actions.add(AC_INFUSE);
		}
		return actions;
	}

	@Override
	public void execute(final Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_INFUSE)) {
			curUser = hero;
			curItem = this;
			GameScene.selectItem(new WndBag.ItemSelector() {
				@Override
				public String textPrompt() {
					return Messages.get(LogosBlankScroll.class, "infuse_prompt");
				}
				@Override
				public boolean itemSelectable(Item item) {
					return item instanceof Wand && ((Wand) item).curCharges > 0;
				}
				@Override
				public void onSelect(Item item) {
					if (item instanceof Wand) {
						Wand w = (Wand) item;
						int used = Math.min(w.curCharges, 3);
						w.curCharges -= used;
						infusedWand = w.getClass();
						infusedCharges = used;
						wandLevel = w.buffedLvl();
						GLog.p(Messages.get(LogosBlankScroll.class, "infused",
							Messages.get(w, "name"), used));
					}
				}
			});
		} else if (action.equals(AC_ZAP) && infusedWand != null && infusedCharges > 0) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect(Integer cell) {
					if (cell == null) return;
					Ballistica shot = new Ballistica(curUser.pos, cell, Ballistica.MAGIC_BOLT);
					try {
						Wand w = infusedWand.getDeclaredConstructor().newInstance();
						w.level(wandLevel);
						w.curCharges = 1;
						w.onZap(shot);
					} catch (Exception e) {
						GLog.w(Messages.get(LogosBlankScroll.class, "failed"));
					}
					infusedCharges--;
					if (infusedCharges <= 0) {
						detach(curUser.belongings.backpack);
					}
					curUser.spendAndNext(1f);
				}
				@Override
				public String prompt() {
					return Messages.get(LogosBlankScroll.class, "zap_prompt");
				}
			});
		}
	}

	@Override
	public void doRead() {
		// Blank scrolls aren't read — they're infused or zapped
	}

	@Override
	public String name() {
		if (infusedWand != null) {
			return Messages.get(this, "infused_name", Messages.get(infusedWand, "name"));
		}
		return super.name();
	}

	@Override
	public String desc() {
		if (infusedWand != null) {
			return Messages.get(this, "infused_desc", Messages.get(infusedWand, "name"),
					infusedCharges, wandLevel);
		}
		return super.desc();
	}

	@Override
	public int value() {
		return 20;
	}

	private static final String INFUSED_WAND    = "infusedWand";
	private static final String INFUSED_CHARGES = "infusedCharges";
	private static final String WAND_LEVEL      = "wandLevel";

	@Override
	@SuppressWarnings("unchecked")
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(INFUSED_WAND, infusedWand != null ? infusedWand.getName() : "");
		bundle.put(INFUSED_CHARGES, infusedCharges);
		bundle.put(WAND_LEVEL, wandLevel);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			String s = bundle.getString(INFUSED_WAND);
			if (!s.isEmpty()) infusedWand = (Class<? extends Wand>) Class.forName(s);
		} catch (ClassNotFoundException e) {
			infusedWand = null;
		}
		infusedCharges = bundle.getInt(INFUSED_CHARGES);
		wandLevel = bundle.getInt(WAND_LEVEL);
	}
}
