/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zootdungeon.items;

import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.utils.Dice;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.HeroClass;
import com.zootdungeon.actors.hero.HeroSubClass;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

abstract public class KindOfWeapon extends EquipableItem {

	protected String hitSound = Assets.Sounds.HIT;
	protected float hitSoundPitch = 1f;
	
	@Override
	public void execute(Hero hero, String action) {
		if (hero.subClass == HeroSubClass.CHAMPION && action.equals(AC_EQUIP)){
			usesTargeting = false;
			String primaryName = Messages.titleCase(hero.belongings.weapon != null ? hero.belongings.weapon.trueName() : Messages.get(KindOfWeapon.class, "empty"));
			String secondaryName = Messages.titleCase(hero.belongings.secondWep != null ? hero.belongings.secondWep.trueName() : Messages.get(KindOfWeapon.class, "empty"));
			if (primaryName.length() > 18) primaryName = primaryName.substring(0, 15) + "...";
			if (secondaryName.length() > 18) secondaryName = secondaryName.substring(0, 15) + "...";
			GameScene.show(new WndOptions(
					new ItemSprite(this),
					Messages.titleCase(name()),
					Messages.get(KindOfWeapon.class, "which_equip_msg"),
					Messages.get(KindOfWeapon.class, "which_equip_primary", primaryName),
					Messages.get(KindOfWeapon.class, "which_equip_secondary", secondaryName)
			){
				@Override
				protected void onSelect(int index) {
					super.onSelect(index);
					if (index == 0 || index == 1){
						//In addition to equipping itself, item reassigns itself to the quickslot
						//This is a special case as the item is being removed from inventory, but is staying with the hero.
						int slot = Dungeon.quickslot.getSlot( KindOfWeapon.this );
						slotOfUnequipped = -1;
						if (index == 0) {
							doEquip(hero);
						} else {
							equipSecondary(hero);
						}
						if (slot != -1) {
							Dungeon.quickslot.setSlot( slot, KindOfWeapon.this );
							updateQuickslot();
						//if this item wasn't quickslotted, but the item it is replacing as equipped was
						//then also have the item occupy the unequipped item's quickslot
						} else if (slotOfUnequipped != -1 && defaultAction() != null) {
							Dungeon.quickslot.setSlot( slotOfUnequipped, KindOfWeapon.this );
							updateQuickslot();
						}
					}
				}
			});
		} else {
			super.execute(hero, action);
		}
	}

	@Override
	public boolean isEquipped( Hero hero ) {
		return hero != null && (hero.belongings.weapon() == this || hero.belongings.secondWep() == this);
	}

	private static boolean isSwiftEquipping = false;

	protected float timeToEquip( Hero hero ) {
		return isSwiftEquipping ? 0f : super.timeToEquip(hero);
	}
	
	@Override
	public boolean doEquip( Hero hero ) {

		isSwiftEquipping = false;
		if (hero.belongings.contains(this) && hero.hasTalent(Talent.SWIFT_EQUIP)){
			if (hero.buff(Talent.SwiftEquipCooldown.class) == null
					|| hero.buff(Talent.SwiftEquipCooldown.class).hasSecondUse()){
				isSwiftEquipping = true;
			}
		}

		// 15/25% chance
		if (hero.heroClass != HeroClass.CLERIC && hero.hasTalent(Talent.HOLY_INTUITION)
				&& cursed && !cursedKnown
				&& Random.Int(20) < 1 + 2*hero.pointsInTalent(Talent.HOLY_INTUITION)){
			cursedKnown = true;
			GLog.p(Messages.get(this, "curse_detected"));
			return false;
		}

		detachAll( hero.belongings.backpack );
		
		if (hero.belongings.weapon == null || hero.belongings.weapon.doUnequip( hero, true )) {
			
			hero.belongings.weapon = this;
			activate( hero );
			Talent.onItemEquipped(hero, this);
			Badges.validateDuelistUnlock();
			updateQuickslot();

			cursedKnown = true;
			if (cursed) {
				equipCursed( hero );
				GLog.n( Messages.get(KindOfWeapon.class, "equip_cursed") );
			}

			hero.spendAndNext( timeToEquip(hero) );
			if (isSwiftEquipping) {
				GLog.i(Messages.get(this, "swift_equip"));
				if (hero.buff(Talent.SwiftEquipCooldown.class) == null){
					Buff.affect(hero, Talent.SwiftEquipCooldown.class, 19f)
							.secondUse = hero.pointsInTalent(Talent.SWIFT_EQUIP) == 2;
				} else if (hero.buff(Talent.SwiftEquipCooldown.class).hasSecondUse()) {
					hero.buff(Talent.SwiftEquipCooldown.class).secondUse = false;
				}
				isSwiftEquipping = false;
			}
			return true;
			
		} else {
			isSwiftEquipping = false;
			collect( hero.belongings.backpack );
			return false;
		}
	}

