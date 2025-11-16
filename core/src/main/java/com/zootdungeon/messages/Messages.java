package com.zootdungeon.messages;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.zootdungeon.Assets;
import com.zootdungeon.CDSettings;
import com.zootdungeon.ColaDungeon;
import com.watabou.utils.FileUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/*
	Simple wrapper class for libGDX I18NBundles.

	The core idea here is that each string resource's key is a combination of the class definition and a local value.
	An object or static method would usually call this with an object/class reference (usually its own) and a local key.
	This means that an object can just ask for "name" rather than, say, "items.weapon.enchantments.death.name"
 */
public class Messages {

	private static ArrayList<I18NBundle> bundles;
	private static ArrayList<I18NBundle> originBundles;
	private static Languages lang;
	private static Locale locale;
	private static HashMap<String, String> externalStrings;
	private static Languages externalOriginLanguage;
	private static String externalAuthor;
	private static String externalVersion;

	public static final String NO_TEXT_FOUND = "!!!NO TEXT FOUND!!!";

	public static Languages lang(){
		return lang;
	}

	public static Locale locale(){
		return locale;
	}

	/**
	 * Setup Methods
	 */

	private static String[] prop_files = new String[]{
			Assets.Messages.ACTORS,
			Assets.Messages.ITEMS,
			Assets.Messages.JOURNAL,
			Assets.Messages.LEVELS,
			Assets.Messages.MISC,
			Assets.Messages.PLANTS,
			Assets.Messages.SCENES,
			Assets.Messages.UI,
			Assets.Messages.WINDOWS
	};

	static{
		formatters = new HashMap<>();
		setup(CDSettings.language());
	}

	public static void setup( Languages lang ){
		externalStrings = null;
		externalOriginLanguage = null;
		externalAuthor = null;
		externalVersion = null;
		originBundles = null;
		//seeing as missing keys are part of our process, this is faster than throwing an exception
		I18NBundle.setExceptionOnMissingKey(false);

		//store language and locale info for various string logic
		Messages.lang = lang;
		Locale bundleLocal;
		if (lang == Languages.ENGLISH){
			locale = Locale.ENGLISH;
			bundleLocal = Locale.ROOT; //english is source, uses root locale for fetching bundle
		} else {
			locale = Locale.forLanguageTag(lang.code());
			bundleLocal = locale;
		}
		formatters.clear();

		//strictly match the language code when fetching bundles however
		bundles = new ArrayList<>();
		for (String file : prop_files) {
			bundles.add(I18NBundle.createBundle(Gdx.files.internal(file), bundleLocal));
		}

		// Attempt to load external overlay language file (from /lang folder)
		loadExternalOverlay(lang);
	}



	/**
	 * Resource grabbing methods
	 */

	public static String get(String key, Object...args){
		return get(null, key, args);
	}

	public static String get(Object o, String k, Object...args){
		return get(o.getClass(), k, args);
	}

	public static String get(Class c, String k, Object...args){
		String key;
		if (c != null){
			key = c.getName().replace("com.zootdungeon.", "");
			key += "." + k;
		} else
			key = k;

		String value = getFromBundle(key.toLowerCase(Locale.ENGLISH));
		if (value != null){
			if (args.length > 0) return format(value, args);
			else return value;
		} else {
			//this is so child classes can inherit properties from their parents.
			//in cases where text is commonly grabbed as a utility from classes that aren't mean to be instantiated
			//(e.g. flavourbuff.dispTurns()) using .class directly is probably smarter to prevent unnecessary recursive calls.
			if (c != null && c.getSuperclass() != null){
				return get(c.getSuperclass(), k, args);
			} else {
				return NO_TEXT_FOUND;
			}
		}
	}
	private static String getFromBundle(String key){
		String result;
		// 1) External overlay takes highest priority
		if (externalStrings != null){
			result = externalStrings.get(key);
			if (result != null){
				return result;
			}
		}
		// 2) Selected in-game language bundles
		for (I18NBundle b : bundles){
			result = b.get(key);
			//if it isn't the return string for no key found, return it
			if (result.length() != key.length()+6 || !result.contains(key)){
				return result;
			}
		}
		// 3) Origin fallback bundles defined by external file
		if (originBundles != null){
			for (I18NBundle b : originBundles){
				result = b.get(key);
				if (result.length() != key.length()+6 || !result.contains(key)){
					return result;
				}
			}
		}
		return null;
	}



	/**
	 * String Utility Methods
	 */

