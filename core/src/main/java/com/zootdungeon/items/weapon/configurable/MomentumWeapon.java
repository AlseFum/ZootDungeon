package com.zootdungeon.items.weapon.configurable;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.effects.Pushing;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.plants.Swiftthistle;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class MomentumWeapon extends MeleeWeapon {
    
    // 速度阈值：超过这个速度时推开敌人
    public float pushThreshold = 1.2f;
    
    // 伤害倍率：每单位速度增加的伤害倍率
    public float damageMultiplierPerSpeed = 0.2f;
    
    // 冲量衰减：每回合衰减
    public float momentumDecay = 0.15f;
    
    {
        image = ItemSpriteSheet.SPEAR;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.0f;
        
        tier = 0;
    }
    
    @Override
    public void activate(Char ch) {
        super.activate(ch);
        if (ch instanceof Hero) {
            Buff.affect(ch, MomentumTracker.class).weapon = this;
        }
    }
    
    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (super.doUnequip(hero, collect, single)) {
            MomentumTracker tracker = hero.buff(MomentumTracker.class);
            if (tracker != null) {
                tracker.detach();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public String name() {
        return Messages.get(this, "name");
    }
    
    @Override
    public String desc() {
        return Messages.get(this, "desc",
                (int) pushThreshold,
                (int) (damageMultiplierPerSpeed * 100));
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }

    public MomentumWeapon randomize() {
        tier = com.watabou.utils.Random.IntRange(0, 5);
        level(com.watabou.utils.Random.IntRange(0, 3));
        pushThreshold = com.watabou.utils.Random.Float(0.8f, 2.2f);
        damageMultiplierPerSpeed = com.watabou.utils.Random.Float(0.1f, 0.45f);
        momentumDecay = com.watabou.utils.Random.Float(0.05f, 0.25f);
        return this;
    }

    @Override
    public Item random() {
        return randomize();
    }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        // 检查是否在时间气泡中
        if (attacker instanceof Hero && attacker.buff(Swiftthistle.TimeBubble.class) != null) {
            // 在时间气泡中的特殊攻击
            Hero hero = (Hero) attacker;
            
            // 计算特殊伤害：min(100 + tier * 30, 30% * target.HT)
            int timeBubbleDamage = Math.max(100 + tier * 30, Math.round(defender.HT * 0.3f));
            
            // 武器损坏：从装备中移除并丢弃
            if (isEquipped(hero)) {
                doUnequip(hero, false, false);
                if (hero.belongings.backpack != null) {
                    detach(hero.belongings.backpack);
                }
                GLog.w(Messages.get(this, "time_bubble_broken"));
            }
            
            // 返回特殊伤害，替换正常伤害
            return timeBubbleDamage;
        }
        
        // 先调用父类的 proc 方法处理基础逻辑
        damage = super.proc(attacker, defender, damage);
        
        // 获取动量追踪器
        MomentumTracker tracker = attacker.buff(MomentumTracker.class);
        if (tracker != null && tracker.weapon == this) {
            float momentum = tracker.getCurrentSpeed();
            
            // 根据冲量增加伤害
            if (momentum > 0) {
                float damageMultiplier = 1.0f + momentum * damageMultiplierPerSpeed;
                damage = Math.round(damage * damageMultiplier);
            }
            
            // 如果冲量够高，推开敌人
            if (momentum >= pushThreshold && defender.isAlive()) {
                // 计算推开方向：从攻击者指向目标
                Ballistica trajectory = new Ballistica(attacker.pos, defender.pos, Ballistica.PROJECTILE);
                
                // 如果轨迹有效且目标还在原位置
                if (trajectory.path.size() > 1 && defender.pos == trajectory.collisionPos) {
                    // 计算推开强度：基于冲量
                    int pushPower = Math.min(3, Math.round(momentum));
                    
                    // 推开敌人
                    WandOfBlastWave.throwChar(defender, trajectory, pushPower, false, false, this);
                }
            }
        }
        
        return damage;
    }
    
    // 动量追踪器 Buff
    public static class MomentumTracker extends Buff {
        
        {
            type = buffType.POSITIVE;
            announced = false;
        }
        
        public MomentumWeapon weapon;
        
        // 位置历史：记录最近几次移动的位置（用于计算冲量）
        private ArrayList<Integer> positionHistory = new ArrayList<>();
        // 当前冲量值
        private float currentMomentum = 0f;
        
        // 获取最大历史记录数量（基于武器 tier）
        private int getMaxHistorySize() {
            if (weapon != null) {
                // tier 0 = 3, tier 1 = 4, tier 2 = 5, tier 3 = 6, tier 4 = 7, tier 5 = 8
                return 3 + weapon.tier;
            }
            return 3; // 默认值
        }
        
        @Override
        public String name() {
            return Messages.get(MomentumTracker.class, "name");
        }

        @Override
        public int icon() {
            return BuffIndicator.HASTE;
        }
        
        @Override
        public void tintIcon(Image icon) {
            // 根据冲量改变颜色：冲量越高颜色越亮
            float intensity = Math.min(1.0f, currentMomentum / 3.0f);
            icon.hardlight(0.5f + 0.5f * intensity, 1f, 0.5f + 0.5f * intensity); // 黄绿色渐变
        }
        
        @Override
        public String iconTextDisplay() {
            // 在图标上显示当前冲量
            if (currentMomentum > 0) {
                return String.format("%.1f", currentMomentum);
            }
            return ""; // 返回空字符串而不是 null
        }
        
        @Override
        public String desc() {
            if (currentMomentum <= 0) {
                return Messages.get(MomentumWeapon.class, "momentum_desc_no_speed");
            }
            float dmgScale = weapon == null ? 0.2f : weapon.damageMultiplierPerSpeed;
            float threshold = weapon == null ? 1.2f : weapon.pushThreshold;
            int damageBonus = (int)(currentMomentum * dmgScale * 100);
            if (currentMomentum >= threshold) {
                return Messages.get(MomentumWeapon.class, "momentum_desc_with_push", 
                        String.format("%.1f", currentMomentum), damageBonus);
            } else {
                return Messages.get(MomentumWeapon.class, "momentum_desc", 
                        String.format("%.1f", currentMomentum), damageBonus);
            }
        }
        
        @Override
        public boolean act() {
            if (target instanceof Hero) {
                Hero hero = (Hero) target;
                
                // 记录当前位置
                int currentPos = hero.pos;
                
                // 检查位置是否改变
                if (positionHistory.isEmpty() || positionHistory.get(positionHistory.size() - 1) != currentPos) {
                    // 位置改变了，记录当前位置
                    positionHistory.add(currentPos);
                    
                    // 限制历史记录数量（基于武器 tier）
                    int maxSize = getMaxHistorySize();
                    if (positionHistory.size() > maxSize) {
                        positionHistory.remove(0);
                    }
                    
                    // 计算冲量：基于最近几次移动的累积距离和方向一致性
                    calculateMomentum();
                } else {
                    // 位置没改变，衰减冲量
                    float decay = weapon == null ? 0.15f : weapon.momentumDecay;
                    currentMomentum = Math.max(0f, currentMomentum - decay);
                }
            }
            
            spend(TICK);
            return true;
        }
        
        // 计算冲量：基于位置历史
        private void calculateMomentum() {
            if (positionHistory.size() < 2) {
                currentMomentum = 0f;
                return;
            }
            
            int maxSize = getMaxHistorySize();
            // 速度加成阈值：当历史记录达到最大长度的70%时，开始考虑速度加成
            int speedBonusThreshold = Math.max(2, (int)(maxSize * 0.7f));
            boolean useSpeedBonus = positionHistory.size() >= speedBonusThreshold;
            
            float totalMomentum = 0f;
            int consecutiveDirection = 0; // 连续同方向移动的计数
            float currentMoveSpeed = 0f; // 当前移动速度（从位置历史计算）
            
            // 计算每次移动的距离和方向
            for (int i = 1; i < positionHistory.size(); i++) {
                int prevPos = positionHistory.get(i - 1);
                int currPos = positionHistory.get(i);
                
                // 计算移动距离
                int distance = Dungeon.level.distance(prevPos, currPos);
                
                // 获取最后一次移动的速度（从位置历史计算，即当前移动距离）
                if (i == positionHistory.size() - 1) {
                    // 如果target是Hero，使用hero.speed()，否则使用移动距离
                    if (target instanceof Hero) {
                        currentMoveSpeed = ((Hero) target).speed();
                    } else {
                        currentMoveSpeed = distance;
                    }
                }
                
                // 计算移动方向（简化：使用位置差）
                int dx1 = (prevPos % Dungeon.level.width()) - (currPos % Dungeon.level.width());
                int dy1 = (prevPos / Dungeon.level.width()) - (currPos / Dungeon.level.width());
                
                // 检查是否与前一次移动方向一致
                if (i > 1) {
                    int prevPrevPos = positionHistory.get(i - 2);
                    int dx2 = (prevPrevPos % Dungeon.level.width()) - (prevPos % Dungeon.level.width());
                    int dy2 = (prevPrevPos / Dungeon.level.width()) - (prevPos / Dungeon.level.width());
                    
                    // 方向一致（大致相同）
                    if ((dx1 > 0 && dx2 > 0) || (dx1 < 0 && dx2 < 0) || (dx1 == 0 && dx2 == 0)) {
                        if ((dy1 > 0 && dy2 > 0) || (dy1 < 0 && dy2 < 0) || (dy1 == 0 && dy2 == 0)) {
                            consecutiveDirection++;
                        }
                    }
                }
                
                // 基础冲量 = 移动距离
                // 如果方向一致，给予额外加成（连续移动累积更多冲量）
                float momentum = distance * (1.0f + consecutiveDirection * 0.2f);
                totalMomentum += momentum;
            }
            
            // 如果达到速度加成阈值，使用当前移动速度加入冲量
            if (useSpeedBonus && currentMoveSpeed > 0) {
                // 速度加成：当前移动速度越高，冲量加成越多（最多加成50%）
                float speedBonus = Math.min(0.5f, currentMoveSpeed * 0.15f);
                totalMomentum *= (1.0f + speedBonus);
            }
            
            currentMomentum = totalMomentum;
        }
        
        public float getCurrentSpeed() {
            return currentMomentum;
        }
        
        @Override
        public void detach() {
            super.detach();
            positionHistory.clear();
            currentMomentum = 0f;
        }
        
        private static final String POSITION_HISTORY = "position_history";
        private static final String CURRENT_MOMENTUM = "current_momentum";
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            // 将位置历史转换为 int 数组存储
            int[] historyArray = new int[positionHistory.size()];
            for (int i = 0; i < positionHistory.size(); i++) {
                historyArray[i] = positionHistory.get(i);
            }
            bundle.put(POSITION_HISTORY, historyArray);
            bundle.put(CURRENT_MOMENTUM, currentMomentum);
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            positionHistory.clear();
            if (bundle.contains(POSITION_HISTORY)) {
                int[] historyArray = bundle.getIntArray(POSITION_HISTORY);
                if (historyArray != null) {
                    for (int pos : historyArray) {
                        positionHistory.add(pos);
                    }
                }
            }
            currentMomentum = bundle.contains(CURRENT_MOMENTUM) ? bundle.getFloat(CURRENT_MOMENTUM) : 0f;
        }
    }

    private static final String PUSH_THRESHOLD = "pushThreshold";
    private static final String DAMAGE_PER_SPEED = "damageMultiplierPerSpeed";
    private static final String MOMENTUM_DECAY = "momentumDecay";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(PUSH_THRESHOLD, pushThreshold);
        bundle.put(DAMAGE_PER_SPEED, damageMultiplierPerSpeed);
        bundle.put(MOMENTUM_DECAY, momentumDecay);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(PUSH_THRESHOLD)) pushThreshold = bundle.getFloat(PUSH_THRESHOLD);
        if (bundle.contains(DAMAGE_PER_SPEED)) damageMultiplierPerSpeed = bundle.getFloat(DAMAGE_PER_SPEED);
        if (bundle.contains(MOMENTUM_DECAY)) momentumDecay = bundle.getFloat(MOMENTUM_DECAY);
    }
}

