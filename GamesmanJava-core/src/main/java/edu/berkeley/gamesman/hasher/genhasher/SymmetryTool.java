package edu.berkeley.gamesman.hasher.genhasher;

/**
 * @author choochootrain
 *
 * @param <S>
 */
public class SymmetryTool<S extends GenState> {
	
	public void rotateToBaseState(GenHasher<S> hasher, S state, S rotateTo,
			int numChanged) {
		
		
		assert hasher.validTest(rotateTo);
	}

}
