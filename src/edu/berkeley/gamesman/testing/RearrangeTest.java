package edu.berkeley.gamesman.testing;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.ConnectGame;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.hasher.*;

import java.util.*;

public final class RearrangeTest {
	static ConnectGame gToCompareTo;
	static Record recordToTest;
	static Record recordToCompare;
	
	public static void main(String[] args) {
		Database dbBeingTested = Database.openDatabase(args[0]); 
		Database dbToCompareTo = Database.openDatabase(args[1]);
		Configuration confBeingTested = dbBeingTested.getConfiguration(); 
		Configuration confToCompareTo = dbToCompareTo.getConfiguration();
		
		//for testing Connections
		ConnectGame gBeingTested = (ConnectGame)confBeingTested.getGame(); 
		gToCompareTo = (ConnectGame) confToCompareTo.getGame();
		
		//Game gBeingTested = confBeingTested.getGame(); 
		//Game TTTgToCompareTo = confToCompareTo.getGame();
		
		recordToTest = new Record(confBeingTested);
		recordToCompare = new Record(confToCompareTo);
		
		compareDatabases(dbBeingTested,dbToCompareTo); 
		//compares RearrangeHasher/RearrangeSolver
		//database with DartboardHasher/TierSolver database
														
		//testGame(gBeingTested,TTTgToCompareTo,dbBeingTested,dbToCompareTo);
		//tests TicTacToe
	}
	
	//compares database solved by RearrangeHasher with DartboardHasher
	//prints locations with errors if any, and "complete" when done
	public static void compareDatabases(Database rearrangeD, Database testerD){
		DatabaseHandle dhTest = rearrangeD.getHandle();
		DatabaseHandle dhTester = testerD.getHandle();
		
		int numTiers = gToCompareTo.numberOfTiers();
		int boardSize = numTiers - 1;
		int currTier = 0;
		char[] board = new char[boardSize];
		
		while (currTier < numTiers){
			RearrangeHasher rh = new RearrangeHasher(boardSize);
			DartboardHasher dh = new DartboardHasher(boardSize, ' ', 'O', 'X');
			
			int numX = (currTier+1)/2;
			int numO = currTier/2;
			int numSpaces = boardSize - numX - numO;
			
			rh.setNums(numSpaces, numO, numX);
			dh.setNums(numSpaces, numO, numX);
			
			long hashesLeftInTier = gToCompareTo.numHashesForTier(currTier);
			while(hashesLeftInTier > 0){
				
				if(hashesLeftInTier%100000==0){
					System.out.println(hashesLeftInTier);
				}
				
				long rhhash = rh.getHash();
				rh.getCharArray(board);
				long dhhash = dh.hash(board);
				
				TierState rhPosition = new TierState(currTier,rhhash);
				TierState dhPosition = new TierState(currTier,dhhash);
				
				gToCompareTo.longToRecord(rhPosition, rearrangeD.getRecord(dhTest, gToCompareTo.stateToHash(rhPosition)),
						recordToTest); 
				gToCompareTo.longToRecord(dhPosition, testerD.getRecord(dhTester, gToCompareTo.stateToHash(dhPosition)),
					recordToCompare); 
				
				if(!recordToTest.equals(recordToCompare)){
					System.out.print("Board ");
					System.out.println(board);
					System.out.print(recordToTest);
					System.out.println(" Should be " + recordToCompare);
					System.out.println("RearrangeHash: " + rhhash +
							" DartboardHash: " + dhhash);
				}
				
				rh.next();
				hashesLeftInTier--;
			}
			currTier++;
		}
		
		System.out.println("complete");
		
	}

	//compares databases of a game solved by different hashes
	public static <S extends State> void testGame(Game<S> gTest, Game<S> gTester, Database dbTest, Database dbTester) {
		Stack <S> positionsToCheck=new Stack<S>();
		Stack <S> testingPositions=new Stack<S>();
		
		S position = gTest.startingPositions().iterator().next();
		S correctPosition = gTester.startingPositions().iterator().next();
		
		positionsToCheck.push(position);
		testingPositions.push(correctPosition);
		
		S currentPosition;
		S testPosition;

		Record storeRecordTest = gTest.newRecord();
		Record storeRecordTester = gTester.newRecord();
	
		DatabaseHandle dhTest = dbTest.getHandle();
		DatabaseHandle dhTester = dbTester.getHandle();
		
		int counter = 0;
		while(positionsToCheck.empty()){
			counter++;
			currentPosition = positionsToCheck.pop();
			testPosition = testingPositions.pop();
			
			if(counter%100000==0){
				System.out.println(currentPosition + " " + testPosition);
			}
			
			gTest.longToRecord(currentPosition, dbTest.getRecord(dhTest, gTest.stateToHash(currentPosition)),
					storeRecordTest); 
			gTester.longToRecord(testPosition, dbTester.getRecord(dhTester, gTester.stateToHash(testPosition)),
				storeRecordTester); 
			
			if(!storeRecordTest.equals(storeRecordTester)){ //comparing values
				System.out.print("Being tested: " + currentPosition + " " + testPosition);
				System.out.print(" Stored: " + storeRecordTest);
				System.out.println(" Should be: " + storeRecordTester);
			}

			Collection<Pair<String, S>> moves = gTest.validMoves(currentPosition);
			Collection<Pair<String, S>> testMoves = gTester.validMoves(testPosition);
			for (Pair<String, S> move : moves) {
				positionsToCheck.push(move.cdr);
			}
			for (Pair<String, S> move : testMoves) {
				testingPositions.push(move.cdr);
			}
		}
		System.out.println("complete");
	}
}
