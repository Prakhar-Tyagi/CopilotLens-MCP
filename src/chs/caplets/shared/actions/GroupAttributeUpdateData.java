package chs.caplets.shared.actions;

public class GroupAttributeUpdateData
{

	private String name;
	private String value;
	private boolean isAttribute;

	public GroupAttributeUpdateData(String name, String value, boolean isAttribute)
	{
		this.name = name;
		this.value = value;
		this.isAttribute = isAttribute;
	}

	public GroupAttributeUpdateData(GroupAttributeUpdateData data)
	{
		name = data.name;
		value = data.value;
		isAttribute = data.isAttribute;
	}

	public String getName()
	{
		return name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public boolean isAttribute()
	{
		return isAttribute;
	}
}
