package com.zootdungeon.event;

//here, is some atomic events need to be dispatched and triggered
import com.zootdungeon.Assets;
import com.zootdungeon.Badges;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.particles.BlastParticle;
import com.zootdungeon.effects.particles.SmokeParticle;
import com.zootdungeon.items.Heap;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.mechanics.Damage;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zootdungeon.items.bombs.Bomb.ConjuredBomb;
import java.util.ArrayList;

public class misc{
    public static void explode(int cell,boolean isDestructive,int explosionRange,Object source){
		//We're blowing up, so no need for a fuse anymore.
		//

		Sample.INSTANCE.play( Assets.Sounds.BLAST );

		if (isDestructive) {

			ArrayList<Integer> affectedCells = new ArrayList<>();
			ArrayList<Char> affectedChars = new ArrayList<>();
			
			if (Dungeon.level.heroFOV[cell]) {
				CellEmitter.center(cell).burst(BlastParticle.FACTORY, 30);
			}
			
			boolean terrainAffected = false;
			boolean[] explodable = new boolean[Dungeon.level.length()];
			BArray.not( Dungeon.level.solid, explodable);
			BArray.or( Dungeon.level.flamable, explodable, explodable);
			PathFinder.buildDistanceMap( cell, explodable, explosionRange );
			for (int i = 0; i < PathFinder.distance.length; i++) {
				if (PathFinder.distance[i] != Integer.MAX_VALUE) {
					affectedCells.add(i);
					Char ch = Actor.findChar(i);
					if (ch != null) {
						affectedChars.add(ch);
					}
				}
			}

			for (int i : affectedCells){
				if (Dungeon.level.heroFOV[i]) {
					CellEmitter.get(i).burst(SmokeParticle.FACTORY, 4);
				}

				if (Dungeon.level.flamable[i]) {
					Dungeon.level.destroy(i);
					GameScene.updateMap(i);
					terrainAffected = true;
				}

				//destroys items / triggers bombs caught in the blast.
				Heap heap = Dungeon.level.heaps.get(i);
				if (heap != null) {
					heap.explode();
				}
			}
			
			for (Char ch : affectedChars){

				//if they have already been killed by another bomb
				if(!ch.isAlive()){
					continue;
				}

				int dmg = Random.NormalIntRange(4 + Dungeon.scalingDepth(), 12 + 3*Dungeon.scalingDepth());
				dmg -= ch.drRoll();

				if (dmg > 0) {
					Damage.environment(ch, Damage.PHYSICAL, dmg, source);
				}
				
				if (ch == Dungeon.hero && !ch.isAlive()) {
					if (source instanceof ConjuredBomb){
						Badges.validateDeathFromFriendlyMagic();
					}
					GLog.n(Messages.get(source, "ondeath"));
					Dungeon.fail(source);
				}
			}
			
			if (terrainAffected) {
				Dungeon.observe();
			}
		}
	}
}