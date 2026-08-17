/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.servicedoc.GenerateServiceDocumentationAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ILifecycleType;
import chs.caf.caplet.helpers.LifecycleActionTypeHolder;
import chs.caf.caplet.helpers.LifecycleTypeHolder;
import chs.caf.caplet.helpers.design.WiringDesignGeneratorAction;
import chs.caplets.publisher.PublisherProjectGenerateCAVALAction;
import chs.caplets.shared.BaseLifecycle;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISystemLogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IHarnessBuildList;
import chs.cof.project.folder.INormalFolder;
import chs.cof.topology.ITopologyDesign;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;

import java.awt.Component;

/**
 * FEAT14997 - Offline Service Documentation User: kayyagar Date: Oct 12, 2010 Time: 7:28:07 PM
 */
public class SvcDocLifeCycle extends LogicLifecycle
{

	public SvcDocLifeCycle(Caplet caplet)
	{
		super(caplet);
		addTypesForFilter(
				new LifecycleActionTypeHolder(IHarnessBuildList.class, new GenerateServiceDocumentationAction(caplet)));

		// Todo - WiringDesignGeneratorAction action should be present in integrator caplet, but publisher not support integrator caplet, so adding here
		addTypesForFilter(
				new LifecycleActionTypeHolder(ITopologyDesign.class, new WiringDesignGeneratorAction(caplet)));

		String deleteStr = ResourceMgr.getString(SvcDocLifeCycle.class, "Lifecycle.Delete.text");
		char deleteMnemonic = ResourceMgr.getMnemonic(SvcDocLifeCycle.class, "Lifecycle.Delete.mnemonic");
		addTypeForDelete(new LifecycleTypeHolder(INormalFolder.class, deleteStr, deleteMnemonic));
	}

	@Override protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new SvcDocController(caplet, design, diagram);
	}

	@Override protected Class<? extends BaseLifecycle> getResourceClass()
	{
		return SvcDocLifeCycle.class;
	}

	@Override protected void doForceClosePrompt(String headMessage, ActionType actionType)
	{
		Component parent = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		MessageHelper.showInformationMessage(parent, headMessage,
				ResourceMgr.getString(getResourceClass(), "BaseLifecycle.msg.noRevision.forceClose"));
	}

	@Override protected void addTypeForDelete(ILifecycleType f)
	{
		if (INormalFolder.class.equals(f.getLifecycleClass())) {
			// Support only deletion of empty NormalFolder
			super.addTypeForDelete(f);
		}
	}

	//Commenting this to enable revise action from Teamcenter to capital for Publisher
	/*@Override protected void addTypeForCreateRevision(ILifecycleType f)
	{
		//Do nothing-->we should'nt be allowed to create revisions on designs from service doc
		//return;
	}*/

//	@Override
//	protected void addTypesForGeneralLifecycleActivities()
//	{
//		super.addTypesForGeneralLifecycleActivities();
//		// Open Types
//		ILifecycleType openDiagramType = getDiagramLifecycleType(IHarnessDiagram.class, "Lifecycle.Open.");
//		addTypeForOpen(openDiagramType);
//	}

	@Override protected void addTypesForNew(ICaplet caplet)
	{
		addTypeForNew(getDiagramLifecycleType(ISystemLogicDesign.class, "Lifecycle.NewDiagram."));
		addTypeForNew(getDiagramLifecycleType(ILayoutLogicDesign.class, "Lifecycle.NewDiagram."));
	}

	@Override protected void addProjectGenerateCavalAction(ICaplet caplet)
	{
		addTypesForFilter(
				new LifecycleActionTypeHolder(IProject.class, new PublisherProjectGenerateCAVALAction(caplet)));
	}
}
