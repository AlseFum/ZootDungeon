package com.zootdungeon.levels.entities;

/**
 * {@link DebugCellMarker} 的调试贴图。直接复用 {@code cola/trashbin.png}（16x16），
 * 便于在地图上快速定位被放置的调试实体。
 */
public class DebugCellMarkerSprite extends CellEntitySprite {

    public DebugCellMarkerSprite() {
        super();
        texture("cola/trashbin.png");
    }
}
