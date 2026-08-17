/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.shared.helper.ISelectSharedAdapter;
import chs.caplets.logic.actions.shared.helper.SelectSharedHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;

public class SelectSharedFunctionPanel extends SelectSharedPanel
{

	public SelectSharedFunctionPanel(
			@NotNull EditSharedPinListModel emodel, boolean fromSymbol,
			ILogicDesign design)
	{
		super(emodel, fromSymbol, design);
	}

	@NotNull @Override
	protected SelectSharedHandler createHandler(@NotNull EditSharedPinListModel emodel, ILogicDesign design,
			@NotNull ISelectSharedAdapter creator, boolean fromSymbol)
	{
		final IProject currentProject = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
		return new SelectSharedHandler(emodel, design, fromSymbol, creator, currentProject)
		{
			//We dont support generated names in case of shared functions
			@Override protected void createGeneratedNameProperty(@NotNull ISelectSharedAdapter creator)
			{
				//do nothing
			}
		};
	}
}
