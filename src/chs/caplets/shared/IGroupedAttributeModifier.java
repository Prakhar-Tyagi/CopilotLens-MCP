package chs.caplets.shared;

import chs.cof.logical.cable.ILogicObject;
import chs.common.IUID;
import chs.common.attr.IAttributeProvider;
import chs.utilities.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IGroupedAttributeModifier
{

	Pair<String, String> getAttributeNameValue();

	List<String> getExpectedChildAttributeValue(@NotNull ILogicObject logicObject);

	boolean isAcceptable(IGroupedAttributeModifier transferingFolderNode);

	Collection<IUID> recursiveGetAllChildObjects();

	<T extends IAttributeProvider> Map<String, Collection<T>> getTrailingValueForAttributeBelowThisLevelInTree(
			Class<T> classType);
}
