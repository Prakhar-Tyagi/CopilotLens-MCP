/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.common.IAttributePropertyProvider;
import chs.common.INamedObject;
import chs.utilities.AlphaNumComparator;
import chs.utilities.Pair;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * merge comparison
 */
public class MergeComparison implements IMergeComparison<IMergeActionChange, IAttributePropertyProvider>
{

	public static final String NAME_SEPERATOR = ":";
	@Nullable private ICachedObject sourceObject;
	@Nullable private ICachedObject targetObject;
	@Nullable private ICachedObject mergedObject;
	@NotNull private final Map<IObjectInfo, IObjectInfo> mObjectMappingInfo = new HashMap<>();
	@NotNull private List<IMergeActionChange> mChanges;
	@Nullable private IMergeComparison<IMergeActionChange, IAttributePropertyProvider> mParent;
	private List<IMergeComparison<IMergeActionChange, IAttributePropertyProvider>> mChildren;
	private boolean m_changesUpdated = false;

	MergeComparison(@Nullable IMergeComparison<IMergeActionChange, IAttributePropertyProvider> parent)
	{
		mParent = parent;
		mChildren = new ArrayList<>();
		mChanges = new ArrayList<>();
	}

	public MergeComparison(@Nullable IMergeComparison<IMergeActionChange, IAttributePropertyProvider> parent,
			@Nullable ICachedObject source, @Nullable ICachedObject target, @Nullable ICachedObject merged,
			@NotNull Map<IObjectInfo, IObjectInfo> objectMappingInfo)
	{
		this(parent);
		sourceObject = source;
		targetObject = target;
		mergedObject = merged;
		mObjectMappingInfo.putAll(objectMappingInfo);
	}

	@Override public void setInitialStateOfSourceObject(@NotNull IAttributePropertyProvider obj)
	{
		sourceObject = AttrPropExtractor.extractObjectInfo(null, obj);
	}

	@Override public void setInitialStateOfTargetObject(@NotNull IAttributePropertyProvider obj)
	{
		targetObject = AttrPropExtractor.extractObjectInfo(null, obj);
	}

	@Override public void setTransformedState(@NotNull IAttributePropertyProvider obj)
	{
		mergedObject = AttrPropExtractor.extractObjectInfo(null, obj);
	}

	@Override public void addObjectMapping(@NotNull INamedObject key, @NotNull INamedObject value)
	{
		IObjectInfo keyInfo = ObjectInfo.getObjectInfo(key);
		assert !mObjectMappingInfo.containsKey(keyInfo);
		mObjectMappingInfo.put(keyInfo, ObjectInfo.getObjectInfo(value));
	}

	@Override @NotNull public Collection<IMergeActionChange> computeChanges()
	{
		if (m_changesUpdated) {
			return mChanges;
		}
		if (sourceObject == null || mergedObject == null) {
			return mChanges;
		}
		Set<ICachedObject> objects = new HashSet<ICachedObject>();
		objects.add(sourceObject); // interested only in source key changes
		Set<String> allAttributes = collectKeys(objects, obj -> obj.getAttributes());
		Set<String> allProperties = collectKeys(objects, obj -> obj.getProperties());
		mChanges.addAll(
				collectAttributeChanges(sourceObject, targetObject, mergedObject, allAttributes,
						obj -> obj.getAttributes(),
						IActionChange.ComparisonField.Attribute));

		mChanges.addAll(
				collectAttributeChanges(sourceObject, targetObject, mergedObject, allProperties,
						obj -> obj.getProperties(),
						IActionChange.ComparisonField.Property));

		Collections.sort(mChanges, new Comparator<IMergeActionChange>()
		{
			@Override public int compare(IMergeActionChange o1, IMergeActionChange o2)
			{
				boolean isWarning = MergeActionChangeDisplayHelper.WARNING_SET.contains(o1.getKey());
				if (isWarning ^ MergeActionChangeDisplayHelper.WARNING_SET.contains(o2.getKey())) {
					if (isWarning) {
						return -1;
					}
					else {
						return 1;
					}
				}
				return o1.getKey().compareToIgnoreCase(o2.getKey());
			}
		});
		findChildOjectChanges();
		m_changesUpdated = true;
		return mChanges;
	}

