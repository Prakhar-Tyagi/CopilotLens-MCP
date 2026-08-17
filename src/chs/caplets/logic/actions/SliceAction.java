/*
 * Copyright 2015-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.AbstractSliceAction;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IProjectPreferenceMgr;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import chs.utility.ProjectHelper;
import chs.utility.slice.ConductorSliceProcessor;
import chs.utility.slice.ISliceLogger;
import chs.utility.slice.ISliceProcessor;
import chs.utility.slice.PinListSliceProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: vkhatri Date: 8 Sep, 2012 Time: 12:37:10 PM To change this template use File |
 * Settings | File Templates.
 */
public class SliceAction extends AbstractSliceAction
{

	/**
	 * Constructor for the SliceAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public SliceAction(ICapletController controller)
	{
		super(controller, false, true);
	}

	@NotNull protected Set<ISliceProcessor> getSliceProcessors()
	{
		final Set<ISliceProcessor> processors = new LinkedHashSet<>();

		ISliceLogger sliceLogger = getSliceLogger();
		if(isConductorSlicable()) {
			processors.add(new ConductorSliceProcessor(sliceLogger));
		}
		processors.add(new PinListSliceProcessor(sliceLogger));

		return processors;
	}

	protected boolean isConductorSlicable()
	{
		if(isLogicalDesign()) {
			IProjectPreferenceMgr projectPreferenceMgr =
					ProjectHelper.getProjectPreferences(getCurrentProject());
			return projectPreferenceMgr.getAllowSliceConductor();
		}

		return true;
	}

	protected boolean isLogicalDesign()
	{
		IBaseDiagram baseDiagram = getBaseDiagram();

		if(baseDiagram != null) {
			IDesignContainer designDescriptor = baseDiagram.getDesignContainer();
			return designDescriptor != null && designDescriptor.getDesignType() == DesignType.LOGICAL;
		}

		return false;
	}

	@Nullable protected IProject getCurrentProject()
	{
		return FactoryMgr.getSystemFactory().getCAFUtils().getCurrentProject();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		setupMoveOrthogonal(true);
		getSliceLogger().prepareForSlice();
		return super.onActivate(e);
	}

	/**
	 * Gets the ActionUIClass attribute of the SliceAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return SliceActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(SliceAction.class, "SliceAction.statusbar.text");
	}

	@NotNull @Override protected ISliceLogger createSliceLogger()
	{
		return new SliceLogger();
	}
}
