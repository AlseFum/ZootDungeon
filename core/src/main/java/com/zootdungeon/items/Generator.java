package com.zootdungeon.items;

import com.zootdungeon.Dungeon;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.artifacts.Artifact;
import com.zootdungeon.items.food.Food;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.items.potions.exotic.ExoticPotion;
import com.zootdungeon.items.potions.Potion;
import com.zootdungeon.items.rings.Ring;
import com.zootdungeon.items.scrolls.exotic.ExoticScroll;
import com.zootdungeon.items.scrolls.Scroll;
import com.zootdungeon.items.stones.Runestone;
import com.zootdungeon.items.trinkets.Trinket;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.plants.Plant;
// import com.zootdungeon.utils.EventBus; // EventBus removed - TODO: restore when needed
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Supplier;


public class Generator {
	
	public enum Category {
		WEAPON	( 2, 2, MeleeWeapon.class),
		WEP_T1	( 0, 0, MeleeWeapon.class),
		WEP_T2	( 0, 0, MeleeWeapon.class),
		WEP_T3	( 0, 0, MeleeWeapon.class),
		WEP_T4	( 0, 0, MeleeWeapon.class),
		WEP_T5	( 0, 0, MeleeWeapon.class),

		ARMOR	( 2, 1, Armor.class),
		ARM_T1	( 0, 0, Armor.class),
		ARM_T2	( 0, 0, Armor.class),
		ARM_T3	( 0, 0, Armor.class),
		ARM_T4	( 0, 0, Armor.class),
		ARM_T5	( 0, 0, Armor.class),

		MISSILE ( 1, 2, MissileWeapon.class),
		MIS_T1  ( 0, 0, MissileWeapon.class),
		MIS_T2  ( 0, 0, MissileWeapon.class),
		MIS_T3  ( 0, 0, MissileWeapon.class),
		MIS_T4  ( 0, 0, MissileWeapon.class),
		MIS_T5  ( 0, 0, MissileWeapon.class),

		WAND	( 1, 1, Wand.class),
		RING	( 1, 0, Ring.class),
		ARTIFACT( 0, 1, Artifact.class),

		FOOD	( 0, 0, Food.class),

		POTION	( 8, 8, Potion.class),
		SEED	( 1, 1, Plant.Seed.class),

		SCROLL	( 8, 8, Scroll.class),
		STONE   ( 1, 1, Runestone.class),

		GOLD	( 10, 10, Gold.class),

		// Add missing categories
		TRINKET ( 1, 1, Trinket.class),
		CUSTOM_ITEM ( 0, 0, Item.class),
		CUSTOM_FOOD ( 0, 0, Food.class);

		// LootRegistry CategoryPool 引用（延迟解析）
		private LootRegistry.CategoryPool pool;

		public Class<?>[] classes;

		//some item types use a deck-based system, where the probs decrement as items are picked
		// until they are all 0, and then they reset. Those generator classes should define
		// defaultProbs. If defaultProbs is null then a deck system isn't used.
		//Artifacts in particular don't reset, no duplicates!
		public float[] probs;
		public float[] defaultProbs = null;

		//some items types have two decks and swap between them
		// this enforces more consistency while still allowing for better precision
		public float[] defaultProbs2 = null;
		public boolean using2ndProbs = false;
		//but in such cases we still need a reference to the full deck in case of non-deck generation
		public float[] defaultProbsTotal = null;

		//These variables are used as a part of the deck system, to ensure that drops are consistent
		// regardless of when they occur (either as part of seeded levelgen, or random item drops)
		public Long seed = null;
		public int dropped = 0;

		//game has two decks of 35 items for overall category probs
		//one deck has a ring and extra armor, the other has an artifact and extra thrown weapon
		//Note that pure random drops only happen as part of levelgen atm, so no seed is needed here
		public float firstProb;
		public float secondProb;
		public Class<? extends Item> superClass;
		public Supplier<Item> make; // 新增：物品生成函数


