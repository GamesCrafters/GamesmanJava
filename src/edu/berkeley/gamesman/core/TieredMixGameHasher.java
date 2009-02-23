package edu.berkeley.gamesman.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author DNSpies
 * For a tiered game that hashes its own positions
 */
public abstract class TieredMixGameHasher extends MixGameHasher {
	/**
	 * Initialize game width/height
	 * 
	 * @param conf configuration
	 */
	public TieredMixGameHasher(Configuration conf) {
		super(conf);
		tiers = numberOfTiers();
		tierOffset = new BigInteger[tiers];
	}

	private BigInteger nextOffset = BigInteger.ZERO;
	private final BigInteger[] tierOffset;
	private final int tiers;

	@Override
	public Collection<BigInteger> validMoveHashes() {
		Collection<BigInteger> vmuh = moveHashesWithoutTiers();
		Collection<Integer> vmt = moveTiers();
		if(vmuh.size()!=vmt.size())
			new Exception("Hash/tier sizes do not match").printStackTrace();
		Iterator<BigInteger> vmuhit = vmuh.iterator();
		Iterator<Integer> vmtit = vmt.iterator();
		ArrayList<BigInteger> moveHashes = new ArrayList<BigInteger>(vmuh
				.size());
		while (vmuhit.hasNext()) {
			moveHashes.add(vmuhit.next().add(hashOffsetForTier(vmtit.next())));
		}
		return moveHashes;
	}

	private BigInteger hashOffsetForTier(int t) {
		return tierOffset[t];
	}

	@Override
	public BigInteger getHash() {
		return getHashWithoutTier().add(hashOffsetForTier(getTier()));
	}

	/**
	 * @return The tier of this position
	 */
	public abstract int getTier();

	/**
	 * Do not call this method.  It is slow and unnecessary
	 * 
	 * @return The last possible hash
	 */
	public final BigInteger lastHash() {
		BigInteger total=BigInteger.ZERO;
		setTier(0);
		for(int i=0;i<tiers;i++){
			nextTier();
			total=total.add(numHashesForTier());
		}
		return total.subtract(BigInteger.ONE);
	}

	@Override
	public boolean hasNext() {
		return hasNextPositionInTier() || hasNextTier();
	}

	/**
	 * @return Whether there's another position in this tier
	 */
	public boolean hasNextPositionInTier() {
		return getHashWithoutTier().compareTo(lastHashValueForTier()) < 0;
	}

	/**
	 * @return Whether there's a higher tier
	 */
	public boolean hasNextTier() {
		return getTier() < tiers;
	}

	@Override
	public void nextPosition() {
		if (hasNextPositionInTier())
			nextPositionInTier();
		else
			nextTier();
	}

	/**
	 * Cycle to next tier
	 */
	public abstract void nextTier();
	
	/**
	 * Cycle back to last tier
	 */
	public abstract void prevTier();

	/**
	 * Set board to tier
	 * 
	 * @param tier The tier
	 */
	public abstract void setTier(int tier);

	/**
	 * Return the first hashed value in a given tier
	 * 
	 * @param tier The tier we're interested in
	 * @return The first hash value that will be in this tier
	 */
	public final BigInteger getOffset(int tier) {
		return tierOffset[tier];
	}
	
	/**
	 * Create and store the offset of the current tier.
	 * @throws Exception Offset already initialized
	 */
	public final void storeOffset() throws Exception{
		if(tierOffset[getTier()]!=null)
			throw new Exception("Offset already initialized: "+getTier());
		tierOffset[getTier()]=nextOffset;
		nextOffset=nextOffset.add(numHashesForTier());
	}

	/**
	 * Return the last hash value a tier represents
	 * 
	 * @return The last hash that will be in the given tier
	 */
	public final BigInteger lastHashValueForTier() {
		return numHashesForTier().subtract(BigInteger.ONE);
	}
	
	/**
	 * Indicate the number of tiers that this game has (Note how this is
	 * distinct from the /last/ tier)
	 * 
	 * @return Number of tiers. Limited to java primitive int type for now
	 */
	public abstract int numberOfTiers();
	
	/**
	 * @return The hashes of all valid moves from this position (without the
	 *         tier)
	 */
	public abstract Collection<BigInteger> moveHashesWithoutTiers();
	
	/**
	 * @return The tiers of all valid moves from this position
	 */
	public abstract Collection<Integer> moveTiers();
	
	/**
	 * @return The hash of this position (without the tier)
	 */
	public abstract BigInteger getHashWithoutTier();
	
	/**
	 * Cycle to next position
	 */
	public abstract void nextPositionInTier();
	
	/**
	 * Return the number of hashes in this tier
	 * 
	 * @return Size of the tier
	 */
	public abstract BigInteger numHashesForTier();
}
