import combinatorialSolver.Permutation;
import database.Database;

/* Class containing main routines of the padding scheme */
public class pbe {

	public static void main(String[] args) {
		
		
		Permutation sigma = new Permutation(new Integer[]{0,2,1});
		Database database = new Database(sigma);

		// int[] columns = {2, 13, 14, 15, 19, 22, 23, 249, 250};
		int[] columns = {2, 14, 15, 19, 22, 23, 249, 250};
		database.readDB("PUDF_base1q2010_tab.txt", columns);
		
		database.getEncodings();
		database.suppress(0, 27);
		database.encryption();
		database.padding();
		
		database.showMergeSizes();
		//database.showMarginalCounts(4,6,7);
		//database.showMarginalCounts(columns.length-2, columns.length-1);
		database.showExpansion();
		database.showAlternativeExpansion();	
	}
}
