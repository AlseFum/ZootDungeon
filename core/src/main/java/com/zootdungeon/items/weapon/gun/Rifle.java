package com.zootdungeon.items.weapon.gun;

import java.util.ArrayList;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Rifle extends Gun {

    private static final int SHOTS_PER_BURST = 3; // 每次点射发射的子弹数
    private static final int SHOTS_PER_SPRAY = 10; // 扫射消耗的弹药量
    private static final float SPRAY_CHANCE = 0.4f; // 扫射命中周围格子的基础概率

    private static final String AC_BLAST = "blast";
    private static final String AC_SPRAY = "spray";

    {
        image = ItemSpriteSheet.CROSSBOW; // 暂时使用十字弩的图标
        defaultAction = AC_BLAST; // 默认动作为点射

        maxAmmo = 30;
        ammo = maxAmmo;
        reloadTime = 1.5f;

        usesTargeting = true;
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    protected void addSubActions(Hero hero, ArrayList<String> actions) {
        actions.add(AC_BLAST);
        actions.add(AC_SPRAY);
    }

    @Override
    public String subActionName(String action, Hero hero) {
        if (action.equals(AC_BLAST)) {
            return Messages.get(this, "ac_blast");
        }
        if (action.equals(AC_SPRAY)) {
            return Messages.get(this, "ac_spray");
        }
        return null;
    }

    @Override
    protected void executeSubAction(Hero hero, String action) {
        if (action.equals(AC_BLAST)) {
            if (ammo < SHOTS_PER_BURST) {
                GLog.w(Messages.get(this, "ammo_need"), SHOTS_PER_BURST);
                return;
            }
            GameScene.selectCell(blask_callback);

        } else if (action.equals(AC_SPRAY)) {
            if (ammo < SHOTS_PER_SPRAY) {
                GLog.w(Messages.get(this, "ammo_need"), SHOTS_PER_SPRAY);
                return;
            }
            GameScene.selectCell(spray_callback);
        }
    }

    public CellSelector.Listener blask_callback = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target != null) {
                blast(target);
            }
        }

        @Override
        public String prompt() {
            return Messages.get(Rifle.this, "prompt_blast");
        }
    };

    public CellSelector.Listener spray_callback = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target != null) {
                spray(target);
            }
        }

        @Override
        public String prompt() {
            return Messages.get(Rifle.this, "prompt_spray");
        }
    };

    private void blast(int targetPos) {
        if (ammo < SHOTS_PER_BURST) {
            GLog.w(Messages.get(this, "ammo_need"), SHOTS_PER_BURST);
            return;
        }
        curUser.sprite.zap(targetPos);
        for (int i = 0; i < SHOTS_PER_BURST; i++) {
            fire(targetPos, false); // 使用fire而不是shoot，并且不消耗行动点
            float pitch = 0.8f + Random.Float(0.4f);
            Sample.INSTANCE.play(Assets.Sounds.HIT, pitch);
            consumeAmmo(1);
            }
            curUser.spendAndNext(delayFactor(curUser));
        
    }

    private void spray(int targetPos) {
        if (ammo < SHOTS_PER_SPRAY) {
            GLog.w(Messages.get(this, "ammo_need"), SHOTS_PER_SPRAY);
            return;
        }

        curUser.sprite.zap(targetPos);
        for (int i = 0; i < SHOTS_PER_SPRAY; i++) {
            // 计算实际射击位置
            int actualTargetPos = targetPos;
            if (Random.Float() < SPRAY_CHANCE) { // 30%概率射偏
                // 获取周围8格的位置
                int[] neighbors = PathFinder.NEIGHBOURS8;
                int randomNeighbor = neighbors[Random.Int(neighbors.length)];
                actualTargetPos = targetPos + randomNeighbor;
            }
            
            fire(actualTargetPos, false); // 使用fire而不是shoot，并且不消耗行动点
            float pitch = 0.8f + Random.Float(0.4f);
            Sample.INSTANCE.play(Assets.Sounds.HIT, pitch);
            consumeAmmo(1);
        }
        curUser.spendAndNext(delayFactor(curUser));
    }

    @Override
    public int STRReq(int lvl) {
        return 8 + Math.round(lvl * 0.5f); // 较低的力量需求
    }

    @Override
    public int min(int lvl) {
        return 3 + 2 * lvl; // 较低的单发伤害
    }

    @Override
    public int max(int lvl) {
        return 8 + 4 * lvl; // 较低的单发伤害
    }
}
