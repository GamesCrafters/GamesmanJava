package edu.berkeley.gamesman;

import java.util.Map;

public interface Opener {
	public RecordFetcher addDatabase(Map<String, String> params, String game,
			String filename);
}
