/*
 * Copyright (c) 2018. Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 *  SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.caplet.helpers.LifecycleTypeHolder;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.common.IProperty;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utilities.suite.ApplicationSuiteInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

class CavaiLifecycleTypeHolder extends LifecycleTypeHolder
{

	CavaiLifecycleTypeHolder(Class<?> lifecycleClass, String menuText, Icon icon)
	{
		super(lifecycleClass, menuText, icon);
	}

	@Override public boolean isType(@NotNull Object object)
	{
		if (super.isType(object) && isCapitalApplicationSuite()) {
			final ISystemLogicDiagram diagram = CommonUtils.cast(object, ISystemLogicDiagram.class);
			IProperty propertyByName = diagram != null ? diagram.findPropertyByName("Functional Diagram") : null;
			return propertyByName != null && !propertyByName.isEditable();
		}
		return false;
	}

	protected boolean isCapitalApplicationSuite()
	{
		return ((ApplicationSuiteInfo.getInstance().getCurrentApplicationSuite().getAppSuite() ==
				ApplicationSuiteInfo.AppSuite.Capital) && !AppInfo.isSvcDoc());
	}
}
