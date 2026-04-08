/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
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

package com.zootdungeon.arknights.necrass;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.GhostSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;

public class NecrassCard extends MissileWeapon {
	static {
		SpriteRegistry.texture("sheet.cola.necrass_card", "cola/necrass_card.png")
				.grid(32, 32)
				.label("necrass_card");
	}

	{
		image = SpriteRegistry.byLabel("necrass_card");
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.2f;

		bones = false;

		tier = 2;
		baseUses = 8;
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	@Override
	public String info() {
		String info = super.info();

		String distanceBlock = "\n\n" + Messages.get(MissileWeapon.class, "distance");
		info = info.replace(distanceBlock, "");

		String durabilityHeader = "\n\n" + Messages.get(this, "durability");
		int idx = info.indexOf(durabilityHeader);
		return idx >= 0 ? info.substring(0, idx) : info;
	}

	@Override
	public int max(int lvl) {
		return 8 * tier + (tier == 1 ? 2 * lvl : tier * lvl);
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		// 检查是否会击杀敌人
		if (attacker instanceof Hero && defender instanceof Mob) {
			boolean willKill = defender.HP <= damage;

			if (willKill && defender.alignment == Char.Alignment.ENEMY) {
				// 延迟生成仆役，在伤害应用之后
				final int targetPos = defender.pos;
				final Hero hero = (Hero) attacker;

				// 使用延迟回调来确保在敌人死亡后生成仆役
				Actor.add(new Actor() {
					{
						actPriority = VFX_PRIO - 1; // 在视觉效果之前执行
					}

					@Override
					protected boolean act() {
						// 检查敌人是否真的死亡了
						Char target = Actor.findChar(targetPos);
						if (target == null || !target.isAlive()) {
							// 敌人已死亡，生成仆役
							createMinion(targetPos, hero);
						}
						Actor.remove(this);
						return true;
					}
				});
			}
		}

		return damage;
	}

	@Override
	protected void rangedHit(Char enemy, int cell) {
		// 命中后照常消耗耐久（用于断裂提示/统计），但不掉落回地图：投掷即消散
		decrementDurability();
		parent = null;
		durability = 0;
	}

	@Override
	protected void rangedMiss(int cell) {
		// 落空同样消散（默认实现会 onThrow() 掉落到地面）
		decrementDurability();
		parent = null;
		durability = 0;
	}

	private void createMinion(int pos, Hero hero) {
		// 找到敌人附近的空位置
		int summonPos = findNearbyEmptyCell(pos);
		if (summonPos == -1) {
			// 如果没有空位置，就放在敌人死亡的位置
			summonPos = pos;
		}

		// 根据武器等级决定仆役的强度
		int powerLevel = buffedLvl() + tier * 2;

		// 创建仆役
		SummonedServant servant = new SummonedServant(powerLevel, tier);
		servant.pos = summonPos;
		servant.state = servant.HUNTING;

		// 添加到场景
		GameScene.add(servant);

		// 召唤特效
		CellEmitter.get(summonPos).burst(Speck.factory(Speck.STAR), 6);
		Sample.INSTANCE.play(Assets.Sounds.MELD);

		GLog.p(Messages.get(this, "msg_summoned", servant.name()));
	}

	private int findNearbyEmptyCell(int centerPos) {
		// 先检查8个相邻位置
		for (int offset : PathFinder.NEIGHBOURS8) {
			int pos = centerPos + offset;
			if (pos >= 0 && pos < Dungeon.level.length()
					&& Dungeon.level.passable[pos]
					&& Actor.findChar(pos) == null) {
				return pos;
			}
		}

		// 如果相邻位置没有，检查更远的位置（距离2）
		for (int offset : PathFinder.NEIGHBOURS8) {
			int pos = centerPos + offset * 2;
			if (pos >= 0 && pos < Dungeon.level.length()
					&& Dungeon.level.passable[pos]
					&& Actor.findChar(pos) == null) {
				return pos;
			}
		}

		return -1; // 没有找到空位置
	}

	// 仆役内部类
	public static class SummonedServant extends Mob {

		private int powerLevel;
		private int weaponTier;
		private static final int FOLLOW_RANGE = 8; // 追随玩家的范围（超过此距离会主动靠近）
		private static final int IDEAL_DISTANCE = 2; // 理想跟随距离（保持在此距离附近）
		private static final int ATTACK_RANGE = 5; // 攻击玩家的敌人范围

		{
			spriteClass = GhostSprite.class;
			alignment = Alignment.ALLY;
			intelligentAlly = true; // 设置为智能盟友，可以更好地跟随玩家

			// 不生成战利品
			lootChance = 0f;

			// 不显示经验值
			EXP = 0;

			// 默认状态
			state = HUNTING;
		}

		public SummonedServant(int powerLevel, int weaponTier) {
			this.powerLevel = powerLevel;
			this.weaponTier = weaponTier;

			// 根据武器等级设置属性
			// 基础HP: 15 + powerLevel * 3 + tier * 4
			HP = HT = 15 + powerLevel * 3 + weaponTier * 4;

			// 基础防御: 3 + powerLevel + tier * 2
			defenseSkill = 3 + powerLevel + weaponTier * 2;

			// 最大等级: (powerLevel + tier) / 2 (向上取整)
			maxLvl = Math.max(1, (powerLevel + weaponTier + 1) / 2);
		}

		@Override
		public int damageRoll() {
			// 伤害: 基础2-5 + powerLevel + tier * 2
			int base = Random.NormalIntRange(2, 5);
			return base + powerLevel + weaponTier * 2;
		}

		@Override
		public int attackSkill(Char target) {
			// 攻击技能: 基础10 + powerLevel * 2 + tier * 2
			return 10 + powerLevel * 2 + weaponTier * 2;
		}

		@Override
		public int drRoll() {
			// 伤害减免: 0 到 (powerLevel + tier) / 2
			return Random.NormalIntRange(0, (powerLevel + weaponTier) / 2);
		}

		@Override
		public String name() {
			return Messages.get(this, "name", powerLevel, weaponTier);
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		protected boolean act() {
			// 如果英雄死亡，仆役也消失
			if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
				die(null);
				return true;
			}

			// 初始化视野数组（如果为 null 或长度不匹配）
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
				fieldOfView = new boolean[Dungeon.level.length()];
			}

			// 更新视野
			Dungeon.level.updateFieldOfView(this, fieldOfView);

			// 寻找玩家范围内的敌人
			Char target = findEnemyNearHero();

			if (target != null) {
				// 找到敌人，攻击它
				enemy = target;
				enemySeen = true;
				state = HUNTING;
				// 调用父类的 act 方法处理攻击
				return super.act();
			} else {
				// 没有敌人，追随玩家
				enemy = null;
				enemySeen = false;
				state = HUNTING;
				this.target = Dungeon.hero.pos; // 设置目标位置为玩家位置
				followHero();
				return true;
			}
		}

		private Char findEnemyNearHero() {
			if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
				return null;
			}

			int heroPos = Dungeon.hero.pos;
			Char closest = null;
			int closestDist = Integer.MAX_VALUE;

			// 在玩家周围寻找敌人
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.alignment == Alignment.ENEMY && mob.isAlive() && mob != this) {
					int distToHero = Dungeon.level.distance(mob.pos, heroPos);
					int distToMe = Dungeon.level.distance(mob.pos, pos);

					// 敌人必须在玩家范围内，且在视野内
					if (distToHero <= ATTACK_RANGE && fieldOfView[mob.pos] && mob.invisible <= 0) {
						if (distToMe < closestDist) {
							closest = mob;
							closestDist = distToMe;
						}
					}
				}
			}

			return closest;
		}

		private void followHero() {
			if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
				spend(1 / speed());
				return;
			}

			int heroPos = Dungeon.hero.pos;
			int distToHero = Dungeon.level.distance(pos, heroPos);

			// 如果距离玩家太远（超过跟随范围），快速靠近
			if (distToHero > FOLLOW_RANGE) {
				if (!getCloser(heroPos)) {
					spend(1 / speed());
				}
			} else if (distToHero > IDEAL_DISTANCE) {
				// 距离在理想距离和跟随范围之间，慢慢靠近玩家
				if (!getCloser(heroPos)) {
					spend(1 / speed());
				}
			} else {
				// 距离合适（在理想距离内），保持静止
				spend(1 / speed());
			}
		}

		@Override
		public void die(Object cause) {
			super.die(cause);
			// 仆役死亡时可能有特效
		}

		@Override
		public Item createLoot() {
			// 仆役不生成战利品
			return null;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("powerLevel", powerLevel);
			bundle.put("weaponTier", weaponTier);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			powerLevel = bundle.getInt("powerLevel");
			weaponTier = bundle.getInt("weaponTier");
		}
	}
}
