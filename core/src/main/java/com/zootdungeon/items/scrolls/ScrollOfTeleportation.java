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

package com.zootdungeon.items.scrolls;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

public class ScrollOfTeleportation extends Scroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_TELEPORT;
	}

	@Override
	public void doRead() {

		detach(curUser.belongings.backpack);
		Sample.INSTANCE.play( Assets.Sounds.READ );

		if (ScrollEffects.teleportPreferringUnseen( curUser )){
			readAnimation();
		}
		identify();

	}

	/** @deprecated Use {@link ScrollEffects#teleportToLocation} instead. */
	@Deprecated
	public static boolean teleportToLocation(Char ch, int pos){
		return ScrollEffects.teleportToLocation(ch, pos);
	}

	/** @deprecated Use {@link ScrollEffects#teleportChar} instead. */
	@Deprecated
	public static boolean teleportChar( Char ch ) {
		return ScrollEffects.teleportChar( ch );
	}

	/** @deprecated Use {@link ScrollEffects#teleportChar} instead. */
	@Deprecated
	public static boolean teleportChar( Char ch, Class source ) {
		return ScrollEffects.teleportChar( ch, source );
	}

	/** @deprecated Use {@link ScrollEffects#teleportPreferringUnseen} instead. */
	@Deprecated
	public static boolean teleportPreferringUnseen( Hero hero ){
		return ScrollEffects.teleportPreferringUnseen(hero);
	}

	// teleportInNonRegularLevel moved to ScrollEffects

	/** @deprecated Use {@link ScrollEffects#appear} instead. */
	@Deprecated
	public static void appear( Char ch, int pos ) {
		ScrollEffects.appear(ch, pos);
	}

	/** @deprecated Use {@link ScrollEffects#appearVFX} instead. */
	@Deprecated
	public static void appearVFX( Char ch ){
		ScrollEffects.appearVFX(ch);
	}

	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}
}