		private Category(float firstProb, float secondProb, Class<? extends Item> superClass) {
			this.firstProb = firstProb;
			this.secondProb = secondProb;
			this.superClass = superClass;
			this.make = null;
			// Initialize empty arrays that will be populated by registerItem
			this.classes = new Class<?>[0];
			this.probs = new float[0];
		}

		/**
		 * 获取对应的 LootRegistry.CategoryPool
		 */
		public LootRegistry.CategoryPool pool() {
			if (pool == null) {
				// 延迟获取，从 LootRegistry 查找同名 pool
				String key = name();
				pool = LootRegistry.CATEGORIES.get(key);
				if (pool == null) {
					// 如果 LootRegistry 中没有，创建一个空的（用于自定义类别）
					pool = LootRegistry.category(key, superClass, make);
				}
			}
			return pool;
		}

		//some generator categories can have ordering within that category as well
		// note that sub category ordering doesn't need to always include items that belong
		// to that categories superclass, e.g. bombs are ordered within thrown weapons
		private static HashMap<Class, ArrayList<Class>> subOrderings = new HashMap<>();
		static {
			subOrderings.put(MissileWeapon.class, new ArrayList<>(Arrays.asList(MissileWeapon.class)));
			subOrderings.put(Potion.class, new ArrayList<>(Arrays.asList(Waterskin.class, Potion.class, ExoticPotion.class)));
			subOrderings.put(Scroll.class, new ArrayList<>(Arrays.asList(Scroll.class, ExoticScroll.class)));
		}

		//in case there are multiple matches, this will return the latest match
		public static int order( Item item ) {
			int catResult = -1, subResult = 0;
			for (int i=0; i < values().length; i++) {
				ArrayList<Class> subOrdering = subOrderings.get(values()[i].superClass);
				if (subOrdering != null){
					for (int j=0; j < subOrdering.size(); j++){
						if (subOrdering.get(j).isInstance(item)){
							catResult = i;
							subResult = j;
						}
					}
				} else {
					if (values()[i].superClass.isInstance(item)) {
						catResult = i;
						subResult = 0;
					}
				}
			}
			if (catResult != -1) return catResult*100 + subResult;

			//items without a category-defined order are sorted based on the spritesheet
			return Short.MAX_VALUE+item.image();
		}
	}

	private static final float[][] floorSetTierProbs = new float[][] {
			{0, 75, 20,  4,  1},
			{0, 25, 50, 20,  5},
			{0,  0, 40, 50, 10},
			{0,  0, 20, 40, 40},
			{0,  0,  0, 20, 80}
	};

	private static boolean usingFirstDeck = false;
	private static HashMap<Category,Float> defaultCatProbs = new LinkedHashMap<>();
	private static HashMap<Category,Float> categoryProbs = new LinkedHashMap<>();

	public static void fullReset() {
		usingFirstDeck = Random.Int(2) == 0;
		generalReset();

		for (Category cat : Category.values()) {
			cat.using2ndProbs =  cat.defaultProbs2 != null && Random.Int(2) == 0;
			reset(cat);
			if (cat.defaultProbs != null) {
				cat.seed = Random.Long();
				cat.dropped = 0;
			}
		}
	}

	public static void generalReset(){
		for (Category cat : Category.values()) {
			categoryProbs.put( cat, usingFirstDeck ? cat.firstProb : cat.secondProb );
			defaultCatProbs.put( cat, cat.firstProb + cat.secondProb );
		}
	}

	public static void reset(Category cat){
		if (cat.defaultProbs != null) {
			if (cat.defaultProbs2 != null){
				cat.using2ndProbs = !cat.using2ndProbs;
				cat.probs = cat.using2ndProbs ? cat.defaultProbs2.clone() : cat.defaultProbs.clone();
			} else {
				cat.probs = cat.defaultProbs.clone();
			}
		}
	}

	//reverts changes to drop chances generates by this item
	//equivalent of shuffling the card back into the deck, does not preserve order!
	public static void undoDrop(Item item){
		undoDrop(item.getClass());
	}

