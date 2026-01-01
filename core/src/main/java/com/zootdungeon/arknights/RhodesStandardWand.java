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

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class RhodesStandardWand extends Wand {
    
    private static final String MODE = "mode";
    private static final String STORED_CHARGE = "storedCharge";
    private static final String AC_SWITCH_MODE = "SWITCH_MODE";
    
    // 攻击模式：1=普通魔弹，2=AOE，3=链式，4=蓄力
    private int mode = 1;
    
    // 模式4存储的充能
    private int storedCharge = 0;
    
    static {
        SpriteRegistry.registerItemTexture("cola/rhodes_wand.png", 32)
                .label("rhodes_standard_wand");
    }
    
    {
        image = SpriteRegistry.itemByName("rhodes_standard_wand");
        collisionProperties = Ballistica.MAGIC_BOLT;
    }
    
    @Override
    public String name() {
        return "罗德岛标准法杖";
    }
    
    @Override
    public String desc() {
        String desc = "一把可以切换攻击模式的标准法杖。";
        desc += "\n\n当前模式: " + getModeName();
        if (storedCharge > 0) {
            desc += "\n存储充能: " + storedCharge;
        }
        desc += "\n\n模式1 - 普通魔弹: 标准的魔法弹攻击，无特殊效果。";
        desc += "\n\n模式2 - 范围攻击: 对选中位置周围四格/八格造成伤害。蓄力后使用八格范围。";
        desc += "\n\n模式3 - 链式攻击: 攻击击中目标后会跳向下一个目标，总跳跃次数依等级而定。蓄力后增加跳跃次数。";
        desc += "\n\n模式4 - 蓄力模式: 不攻击，将当前充能储存起来，直到下一次攻击。下次攻击的伤害/范围/跳跃次数按储存数增加。";
        return desc;
    }
    
    private String getModeName() {
        switch (mode) {
            case 1: return "普通魔弹";
            case 2: return "范围攻击";
            case 3: return "链式攻击";
            case 4: return "蓄力模式";
            default: return "未知";
        }
    }
    
    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SWITCH_MODE);
        return actions;
    }
    
    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SWITCH_MODE)) {
            return "切换模式 (" + getModeName() + ")";
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        
        if (action.equals(AC_SWITCH_MODE)) {
            mode = (mode % 4) + 1; // 在1-4之间循环
            GLog.p("切换到模式: " + getModeName());
            updateQuickslot();
        }
    }
    
    @Override
    public void onZap(Ballistica bolt) {
        if (mode == 4) {
            // 模式4：蓄力（不攻击，只充能）
            // 充能已经在zap时被消耗了，现在将其存储起来
            storedCharge += chargesPerCast();
            GLog.p("充能已存储! 当前存储: " + storedCharge);
            updateQuickslot();
            return;
        }
        
        int multiplier = 1 + storedCharge; // 蓄力加成
        
        switch (mode) {
            case 1:
                // 模式1：普通魔弹
                doNormalAttack(bolt, multiplier);
                break;
            case 2:
                // 模式2：AOE攻击
                doAOEAttack(bolt, multiplier);
                break;
            case 3:
                // 模式3：链式攻击
                doChainAttack(bolt, multiplier);
                break;
        }
        
        // 释放存储的充能
        storedCharge = 0;
        updateQuickslot();
    }
    
    private void doNormalAttack(Ballistica bolt, int multiplier) {
        int targetPos = bolt.collisionPos;
        Char target = Actor.findChar(targetPos);
        
        if (target != null && target.alignment == Char.Alignment.ENEMY) {
            wandProc(target, chargesPerCast() * multiplier);
            int damage = damageRoll() * multiplier;
            target.damage(damage, this);
            Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f));
            target.sprite.burst(0xFFFFFFFF, buffedLvl() / 2 + 2);
        }
    }
    
    private int damageRoll() {
        // 基础伤害：2 + 等级 到 8 + 2*等级
        int min = 2 + buffedLvl();
        int max = 8 + 2 * buffedLvl();
        return Random.NormalIntRange(min, max);
    }
    
    private void doAOEAttack(Ballistica bolt, int multiplier) {
        int targetPos = bolt.collisionPos;
        boolean useEight = (multiplier >= 2); // 如果有多倍充能，使用8格，否则4格
        
        int[] area = useEight ? PathFinder.NEIGHBOURS8 : PathFinder.NEIGHBOURS4;
        HashSet<Integer> affected = new HashSet<>();
        affected.add(targetPos); // 目标位置本身
        
        // 收集所有受影响的格子（包括不可通行的格子）
        for (int offset : area) {
            int pos = targetPos + offset;
            if (pos >= 0 && pos < Dungeon.level.length()) {
                affected.add(pos);
            }
        }
        
        // 对所有受影响的格子造成伤害
        for (int pos : affected) {
            Char ch = Actor.findChar(pos);
            if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
                wandProc(ch, chargesPerCast() * multiplier);
                int damage = damageRoll() * multiplier;
                ch.damage(damage, this);
                Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f));
                ch.sprite.burst(0xFFFFFFFF, buffedLvl() / 2 + 2);
            }
            // 特效
            CellEmitter.get(pos).burst(Speck.factory(Speck.EVOKE), 3);
        }
        
        Sample.INSTANCE.play(Assets.Sounds.BLAST);
    }
    
    private void doChainAttack(Ballistica bolt, int multiplier) {
        int maxJumps = buffedLvl() / 2 + 1; // 基础跳跃次数，根据等级计算
        maxJumps = Math.max(1, maxJumps); // 至少1次
        maxJumps += storedCharge; // 蓄力加成增加跳跃次数
        
        ArrayList<Char> targets = new ArrayList<>();
        Char firstTarget = Actor.findChar(bolt.collisionPos);
        
        // 找到第一个目标
        if (firstTarget != null && firstTarget.alignment == Char.Alignment.ENEMY) {
            targets.add(firstTarget);
            
            // 找到后续链式目标
            int currentPos = firstTarget.pos;
            for (int i = 1; i < maxJumps; i++) {
                Char nextTarget = findNextChainTarget(currentPos, targets);
                if (nextTarget == null) {
                    break; // 没有更多目标
                }
                targets.add(nextTarget);
                currentPos = nextTarget.pos;
            }
            
            // 对所有目标造成伤害
            for (Char target : targets) {
                wandProc(target, chargesPerCast() * multiplier);
                int damage = damageRoll() * multiplier;
                target.damage(damage, this);
                Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f));
                target.sprite.burst(0xFFFFFFFF, buffedLvl() / 2 + 2);
                
                // 链式特效 - 显示连接线
                if (target != firstTarget) {
                    CellEmitter.get(target.pos).burst(Speck.factory(Speck.STAR), 3);
                }
            }
        }
    }
    
    private Char findNextChainTarget(int fromPos, ArrayList<Char> hit) {
        Char closest = null;
        int closestDist = Integer.MAX_VALUE;
        
        // 先尝试8格范围内的目标
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY 
                    && mob.isAlive() 
                    && !hit.contains(mob)
                    && mob.pos != fromPos) {
                int dist = Dungeon.level.distance(fromPos, mob.pos);
                if (dist > 0 && dist <= 8 && dist < closestDist) {
                    closest = mob;
                    closestDist = dist;
                }
            }
        }
        
        return closest;
    }
    
    
    @Override
    public void fx(Ballistica bolt, Callback callback) {
        if (mode == 4) {
            // 模式4不需要攻击特效，显示充能特效
            CellEmitter.get(curUser.pos).burst(Speck.factory(Speck.STAR), 6);
            Sample.INSTANCE.play(Assets.Sounds.MELD);
            callback.call();
            return;
        }
        
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
    
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(MODE, mode);
        bundle.put(STORED_CHARGE, storedCharge);
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(MODE)) {
            mode = bundle.getInt(MODE);
        } else {
            mode = 1;
        }
        if (bundle.contains(STORED_CHARGE)) {
            storedCharge = bundle.getInt(STORED_CHARGE);
        } else {
            storedCharge = 0;
        }
    }

}
