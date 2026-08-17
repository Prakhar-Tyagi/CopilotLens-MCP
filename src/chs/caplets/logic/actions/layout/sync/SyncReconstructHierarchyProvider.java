/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldBody;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.UIDObjectHierarchyProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SyncReconstructHierarchyProvider extends UIDObjectHierarchyProvider
{

	@NotNull private ISyncReconstructibleChildrenProvider mChildrenProvider;

	public SyncReconstructHierarchyProvider()
	{
		mChildrenProvider = new SyncReconstructibleChildrenProvider();
	}

	@NotNull @Override protected List<ChildProvider> getChildrenProviders()
	{
		return Arrays.asList(new ChildProvider()
							 {
								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IDevice.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IDevice) parent);
								 }
							 }, new ChildProvider()
							 {
								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IConnector.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IConnector) parent);
								 }
							 }, new ChildProvider()
							 {
								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IPinList.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull
										 IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IPinList) parent);
								 }
							 }, new ChildProvider()
							 {
								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IMulticore.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IMulticore) parent);
								 }
							 }
		);
	}

	@NotNull @Override protected List<ParentProvider> getParentProviders()
	{
		return Collections.emptyList(); // we don't need parent objects during sync reconstruction phase
	}

	private interface ISyncReconstructibleChildrenProvider
	{

		@NotNull List<IUID> getChildren(@NotNull IDevice device);

		@NotNull List<IUID> getChildren(@NotNull IConnector connector);

		@NotNull List<IUID> getChildren(@NotNull IPinList pinList);

		@NotNull List<IUID> getChildren(@NotNull IMulticore multicore);
	}

	private static class SyncReconstructibleChildrenProvider implements ISyncReconstructibleChildrenProvider
	{

		@NotNull @Override public List<IUID> getChildren(@NotNull IDevice device)
		{
			Stream<? extends IUIDObject> deviceChildren = getPinlistChildren(device);
			deviceChildren = Stream.concat(deviceChildren, getDeviceConnectors(device));
			deviceChildren = Stream.concat(deviceChildren, device.getInternalLinkCollection().stream());
			return toUIDList(deviceChildren);
		}

		@NotNull @Override public List<IUID> getChildren(@NotNull IConnector connector)
		{
			Stream<? extends IUIDObject> connectorChildren = getPinlistChildren(connector);
			final IBackshell backshell = connector.getBackshell();
			if (backshell != null) {
				connectorChildren = Stream.concat(connectorChildren, Stream.of(backshell));
			}
			return toUIDList(connectorChildren);
		}

		@NotNull @Override public List<IUID> getChildren(@NotNull IPinList pinList)
		{
			return toUIDList(getPinlistChildren(pinList));
		}

		@NotNull @Override public List<IUID> getChildren(@NotNull IMulticore multicore)
		{
			final IShieldBody shieldBody = multicore.getShieldBody();
			if (shieldBody != null) {
				return Arrays.asList(shieldBody.getUID());
			}
			return Collections.emptyList();
		}

		private Stream<? extends IUIDObject> getPinlistChildren(@NotNull IPinList pinList)
		{
			return pinList.getGenericPins().stream();
		}

		@NotNull private List<IUID> toUIDList(@NotNull Stream<? extends IUIDObject> stream)
		{
			return stream
					.map(uObj -> uObj.getUID())
					.collect(Collectors.toList());
		}

		@NotNull private Stream<? extends IUIDObject> getDeviceConnectors(@NotNull IDevice device)
		{
			if (device.getNumDeviceConnectors() > 0) {
				return device.getDeviceConnectors().stream();
			}
			return Stream.empty();
		}
	}
}
