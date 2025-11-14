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

package com.zootdungeon.items.armor.glyphs;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Charm;
import com.zootdungeon.actors.buffs.Degrade;
import com.zootdungeon.actors.buffs.Hex;
import com.zootdungeon.actors.buffs.MagicalSleep;
import com.zootdungeon.actors.buffs.Vulnerable;
import com.zootdungeon.actors.buffs.Weakness;
import com.zootdungeon.actors.hero.spells.Judgement;
import com.zootdungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.zootdungeon.actors.hero.abilities.mage.ElementalBlast;
import com.zootdungeon.actors.hero.abilities.mage.WarpBeacon;
import com.zootdungeon.actors.hero.spells.GuidingLight;
import com.zootdungeon.actors.hero.spells.HolyLance;
import com.zootdungeon.actors.hero.spells.HolyWeapon;
import com.zootdungeon.actors.hero.spells.Smite;
import com.zootdungeon.actors.hero.spells.Sunray;
import com.zootdungeon.actors.mobs.CrystalWisp;
import com.zootdungeon.actors.mobs.DM100;
import com.zootdungeon.actors.mobs.Eye;
import com.zootdungeon.actors.mobs.Shaman;
import com.zootdungeon.actors.mobs.Warlock;
import com.zootdungeon.actors.mobs.YogFist;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.bombs.ArcaneBomb;
import com.zootdungeon.items.bombs.HolyBomb;
import com.zootdungeon.items.scrolls.ScrollOfRetribution;
import com.zootdungeon.items.scrolls.ScrollOfTeleportation;
import com.zootdungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.zootdungeon.items.wands.CursedWand;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.wands.WandOfDisintegration;
import com.zootdungeon.items.wands.WandOfFireblast;
import com.zootdungeon.items.wands.WandOfFrost;
import com.zootdungeon.items.wands.WandOfLightning;
import com.zootdungeon.items.wands.WandOfLivingEarth;
import com.zootdungeon.items.wands.WandOfMagicMissile;
import com.zootdungeon.items.wands.WandOfPrismaticLight;
import com.zootdungeon.items.wands.WandOfTransfusion;
import com.zootdungeon.items.wands.WandOfWarding;
import com.zootdungeon.items.weapon.enchantments.Blazing;
import com.zootdungeon.items.weapon.enchantments.Grim;
import com.zootdungeon.items.weapon.enchantments.Shocking;
import com.zootdungeon.items.weapon.missiles.darts.HolyDart;
import com.zootdungeon.levels.traps.DisintegrationTrap;
import com.zootdungeon.levels.traps.GrimTrap;
import com.zootdungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

import java.util.HashSet;

public class AntiMagic extends Armor.Glyph {

	private static ItemSprite.Glowing TEAL = new ItemSprite.Glowing( 0x88EEFF );
	
	public static final HashSet<Class> RESISTS = new HashSet<>();
	static {
		RESISTS.add( MagicalSleep.class );
		RESISTS.add( Charm.class );
		RESISTS.add( Weakness.class );
		RESISTS.add( Vulnerable.class );
		RESISTS.add( Hex.class );
		RESISTS.add( Degrade.class );
		
		RESISTS.add( DisintegrationTrap.class );
		RESISTS.add( GrimTrap.class );

		RESISTS.add( ArcaneBomb.class );
		RESISTS.add( HolyBomb.HolyDamage.class );
		RESISTS.add( ScrollOfRetribution.class );
		RESISTS.add( ScrollOfPsionicBlast.class );
		RESISTS.add( ScrollOfTeleportation.class );
		RESISTS.add( HolyDart.class );

		RESISTS.add( GuidingLight.class );
		RESISTS.add( HolyWeapon.class );
		RESISTS.add( Sunray.class );
		RESISTS.add( HolyLance.class );
		RESISTS.add( Smite.class );
		RESISTS.add( Judgement.class );

		RESISTS.add( ElementalBlast.class );
		RESISTS.add( CursedWand.class );
		RESISTS.add( WandOfBlastWave.class );
		RESISTS.add( WandOfDisintegration.class );
		RESISTS.add( WandOfFireblast.class );
		RESISTS.add( WandOfFrost.class );
		RESISTS.add( WandOfLightning.class );
		RESISTS.add( WandOfLivingEarth.class );
		RESISTS.add( WandOfMagicMissile.class );
		RESISTS.add( WandOfPrismaticLight.class );
		RESISTS.add( WandOfTransfusion.class );
		RESISTS.add( WandOfWarding.Ward.class );

		RESISTS.add( ElementalStrike.class );
		RESISTS.add( Blazing.class );
		RESISTS.add( WandOfFireblast.FireBlastOnHit.class );
		RESISTS.add( Shocking.class );
		RESISTS.add( WandOfLightning.LightningOnHit.class );
		RESISTS.add( Grim.class );

		RESISTS.add( WarpBeacon.class );
		
		RESISTS.add( DM100.LightningBolt.class );
		RESISTS.add( Shaman.EarthenBolt.class );
		RESISTS.add( CrystalWisp.LightBeam.class );
		RESISTS.add( Warlock.DarkBolt.class );
		RESISTS.add( Eye.DeathGaze.class );
		RESISTS.add( YogFist.BrightFist.LightBeam.class );
		RESISTS.add( YogFist.DarkFist.DarkBolt.class );
	}
	
	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {
		//no proc effect, triggers in Char.damage
		return damage;
	}
	
	public static int drRoll( Char owner, int level ){
		if (level == -1){
			return 0;
		} else {
			return Random.NormalIntRange(
					Math.round(level * genericProcChanceMultiplier(owner)),
					Math.round((3 + (level * 1.5f)) * genericProcChanceMultiplier(owner)));
		}
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return TEAL;
	}

}