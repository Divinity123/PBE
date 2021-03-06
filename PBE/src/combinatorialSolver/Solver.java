package combinatorialSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.NumAttribute;

public class Solver {

	private List<NumAttribute> iterator;
	private List<Integer> iterator2;
	private Integer[] colSizes;
	
	private Integer[] attributeSizes;
	
	private Integer assignmentNumber = 0;
	private Integer[] assignment;
	private Integer[] assignmentSizes;
	
	private Integer[] columnGroup;
	private Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
	private List<Integer[]> trace;
	
	
	/* class constructor */
	public Solver(List<NumAttribute> iterator, Integer[] colSizes)
	{
		this.iterator = iterator.stream().collect(Collectors.toList());
		
		this.colSizes = new Integer[2];
		for (int ii = 0; ii < colSizes.length; ii++)
			this.colSizes[ii] = colSizes[ii];
		
		assignment = new Integer[colSizes[0] * colSizes[1]];
		assignmentSizes = new Integer[colSizes[1]];
		
		attributeSizes = new Integer[colSizes[0] * colSizes[1]];
		for (int ii = 0; ii < attributeSizes.length; ii++)
			attributeSizes[ii] = 0;
		for (NumAttribute att: iterator)
			attributeSizes[att.getVal()] = att.getCount();
		
		columnGroup = new Integer[colSizes[1]];
		trace = new ArrayList<Integer[]>();
	}
	
	
	/* main method to solve the problem */
	public void solve()
	{
		/* for first round, we have to:
		 * 1. determine the set of columns for one complete square block
		 * 2. determine permutation used for the rest of the algorithm
		 * 3. determine padded size for each assignment number
		 * More constraint: completely fix column groups */
		
		/* construct feasible set and trace row for first round and solve */
		List<List<Integer>> feasibleSet = getFeasibleSet();
		addTrace();
		
		List<Integer> firstGroup = solveRound(feasibleSet);
		assignmentSizes[0] = attributeSizes[firstGroup.get(0)];
		columnGroup = getColumnGroup(firstGroup);
		assignmentNumber++;
		
		//System.out.println(Arrays.toString(firstGroup.toArray()));
		/* remove items from the iterator */
		for (int ii = 0; ii < firstGroup.size(); ii++)
		{
			for (int jj = 0; jj < iterator.size(); jj++)
			{
				if (firstGroup.get(ii) == iterator.get(jj).getVal())
				{
					iterator.remove(jj);
					break;
				}
			}
		}
		
		/* build an index lookup table for the column group */
		for (int idx = 0; idx < columnGroup.length; idx++)
			indexMap.put(columnGroup[idx], idx);
		
		/* update trace */
		for (int ii = 0; ii < colSizes[1]; ii++)
			trace.get(0)[indexMap.get(columnGroup[ii])] = separateAttribute(firstGroup.get(ii))[1];
		addTrace();
		
		/* solve remaining rounds of the first group */
		for (int iteration = 1; iteration < colSizes[1]; iteration++)
		{
			feasibleSet = getFeasibleSet(columnGroup);
			List<Integer> group = solveRound(feasibleSet);
			assignmentSizes[assignmentNumber] = attributeSizes[group.get(0)];
			assignmentNumber++;
			
			/* remove items from the iterator */
			for (int ii = 0; ii < group.size(); ii++)
			{
				for (int jj = 0; jj < iterator.size(); jj++)
				{
					if (group.get(ii) == iterator.get(jj).getVal())
					{
						iterator.remove(jj);
						break;
					}
				}
			}
			
			/* update trace */
			addTrace();
			
			//System.out.println(Arrays.toString(group.toArray()));
		}
		
		/* make assignments to the remaining columns according to the permutation */
		repeatPermutation();
	}
	
	
	/* core method to find a set of permutations in the problem space */
	private List<Integer> solveRound(List<List<Integer>> feasibleSet)
	{
		List<Integer> result = new ArrayList<Integer>();
		reduceFeasibleIterator(feasibleSet);
			
		for (int iteration = 0; iteration < colSizes[1]; iteration++)
		{
			/* take the first feasible attribute from the iterator */
			Integer currentAttribute = -1;
			for (int ii = 0; ii < iterator2.size(); ii++)
			{
				currentAttribute = iterator2.get(ii);
				Integer[] currentAttributes = separateAttribute(currentAttribute);
				
				/* update trace */
				if (assignmentNumber > 0)
					trace.get(assignmentNumber)[indexMap.get(currentAttributes[0])] = currentAttributes[1];
				
				if (checkFeasible() == true)
				{
					
					/* make assignment */
					assignment[currentAttribute] = assignmentNumber;
					result.add(currentAttribute);
					
					/* update feasible set and remove elements from iterator */
					for (int col = 0; col < 2; col++)
						for (int idx = 0; idx < feasibleSet.get(col).size(); idx++)
							if (currentAttributes[col] == feasibleSet.get(col).get(idx))
								feasibleSet.get(col).remove(idx);
					reduceFeasibleIterator2(feasibleSet);
					break;
				}
				else
				{
					trace.get(assignmentNumber)[indexMap.get(currentAttributes[0])] = -1;
					currentAttribute = -1;
				}
			}
		}
		
		return result; 
	}
	
	
	/* method used to check if the current state (trace) is feasible */
	private boolean checkFeasible()
	{
		/* trivially true for the first row */
		if (assignmentNumber == 0)
			return true;
		
		/* check if the columns are occupied */
		boolean[] occupied = new boolean[colSizes[1]];
		for (int ii = 0; ii < colSizes[1]; ii++)
			occupied[ii] = false;
		for (int ii = 0; ii < colSizes[1]; ii++)
			if (trace.get(assignmentNumber)[ii] >= 0)
				occupied[ii] = true;
		
		/* if all columns are occupied, return true */
		int occupiedSize = 0;
		for (int ii = 0; ii < colSizes[1]; ii++)
			if (occupied[ii] == true)
				occupiedSize++;
		if (occupiedSize == colSizes[1])
			return true;
		
		
		/* for all un-occupied entries, obtain the set of feasible solutions */
		List<List<Integer>> feasibleSolution = new ArrayList<List<Integer>>();
		for (int ii = 0; ii < colSizes[1]; ii++)
		{
			if (occupied[ii] == false)
			{
				List<Integer> feasibleSolutionCol = new ArrayList<Integer>();
				for (int sol = 0; sol < colSizes[1]; sol++)
				{
					boolean add = true;
					
					for (int jj = 0; jj < colSizes[1]; jj++)
						if (trace.get(assignmentNumber)[jj] == sol)
							add = false;
					
					for (int jj = 0; jj < assignmentNumber; jj++)
						if (trace.get(jj)[ii] == sol)
							add = false;
					
					if (add == true)
						feasibleSolutionCol.add(sol);
				}
				
				/* if any entry contains no feasible solution, return false immediately */
				if (feasibleSolutionCol.isEmpty() == true)
					return false;
				feasibleSolution.add(feasibleSolutionCol);
			}
		}
		
		/* for every subset of entries, there should be at least as many attributes to pick from */
		for (int ii = 0; ii < Math.pow(2, feasibleSolution.size()); ii++)
		{
			/* figure out the corresponding subset */
			boolean[] pick = new boolean[feasibleSolution.size()];
			int pickSize = 0;
			for (int jj = 0; jj < feasibleSolution.size(); jj++)
			{
				if ((((int)(ii / Math.pow(2, feasibleSolution.size() - jj - 1))) % 2) == 0)
					pick[jj] = false;
				else
				{
					pick[jj] = true;
					pickSize++;
				}
			}
			
			/* compute span of the subset */
			Set<Integer> span = new HashSet<Integer>();
			for (int jj = 0; jj < feasibleSolution.size(); jj++)
				if (pick[jj] == true)
					for (int kk = 0; kk < feasibleSolution.get(jj).size(); kk++)
						span.add(feasibleSolution.get(jj).get(kk));
			
			if (span.size() < pickSize)
				return false;
		}
		
		return true;
	}
	
	
	/* core method to copy permutation to other columns */
	private void repeatPermutation()
	{
		/* put the set of occupied columns into a set */
		Set<Integer> occupiedColumns = new HashSet<Integer>();
		for (int ii = 0; ii < columnGroup.length; ii++)
			occupiedColumns.add(columnGroup[ii]);
		
		/* construct a list for the un-occupied columns */
		List<Integer> unoccupiedColumns = new ArrayList<Integer>();
		for (int ii = 0; ii < colSizes[0]; ii++)
			if (occupiedColumns.contains(ii) == false)
				unoccupiedColumns.add(ii);
		
		
		/* choose the column with the lowest cost of padding repetitively to fill the rest of the columns */
		for (int ii = 0; ii < columnGroup.length; ii++)
		{
			for (int jj = 1; jj < colSizes[0] / colSizes[1]; jj++)
			{
				int idx = jj * columnGroup.length + ii;
				
				//int idx = findBestColumn(unoccupiedColumns, ii);
				
				for (int kk = 0; kk < colSizes[1]; kk++)
				{
					Integer attribute = mergeAttributes(idx, kk);
					Integer reference = mergeAttributes(ii, kk);
					//Integer attribute = mergeAttributes(unoccupiedColumns.get(idx), kk);
					//Integer reference = mergeAttributes(columnGroup[ii], kk);
					assignment[attribute] = assignment[reference];
				}
				//unoccupiedColumns.remove(idx);
			}
		}
	}

	
	/* method to compute cost of each fit, and return the column index with the lowest cost */
	private Integer findBestColumn(List<Integer> unoccupiedColumns, Integer referenceIdx)
	{
		int minimalIdx = 0;
		int minimalCost = Integer.MAX_VALUE;
		
		for (int idx = 0; idx < unoccupiedColumns.size(); idx++)
		{
			/* compute cost of assignment */
			int cost = 0;
			for (int col = 0; col < colSizes[1]; col++)
			{
				Integer currentAttribute   = mergeAttributes(unoccupiedColumns.get(idx), col);
				Integer referenceAttribute = mergeAttributes(columnGroup[referenceIdx], col);
				cost += (assignmentSizes[assignment[referenceAttribute]] - attributeSizes[currentAttribute]);
			}
			
			if (cost < minimalCost)
			{
				minimalCost = cost;
				minimalIdx = idx;
			}
		}
				
		return minimalIdx;
	}
	
	
	
