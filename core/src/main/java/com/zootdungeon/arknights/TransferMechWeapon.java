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
import com.zootdungeon.sprites.GhostSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.HashSet;

public class TransferMechWeapon extends MeleeWeapon {
    
    private static final String AC_RELEASE_MECH = "RELEASE_MECH";
    private static final String MECH_COUNT = "mechCount";
    
    // 存储所有活跃的mech（包括buff和char形式）
    private HashSet<TransferMechBuff> activeMechBuffs = new HashSet<>();
    private HashSet<TransferMechChar> activeMechChars = new HashSet<>();
    
    {
        image = ItemSpriteSheet.DAGGER;
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
            return "释放机甲";
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
            TransferMechBuff mech = new TransferMechBuff();
            mech.weapon = this;
            mech.power = tier + buffedLvl();
            if (mech.attachTo(target)) {
                activeMechBuffs.add(mech);
            }
        }
        
        // 特效
        CellEmitter.get(target.pos).burst(Speck.factory(Speck.STAR), mechCount * 2);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
        
        GLog.p("释放了 " + mechCount + " 个机甲！");
        hero.spendAndNext(1f);
    }
    
    // 当敌人死亡时，将buff转换为char
    public void convertBuffToChar(TransferMechBuff buff, int deathPos) {
        activeMechBuffs.remove(buff);
        
        // 创建char形式的mech
        TransferMechChar mechChar = new TransferMechChar();
        mechChar.weapon = this;
        mechChar.power = buff.power;
        mechChar.pos = deathPos;
        mechChar.state = mechChar.HUNTING;
        
        GameScene.add(mechChar);
        activeMechChars.add(mechChar);
        
        // 特效
        CellEmitter.get(deathPos).burst(Speck.factory(Speck.STAR), 6);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
    }
    
    // 当char找到新目标时，转换为buff
    public void convertCharToBuff(TransferMechChar mechChar, Char newTarget) {
        activeMechChars.remove(mechChar);
        mechChar.die(null);
        
        // 创建buff形式的mech
        TransferMechBuff mechBuff = new TransferMechBuff();
        mechBuff.weapon = this;
        mechBuff.power = mechChar.power;
        if (mechBuff.attachTo(newTarget)) {
            activeMechBuffs.add(mechBuff);
        }
        
        // 特效
        CellEmitter.get(newTarget.pos).burst(Speck.factory(Speck.STAR), 4);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
    }
    
    // 移除mech
    public void removeMechBuff(TransferMechBuff mech) {
        activeMechBuffs.remove(mech);
    }
    
    public void removeMechChar(TransferMechChar mech) {
        activeMechChars.remove(mech);
    }
    
    // 获取所有活跃的mech
    public HashSet<TransferMechBuff> getActiveMechBuffs() {
        return new HashSet<>(activeMechBuffs);
    }
    
    public HashSet<TransferMechChar> getActiveMechChars() {
        return new HashSet<>(activeMechChars);
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
        bundle.put(MECH_COUNT, activeMechBuffs.size() + activeMechChars.size());
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        // 注意：mech会在游戏加载时自动恢复，这里只需要清空集合
        activeMechBuffs.clear();
        activeMechChars.clear();
    }
    
    // Mech Buff类
    public static class TransferMechBuff extends Buff {
        
        public TransferMechWeapon weapon;
        public int power = 1;
        private static final int AOE_RANGE = 2; // 对附近单位造成伤害的范围
        
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
            icon.hardlight(0.5f, 0.5f, 1f); // 蓝色
        }
        
        @Override
        public String iconTextDisplay() {
            return String.valueOf(power);
        }
        
        @Override
        public boolean act() {
            if (target == null || !target.isAlive()) {
                // 目标死亡，转换为char
                if (weapon != null && target != null) {
                    int deathPos = target.pos;
                    weapon.convertBuffToChar(this, deathPos);
                }
                detach();
                return true;
            }
            
            // 对目标本身造成伤害
            int damage = Random.NormalIntRange(power, power * 2);
            target.damage(damage, this);
            
            // 对附近单位造成伤害
            damageNearbyEnemies();
            
            spend(TICK);
            return true;
        }
        
        private void damageNearbyEnemies() {
            if (target == null || !target.isAlive()) {
                return;
            }
            
            int centerPos = target.pos;
            
            // 对范围内的所有敌人造成伤害
            for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
                int pos = centerPos + PathFinder.NEIGHBOURS8[i];
                if (pos >= 0 && pos < Dungeon.level.length()) {
                    Char ch = Actor.findChar(pos);
                    if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch != target) {
                        int aoeDamage = Random.NormalIntRange(power, power * 2);
                        ch.damage(aoeDamage, this);
                    }
                }
            }
        }
        
        @Override
        public void detach() {
            if (weapon != null) {
                weapon.removeMechBuff(this);
            }
            super.detach();
        }
        
        private static final String POWER = "power";
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(POWER, power);
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            power = bundle.getInt(POWER);
        }
        
        @Override
        public String desc() {
            return Messages.get(TransferMechWeapon.class, "mech_buff_desc", power);
        }
    }
    
    // Mech Char类
    public static class TransferMechChar extends Mob {
        
        public TransferMechWeapon weapon;
        public int power = 1;
        private static final int SEARCH_RANGE = 8; // 搜索敌人的范围
        
        {
            spriteClass = GhostSprite.class;
            alignment = Alignment.ALLY;
            intelligentAlly = true;
            
            lootChance = 0f;
            EXP = 0;
            
            state = HUNTING;
        }
        
        public TransferMechChar() {
            // 设置基础属性
            HP = HT = 10 + power * 2;
            defenseSkill = 5 + power;
            maxLvl = Math.max(1, power);
        }
        
        @Override
        public int damageRoll() {
            return Random.NormalIntRange(power, power * 2);
        }
        
        @Override
        public int attackSkill(Char target) {
            return 10 + power * 2;
        }
        
        @Override
        public int drRoll() {
            return Random.NormalIntRange(0, power / 2);
        }
        
        @Override
        public String name() {
            return "转移机甲";
        }
        
        @Override
        public String description() {
            return Messages.get(this, "desc");
        }
        
        @Override
        protected boolean act() {
            // 如果英雄死亡，mech也消失
            if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
                if (weapon != null) {
                    weapon.removeMechChar(this);
                }
                die(null);
                return true;
            }
            
            // 初始化视野数组
            if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
                fieldOfView = new boolean[Dungeon.level.length()];
            }
            
            // 更新视野
            Dungeon.level.updateFieldOfView(this, fieldOfView);
            
            // 寻找附近的敌人
            Char target = findNearbyEnemy();
            
            if (target != null) {
                // 找到敌人，移动到敌人位置并转换为buff
                if (Dungeon.level.adjacent(pos, target.pos)) {
                    // 已经在相邻位置，直接转换
                    if (weapon != null) {
                        weapon.convertCharToBuff(this, target);
                    }
                    return true;
                } else {
                    // 移动到敌人附近
                    if (getCloser(target.pos)) {
                        return true;
                    } else {
                        // 无法移动，等待
                        spend(1 / speed());
                        return true;
                    }
                }
            } else {
                // 没有找到敌人，等待
                spend(1 / speed());
                return true;
            }
        }
        
        private Char findNearbyEnemy() {
            Char closest = null;
            int closestDist = Integer.MAX_VALUE;
            
            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob.alignment == Alignment.ENEMY && mob.isAlive()) {
                    int dist = Dungeon.level.distance(pos, mob.pos);
                    if (dist <= SEARCH_RANGE && dist < closestDist) {
                        if (fieldOfView == null || fieldOfView[mob.pos]) {
                            closest = mob;
                            closestDist = dist;
                        }
                    }
                }
            }
            
            return closest;
        }
        
        @Override
        public void die(Object cause) {
            if (weapon != null) {
                weapon.removeMechChar(this);
            }
            super.die(cause);
        }
        
        @Override
        public Item createLoot() {
            return null;
        }
        
        private static final String POWER = "power";
        private static final String WEAPON_ID = "weapon_id";
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(POWER, power);
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            power = bundle.getInt(POWER);
            // 重新设置属性
            HP = HT = 10 + power * 2;
            defenseSkill = 5 + power;
            maxLvl = Math.max(1, power);
        }
    }
}
