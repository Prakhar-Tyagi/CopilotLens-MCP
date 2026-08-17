package chs.caplets.shared;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface IGroupAttributeAddEntry
{
	String getDisplayName();

	String getName();

	Collection<String> seperator();

	void addSeperator(String givenSeperator);

	void setSeperator(@Nullable Collection<String> seperators);

	boolean isAttribute();
}
