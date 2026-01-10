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
package com.zootdungeon.items.weapon.ambushWeapon;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
public class AmbushWeapon extends MeleeWeapon{

    {
        image = ItemSpriteSheet.DAGGER;
        hitSound = Assets.Sounds.HIT_STAB;
        hitSoundPitch = 1.1f;

        tier = 1;

        bones = false;
    }

    @Override
    public int max(int lvl) {
        return  4*(tier+1) +    //8 base, down from 10
                lvl*(tier+1);   //scaling unchanged
    }
    public float ambushRate=0.5f;
    @Override
    public int damageRoll(Char owner) {
        if (owner instanceof Hero) {
            Hero hero = (Hero)owner;
            Char enemy = hero.enemy();
            if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
                // surprise hit: 偏向高端的伤害
                int lvl = buffedLvl();
                int mn = min(lvl);
                int mx = max(lvl);
                int diff = mx - mn;

                // 基础部分：将 [mn, mx] 区间压缩到 [mn+ambushRate*diff, mx]
                int biasedMin = mn + Math.round(diff * ambushRate);
                if (biasedMin > mx) biasedMin = mx;

                int damage = augment.damageFactor(com.watabou.utils.Random.NormalIntRange(biasedMin, mx));
                damage = hero.heroDamageIntRange(damage, STRReq());
                return damage;
            }
        }
        return super.damageRoll(owner);
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    public boolean useTargeting(){
        return false;
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        sneakAbility(hero, target, 5, 2+buffedLvl(), this);
    }

    @Override
    public String abilityInfo() {
        if (levelKnown){
            return Messages.get(this, "ability_desc", 2+buffedLvl());
        } else {
            return Messages.get(this, "typical_ability_desc", 2);
        }
    }

    @Override
    public String upgradeAbilityStat(int level) {
        return Integer.toString(2+level);
    }

    public static void sneakAbility(Hero hero, Integer target, int maxDist, int invisTurns, MeleeWeapon wep){
        if (target == null) {
            return;
        }

        PathFinder.buildDistanceMap(Dungeon.hero.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null), maxDist);
        if (PathFinder.distance[target] == Integer.MAX_VALUE || !Dungeon.level.heroFOV[target] || hero.rooted) {
            GLog.w(Messages.get(wep, "ability_target_range"));
            if (Dungeon.hero.rooted) PixelScene.shake( 1, 1f );
            return;
        }

        if (Actor.findChar(target) != null) {
            GLog.w(Messages.get(wep, "ability_occupied"));
            return;
        }

        wep.beforeAbilityUsed(hero, null);
        Buff.prolong(hero, Invisibility.class, invisTurns-1); //1 fewer turns as ability is instant

        Dungeon.hero.sprite.turnTo( Dungeon.hero.pos, target);
        Dungeon.hero.pos = target;
        Dungeon.level.occupyCell(Dungeon.hero);
        Dungeon.observe();
        GameScene.updateFog();
        Dungeon.hero.checkVisibleMobs();

        Dungeon.hero.sprite.place( Dungeon.hero.pos );
        CellEmitter.get( Dungeon.hero.pos ).burst( Speck.factory( Speck.WOOL ), 6 );
        Sample.INSTANCE.play( Assets.Sounds.PUFF );

        hero.next();
        wep.afterAbilityUsed(hero);
    }
}

