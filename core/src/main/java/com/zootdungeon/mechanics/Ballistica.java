package com.zootdungeon.mechanics;

import com.zootdungeon.Dungeon;
import com.zootdungeon.ColaDungeon;
import com.zootdungeon.actors.Actor;

import java.util.ArrayList;
import java.util.List;

public class Ballistica {

	public ArrayList<Integer> path = new ArrayList<>();
	public Integer sourcePos = null;
	public Integer collisionPos = null;
	public Integer collisionProperties = null;
	public Integer dist = 0;

	public static final int STOP_TARGET = 1;
	public static final int STOP_CHARS = 2;
	public static final int STOP_SOLID = 4;
	public static final int IGNORE_SOFT_SOLID = 8;

	public static final int PROJECTILE =  STOP_TARGET | STOP_CHARS | STOP_SOLID;

	public static final int MAGIC_BOLT =  STOP_CHARS | STOP_SOLID;

	public static final int WONT_STOP = 0;

	public Ballistica(int from, int to, int params) {
		sourcePos = from;
		collisionProperties = params;
		build(from, to,
				(params & STOP_TARGET) > 0,
				(params & STOP_CHARS) > 0,
				(params & STOP_SOLID) > 0,
				(params & IGNORE_SOFT_SOLID) > 0);

		if (collisionPos != null) {
			dist = path.indexOf(collisionPos);
		} else if (!path.isEmpty()) {
			collisionPos = path.get(dist = path.size() - 1);
		} else {
			path.add(from);
			collisionPos = from;
			dist = 0;
		}
	}

	private void build(int from, int to, boolean stopTarget, boolean stopChars, boolean stopTerrain, boolean ignoreSoftSolid) {
		int w = Dungeon.level.width();
		int mapSize = w * Dungeon.level.height();

		path = Geometry.bresenhamPath(from, to, w, mapSize);

		for (int cell : path) {
			if (collisionPos != null) break;

			if (stopTerrain
					&& cell != sourcePos
					&& !Dungeon.level.passable[cell]
					&& !Dungeon.level.avoid[cell]
					&& Actor.findChar(cell) == null) {
				collide(path.get(path.indexOf(cell) - 1));
				break;
			}

			if (stopTerrain && cell != sourcePos && Dungeon.level.solid[cell]) {
				if (ignoreSoftSolid && (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])) {
				} else {
					collide(cell);
				}
			}
			if (cell != sourcePos && stopChars && Actor.findChar(cell) != null) {
				collide(cell);
			}
			if (cell == to && stopTarget) {
				collide(cell);
			}
		}
	}

	private void collide(int cell) {
		if (collisionPos == null) {
			collisionPos = cell;
		}
	}

	public List<Integer> subPath(int start, int end) {
		try {
			end = Math.min(end, path.size() - 1);
			return path.subList(start, end + 1);
		} catch (Exception e) {
			ColaDungeon.reportException(e);
			return new ArrayList<>();
		}
	}

	public static Ballistica of(int from, int to, int params) {
		return new Ballistica(from, to, params);
	}
}
