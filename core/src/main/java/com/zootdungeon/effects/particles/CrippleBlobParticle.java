package com.zootdungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;

/** Heavy falling dark-purple particles for MISERY CrippleBlob impact. */
public class CrippleBlobParticle extends PixelParticle.Shrinking {

	public static final Factory FACTORY = new Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((CrippleBlobParticle) emitter.recycle(CrippleBlobParticle.class)).reset(x, y);
		}
	};

	public CrippleBlobParticle() {
		super();
		acc.set(0, +60); // fall downward
	}

	public void reset(float x, float y) {
		revive();

		this.x = x;
		this.y = y;

		left = lifespan = Random.Float(0.4f, 0.8f);
		size = Random.Int(3, 6);
		speed.set(Random.Float(-8, +8), Random.Float(-20, +5));
	}

	@Override
	public void update() {
		super.update();

		float p = left / lifespan;
		color(ColorMath.interpolate(0x440022, 0x110011, p));
		am = p < 0.2f ? p / 0.2f * 0.8f : (1f - p) / 0.8f * 0.8f;
	}
}
