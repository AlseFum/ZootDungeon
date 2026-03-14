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

package com.zootdungeon.windows;

import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.ui.HealthBar;
import com.zootdungeon.ui.IconButton;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ui.Component;

public class WndInfoMob extends WndTitledMessage {

	private static final int GAP = 2;
	private static final int BTN_SIZE = 16;

	public WndInfoMob( Mob mob ) {
		super( new MobTitle( mob ), mob.info() );
		IconButton allBuffsBtn = new IconButton( Icons.get( Icons.BUFFS ) ) {
			@Override
			protected void onClick() {
				super.onClick();
				GameScene.show( new WndAllBuffs( mob ) );
			}
		};
		allBuffsBtn.setRect( width - BTN_SIZE - GAP, GAP, BTN_SIZE, BTN_SIZE );
		add( allBuffsBtn );
		bringToFront( allBuffsBtn );
	}

	private static class MobTitle extends Component {

		private static final int GAP = 2;

		private CharSprite image;
		private RenderedTextBlock name;
		private HealthBar health;

		public MobTitle( Mob mob ) {
			name = PixelScene.renderTextBlock( Messages.titleCase( mob.name() ), 9 );
			name.hardlight( TITLE_COLOR );
			add( name );

			image = mob.sprite();
			add( image );

			health = new HealthBar();
			health.level( mob );
			add( health );
		}

		@Override
		protected void layout() {
			image.x = 0;
			image.y = Math.max( 0, name.height() + health.height() - image.height() );

			float w = width - image.width() - GAP;
			name.maxWidth( (int) w );
			name.setPos( x + image.width() + GAP,
					image.height() > name.height() ? y + (image.height() - name.height()) / 2 : y );

			health.setRect( image.width() + GAP, name.bottom() + GAP, w, health.height() );

			height = Math.max( image.y + image.height(), health.bottom() );
		}
	}
}
