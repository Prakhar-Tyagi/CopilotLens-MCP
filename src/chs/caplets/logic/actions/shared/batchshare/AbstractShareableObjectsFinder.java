/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.bridges.harness.MergeHelper;
import chs.utility.CompositeKey;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedAbstractable;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.cof.project.OptionExpression;
import chs.common.IDesignAbstraction;
import chs.common.IExpression;
import chs.common.IProperty;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeType;
import chs.common.attr.IAttributeTypes;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.SharedObjectAbstractionMatcher;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract shareable objects finder
 */
public abstract class AbstractShareableObjectsFinder
{

	@NotNull private final IProject m_project;
	@NotNull private final Set<ILogicDesign> m_designsInScope;
	@NotNull protected final Collection<ISharedObject> m_sharedObjectsInScope;
	@NotNull private IObjectInfoProvider m_objectInfoProvider;
	@NotNull private Predicate<IObjectInfo> m_objectFilter = IObjectInfo::isNonShared;


	protected AbstractShareableObjectsFinder(@NotNull IProject project, @NotNull Set<ILogicDesign> designsInScope,
			@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider)
	{
		m_project = project;
		m_designsInScope = designsInScope;
		m_sharedObjectsInScope = new HashSet<>(sharedObjectsInScope);
		m_objectInfoProvider = objectInfoProvider;
	}

	protected AbstractShareableObjectsFinder(@NotNull IProject project, @NotNull Set<ILogicDesign> designsInScope,
	@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider,
	@NotNull Predicate<IObjectInfo> objectFilter)
	{
		this(project, designsInScope, sharedObjectsInScope, objectInfoProvider);
		m_objectFilter = m_objectFilter.and(objectFilter);
    }

	@NotNull
	public Set<IShareableObjectGroup> collectShareableObjectGroups(
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		Set<ShareableEntityTypeEnum> typesToBeConsidered =
				entitiesShareCriteria.stream().map(IEntityShareCriteria::getEntityType).collect(Collectors.toSet());
		return collectShareableObjectGroups(entitiesShareCriteria, typesToBeConsidered);
	}

	@NotNull
	public Set<IShareableObjectGroup> collectShareableObjectGroups(
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria,
			@NotNull Set<ShareableEntityTypeEnum> typesToBeConsidered)
	{
		Collection<IObjectInfo> objectsInfo =
				m_objectInfoProvider.getObjectInfos(m_designsInScope, typesToBeConsidered,
		m_objectFilter);
		if (objectsInfo.isEmpty()) {
			return new HashSet<>();
		}
		Set<IShareableObjectGroup> shareableObjectGroups = getShareableObjectGroups(entitiesShareCriteria, objectsInfo);
		for (IEntityShareCriteria entityShareCriteria : entitiesShareCriteria) {
			assignTargetObjectsToCandidateGroups(shareableObjectGroups, entityShareCriteria);
		}

		return shareableObjectGroups;
	}

