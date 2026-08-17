/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.helpers;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectorBase;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.UIDObjectHierarchyProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author pbhawsar on 25-05-2017
 */
public class LogicObjectHierarchyProvider extends UIDObjectHierarchyProvider
{

	@NotNull private ILogicObjectChildrenProvider mChildrenProvider;
	@NotNull private ILogicObjectParentProvider mParentProvider;

	public LogicObjectHierarchyProvider(@NotNull ILogicObjectChildrenProvider childrenProvider,
			@NotNull ILogicObjectParentProvider parentProvider)
	{
		mChildrenProvider = childrenProvider;
		mParentProvider = parentProvider;
	}

	@NotNull public List<ChildProvider> getChildrenProviders()
	{
		return Arrays.asList(new ChildProvider()
							 {
								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IPinList.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IPinList) parent);
								 }
							 }, new ChildProvider()
							 {

								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IGeneralHighway.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IGeneralHighway) parent);
								 }
							 }, new ChildProvider()
							 {

								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return IAssembly.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((IAssembly) parent);
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
							 }, new ChildProvider()
							 {

								 @NotNull @Override public Class<?> getParentClass()
								 {
									 return ISchemStackPin.class;
								 }

								 @NotNull @Override public List<IUID> getChildren(@NotNull IUIDObject parent)
								 {
									 return mChildrenProvider.getChildren((ISchemStackPin) parent);
								 }
							 }
		);
	}

	@NotNull @Override public List<ParentProvider> getParentProviders()
	{
		return Arrays.asList(new ParentProvider()
		{
			@NotNull @Override public Class<?> getChildClass()
			{
				return IAbstractPin.class;
			}

			@Nullable @Override public IUIDObject getParent(@NotNull IUIDObject child)
			{
				return mParentProvider.getParent((IAbstractPin) child);
			}
		}, new ParentProvider()
		{
			@NotNull @Override public Class<?> getChildClass()
			{
				return IDeviceConnector.class;
			}

			@Nullable @Override public IUIDObject getParent(@NotNull IUIDObject child)
			{
				return mParentProvider.getParent((IDeviceConnector) child);
			}
		}, new ParentProvider()
		{
			@NotNull @Override public Class<?> getChildClass()
			{
				return IConnectorBase.class;
			}

			@Nullable @Override public IUIDObject getParent(@NotNull IUIDObject child)
			{
				return mParentProvider.getParent((IConnectorBase) child);
			}
		}, new ParentProvider()
		{
			@NotNull @Override public Class<?> getChildClass()
			{
				return IBackshell.class;
			}

			@Nullable @Override public IUIDObject getParent(@NotNull IUIDObject child)
			{
				return mParentProvider.getParent((IBackshell) child);
			}
		}, new ParentProvider()
		{
			@NotNull @Override public Class<?> getChildClass()
			{
				return IConductor.class;
			}

			@Nullable @Override public IUIDObject getParent(@NotNull IUIDObject child)
			{
				return mParentProvider.getParent((IConductor) child);
			}
		}, new ParentProvider()
		{
			@NotNull @Override public Class<?> getChildClass()
			{
				return IMulticore.class;
			}

			@Nullable @Override public IUIDObject getParent(@NotNull IUIDObject child)
			{
				return mParentProvider.getParent((IMulticore) child);
			}
		});
	}
}
