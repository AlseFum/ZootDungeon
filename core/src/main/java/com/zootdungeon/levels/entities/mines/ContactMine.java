package com.zootdungeon.levels.entities.mines;

import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.levels.entities.CellEntitySprite;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.CharSprite;

/**
 * 接触地雷。
 * <p>
 * 敌方角色踩上即爆炸：对该敌人造成 {@link #damage()} 点伤害
 * （走 {@link Char#damage(int, Object)}，即不经过物理 DR 管线，
 * 因此硬壳 / 高护甲敌人也吃得实实在在），并把它朝<strong>随机方向</strong>
 * 推开 {@link #knockbackPower()} 格。
 * <p>
 * 英雄或盟友踩上去不会触发（见 {@link Mine#isEnemy(Char)}）。
 */
public class ContactMine extends Mine {

    @Override
    public Class<? extends CellEntitySprite> spriteClass() {
        return ContactMineSprite.class;
    }

    /** 单体伤害。 */
    public int damage() {
        return 20;
    }

    /** 击退格数。 */
    public int knockbackPower() {
        return 3;
    }

    @Override
    public void onStep(Char who) {
        if (isEnemy(who)) {
            detonate();
        }
    }

    @Override
    protected void onDetonate() {
        if (Dungeon.level == null) {
            return;
        }

        Char victim = Actor.findChar(pos);
        if (victim == null || !victim.isAlive()) {
            return;
        }

        int dmg = Math.max(0, damage());
        if (dmg > 0) {
            victim.damage(dmg, this);
        }
        if (victim.sprite != null) {
            victim.sprite.showStatus(CharSprite.NEGATIVE,
                    Messages.get(this, "status", dmg));
        }

        // 死于爆炸直接就地结算，不再推挤。
        if (!victim.isAlive()) {
            return;
        }
        if (victim.properties().contains(Char.Property.IMMOVABLE)) {
            return;
        }

        // 随机方向推开（8 邻向量中随机挑一个作为推挤航向）。
        int dir = PathFinder.NEIGHBOURS8[Random.Int(PathFinder.NEIGHBOURS8.length)];
        int aim = victim.pos + dir;
        Ballistica trajectory = new Ballistica(victim.pos, aim, Ballistica.MAGIC_BOLT);
        WandOfBlastWave.throwChar(victim, trajectory, knockbackPower(), false, true, this);
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", damage(), knockbackPower());
    }
}