	public static String format( String format, Object...args ) {
		try {
			return String.format(locale(), format, args);
		} catch (IllegalFormatException e) {
			ColaDungeon.reportException( new Exception("formatting error for the string: " + format, e) );
			return format;
		}
	}

	private static HashMap<String, DecimalFormat> formatters;

	public static String decimalFormat( String format, double number ){
		if (!formatters.containsKey(format)){
			formatters.put(format, new DecimalFormat(format, DecimalFormatSymbols.getInstance(locale())));
		}
		return formatters.get(format).format(number);
	}

	public static String capitalize( String str ){
		if (str.length() == 0)  return str;
		else                    return str.substring( 0, 1 ).toUpperCase(locale) + str.substring( 1 );
	}

	//Words which should not be capitalized in title case, mostly prepositions which appear ingame
	//This list is not comprehensive!
	private static final HashSet<String> noCaps = new HashSet<>(
			Arrays.asList("a", "an", "and", "of", "by", "to", "the", "x", "for")
	);

	public static String titleCase( String str ){
		//English capitalizes every word except for a few exceptions
		if (lang == Languages.ENGLISH){
			String result = "";
			//split by any unicode space character
			for (String word : str.split("(?<=\\p{Zs})")){
				if (noCaps.contains(word.trim().toLowerCase(Locale.ENGLISH).replaceAll(":|[0-9]", ""))){
					result += word;
				} else {
					result += capitalize(word);
				}
			}
			//first character is always capitalized.
			return capitalize(result);
		}

		//Otherwise, use sentence case
		return capitalize(str);
	}

	public static String upperCase( String str ){
		return str.toUpperCase(locale);
	}

	public static String lowerCase( String str ){
		return str.toLowerCase(locale);
	}

	/**
	 * External language overlay loading
	 */
	private static void loadExternalOverlay(Languages selectedLanguage){
		try {
			FileHandle langDir = findLangDirectory();
			if (langDir == null || !langDir.exists() || !langDir.isDirectory()) {
				return;
			}

			FileHandle[] candidateFiles = langDir.list();
			if (candidateFiles == null || candidateFiles.length == 0){
				return;
			}

			// Selection strategy:
			// 1) Prefer filename suffix indicating selected language (e.g., *_zh.lang, -zh.lang, .zh.lang)
			// 2) Next, prefer file which declares language/lang == selectedLanguage.code()
			// 3) If none match but only one file exists, use it
			// 4) Otherwise pick the first readable properties-like file
			FileHandle chosen = null;
			Properties chosenProps = null;
			String code = selectedLanguage.code().toLowerCase(Locale.ENGLISH);

			// If user selected a specific overlay, try that first
			String selectedOverlay = CDSettings.languageOverlay();
			// Respect explicit 'None' choice: skip any overlays entirely
			if (CDSettings.LANG_OVERLAY_DISABLED.equals(selectedOverlay) || "".equals(selectedOverlay)){
				return; // overlays disabled, do not auto-detect
			}
			if (selectedOverlay != null){
				for (FileHandle fh : candidateFiles){
					if (fh.isDirectory()) continue;
					if (!isLangFile(fh)) continue;
					if (fh.name().equals(selectedOverlay)){
						Properties p = safeLoadProps(fh);
						if (p != null){
							chosen = fh;
							chosenProps = p;
						}
						break;
					}
				}
			}

			// Pass 1: filename suffix
			if (chosen == null){
				for (FileHandle fh : candidateFiles){
					if (fh.isDirectory()) continue;
					if (!isLangFile(fh)) continue;
					if (!filenameIndicatesLanguage(fh, code)) continue;
					Properties p = safeLoadProps(fh);
					if (p == null) continue;
					chosen = fh;
					chosenProps = p;
					break;
				}
			}

			// Pass 2: declared language in file contents
			if (chosen == null){
				for (FileHandle fh : candidateFiles){
					if (fh.isDirectory()) continue;
					if (!isLangFile(fh)) continue;
					Properties p = safeLoadProps(fh);
					if (p == null) continue;
					String declaredLang = firstNonEmpty(p.getProperty("language"), p.getProperty("lang"));
					if (declaredLang != null && declaredLang.trim().equalsIgnoreCase(code)){
						chosen = fh;
						chosenProps = p;
						break;
					}
					// remember first readable if we need a fallback
					if (chosen == null){
						chosen = fh;
						chosenProps = p;
					}
				}
			}

			if (chosen == null || chosenProps == null){
				return;
			}

			// Extract meta
			externalAuthor = trimToNull(chosenProps.getProperty("author"));
			externalVersion = trimToNull(chosenProps.getProperty("version"));
			String originCode = trimToNull(chosenProps.getProperty("origin"));
			if (originCode == null) originCode = "en";
			externalOriginLanguage = Languages.matchCode(originCode);

			// Build external strings map (skip meta keys)
			externalStrings = new HashMap<>();
			for (Map.Entry<Object, Object> e : chosenProps.entrySet()){
				String k = String.valueOf(e.getKey());
				if (isMetaKey(k)) continue;
				String v = String.valueOf(e.getValue());
				// normalize keys to lowercase to match Messages.get() behavior
				externalStrings.put(k.toLowerCase(Locale.ENGLISH), v);
			}

			// Prepare origin bundles for fallback if defined
			if (externalOriginLanguage != null){
				Locale originLocale;
				if (externalOriginLanguage == Languages.ENGLISH){
					originLocale = Locale.ROOT; // english is source
				} else {
					originLocale = Locale.forLanguageTag(externalOriginLanguage.code());
				}
				originBundles = new ArrayList<>();
				for (String file : prop_files) {
					originBundles.add(I18NBundle.createBundle(Gdx.files.internal(file), originLocale));
				}
			}
		} catch (Exception ex){
			ColaDungeon.reportException(new Exception("Failed to load external language overlay", ex));
			// ensure no partial state
			externalStrings = null;
			originBundles = null;
			externalOriginLanguage = null;
		}
	}

