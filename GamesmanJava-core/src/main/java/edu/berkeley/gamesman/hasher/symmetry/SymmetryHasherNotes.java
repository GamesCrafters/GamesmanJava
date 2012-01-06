/**
 * User must provide us with a set of sets of digits that represent independent 
 * gameboards with high symmetry counts
 * We are splitting up the gameboard into independent partitions and we want 
 * to have the highest reduction in board positions.
 * We assume the user provides an equal function for his board which tells 
 * us whether two positions are equal 
 *
 */

package edu.berkeley.gamesman.hasher.symmetry;

/**
 * @author grunsab
 *
 */

/*Write something that finds the symmetries on that partition
 * (Need to convert python symmetry finder to that)
 * 
 * Iterates over all the asymmetric positions (on just that partition) and adds
 * an offset to the offset table. Call genhasher numhashs to calculate offset
 * because adding that on. Numhashs is on the rest of the board.
 * 
 * Basically given a board partitions
 * X__X
 * ____
 * ____
 * X__X
 * we need to create a offset table which means to say that say you have
 *_O_X     0
 *_X_X    30
 *__X0    60
 *__OX   100
 *
 *then the positions represent the asymmetric partitions and the number 
 *between two positions represents the number of positions that start 
 *with the partition position.
 *So for instance there are 30 positions that start with _0_X, etc.
 *We need to create the offset table as a list. So I already have the list of 
 *asymmetric positions.X
 *TO get this number I use the genhasher that will be inputed. So as input to 
 *SymmetryHasher will be a genhasher for the game that wants to be hashed, and 
 *I will ask genhasher number of Positions with this starting position, and
 *it will output to me the number of positions for something like _O_X . 
 *Then I will implement two methods hash and unhash.
 *Hash will take a position of arbitrary nature, and will rotate or flip the board
 *to reach one of the asymmetric positions (minus those pieces that are not 
 *part of the partition used for symmetric finding).Then we look up that position
 *in the offset table and subtract it, and then put that in the hasher with
 *it's offset code.
 *To do this the developer needs to provide us with how to flip and rotate boards
 *and we need to provide an interface for them to do this.
 *To unhash positions we need to take a number that is our hashcode, 
 *and binary search to our range. So if we have 77 we see that that's between
 *60 and 100, so that position must have starting positons __XO. We then subtract
 *60 from 77 to get that our genHash code is 17. Then we unhash that to get the 
 *positions for the rest of the pieces of the board, and return the total position.
 *
 *Note that the genHasher we are given with the board must be different from the 
 *one we use, which requires a modificiation with the genHasher which David will
 *provide, basically making the second genHasher have fewer positions to consider
 *(since the offset table takes care of the rest of the board positions).
 *
 *Major TO-Do is the interface for the developer, and creating the offset table
 *which is organzing this table from low to high. 
 *
 */

