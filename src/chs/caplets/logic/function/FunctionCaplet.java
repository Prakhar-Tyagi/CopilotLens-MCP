/*
 * Copyright 2010-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.function;

import chs.ans.ContainerType;
import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caplets.capture.SysMLImportFactory;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.IFunctionCaplet;
import chs.caplets.logic.LogicDataTransfer;
import chs.caplets.logic.LogicLibrariedObjectUpdater;
import chs.caplets.logic.LogicUtilsFactory;
import chs.caplets.shared.BaseCaplet;
import chs.cof.library.ILibrariedObjectUpdater;
import chs.cof.security.FunctionalPermissionEnum;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.buildlist.FunctionBuildListDRCDomainManager;
import chs.ctf.drc.logic.FunctionDesignDRCDomainManager;
import chs.system.FactoryMgr;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;
import chs.utility.ui.HelpMgr;
import chs.utility.ui.IHelpMgr;
import com.mentor.chs.plugin.action.IXBaseAction;
import com.mentor.chs.plugin.action.capture.IXFunctionDesignAction;
import com.mentor.chs.plugin.changemanager.IXBridgeProcessor;
import com.mentor.chs.plugin.changemanager.IXFunctionBridgeProcessor;
import com.mentor.chs.plugin.designinspection.IXFunctionDesignInspectionPanel;
import com.mentor.chs.plugin.designinspection.IXInspectionPanel;
import com.mentor.chs.plugin.designtab.IXDesignTabPanel;
import com.mentor.chs.plugin.designtab.IXFunctionDesignTabPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Collections;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class FunctionCaplet extends BaseCaplet implements IFunctionCaplet
{

	private static IFunctionCaplet m_caplet = null;

	public static IFunctionCaplet getCaplet()
	{
		return m_caplet;
	}

	public FunctionCaplet()
	{
		super(new LogicDataTransfer());

		if (m_caplet == null) {
			m_caplet = this;
		}
		else {
			throw new RuntimeException("Caplet has already been instantiated");
		}
	}

	public String getName()
	{
		return AppInfo.getFullApplicationName(AppInfo.App.CONCORDFUNCTIONS);
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(FunctionCaplet.class, "FunctionCaplet.Design.Type");
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Functional";
	}

	/**
	 * @return the Icon for the window.
	 */
	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.CONCORDFUNCTIONS);
		return brandingImagery.getSmallIcon();
	}

	protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(FunctionDesignDRCDomainManager.getInstance());
		drcs.add(FunctionBuildListDRCDomainManager.getInstance());
	}

	public void initializeCaplet(@NotNull IFIB fib)
	{
		// dts0100353308 - ensure plugin manager is initialized at start-up for Logic application
		initializePluginMgrs();

		initialiseHelpSets();

		super.initializeCaplet(fib);

		FactoryMgr.getLogicalFactory().setLogicUtilsFactrory(new LogicUtilsFactory());
		FactoryMgr.getLogicalFactory().setSysMLImportFactory(new SysMLImportFactory());
	}

	protected void initialiseHelpSets()
	{
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.SYSTEMSCAPTURE, true);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.LOGIC, false);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.STYLING, false);
		//TODO-chandras: CONCORDFX shoould we remove this?
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.HARNESSXC, false);
	}

	/**
	 * Returns the specific custom action class that this Caplet supports.
	 *
	 * @return IXLogicAction
	 */
	@Nullable
	public Class<? extends IXBaseAction> getCustomClass()
	{
		return IXFunctionDesignAction.class;
	}

	public Class<? extends IXInspectionPanel> getInspectionPanelClass()
	{
		return IXFunctionDesignInspectionPanel.class;
	}

	public Class<? extends IXDesignTabPanel> getDesignPanelClass()
	{
		return IXFunctionDesignTabPanel.class;
	}

	/**
	 * @see ICaplet#createLibrariedObjectUpdater()
	 */
	@Override @Nullable public ILibrariedObjectUpdater createLibrariedObjectUpdater()
	{
		return new LogicLibrariedObjectUpdater();
	}

	/**
	 * @return the functional permissions used for editing
	 */
	public FunctionalPermissionEnum getEditFunctionalPermissionEnum()
	{
		return FunctionalPermissionEnum.EditFunctionalDesigns;
	}

	protected Class<? extends ICaplet> getResourceClass()
	{
		return FunctionCaplet.class;
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new FunctionLifeCycle(this);
	}

	protected IResource createResource()
	{
		return new FunctionResource(this);
	}

	@Override public boolean isFunctionCaplet()
	{
		return true;
	}

	public boolean isAnalysisSupported()
	{
		return false;
	}

	public boolean isSchematicEditor()
	{
		return false;
	}

	@Override public Collection<ContainerType> getOpenableContainerTypes()
	{
		return Collections.singleton(ContainerType.FUNCTION);
	}

	@NotNull
	@Override
	public Collection<DesignType> getOpenAbleDesignTypes() {
		return Collections.singleton(DesignType.FUNCTIONS);
	}

	@Override public Class<? extends IXBridgeProcessor> getBridgeProcessorClass()
	{
		return IXFunctionBridgeProcessor.class;
	}

	@Override public Type getType()
	{
		return Type.ARTISAN_FUNCTION;
	}
}
