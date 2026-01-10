package com.zootdungeon.utils;
//these are actually general, the usage depends on context.
public interface Pred {
	
	default boolean accept(Class<?> clazz) {
		return false;
	}

	default boolean accept(Class<?> clazz1, Class<?> clazz2) {
		return false;
	}

	default Class<?> mapTo(Class<?> clazz) {
		return null;
	}
	default int count(Class<?> ...classes){
		return 0;
	}
}

