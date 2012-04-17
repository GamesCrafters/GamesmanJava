package edu.berkeley.gamesman.parallel;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;

public interface FlipRecord extends FixedLengthWritable {

	public void set(GameValue value);

	public void set(GameValue value, int remoteness);

	public void previousPosition(FlipRecord from);
}
