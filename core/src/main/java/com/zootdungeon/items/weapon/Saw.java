package com.zootdungeon.items.weapon;
import com.zootdungeon.items.weapon.base.MeleeWeapon;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class Saw extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SHORTSWORD; // TODO: replace with saw sprite
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

		tier = 3;
		DLY = 0.9f;
	}

	@Override
	protected int baseChargeUse(Hero hero, Char target) {
		return 1;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		Saw.sawAbility(hero, target, 1.2f, 0, this);
	}

	public static void sawAbility(Hero hero, Integer target, float dmgMulti, int dmgBonus, MeleeWeapon wep) {
		if (target == null) return;

		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}

		hero.belongings.abilityWeapon = wep;
		if (!hero.canAttack(enemy)) {
			GLog.w(Messages.get(wep, "ability_target_range"));
			hero.belongings.abilityWeapon = null;
			return;
		}
		hero.belongings.abilityWeapon = null;

		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				wep.beforeAbilityUsed(hero, enemy);
				AttackIndicator.target(enemy);
				if (hero.attack(enemy, dmgMulti, dmgBonus, 1f)) {
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				}
				wep.afterAbilityUsed(hero);
				hero.spendAndNext(hero.attackDelay());
			}
		});
	}

	@Override
	public String abilityInfo() {
		return Messages.get(this, "ability_desc");
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(level + 3);
	}
}
