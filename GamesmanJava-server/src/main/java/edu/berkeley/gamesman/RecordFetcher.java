package edu.berkeley.gamesman;


import java.util.List;

import org.apache.thrift.TException;

import edu.berkeley.gamesman.thrift.GamestateResponse;

public interface RecordFetcher {

	public List<GamestateResponse> getNextMoveValues(String board)
			throws TException;

	public GamestateResponse getMoveValue(String board) throws TException;

}
