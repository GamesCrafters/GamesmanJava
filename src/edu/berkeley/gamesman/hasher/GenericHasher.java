package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.DBEnum;
import edu.berkeley.gamesman.util.Util;

public final class GenericHasher extends Hasher {
	
	private DBEnum dbe;
	
	public void setGame(Game<char[][],?> game){
		super.setGame(game);
		Enum<?> e = game.possiblePositionValues();
		Util.assertTrue(e.getClass().isEnum());
		
		if(e instanceof DBEnum){
			dbe = (DBEnum)e;
		}else{
			dbe = new DBEnumWrapper(e);
		}
	}
	
	@Override
	public BigInteger hash(char[][] board) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[][] unhash(BigInteger hash) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private class DBEnumWrapper implements DBEnum {

		@SuppressWarnings("unchecked")
		List<? extends Enum> values;
		
		public DBEnumWrapper(Enum<?> e){
			values = Arrays.asList(e.getClass().getEnumConstants());
		}
		
		public int value() {
			Util.warn("DBEnumWrapper.value() called :(");
			return -1;
		}
		
		public int value(Enum<?> e){
			return values.indexOf(e);
		}

		public int maxValue() {
			return values.size()-1;
		}
		
	}
	
}
