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

package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.Splash;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.MissileSprite;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class RhodesStandardBow extends MeleeWeapon {
    
    public static final String AC_SHOOT = "SHOOT";
    
    {
        image = ItemSpriteSheet.SPIRIT_BOW;
        
        defaultAction = AC_SHOOT;
        usesTargeting = true;
        
        tier = 0;
        DLY = 1.0f;
    }
    
    @Override
    public String name() {
        return "罗德岛标准弓";
    }
    
    @Override
    public String info() {
        String info = "一把标准的弓，需要消耗箭才能进行远程攻击。";
        info += "\n\n如果没有箭，只能进行近战攻击。";
        return info;
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }
    
    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            actions.add(AC_SHOOT);
        }
        return actions;
    }
    
    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SHOOT)) {
            return "射击";
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        
        if (action.equals(AC_SHOOT)) {
            // 检查是否有箭
            Arrow arrow = findArrow(hero);
            if (arrow == null) {
                GLog.w("没有箭！只能进行近战攻击。");
                return;
            }
            
            curUser = hero;
            curItem = this;
            GameScene.selectCell(shooter);
        }
    }
    
    /**
     * 从背包中查找箭
     */
    private Arrow findArrow(Hero hero) {
        for (Item item : hero.belongings.backpack) {
            if (item instanceof Arrow) {
                Arrow arrow = (Arrow) item;
                // 如果没有关联弓，或者关联的是当前弓，都可以使用
                if (arrow.getBow() == null || arrow.getBow() == this) {
                    // 如果箭没有关联弓，设置关联
                    if (arrow.getBow() == null) {
                        arrow.setBow(this);
                    }
                    return arrow;
                }
            }
        }
        return null;
    }
    
    /**
     * 消耗一支箭
     */
    private boolean consumeArrow(Hero hero) {
        Arrow arrow = findArrow(hero);
        if (arrow != null) {
            arrow.quantity(arrow.quantity() - 1);
            if (arrow.quantity() <= 0) {
                arrow.detachAll(hero.belongings.backpack);
            }
            return true;
        }
        return false;
    }
    
    private CellSelector.Listener shooter = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target != null) {
                if (Dungeon.level.adjacent(curUser.pos, target)) {
                    // 相邻目标，进行近战攻击
                    Char enemy = Actor.findChar(target);
                    if (enemy != null && curUser.canAttack(enemy)) {
                        curUser.attack(enemy);
                    }
                } else {
                    // 远程目标，需要箭
                    Arrow arrow = findArrow(curUser);
                    if (arrow == null) {
                        GLog.w("没有箭！无法进行远程攻击。");
                        return;
                    }
                    
                    // 消耗箭并射击
                    if (consumeArrow(curUser)) {
                        // 创建新的箭实例用于射击，并关联当前弓
                        Arrow shootArrow = new Arrow().setBow(RhodesStandardBow.this);
                        shootArrow.cast(curUser, target);
                    }
                }
            }
        }
        
        @Override
        public String prompt() {
            return "选择目标";
        }
    };
    
    /**
     * 箭的内部类
     * 不可投掷的投掷物，会粘附在敌人身上
     */
    public static class Arrow extends MissileWeapon {
        
        // 存储关联的弓的引用
        private RhodesStandardBow bow;
        
        {
            image = ItemSpriteSheet.SPIRIT_ARROW;
            hitSound = Assets.Sounds.HIT_ARROW;
            
            tier = 1;
            stackable = true;
            sticky = true; // 确保粘附功能开启
            defaultAction = null; // 移除默认投掷行动
        }
        
        /**
         * 设置关联的弓
         */
        public Arrow setBow(RhodesStandardBow bow) {
            this.bow = bow;
            return this;
        }
        
        /**
         * 获取所属的弓
         */
        public RhodesStandardBow getBow() {
            return bow;
        }
        
        @Override
        public String name() {
            return "箭";
        }
        
        @Override
        public String info() {
            return "标准的箭矢，用于弓的远程攻击。箭会粘附在敌人身上。";
        }
        
        @Override
        public ArrayList<String> actions(Hero hero) {
            // 移除所有行动，包括投掷，箭只能通过弓来使用
            return new ArrayList<>();
        }
        
        @Override
        public int min(int lvl) {
            return 2 * tier + lvl;
        }
        
        @Override
        public int max(int lvl) {
            return 5 * tier + 2 * lvl;
        }
        
        @Override
        public int damageRoll(Char owner) {
            // 如果有关联的弓，使用弓的伤害计算，否则使用箭自己的伤害
            if (bow != null) {
                return bow.damageRoll(owner);
            }
            return super.damageRoll(owner);
        }
        
        @Override
        protected void onThrow(int cell) {
            Char enemy = Actor.findChar(cell);
            if (enemy == null || enemy == curUser) {
                parent = null;
                Splash.at(cell, 0xCC99FFFF, 1);
            } else {
                if (!curUser.shoot(enemy, this)) {
                    Splash.at(cell, 0xCC99FFFF, 1);
                }
            }
        }
        
        @Override
        public void throwSound() {
            Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(0.87f, 1.15f));
        }
        
        @Override
        public void cast(Hero user, int dst) {
            super.cast(user, dst);
        }
    }
    
    /**
     * 创建一支箭
     */
    public Arrow knockArrow() {
        return new Arrow().setBow(this);
    }
}