	public boolean equipSecondary( Hero hero ){

		isSwiftEquipping = false;
		if (hero.belongings.contains(this) && hero.hasTalent(Talent.SWIFT_EQUIP)){
			if (hero.buff(Talent.SwiftEquipCooldown.class) == null
					|| hero.buff(Talent.SwiftEquipCooldown.class).hasSecondUse()){
				isSwiftEquipping = true;
			}
		}

		boolean wasInInv = hero.belongings.contains(this);
		detachAll( hero.belongings.backpack );

		if (hero.belongings.secondWep == null || hero.belongings.secondWep.doUnequip( hero, true )) {

			hero.belongings.secondWep = this;
			activate( hero );
			Talent.onItemEquipped(hero, this);
			Badges.validateDuelistUnlock();
			updateQuickslot();

			cursedKnown = true;
			if (cursed) {
				equipCursed( hero );
				GLog.n( Messages.get(KindOfWeapon.class, "equip_cursed") );
			}

			hero.spendAndNext( timeToEquip(hero) );
			if (isSwiftEquipping) {
				GLog.i(Messages.get(this, "swift_equip"));
				if (hero.buff(Talent.SwiftEquipCooldown.class) == null){
					Buff.affect(hero, Talent.SwiftEquipCooldown.class, 19f)
							.secondUse = hero.pointsInTalent(Talent.SWIFT_EQUIP) == 2;
				} else if (hero.buff(Talent.SwiftEquipCooldown.class).hasSecondUse()) {
					hero.buff(Talent.SwiftEquipCooldown.class).secondUse = false;
				}
				isSwiftEquipping = false;
			}
			return true;

		} else {
			isSwiftEquipping = false;
			collect( hero.belongings.backpack );
			return false;
		}
	}

	@Override
	public boolean doUnequip( Hero hero, boolean collect, boolean single ) {
		boolean second = hero.belongings.secondWep == this;

		if (second){
			//do this first so that the item can go to a full inventory
			hero.belongings.secondWep = null;
		}

		if (super.doUnequip( hero, collect, single )) {

			if (!second){
				hero.belongings.weapon = null;
			}
			return true;

		} else {

			if (second){
				hero.belongings.secondWep = this;
			}
			return false;

		}
	}

	public int min(){
		return min(buffedLvl());
	}

	public int max(){
		return max(buffedLvl());
	}

	abstract public int min(int lvl);
	abstract public int max(int lvl);

	/**
	 * 构建武器在指定等级下的“基础伤害骰组”（不包含 exSTR 和其它加成）。
	 * 这里使用一个简单的 1dX + K 近似 min-max 区间，供描述与调试使用。
	 */
	public Dice baseDamageDice(int lvl){
		int mn = min(lvl);
		int mx = max(lvl);
		if (mn >= mx){
			return Dice.of(mn);
		}
		int span = mx - mn;
		// 使用 1d(span+1) + (mn-1) 覆盖 [mn, mx] 区间
		return Dice.of(new Dice.Die(1, span + 1), mn - 1);
	}

	/**
	 * 计算针对指定角色的 exSTR（额外力量）伤害加成（数值形式）。
	 * 之前是 Hero.heroDamageIntRange(0, exStr)，为了保留 clover 等机制，仍然使用该入口。
	 */
	protected int rollExStrBonus(Hero hero){
		int req = (this instanceof Weapon) ? ((Weapon) this).STRReq() : 0;
		int exStr = hero.STR() - req;
		if (exStr > 0){
			return Hero.heroDamageIntRange(0, exStr);
		}
		return 0;
	}

	/**
	 * 将 exSTR 对伤害的影响体现在骰组中，用于描述：
	 * 0..exStr 近似为 1d(exStr+1) - 1。
	 */
	public Dice exSTRDice(Hero hero){
		int req = (this instanceof Weapon) ? ((Weapon) this).STRReq() : 0;
		int exStr = hero.STR() - req;
		if (exStr <= 0){
			// 无额外力量加成时返回空骰组，合并时不会改变结果
			return new Dice();
		}
		// 对应 0..exStr 的波动：1d(exStr+1) - 1
		return Dice.of(new Dice.Die(1, exStr + 1), -1);
	}

	/**
	 * 统一构建“武器完整伤害骰组”（基础 + exSTR），用于 UI 描述或调试。
	 * 真正的数值结算仍走 damageRoll 及 heroDamageIntRange，保证旧存档与平衡不被突变破坏。
	 */
	public Dice damageDice(Char owner){
		Dice dice = baseDamageDice(buffedLvl());
		if (owner instanceof Hero){
			dice.addAll(exSTRDice((Hero) owner));
		}
		return dice;
	}

	public int damageRoll( Char owner ) {
		Dice dice = damageDice(owner);
		return dice.rollTotalGame();
	}
	
	public float accuracyFactor( Char owner, Char target ) {
		return 1f;
	}
	
	public float delayFactor( Char owner ) {
		return 1f;
	}

	public int reachFactor( Char owner ){
		return 1;
	}
	
	public boolean canReach( Char owner, int target){
		int reach = reachFactor(owner);
		if (Dungeon.level.distance( owner.pos, target ) > reach){
			return false;
		} else {
			boolean[] passable = BArray.not(Dungeon.level.solid, null);
			for (Char ch : Actor.chars()) {
				if (ch != owner) passable[ch.pos] = false;
			}
			
			PathFinder.buildDistanceMap(target, passable, reach);
			
			return PathFinder.distance[owner.pos] <= reach;
		}
	}

	public int defenseFactor( Char owner ) {
		return 0;
	}
	
	public int proc( Char attacker, Char defender, int damage ) {
		return damage;
	}

	public void hitSound( float pitch ){
		Sample.INSTANCE.play(hitSound, 1, pitch * hitSoundPitch);
	}
	
}
