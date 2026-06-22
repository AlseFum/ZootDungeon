package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.weapon.RhodesStandardBow;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.ActionIndicator;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.ui.HeroIcon;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;

/**
 * OUTCAST talent 5: High Noon.
 * Charges up over 7/6/5 turns. Has an action to fire all ammo at all visible enemies.
 */
public class OutcastHighNoon extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
	}

	private float chargeTime = 0f;
	private int turnsSinceAttack = 0;

	@Override
	public boolean act() {
		turnsSinceAttack++;
		chargeTime += TICK;
		spend(TICK);
		if (chargeTime >= getDuration()) {
			detach();
		}
		return true;
	}

	private float getDuration() {
		if (!(target instanceof Hero)) return 7f;
		int pts = ((Hero) target).pointsInTalent(Talent.OUTCAST_HIGH_NOON);
		int[] durations = {0, 7, 6, 5};
		return durations[pts];
	}

	/** Called when the hero attacks — resets charge buildup */
	public void onAttack() {
		turnsSinceAttack = 0;
	}

	public int getTurnsSinceAttack() {
		return turnsSinceAttack;
	}

	@Override
	public int icon() {
		return BuffIndicator.TIME;
	}

	@Override
	public void tintIcon(com.watabou.noosa.Image icon) {
		icon.hardlight(1f, 0.3f, 0.1f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) (getDuration() - chargeTime / TICK));
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", (int) (getDuration() - chargeTime / TICK));
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	// region ActionIndicator

	@Override
	public String actionName() {
		return Messages.get(this, "action_name");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.ACE_COMBO; // TODO: dedicated icon
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(Integer.toString(turnsSinceAttack), PixelScene.pixelFont);
		txt.hardlight(CharSprite.WARNING);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		return 0xCC3300;
	}

	@Override
	public void doAction() {
		GameScene.show(new com.zootdungeon.windows.WndOptions(
				Messages.get(this, "action_name"),
				Messages.get(this, "confirm"),
				Messages.get(this, "yes"),
				Messages.get(this, "no")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) {
					executeHighNoon();
				}
				hide();
			}
		});
	}

	private void executeHighNoon() {
		Hero hero = (Hero) target;
		RhodesStandardBow bow = hero.belongings.getItem(RhodesStandardBow.class);
		if (bow == null) return;

		float[] dmgMult = {1f, 2f, 3f, 4f};
		int pts = hero.pointsInTalent(Talent.OUTCAST_HIGH_NOON);

		// Fire at all visible enemies
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.heroFOV[ch.pos]) {
				int dmg = Math.round((bow.max() + bow.min()) / 2f * dmgMult[pts]);
				ch.damage(dmg, hero);
			}
		}

		// Empty the clip
		RhodesStandardBow.RhodesArrowStuck stuck = hero.buff(RhodesStandardBow.RhodesArrowStuck.class);
		if (stuck != null) stuck.detach();

		hero.spendAndNext(hero.attackDelay());
		detach();
	}

	// endregion

	private static final String CHARGE_TIME    = "chargeTime";
	private static final String TURNS_SINCE    = "turnsSinceAttack";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE_TIME, chargeTime);
		bundle.put(TURNS_SINCE, turnsSinceAttack);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		chargeTime = bundle.getFloat(CHARGE_TIME);
		turnsSinceAttack = bundle.getInt(TURNS_SINCE);
	}
}
