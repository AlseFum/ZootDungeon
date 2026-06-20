package com.zootdungeon.actors.blobs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.effects.BlobEmitter;
import com.zootdungeon.effects.particles.CrippleBlobParticle;
import com.zootdungeon.messages.Messages;

/** Temporary shadow miasma from MISERY CrippleBlob talent. Fades over time, uses falling particles. */
public class CrippleBlob extends Blob {

	{
		actPriority = HERO_PRIO;
	}

	@Override
	protected void evolve() {
		int cell;
		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {
					off[cell] = cur[cell] - 1; // fade by 1 per turn
					volume += off[cell];
				} else {
					off[cell] = 0;
				}
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.pour(CrippleBlobParticle.FACTORY, 0.15f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	/** Check if a cell has cripple miasma (used alongside MiseryShadowBlob for shadow checks). */
	public static boolean isCrippleCell(int cell) {
		if (Dungeon.level == null) return false;
		CrippleBlob blob = (CrippleBlob) Dungeon.level.blobs.get(CrippleBlob.class);
		return blob != null && blob.cur != null && blob.cur[cell] > 0;
	}
}
