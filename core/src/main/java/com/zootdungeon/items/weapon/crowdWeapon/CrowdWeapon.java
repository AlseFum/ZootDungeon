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
package com.zootdungeon.items.weapon.crowdWeapon;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;


import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.utils.GLog;

public class CrowdWeapon extends MeleeWeapon {

    {
        image = ItemSpriteSheet.WAR_SCYTHE;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 0.9f;

        tier = 5;
        ACC = 0.8f; //20% penalty to accuracy
    }

    @Override
    public int max(int lvl) {
        return  Math.round(6.67f*(tier+1)) +    //40 base, up from 30
                lvl*(tier+1);                   //scaling unchanged
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        //replaces damage with 30+4.5*lvl bleed, roughly 133% avg base dmg, 129% avg scaling
        int bleedAmt = augment.damageFactor(Math.round(30f + 4.5f*buffedLvl()));
        harvestAbility(hero, target, 0f, bleedAmt, this);
    }

    @Override
    public String abilityInfo() {
        int bleedAmt = levelKnown ? Math.round(30f + 4.5f*buffedLvl()) : 30;
        if (levelKnown){
            return Messages.get(this, "ability_desc", augment.damageFactor(bleedAmt));
        } else {
            return Messages.get(this, "typical_ability_desc", bleedAmt);
        }
    }

    @Override
    public String upgradeAbilityStat(int level) {
        return Integer.toString(augment.damageFactor(Math.round(30f + 4.5f*level)));
    }

    public static void harvestAbility(Hero hero, Integer target, float bleedMulti, int bleedBoost, MeleeWeapon wep){

        if (target == null) {
            return;
        }

        Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(wep, "ability_no_target"));
            return;
        }

        hero.belongings.abilityWeapon = wep;
        if (!hero.canAttack(enemy)){
            GLog.w(Messages.get(wep, "ability_target_range"));
            hero.belongings.abilityWeapon = null;
            return;
        }
        hero.belongings.abilityWeapon = null;

        hero.sprite.attack(enemy.pos, new Callback() {
            @Override
            public void call() {
                wep.beforeAbilityUsed(hero, enemy);
                AttackIndicator.target(enemy);

                Buff.affect(enemy, HarvestBleedTracker.class, 0);
                if (hero.attack(enemy, bleedMulti, bleedBoost, Char.INFINITE_ACCURACY)){
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
                }

                Invisibility.dispel();
                hero.spendAndNext(hero.attackDelay());
                if (!enemy.isAlive()){
                    wep.onAbilityKill(hero, enemy);
                }
                wep.afterAbilityUsed(hero);
            }
        });

    }

    public static class HarvestBleedTracker extends FlavourBuff {};

}
