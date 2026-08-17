/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic;

import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caplets.logic.actions.inlineassist.IInlineInsertionControllerFactory;
import chs.caplets.logic.actions.inlineassist.InlineInsertionControllerFactory;
import chs.caplets.shared.BaseCaplet;
import chs.cof.library.ILibrariedObjectUpdater;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.buildlist.LogicBuildListDRCDomainManager;
import chs.ctf.drc.logic.LogicDesignDRCDomainManager;
import chs.subsystem.immersedapp.IImmersedInitializer;
import chs.subsystem.immersedapp.ImmersedAppServices;
import chs.system.FactoryMgr;
import chs.utilities.AppInfo;
import chs.utilities.topology.TopologyServices;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;
import chs.utility.ui.HelpMgr;
import chs.utility.ui.IHelpMgr;
import com.mentor.chs.plugin.action.IXBaseAction;
import com.mentor.chs.plugin.action.IXLogicAction;
import com.mentor.chs.plugin.changemanager.IXBridgeProcessor;
import com.mentor.chs.plugin.changemanager.IXLogicBridgeProcessor;
import com.mentor.chs.plugin.designinspection.IXInspectionPanel;
import com.mentor.chs.plugin.designinspection.IXLogicInspectionPanel;
import com.mentor.chs.plugin.designtab.IXDesignTabPanel;
import com.mentor.chs.plugin.designtab.IXLogicDesignTabPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class Caplet extends BaseCaplet
{

	private static ILogicCaplet m_caplet = null;

	public static ILogicCaplet getCaplet()
	{
		return m_caplet;
	}

	public Caplet()
	{
		super(new LogicDataTransfer());

		if (m_caplet == null) {
			registerServices();
			m_caplet = this;
		}
		else {
			throw new RuntimeException("Caplet has already been instantiated");
		}
	}

	/**
	 * @see BaseCaplet#getResourceClass()
	 */
	protected Class<? extends ICaplet> getResourceClass()
	{
		return Caplet.class;
	}

	public String getName()
	{
		return AppInfo.getFullApplicationName(AppInfo.App.LOGIC_DESIGNER);
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Logical";
	}

	/**
	 * @return the Icon for the window.
	 */
	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.LOGIC_DESIGNER);
		return brandingImagery.getSmallIcon();
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new LogicLifecycle(this);
	}

	protected IResource createResource()
	{
		return new LogicResource(this);
	}

	protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(LogicDesignDRCDomainManager.getInstance());
		drcs.add(LogicBuildListDRCDomainManager.getInstance());
	}

	public void initializeCaplet(@NotNull IFIB fib)
	{
		if (AppInfo.getAppInfo().getApp() != AppInfo.App.RUNNER) {
			// dts0100353308 - ensure plugin manager is initialized at start-up for Logic application
			initializePluginMgrs();
		}

		initialiseHelpSets();

		super.initializeCaplet(fib);

		FactoryMgr.getLogicalFactory().setLogicUtilsFactrory(new LogicUtilsFactory());
		TopologyServices.instance().registerInstance(IInlineInsertionControllerFactory.class,
				new InlineInsertionControllerFactory());
	}

	protected void initialiseHelpSets()
	{
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.LOGIC, false);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.STYLING, false);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.HARNESSXC, false);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.ANALYSIS, false);
	}

	private void registerServices()
	{
		ImmersedAppServices.registerExtension(Caplet.class, IImmersedInitializer.class,
				o -> new ImmersedInitializerLogicCaplet());
	}

	/**
	 * Returns the specific custom action class that this Caplet supports.
	 *
	 * @return IXLogicAction
	 */
	@Nullable
	public Class<? extends IXBaseAction> getCustomClass()
	{
		return IXLogicAction.class;
	}

	public Class<? extends IXInspectionPanel> getInspectionPanelClass()
	{
		return IXLogicInspectionPanel.class;
	}

	public Class<? extends IXDesignTabPanel> getDesignPanelClass()
	{
		return IXLogicDesignTabPanel.class;
	}

	@Override public Class<? extends IXBridgeProcessor> getBridgeProcessorClass()
	{
		return IXLogicBridgeProcessor.class;
	}

	/**
	 * @see ICaplet#createLibrariedObjectUpdater()
	 */
	@Override @Nullable public ILibrariedObjectUpdater createLibrariedObjectUpdater()
	{
		return new LogicLibrariedObjectUpdater();
	}

	@Override public Type getType()
	{
		return Type.LOGIC;
	}
}