	public static void undoDrop(Class cls){
		for (Category cat : Category.values()){
			if (cls.isAssignableFrom(cat.superClass)){
				if (cat.defaultProbs == null) continue;
				for (int i = 0; i < cat.classes.length; i++){
					if (cls == cat.classes[i]){
						cat.probs[i]++;
					}
				}
			}
		}
	}
	
	public static Item random() {
		Category cat = Random.chances( categoryProbs );
		if (cat == null){
			usingFirstDeck = !usingFirstDeck;
			generalReset();
			cat = Random.chances( categoryProbs );
		}
		categoryProbs.put( cat, categoryProbs.get( cat ) - 1);

		if (cat == Category.SEED) {
			//We specifically use defaults for seeds here because, unlike other item categories
			// their predominant source of drops is grass, not levelgen. This way the majority
			// of seed drops still use a deck, but the few that are spawned by levelgen are consistent
			return randomUsingDefaults(cat);
		} else {
			return random(cat);
		}
	}

	public static Item randomUsingDefaults(){
		return randomUsingDefaults(Random.chances( defaultCatProbs ));
	}
	
	public static Item random( Category cat ) {
		switch (cat) {
			case ARMOR:
				return randomArmor();
			case WEAPON:
				return randomWeapon();
			case MISSILE:
				return randomMissile();
			case ARTIFACT:
				Item item = randomArtifact();
				return item != null ? item : random(Category.RING);
			case TRINKET:
				return LootRegistry.random(cat);
			default:
				// 委托到 LootRegistry.CategoryPool
				Item result = LootRegistry.random(cat);
				if (result == null) {
					reset(cat);
					result = LootRegistry.randomUsingDefaults(cat);
				}
				return result != null ? result : new Gold().random();
		}
	}

	//overrides any deck systems and always uses default probs
	public static Item randomUsingDefaults( Category cat ){
		if (cat == Category.WEAPON){
			return randomWeapon(true);
		} else if (cat == Category.MISSILE){
			return randomMissile(true);
		} else if (cat == Category.ARTIFACT) {
			return randomArtifact();
		} else if (cat == Category.ARMOR) {
			return randomArmor(true);
		}
		// 委托到 LootRegistry
		Item result = LootRegistry.randomUsingDefaults(cat);
		return result != null ? result : new Gold().random();
	}
	
	public static Item random( Class<? extends Item> cl ) {
		return Reflection.newInstance(cl).random();
	}


	public static final Category[] wepTiers = new Category[]{
			Category.WEP_T1,
			Category.WEP_T2,
			Category.WEP_T3,
			Category.WEP_T4,
			Category.WEP_T5
	};

	public static MeleeWeapon randomWeapon(){
		return randomWeapon(Dungeon.depth / 5);
	}

	public static MeleeWeapon randomWeapon(int floorSet) {
		return randomWeapon(floorSet, false);
	}

	public static MeleeWeapon randomWeapon(boolean useDefaults) {
		return randomWeapon(Dungeon.depth / 5, useDefaults);
	}
	
