package au.edu.educationau.opensource;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to be able to calculate the plurals of most English words correctly without
 * the need of a dictionary file.<br />
 * Note that PluralUtils will not know if a word already is a plural or if the word
 * is not a noun and therfore has no plural form.
 * @author James Andrews
 */
public class PluralUtil {

	/**
	 * The English language loves to break its own rules, so keep a
	 * map of such rule breakers when the algorithm wont cut it.
	 */
	private Map<String,String> knownPlurals;
	
	/**
	 * Regex used to grab the last word of a phrase
	 */
	private static Pattern LAST_WORD = Pattern.compile("(.*(?:\\s|-))?(.+)");
	
	private static Set<Character> VOWELS = new HashSet<Character>(Arrays.asList('a','e','i','o','u'));
	
	public static PluralUtil loadDefault() {
		Properties props = new Properties();
		try {
			props.load(PluralUtil.class.getResourceAsStream("plurals.properties"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new PluralUtil(props);
	}
	
	public PluralUtil(Properties knownPlurals) {
		this.knownPlurals = new HashMap<String,String>();
		for(Map.Entry<Object,Object> entry : knownPlurals.entrySet()) {
			this.knownPlurals.put((String)entry.getKey(), (String)entry.getValue());
		}
	}
	
	public PluralUtil(Map<String,String> knownPlurals) {
		this.knownPlurals.putAll(knownPlurals);
	}
	
	/**
	 * Returns the plural of the word (or null if it's known that the word
	 * can't be pluralised).
	 * 
	 * @param input A word (or words) to be pluralised, only the last word will be examined.
	 * Case insensitive except if the entire word is upper cased it is assumed it's an
	 * acronym.
	 * @return
	 * <ul>
	 * <li>&lt;null&gt; if the word can't be pluralised.</li>
	 * <li>Possibly the same word that was passed in (e.g. "fish" returns "fish").</li>
	 * <li>Hopefully the actual plural of the word, leading capital letters will be maintained.</li>
	 * </ul>
	 */
	public String calculatePlural(String input) {
		Matcher wordMatcher = LAST_WORD.matcher(input);
		if (!wordMatcher.matches()) {
			return null;
		} else {
			String prefix = wordMatcher.group(1);
			String lastWord = wordMatcher.group(2);
			
			String pluralLastWord = singleWordPossiblePlurals(lastWord);
			if (pluralLastWord == null) {
				return null;
			} else if (prefix == null) {
				return pluralLastWord;
			} else {
				return prefix + pluralLastWord;
			}
		}
	}
	
	protected String singleWordPossiblePlurals(String word) {
		if (word.length() <= 1) {
			return null;
		}
		//probably an acronym
		boolean allUpperCase = word.toUpperCase().equals(word);
		if (allUpperCase) {
			return word + "s";
		}
		
		String lowerCase = word.toLowerCase();
		String knownPlural = knownPlurals.get(lowerCase);
		boolean upperFirst = Character.isUpperCase(word.charAt(0));
		if (knownPlural != null) {
			if (upperFirst) {
				return knownPlural.substring(0,1).toUpperCase() + knownPlural.substring(1);
			} else {
				return knownPlural;
			}
		}
		
		Character last1 = lowerCase.charAt(lowerCase.length()-1);
		Character last2 = lowerCase.charAt(lowerCase.length()-2);
		
		String root = lowerCase;
		String post = "";
		int length = lowerCase.length();
		
		if (last1 == 'y' && !VOWELS.contains(last2)) {
			root = lowerCase.substring(0, length-1);
			post = "ies";
		} else if (last1 == 's') {
			if (!VOWELS.contains(last2)) {
				if (lowerCase.endsWith("ius")) {
					root = lowerCase.substring(0, length-2);
					post = "i";
				} else {
					root = lowerCase.substring(0, length-1);
					post = "es";
				}
			}
		} else if (last1 == 'o' || lowerCase.endsWith("ch") || lowerCase.endsWith("sh")) {
			post = "es";
		} else {
			post = "s";
		}
		String plural = root + post;
		if (upperFirst) {
			return plural.substring(0,1).toUpperCase() + plural.substring(1);
		} else {
			return plural;
		}
	}
	
}