	@Override public void addChange(@NotNull IMergeActionChange change)
	{
		mChanges.add(change);
	}

	private void findChildOjectChanges()
	{
		if (sourceObject == null || mergedObject == null) {
			return;
		}
		if (sourceObject instanceof IDeviceSnapShotObject && targetObject instanceof IDeviceSnapShotObject &&
				mergedObject instanceof IDeviceSnapShotObject) {
			populateDeviceChildrenComparison((IDeviceSnapShotObject) sourceObject, (IDeviceSnapShotObject) targetObject,
					(IDeviceSnapShotObject) mergedObject);
		}else if(sourceObject instanceof IConnectorSnapShot && targetObject instanceof IConnectorSnapShot &&
				mergedObject instanceof IConnectorSnapShot){
			populateConnectorChildrenComparison((IConnectorSnapShot) sourceObject, (IConnectorSnapShot) targetObject,
					(IConnectorSnapShot) mergedObject);
		}
		else {
			for (ICachedObject sourceChildObject : sourceObject.getChildren()) {
				populateChildComparisons(sourceChildObject);
			}
		}
		// Now fetch all child object changes
		mChildren.stream().forEach(ch -> mChanges.addAll(ch.computeChanges()));
	}

	private void populateConnectorChildrenComparison(@NotNull IConnectorSnapShot src, @NotNull IConnectorSnapShot tgt,
			@NotNull IConnectorSnapShot merge)
	{
		IMergeComparison<IMergeActionChange, IAttributePropertyProvider> backshell = null;

		for (ICachedObject child : src.getChildren()) {
			IObjectInfo searchKey = getSearchKey(child);
			if (src.getBackShellUID() != null && src.getBackShellUID().equals(child.getObjectInfo().getUID())) {
				ICachedObject matchingTgtBackShell = getMatchingBackShell(tgt);
				ICachedObject matchingMergedBackShell = getMatchingBackShell(merge);
				if (matchingMergedBackShell != null) {
					backshell = new MergeComparison(this, child, matchingTgtBackShell, matchingMergedBackShell,
							mObjectMappingInfo);
				}
			}
			else {
				ICachedObject matchingTargetPin = getMatchingConnectorPin(tgt, searchKey);
				ICachedObject matchingMergedPin = getMatchingConnectorPin(merge, searchKey);
				if (matchingMergedPin != null) {
					mChildren.add(new MergeComparison(this, child, matchingTargetPin, matchingMergedPin,
							mObjectMappingInfo));
				}
			}
		}
		if (backshell != null) {
			mChildren.add(backshell);
		}
	}

