package net.sf.openrocket.motor;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares two motors' common names.  The motors are ordered first
 * by their motor class and second by their average thrust.
 * Based on DesignationComparator written by Sampo Niskanen <sampo.niskanen@iki.fi>
 * 
 */
public class CommonNameComparator implements Comparator<String> {
	private static final Collator COLLATOR;
	static {
		COLLATOR = Collator.getInstance(Locale.US);
		COLLATOR.setStrength(Collator.PRIMARY);
	}
	
	/*
	 * Regexp to parse the multitude of designations.  Supported types:
	 *
	 *   Micro...
	 *   1/4A4...
	 *   1/2A4...
	 *   A4...
	 *   
	 * Capture groups:
	 *   1 = divisor number for 1/2A, 1/4A etc, otherwise null
	 *   2 = impulse class letter
	 *   3 = average thrust
	 *   4 = stuff after thrust number
	 */
	private Pattern pattern =
			Pattern.compile("^(1/([1-8]))?([A-Z])([0-9]+)(.*?)$");
	
	@Override
	public int compare(String o1, String o2) {
		int value;
		Matcher m1, m2;

		// If one is a Micro-Maxx, it has lowest value
		if (o1.toUpperCase().startsWith("MICRO")) {
			if (o2.toUpperCase().startsWith("MICRO")) {
				return 0;
			} else {
				return -1;
			}
		} else if (o2.toUpperCase().startsWith("MICRO")) {
			return 1;
		}

		System.out.println("o1: " + o1);
		System.out.println("o2: " + o2);

		m1 = pattern.matcher(o1);
		m2 = pattern.matcher(o2);

		System.out.println("m1: " + m1.group());
		System.out.println("m2: " + m2.group());
		
		if (m1.find() && m2.find()) {
			
			String o1Class = m1.group(2);
			int o1Thrust = Integer.parseInt(m1.group(3));
			String o1Extra = m1.group(4);
			
			String o2Class = m2.group(2);
			int o2Thrust = Integer.parseInt(m2.group(3));
			String o2Extra = m2.group(4);
			
			// 1. Motor class
			if (o1Class.equalsIgnoreCase("A") && o2Class.equalsIgnoreCase("A")) {
				//  1/2A and 1/4A comparison
				String sub1 = m1.group(1);
				String sub2 = m2.group(1);
				
				if (sub1 != null || sub2 != null) {
					if (sub1 == null)
						sub1 = "1";
					if (sub2 == null)
						sub2 = "1";
					value = -COLLATOR.compare(sub1, sub2);
					if (value != 0)
						return value;
				}
			}
			value = COLLATOR.compare(o1Class, o2Class);
			if (value != 0)
				return value;
			
			// 2. Average thrust
			if (o1Thrust != o2Thrust)
				return o1Thrust - o2Thrust;
			
			// 3. Extra modifier
			return COLLATOR.compare(o1Extra, o2Extra);
			
		} else {
			
			// Not understandable designation, simply compare strings
			return COLLATOR.compare(o1, o2);
		}
	}
}
