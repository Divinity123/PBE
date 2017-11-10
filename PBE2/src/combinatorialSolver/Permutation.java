package combinatorialSolver;

public class Permutation {

	private Integer[] successor;
	
	public Permutation(Integer[] perm)
	{
		successor = new Integer[perm.length];
		for (int ii = 0; ii < perm.length - 1; ii++)
			successor[perm[ii]] = perm[ii+1];
		successor[perm[perm.length-1]] = perm[0];
	}
	
	public Integer size()
	{
		return this.successor.length;
	}
	
	public Integer successor(Integer val)
	{
		return this.successor[val];
	}
}
