package com.zootdungeon.levels.entities;

import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PointF;
import com.zootdungeon.Dungeon;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.tiles.DungeonTilemap;

/**
 * {@link CellEntity} 的视觉基类。
 * <p>
 * 默认继承 {@link Image}，子类既可以通过
 * {@code copy(spriteSheet.get(region))} 从图集切片，也可以自行
 * {@code texture("assets/...")} 加载独立贴图。
 * <p>
 * 渲染流程：GameScene 在启动时遍历 {@link com.zootdungeon.levels.Level#cellEntities}，
 * 为每个实体调用 {@link Class#newInstance()} 实例化对应 sprite，再调用 {@link #link(CellEntity)}
 * 以关联、{@link #place(int)} 以摆到世界坐标。
 */
public class CellEntitySprite extends Image {

    protected CellEntity entity;

    /** 是否像 item sprite 一样轻微抬高像素（避免与底层地面完全贴合）。子类可覆盖。 */
    protected float perspectiveRaise = 5 / 16f;

    public CellEntitySprite() {
        super();
    }

    /** 绑定数据实体。Sprite 在被 GameScene 管理期间只绑定一个 {@link CellEntity}。 */
    public void link(CellEntity e) {
        this.entity = e;
        place(e.pos);
        reset();
    }

    /** 摆到指定格子的中心。 */
    public void place(int cell) {
        if (Dungeon.level != null) {
            point(worldToCamera(cell));
        }
    }

    /** 计算 cell 中心到世界相机的坐标；与 {@link com.zootdungeon.sprites.ItemSprite#worldToCamera} 一致。 */
    public PointF worldToCamera(int cell) {
        final int csize = DungeonTilemap.SIZE;
        return new PointF(
                PixelScene.align(((cell % Dungeon.level.width()) + 0.5f) * csize - width() * 0.5f),
                PixelScene.align(((cell / Dungeon.level.width()) + 1.0f) * csize - height() - csize * perspectiveRaise));
    }

    /** 被链入场景时做一次视觉初始化（子类可覆盖，如：默认半透明、默认动画等）。 */
    public void reset() {
        alpha(1f);
        active = true;
        visible = true;
    }

    /** 放一个淡入效果：新生成的 CellEntity 通常用这个。 */
    public void fadeIn() {
        alpha(0f);
        parent.add(new AlphaTweener(this, 1f, 0.4f));
    }

    public CellEntity entity() {
        return entity;
    }
}