	private static FileHandle findLangDirectory(){
		// Use shared FileUtils base path for platform-consistent location
		FileHandle dir = FileUtils.getFileHandle("lang");
		if (dir != null && dir.exists() && dir.isDirectory()){
			return dir;
		} else {
			return null;
		}
	}

	private static Properties safeLoadProps(FileHandle fh){
		try {
			// Read as UTF-8 to support unicode characters directly
			InputStream in = fh.read();
			try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				Properties p = new Properties();
				p.load(reader);
				return p;
			}
		} catch (Throwable t){
			ColaDungeon.reportException(new Exception("Failed reading language file: " + fh.path(), t));
			return null;
		}
	}

	private static boolean isMetaKey(String key){
		if (key == null) return false;
		String k = key.trim().toLowerCase(Locale.ENGLISH);
		return k.equals("author") || k.equals("version") || k.equals("origin") || k.equals("language") || k.equals("lang");
	}

	private static String trimToNull(String s){
		if (s == null) return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static boolean filenameIndicatesLanguage(FileHandle fh, String langCode){
		String name = fh.name().toLowerCase(Locale.ENGLISH);
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		return base.endsWith("_" + langCode)
				|| base.endsWith("-" + langCode)
				|| base.endsWith("." + langCode);
	}

	private static boolean isLangFile(FileHandle fh){
		String name = fh.name().toLowerCase(Locale.ENGLISH);
		return name.endsWith(".lang");
	}

	private static String firstNonEmpty(String... vals){
		if (vals == null) return null;
		for (String v : vals){
			String t = trimToNull(v);
			if (t != null) return t;
		}
		return null;
	}

	/**
	 * Returns a list of available overlay filenames in the lang directory which appear to
	 * target the provided language (based on filename suffix or declared lang).
	 */
	public static ArrayList<String> availableOverlays(Languages forLanguage){
		ArrayList<String> list = new ArrayList<>();
		try {
			FileHandle dir = findLangDirectory();
			if (dir == null || !dir.exists() || !dir.isDirectory()) return list;
			FileHandle[] files = dir.list();
			if (files == null || files.length == 0) return list;
			String code = forLanguage.code().toLowerCase(Locale.ENGLISH);
			for (FileHandle fh : files){
				if (fh.isDirectory() || !isLangFile(fh)) continue;
				// include if filename indicates language or file declares it
				if (filenameIndicatesLanguage(fh, code)) {
					list.add(fh.name());
				} else {
					Properties p = safeLoadProps(fh);
					if (p == null) continue;
					String declaredLang = firstNonEmpty(p.getProperty("language"), p.getProperty("lang"));
					if (declaredLang != null && declaredLang.trim().equalsIgnoreCase(code)){
						list.add(fh.name());
					}
				}
			}
		} catch (Throwable t){
			ColaDungeon.reportException(new Exception("Failed to list overlays", t));
		}
		return list;
	}
}