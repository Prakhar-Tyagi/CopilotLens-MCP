/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IEntityShareCriteria;
import chs.caplets.logic.actions.shared.batchshare.IObjectInfo;
import chs.caplets.logic.actions.shared.batchshare.IShareableObjectGroup;
import chs.caplets.logic.actions.shared.batchshare.ShareabilityStatus;
import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IRevisionedObject;
import chs.common.attr.IAttributeType;
import chs.utilities.IXMLTags;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class BatchShareGroup implements IBatchShareGroup
{

	@Nullable private String sharedObjectName;
	private boolean isValid;
	private Map<IObjectInfo, IBatchShareRow> objectMap;
	private IShareableObjectGroup underlyingGroup;
	private ShareabilityStatus status;
	@NotNull private String m_matchCriteriaValues;

	public BatchShareGroup(@NotNull IShareableObjectGroup group,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		objectMap = new HashMap<>();
		underlyingGroup = group;
		Set<ISharedObject> targetSharedObjects = underlyingGroup.getTargetSharedObjects();
		status = underlyingGroup.validate();
		isValid = status == ShareabilityStatus.VALID;
		sharedObjectName =
				targetSharedObjects.size() == 1 ? getSharedObjectName(targetSharedObjects.iterator().next()) : null;
		m_matchCriteriaValues = evaluateMatchByCriteriaValues(group, entitiesShareCriteria);
		constructRowElements();
	}

	@NotNull private String getSharedObjectName(@NotNull ISharedObject sharedObject)
	{
		return sharedObject instanceof IRevisionedObject ? ((IRevisionedObject) sharedObject).getFullName() :
				sharedObject.getName();
	}

	private void constructRowElements()
	{
		underlyingGroup.getShareableObjectInfos().forEach(objectInfo -> {
			BatchShareRow batchShareRow = new BatchShareRow(objectInfo, this);
			objectMap.put(objectInfo, batchShareRow);
		});
		IObjectInfo anchor = underlyingGroup.getRepresentativeObjectInfo();
		if (underlyingGroup.getTargetSharedObjects().isEmpty() && objectMap.containsKey(anchor)) {
			IBatchShareRow anchorRow = objectMap.get(anchor);
			anchorRow.setAction(Action.SHARE);
		}
	}

	@NotNull private String evaluateMatchByCriteriaValues(@NotNull IShareableObjectGroup group,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		ShareableEntityTypeEnum objectType = group.getType();
		IEntityShareCriteria entityShareCriteria =
				entitiesShareCriteria.stream().filter(shareCriteria -> shareCriteria.getEntityType().equals(objectType))
						.findFirst().orElse(null);
		List<String> criteriaValues = new ArrayList<>();
		if (entityShareCriteria != null) {
			IObjectInfo objectInfo = group.getRepresentativeObjectInfo();
			List<IAttributeType> attributes = entityShareCriteria.getAttributes().stream()
					.sorted((attr1, attr2) -> attr1.getName().compareTo(attr2.getName())).collect(Collectors.toList());
			for (IAttributeType attribute : attributes) {
				criteriaValues.add(StringUtils.concatenate(attribute.getDisplayName(), StringUtils.COLON,
						objectInfo.getAttributeValue(attribute.getXMLName())));
			}
			List<String> properties =
					entityShareCriteria.getProperties().stream().sorted().collect(Collectors.toList());
			for (String propertyName : properties) {
				criteriaValues.add(StringUtils
						.concatenate(propertyName, StringUtils.COLON, objectInfo.getPropertyValue(propertyName)));
			}
			if (entityShareCriteria.matchConnectivity()) {
				String representativeObjectName =
						underlyingGroup.getTargetSharedObjects().stream().map(this::getSharedObjectName).sorted()
								.findFirst().orElse(objectInfo.getAttributeValue(IXMLTags.NAME));
				criteriaValues.add(StringUtils.concatenate(ResourceMgr.getString(BatchShareGroup.class,
						"BatchShareGroup.matchedByColumn.connectivityCriterionText"), StringUtils.COLON,
						representativeObjectName));
			}
		}
		return criteriaValues.stream().collect(Collectors.joining(StringUtils.COMMA));
	}

	@NotNull @Override public Collection<IBatchShareRow> getBatchShareElements()
	{
		return objectMap.values();
	}

	@Override public boolean isValid()
	{
		return isValid;
	}

	@Nullable @Override public String getTargetSharedObjectName()
	{
		return sharedObjectName;
	}

	@NotNull @Override public ShareabilityStatus getStatus()
	{
		return status;
	}

	@NotNull @Override public String getMatchCriteriaValues()
	{
		return m_matchCriteriaValues;
	}
}
