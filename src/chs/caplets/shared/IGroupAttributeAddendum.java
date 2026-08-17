package chs.caplets.shared;

import java.util.Collection;

public interface IGroupAttributeAddendum
{

	Collection<IGroupAttributeAddEntry> getChildAttributesConfigured();

	boolean setChildAttributes(Collection<IGroupAttributeAddEntry> attributes);
}
