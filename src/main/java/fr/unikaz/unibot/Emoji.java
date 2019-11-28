package fr.unikaz.unibot;

import fr.unikaz.unibot.listeners.Party;

import org.apache.commons.lang.ObjectUtils;

public enum Emoji {
	ZERO("\u0030\u20e3", "0️⃣"),
	ONE("\u0031\u20e3", "1️⃣"),
	TWO("\u0032\u20e3", "2️⃣"),
	THREE("\u0033\u20e3", "3️⃣"),
	FOUR("\u0034\u20e3", "4️⃣"),
	FIVE("\u0035\u20e3", "5️⃣"),
	SIX("\u0036\u20e3", "6️⃣"),
	SEVEN("\u0037\u20e3", "7️⃣"),
	EIGHT("\u0038\u20e3", "8️⃣"),
	NINE("\u0039\u20e3", "9️⃣"),
	WHITE_CHECK_MARK("✅"),
	X("❌");
	public final String val;
	public final String val2;

	public static final Emoji[] DIGITS={
		ZERO,ONE,TWO,THREE,FOUR,FIVE,SIX,SEVEN,EIGHT,NINE
	};

	Emoji(String val) {
		this.val = val;
		this.val2 = null;
	}
	Emoji(String val, String val2) {
		this.val = val;
		this.val2 = val2;
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

	public boolean is(String e) {
		if(e == null) return false;
		return e.equals(val) || e.equals(val2);
	}

	public static String intToEmoji(int value){
		if(value == 0 ) return ZERO.val;
		StringBuilder s = new StringBuilder();
		while(value > 0){
			s.insert(0, DIGITS[value % 10].val);
			value /= 10;
		}
		return s.toString();
	}
}
