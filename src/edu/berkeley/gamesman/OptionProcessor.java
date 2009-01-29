package edu.berkeley.gamesman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Utility class for processing of command line arguments
 * @author Steven Schlansker
 *
 */
public final class OptionProcessor {

	private OptionProcessor(){}
	
	private static class Option implements Comparable<Option> {
		String s,l,h,d;
		boolean p;
		int g;
		Option(String shortForm, String longForm, boolean takesParam, String helpString, String dfl){
			s = "-" + shortForm;
			l = "--" + longForm;
			p = takesParam;
			h = helpString;
			d = dfl;
			g = group;
		}
		
		String checkOpt(String[] args){
			boolean needParam = false;
			for(String opt : args){
				if(needParam) return opt;
				if(s.equals(opt) || l.equals(opt)){
					if(p) needParam = true;
					else return s;
				}
			}
			return d;
		}

		@Override
		public int compareTo(Option o) {
			return (o.g + o.l).compareTo(g + l);
		}
	}
	
	private static Hashtable<String,Option> opts = new Hashtable<String,Option>();
	private static String[] myArgs;
	protected static int group = 0;
	
	/**
	 * Initializes the global OptionProcessor with the given args
	 * @param args Arguments passed into Gamesman
	 * @see Gamesman#main(String[])
	 */
	public static void initializeOptions(String args[]){
		myArgs = args;
	}
	
	/**
	 * Informs the option processor about a new option that you would like to acceptå
	 * The processor will now accept either -short or --long-opt
	 * @param shortForm Short option (e.g. x)
	 * @param longForm Long option (e.g. with-x)
	 * @param takesParam Does the option require a parameter?
	 * @param helpString Descriptive string to output with --help
	 */
	public static void acceptOption(String shortForm, String longForm, boolean takesParam, String helpString){
		Option newopt = new Option(shortForm,longForm,takesParam,helpString,null);
		
		Util.assertTrue(!opts.containsKey(shortForm) && !opts.containsKey(longForm));
		
		opts.put(shortForm, newopt);
		opts.put(longForm,newopt);
	}
	
	/**
	 * Informs the option processor about a new option that you would like to acceptå
	 * The processor will now accept either -short or --long-opt
	 * @param shortForm Short option (e.g. x)
	 * @param longForm Long option (e.g. with-x)
	 * @param takesParam Does the option require a parameter?
	 * @param helpString Descriptive string to output with --help
	 * @param dfl Default value if not specified
	 */
	public static void acceptOption(String shortForm, String longForm, boolean takesParam, String helpString, String dfl){
		Option newopt = new Option(shortForm,longForm,takesParam,helpString,dfl);
		
		Util.assertTrue(!opts.contains(shortForm) && !opts.contains(longForm));
		
		opts.put(shortForm, newopt);
		opts.put(longForm,newopt);
	}
	
	/**
	 * Defines a distinct option group (solely for purposes of sorting the help string)
	 */
	public static void nextGroup(){
		group++;
	}
	
	/**
	 * Determines if an option was specified on the command line
	 * @param opt Short or long form of option (must be same as in acceptOption)
	 * @return Parameter string if the option had a parameter, Non-null String if the option had no parameter, null if no parameter specified
	 * @see #acceptOption
	 */
	public static String checkOption(String opt){
		return opts.get(opt).checkOpt(myArgs);
	}

	/**
	 * Print help strings for all options declared so far
	 */
	public static void help(){
		ArrayList<Option> sortedOpts = new ArrayList<Option>(new HashSet<Option>(opts.values()));
		Collections.sort(sortedOpts);
		Collections.reverse(sortedOpts);
		
		int maxs = 0, maxl = 0;
		
		for(Option opt : sortedOpts){
			maxs = Math.max(opt.s.length(), maxs);
			maxl = Math.max(opt.l.length(), maxl);
		}
		
		int curg = -1;
		
		for(Option opt : sortedOpts){
			if(opt.g != curg){
				curg = opt.g;
				System.out.println();
			}
			System.out.printf("%"+maxs+"s,  %"+maxl+"s:   %s\n", opt.s,opt.l,opt.h);
		}
	}
}