	private void assignTargetObjectsToCandidateGroups(@NotNull Set<IShareableObjectGroup> shareableObjectGroups,
			@NotNull IEntityShareCriteria entityShareCriteria)
	{
		Set<IShareableObjectGroup> candidateGroups = shareableObjectGroups.stream()
				.filter(group -> entityShareCriteria.getEntityType().equals(group.getType())).collect(
						Collectors.toSet());
		if (candidateGroups.isEmpty()) {
			return;
		}
		Set<ISharedObject> sharedObjects = getSharedObjects(entityShareCriteria.getEntityType());
		final Map<ISharedObject, IConnectivityInfo> sharedObjectConnectivityInfoMap = new HashMap<>();
		if (entityShareCriteria.matchConnectivity()) {
			collectSharedConnectivityInfos(sharedObjects, sharedObjectConnectivityInfoMap);
		}

		SetMap<CompositeKey, ISharedObject> sharedObjectsByCriteria = new SetMap<>();
		Function<ISharedObject, Function<IAttributeType, String>> attrValProvider = sharedObject -> attr -> {
			IAttribute attribute = sharedObject.getAttribute(attr.getName());
			return attribute == null ? null : attribute.getAsUnformattedString();
		};
		Function<ISharedObject, Function<String, String>> propValProvider = sharedObject -> propName -> {
			IProperty property = sharedObject.findPropertyByName(propName);
			return property == null ? null : property.getAsUnformattedString();
		};
		Function<ISharedObject, Supplier<IConnectivityInfo>> connectivityInfoProvider = sharedObject ->
				() -> sharedObjectConnectivityInfoMap.get(sharedObject);
		for (ISharedObject sharedObject : sharedObjects) {
			CompositeKey groupByKey = getKeyByCriteria(attrValProvider.apply(sharedObject),
					propValProvider.apply(sharedObject),
					connectivityInfoProvider.apply(sharedObject),
					entityShareCriteria);
			if (groupByKey != null) {
				if (sharedObject instanceof ISharedConnector) {
					ISharedConnector sharedConnector = (ISharedConnector) sharedObject;
					if (sharedConnector.isInlineHalf()) {
						if (sharedConnector.getType() != PinListTypeEnum.TypeInlineJack) {
							continue;
						}
						ISharedConnector mate =
								sharedConnector.getMates().stream().filter(ISharedConnector::isInlineHalf).findFirst()
										.orElse(null);
						CompositeKey matedConnGroupByKey = getKeyByCriteria(attrValProvider.apply(mate),
								propValProvider.apply(mate),
								connectivityInfoProvider.apply(mate),
								entityShareCriteria);
						if (matedConnGroupByKey == null) {
							groupByKey = null;
						}
						else {
							List<Object> keys = new ArrayList<>(groupByKey.keys());
							keys.addAll(matedConnGroupByKey.keys());
							groupByKey = new CompositeKey(keys);
						}
					}
				}
				if (groupByKey != null) {
					sharedObjectsByCriteria.add(groupByKey, sharedObject);
				}
			}
		}
		for (IShareableObjectGroup shareableObjectGroup : candidateGroups) {
			IObjectInfo representativeObjectInfo = shareableObjectGroup.getRepresentativeObjectInfo();
			CompositeKey groupByKey = getMappingKey(representativeObjectInfo, entityShareCriteria);
			if (groupByKey != null) {
				Set<ISharedObject> candidateTargetObjects = sharedObjectsByCriteria.get(groupByKey);
				candidateTargetObjects = filterTargetSharedObjects(candidateTargetObjects);
				shareableObjectGroup.setCandidateTargetSharedObjects(candidateTargetObjects);
			}
		}
	}

	@NotNull private Set<ISharedObject> filterTargetSharedObjects(@NotNull Set<ISharedObject> candidateTargetObjects)
	{
		if (!candidateTargetObjects.isEmpty()) {
			Collection<ISharedUsage> projectSharedUsages =
					LogicUtils.getProjectSharedUsages(m_project, candidateTargetObjects, m_designsInScope);
			Set<ISharedObject> revisionsUsedInDesigns =
					projectSharedUsages.stream().map(ISharedUsage::getSharedObject).collect(Collectors.toSet());
			if (revisionsUsedInDesigns.size() == 1) {
				return revisionsUsedInDesigns;
			}
			else if (revisionsUsedInDesigns.size() > 1) {
				return candidateTargetObjects;
			}
		}

		if (!candidateTargetObjects.isEmpty() &&
				SharedObjectAbstractionMatcher.doSharedObjectsSupportAbstraction(candidateTargetObjects)) {
			Set<ISharedObject> sharedObjectsMatchingAbstraction = new HashSet<>();
			for (ILogicDesign design : m_designsInScope) {
				IDesignAbstraction designAbstraction = design.getDesignAbstraction();
				for (ISharedObject candidateTargetObject : candidateTargetObjects) {
					IDesignAbstraction shObjAbstraction =
							((ISharedAbstractable) candidateTargetObject).getDesignAbstraction();
					if (SharedObjectAbstractionMatcher.areAbstractionsSame(designAbstraction, shObjAbstraction)) {
						sharedObjectsMatchingAbstraction.add(candidateTargetObject);
					}
				}
			}
			if (sharedObjectsMatchingAbstraction.isEmpty()) {
				return Collections.emptySet();
			}
			else if (sharedObjectsMatchingAbstraction.size() == 1) {
				return sharedObjectsMatchingAbstraction;
			}
		}
		return candidateTargetObjects;
	}

