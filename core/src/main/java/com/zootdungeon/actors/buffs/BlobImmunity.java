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

package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.blobs.Blizzard;
import com.zootdungeon.actors.blobs.ConfusionGas;
import com.zootdungeon.actors.blobs.CorrosiveGas;
import com.zootdungeon.actors.blobs.Electricity;
import com.zootdungeon.actors.blobs.Fire;
import com.zootdungeon.actors.blobs.Freezing;
import com.zootdungeon.actors.blobs.Inferno;
import com.zootdungeon.actors.blobs.ParalyticGas;
import com.zootdungeon.actors.blobs.Regrowth;
import com.zootdungeon.actors.blobs.SmokeScreen;
import com.zootdungeon.actors.blobs.StenchGas;
import com.zootdungeon.actors.blobs.StormCloud;
import com.zootdungeon.actors.blobs.ToxicGas;
import com.zootdungeon.actors.blobs.Web;
import com.zootdungeon.actors.mobs.Tengu;
import com.zootdungeon.levels.rooms.special.MagicalFireRoom;
import com.zootdungeon.ui.BuffIndicator;

public class BlobImmunity extends FlavourBuff {
	
	{
		type = buffType.POSITIVE;
	}
	
	public static final float DURATION	= 20f;
	
	@Override
	public int icon() {
		return BuffIndicator.IMMUNITY;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	{
		//all harmful blobs
		immunities.add( Blizzard.class );
		immunities.add( ConfusionGas.class );
		immunities.add( CorrosiveGas.class );
		immunities.add( Electricity.class );
		immunities.add( Fire.class );
		immunities.add( MagicalFireRoom.EternalFire.class );
		immunities.add( Freezing.class );
		immunities.add( Inferno.class );
		immunities.add( ParalyticGas.class );
		immunities.add( Regrowth.class );
		immunities.add( SmokeScreen.class );
		immunities.add( StenchGas.class );
		immunities.add( StormCloud.class );
		immunities.add( ToxicGas.class );
		immunities.add( Web.class );

		immunities.add(Tengu.FireAbility.FireBlob.class);
	}

}
