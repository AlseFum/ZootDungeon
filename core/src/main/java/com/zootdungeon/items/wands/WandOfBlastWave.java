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

package com.zootdungeon.items.wands;
import com.zootdungeon.items.ItemEffects;

import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Paralysis;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.effects.Effects;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.effects.Pushing;
import com.zootdungeon.items.weapon.enchantments.Elastic;
import com.zootdungeon.items.weapon.MagesStaff;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.features.Door;
import com.zootdungeon.levels.traps.TenguDartTrap;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.tiles.DungeonTilemap;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class WandOfBlastWave extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_BLAST_WAVE;

		collisionProperties = Ballistica.PROJECTILE;
	}

	public int min(int lvl){
		return 1+lvl;
	}

	public int max(int lvl){
		return 3+3*lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {
		Sample.INSTANCE.play( Assets.Sounds.BLAST );
		ItemEffects.blastWave(bolt.collisionPos);

		//presses all tiles in the AOE first, with the exception of tengu dart traps
		for (int i : PathFinder.NEIGHBOURS9){
			if (!(Dungeon.level.traps.get(bolt.collisionPos+i) instanceof TenguDartTrap)) {
				Dungeon.level.pressCell(bolt.collisionPos + i);
			}
		}

		//throws other chars around the center.
		for (int i  : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(bolt.collisionPos + i);

			if (ch != null){
				wandProc(ch, chargesPerCast());
				if (ch.alignment != Char.Alignment.ALLY) ch.damage(damageRoll(), this);

				//do not push chars that are dieing over a pit, or that move due to the damage
				if ((ch.isAlive() || ch.flying || !Dungeon.level.pit[ch.pos])
						&& ch.pos == bolt.collisionPos + i) {
					Ballistica trajectory = new Ballistica(ch.pos, ch.pos + i, Ballistica.MAGIC_BOLT);
					int strength = 1 + Math.round(buffedLvl() / 2f);
					ItemEffects.knockback(ch, trajectory, strength, false, true, this);
				}

			}
		}

		//throws the char at the center of the blast
		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null){
			wandProc(ch, chargesPerCast());
			ch.damage(damageRoll(), this);

			//do not push chars that are dieing over a pit, or that move due to the damage
			if ((ch.isAlive() || ch.flying || !Dungeon.level.pit[ch.pos])
					&& bolt.path.size() > bolt.dist+1 && ch.pos == bolt.collisionPos) {
				Ballistica trajectory = new Ballistica(ch.pos, bolt.path.get(bolt.dist + 1), Ballistica.MAGIC_BOLT);
				int strength = buffedLvl() + 3;
				ItemEffects.knockback(ch, trajectory, strength, false, true, this);
			}
		}
		
	}


	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {

		Talent.EmpoweredStrikeTracker tracker = attacker.buff(Talent.EmpoweredStrikeTracker.class);

		if (tracker != null){
			tracker.delayedDetach = true;
		}

		//acts like elastic enchantment
		//we delay this with an actor to prevent conflicts with regular elastic
		//so elastic always fully resolves first, then this effect activates
		Actor.add(new Actor() {
			{
				actPriority = VFX_PRIO+9; //act after pushing effects
			}

			@Override
			protected boolean act() {
				Actor.remove(this);
				if (defender.isAlive()) {
					new ItemEffects.BlastWaveEnchantment().proc(staff, attacker, defender, damage);
				}
				if (tracker != null) tracker.detach();
				return true;
			}
		});
	}

	// BlastWaveOnHit moved to ItemEffects.BlastWaveEnchantment

	@Override
	public String upgradeStat2(int level) {
		return Integer.toString(3 + level);
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar( curUser.sprite.parent,
				MagicMissile.FORCE,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( 0x664422 ); particle.am = 0.6f;
		particle.setLifespan(3f);
		particle.speed.polar(Random.Float(PointF.PI2), 0.3f);
		particle.setSize( 1f, 2f);
		particle.radiateXY(2.5f);
	}

	public static class BlastWave extends Image {

		private static final float TIME_TO_FADE = 0.2f;

		private float time;
		private float size;

		public BlastWave(){
			super(Effects.get(Effects.Type.RIPPLE));
			origin.set(width / 2, height / 2);
		}

		public void reset(int pos, float size) {
			revive();

			x = (pos % Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - width) / 2;
			y = (pos / Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - height) / 2;

			time = TIME_TO_FADE;
			this.size = size;
		}

		@Override
		public void update() {
			super.update();

			if ((time -= Game.elapsed) <= 0) {
				kill();
			} else {
				float p = time / TIME_TO_FADE;
				alpha(p);
				scale.y = scale.x = (1-p)*size;
			}
		}

	}
}
