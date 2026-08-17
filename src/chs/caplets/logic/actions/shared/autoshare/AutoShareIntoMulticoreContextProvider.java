/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto share-into provider for multicore - counter part for share into dialog in auto flow
 */
public class AutoShareIntoMulticoreContextProvider extends AutoShareMulticoreContextProvider
{

	@NotNull private IUID m_sharedMulticore;
	@NotNull private Map<ILogicObject, ISharedObject> m_multicoreToSharedHierarchyMap;

	public AutoShareIntoMulticoreContextProvider(@NotNull IMulticore multicore, @NotNull ILogicDesign design,
			@NotNull IUID sharedMulticore, @NotNull Map<ILogicObject, ISharedObject> multicoreToSharedHierarchyMap,
			@NotNull IMessageReporterWithContext reporter)
	{
		super(multicore, design, reporter);
		m_sharedMulticore = sharedMulticore;
		m_multicoreToSharedHierarchyMap = multicoreToSharedHierarchyMap;
	}

	@Nullable @Override public IUID getSharedMulticoreUID()
	{
		return m_sharedMulticore;
	}

	@Nullable private ISharedMulticore getSharedMulticore()
	{
		return UIDMgr.getObjectOfType(m_sharedMulticore, ISharedMulticore.class);
	}

	@Nullable @Override public String getSharedMulticoreName()
	{
		final ISharedMulticore sharedMulticore = getSharedMulticore();
		return sharedMulticore != null ? sharedMulticore.getName() : null;
	}

	@Nullable @Override public String getSharedMulticoreRevision()
	{
		final ISharedMulticore sharedMulticore = getSharedMulticore();
		return sharedMulticore != null ? sharedMulticore.getRevision() : null;
	}

	@Override public boolean isSharedMulticoreNameGenerated()
	{
		return false;
	}

	@NotNull @Override public Map<ILogicObject, IUID> getMulticoreToSharedHierarchyMap()
	{
		final Map<ILogicObject, IUID> logicToSharedUIDMap = new HashMap<>();
		for (Map.Entry<ILogicObject, ISharedObject> logicToSharedEntry : m_multicoreToSharedHierarchyMap.entrySet()) {
			logicToSharedUIDMap.put(logicToSharedEntry.getKey(), logicToSharedEntry.getValue().getUID());
		}

		return Collections.unmodifiableMap(logicToSharedUIDMap);
	}

	@Override public boolean validate()
	{
		boolean isSharedObjectPlaceAble = !isAnotherRevisionAlreadyUsed(getSharedMulticore());
		if (!isSharedObjectPlaceAble) {
			reportAnotherRevisionAlreadyUsed(m_multicore);
		}
		return isSharedObjectPlaceAble;
	}

	private boolean isAnotherRevisionAlreadyUsed(@Nullable ISharedMulticore sharedMulticore)
	{
		if (sharedMulticore != null) {
			return SharedObjectRevisionHelper.getOtherRevisionUsedInDesign(sharedMulticore, m_design) != null;
		}
		return false;
	}

	private void reportAnotherRevisionAlreadyUsed(@NotNull IMulticore multicore)
	{
		String displayTypeName = StringUtils.toLowerCase(COFTypeEnum.getDisplayableTypeName(multicore));
		String usedRevisionMsg = ResourceMgr.getString(AutoShareIntoConductorGroupActionHelper.class,
				"AutoShareIntoMulticoreContextProvider.AnotherRevisionUsed.msg", displayTypeName);
		reportError(usedRevisionMsg);
	}
}