	private void populateDeviceChildrenComparison(@NotNull IDeviceSnapShotObject source,
			@NotNull IDeviceSnapShotObject target, @NotNull IDeviceSnapShotObject merged)
	{
		Set<ICachedObject> sourceDevicePins = new HashSet<>();
		Map<String, ICachedObject> uidToDCMap = new HashMap<>();
		source.getChildren().forEach(child -> {
			if (source.getDevicePinUIDs().contains(child.getObjectInfo().getUID())) {
				sourceDevicePins.add(child);
			}
			else if (source.getDeviceConnectorUIDs().contains(child.getObjectInfo().getUID())) {
				uidToDCMap.put(child.getObjectInfo().getUID(), child);
			}
		});

		List<ICachedObject> sourcePins = new ArrayList<>(sourceDevicePins);
		Collections.sort(sourcePins, new Comparator<ICachedObject>()
		{
			@Override public int compare(ICachedObject o1, ICachedObject o2)
			{
				return new AlphaNumComparator<String>(true, true, true)
						.compare(o1.getObjectInfo().getName(), o2.getObjectInfo().getName());
			}
		});

		Set<Pair<ICachedObject, ICachedObject>> processedComparisons = new HashSet<>();

		for (ICachedObject sourcePin : sourcePins) {
			IObjectInfo searchKey = getSearchKey(sourcePin);
			ICachedObject matchingTargetPin = getMatchingPin(target, searchKey);
			ICachedObject matchingMergedPin = getMatchingPin(merged, searchKey);
			if (matchingMergedPin == null) {
				continue;
			}
			MergeComparison pinComparison = new MergeComparison(this, sourcePin, matchingTargetPin, matchingMergedPin,
					mObjectMappingInfo);
			mChildren.add(pinComparison);

			if (!source.getDevicePinToDeviceConnectorMap().containsKey(sourcePin.getObjectInfo().getUID())) {
				continue;
			}

			ICachedObject targetDC = null;
			if (matchingTargetPin != null) {
				String matchingTargetDCUID =
						target.getDevicePinToDeviceConnectorMap().get(matchingTargetPin.getObjectInfo().getUID());
				Optional<ICachedObject> matchingTargetDC =
						target.getChildren().stream()
								.filter(object -> object.getObjectInfo().getUID() != null &&
										object.getObjectInfo().getUID().equals(matchingTargetDCUID))
								.findFirst();
				if (matchingTargetDC.isPresent()) {
					targetDC = matchingTargetDC.get();
				}
			}

			String matchingMergedDCUID =
					merged.getDevicePinToDeviceConnectorMap().get(matchingMergedPin.getObjectInfo().getUID());
			Optional<ICachedObject> matchingMergedDC =
					merged.getChildren().stream()
							.filter(object -> object.getObjectInfo().getUID() != null &&
									object.getObjectInfo().getUID().equals(matchingMergedDCUID))
							.findFirst();
			ICachedObject mergedDC = null;
			if (matchingMergedDC.isPresent()) {
				mergedDC = matchingMergedDC.get();
			}

			ICachedObject sourceDC =
					uidToDCMap.get(source.getDevicePinToDeviceConnectorMap().get(sourcePin.getObjectInfo().getUID()));
			Pair<ICachedObject, ICachedObject> comparison = new Pair<>(sourceDC, mergedDC);
			if (mergedDC != null && !processedComparisons.contains(comparison)) {
				pinComparison.getChildren().add(new MergeComparison(pinComparison, sourceDC,
						targetDC, mergedDC,
						mObjectMappingInfo));
				processedComparisons.add(comparison);
			}
		}
	}

	@Nullable
	private ICachedObject getMatchingPin(@NotNull IDeviceSnapShotObject target, @NotNull IObjectInfo searchKey)
	{
		for (ICachedObject object : target.getChildren()) {
			if (object.getObjectInfo().getName().equals(searchKey.getName()) &&
					target.getDevicePinUIDs().contains(object.getObjectInfo().getUID())) {
				return object;
			}
		}
		return null;
	}

	@Nullable
	private ICachedObject getMatchingConnectorPin(@NotNull IConnectorSnapShot target, @NotNull IObjectInfo searchKey)
	{
		for (ICachedObject object : target.getChildren()) {
			if (object.getObjectInfo().getName().equals(searchKey.getName()) &&
					target.getPinUIDs().contains(object.getObjectInfo().getUID())) {
				return object;
			}
		}
		return null;
	}

	@Nullable
	private ICachedObject getMatchingBackShell(@NotNull IConnectorSnapShot target)
	{
		for (ICachedObject object : target.getChildren()) {
			if (Objects.equals(target.getBackShellUID(), object.getObjectInfo().getUID())) {
				return object;
			}
		}
		return null;
	}


	private void populateChildComparisons(@NotNull ICachedObject sourceChildObject)
	{
		IObjectInfo searchKey = getSearchKey(sourceChildObject);
		ICachedObject targetChildObject = getMatchingChildByName(targetObject, searchKey);
		ICachedObject mergedChildObject = getMatchingChildByName(mergedObject, searchKey);

		if (mergedChildObject == null) {
			return;
		}
		MergeComparison child = new MergeComparison(this, sourceChildObject, targetChildObject, mergedChildObject,
				mObjectMappingInfo);
		mChildren.add(child);
	}

	@NotNull private IObjectInfo getSearchKey(@NotNull ICachedObject sourceChildObject)
	{
		IObjectInfo searchKey = sourceChildObject.getObjectInfo();
		if (mObjectMappingInfo.containsKey(searchKey)) {
			searchKey = mObjectMappingInfo.get(searchKey);
		}
		return searchKey;
	}

	@Nullable
	private ICachedObject getMatchingChildByName(@Nullable ICachedObject parent, @NotNull IObjectInfo objectInfo)
	{
		if (parent != null) {
			for (ICachedObject object : parent.getChildren()) {
				if (object.getObjectInfo().getName().equals(objectInfo.getName())) {
					return object;
				}
			}
		}
		return null;
	}

