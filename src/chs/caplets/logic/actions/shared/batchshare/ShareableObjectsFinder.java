/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * To find, group and map the objects of given designs to shared objects in scope based on provided share criteria
 */
class ShareableObjectsFinder extends AbstractShareableObjectsFinder
{

	ShareableObjectsFinder(@NotNull IProject project, @NotNull Set<ILogicDesign> designsInScope,
			@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider)
	{
		super(project, designsInScope, sharedObjectsInScope, objectInfoProvider);
	}

	ShareableObjectsFinder(@NotNull IProject project, @NotNull Set<ILogicDesign> designsInScope,
			@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider,
			@NotNull Predicate<IObjectInfo> objectFilter)
	{
		super(project, designsInScope, sharedObjectsInScope, objectInfoProvider, objectFilter);
	}

	@Override @NotNull protected Set<ISharedObject> getSharedObjects(@NotNull ShareableEntityTypeEnum type)
	{
		if (ShareableEntityTypeEnum.DEVICE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypeDevice.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.GROUND.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypeGround.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.SPLICE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypeSplice.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.PLUG.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypePlug.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.JACK.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypeJack.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.INLINE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypeInlineJack.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.RING_TERMINAL.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedPinList.class::isInstance)
					.map(ISharedPinList.class::cast)
					.filter(sharedPinlist -> PinListTypeEnum.TypeRingTerminal.equals(sharedPinlist.getType()))
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.WIRE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedConductor.class::isInstance)
					.map(ISharedConductor.class::cast)
					.filter(ISharedConductor::isWire)
					.filter(sharedConductor -> sharedConductor.getMulticore() == null)
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.NET.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedConductor.class::isInstance)
					.map(ISharedConductor.class::cast)
					.filter(ISharedConductor::isNet)
					.filter(sharedConductor -> sharedConductor.getMulticore() == null)
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.MULTICORE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(sharedObject -> !ISharedOverbraid.class.isInstance(sharedObject))
					.filter(ISharedMulticore.class::isInstance)
					.map(ISharedMulticore.class::cast)
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.OVERBRAID.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedOverbraid.class::isInstance)
					.map(ISharedOverbraid.class::cast)
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.SINGLE_LINE.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedSingleLine.class::isInstance)
					.map(ISharedSingleLine.class::cast)
					.collect(Collectors.toSet());
		}
		if (ShareableEntityTypeEnum.HIGHWAY.equals(type)) {
			return m_sharedObjectsInScope.stream()
					.filter(ISharedGeneralHighway.class::isInstance)
					.map(ISharedGeneralHighway.class::cast)
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}
}