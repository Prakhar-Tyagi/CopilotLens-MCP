/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.ans.ContainerType;
import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ILayoutCaplet;
import chs.caplets.layout.LayoutDataTransfer;
import chs.caplets.shared.BaseCaplet;
import chs.cof.library.ILibrariedObjectUpdater;
import chs.cof.security.FunctionalPermissionEnum;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.buildlist.LayoutBuildListDRCDomainManager;
import chs.ctf.drc.logic.LayoutDesignDRCDomainManager;
import chs.system.FactoryMgr;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;
import chs.utility.ui.HelpMgr;
import chs.utility.ui.IHelpMgr;
import com.mentor.chs.plugin.action.IXBaseAction;
import com.mentor.chs.plugin.action.IXLayoutDesignAction;
import com.mentor.chs.plugin.changemanager.IXBridgeProcessor;
import com.mentor.chs.plugin.changemanager.IXLayoutBridgeProcessor;
import com.mentor.chs.plugin.designinspection.IXInspectionPanel;
import com.mentor.chs.plugin.designinspection.IXLayoutDesignInspectionPanel;
import com.mentor.chs.plugin.designtab.IXDesignTabPanel;
import com.mentor.chs.plugin.designtab.IXLayoutDesignTabPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Collections;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class LayoutCaplet extends BaseCaplet implements ILayoutCaplet
{

	private static ILayoutCaplet m_caplet = null;

	public static ILayoutCaplet getCaplet()
	{
		return m_caplet;
	}

	public LayoutCaplet()
	{
		super(new LayoutDataTransfer());

		if (m_caplet == null) {
			m_caplet = this;
		}
		else {
			throw new RuntimeException("Caplet has already been instantiated");
		}
	}

	public String getRibbonIdentifier()
	{
		return AppInfo.LAYOUT;
	}

	public String getName()
	{
		return AppInfo.getFullApplicationName(AppInfo.LAYOUT);
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(LayoutCaplet.class, "Layout.Design.Type");
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Layout";
	}

	/**
	 * @return the Icon for the window.
	 */
	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.LOGIC_DESIGNER);
		return brandingImagery.getSmallIcon();
	}

	protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(LayoutDesignDRCDomainManager.getInstance());
		drcs.add(LayoutBuildListDRCDomainManager.getInstance());
	}

	@Override public Collection<ContainerType> getOpenableContainerTypes()
	{
		return Collections.singleton(ContainerType.LAYOUT);
	}

	@NotNull
	@Override
	public Collection<DesignType> getOpenAbleDesignTypes() {
		return Collections.singleton(DesignType.LAYOUT);
	}

	public boolean isAnalysisSupported()
	{
		return false;
	}

	public void initializeCaplet(@NotNull IFIB fib)
	{
		// dts0100353308 - ensure plugin manager is initialized at start-up for Logic application
		initializePluginMgrs();

		initialiseHelpSets();

		super.initializeCaplet(fib);

		FactoryMgr.getLogicalFactory().setLogicUtilsFactrory(new LogicUtilsFactory());
	}

	protected void initialiseHelpSets()
	{
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.LAYOUT, true);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.LOGIC, false);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.STYLING, false);
	}

	/**
	 * Returns the specific custom action class that this Caplet supports.
	 *
	 * @return IXLogicAction
	 */
	@Nullable
	public Class<? extends IXBaseAction> getCustomClass()
	{
		return IXLayoutDesignAction.class;
	}

	public Class<? extends IXInspectionPanel> getInspectionPanelClass()
	{
		return IXLayoutDesignInspectionPanel.class;
	}

	public Class<? extends IXDesignTabPanel> getDesignPanelClass()
	{
		return IXLayoutDesignTabPanel.class;
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
		return FunctionalPermissionEnum.EditLayoutDesigns;
	}

	protected Class<? extends ICaplet> getResourceClass()
	{
		return LayoutCaplet.class;
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new LayoutLifecycle(this);
	}

	protected IResource createResource()
	{
		return new LayoutResource(this);
	}

	@Override public boolean isLayoutCaplet()
	{
		return true;
	}

	public boolean isSchematicEditor()
	{
		return false;
	}

	@Override public Class<? extends IXBridgeProcessor> getBridgeProcessorClass()
	{
		return IXLayoutBridgeProcessor.class;
	}

	@Override public Type getType()
	{
		return Type.LAYOUT;
	}
}