	/* method to add a new row of trace */
	private void addTrace()
	{
		Integer[] row = new Integer[colSizes[1]];
		for (int ii = 0; ii < colSizes[1]; ii++)
			row[ii] = -1;
		trace.add(row);
	}
	
	
	
	/* methods to reduce iterator give feasible set, much easier without modulo operations */
	private void reduceFeasibleIterator(List<List<Integer>> feasibleSet)
	{
		iterator2 = new ArrayList<Integer>();
		
		/* use hash sets to represent feasible attributes (column wise) */
		Set<Integer> feasibleCol0 = new HashSet<Integer>();
		Set<Integer> feasibleCol1 = new HashSet<Integer>();
		for (int ii = 0; ii < feasibleSet.get(0).size(); ii++)
			feasibleCol0.add(feasibleSet.get(0).get(ii));
		for (int ii = 0; ii < feasibleSet.get(1).size(); ii++)
			feasibleCol1.add(feasibleSet.get(1).get(ii));
		
		/* perform elimination */
		for (int ii = 0; ii < iterator.size(); ii++)
		{
			Integer[] attributes = separateAttribute(iterator.get(ii).getVal());
			if ((feasibleCol0.contains(attributes[0].intValue()) == true) && (feasibleCol1.contains(attributes[1].intValue()) == true))
				iterator2.add(iterator.get(ii).getVal());
		}
	}
	
	
	private void reduceFeasibleIterator2(List<List<Integer>> feasibleSet)
	{
		List<Integer> iterator3 = new ArrayList<Integer>();
		
		/* use hash sets to represent feasible attributes (column wise) */
		Set<Integer> feasibleCol0 = new HashSet<Integer>();
		Set<Integer> feasibleCol1 = new HashSet<Integer>();
		for (int ii = 0; ii < feasibleSet.get(0).size(); ii++)
			feasibleCol0.add(feasibleSet.get(0).get(ii));
		for (int ii = 0; ii < feasibleSet.get(1).size(); ii++)
			feasibleCol1.add(feasibleSet.get(1).get(ii));
		
		/* perform elimination */
		for (int ii = 0; ii < iterator2.size(); ii++)
		{
			Integer[] attributes = separateAttribute(iterator2.get(ii));
			if ((feasibleCol0.contains(attributes[0].intValue()) == true) && (feasibleCol1.contains(attributes[1].intValue()) == true))
				iterator3.add(iterator2.get(ii));
		}
		
		iterator2 = iterator3.stream().collect(Collectors.toList());
	}
	
	
	/* methods to construct feasible sets */
	private List<List<Integer>> getFeasibleSet()
	{
		List<List<Integer>> feasibleSet = new ArrayList<List<Integer>>();
		
		/* add first column */
		List<Integer> feasibleSetCol0 = new ArrayList<Integer>();
		for (int feasible = 0; feasible < colSizes[1]; feasible++)
				feasibleSetCol0.add(feasible);
		feasibleSet.add(feasibleSetCol0);
			
		/* add second column */
		List<Integer> feasibleSetCol1 = new ArrayList<Integer>();
		for (int feasible = 0; feasible < colSizes[1]; feasible++)
			feasibleSetCol1.add(feasible);
		feasibleSet.add(feasibleSetCol1);

		return feasibleSet;
	}
	
	
	/* changed to full constraint */
	private List<List<Integer>> getFeasibleSet(Integer[] columnGroup)
	{
		List<List<Integer>> feasibleSet = new ArrayList<List<Integer>>();
		
		/* add first column */
		List<Integer> feasibleSetCol0 = new ArrayList<Integer>();
		for (int feasible = 0; feasible < colSizes[1]; feasible++)
			feasibleSetCol0.add(feasible);
		feasibleSet.add(feasibleSetCol0);
			
		/* add second column */
		List<Integer> feasibleSetCol1 = new ArrayList<Integer>();
		for (int feasible = 0; feasible < colSizes[1]; feasible++)
			feasibleSetCol1.add(feasible);
		feasibleSet.add(feasibleSetCol1);
		
		return feasibleSet;
	}
	
	
	/* method to get column group */
	private Integer[] getColumnGroup(List<Integer> group)
	{
		Integer[] colGroup = new Integer[colSizes[1]];
		for (int ii = 0; ii < group.size(); ii++)
		{
			Integer attribute0 = separateAttribute(group.get(ii))[0];
			colGroup[ii] = attribute0;
		}
		return colGroup;
	}
	
	
	/* method to obtain column-wise attributes */
	private Integer[] separateAttribute(Integer attribute)
	{
		return new Integer[]{attribute / colSizes[1], attribute % colSizes[1]};		
	}
	
	
	/* method to merge attributes */
	private Integer mergeAttributes(Integer upper, Integer lower)
	{
		return upper * colSizes[1] + lower;
	}
	
	
	/* method to return assignments */
	public Integer[] getAssignment()
	{
		return assignment;
	}
	

}
	
	
