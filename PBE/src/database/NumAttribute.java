package database;

public class NumAttribute {
	
	/* main items in the object */
	private Integer attribute;
	private Integer count;
	
	public NumAttribute(Integer attribute, Integer count)
	{
		this.attribute = attribute;
		this.count     = count;
	}
	
	public void modifyCount(Integer count)
	{
		this.count = count;
	}
	
	public Integer getVal()
	{
		return this.attribute;
	}
	
	public Integer getCount()
	{
		return this.count;
	}
}
