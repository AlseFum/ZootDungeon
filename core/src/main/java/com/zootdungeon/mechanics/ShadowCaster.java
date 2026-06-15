package com.zootdungeon.mechanics;

import com.zootdungeon.ColaDungeon;
import com.watabou.utils.BArray;

public final class ShadowCaster {

	public static final int MAX_DISTANCE = 20;

	public static int[][] rounding = Geometry.buildShadowRounding(MAX_DISTANCE);

	public static void castShadow(int x, int y, int w, boolean[] fieldOfView, boolean[] blocking, int distance) {

		if (distance >= MAX_DISTANCE) {
			distance = MAX_DISTANCE;
		}

		BArray.setFalse(fieldOfView);

		int src = y * w + x;
		if (src >= 0 && src < fieldOfView.length) {
			fieldOfView[src] = true;
		}

		try {
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, -1, false);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, +1, true);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, +1, true);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, +1, false);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, +1, false);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, -1, true);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, -1, true);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, -1, false);
		} catch (Exception e) {
			ColaDungeon.reportException(e);
			BArray.setFalse(fieldOfView);
		}

	}

	private static void scanOctant(int distance, boolean[] fov, boolean[] blocking, int row,
								   int x, int y, int w, double lSlope, double rSlope,
								   int mX, int mY, boolean mXY) {

		boolean inBlocking = false;
		int start, end;

		int[] roundingAtDist = Geometry.roundingAtDistance(rounding, distance);

		for (; row <= distance; row++) {

			if (rSlope < lSlope) return;

			start = Geometry.startColumn(row, lSlope);
			end = Geometry.endColumn(row, rSlope, roundingAtDist[row]);

			int cell = x + y * w;

			if (mXY)    cell += mX * start * w + mY * row;
			else        cell += mX * start + mY * row * w;

			for (int col = start; col <= end; col++) {

				if (Geometry.isEndBlockedBySlope(row, end, inBlocking, rSlope)) {
					break;
				}

				if (cell < 0 || cell >= fov.length || cell >= blocking.length) {
					if (!mXY)   cell += mX;
					else        cell += mX * w;
					continue;
				}

				fov[cell] = true;

				if (blocking[cell]) {
					if (!inBlocking) {
						inBlocking = true;
						if (col != start) {
							scanOctant(distance, fov, blocking, row + 1, x, y, w, lSlope,
									(col - 0.5) / (row + 0.5),
									mX, mY, mXY);
						}
					}

				} else {
					if (inBlocking) {
						inBlocking = false;
						lSlope = (col - 0.5) / (row - 0.5);
					}
				}

				if (!mXY)   cell += mX;
				else        cell += mX * w;

			}

			if (inBlocking) return;
		}
	}
}
