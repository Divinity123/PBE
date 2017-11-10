package database;

import java.util.List;

import combinatorialSolver.Permutation;

public interface DB_operations {
	
	/* method to read from file */
	boolean readDB(String fileName, int[] columns);
	
	/* method to get histogram given a DB, either taking the full row as an attribute or a specific column */
	void getHistogram();
	void getHistogramColumn(Integer column);
	List<NumAttribute> getHistogramColums(Integer column1, Integer column2);

	/* method to find minimal packing for each column */
	int getMinimumPack(Integer[] counts);
	
	/* method to find target merging sizes for each column */
	int[] getMergeSize(int[] minSizes, int[] maxSizes, Integer base);
	
	/* method to get encryptions for each attribute */
	void getEncodings();
	
	/* method to encrypt the database and store results in the encrypted DB */
	void encryption();
	
	/* core padding function */
	void padding();
	
	/* method to show marginal counts */
	void showMarginalCounts(Integer firstCol, Integer secondCol);
}