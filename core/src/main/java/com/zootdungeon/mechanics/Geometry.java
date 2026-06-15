package com.zootdungeon.mechanics;

import com.watabou.utils.GameMath;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Stateless helpers for grid and visibility geometry.
 */
public final class Geometry {

	private Geometry() {
	}

	/**
	 * Builds the full Bresenham-style grid path from {@code from} toward {@code to},
	 * continuing until the stepped line exits the map bounds.
	 */
	public static ArrayList<Integer> bresenhamPath(int from, int to, int width, int mapSize) {
		ArrayList<Integer> path = new ArrayList<>();

		int x0 = from % width;
		int x1 = to % width;
		int y0 = from / width;
		int y1 = to / width;

		int dx = x1 - x0;
		int dy = y1 - y0;

		int stepX = dx > 0 ? +1 : -1;
		int stepY = dy > 0 ? +1 : -1;

		dx = Math.abs(dx);
		dy = Math.abs(dy);

		int stepA;
		int stepB;
		int dA;
		int dB;

		if (dx > dy) {
			stepA = stepX;
			stepB = stepY * width;
			dA = dx;
			dB = dy;
		} else {
			stepA = stepY * width;
			stepB = stepX;
			dA = dy;
			dB = dx;
		}

		int cell = from;
		int err = dA / 2;
		while (insideMap(cell, mapSize)) {
			path.add(cell);
			cell += stepA;
			err += dB;
			if (err >= dA) {
				err -= dA;
				cell += stepB;
			}
		}

		return path;
	}

	/**
	 * Returns an inclusive slice of a cell path. Invalid ranges return an empty list.
	 */
	public static List<Integer> subPath(List<Integer> path, int start, int end) {
		if (path == null || path.isEmpty() || start < 0 || start >= path.size()) {
			return new ArrayList<>();
		}
		int clampedEnd = Math.min(end, path.size() - 1);
		if (clampedEnd < start) {
			return new ArrayList<>();
		}
		return path.subList(start, clampedEnd + 1);
	}

	/**
	 * Converts a cell index into its center point in world-grid coordinates.
	 */
	public static PointF cellCenter(int cell, int width) {
		return new PointF(cell % width + 0.5f, cell / width + 0.5f);
	}

	/**
	 * Floors a world-grid position into a map point and clamps it to the map bounds.
	 */
	public static Point pointFromWorld(float x, float y, int width, int height) {
		return new Point(
				(int) GameMath.gate(0, (int) Math.floor(x), width - 1),
				(int) GameMath.gate(0, (int) Math.floor(y), height - 1)
		);
	}

	/**
	 * Converts an integer grid point into a cell index.
	 */
	public static int pointToCell(Point point, int width) {
		return point.x + point.y * width;
	}

	/**
	 * Samples all unique target cells needed to approximate a cone area with rays.
	 * Includes an inner ring for wider cones to reduce gaps between sampled rays.
	 */
	public static LinkedHashSet<Integer> coneTargetCells(int from, int to, int width, int height, float maxDist, float degrees) {
		PointF fromP = cellCenter(from, width);
		PointF toP = cellCenter(to, width);

		float distance = PointF.distance(fromP, toP);
		if (distance > maxDist) {
			toP = PointF.inter(fromP, toP, maxDist / distance);
			distance = maxDist;
		}

		float circleRadius = distance + 0.5f;
		float initialAngle = PointF.angle(fromP, toP) / PointF.G2R;

		LinkedHashSet<Integer> targetCells = new LinkedHashSet<>();
		PointF scan = new PointF();

		for (float angle = initialAngle + degrees / 2f; angle >= initialAngle - degrees / 2f; angle -= 0.5f) {
			addConeTargetCell(targetCells, scan, fromP, angle, circleRadius, width, height);
			if (circleRadius >= 4) {
				addConeTargetCell(targetCells, scan, fromP, angle, circleRadius - 1, width, height);
			}
		}

		return targetCells;
	}

	/**
	 * Samples the cells that lie along the cone's outer arc, preserving clockwise order.
	 */
	public static LinkedHashSet<Integer> coneOuterCells(int from, int to, int width, int height, float maxDist, float degrees) {
		PointF fromP = cellCenter(from, width);
		PointF toP = cellCenter(to, width);

		float distance = PointF.distance(fromP, toP);
		if (distance > maxDist) {
			toP = PointF.inter(fromP, toP, maxDist / distance);
			distance = maxDist;
		}

		float circleRadius = distance + 0.5f;
		float initialAngle = PointF.angle(fromP, toP) / PointF.G2R;

		LinkedHashSet<Integer> outerCells = new LinkedHashSet<>();
		PointF scan = new PointF();

		for (float angle = initialAngle + degrees / 2f; angle >= initialAngle - degrees / 2f; angle -= 0.5f) {
			addConeTargetCell(outerCells, scan, fromP, angle, circleRadius, width, height);
		}

		return outerCells;
	}

	/**
	 * Precomputes the per-distance horizontal reach used to keep recursive shadowcasting circular.
	 */
	public static int[][] buildShadowRounding(int maxDistance) {
		int[][] rounding = new int[maxDistance + 1][];
		for (int i = 1; i <= maxDistance; i++) {
			rounding[i] = new int[i + 1];
			for (int j = 1; j <= i; j++) {
				rounding[i][j] = (int) Math.min(
						j,
						Math.round(i * Math.cos(Math.asin(j / (i + 0.5)))));
			}
		}
		return rounding;
	}

	/**
	 * Returns the rounding row for a given FOV distance, including the distance-2 corner fill tweak.
	 */
	public static int[] roundingAtDistance(int[][] rounding, int distance) {
		if (distance == 2) {
			int[] copy = rounding[distance].clone();
			copy[2] = 2;
			return copy;
		}
		return rounding[distance];
	}

	/**
	 * Computes the first scanned column of an octant row from its left slope.
	 */
	public static int startColumn(int row, double leftSlope) {
		return leftSlope == 0 ? 0 : (int) Math.floor((row - 0.5) * leftSlope + 0.499);
	}

	/**
	 * Computes the last scanned column of an octant row from its right slope.
	 */
	public static int endColumn(int row, double rightSlope, int maxColumn) {
		return rightSlope == 1 ? maxColumn : Math.min(maxColumn,
				(int) Math.ceil((row + 0.5) * rightSlope - 0.499));
	}

	/**
	 * Detects the shadowcasting edge case where the final cell should be skipped due to slope rounding.
	 */
	public static boolean isEndBlockedBySlope(int row, int end, boolean inBlocking, double rightSlope) {
		return inBlocking && (int) Math.ceil((row - 0.5) * rightSlope - 0.499) != end;
	}

	private static boolean insideMap(int cell, int mapSize) {
		return cell >= 0 && cell < mapSize;
	}

	private static void addConeTargetCell(LinkedHashSet<Integer> cells, PointF scan, PointF fromP,
									  float angle, float radius, int width, int height) {
		scan.polar(angle * PointF.G2R, radius);
		scan.offset(fromP);
		scan.x += (fromP.x > scan.x ? +0.5f : -0.5f);
		scan.y += (fromP.y > scan.y ? +0.5f : -0.5f);
		Point point = pointFromWorld(scan.x, scan.y, width, height);
		cells.add(pointToCell(point, width));
	}
}
