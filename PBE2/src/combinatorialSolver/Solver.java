package combinatorialSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import database.NumAttribute;

public class Solver {

	private List<NumAttribute> iterator;
	private Integer[] colSizes;
	private Integer blocks;
	
	private Integer[] attributeSizes;
	
	private Integer assignmentNumber = 0;
	private Integer[] assignment;
	private Integer[] assignmentSizes;
	
	private Permutation sigma;
	private boolean[] rowCheck;
	private List<Integer[]> assignmentGroup;
	
	private List<Integer[]> trace;
	
	/* class constructor */
	public Solver(List<NumAttribute> iterator, Integer[] colSizes, Permutation sigma)
	{
		this.iterator = iterator.stream().collect(Collectors.toList());
		
		this.colSizes = new Integer[2];
		for (int ii = 0; ii < colSizes.length; ii++)
			this.colSizes[ii] = colSizes[ii];
		
		/* initalize assignment groups */
		this.blocks = colSizes[1] / sigma.size();
		assignmentGroup = new ArrayList<Integer[]>();
		for (int ii = 0; ii < this.blocks; ii++)
		{
			Integer[] assignmentBlock = new Integer[sigma.size()];
			assignmentGroup.add(assignmentBlock);
			for (int jj = 0; jj < sigma.size(); jj++)
				assignmentGroup.get(ii)[jj] = ii*sigma.size() + jj;
		}
		
		
		/* Initialise variables associated to assignments themselves */
		assignment = new Integer[colSizes[0] * colSizes[1]];
		assignmentSizes = new Integer[colSizes[1]];
		for (int ii = 0; ii < assignmentSizes.length; ii++)
			assignmentSizes[ii] = 0;
		
		
		attributeSizes = new Integer[colSizes[0] * colSizes[1]];
		for (int ii = 0; ii < attributeSizes.length; ii++)
			attributeSizes[ii] = 0;
		for (NumAttribute att: iterator)
			attributeSizes[att.getVal()] = att.getCount();
		
		this.sigma = sigma;
		
		rowCheck = new boolean[colSizes[1]];
		for (int ii = 0; ii < rowCheck.length; ii++)
			rowCheck[ii] = false;
		
		trace = new ArrayList<Integer[]>();
	}

	
	/* main method to find a solution */
	public void solve()
	{
		for (int blockNumber = 0; blockNumber < blocks; blockNumber++)
		{
			/* find assignment for first row in the block */
			Integer[] colAssignment = getColAssignment(blockNumber); // get column index only
			Integer[] rowAssignment = getRowAssignment(colAssignment); // use repetition for the moment, definitey can do better
			
			/* solve for the next block */
			for (int jj = 0; jj < sigma.size(); jj++)
			{
				/* find a row */
				Integer rowIdx = findNextRow(rowAssignment);
				
				/* commit assignments */
				commitAssignments(rowIdx, rowAssignment);
				
				/* get next permutation */
				rowAssignment = getNextRowAssignment(rowAssignment);
			}
		}
	}
	
	
	/* method to get row assignment for the next assignment group, not optimal solution */
	private Integer[] getColAssignment(Integer blockNumber)
	{
		/* add trace */
		addTrace();
		
		/* get next row that does not have assignment yet */
		int rowIdx = 0;
		for (rowIdx = 0; rowIdx < colSizes[1]; rowIdx++)
			if (rowCheck[rowIdx] == false)
				break;
		
		/* construct an iterator for un-assigned columns */
		List<Integer> iterator = new ArrayList<Integer>();
		for (int col = 0; col < colSizes[0] / sigma.size(); col++)
			iterator.add(col);
		
		/* construct an iterator for assignment group */
		Integer[] groupSizes = new Integer[colSizes[1]/sigma.size()];
		for (int ii = 0; ii < groupSizes.length; ii++)
		{
			groupSizes[ii] = 0;
			for (int jj = 0; jj < sigma.size(); jj++)
				groupSizes[ii] += assignmentSizes[assignmentGroup.get(ii)[jj]];
		}
		int[] groupIterator = IntStream.range(0, groupSizes.length).boxed().sorted((i,j) -> groupSizes[i].compareTo(groupSizes[j])).mapToInt(ele -> ele).toArray();
		Collections.reverse(Arrays.asList(groupIterator));
		
		
		/* make assignments, this is not optimal */
		for (int ii = 0; ii < groupIterator.length; ii++)
		{
			for (int repeat  = 0; repeat < colSizes[0]/colSizes[1]; repeat++)
			{
				int minColIdx = 0;
				int minCost   = Integer.MAX_VALUE;
				
				for (int idx = 0; idx < iterator.size(); idx++)
				{
					/* compute cost */
					int cost = 0;
					for (int jj = 0; jj < sigma.size(); jj++)
						cost -= attributeSizes[iterator.get(idx)*sigma.size() + jj];
					
					/* if the cost is less than the current minimum and the assignment is feasible, commit assignment */
					if (cost < minCost)
					{
						trace.get(blockNumber)[iterator.get(idx)] = groupIterator[ii];
						
						if (isFeasible() == true)
						{
							minCost = cost;
							minColIdx = idx;
						}
						else
						{
							trace.get(blockNumber)[iterator.get(idx)] = -1;
						}
					}
				}
				
				/* remove column idx from iterator */
				iterator.remove(minColIdx);
				
				/* update group size */
				if (-minCost > groupSizes[ii])
					groupSizes[ii] = -minCost;
			}
		}
		

		return trace.get(blockNumber);
	}
	
	
	/* method to check if current trace is feasible */
	private boolean isFeasible()
	{
		int currentRow = trace.size() - 1;
		
		/* check if the current trace has anything in common with previous ones in each column */
		for (int ii = 0; ii < currentRow; ii++)
			for (int col = 0; col < colSizes[0]/sigma.size(); col++)
				if (trace.get(currentRow)[col] == trace.get(ii)[col])
					return false;
		
		/* compute how many times each assignment number has been used */
		int[] usedSizes = new int[colSizes[1]/sigma.size()];
		for (int ii = 0; ii < usedSizes.length; ii++)
			usedSizes[ii] = 0;
		for (int ii = 0; ii < trace.get(currentRow).length; ii++)
			if (trace.get(currentRow)[ii] >= 0)
				usedSizes[trace.get(currentRow)[ii]]++;
		
		/* if any size goes over the permitted size, reject solution */
		for (int ii = 0; ii < usedSizes.length; ii++)
			if (usedSizes[ii] > colSizes[0]/colSizes[1])
				return false;	
		
		/* for columns not occupied, find the set of feasible solutions */
		List<List<Integer>> feasibleColumns = new ArrayList<List<Integer>>();
		for (int assign = 0; assign < colSizes[1]/sigma.size(); assign++)
		{
			List<Integer> feasibleColumnsAtt = new ArrayList<Integer>();
			for (int col = 0; col < colSizes[0]/sigma.size(); col++)
			{
				boolean addToList = true;
				
				/* check if the column is occupied by a differet value */
				if ((trace.get(currentRow)[col] != -1) && (trace.get(currentRow)[col] != assign))
					addToList = false;
				
				/* no rows on top should contain the same value */
				for (int ii = 0; ii < currentRow; ii++)
					if (trace.get(ii)[col] == assign)
						addToList = false;
				
				if (addToList == true)
					feasibleColumnsAtt.add(col);
			}
			feasibleColumns.add(feasibleColumnsAtt);
		}
		
		
		/* check that for any combination of columns, there is a solution */
		for (int ii = 0; ii < Math.pow(2, feasibleColumns.size()); ii++)
		{
			/* figure out the corresponding subset */
			boolean[] pick = new boolean[feasibleColumns.size()];
			int pickSize = 0;
			for (int jj = 0; jj < feasibleColumns.size(); jj++)
			{
				if ((((int)(ii / Math.pow(2, feasibleColumns.size() - jj - 1))) % 2) == 0)
					pick[jj] = false;
				else
				{
					pick[jj] = true;
					pickSize++;
				}
			}
			
			/* compute span of the subset */
			Set<Integer> span = new HashSet<Integer>();
			for (int jj = 0; jj < feasibleColumns.size(); jj++)
				if (pick[jj] == true)
					for (int kk = 0; kk < feasibleColumns.get(jj).size(); kk++)
						span.add(feasibleColumns.get(jj).get(kk));
			
			/* remove assignments that are already made */
			Integer requiredSize = pickSize * colSizes[0] / colSizes[1];
			for (int jj = 0; jj < colSizes[1]/sigma.size(); jj++)
				if (pick[jj] == true)
					requiredSize -= usedSizes[jj];
			
			if (span.size() < requiredSize)
				return false;
		}
		return true;
	}
	
	
	/* get actual assignment for the first row, use repetition over columns for the moment */
	private Integer[] getRowAssignment(Integer[] colAssignment)
	{
		Integer[] result = new Integer[colSizes[0]];
		
		for (int col = 0; col < colAssignment.length; col++)
			for (int ii = 0; ii < sigma.size(); ii++)
				result[col*sigma.size()+ii] = assignmentGroup.get(colAssignment[col])[ii];
		
		return result;
	}
	
	
	/* method to find next row */
	private Integer findNextRow(Integer[] rowAssignment)
	{
		Integer minIdx = 0;
		Integer minCost = Integer.MAX_VALUE;
		
		for (int currentIdx = 0; currentIdx < colSizes[1]; currentIdx++)
		{
			if (rowCheck[currentIdx] == false)
			{
				Integer cost = findCost(currentIdx, rowAssignment);
				if (cost < minCost)
				{
					minCost = cost;
					minIdx = currentIdx;
				}
			}
		}
		
		return minIdx;
	}
	
