package database;

public class Attribute {
	
	/* main items in the object */
	private String attribute;
	private Integer count;
	
	public Attribute(String attribute, Integer count)
	{
		this.attribute = attribute;
		this.count     = count;
	}
	
	public void modifyCount(Integer count)
	{
		this.count = count;
	}
	
	public String getString()
	{
		return this.attribute;
	}
	
	public Integer getCount()
	{
		return this.count;
	}
}
