/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IShareHelper;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Abstract class to support sharing a Single Line
 */
public abstract class AbstractBaseShareSingleLineActionHelper extends AbstractBaseShareConductorActionHelper<ISharedSingleLine>
{

	protected AbstractBaseShareSingleLineActionHelper(@NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram,
			IShareHelper shareHelper)
	{
		super(design, diagram, shareHelper);
	}

	@Override protected Iterator<ISharedSingleLine> getShareObjectsUsedOnDesign()
	{
		return m_design.getSharedSingleLines();
	}

	@Override protected boolean hasDuplicateName(@NotNull ISharedSingleLine sharedObject)
	{
		return sharedObject.getName().equalsIgnoreCase(m_logicObject.getName());
	}

	@NotNull @Override protected Set<ISharedSingleLine> getOrderedSharedConductors(@NotNull ISharedConductorMgr condMgr)
	{
		Set<ISharedSingleLine> sharedSingleLines = new LinkedHashSet<ISharedSingleLine>();
		sharedSingleLines.addAll(CollectionUtils.createList(condMgr.getSharedSingleLines()));
		return sharedSingleLines;
	}

	@Override protected void reportSharedObjectMgrLocked()
	{
		LogicActionMessageHelper.warnLocked(m_sharedObjectMgr);
	}

	@Override protected void reportSharedObjectDeleted(@NotNull ISharedObject shareIntoObj)
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(AbstractBaseShareConductorActionHelper.class,
						"BaseShareActionHelper.SharedObjectDeleted.Heading"),
				ResourceMgr.getString(AbstractBaseShareConductorActionHelper.class,
						"BaseShareActionHelper.SharedObjectDeleted.Text", shareIntoObj.getName()));
	}

	@Override protected void transferConnectivity(ILogicObject logicObject)
	{

	}
}
