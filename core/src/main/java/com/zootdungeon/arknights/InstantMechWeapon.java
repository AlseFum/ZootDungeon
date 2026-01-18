package com.zootdungeon.arknights;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.HashSet;

public class InstantMechWeapon extends MeleeWeapon {
    
    private static final String AC_RELEASE_MECH = "RELEASE_MECH";
    private static final String MECH_COUNT = "mechCount";
    
    // 存储所有活跃的mech
    private HashSet<InstantMechBuff> activeMechs = new HashSet<>();
    
    {
        image = ItemSpriteSheet.SWORD;
        tier = 2;
    }
    
    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            actions.add(AC_RELEASE_MECH);
        }
        return actions;
    }
    
    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_RELEASE_MECH)) {
            return "释放无人机";
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_RELEASE_MECH)) {
            GameScene.selectCell(new MechTargeter(hero));
        }
    }
    
    private class MechTargeter extends CellSelector.Listener {
        private Hero hero;
        
        public MechTargeter(Hero hero) {
            this.hero = hero;
        }
        
        @Override
        public void onSelect(Integer target) {
            if (target != null) {
                Char targetChar = Actor.findChar(target);
                if (targetChar != null && targetChar.alignment == Char.Alignment.ENEMY) {
                    releaseMechs(targetChar, hero);
                } else {
                    GLog.w("必须选择一个敌人目标！");
                }
            }
        }
        
        @Override
        public String prompt() {
            return "选择目标敌人";
        }
    }
    
    private void releaseMechs(Char target, Hero hero) {
        // 根据tier和level计算mech数量：2 + tier + level
        int mechCount = 2 + tier + buffedLvl();
        
        for (int i = 0; i < mechCount; i++) {
            // 创建mech buff并附加到目标
            InstantMechBuff mech = new InstantMechBuff();
            mech.weapon = this;
            mech.power = tier + buffedLvl();
            if (mech.attachTo(target)) {
                activeMechs.add(mech);
            }
        }
        
        // 特效
        CellEmitter.get(target.pos).burst(Speck.factory(Speck.STAR), mechCount * 2);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
        
        GLog.p("释放了 " + mechCount + " 个机甲！");
        hero.spendAndNext(1f);
    }
    
    // 移除mech（当mech被销毁时调用）
    public void removeMech(InstantMechBuff mech) {
        activeMechs.remove(mech);
    }
    
    // 获取所有活跃的mech
    public HashSet<InstantMechBuff> getActiveMechs() {
        return new HashSet<>(activeMechs);
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
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(MECH_COUNT, activeMechs.size());
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        // 注意：mech会在游戏加载时自动恢复，这里只需要清空集合
        activeMechs.clear();
    }
    
    // Mech Buff类
    public static class InstantMechBuff extends Buff {
        
        public InstantMechWeapon weapon;
        public int power = 1;
        private static final float EXPLODE_CHANCE = 0.15f; // 爆炸概率
        private static final float AUTO_TARGET_CHANCE = 0.2f; // 自动索敌概率（每回合）
        private static final int TRANSFER_RANGE = 5; // 转移范围
        private static final int EXPLODE_RANGE = 2; // 爆炸范围
        
        {
            type = buffType.NEGATIVE;
            announced = true;
        }
        
        @Override
        public int icon() {
            return BuffIndicator.CORRUPT;
        }
        
        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(1f, 0.5f, 0.5f); // 红色
        }
        
        @Override
        public String iconTextDisplay() {
            return String.valueOf(power);
        }
        
        @Override
        public boolean act() {
            // 如果目标死亡，尝试转移到其他敌人
            if (target == null || !target.isAlive()) {
                if (transferToOtherEnemy()) {
                    // 转移成功，继续执行
                    spend(TICK);
                    return true;
                } else {
                    // 没有可转移的目标，移除
                    detach();
                    return true;
                }
            }
            
            // 先消耗时间，确保每回合正确计时
            spend(TICK);
            
            // 每回合造成伤害
            int damage = Random.NormalIntRange(power, power * 2);
            target.damage(damage, this);
            
            // 检查是否爆炸
            if (Random.Float() < EXPLODE_CHANCE) {
                explode();
                // 爆炸后转移到其他敌人
                transferToOtherEnemy();
            } else {
                // 正常情况也可能自动索敌并转移（较低概率）
                if (Random.Float() < AUTO_TARGET_CHANCE) {
                    transferToOtherEnemy();
                }
            }
            
            return true;
        }
        
        private void explode() {
            if (target == null || !target.isAlive()) {
                return;
            }
            
            int centerPos = target.pos;
            
            // 对爆炸范围内的所有敌人造成伤害
            for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
                int pos = centerPos + PathFinder.NEIGHBOURS8[i];
                if (pos >= 0 && pos < Dungeon.level.length()) {
                    Char ch = Actor.findChar(pos);
                    if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch != target) {
                        int explodeDamage = Random.NormalIntRange(power * 2, power * 3);
                        ch.damage(explodeDamage, this);
                    }
                }
            }
            
            // 特效
            CellEmitter.get(centerPos).burst(Speck.factory(Speck.INFERNO), 8);
            Sample.INSTANCE.play(Assets.Sounds.BLAST);
        }
        
        private boolean transferToOtherEnemy() {
            // 获取当前位置（如果目标已死亡，使用死亡位置）
            int currentPos;
            if (target != null) {
                currentPos = target.pos;
            } else {
                // 如果target为null，无法转移
                return false;
            }
            
            ArrayList<Char> candidates = new ArrayList<>();
            
            // 在范围内寻找其他敌人
            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob.alignment == Char.Alignment.ENEMY 
                        && mob.isAlive() 
                        && mob != target
                        && Dungeon.level.distance(mob.pos, currentPos) <= TRANSFER_RANGE) {
                    candidates.add(mob);
                }
            }
            
            if (!candidates.isEmpty()) {
                // 随机选择一个敌人转移
                Char newTarget = Random.element(candidates);
                
                // 保存当前时间状态（在detach之前）
                float currentTime = cooldown();
                
                // 从当前目标移除（如果目标还存在）
                if (target != null) {
                    detach();
                }
                
                // 附加到新目标
                if (attachTo(newTarget)) {
                    // 恢复时间状态，确保转移后时间正确
                    if (currentTime > 0) {
                        postpone(currentTime);
                    } else {
                        timeToNow();
                    }
                    
                    if (weapon != null) {
                        weapon.activeMechs.add(this);
                    }
                    return true;
                }
            }
            
            return false;
        }
        
        @Override
        public void detach() {
            if (weapon != null) {
                weapon.removeMech(this);
            }
            super.detach();
        }
        
        private static final String POWER = "power";
        private static final String WEAPON_ID = "weapon_id";
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(POWER, power);
            // 注意：weapon引用需要在恢复时重新设置
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            power = bundle.getInt(POWER);
            // weapon引用需要在武器恢复时重新设置
        }
        
        @Override
        public String desc() {
            return Messages.get(InstantMechWeapon.class, "mech_desc", power);
        }
    }
}
