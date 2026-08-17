/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.batchshare.AbstractShareRow;
import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.buildlist.IBuildListIterator;
import chs.common.IUID;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeType;
import chs.system.FactoryMgr;
import chs.utilities.IXMLTags;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Concrete implementation of {@link IBatchUnshareRow} representing a single row in the batch unshare table.
 * <p>
 * This class extends {@link AbstractShareRow} to provide data for redundant shared objects that are
 * candidates for unsharing in batch operations.
 */
public class BatchUnshareRow extends AbstractShareRow implements IBatchUnshareRow
{

	@NotNull private final ISharedObject m_sharedObject;
	@NotNull private final String m_objectUID;
	@NotNull private final IUID m_designUID;
	@NotNull private final ShareableEntityTypeEnum m_objectType;
	@NotNull private final String m_name;

	public BatchUnshareRow(@NotNull ISharedObject sharedObject, @NotNull String objectUID, @NotNull IUID designUID,
			@NotNull ShareableEntityTypeEnum objectType)
	{
		super(sharedObject.getProject().getDesignMgr().getDesignDescriptor(designUID));
		m_designUID = designUID;
		m_sharedObject = sharedObject;
		m_objectUID = objectUID;
		m_name = getObjectDisplayName();
		m_objectType = objectType;
	}

	@Override @Nullable public String getAttributeValue(@NotNull String attributeName)
	{
		IAttribute attr = m_sharedObject.getAttribute(attributeName);
		return attr != null ? attr.getAsString() : null;
	}

	@Override @Nullable public String getAttributeValue(@NotNull IAttributeType attribute)
	{
		return getAttributeValue(attribute.getName());
	}

	@NotNull private String getObjectDisplayName()
	{
		String name = StringUtils.nonNull(getAttributeValue(IXMLTags.NAME));
		String mateName = StringUtils.EMPTY_STRING;
		if (m_sharedObject instanceof ISharedConnector sharedConnector && sharedConnector.getType().isInline()) {
			Set<ISharedConnector> mates = sharedConnector.getMates();
			if (mates.size() == 1) {
				mateName = mates.iterator().next().getName();
			}
		}
		return StringUtils.isBlank(mateName) ? name : StringUtils.concatenate(name, StringUtils.COLON, mateName);
	}

	@Override @NotNull public String getName()
	{
		return m_name;
	}

	@Override @NotNull public IUID getObjectUID()
	{
		return FactoryMgr.getCommonFactory().constructUID(m_objectUID);
	}

	@NotNull public IUID getSharedObjectUID()
	{
		return m_sharedObject.getUID();
	}

	@Override @NotNull public IUID getDesignUID()
	{
		return m_designUID;
	}

	@Override @NotNull public ShareableEntityTypeEnum getObjectType()
	{
		return m_objectType;
	}

	@Override @Nullable public String getRevision()
	{
		return m_sharedObject instanceof IRevisionedSharedObject revSharedObject ? revSharedObject.getRevision() : null;
	}

	@Override @Nullable public String getBuildList()
	{
		if (m_designDescriptor == null || m_designDescriptor.getProject() == null) {
			return null;
		}

		IBuildListIterator buildListIterator =
				m_designDescriptor.getProject().getBuildListMgr().getBuildListsContainingDesign(getDesignUID());

		return buildListIterator == null ? null : collectBuildListNames(buildListIterator);
	}

	/**
	 * Collects build list names from the iterator.
	 *
	 * @param iterator the build list iterator
	 * @return comma-separated build list names, or null if none found
	 */
	@Nullable protected String collectBuildListNames(@NotNull IBuildListIterator iterator)
	{
		List<String> buildListNames = new ArrayList<>();

		while (iterator.hasNext()) {
			IBuildList buildList = iterator.getNext();
			if (buildList != null && buildList.getName() != null) {
				buildListNames.add(buildList.getName());
			}
		}

		return buildListNames.isEmpty() ? null : String.join(", ", buildListNames);
	}

	@Override public boolean isValid()
	{
		return true;
	}
}

