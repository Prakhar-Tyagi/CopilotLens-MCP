/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.ans.ContainerType;
import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.helpers.CapletHelper;
import chs.caf.plugin.CAFCustomActionMgr;
import chs.caf.plugin.SymbolCustomActionDelegate;
import chs.cof.symbol.IAbstractLibrary;
import chs.common.IRealm;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;
import chs.utility.ui.HelpMgr;
import chs.utility.ui.IHelpMgr;
import com.mentor.chs.plugin.action.IXBaseAction;
import com.mentor.chs.plugin.action.IXSymbolAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@ApplicationSpecification(
		includeIn = {Application.CapitalEssentialsSymbolDesigner, Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class Caplet extends CapletHelper
{

	private IFIB m_fib = null;
	private ICapletLifecycle m_lifeCycle = null;
	private IResource m_resource = null;

	public Caplet()
	{
		super(new SymbolDataTransfer());
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(Caplet.class, "Caplet.Design.Type");
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Symbol";
	}

	@Override public Collection<ContainerType> getOpenableContainerTypes()
	{
		return new HashSet<>();
	}

	@NotNull
	@Override
	public Collection<DesignType> getOpenAbleDesignTypes() {
		return List.of();
	}

	// The name of the caplet
	public String getName()
	{
		return AppInfo.getFullApplicationName(AppInfo.getSymbolAppInfo());
	}

	/**
	 * * @return the Icon for the window.
	 */
	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.getSymbolAppInfo());
		Icon sampleIcon = brandingImagery.getSmallIcon();
		return sampleIcon;
	}

	@NotNull public IFIB getFIB()
	{
		return m_fib;
	}

	public ICapletLifecycle getLifecycle()
	{
		return m_lifeCycle;
	}

	public IResource getResource()
	{
		return m_resource;
	}

	public void initializeCaplet(@NotNull IFIB fib)
	{
		// Remember the FIB so we can communicate with CAF
		m_fib = fib;

		// Create Interface Implementations
		m_lifeCycle = createLifecycle();
		m_resource = createResource();
		initializePluginMgrs();
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.SYMBOL, false);
		//Needed to allow help to be invoked on Analysis action (i.e. attach model, build) via ribbon
		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.ANALYSIS, false);
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new Lifecycle(this);
	}

	protected IResource createResource()
	{
		return new Resource(this);
	}

	@NotNull public Class<? extends IRealm> getRealm()
	{
		return IAbstractLibrary.class;
	}

	public boolean isAnalysisSupported()
	{
		return true;
	}

	public boolean isBridgesSupported()
	{
		return false;
	}

	/**
	 * @return does this caplet edit symbols?
	 */
	public boolean isSymbolEditor()
	{
		return true;
	}

	@Nullable
	public Class<? extends IXBaseAction> getCustomClass()
	{
		return IXSymbolAction.class;
	}

	@Override public boolean isInspectionPanelSupported()
	{
		return false;
	}

	@Override protected void initializeCustomActionDelegate()
	{
		CAFCustomActionMgr.getInstance().setCustomActionDelegate(SymbolCustomActionDelegate.getInstance());
	}

	@Override public int getToolFlowOrder()
	{
		return 3;
	}

	@Override public Type getType()
	{
		return Type.SYMBOL;
	}
}
