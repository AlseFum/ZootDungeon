package com.zootdungeon.mechanics;

import com.zootdungeon.Dungeon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class ConeAOE {

	public Ballistica coreRay;

	public ArrayList<Ballistica> outerRays = new ArrayList<>();
	public ArrayList<Ballistica> rays = new ArrayList<>();
	public HashSet<Integer> cells = new HashSet<>();

	public ConeAOE(Ballistica core, float degrees) {
		this(core, Float.POSITIVE_INFINITY, degrees, core.collisionProperties);
	}

	public ConeAOE(Ballistica core, float maxDist, float degrees, int ballisticaParams) {

		coreRay = core;

		int w = Dungeon.level.width();
		int h = Dungeon.level.height();

		LinkedHashSet<Integer> targetCells = Geometry.coneTargetCells(
				core.sourcePos, core.collisionPos, w, h, maxDist, degrees);
		LinkedHashSet<Integer> outerCells = Geometry.coneOuterCells(
				core.sourcePos, core.collisionPos, w, h, maxDist, degrees);

		for (int c : targetCells) {
			Ballistica ray = new Ballistica(core.sourcePos, c, ballisticaParams);
			cells.addAll(ray.subPath(1, ray.dist));
			rays.add(ray);
			if (outerCells.contains(c)) {
				outerRays.add(ray);
			}
		}

	}

}
