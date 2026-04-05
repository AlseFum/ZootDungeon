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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zootdungeon.items.weapon.configurable;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class StateSwitchWeapon extends MeleeWeapon {
    
    {
        image = ItemSpriteSheet.SWORD;
        tier = 3;
        bones = false;
    }
    
    // 武器状态枚举
    public enum WeaponState {
        NORMAL,      // 普通状态
        DEFENSE_MODE // 防御转换状态：将防御骰点数按比例加入攻击
    }
    
    // 当前武器状态
    private WeaponState currentState = WeaponState.NORMAL;
    
    // 防御转换比例（防御骰值的百分比加入攻击）
    private float defenseToAttackRatio = 0.5f; // 50%
    
    public static final String AC_SWITCH = "SWITCH";
    
    @Override
    public String name(){
        return Messages.get(this, currentState == WeaponState.NORMAL ? "name_normal" : "name_defense");
    }

    @Override
    public String desc(){
        int pct = (int) (defenseToAttackRatio * 100);
        return Messages.get(this,
                currentState == WeaponState.NORMAL ? "desc_normal" : "desc_defense",
                pct);
    }
    
    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SWITCH);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SWITCH)) {
            return Messages.get(this, "ac_switch");
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SWITCH)) {
            // 切换状态
            if (currentState == WeaponState.NORMAL) {
                currentState = WeaponState.DEFENSE_MODE;
                GLog.p(Messages.get(this, "msg_switch_defense"));
            } else {
                currentState = WeaponState.NORMAL;
                GLog.p(Messages.get(this, "msg_switch_normal"));
            }
            updateQuickslot();
        } else {
            super.execute(hero, action);
        }
    }
    
    // @Override
    // public int damageRoll(Char owner) {
    //     int baseDamage = super.damageRoll(owner);
        
    //     // 如果是防御模式，需要在实际攻击时根据防御骰值调整
    //     // 这里先返回基础伤害，实际调整在 proc 方法中进行
    //     return baseDamage;
    // }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);
        
        // 如果是防御模式，将防御骰值按比例加入伤害
        if (currentState == WeaponState.DEFENSE_MODE && attacker instanceof Hero) {
            Hero hero = (Hero) attacker;
            
            // 获取防御骰值（含减防 debuff）
            int defenseRoll = defender.defenseSkill(hero);
            
            // 将防御骰值按比例加入伤害
            int bonusDamage = Math.round(defenseRoll * defenseToAttackRatio);
            damage += bonusDamage;
            
            if (bonusDamage > 0) {
                GLog.i(Messages.get(this, "msg_defense_bonus", bonusDamage));
            }
        }
        
        return damage;
    }

    public StateSwitchWeapon randomize() {
        tier = Random.IntRange(1, 5);
        level(Random.IntRange(0, 3));
        defenseToAttackRatio = Random.Float(0.25f, 1.0f);
        currentState = Random.Int(2) == 0 ? WeaponState.NORMAL : WeaponState.DEFENSE_MODE;
        return this;
    }

    @Override
    public Item random() {
        return randomize();
    }
    
    private static final String STATE = "state";
    private static final String DEF_RATIO = "defenseToAttackRatio";
    
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STATE, currentState.name());
        bundle.put(DEF_RATIO, defenseToAttackRatio);
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(STATE)) {
            try {
                currentState = WeaponState.valueOf(bundle.getString(STATE));
            } catch (Exception e) {
                currentState = WeaponState.NORMAL;
            }
        }
        if (bundle.contains(DEF_RATIO)) {
            defenseToAttackRatio = bundle.getFloat(DEF_RATIO);
        }
    }
}