	public static MeleeWeapon randomWeapon(int floorSet, boolean useDefaults) {

		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		MeleeWeapon w;
		if (useDefaults){
			w = (MeleeWeapon) randomUsingDefaults(wepTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			w = (MeleeWeapon) random(wepTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return w;
	}
	
	public static final Category[] misTiers = new Category[]{
			Category.MIS_T1,
			Category.MIS_T2,
			Category.MIS_T3,
			Category.MIS_T4,
			Category.MIS_T5
	};
	public static final Category[] armTiers = new Category[]{
			Category.ARM_T1,
			Category.ARM_T2,
			Category.ARM_T3,
			Category.ARM_T4,
			Category.ARM_T5
		};
	
	public static MissileWeapon randomMissile(){
		return randomMissile(Dungeon.depth / 5);
	}

	public static MissileWeapon randomMissile(int floorSet) {
		return randomMissile(floorSet, false);
	}

	public static MissileWeapon randomMissile(boolean useDefaults) {
		return randomMissile(Dungeon.depth / 5, useDefaults);
	}

	public static MissileWeapon randomMissile(int floorSet, boolean useDefaults) {
		
		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		MissileWeapon w;
		if (useDefaults){
			w = (MissileWeapon)randomUsingDefaults(misTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			w = (MissileWeapon)random(misTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return w;
	}

	//enforces uniqueness of artifacts throughout a run.

	public static Armor randomArmor(){
		return randomArmor(Dungeon.depth / 5);
	}

	public static Armor randomArmor(int floorSet) {
		return randomArmor(floorSet, false);
	}

	public static Armor randomArmor(boolean useDefaults) {
		return randomArmor(Dungeon.depth / 5, useDefaults);
	}

	public static Armor randomArmor(int floorSet, boolean useDefaults) {
		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);
		Armor a;
		if (useDefaults){
			a = (Armor)randomUsingDefaults(armTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			a = (Armor)random(armTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return a;
	}
	public static Artifact randomArtifact() {
		Item item = LootRegistry.random(Category.ARTIFACT);
		return item instanceof Artifact ? (Artifact) item : null;
	}

	public static boolean removeArtifact(Class<?extends Artifact> artifact) {
		return LootRegistry.resetArtifact(artifact);
	}
	private static final String FIRST_DECK = "first_deck";
	private static final String GENERAL_PROBS = "general_probs";
	private static final String CATEGORY_PROBS = "_probs";
	private static final String CATEGORY_USING_PROBS2 = "_using_probs2";
	private static final String CATEGORY_SEED = "_seed";
	private static final String CATEGORY_DROPPED = "_dropped";

	public static void storeInBundle(Bundle bundle) {
		bundle.put(FIRST_DECK, usingFirstDeck);

		Float[] genProbs = categoryProbs.values().toArray(new Float[0]);
		float[] storeProbs = new float[genProbs.length];
		for (int i = 0; i < storeProbs.length; i++){
			storeProbs[i] = genProbs[i];
		}
		bundle.put( GENERAL_PROBS, storeProbs);

		for (Category cat : Category.values()){
			if (cat.defaultProbs == null) continue;

			bundle.put(cat.name().toLowerCase() + CATEGORY_PROBS, cat.probs);

			if (cat.defaultProbs2 != null){
				bundle.put(cat.name().toLowerCase() + CATEGORY_USING_PROBS2, cat.using2ndProbs);
			}

			if (cat.seed != null) {
				bundle.put(cat.name().toLowerCase() + CATEGORY_SEED, cat.seed);
				bundle.put(cat.name().toLowerCase() + CATEGORY_DROPPED, cat.dropped);
			}
		}
	}

	public static void restoreFromBundle(Bundle bundle) {
		fullReset();

		usingFirstDeck = bundle.getBoolean(FIRST_DECK);

		//restore category probs
		for (Category cat : Category.values()){
			if (bundle.contains(cat.name().toLowerCase() + CATEGORY_PROBS)){
				cat.probs = bundle.getFloatArray(cat.name().toLowerCase() + CATEGORY_PROBS);
			}
			
			if (bundle.contains(cat.name().toLowerCase() + CATEGORY_USING_PROBS2)){
				cat.using2ndProbs = bundle.getBoolean(cat.name().toLowerCase() + CATEGORY_USING_PROBS2);
			}
			
			if (bundle.contains(cat.name().toLowerCase() + CATEGORY_SEED)){
				cat.seed = bundle.getLong(cat.name().toLowerCase() + CATEGORY_SEED);
			}
			
			if (bundle.contains(cat.name().toLowerCase() + CATEGORY_DROPPED)){
				cat.dropped = bundle.getInt(cat.name().toLowerCase() + CATEGORY_DROPPED);
			}
		}
		
		//restore general category probs
		if (bundle.contains(GENERAL_PROBS)){
			float[] probs = bundle.getFloatArray(GENERAL_PROBS);
			if (probs.length == Category.values().length) {
				for (int i = 0; i < probs.length; i++) {
					categoryProbs.put(Category.values()[i], probs[i]);
				}
			}
		}
	}
	
	/**
	 * Registers a new item to a specific category at runtime.
	 * This allows for adding custom items to the generator system.
	 * 
	 * @param category the category to add the item to
	 * @param itemClass the class of the item to add
	 * @param probability the probability of the item appearing (relative to other items in the category)
	 * @return true if the item was successfully registered, false otherwise
	 */
	public static boolean registerItem(Category category, Class<? extends Item> itemClass, float probability) {
		return registerItem(category, itemClass, probability, 0);
	}




	/**
	 * 注册一个物品到生成系统
	 * @param category 物品类别
	 * @param itemClass 物品类型
	 * @param probability1 第一套概率
	 * @param probability2 第二套概率（如果类别使用双概率系统）
	 * @return 是否成功注册
	 */
	public static boolean registerItem(Category category, Class<? extends Item> itemClass, float probability1, float probability2) {
		if (category == null || itemClass == null || probability1 < 0 || probability2 < 0) {
			return false;
		}

		// 同步到 LootRegistry.CategoryPool（主要数据源）
		category.pool().register(itemClass, probability1, probability2);

		// 同步到 Generator.Category（兼容层）
		category.make = () -> {
			try {
				return (Item)Reflection.newInstance(itemClass).random();
			} catch (Exception e) {
				return new Gold().random();
			}
		};

		// 检查物品是否已经存在
		for (int i = 0; i < category.classes.length; i++) {
			if (category.classes[i].equals(itemClass)) {
				category.probs[i] = probability1;
				if (category.defaultProbs != null) {
					category.defaultProbs[i] = probability1;
				}
				if (category.defaultProbs2 != null) {
					category.defaultProbs2[i] = probability2;
					if (category.defaultProbsTotal != null) {
						category.defaultProbsTotal[i] = probability1 + probability2;
					}
				}
				return true;
			}
		}

		// 创建新数组
		Class<?>[] newClasses = new Class<?>[category.classes.length + 1];
		float[] newProbs = new float[category.probs.length + 1];
		System.arraycopy(category.classes, 0, newClasses, 0, category.classes.length);
		System.arraycopy(category.probs, 0, newProbs, 0, category.probs.length);
		newClasses[newClasses.length - 1] = itemClass;
		newProbs[newProbs.length - 1] = probability1;
		category.classes = newClasses;
		category.probs = newProbs;

		if (category.defaultProbs == null) {
			category.defaultProbs = new float[newProbs.length];
			System.arraycopy(newProbs, 0, category.defaultProbs, 0, newProbs.length);
		} else {
			float[] newDefaultProbs = new float[category.defaultProbs.length + 1];
			System.arraycopy(category.defaultProbs, 0, newDefaultProbs, 0, category.defaultProbs.length);
			newDefaultProbs[newDefaultProbs.length - 1] = probability1;
			category.defaultProbs = newDefaultProbs;
		}

		if (category.defaultProbs2 != null || probability2 > 0) {
			if (category.defaultProbs2 == null) {
				category.defaultProbs2 = new float[newProbs.length];
				System.arraycopy(category.defaultProbs, 0, category.defaultProbs2, 0, category.defaultProbs.length - 1);
				category.defaultProbs2[category.defaultProbs2.length - 1] = probability2;
			} else {
				float[] newDefaultProbs2 = new float[category.defaultProbs2.length + 1];
				System.arraycopy(category.defaultProbs2, 0, newDefaultProbs2, 0, category.defaultProbs2.length);
				newDefaultProbs2[newDefaultProbs2.length - 1] = probability2;
				category.defaultProbs2 = newDefaultProbs2;
			}

			if (category.defaultProbsTotal == null) {
				category.defaultProbsTotal = new float[newProbs.length];
			} else {
				float[] newTotalProbs = new float[category.defaultProbsTotal.length + 1];
				System.arraycopy(category.defaultProbsTotal, 0, newTotalProbs, 0, category.defaultProbsTotal.length);
				category.defaultProbsTotal = newTotalProbs;
			}
			category.defaultProbsTotal[category.defaultProbsTotal.length - 1] = probability1 + probability2;
		}
		return true;
	}
}
