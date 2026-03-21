package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.Statistics;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.RegularLevel;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.builders.Builder;
import com.zootdungeon.levels.builders.FigureEightBuilder;
import com.zootdungeon.levels.builders.LoopBuilder;
import com.zootdungeon.levels.painters.Painter;
import com.zootdungeon.levels.painters.RegularPainter;
import com.zootdungeon.levels.rooms.Room;
import com.zootdungeon.levels.rooms.secret.SecretRoom;
import com.zootdungeon.levels.rooms.special.PitRoom;
import com.zootdungeon.levels.rooms.special.ShopRoom;
import com.zootdungeon.levels.rooms.special.SpecialRoom;
import com.zootdungeon.levels.rooms.standard.StandardRoom;
import com.zootdungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.zootdungeon.levels.rooms.standard.exit.ExitRoom;
import com.zootdungeon.levels.themes.Theme;
import com.zootdungeon.levels.traps.BurningTrap;
import com.zootdungeon.levels.traps.ChillingTrap;
import com.zootdungeon.levels.traps.ShockingTrap;
import com.zootdungeon.levels.traps.WornDartTrap;
import com.zootdungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 与普通 HourOfAnAwakening 层相同的 Regular 生成，额外固定一间 {@link SkullShattererChamberRoom}（碎骨 + 共享警觉小怪；碎骨或任一小怪目击英雄后全队小怪拉响）。
 */
public class HourOfAnAwakeningBossLevel extends RegularLevel {

	private static final String SHATTER_SQUAD = "shatter_squad_hero";

	/** 碎骨或任一小怪真实目击英雄后置 true；此后所有 {@link ShattererSquadMob} AI 均视为看见英雄 */
	private boolean shattererSquadHeroLinked;

	{
		color1 = 0x4a5568;
		color2 = 0x718096;
		viewDistance = 10;
	}

	public void onShattererSquadSpottedHero() {
		shattererSquadHeroLinked = true;
	}

	public boolean areShattererSquadLinkedOnHero() {
		return shattererSquadHeroLinked;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SHATTER_SQUAD, shattererSquadHeroLinked );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		shattererSquadHeroLinked = bundle.getBoolean( SHATTER_SQUAD );
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();
		initRooms.add( roomEntrance = EntranceRoom.createEntrance() );
		initRooms.add( roomExit = ExitRoom.createExit() );

		int standards = standardRooms( feeling == Feeling.LARGE );
		if ( feeling == Feeling.LARGE ) {
			standards = (int) Math.ceil( standards * 1.5f );
		}
		for ( int i = 0; i < standards; i++ ) {
			StandardRoom s;
			do {
				s = StandardRoom.createRoom();
			} while ( !s.setSizeCat( standards - i ) );
			i += s.sizeFactor() - 1;
			initRooms.add( s );
		}

		if ( Theme.shopOnLevel() ) {
			initRooms.add( new ShopRoom() );
		}

		int specials = specialRooms( feeling == Feeling.LARGE );
		if ( feeling == Feeling.LARGE ) {
			specials++;
		}
		SpecialRoom.initForFloor();
		initRooms.add( new SkullShattererChamberRoom() );
		for ( int i = 1; i < specials; i++ ) {
			SpecialRoom s = SpecialRoom.createRoom();
			if ( s instanceof PitRoom ) {
				specials++;
			}
			initRooms.add( s );
		}

		int secrets = SecretRoom.secretsForFloor( Dungeon.depth );
		if ( feeling == Feeling.SECRETS ) {
			secrets++;
		}
		for ( int i = 0; i < secrets; i++ ) {
			initRooms.add( SecretRoom.createRoom() );
		}

		return initRooms;
	}

	@Override
	protected int standardRooms( boolean forceMax ) {
		if ( forceMax ) {
			return 6;
		}
		return 4 + Random.chances( new float[]{1, 3, 1} );
	}

	@Override
	protected int specialRooms( boolean forceMax ) {
		if ( forceMax ) {
			return 2;
		}
		return 1 + Random.chances( new float[]{1, 4} );
	}

	@Override
	protected Builder builder() {
		if ( Random.Int( 2 ) == 0 ) {
			return new LoopBuilder()
					.setLoopShape( 2,
							Random.Float( 0f, 0.65f ),
							Random.Float( 0f, 0.50f ) );
		} else {
			return new FigureEightBuilder()
					.setLoopShape( 2,
							Random.Float( 0.3f, 0.8f ),
							0f );
		}
	}

	@Override
	protected Painter painter() {
		return new RegularPainter() {
			@Override
			protected void decorate( Level level, ArrayList<Room> rooms ) {
			}
		}
				.setWater( feeling == Feeling.WATER ? 0.85f : 0.25f, 4 )
				.setGrass( feeling == Feeling.GRASS ? 0.75f : 0.18f, 3 )
				.setTraps( nTraps(), trapClasses(), trapChances() );
	}

	@Override
	protected boolean build() {
		if ( !super.build() ) {
			return false;
		}
		Level.set( exit(), Terrain.LOCKED_EXIT, this );
		return true;
	}

	@Override
	public String tilesTex() {
		return com.zootdungeon.sprites.SpriteRegistry.tilemapTilesTextureOr(
				Assets.Environment.TILES_PRISON,
				tilemapKey
		);
	}

	@Override
	public String waterTex() {
		return com.zootdungeon.sprites.SpriteRegistry.tilemapTilesTextureOr(
				Assets.Environment.WATER_PRISON,
				tilemapKey
		);
	}

	@Override
	protected Class<?>[] trapClasses() {
		return new Class<?>[]{
				WornDartTrap.class, ChillingTrap.class, BurningTrap.class, ShockingTrap.class
		};
	}

	@Override
	protected float[] trapChances() {
		return new float[]{4, 2, 2, 2};
	}

	@Override
	public void playLevelMusic() {
		if ( locked ) {
			Music.INSTANCE.play( Assets.Music.PRISON_BOSS, true );
		} else {
			Music.INSTANCE.playTracks( HourOfAnAwakeningLevel.TRACK_LIST, HourOfAnAwakeningLevel.TRACK_CHANCES, false );
		}
	}

	@Override
	public void seal() {
		if ( !locked ) {
			super.seal();
			Statistics.qualifiedForBossChallengeBadge = true;
			set( entrance(), Terrain.WATER );
			GameScene.updateMap( entrance() );
			GameScene.ripple( entrance() );
			Game.runOnRenderThread( new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play( Assets.Music.PRISON_BOSS, true );
				}
			} );
		}
	}

	@Override
	public void unseal() {
		if ( locked ) {
			super.unseal();
			set( entrance(), Terrain.ENTRANCE );
			GameScene.updateMap( entrance() );
			Game.runOnRenderThread( new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut( 5f, new Callback() {
						@Override
						public void call() {
							Music.INSTANCE.end();
						}
					} );
				}
			} );
		}
	}
}
