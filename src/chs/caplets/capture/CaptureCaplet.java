/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture;

// caf imports

import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caplets.logic.LogicDataTransfer;
import chs.caplets.logic.LogicResource;
import chs.caplets.shared.BaseCaplet;
import chs.cof.security.FunctionalPermissionEnum;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.buildlist.LogicBuildListDRCDomainManager;
import chs.ctf.drc.logic.LogicDesignDRCDomainManager;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
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

@ApplicationSpecification(includeIn = {Application.CapitalCapture, Application.CapitalArchitect})

public class CaptureCaplet extends BaseCaplet
{

	private static ICaplet m_caplet = null;

	public void initializeCaplet(@NotNull IFIB fib)
	{
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.CAPTURE, false);
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.STYLING, false);
		super.initializeCaplet(fib);
	}

	public FunctionalPermissionEnum getEditFunctionalPermissionEnum()
	{
		return null;
	}

	public static ICaplet getCaplet()
	{
		return m_caplet;
	}

	public CaptureCaplet()
	{
		super(new LogicDataTransfer());

		if (m_caplet == null) {
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
		return CaptureCaplet.class;
	}

	/**
	 * Returns the name of this caplet.
	 */
	public String getName()
	{
		return AppInfo.getFullApplicationName(AppInfo.App.CAPTURE);
	}

	/**
	 * @return the Icon for the window.
	 */
	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.CAPTURE);
		return brandingImagery.getSmallIcon();
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new CaptureLifecycle(this);
	}

	protected IResource createResource()
	{
		return new LogicResource(this);
	}

	protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		// Use Logics (switch on/off in DRC itself)
		drcs.add(LogicDesignDRCDomainManager.getInstance());
		drcs.add(LogicBuildListDRCDomainManager.getInstance());
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

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(CaptureCaplet.class, "Caplet.SystemsDesign.Type");
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Systems";
	}

	/**
	 * @return The IXInspectionPanel.class that is supported by this caplet
	 */
	@Nullable public Class<? extends IXInspectionPanel> getInspectionPanelClass()
	{
		return IXLogicInspectionPanel.class;
	}

	/**
	 * @return The IXDesignTabPanel.class that is supported by this caplet
	 */
	public Class<? extends IXDesignTabPanel> getDesignPanelClass()
	{
		return IXLogicDesignTabPanel.class;
	}

	@Override public Class<? extends IXBridgeProcessor> getBridgeProcessorClass()
	{
		return IXLogicBridgeProcessor.class;
	}

	@Override public Type getType()
	{
		return Type.CAPITAL_CAPTURE;
	}
}