	private void collectSharedConnectivityInfos(@NotNull Set<ISharedObject> sharedObjects,
			@NotNull Map<ISharedObject, IConnectivityInfo> sharedObjectConnectivityInfoMap)
	{
		List<IUID> designIDsUsingSharedObjects =
				LogicUtils.getLogicDesignsUsingSharedObjectsWithoutDomainFilter(m_project, sharedObjects, null);
		Set<ILogicDesign> candidateDesigns =
				UIDUtils.convertToObjectSet(designIDsUsingSharedObjects, ILogicDesign.class);
		Set<ShareableEntityTypeEnum> candidateTypes = Set.of(ShareableEntityTypeEnum.WIRE, ShareableEntityTypeEnum.NET);
		Set<String> sharedUIDScope =
				sharedObjects.stream().map(IUIDObject::getUID).map(IUID::getString).collect(Collectors.toSet());
		Collection<IObjectInfo> objectInfos = m_objectInfoProvider.getObjectInfos(candidateDesigns, candidateTypes,
				objectInfo -> sharedUIDScope.contains(objectInfo.getSharedUID()));
		Map<String, IObjectInfo> objectInfoBySharedUIDMap =
				objectInfos.stream().collect(Collectors.toMap(IObjectInfo::getSharedUID, Function.identity()));
		sharedObjects.forEach(sharedObject -> {
			IObjectInfo objectInfo = objectInfoBySharedUIDMap.get(sharedObject.getUID().getString());
			IConnectivityInfo sharedObjectConnectivityInfo =
					objectInfo == null ? null : objectInfo.getConnectivityInfo();
			if (sharedObjectConnectivityInfo != null) {
				sharedObjectConnectivityInfoMap.put(sharedObject, sharedObjectConnectivityInfo);
			}
		});
	}

	@Nullable private CompositeKey getMappingKey(@NotNull IObjectInfo objectInfo,
			@NotNull IEntityShareCriteria entityShareCriteria)
	{
		CompositeKey groupByKey = getKeyByCriteria(attr -> objectInfo.getAttributeValue(attr.getXMLName()),
				objectInfo::getPropertyValue, objectInfo::getConnectivityInfo, entityShareCriteria);
		if (groupByKey != null && objectInfo instanceof IInlineConnectorInfo) {
			IInlineConnectorInfo inlineConnectorInfo = (IInlineConnectorInfo) objectInfo;
			IInlineConnectorInfo matedConnector = Objects.requireNonNull(inlineConnectorInfo.getMatedConnector());
			CompositeKey matedConnGroupByKey =
					getKeyByCriteria(attr -> matedConnector.getAttributeValue(attr.getXMLName()),
							matedConnector::getPropertyValue, matedConnector::getConnectivityInfo, entityShareCriteria);
			if (matedConnGroupByKey == null) {
				groupByKey = null;
			}
			else {
				List<Object> keys = new ArrayList<>(groupByKey.keys());
				keys.addAll(matedConnGroupByKey.keys());
				groupByKey = new CompositeKey(keys);
			}
		}
		return groupByKey;
	}

	@Nullable private CompositeKey getKeyByCriteria(@NotNull Function<IAttributeType, String> attrValueProvider,
			@NotNull Function<String, String> propertyValueProvider,
			@NotNull Supplier<IConnectivityInfo> connectivityInfoProvider,
			@NotNull IEntityShareCriteria entityShareCriteria)
	{
		List<Object> criteriaValues = new ArrayList<>();
		List<IAttributeType> attributes = entityShareCriteria.getAttributes().stream()
				.sorted((attr1, attr2) -> attr1.getName().compareTo(attr2.getName())).collect(Collectors.toList());
		for (IAttributeType attribute : attributes) {
			String attributeValue = getAttributeValue(attrValueProvider, attribute);
			if (attributeValue == null) {
				return null;
			}
			criteriaValues.add(attributeValue);
		}

		List<String> properties = entityShareCriteria.getProperties().stream().sorted().collect(Collectors.toList());
		for (String propertyName : properties) {
			String propertyValue = propertyValueProvider.apply(propertyName);
			if (propertyValue == null) {
				return null;
			}
			criteriaValues.add(propertyValue);
		}

		if (entityShareCriteria.matchConnectivity()) {
			IConnectivityInfo connectivityInfo = connectivityInfoProvider.get();
			if (connectivityInfo == null) {
				return null;
			}
			criteriaValues.add(connectivityInfo);
		}
		return criteriaValues.isEmpty() ? null : new CompositeKey(criteriaValues);
	}

