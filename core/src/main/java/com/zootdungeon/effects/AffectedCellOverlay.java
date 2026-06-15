/*
 * ColaDungeon - 伤害/效果范围预览覆盖层
 * 在选格时根据当前悬停格子高亮「受影响范围」，便于玩家放置陷阱或释放技能。
 */

package com.zootdungeon.effects;

import com.zootdungeon.Dungeon;
import com.zootdungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

import java.util.Collection;

/**
 * 在选格（CellSelector）时显示「受影响格子」的半透明高亮，用于技能/陷阱范围提示。
 * 由 CellSelector 在悬停时根据 Listener 的 {@link com.zootdungeon.scenes.CellSelector.HasRangePreview#getAffectedCells(int)}
 * 更新显示。
 */
public class AffectedCellOverlay extends Group {

	private static final float ALPHA = 0.4f;
	private static final int DEFAULT_COLOR = 0x44FF4444; // 半透明红

	private Image[] cellImages = new Image[0];

	public AffectedCellOverlay() {}

	/**
	 * 设置要显示的受影响格子，并刷新显示。
	 * @param cells 格子索引集合；null 或空则清空
	 */
	public void setCells(Collection<Integer> cells) {
		setCells(cells, DEFAULT_COLOR);
	}

	/**
	 * 设置要显示的受影响格子及高亮颜色。
	 * @param cellSet 格子索引集合；null 或空则清空
	 * @param color   ARGB，如 0x44FF4444
	 */
	public void setCells(Collection<Integer> cellSet, int color) {
		if (cellSet == null || cellSet.isEmpty()) {
			clear();
			return;
		}
		int n = 0;
		for (Integer pos : cellSet) {
			if (pos != null && pos >= 0 && pos < Dungeon.level.length()) n++;
		}
		if (n == 0) {
			clear();
			return;
		}
		// 复用或扩展 Image 数组
		if (cellImages.length < n) {
			for (Image c : cellImages) if (c != null) c.killAndErase();
			cellImages = new Image[n];
		}
		int i = 0;
		for (Integer pos : cellSet) {
			if (pos == null || pos < 0 || pos >= Dungeon.level.length()) continue;
			Image img = cellImages[i];
			if (img == null || img.parent == null) {
				img = new Image(TextureCache.createSolid(color));
				img.origin.set(0, 0);
				img.scale.set(DungeonTilemap.SIZE, DungeonTilemap.SIZE);
				cellImages[i] = img;
				add(img);
			}
			img.point(DungeonTilemap.tileToWorld(pos));
			img.alpha(ALPHA);
			img.visible = true;
			img.revive();
			i++;
		}
		for (int j = i; j < cellImages.length; j++) {
			if (cellImages[j] != null) {
				cellImages[j].visible = false;
				cellImages[j].killAndErase();
			}
		}
	}

	/** 清空所有高亮 */
	public void clear() {
		for (Image c : cellImages) {
			if (c != null) c.killAndErase();
		}
		cellImages = new Image[0];
	}
}