	@Nullable @Override public String getSourceObjectName()
	{
		if (sourceObject == null) {
			return null;
		}
		IMergeComparison<IMergeActionChange, IAttributePropertyProvider> parent = getParent();
		StringBuilder name = new StringBuilder();
		if (parent != null && parent.getSourceObjectName() != null) {
			name.append(parent.getSourceObjectName()).append(NAME_SEPERATOR)
					.append(sourceObject.getObjectInfo().getName());
		}
		else {
			name.append(sourceObject.getObjectInfo().getName());
		}
		return name.toString();
	}

	@NotNull @Override public Collection<IMergeComparison<IMergeActionChange, IAttributePropertyProvider>> getChildren()
	{
		return mChildren;
	}

	@Nullable @Override public IMergeComparison<IMergeActionChange, IAttributePropertyProvider> getParent()
	{
		return mParent;
	}

	@Nullable @Override public ICachedObject getSourceObject()
	{
		return sourceObject;
	}

	@Nullable @Override public ICachedObject getMergedObject()
	{
		return mergedObject;
	}

	@Nullable @Override public ICachedObject getTargetObject()
	{
		return targetObject;
	}

	@NotNull private Set<String> collectKeys(@NotNull Collection<ICachedObject> objects,
			Function<ICachedObject, Map<String, String>> function)
	{
		Set<String> attributeKeys = new HashSet<>();
		objects.stream().forEach(obj -> collectKeys(attributeKeys, function.apply(obj)));
		return attributeKeys;
	}

	@NotNull private Set<IMergeActionChange> collectAttributeChanges(@NotNull ICachedObject source,
			@Nullable ICachedObject target,
			@NotNull ICachedObject merged, Set<String> keys,
			Function<ICachedObject, Map<String, String>> function, IActionChange.ComparisonField field)
	{
		Set<IMergeActionChange> changes = new HashSet<>();
		for (String mKey : keys) {
			String sourceVal = StringUtils.nonNull(function.apply(source).get(mKey));
			String mergedVal = StringUtils.nonNull(function.apply(merged).get(mKey));
			if (target == null) {
				if (!StringUtils.equals(sourceVal, mergedVal)) {
					createChange(source, field, changes, mKey, sourceVal, null, mergedVal);
				}
				continue;
			}
			String targetVal = StringUtils.nonNull(function.apply(target).get(mKey));
			if (IActionChange.ComparisonField.Attribute.equals(field)) {
				boolean sourceDiffersMergedValue = !StringUtils.equals(sourceVal, mergedVal);
				if (sourceDiffersMergedValue) {
					createChange(source, field, changes, mKey, sourceVal, targetVal, mergedVal);
				}
				else {
					boolean keyExistsOnTarget = target.getAttributes().keySet().contains(mKey);
					if (keyExistsOnTarget) {
						boolean targetValueDiffersMerged = !StringUtils.equals(targetVal, mergedVal);
						if (targetValueDiffersMerged) {
							createChange(source, field, changes, mKey, sourceVal, targetVal, mergedVal);
						}
					}
				}
			}
			else {
				if (!StringUtils.equals(sourceVal, targetVal) || !StringUtils.equals(targetVal, mergedVal)) {
					createChange(source, field, changes, mKey, sourceVal, targetVal, mergedVal);
				}
			}
		}
		return changes;
	}

	private void createChange(@NotNull ICachedObject source,
			IActionChange.ComparisonField field, Set<IMergeActionChange> changes, String mKey, String sourceVal,
			@Nullable String targetVal, String mergedVal)
	{
		StringBuilder sourceName = new StringBuilder();
		IMergeComparison<IMergeActionChange, IAttributePropertyProvider> parent = getParent();
		if (parent != null) {
			sourceName.append(parent.getSourceObjectName()).append(NAME_SEPERATOR)
					.append(source.getObjectInfo().getName());
		}
		else {
			sourceName.append(source.getObjectInfo().getName());
		}
		changes.add(new MergeActionChange(mKey, sourceVal, targetVal, mergedVal, field, sourceName.toString(),
				source.getObjectTypeDisplayName(), null));
	}

	private void collectKeys(Set<String> collector, Map<String, String> val)
	{
		collector.addAll(val.keySet());
	}
}