	@Nullable private String getAttributeValue(@NotNull Function<IAttributeType, String> attrValueProvider,
			@NotNull IAttributeType attribute)
	{
		String attributeValue = attrValueProvider.apply(attribute);
		if (attributeValue != null && IAttributeTypes.OPTION_EXP.equals(attribute.getName())) {
			IExpression expression = MergeHelper.getSortedExpression(new OptionExpression(attributeValue), true);
			if (expression != null) {
				return StringUtils.trim(expression.toString());
			}
		}
		return attributeValue;
	}

	/**
	 * Provides shared objects present in the project related to specified shareable entity type
	 *
	 * @param type shareable entity type
	 * @return collection of shared objects related to specified shareable entity type
	 */
	@NotNull protected abstract Set<ISharedObject> getSharedObjects(@NotNull ShareableEntityTypeEnum type);

	@NotNull private Set<IShareableObjectGroup> getShareableObjectGroups(
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria, @NotNull Collection<IObjectInfo> objectInfos)
	{
		Set<IShareableObjectGroup> shareableObjectGroups = new HashSet<>();
		Map<ShareableEntityTypeEnum, IEntityShareCriteria> entityShareCriteriaMap = entitiesShareCriteria.stream()
				.collect(Collectors.toMap(IEntityShareCriteria::getEntityType, Function.identity()));
		for (IEntityShareCriteria entityShareCriteria : entityShareCriteriaMap.values()) {
			shareableObjectGroups.addAll(getShareableObjectGroups(entityShareCriteria, objectInfos));
		}
		return shareableObjectGroups;
	}

	@NotNull
	private Set<IShareableObjectGroup> getShareableObjectGroups(@NotNull IEntityShareCriteria entityShareCriteria,
			@NotNull Collection<IObjectInfo> objectInfos)
	{
		Collection<IObjectInfo> candidateObjectInfos =
				getObjectInfosByType(objectInfos, entityShareCriteria.getEntityType());
		if (objectInfos.isEmpty()) {
			return Collections.emptySet();
		}
		Collection<Set<IObjectInfo>> candidateGroups = candidateObjectInfos.stream()
				.filter(objectInfo -> getMappingKey(objectInfo, entityShareCriteria) != null)
				.collect(Collectors.groupingBy(objectInfo -> getMappingKey(objectInfo, entityShareCriteria),
						Collectors.toSet()))
				.values();
		return candidateGroups.stream().map(ShareableObjectGroup::new).collect(Collectors.toSet());
	}

	@NotNull private Collection<IObjectInfo> getObjectInfosByType(@NotNull Collection<IObjectInfo> objectInfos,
			@NotNull ShareableEntityTypeEnum type)
	{
		Stream<? extends IObjectInfo> objectInfosByType = objectInfos.stream()
				.filter(objectInfo -> objectInfo.getType().equals(type))
				.filter(IObjectInfo::isNonShared);
		if (type.equals(ShareableEntityTypeEnum.INLINE)) {
			objectInfosByType = objectInfosByType.filter(IInlineConnectorInfo.class::isInstance)
					.map(IInlineConnectorInfo.class::cast)
					.filter(IInlineConnectorInfo::isJack)
					.filter(inlinePlugInfo -> inlinePlugInfo.getMatedConnector() != null);
		}
		else if (type.equals(ShareableEntityTypeEnum.NET) || type.equals(ShareableEntityTypeEnum.WIRE) ||
				type.equals(ShareableEntityTypeEnum.MULTICORE) || type.equals(ShareableEntityTypeEnum.OVERBRAID)) {
			Set<String> innerCores = objectInfos.stream().filter(IMulticoreInfo.class::isInstance)
					.map(IMulticoreInfo.class::cast)
					.map(IMulticoreInfo::getInnercoreUIDs)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet());
			objectInfosByType = objectInfosByType.filter(objectInfo -> !innerCores.contains(objectInfo.getUID()));

			if (type.equals(ShareableEntityTypeEnum.MULTICORE)) {
				//do not show Single Line's multicore
				Set<String> singleLineMulticores =
						objectInfos.stream().filter(SingleLineInfo.class::isInstance).map(SingleLineInfo.class::cast)
								.map(SingleLineInfo::getMulticoreUIDs).flatMap(Collection::stream)
								.collect(Collectors.toSet());
				objectInfosByType =
						objectInfosByType.filter(objectInfo -> !singleLineMulticores.contains(objectInfo.getUID()));
			}
		}
		return objectInfosByType.collect(Collectors.toSet());
	}
}
