package edu.berkeley.gamesman.hasher;
public class MMHasher 
{	

	public static void main(String[] args) 
	{
		char[] input = "XX OXO".toCharArray();
		MMBoard test = new MMBoard(input);
		test.debugPrint();
	}
	public static long hash(char[] pieces)
	{
		MMBoard board = new MMBoard(pieces);
		return board.hashX;
	}
	public static void unhash(long hash, char[] pieces, int numX, int numO)
	{
		//TODO
	}
}

class MMBoard
{
	MMBoardElement first;	// first element of the board
	long xMajorHash;
	long xMinorHash;
	long oMajorHash;
	long oMinorHash;
	long hashX;
	long hashO;
	MMBoard(char[] pieces)
	{
		xMajorHash=0;
		xMinorHash=0;
		oMajorHash=0;
		oMinorHash=0;
		int numX=0;
		int numO=0;
		int nonXCount=-1;
		int nonOCount=-1;
		
		MMBoardElement current=null;
		MMBoardElement last=null;
		MMBoardElement lastX=null;
		MMBoardElement lastO=null;
		
		for(int i=0; i<pieces.length; i++)
		{
			current = new MMBoardElement(pieces[i],i);
			if(i==0)
			{
				first = current;
			}
			if(last!=null)
			{
				last.next = current;
			}
			current.prev=last;
			current.prevX=lastX;
			current.prevO=lastO;
			
			if(pieces[i]=='X')
			{
				lastX=current;
				while(last!=null && last.nextX==null)
				{
					last.nextX=current;
					last=last.prev;
				}
				nonOCount++;
				numX++;
			}
			else if(pieces[i]=='O')
			{
				lastO=current;
				while(last!=null && last.nextO==null)
				{
					last.nextO=current;
					last=last.prev;
				}
				nonXCount++;
				numO++;
			}
			else
			{
				nonOCount++;
				nonXCount++;
			}
			
			current.numX=numX;
			current.numO=numO;
			current.nonXCount=nonXCount;
			current.nonOCount=nonOCount;
			
			current.xMajorHash=getPartialXMajorHash(current);
			current.xMinorHash=getPartialXMinorHash(current);			
			current.oMajorHash=getPartialOMajorHash(current);
			current.oMinorHash=getPartialOMinorHash(current);
			
			if(current.value=='X')
			{
				xMajorHash+=current.xMajorHash;
				xMinorHash+=current.xMinorHash;
			}
			else if(current.value=='O')
			{
				oMajorHash+=current.oMajorHash;
				oMinorHash+=current.oMinorHash;				
			}
			last = current;
		}
		hashX= xMajorHash*comb(nonXCount+1, numO)+oMinorHash;
		hashO= oMajorHash*comb(nonOCount+1, numX)+xMinorHash;
		
	}
	long getPartialXMajorHash(MMBoardElement e)
	{
		if(e.numX<=1)
		{
			return smallComb(e.index, e.numX);
		}
		return quickComb(e.prev.xMajorHash, e.prev.index, e.prev.numX, e.index, e.numX);
	}
	long getPartialXMinorHash(MMBoardElement e)
	{
		if(e.numX<=1)
		{
			return smallComb(e.nonOCount, e.numX);
		}
		return quickComb(e.prev.xMinorHash, e.prev.nonOCount, e.prev.numX, e.nonOCount, e.numX);
	}
	long getPartialOMajorHash(MMBoardElement e)
	{
		if(e.numO<=1)
		{
			return smallComb(e.index, e.numX);
		}
		return quickComb(e.prev.oMajorHash, e.prev.index, e.prev.numO, e.index, e.numO);
	}
	long getPartialOMinorHash(MMBoardElement e)
	{
		if(e.numO<=1)
		{
			return smallComb(e.nonXCount, e.numO);
		}
		return quickComb(e.prev.oMinorHash, e.prev.nonXCount, e.prev.numO, e.nonXCount, e.numO);
	}
	int comb(int n, int r)
	{
		int temp = n-r+1;
		int prod = 1;
		while (temp<=n)
		{
			prod *= temp;
			temp++;
		}
		temp=2;
		while(temp<=r)
		{
			prod /=temp;
			temp++;
		}
		return prod;

	}
	long smallComb(int n, int r)
	{
		if(n==0)
		{
			if(r==0)
			{
				return 1;
			}
			return 0;
		}
		return n;
	}
	long quickComb(long prev, int prevN, int prevR, int n, int r)
	{
		if(prevN==n)
		{
			return prev;
		}
		else
		{
			if(n==r)
			{
				return 1;
			}
			if(prevR==r)
			{
				return prev*n/(n-r);
			}
			else
			{
				return prev*n/r;
			}
		}
	}
	void debugPrint()
	{
		MMBoardElement temp = first;
		while(temp!=null)
		{
			System.out.println("Index "+temp.index);
			System.out.println("XMajorHash: "+temp.index+" C "+temp.numX+" = "+temp.xMajorHash);
			System.out.println("OMinorHash: "+temp.nonXCount+" C "+temp.numO+" = "+temp.oMinorHash);
			System.out.println("OMajorHash: "+temp.index+" C "+temp.numO+" = "+temp.oMajorHash);
			System.out.println("XMinorHash: "+temp.nonOCount+" C "+temp.numX+" = "+temp.xMinorHash);
			System.out.println("***********************");
			temp = temp.next;
		}
		
		System.out.println("XMajorHash: "+xMajorHash);
		System.out.println("OMinorHash: "+oMinorHash);
		System.out.println("Hash: "+hashX);
		System.out.println("==========");
		System.out.println("OMajorHash: "+oMajorHash);
		System.out.println("XMinorHash: "+xMinorHash);
		System.out.println("Hash: "+hashO);
	}
}

class MMBoardElement
{
	char value;				// the piece's char representation
	int index; 				// position of the piece in respect to the game
	
	int numX;				// number of X's seen including this piece
	int numO;				// number of O's seen including this piece
	int nonXCount;			// number of not X seen so far
	int nonOCount;			// number of not O seen so far
	
	long xMajorHash;
	long xMinorHash;
	long oMajorHash;
	long oMinorHash;
	
	MMBoardElement next;
	MMBoardElement prev;
	
	MMBoardElement nextX;
	MMBoardElement prevX;
	
	MMBoardElement nextO;
	MMBoardElement prevO;
	
	MMBoardElement(char value, int index)
	{
		this.value=value;
		this.index=index;
		next=null;
		prev=null;
		nextX=null;
		prevX=null;
		nextO=null;
		prevO=null;
	}
	
}

