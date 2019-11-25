package fr.unikaz.unibot;

import fr.unikaz.unibot.listeners.Party;

import org.apache.commons.lang.ObjectUtils;

public enum Emoji {
	ZERO("\u0030\u20e3"),
	ONE("\u0031\u20e3"),
	TWO("\u0032\u20e3"),
	THREE("\u0033\u20e3"),
	FOUR("\u0034\u20e3"),
	FIVE("\u0035\u20e3"),
	SIX("\u0036\u20e3"),
	SEVEN("\u0037\u20e3"),
	EIGHT("\u0038\u20e3"),
	NINE("\u0039\u20e3"),
	WHITE_CHECK_MARK("✅"),
	X("❌");
	public final String val;

	public static final Emoji[] DIGITS={
		ZERO,ONE,TWO,THREE,FOUR,FIVE,SIX,SEVEN,EIGHT,NINE
	};

	Emoji(String val) {
		this.val = val;
	}

	public static Emoji get(String val) {
		for (Emoji value : values()) {
			if (value.val.equals(val))
				return value;
		}
		return null;
	}
	public static int getDigitValue(String val){
		for (int i = 0; i < DIGITS.length; i++) {
			if(DIGITS[i].name().equals(val))
				return i;
		}
		return -1;
	}
}
