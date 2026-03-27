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

package com.zootdungeon.arknights.tragodia;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.levels.Level;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfPrisonCage extends Wand {
    
    private static final String CHARGE = "charge";
    private static final String CHARGE_CAP = "chargeCap";
    private static final String AC_RELEASE_ALL = "RELEASE_ALL";
    
    private int charge = 0;
    private int chargeCap = 10;
    
    {
        image = ItemSpriteSheet.WAND_MAGIC_MISSILE;
        
        collisionProperties = Ballistica.MAGIC_BOLT;
    }
    
    @Override
    public String name() {
        return "囚笼法杖";
    }
    
    @Override
    public String desc() {
        String desc = "一把特殊的法杖，可以充能。";
        if (charge > 0) {
            desc += "\n\n当前充能: " + charge + "/" + chargeCap;
        }
        desc += "\n\n攻击敌人时会在敌人附近生成一个囚笼，囚笼会不断将敌人拉向自己。";
        desc += "\n\n囚笼的持续时间与法杖的等级(tier)和强化等级(level)有关。";
        desc += "\n\n可以消耗所有充能，一次对场上所有敌人释放此效果。";
        return desc;
    }
    
    @Override
    public ArrayList<String> actions(com.zootdungeon.actors.hero.Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        // 确保AC_RELEASE_ALL在AC_ZAP之后添加
        actions.add(AC_RELEASE_ALL);
        return actions;
    }
    
    @Override
    public String actionName(String action, com.zootdungeon.actors.hero.Hero hero) {
        if (action.equals(AC_RELEASE_ALL)) {
            // 确保读取的是当前实例的charge值
            int currentCharge = this.charge;
            return "释放所有囚笼 (" + currentCharge + "充能)";
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public void execute(com.zootdungeon.actors.hero.Hero hero, String action) {
        super.execute(hero, action);
        
        if (action.equals(AC_RELEASE_ALL)) {
            if (charge > 0) {
                releaseAllCages(hero);
            } else {
                GLog.w("没有充能可以使用！");
            }
        }
    }
    
    private void releaseAllCages(com.zootdungeon.actors.hero.Hero hero) {
        ArrayList<Mob> enemies = new ArrayList<>();
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()) {
                enemies.add(mob);
            }
        }
        
        if (enemies.isEmpty()) {
            GLog.w("没有敌人可以释放囚笼！");
            return;
        }
        
        int usedCharge = charge;
        charge = 0;
        updateQuickslot();
        
        GLog.p("消耗了 " + usedCharge + " 点充能，对所有敌人释放囚笼！");
        
        for (Mob enemy : enemies) {
            createPrisonCage(enemy.pos, hero);
        }
        
        hero.spendAndNext(1f);
    }
    
    @Override
    public void onZap(Ballistica bolt) {
        int targetPos = bolt.collisionPos;
        Char target = Actor.findChar(targetPos);
        
        if (target != null && target.alignment == Char.Alignment.ENEMY) {
            createPrisonCage(targetPos, curUser);
            wandProc(target, chargesPerCast());
        }
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
            // 在敌人附近创建囚笼
            createPrisonCage(defender.pos, (com.zootdungeon.actors.hero.Hero) attacker);
            
            // 充能
            if (charge < chargeCap) {
                charge++;
                updateQuickslot();
                GLog.p("充能增加! 当前充能: " + charge + "/" + chargeCap);
            }
        }
    }
    
    private void createPrisonCage(int targetPos, com.zootdungeon.actors.hero.Hero hero) {
        // 找到敌人附近的空位置
        int cagePos = findNearbyEmptyCell(targetPos);
        if (cagePos == -1) {
            // 如果没有空位置，就放在敌人位置
            cagePos = targetPos;
        }
        
        // 计算持续时间：基础回合数 = tier * 3 + level * 2
        int duration = tier() * 3 + buffedLvl() * 2+10;
        if (duration < 5) duration = 5; // 最少5回合
        
        // 创建囚笼
        PrisonCage cage = Blob.seed(cagePos, duration, PrisonCage.class);
        cage.setTargetPos(targetPos); // 设置目标位置（敌人位置）
        GameScene.add(cage);
        
        // 特效
        CellEmitter.get(cagePos).burst(Speck.factory(Speck.STAR), 8);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
    }
    
    private int findNearbyEmptyCell(int centerPos) {
        // 先检查8个相邻位置
        for (int offset : PathFinder.NEIGHBOURS8) {
            int pos = centerPos + offset;
            if (pos >= 0 && pos < Dungeon.level.length() 
                    && Dungeon.level.passable[pos] 
                    && Actor.findChar(pos) == null) {
                return pos;
            }
        }
        
        // 如果相邻位置没有，检查更远的位置（距离2）
        for (int offset : PathFinder.NEIGHBOURS8) {
            int pos = centerPos + offset * 2;
            if (pos >= 0 && pos < Dungeon.level.length() 
                    && Dungeon.level.passable[pos] 
                    && Actor.findChar(pos) == null) {
                return pos;
            }
        }
        
        return -1; // 没有找到空位置
    }
    
    public int tier() {
        // 法杖的tier，基础值为1，每3级提升1个tier
        // 这样可以让高等级法杖的囚笼持续时间更长
        return 1 + (buffedLvl() / 3);
    }
    
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CHARGE, charge);
        bundle.put(CHARGE_CAP, chargeCap);
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(CHARGE)) {
            charge = bundle.getInt(CHARGE);
        } else {
            charge = 0;
        }
        if (bundle.contains(CHARGE_CAP)) {
            chargeCap = bundle.getInt(CHARGE_CAP);
        } else {
            chargeCap = 10; // 默认值
        }
    }
    
    // 囚笼Blob类
    public static class PrisonCage extends Blob {
        
        private int targetPos = -1; // 目标位置（敌人位置）
        
        {
            actPriority = BLOB_PRIO - 1; // 在普通Blob之前执行
        }
        
        @Override
        protected void evolve() {
            int cell;
            Level l = Dungeon.level;
            
            for (int i = area.left; i < area.right; i++) {
                for (int j = area.top; j < area.bottom; j++) {
                    cell = i + j * l.width();
                    if (cur[cell] > 0) {
                        off[cell] = cur[cell] - 1;
                        volume += off[cell];
                        
                        // 如果囚笼还存在，尝试拉取敌人
                        if (off[cell] > 0 && targetPos >= 0) {
                            pullEnemyTowards(cell);
                        }
                    } else {
                        off[cell] = 0;
                    }
                }
            }
        }
        
        private void pullEnemyTowards(int cagePos) {
            // 寻找附近的敌人
            Char target = null;
            int closestDist = Integer.MAX_VALUE;
            
            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()) {
                    int dist = Dungeon.level.distance(mob.pos, cagePos);
                    // 只拉取距离囚笼3格以内的敌人
                    if (dist <= 3 && dist < closestDist) {
                        target = mob;
                        closestDist = dist;
                    }
                }
            }
            
            if (target != null && closestDist > 0) {
                // 计算拉取方向
                int dx = (cagePos % Dungeon.level.width()) - (target.pos % Dungeon.level.width());
                int dy = (cagePos / Dungeon.level.width()) - (target.pos / Dungeon.level.width());
                
                // 尝试移动到更靠近囚笼的位置
                int newPos = -1;
                
                // 优先选择直接朝向囚笼的方向
                if (Math.abs(dx) > Math.abs(dy)) {
                    // 水平方向优先
                    if (dx > 0) {
                        newPos = target.pos + 1; // 向右
                    } else {
                        newPos = target.pos - 1; // 向左
                    }
                } else {
                    // 垂直方向优先
                    if (dy > 0) {
                        newPos = target.pos + Dungeon.level.width(); // 向下
                    } else {
                        newPos = target.pos - Dungeon.level.width(); // 向上
                    }
                }
                
                // 检查新位置是否有效
                if (newPos >= 0 && newPos < Dungeon.level.length() 
                        && Dungeon.level.passable[newPos] 
                        && Actor.findChar(newPos) == null) {
                    // 移动敌人
                    target.pos = newPos;
                    target.sprite.place(newPos);
                    Dungeon.level.occupyCell(target);
                    
                    // 特效
                    CellEmitter.get(newPos).burst(Speck.factory(Speck.STAR), 3);
                } else {
                    // 如果直接方向不可行，尝试对角线方向
                    for (int offset : PathFinder.NEIGHBOURS8) {
                        newPos = target.pos + offset;
                        if (newPos >= 0 && newPos < Dungeon.level.length() 
                                && Dungeon.level.passable[newPos] 
                                && Actor.findChar(newPos) == null) {
                            // 检查是否更靠近囚笼
                            int newDist = Dungeon.level.distance(newPos, cagePos);
                            if (newDist < closestDist) {
                                target.pos = newPos;
                                target.sprite.place(newPos);
                                Dungeon.level.occupyCell(target);
                                CellEmitter.get(newPos).burst(Speck.factory(Speck.STAR), 3);
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        public void setTargetPos(int pos) {
            targetPos = pos;
        }
        
        @Override
        public void use(com.zootdungeon.effects.BlobEmitter emitter) {
            super.use(emitter);
            emitter.pour(ShadowParticle.UP, 0.1f);
        }
        
        @Override
        public String tileDesc() {
            return Messages.get(this, "desc");
        }
        
        private static final String TARGET_POS = "targetPos";
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(TARGET_POS, targetPos);
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            targetPos = bundle.getInt(TARGET_POS);
        }
    }
}

