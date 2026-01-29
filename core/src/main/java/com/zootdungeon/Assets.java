package com.zootdungeon;

import com.watabou.utils.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assets {
	public static class Effects {
		public static final String EFFECTS      = "effects/effects.png";
		public static final String FIREBALL     = "effects/fireball.png";
		public static final String SPECKS       = "effects/specks.png";
		public static final String SPELL_ICONS  = "effects/spell_icons.png";
		public static final String TEXT_ICONS   = "effects/text_icons.png";
	}
	
	public static class Environment {
		public static final String TERRAIN_FEATURES = "environment/terrain_features.png";

		public static final String VISUAL_GRID  = "environment/visual_grid.png";
		public static final String WALL_BLOCKING= "environment/wall_blocking.png";

		public static final String TILES_SEWERS = "environment/tiles_sewers.png";
		public static final String TILES_PRISON = "environment/tiles_prison.png";
		public static final String TILES_CAVES  = "environment/tiles_caves.png";
		public static final String TILES_CITY   = "environment/tiles_city.png";
		public static final String TILES_HALLS  = "environment/tiles_halls.png";

		public static final String TILES_CAVES_CRYSTAL  = "environment/tiles_caves_crystal.png";
		public static final String TILES_CAVES_GNOLL    = "environment/tiles_caves_gnoll.png";

		public static final String WATER_SEWERS = "environment/water0.png";
		public static final String WATER_PRISON = "environment/water1.png";
		public static final String WATER_CAVES  = "environment/water2.png";
		public static final String WATER_CITY   = "environment/water3.png";
		public static final String WATER_HALLS  = "environment/water4.png";

		public static final String WEAK_FLOOR       = "environment/custom_tiles/weak_floor.png";
		public static final String SEWER_BOSS       = "environment/custom_tiles/sewer_boss.png";
		public static final String PRISON_QUEST     = "environment/custom_tiles/prison_quest.png";
		public static final String PRISON_EXIT      = "environment/custom_tiles/prison_exit.png";
		public static final String CAVES_QUEST      = "environment/custom_tiles/caves_quest.png";
		public static final String CAVES_BOSS       = "environment/custom_tiles/caves_boss.png";
		public static final String CITY_BOSS        = "environment/custom_tiles/city_boss.png";
		public static final String HALLS_SP         = "environment/custom_tiles/halls_special.png";
	}
	
	//TODO include other font assets here? Some are platform specific though...
	public static class Fonts {
		public static final String PIXELFONT= "fonts/pixel_font.png";
	}

	public static class Interfaces {
		public static final String ARCS_BG  = "interfaces/arcs1.png";
		public static final String ARCS_FG  = "interfaces/arcs2.png";

		public static final String BANNERS  = "interfaces/banners.png";
		public static final String BADGES   = "interfaces/badges.png";
		public static final String LOCKED   = "interfaces/locked_badge.png";

		public static final String CHROME   = "interfaces/chrome.png";
		public static final String ICONS    = "interfaces/icons.png";
		public static final String STATUS   = "interfaces/status_pane.png";
		public static final String MENU     = "interfaces/menu_pane.png";
		public static final String MENU_BTN = "interfaces/menu_button.png";
		public static final String TOOLBAR  = "interfaces/toolbar.png";
		public static final String SHADOW   = "interfaces/shadow.png";
		public static final String BOSSHP   = "interfaces/boss_hp.png";

		public static final String SURFACE  = "interfaces/surface.png";

		public static final String BUFFS_SMALL      = "interfaces/buffs.png";
		public static final String BUFFS_LARGE      = "interfaces/large_buffs.png";

		public static final String TALENT_ICONS     = "interfaces/talent_icons.png";
		public static final String TALENT_BUTTON    = "interfaces/talent_button.png";

		public static final String HERO_ICONS       = "interfaces/hero_icons.png";

		public static final String RADIAL_MENU      = "interfaces/radial_menu.png";
	}

	//these points to resource bundles, not raw asset files
	public static class Messages {
		public static final String ACTORS   = "messages/actors/actors";
		public static final String ITEMS    = "messages/items/items";
		public static final String JOURNAL  = "messages/journal/journal";
		public static final String LEVELS   = "messages/levels/levels";
		public static final String MISC     = "messages/misc/misc";
		public static final String PLANTS   = "messages/plants/plants";
		public static final String SCENES   = "messages/scenes/scenes";
		public static final String UI       = "messages/ui/ui";
		public static final String WINDOWS  = "messages/windows/windows";
	}

	public static class Music {
		public static final String THEME_1              = "music/theme_1.ogg";
		public static final String THEME_2              = "music/theme_2.ogg";
		public static final String THEME_FINALE         = "music/theme_finale.ogg";

		public static final String SEWERS_1             = "music/sewers_1.ogg";
		public static final String SEWERS_2             = "music/sewers_2.ogg";
		public static final String SEWERS_3             = "music/sewers_3.ogg";
		public static final String SEWERS_TENSE         = "music/sewers_tense.ogg";
		public static final String SEWERS_BOSS          = "music/sewers_boss.ogg";

		public static final String PRISON_1             = "music/prison_1.ogg";
		public static final String PRISON_2             = "music/prison_2.ogg";
		public static final String PRISON_3             = "music/prison_3.ogg";
		public static final String PRISON_TENSE         = "music/prison_tense.ogg";
		public static final String PRISON_BOSS          = "music/prison_boss.ogg";

		public static final String CAVES_1              = "music/caves_1.ogg";
		public static final String CAVES_2              = "music/caves_2.ogg";
		public static final String CAVES_3              = "music/caves_3.ogg";
		public static final String CAVES_TENSE          = "music/caves_tense.ogg";
		public static final String CAVES_BOSS           = "music/caves_boss.ogg";
		public static final String CAVES_BOSS_FINALE    = "music/caves_boss_finale.ogg";

		public static final String CITY_1               = "music/city_1.ogg";
		public static final String CITY_2               = "music/city_2.ogg";
		public static final String CITY_3               = "music/city_3.ogg";
		public static final String CITY_TENSE           = "music/city_tense.ogg";
		public static final String CITY_BOSS            = "music/city_boss.ogg";
		public static final String CITY_BOSS_FINALE     = "music/city_boss_finale.ogg";

		public static final String HALLS_1              = "music/halls_1.ogg";
		public static final String HALLS_2              = "music/halls_2.ogg";
		public static final String HALLS_3              = "music/halls_3.ogg";
		public static final String HALLS_TENSE          = "music/halls_tense.ogg";
		public static final String HALLS_BOSS           = "music/halls_boss.ogg";
		public static final String HALLS_BOSS_FINALE    = "music/halls_boss_finale.ogg";
	}

	public static class Sounds {
		public static final String CLICK    = "sounds/click.mp3";
		public static final String BADGE    = "sounds/badge.mp3";
		public static final String GOLD     = "sounds/gold.mp3";

		public static final String OPEN     = "sounds/door_open.mp3";
		public static final String UNLOCK   = "sounds/unlock.mp3";
		public static final String ITEM     = "sounds/item.mp3";
		public static final String DEWDROP  = "sounds/dewdrop.mp3";
		public static final String STEP     = "sounds/step.mp3";
		public static final String WATER    = "sounds/water.mp3";
		public static final String GRASS    = "sounds/grass.mp3";
		public static final String TRAMPLE  = "sounds/trample.mp3";
		public static final String STURDY   = "sounds/sturdy.mp3";

		public static final String HIT              = "sounds/hit.mp3";
		public static final String MISS             = "sounds/miss.mp3";
		public static final String HIT_SLASH        = "sounds/hit_slash.mp3";
		public static final String HIT_STAB         = "sounds/hit_stab.mp3";
		public static final String HIT_CRUSH        = "sounds/hit_crush.mp3";
		public static final String HIT_MAGIC        = "sounds/hit_magic.mp3";
		public static final String HIT_STRONG       = "sounds/hit_strong.mp3";
		public static final String HIT_PARRY        = "sounds/hit_parry.mp3";
		public static final String HIT_ARROW        = "sounds/hit_arrow.mp3";
		public static final String ATK_SPIRITBOW    = "sounds/atk_spiritbow.mp3";
		public static final String ATK_CROSSBOW     = "sounds/atk_crossbow.mp3";
		public static final String HEALTH_WARN      = "sounds/health_warn.mp3";
		public static final String HEALTH_CRITICAL  = "sounds/health_critical.mp3";

		public static final String DESCEND  = "sounds/descend.mp3";
		public static final String EAT      = "sounds/eat.mp3";
		public static final String READ     = "sounds/read.mp3";
		public static final String LULLABY  = "sounds/lullaby.mp3";
		public static final String DRINK    = "sounds/drink.mp3";
		public static final String SHATTER  = "sounds/shatter.mp3";
		public static final String ZAP      = "sounds/zap.mp3";
		public static final String LIGHTNING= "sounds/lightning.mp3";
		public static final String LEVELUP  = "sounds/levelup.mp3";
		public static final String DEATH    = "sounds/death.mp3";
		public static final String CHALLENGE= "sounds/challenge.mp3";
		public static final String CURSED   = "sounds/cursed.mp3";
		public static final String TRAP     = "sounds/trap.mp3";
		public static final String EVOKE    = "sounds/evoke.mp3";
		public static final String TOMB     = "sounds/tomb.mp3";
		public static final String ALERT    = "sounds/alert.mp3";
		public static final String MELD     = "sounds/meld.mp3";
		public static final String BOSS     = "sounds/boss.mp3";
		public static final String BLAST    = "sounds/blast.mp3";
		public static final String PLANT    = "sounds/plant.mp3";
		public static final String RAY      = "sounds/ray.mp3";
		public static final String BEACON   = "sounds/beacon.mp3";
		public static final String TELEPORT = "sounds/teleport.mp3";
		public static final String CHARMS   = "sounds/charms.mp3";
		public static final String MASTERY  = "sounds/mastery.mp3";
		public static final String PUFF     = "sounds/puff.mp3";
		public static final String ROCKS    = "sounds/rocks.mp3";
		public static final String BURNING  = "sounds/burning.mp3";
		public static final String FALLING  = "sounds/falling.mp3";
		public static final String GHOST    = "sounds/ghost.mp3";
		public static final String SECRET   = "sounds/secret.mp3";
		public static final String BONES    = "sounds/bones.mp3";
		public static final String BEE      = "sounds/bee.mp3";
		public static final String DEGRADE  = "sounds/degrade.mp3";
		public static final String MIMIC    = "sounds/mimic.mp3";
		public static final String DEBUFF   = "sounds/debuff.mp3";
		public static final String CHARGEUP = "sounds/chargeup.mp3";
		public static final String GAS      = "sounds/gas.mp3";
		public static final String CHAINS   = "sounds/chains.mp3";
		public static final String SCAN     = "sounds/scan.mp3";
		public static final String SHEEP    = "sounds/sheep.mp3";
		public static final String MINE    = "sounds/mine.mp3";

		public static final String[] all = new String[]{
				CLICK, BADGE, GOLD,

				OPEN, UNLOCK, ITEM, DEWDROP, STEP, WATER, GRASS, TRAMPLE, STURDY,

				HIT, MISS, HIT_SLASH, HIT_STAB, HIT_CRUSH, HIT_MAGIC, HIT_STRONG, HIT_PARRY,
				HIT_ARROW, ATK_SPIRITBOW, ATK_CROSSBOW, HEALTH_WARN, HEALTH_CRITICAL,

				DESCEND, EAT, READ, LULLABY, DRINK, SHATTER, ZAP, LIGHTNING, LEVELUP, DEATH,
				CHALLENGE, CURSED, TRAP, EVOKE, TOMB, ALERT, MELD, BOSS, BLAST, PLANT, RAY, BEACON,
				TELEPORT, CHARMS, MASTERY, PUFF, ROCKS, BURNING, FALLING, GHOST, SECRET, BONES,
				BEE, DEGRADE, MIMIC, DEBUFF, CHARGEUP, GAS, CHAINS, SCAN, SHEEP, MINE
		};
	}

	public static class Splashes {
		public static final String WARRIOR  = "splashes/warrior.jpg";
		public static final String MAGE     = "splashes/mage.jpg";
		public static final String ROGUE    = "splashes/rogue.jpg";
		public static final String HUNTRESS = "splashes/huntress.jpg";
		public static final String DUELIST  = "splashes/duelist.jpg";
		public static final String CLERIC   = "splashes/cleric.jpg";

		public static final String SEWERS   = "splashes/sewers.jpg";
		public static final String PRISON   = "splashes/prison.jpg";
		public static final String CAVES    = "splashes/caves.jpg";
		public static final String CITY     = "splashes/city.jpg";
		public static final String HALLS    = "splashes/halls.jpg";
	}

	public static class Sprites {
		public static final String ITEMS        = "sprites/items.png";
		public static final String ITEM_ICONS   = "sprites/item_icons.png";

		public static final String WARRIOR  = "sprites/warrior.png";
		public static final String MAGE     = "sprites/mage.png";
		public static final String ROGUE    = "sprites/rogue.png";
		public static final String HUNTRESS = "sprites/huntress.png";
		public static final String DUELIST  = "sprites/duelist.png";
		public static final String CLERIC   = "sprites/cleric.png";
		public static final String AVATARS  = "sprites/avatars.png";
		public static final String PET      = "sprites/pet.png";
		public static final String AMULET   = "sprites/amulet.png";

		public static final String RAT      = "sprites/rat.png";
		public static final String BRUTE    = "sprites/brute.png";
		public static final String SPINNER  = "sprites/spinner.png";
		public static final String DM300    = "sprites/dm300.png";
		public static final String WRAITH   = "sprites/wraith.png";
		public static final String UNDEAD   = "sprites/undead.png";
		public static final String KING     = "sprites/king.png";
		public static final String PIRANHA  = "sprites/piranha.png";
		public static final String EYE      = "sprites/eye.png";
		public static final String GNOLL    = "sprites/gnoll.png";
		public static final String CRAB     = "sprites/crab.png";
		public static final String GOO      = "sprites/goo.png";
		public static final String SWARM    = "sprites/swarm.png";
		public static final String SKELETON = "sprites/skeleton.png";
		public static final String SHAMAN   = "sprites/shaman.png";
		public static final String THIEF    = "sprites/thief.png";
		public static final String TENGU    = "sprites/tengu.png";
		public static final String SHEEP    = "sprites/sheep.png";
		public static final String KEEPER   = "sprites/shopkeeper.png";
		public static final String BAT      = "sprites/bat.png";
		public static final String ELEMENTAL= "sprites/elemental.png";
		public static final String MONK     = "sprites/monk.png";
		public static final String WARLOCK  = "sprites/warlock.png";
		public static final String GOLEM    = "sprites/golem.png";
		public static final String STATUE   = "sprites/statue.png";
		public static final String SUCCUBUS = "sprites/succubus.png";
		public static final String SCORPIO  = "sprites/scorpio.png";
		public static final String FISTS    = "sprites/yog_fists.png";
		public static final String YOG      = "sprites/yog.png";
		public static final String LARVA    = "sprites/larva.png";
		public static final String GHOST    = "sprites/ghost.png";
		public static final String MAKER    = "sprites/wandmaker.png";
		public static final String TROLL    = "sprites/blacksmith.png";
		public static final String IMP      = "sprites/demon.png";
		public static final String RATKING  = "sprites/ratking.png";
		public static final String BEE      = "sprites/bee.png";
		public static final String MIMIC    = "sprites/mimic.png";
		public static final String ROT_LASH = "sprites/rot_lasher.png";
		public static final String ROT_HEART= "sprites/rot_heart.png";
		public static final String GUARD    = "sprites/guard.png";
		public static final String WARDS    = "sprites/wards.png";
		public static final String GUARDIAN = "sprites/guardian.png";
		public static final String SLIME    = "sprites/slime.png";
		public static final String SNAKE    = "sprites/snake.png";
		public static final String NECRO    = "sprites/necromancer.png";
		public static final String GHOUL    = "sprites/ghoul.png";
		public static final String RIPPER   = "sprites/ripper.png";
		public static final String SPAWNER  = "sprites/spawner.png";
		public static final String DM100    = "sprites/dm100.png";
		public static final String PYLON    = "sprites/pylon.png";
		public static final String DM200    = "sprites/dm200.png";
		public static final String LOTUS    = "sprites/lotus.png";
		public static final String NINJA_LOG        = "sprites/ninja_log.png";
		public static final String SPIRIT_HAWK      = "sprites/spirit_hawk.png";
		public static final String RED_SENTRY       = "sprites/red_sentry.png";
		public static final String CRYSTAL_WISP     = "sprites/crystal_wisp.png";
		public static final String CRYSTAL_GUARDIAN = "sprites/crystal_guardian.png";
		public static final String CRYSTAL_SPIRE    = "sprites/crystal_spire.png";
		public static final String GNOLL_GUARD      = "sprites/gnoll_guard.png";
		public static final String GNOLL_SAPPER     = "sprites/gnoll_sapper.png";
		public static final String GNOLL_GEOMANCER  = "sprites/gnoll_geomancer.png";
		public static final String FUNGAL_SPINNER   = "sprites/fungal_spinner.png";
		public static final String FUNGAL_SENTRY    = "sprites/fungal_sentry.png";
		public static final String FUNGAL_CORE      = "sprites/fungal_core.png";
	}
	public enum ResourceType {
		LANG,       // 语言
		TEXTURE,    // 纹理
		SOUND,      // 音效
		SCRIPT      // 脚本
	}

	public static class ResourceIndex {
		// 四种资源类型分开存储
		public final Map<String, String> langResources;      // 语言资源
		public final Map<String, String> textureResources;   // 纹理资源
		public final Map<String, String> soundResources;    // 音效资源
		public final Map<String, String> scriptResources;  // 脚本资源

		public ResourceIndex() {
			this.langResources = new HashMap<>();
			this.textureResources = new HashMap<>();
			this.soundResources = new HashMap<>();
			this.scriptResources = new HashMap<>();
		}

		/**
		 * 根据类型获取对应的资源映射
		 */
		public Map<String, String> getResourcesByType(ResourceType type) {
			switch (type) {
				case LANG:
					return langResources;
				case TEXTURE:
					return textureResources;
				case SOUND:
					return soundResources;
				case SCRIPT:
					return scriptResources;
				default:
					throw new IllegalArgumentException("Unknown resource type: " + type);
			}
		}

		/**
		 * 添加资源映射
		 */
		public void put(ResourceType type, String resourceID, String resourceContent) {
			getResourcesByType(type).put(resourceID, resourceContent);
		}

		/**
		 * 获取资源内容
		 */
		public String get(ResourceType type, String resourceID) {
			return getResourcesByType(type).get(resourceID);
		}

		/**
		 * 检查是否包含指定资源ID
		 */
		public boolean contains(ResourceType type, String resourceID) {
			return getResourcesByType(type).containsKey(resourceID);
		}

		/**
		 * 获取指定类型的所有资源ID
		 */
		public java.util.Set<String> keySet(ResourceType type) {
			return getResourcesByType(type).keySet();
		}

		/**
		 * 检查指定类型是否为空
		 */
		public boolean isEmpty(ResourceType type) {
			return getResourcesByType(type).isEmpty();
		}

		/**
		 * 检查是否所有类型都为空
		 */
		public boolean isEmpty() {
			return langResources.isEmpty() 
				&& textureResources.isEmpty() 
				&& soundResources.isEmpty() 
				&& scriptResources.isEmpty();
		}

		/**
		 * 保存到Bundle
		 */
		public void storeInBundle(Bundle bundle) {
			if (!langResources.isEmpty()) {
				Bundle langBundle = new Bundle();
				for (Map.Entry<String, String> entry : langResources.entrySet()) {
					langBundle.put(entry.getKey(), entry.getValue());
				}
				bundle.put("LANG", langBundle);
			}
			if (!textureResources.isEmpty()) {
				Bundle textureBundle = new Bundle();
				for (Map.Entry<String, String> entry : textureResources.entrySet()) {
					textureBundle.put(entry.getKey(), entry.getValue());
				}
				bundle.put("TEXTURE", textureBundle);
			}
			if (!soundResources.isEmpty()) {
				Bundle soundBundle = new Bundle();
				for (Map.Entry<String, String> entry : soundResources.entrySet()) {
					soundBundle.put(entry.getKey(), entry.getValue());
				}
				bundle.put("SOUND", soundBundle);
			}
			if (!scriptResources.isEmpty()) {
				Bundle scriptBundle = new Bundle();
				for (Map.Entry<String, String> entry : scriptResources.entrySet()) {
					scriptBundle.put(entry.getKey(), entry.getValue());
				}
				bundle.put("SCRIPT", scriptBundle);
			}
		}

		/**
		 * 从Bundle恢复
		 */
		public void restoreFromBundle(Bundle bundle) {
			langResources.clear();
			textureResources.clear();
			soundResources.clear();
			scriptResources.clear();
			
			if (bundle.contains("LANG")) {
				Bundle langBundle = bundle.getBundle("LANG");
				for (String key : langBundle.getKeys()) {
					langResources.put(key, langBundle.getString(key));
				}
			}
			if (bundle.contains("TEXTURE")) {
				Bundle textureBundle = bundle.getBundle("TEXTURE");
				for (String key : textureBundle.getKeys()) {
					textureResources.put(key, textureBundle.getString(key));
				}
			}
			if (bundle.contains("SOUND")) {
				Bundle soundBundle = bundle.getBundle("SOUND");
				for (String key : soundBundle.getKeys()) {
					soundResources.put(key, soundBundle.getString(key));
				}
			}
			if (bundle.contains("SCRIPT")) {
				Bundle scriptBundle = bundle.getBundle("SCRIPT");
				for (String key : scriptBundle.getKeys()) {
					scriptResources.put(key, scriptBundle.getString(key));
				}
			}
		}
	}

	// 默认索引（包含 Assets 中定义的所有资源）
	public static final ResourceIndex defaultIndex = new ResourceIndex();

	// 统一的索引栈（从栈底到栈顶，栈顶优先级最高）
	public static final List<ResourceIndex> indexStack = new ArrayList<>();

	// 手动覆盖索引（永远在栈顶）
	public static final ResourceIndex manualOverrideIndex = new ResourceIndex();

	// 静态初始化块：将 Assets 中定义的资源添加到 DefaultIndex
	static {
		// TEXTURE 资源（所有图片资源）
		// Effects
		defaultIndex.put(ResourceType.TEXTURE, "effects/effects.png", Effects.EFFECTS);
		defaultIndex.put(ResourceType.TEXTURE, "effects/fireball.png", Effects.FIREBALL);
		defaultIndex.put(ResourceType.TEXTURE, "effects/specks.png", Effects.SPECKS);
		defaultIndex.put(ResourceType.TEXTURE, "effects/spell_icons.png", Effects.SPELL_ICONS);
		defaultIndex.put(ResourceType.TEXTURE, "effects/text_icons.png", Effects.TEXT_ICONS);

		// Environment
		defaultIndex.put(ResourceType.TEXTURE, "environment/terrain_features.png", Environment.TERRAIN_FEATURES);
		defaultIndex.put(ResourceType.TEXTURE, "environment/visual_grid.png", Environment.VISUAL_GRID);
		defaultIndex.put(ResourceType.TEXTURE, "environment/wall_blocking.png", Environment.WALL_BLOCKING);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_sewers.png", Environment.TILES_SEWERS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_prison.png", Environment.TILES_PRISON);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_caves.png", Environment.TILES_CAVES);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_city.png", Environment.TILES_CITY);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_halls.png", Environment.TILES_HALLS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_caves_crystal.png", Environment.TILES_CAVES_CRYSTAL);
		defaultIndex.put(ResourceType.TEXTURE, "environment/tiles_caves_gnoll.png", Environment.TILES_CAVES_GNOLL);
		defaultIndex.put(ResourceType.TEXTURE, "environment/water0.png", Environment.WATER_SEWERS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/water1.png", Environment.WATER_PRISON);
		defaultIndex.put(ResourceType.TEXTURE, "environment/water2.png", Environment.WATER_CAVES);
		defaultIndex.put(ResourceType.TEXTURE, "environment/water3.png", Environment.WATER_CITY);
		defaultIndex.put(ResourceType.TEXTURE, "environment/water4.png", Environment.WATER_HALLS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/weak_floor.png", Environment.WEAK_FLOOR);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/sewer_boss.png", Environment.SEWER_BOSS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/prison_quest.png", Environment.PRISON_QUEST);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/prison_exit.png", Environment.PRISON_EXIT);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/caves_quest.png", Environment.CAVES_QUEST);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/caves_boss.png", Environment.CAVES_BOSS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/city_boss.png", Environment.CITY_BOSS);
		defaultIndex.put(ResourceType.TEXTURE, "environment/custom_tiles/halls_special.png", Environment.HALLS_SP);

		// Fonts
		defaultIndex.put(ResourceType.TEXTURE, "fonts/pixel_font.png", Fonts.PIXELFONT);

		// Interfaces
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/arcs1.png", Interfaces.ARCS_BG);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/arcs2.png", Interfaces.ARCS_FG);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/banners.png", Interfaces.BANNERS);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/badges.png", Interfaces.BADGES);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/locked_badge.png", Interfaces.LOCKED);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/chrome.png", Interfaces.CHROME);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/icons.png", Interfaces.ICONS);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/status_pane.png", Interfaces.STATUS);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/menu_pane.png", Interfaces.MENU);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/menu_button.png", Interfaces.MENU_BTN);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/toolbar.png", Interfaces.TOOLBAR);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/shadow.png", Interfaces.SHADOW);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/boss_hp.png", Interfaces.BOSSHP);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/surface.png", Interfaces.SURFACE);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/buffs.png", Interfaces.BUFFS_SMALL);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/large_buffs.png", Interfaces.BUFFS_LARGE);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/talent_icons.png", Interfaces.TALENT_ICONS);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/talent_button.png", Interfaces.TALENT_BUTTON);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/hero_icons.png", Interfaces.HERO_ICONS);
		defaultIndex.put(ResourceType.TEXTURE, "interfaces/radial_menu.png", Interfaces.RADIAL_MENU);

		// Splashes
		defaultIndex.put(ResourceType.TEXTURE, "splashes/warrior.jpg", Splashes.WARRIOR);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/mage.jpg", Splashes.MAGE);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/rogue.jpg", Splashes.ROGUE);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/huntress.jpg", Splashes.HUNTRESS);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/duelist.jpg", Splashes.DUELIST);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/cleric.jpg", Splashes.CLERIC);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/sewers.jpg", Splashes.SEWERS);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/prison.jpg", Splashes.PRISON);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/caves.jpg", Splashes.CAVES);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/city.jpg", Splashes.CITY);
		defaultIndex.put(ResourceType.TEXTURE, "splashes/halls.jpg", Splashes.HALLS);

		// Sprites
		defaultIndex.put(ResourceType.TEXTURE, "sprites/items.png", Sprites.ITEMS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/item_icons.png", Sprites.ITEM_ICONS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/warrior.png", Sprites.WARRIOR);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/mage.png", Sprites.MAGE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/rogue.png", Sprites.ROGUE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/huntress.png", Sprites.HUNTRESS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/duelist.png", Sprites.DUELIST);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/cleric.png", Sprites.CLERIC);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/avatars.png", Sprites.AVATARS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/pet.png", Sprites.PET);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/amulet.png", Sprites.AMULET);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/rat.png", Sprites.RAT);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/brute.png", Sprites.BRUTE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/spinner.png", Sprites.SPINNER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/dm300.png", Sprites.DM300);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/wraith.png", Sprites.WRAITH);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/undead.png", Sprites.UNDEAD);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/king.png", Sprites.KING);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/piranha.png", Sprites.PIRANHA);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/eye.png", Sprites.EYE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/gnoll.png", Sprites.GNOLL);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/crab.png", Sprites.CRAB);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/goo.png", Sprites.GOO);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/swarm.png", Sprites.SWARM);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/skeleton.png", Sprites.SKELETON);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/shaman.png", Sprites.SHAMAN);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/thief.png", Sprites.THIEF);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/tengu.png", Sprites.TENGU);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/sheep.png", Sprites.SHEEP);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/shopkeeper.png", Sprites.KEEPER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/bat.png", Sprites.BAT);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/elemental.png", Sprites.ELEMENTAL);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/monk.png", Sprites.MONK);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/warlock.png", Sprites.WARLOCK);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/golem.png", Sprites.GOLEM);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/statue.png", Sprites.STATUE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/succubus.png", Sprites.SUCCUBUS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/scorpio.png", Sprites.SCORPIO);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/yog_fists.png", Sprites.FISTS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/yog.png", Sprites.YOG);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/larva.png", Sprites.LARVA);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/ghost.png", Sprites.GHOST);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/wandmaker.png", Sprites.MAKER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/blacksmith.png", Sprites.TROLL);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/demon.png", Sprites.IMP);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/ratking.png", Sprites.RATKING);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/bee.png", Sprites.BEE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/mimic.png", Sprites.MIMIC);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/rot_lasher.png", Sprites.ROT_LASH);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/rot_heart.png", Sprites.ROT_HEART);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/guard.png", Sprites.GUARD);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/wards.png", Sprites.WARDS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/guardian.png", Sprites.GUARDIAN);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/slime.png", Sprites.SLIME);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/snake.png", Sprites.SNAKE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/necromancer.png", Sprites.NECRO);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/ghoul.png", Sprites.GHOUL);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/ripper.png", Sprites.RIPPER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/spawner.png", Sprites.SPAWNER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/dm100.png", Sprites.DM100);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/pylon.png", Sprites.PYLON);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/dm200.png", Sprites.DM200);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/lotus.png", Sprites.LOTUS);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/ninja_log.png", Sprites.NINJA_LOG);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/spirit_hawk.png", Sprites.SPIRIT_HAWK);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/red_sentry.png", Sprites.RED_SENTRY);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/crystal_wisp.png", Sprites.CRYSTAL_WISP);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/crystal_guardian.png", Sprites.CRYSTAL_GUARDIAN);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/crystal_spire.png", Sprites.CRYSTAL_SPIRE);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/gnoll_guard.png", Sprites.GNOLL_GUARD);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/gnoll_sapper.png", Sprites.GNOLL_SAPPER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/gnoll_geomancer.png", Sprites.GNOLL_GEOMANCER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/fungal_spinner.png", Sprites.FUNGAL_SPINNER);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/fungal_sentry.png", Sprites.FUNGAL_SENTRY);
		defaultIndex.put(ResourceType.TEXTURE, "sprites/fungal_core.png", Sprites.FUNGAL_CORE);

		// SOUND 资源（音效）
		for (String sound : Sounds.all) {
			defaultIndex.put(ResourceType.SOUND, sound, sound);
		}

		// LANG 资源（语言资源包）
		defaultIndex.put(ResourceType.LANG, "messages/actors/actors", Messages.ACTORS);
		defaultIndex.put(ResourceType.LANG, "messages/items/items", Messages.ITEMS);
		defaultIndex.put(ResourceType.LANG, "messages/journal/journal", Messages.JOURNAL);
		defaultIndex.put(ResourceType.LANG, "messages/levels/levels", Messages.LEVELS);
		defaultIndex.put(ResourceType.LANG, "messages/misc/misc", Messages.MISC);
		defaultIndex.put(ResourceType.LANG, "messages/plants/plants", Messages.PLANTS);
		defaultIndex.put(ResourceType.LANG, "messages/scenes/scenes", Messages.SCENES);
		defaultIndex.put(ResourceType.LANG, "messages/ui/ui", Messages.UI);
		defaultIndex.put(ResourceType.LANG, "messages/windows/windows", Messages.WINDOWS);
	}

	// 初始化索引栈（defaultIndex 填充完成后执行）
	static {
		init();
	}

	// Bundle键名
	public static final String BUNDLE_KEY_MANUAL_OVERRIDES = "material_manual_overrides";

	// 资源键类（用于Map的key）
	private static class ResourceKey {
		public final ResourceType type;
		public final String resourceID;

		public ResourceKey(ResourceType type, String resourceID) {
			this.type = type;
			this.resourceID = resourceID;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ResourceKey that = (ResourceKey) o;
			return type == that.type && resourceID.equals(that.resourceID);
		}

		@Override
		public int hashCode() {
			return type.hashCode() * 31 + resourceID.hashCode();
		}
	}

	// 当前所有被查询过的资源的最终内容表
	// ResourceKey -> ResourceContent
	public static final Map<ResourceKey, String> resourceCache = new HashMap<>();

	/**
	 * 初始化资源索引系统
	 * 应该在游戏启动时调用
	 */
	public static void init() {
		// 清空栈
		indexStack.clear();
		
		// 添加默认索引到栈底
		indexStack.add(defaultIndex);
		indexStack.add(manualOverrideIndex);

		// 从SaveManager加载手动覆盖
		loadManualOverrides();
	}

	/**
	 * 添加索引到栈
	 * 新添加的索引会放在手动覆盖索引之前
	 */
	public static void addIndex(ResourceIndex index) {
		if (index == null) {
			return;
		}

		// 确保手动覆盖索引在栈顶
		if (!indexStack.isEmpty() && indexStack.get(indexStack.size() - 1) == manualOverrideIndex) {
			indexStack.remove(indexStack.size() - 1);
		}

		indexStack.add(index);

		// 重新添加手动覆盖索引到栈顶
		indexStack.add(manualOverrideIndex);

		// 只更新该索引中包含的资源缓存
		updateResourceCacheForIndex(index);
	}

	/**
	 * 从栈中移除索引
	 */
	public static boolean removeIndex(ResourceIndex index) {
		if (index == null || index == manualOverrideIndex) {
			return false; // 不能移除手动覆盖索引
		}
		boolean removed = indexStack.remove(index);
		if (removed) {
			// 只更新该索引中包含的资源缓存
			updateResourceCacheForIndex(index);
		}
		return removed;
	}

	/**
	 * 清空所有索引（保留手动覆盖索引）
	 */
	public static void clearIndices() {
		indexStack.clear();
		indexStack.add(manualOverrideIndex);
		// 清空所有索引后，所有已缓存的资源都可能受影响，需要全部更新
		java.util.Set<ResourceKey> keysToUpdate = new java.util.HashSet<>(resourceCache.keySet());
		for (ResourceKey key : keysToUpdate) {
			updateResourceCacheForResource(key.type, key.resourceID);
		}
	}

	/**
	 * 设置手动覆盖
	 * 手动覆盖优先级最高，永远在栈顶
	 */
	public static void setManualOverride(ResourceType type, String resourceID, String resourceContent) {
		manualOverrideIndex.put(type, resourceID, resourceContent);
		saveManualOverrides();
		// 更新资源缓存
		updateResourceCacheForResource(type, resourceID);
	}

	/**
	 * 清除手动覆盖
	 */
	public static void clearManualOverride(ResourceType type, String resourceID) {
		Map<String, String> resources = manualOverrideIndex.getResourcesByType(type);
		if (resources.containsKey(resourceID)) {
			resources.remove(resourceID);
			saveManualOverrides();
			// 更新资源缓存
			updateResourceCacheForResource(type, resourceID);
		}
	}

	/**
	 * 清除指定类型的所有手动覆盖
	 */
	public static void clearManualOverrides(ResourceType type) {
		Map<String, String> resources = manualOverrideIndex.getResourcesByType(type);
		java.util.Set<String> affectedIDs = new java.util.HashSet<>(resources.keySet());
		resources.clear();
		saveManualOverrides();
		// 只更新受影响的资源缓存
		for (String resourceID : affectedIDs) {
			updateResourceCacheForResource(type, resourceID);
		}
	}

	/**
	 * 清除所有手动覆盖
	 */
	public static void clearAllManualOverrides() {
		java.util.Set<ResourceKey> affectedKeys = new java.util.HashSet<>();
		for (ResourceType type : ResourceType.values()) {
			Map<String, String> resources = manualOverrideIndex.getResourcesByType(type);
			for (String resourceID : resources.keySet()) {
				affectedKeys.add(new ResourceKey(type, resourceID));
			}
		}
		manualOverrideIndex.langResources.clear();
		manualOverrideIndex.textureResources.clear();
		manualOverrideIndex.soundResources.clear();
		manualOverrideIndex.scriptResources.clear();
		saveManualOverrides();
		// 只更新受影响的资源缓存
		for (ResourceKey key : affectedKeys) {
			updateResourceCacheForResource(key.type, key.resourceID);
		}
	}

	/**
	 * 获取资源内容
	 * 从栈顶向下遍历，找到第一个包含该ResourceID的索引
	 * 查询结果会被缓存到resourceCache中
	 * 
	 * @param type 资源类型
	 * @param resourceID 资源ID
	 * @return 资源内容，如果未找到返回null
	 */
	public static String getResource(ResourceType type, String resourceID) {
		ResourceKey key = new ResourceKey(type, resourceID);
		
		// 从栈顶向下遍历（从后往前）
		for (int i = indexStack.size() - 1; i >= 0; i--) {
			ResourceIndex index = indexStack.get(i);
			if (index.contains(type, resourceID)) {
				String content = index.get(type, resourceID);
				// 缓存查询结果
				resourceCache.put(key, content);
				return content;
			}
		}

		// 未找到时也缓存null
		resourceCache.put(key, null);
		return null;
	}

	/**
	 * 获取资源内容，如果未找到返回默认值
	 */
	public static String getResource(ResourceType type, String resourceID, String defaultValue) {
		String result = getResource(type, resourceID);
		return result != null ? result : defaultValue;
	}

	/**
	 * 获取纹理路径（经 Asset 索引栈分发）。
	 * 所有 Sprite/纹理应通过此方法解析路径，以支持手动覆盖与 Mod 资源索引。
	 *
	 * @param resourceID 纹理资源 ID（通常与默认路径一致，如 Assets.Sprites.RAT）
	 * @return 解析后的纹理路径，未找到时返回 resourceID
	 */
	public static String getTexture(String resourceID) {
		return getResource(ResourceType.TEXTURE, resourceID, resourceID);
	}

	/**
	 * 获取纹理路径，若未找到则返回默认值。
	 */
	public static String getTexture(String resourceID, String defaultValue) {
		return getResource(ResourceType.TEXTURE, resourceID, defaultValue);
	}

	/**
	 * 获取音效路径（经 Asset 索引栈分发）。
	 * 所有 Sample/音效应通过此方法解析路径，以支持手动覆盖与 Mod 资源索引。
	 */
	public static String getSound(String resourceID) {
		return getResource(ResourceType.SOUND, resourceID, resourceID);
	}

	public static String getSound(String resourceID, String defaultValue) {
		return getResource(ResourceType.SOUND, resourceID, defaultValue);
	}

	/**
	 * 返回 Sounds.all 经索引解析后的路径数组，用于 Sample.INSTANCE.load(...)。
	 */
	public static String[] getSoundsAllResolved() {
		String[] out = new String[Sounds.all.length];
		for (int i = 0; i < Sounds.all.length; i++) {
			out[i] = getSound(Sounds.all[i]);
		}
		return out;
	}

	/**
	 * 保存手动覆盖到SaveManager的global bundle
	 */
	public static void saveManualOverrides() {
		try {
			Bundle global = SaveManager.loadGlobal();
			Bundle materialBundle = new Bundle();
			manualOverrideIndex.storeInBundle(materialBundle);
			global.put(BUNDLE_KEY_MANUAL_OVERRIDES, materialBundle);
			SaveManager.saveGlobal(global);
		} catch (Exception e) {
			// 保存失败时静默处理，避免影响游戏运行
			com.watabou.noosa.Game.reportException(e);
		}
	}

	/**
	 * 从SaveManager的global bundle加载手动覆盖
	 */
	public static void loadManualOverrides() {
		try {
			Bundle global = SaveManager.loadGlobal();
			if (global.contains(BUNDLE_KEY_MANUAL_OVERRIDES)) {
				// 记录加载前的资源ID，以便只更新受影响的资源
				java.util.Set<ResourceKey> beforeKeys = new java.util.HashSet<>();
				for (ResourceType type : ResourceType.values()) {
					Map<String, String> resources = manualOverrideIndex.getResourcesByType(type);
					for (String resourceID : resources.keySet()) {
						beforeKeys.add(new ResourceKey(type, resourceID));
					}
				}
				
				Bundle materialBundle = global.getBundle(BUNDLE_KEY_MANUAL_OVERRIDES);
				manualOverrideIndex.restoreFromBundle(materialBundle);
				
				// 记录加载后的资源ID
				java.util.Set<ResourceKey> afterKeys = new java.util.HashSet<>();
				for (ResourceType type : ResourceType.values()) {
					Map<String, String> resources = manualOverrideIndex.getResourcesByType(type);
					for (String resourceID : resources.keySet()) {
						afterKeys.add(new ResourceKey(type, resourceID));
					}
				}
				
				// 只更新受影响的资源（新增的、删除的、修改的）
				java.util.Set<ResourceKey> affectedKeys = new java.util.HashSet<>(beforeKeys);
				affectedKeys.addAll(afterKeys);
				for (ResourceKey key : affectedKeys) {
					updateResourceCacheForResource(key.type, key.resourceID);
				}
			}
		} catch (Exception e) {
			// 加载失败时静默处理，使用默认值
			com.watabou.noosa.Game.reportException(e);
		}
	}

	/**
	 * 获取索引栈的大小（不包括手动覆盖索引）
	 */
	public static int getIndexStackSize() {
		return Math.max(0, indexStack.size() - 1); // 减去手动覆盖索引
	}

	/**
	 * 检查是否有手动覆盖
	 */
	public static boolean hasManualOverrides() {
		return !manualOverrideIndex.isEmpty();
	}

	/**
	 * 检查指定资源是否有手动覆盖
	 */
	public static boolean hasManualOverride(ResourceType type, String resourceID) {
		return manualOverrideIndex.contains(type, resourceID);
	}

	/**
	 * 更新指定资源的缓存
	 * 重新从栈中查询该资源并更新缓存
	 */
	private static void updateResourceCacheForResource(ResourceType type, String resourceID) {
		ResourceKey key = new ResourceKey(type, resourceID);
		// 如果该资源在缓存中，重新查询并更新
		if (resourceCache.containsKey(key)) {
			// 从栈顶向下遍历（从后往前）
			for (int i = indexStack.size() - 1; i >= 0; i--) {
				ResourceIndex index = indexStack.get(i);
				if (index.contains(type, resourceID)) {
					String content = index.get(type, resourceID);
					resourceCache.put(key, content);
					return;
				}
			}
			// 未找到时设置为null
			resourceCache.put(key, null);
		}
	}

	/**
	 * 更新索引中包含的所有资源的缓存
	 * 只更新该索引中包含的、且已在缓存中的资源
	 */
	private static void updateResourceCacheForIndex(ResourceIndex index) {
		for (ResourceType type : ResourceType.values()) {
			Map<String, String> resources = index.getResourcesByType(type);
			for (String resourceID : resources.keySet()) {
				ResourceKey key = new ResourceKey(type, resourceID);
				// 只更新已在缓存中的资源
				if (resourceCache.containsKey(key)) {
					updateResourceCacheForResource(type, resourceID);
				}
			}
		}
	}
}
