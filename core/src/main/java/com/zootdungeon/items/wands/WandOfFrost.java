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
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Chill;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Frost;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.levels.rooms.special.MagicalFireRoom;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class WandOfFrost extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_FROST;
	}

	public int min(int lvl){
		return 2+lvl;
	}

	public int max(int lvl){
		return 8+5*lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {
		Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, 1.1f * Random.Float(0.87f, 1.15f));
		ItemEffects.applyFrostZap(bolt, buffedLvl(), this);
	}

	@Override
	public String upgradeStat2(int level) {
		return Integer.toString(2 + level);
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(curUser.sprite.parent,
				MagicMissile.FROST,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		ItemEffects.applyFrostOnHit(defender, buffedLvl(), procChanceMultiplier(attacker));
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x88CCFF);
		particle.am = 0.6f;
		particle.setLifespan(2f);
		float angle = Random.Float(PointF.PI2);
		particle.speed.polar( angle, 2f);
		particle.acc.set( 0f, 1f);
		particle.setSize( 0f, 1.5f);
		particle.radiateXY(Random.Float(1f));
	}

}
