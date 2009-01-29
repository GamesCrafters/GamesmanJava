package edu.berkeley.gamesman.core;

public enum PrimitiveValue {
	Undecided(0),
	Win(1),
	Lose(2),
	Tie(3);
	
	int value;
	PrimitiveValue(int v){
		value = v;
	}
}