	/* method to find cost of an assignment */
	private Integer findCost(Integer idx, Integer[] rowAssignment)
	{
		/* keep a local copy of assignment sizes */
		Integer[] assignmentSizesLocal = new Integer[assignmentSizes.length];
		for (int ii = 0; ii < assignmentSizes.length; ii++)
			assignmentSizesLocal[ii] = assignmentSizes[ii];
		
		for (int ii = 0; ii < rowAssignment.length; ii++)
		{
			Integer attribute = mergeAttributes(ii, idx);
			if (assignmentSizesLocal[rowAssignment[ii]] < attributeSizes[attribute])
				assignmentSizesLocal[rowAssignment[ii]] = attributeSizes[attribute];
		}
		
		/* compute cost */
		Integer cost = 0;
		for (int row = 0; row < colSizes[1]; row++)
		{
			if (rowCheck[row] == true)
			{
				for (int col = 0; col < colSizes[0]; col++)
				{
					Integer attribute = mergeAttributes(col, row);
					cost += assignmentSizes[assignment[attribute]] - attributeSizes[attribute];
				}
			}
		}
		
		return cost;
	}
	
	
	/* method to commit assignments to a row */
	private void commitAssignments(Integer rowIdx, Integer[] rowAssignment)
	{
		/* set row check to true */
		rowCheck[rowIdx] = true;
		
		for (int ii = 0; ii < rowAssignment.length; ii++)
		{
			Integer attribute = mergeAttributes(ii, rowIdx);
			
			/* write the assignments */
			assignment[attribute] = rowAssignment[ii];
			
			/* update assignment sizes */
			if (assignmentSizes[rowAssignment[ii]] < attributeSizes[attribute])
				assignmentSizes[rowAssignment[ii]] = attributeSizes[attribute];
		}
	}
	
	
	/* method to get next row assignment given the current one */
	private Integer[] getNextRowAssignment(Integer[] rowAssignment)
	{
		Integer[] result = new Integer[rowAssignment.length];
		
		for (int col = 0; col < colSizes[0] / sigma.size(); col++)
		{
			Integer buffer0 = rowAssignment[col*sigma.size()];
			Integer buffer1 = 0;
			
			for (int jj = 0; jj < sigma.size(); jj++)
				result[col*sigma.size() + sigma.successor(jj)] = rowAssignment[col*sigma.size() + jj];
		}
		
		return result;
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
	
	/* method to return the assignments found */
	public Integer[] getAssignment()
	{
		return this.assignment;
	}
	
	/* method to add a row to trace */
	private void addTrace()
	{
		Integer[] row = new Integer[colSizes[0]/sigma.size()];
		trace.add(row);
		
		int currentRow = trace.size()-1;
		for (int ii = 0; ii < trace.get(currentRow).length; ii++)
			trace.get(currentRow)[ii] = -1;
	}
}