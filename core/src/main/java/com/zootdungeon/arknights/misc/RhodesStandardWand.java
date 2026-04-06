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

package com.zootdungeon.arknights.misc;

import com.watabou.noosa.audio.Sample;
import com.zootdungeon.Assets;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class RhodesStandardWand extends Wand {
    
    static {
        SpriteRegistry.texture("sheet.cola.rhodes_wand", "cola/rhodes_wand.png")
                .grid(32, 32)
                .label("rhodes_standard_wand");
    }
    
    {
        image = SpriteRegistry.itemByName("rhodes_standard_wand");
        collisionProperties = Ballistica.MAGIC_BOLT;
    }
    
    public int min() {
        return min(buffedLvl());
    }
    
    public int min(int lvl) {
        // 基础伤害：2 + 等级
        return 2 + lvl;
    }
    
    public int max() {
        return max(buffedLvl());
    }
    
    public int max(int lvl) {
        // 基础伤害：8 + 2*等级
        return 8 + 2 * lvl;
    }
    
    @Override
    public void onZap(Ballistica bolt) {
        int targetPos = bolt.collisionPos;
        Char target = Actor.findChar(targetPos);
        
        if (target != null && target.alignment == Char.Alignment.ENEMY) {
            wandProc(target, chargesPerCast());
            int damage = damageRoll();
            target.damage(damage, this);
            Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f));
            target.sprite.burst(0xFFFFFFFF, buffedLvl() / 2 + 2);
        }
    }
    
    private int damageRoll() {
        // 基础伤害：2 + 等级 到 8 + 2*等级
        return Random.NormalIntRange(min(), max());
    }
    
    @Override
    public String statsDesc() {
        if (levelKnown)
            return Messages.get(this, "stats_desc", min(), max());
        else
            return Messages.get(this, "stats_desc", min(0), max(0));
    }
    
    @Override
    public void fx(Ballistica bolt, Callback callback) {
        MagicMissile.boltFromChar(
                curUser.sprite.parent,
                MagicMissile.MAGIC_MISSILE,
                curUser.sprite,
                bolt.collisionPos,
                callback);
        Sample.INSTANCE.play(Assets.Sounds.ZAP);
    }
    
    @Override
    public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
        if (defender.alignment == Char.Alignment.ENEMY && defender.isAlive()) {
            wandProc(defender, chargesPerCast());
        }
    }
}
