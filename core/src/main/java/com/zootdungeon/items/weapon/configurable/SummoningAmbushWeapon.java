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

package com.zootdungeon.items.weapon.configurable;

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
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.GhostSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Random;

public class SummoningAmbushWeapon extends AmbushWeapon {

    private static final int PHANTOM_KNIFE_IMAGE;

    static {
        SpriteRegistry.texture("sheet.cola.phantom_knife", "cola/phantom_knife.png")
                .grid(64, 64)
                .label("phantom_knife");
        PHANTOM_KNIFE_IMAGE = SpriteRegistry.byLabel("phantom_knife");
    }
    
    private static final String CHARGE = "charge";
    private static final String CHARGE_CAP = "chargeCap";
    
    private int charge = 0;
    private int chargeCap = 10;
    
    {
        image = PHANTOM_KNIFE_IMAGE;
        tier = 1;
        bones = false;
        ambushRate = 0.5f;
    }
    
    @Override
    public String name() {
        return Messages.get(this, "name");
    }
    
    @Override
    public String desc() {
        StringBuilder sb = new StringBuilder(Messages.get(this, "desc_intro"));
        if (charge > 0) {
            sb.append("\n\n").append(Messages.get(this, "desc_charge", charge, chargeCap));
        }
        sb.append("\n\n").append(Messages.get(this, "desc_body"));
        return sb.toString();
    }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);
        
        if (attacker instanceof Hero) {
            Hero hero = (Hero) attacker;
            
            // 检查是否是突袭攻击
            boolean isAmbush = defender instanceof Mob && ((Mob) defender).surprisedBy(hero);
            
            if (isAmbush) {
                // 检查是否会击杀敌人（在伤害应用之前，通过 HP <= damage 判断）
                boolean willKill = defender.HP <= damage;
                
                // 如果突袭且有充能，尝试召唤召唤物（在敌人还活着时）
                if (charge > 0 && !willKill) {
                    trySummonAndAttack(hero, defender);
                }
                
                // 如果突袭时会击杀敌人，充能
                // 注意：这里使用 willKill 判断，虽然可能不够精确，但对于游戏逻辑来说是可以接受的
                if (willKill && charge < chargeCap) {
                    charge++;
                    updateQuickslot();
                    GLog.p(Messages.get(SummoningAmbushWeapon.class, "msg_charge_up", charge, chargeCap));
                }
            }
        }
        
        return damage;
    }
    
    private void trySummonAndAttack(Hero hero, Char enemy) {
        // 找到敌人附近的空位置
        int summonPos = findNearbyEmptyCell(enemy.pos);
        if (summonPos == -1) {
            // 没有空位置，不召唤
            return;
        }
        
        // 根据充能强度和武器tier决定召唤物的强度
        // tier提供基础加成，charge提供额外加成
        int basePower = tier * 2; // tier每级提供2点基础力量
        int chargePower = Math.min(charge, chargeCap);
        int powerLevel = basePower + chargePower;
        
        // 创建召唤物
        SummonedMinion minion = new SummonedMinion(this, powerLevel, tier);
        minion.pos = summonPos;
        minion.state = minion.HUNTING;
        minion.setTarget(enemy.pos);
        
        // 添加到场景
        GameScene.add(minion);
        
        // 召唤特效
        CellEmitter.get(summonPos).burst(Speck.factory(Speck.STAR), 6);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
        
        // 消耗充能（消耗1点，但可以根据需要调整）
        charge--;
        updateQuickslot();
        
        GLog.p(Messages.get(SummoningAmbushWeapon.class, "msg_summoned", minion.name()));
        
        // 召唤物立即攻击敌人
        if (enemy.isAlive() && Actor.findChar(enemy.pos) == enemy) {
            minion.enemy = enemy;
            minion.enemySeen = true;
            // 让召唤物在下一回合攻击
            minion.state = minion.HUNTING;
        }
    }

    private void refundCharge(int amount) {
        if (amount <= 0) return;
        int before = charge;
        charge = Math.min(chargeCap, charge + amount);
        if (charge != before) {
            updateQuickslot();
            GLog.p(Messages.get(SummoningAmbushWeapon.class, "msg_charge_up", charge, chargeCap));
        }
    }
    
    private int findNearbyEmptyCell(int centerPos) {
        // 先检查8个相邻位置
        for (int offset : PathFinder.NEIGHBOURS8) {
            int pos = centerPos + offset;
            if (Dungeon.level.passable[pos] && Actor.findChar(pos) == null) {
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
    
    @Override
    public Item upgrade() {
        chargeCap += 2;
        if (chargeCap > 20) chargeCap = 20;
        return super.upgrade();
    }

    public SummoningAmbushWeapon randomize() {
        tier = Random.IntRange(1, 5);
        level(Random.IntRange(0, 3));
        chargeCap = Random.IntRange(8, 20);
        charge = Random.IntRange(0, chargeCap);
        ambushRate = Random.Float(0.4f, 1.2f);
        return this;
    }

    @Override
    public Item random() {
        return randomize();
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
        charge = bundle.getInt(CHARGE);
        chargeCap = bundle.getInt(CHARGE_CAP);
        if (chargeCap == 0) chargeCap = 10; // 默认值
    }
    
    // 召唤物内部类
    public static class SummonedMinion extends Mob {
        
        private SummoningAmbushWeapon weapon;
        private int powerLevel;
        private int weaponTier;
        private int idleTurns = 0; // 没有目标时的空闲回合数
        private static final int MAX_IDLE_TURNS = 5; // 最大空闲回合数，超过后消失
        
        {
            spriteClass = GhostSprite.class;
            alignment = Alignment.ALLY;
            
            // 不生成战利品
            lootChance = 0f;
            
            // 不显示经验值
            EXP = 0;
            
            // 默认状态
            state = HUNTING;
        }
        
        public SummonedMinion(SummoningAmbushWeapon weapon, int powerLevel, int weaponTier) {
            this.weapon = weapon;
            this.powerLevel = powerLevel;
            this.weaponTier = weaponTier;
            
            // 根据充能强度和武器tier设置属性
            // powerLevel = tier * 2 + charge，范围更大
            // 基础HP: 10 + powerLevel * 2 + tier * 3 (tier额外加成)
            HP = HT = 10 + powerLevel * 2 + weaponTier * 3;
            
            // 基础防御: 2 + powerLevel + tier (tier额外加成)
            defenseSkill = 2 + powerLevel + weaponTier;
            
            // 最大等级: (powerLevel + tier) / 2 (向上取整)
            maxLvl = Math.max(1, (powerLevel + weaponTier + 1) / 2);
        }
        
        @Override
        public int damageRoll() {
            // 伤害: 基础1-3 + powerLevel + tier (tier额外加成)
            int base = Random.NormalIntRange(1, 3);
            return base + powerLevel + weaponTier;
        }
        
        @Override
        public int attackSkill(Char target) {
            // 攻击技能: 基础8 + powerLevel * 2 + tier * 2 (tier额外加成)
            return 8 + powerLevel * 2 + weaponTier * 2;
        }
        
        @Override
        public int drRoll() {
            // 伤害减免: 0 到 (powerLevel + tier) / 2
            return Random.NormalIntRange(0, (powerLevel + weaponTier) / 2);
        }

        @Override
        public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti, int hitCount) {
            boolean wasAlive = enemy != null && enemy.isAlive();
            boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti, hitCount);
            if (wasAlive && enemy != null && !enemy.isAlive() && enemy.alignment == Alignment.ENEMY) {
                if (weapon != null) weapon.refundCharge(1);
            }
            return result;
        }
        
        @Override
        public String name() {
            return Messages.get(SummonedMinion.class, "name", powerLevel, weaponTier);
        }
        
        @Override
        public String description() {
            return Messages.get(SummonedMinion.class, "desc");
        }
        
        @Override
        protected boolean act() {
            // 如果英雄死亡，召唤物也消失
            if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
                die(null);
                return true;
            }
            
            // 初始化视野数组（如果为 null 或长度不匹配）
            if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
                fieldOfView = new boolean[Dungeon.level.length()];
            }
            
            // 更新视野
            Dungeon.level.updateFieldOfView(this, fieldOfView);
            
            // 如果目标不存在或已死亡，寻找新目标
            if (enemy == null || !enemy.isAlive() || enemy == this) {
                enemy = findTargetInFOV();
                if (enemy != null) {
                    enemySeen = true;
                    idleTurns = 0; // 找到目标，重置空闲计时器
                } else {
                    // 没有找到目标，增加空闲回合数
                    idleTurns++;
                    // 如果空闲回合数超过最大值，消失
                    if (idleTurns >= MAX_IDLE_TURNS) {
                        die(null);
                        return true;
                    }
                    // 没有目标时，保持静止状态
                    state = PASSIVE;
                }
            } else {
                // 有目标，重置空闲计时器
                idleTurns = 0;
            }
            
            return super.act();
        }
        
        private Char findTargetInFOV() {
            // 在视野内寻找最近的敌人
            Char closest = null;
            int closestDist = Integer.MAX_VALUE;
            
            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob.alignment == Alignment.ENEMY && mob.isAlive() 
                        && fieldOfView[mob.pos] && mob.invisible <= 0) {
                    int dist = Dungeon.level.distance(pos, mob.pos);
                    if (dist < closestDist) {
                        closest = mob;
                        closestDist = dist;
                    }
                }
            }
            
            return closest;
        }
        
        @Override
        public void die(Object cause) {
            super.die(cause);
            // 召唤物死亡时可能有特效
        }
        
        public void setTarget(int cell) {
            target = cell;
        }
        
        @Override
        public Item createLoot() {
            // 召唤物不生成战利品
            return null;
        }
    }
}

