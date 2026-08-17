/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2015-2026 Siemens
 */
package chs.caplets.shared.actions;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.ILogicCaplet;
import chs.cof.logical.IFunctionLogicDesign;
import chs.common.IDesignContainer;
import chs.images.CHSImageLoader;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.ApplicationSuiteInfo;
import com.mentor.capital.javafx.interfaces.IRibbonConstants;

import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanArchitect, Application.ArtisanFunction,
				Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_LOGIC_AUTO_ROUTE_NET_ACTION",
		label = "Auto-Route Net",
		tooltip = "Auto-Route Net(Ctrl+R)",
		icon = "ico_autoroute_conductor",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class LogicAutoRouteAction extends AbstractAutoRouteAction
{

	public LogicAutoRouteAction(IFIB fib)
	{
		super(fib);
	}

	protected void initMenu()
	{
		updateSetting();
		putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_autoroute_conductor.png"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK));
	}

	private void updateLogicDesignSettings()
	{
		ApplicationSuiteInfo.AppSuite appSuite = ApplicationSuiteInfo.getInstance().getCurrentApplicationSuite().getAppSuite();
		if(appSuite == ApplicationSuiteInfo.AppSuite.Electrical || appSuite ==  ApplicationSuiteInfo.AppSuite.CapitalEssentials)
		{
			putValue(NAME, ResourceMgr.getString(LogicAutoRouteAction.class, "LogicAutoRouteAction.SEElectrical.AutoRoutingNet.Title"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(LogicAutoRouteAction.class, "LogicAutoRouteAction.SEElectrical.AutoRoutingNet.Desc"));
		}else{
			putValue(NAME, ResourceMgr.getString(LogicAutoRouteAction.class, "LogicAutoRouteAction.AutoRoutingNet.Title"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(LogicAutoRouteAction.class, "LogicAutoRouteAction.AutoRoutingNet.Desc"));
		}

		putValue(SHORT_DESCRIPTION, getValue(NAME));

	}

	private void updateFunctionDesignSettings()
	{
		putValue(NAME, ResourceMgr
				.getString(LogicAutoRouteAction.class, "LogicAutoRouteAction.AutoRoutingFunctionConductor.Title"));
		putValue(SHORT_DESCRIPTION, getValue(NAME));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(LogicAutoRouteAction.class,
						"LogicAutoRouteAction.AutoRoutingFunctionConductor.Desc"));
	}

	@Override public boolean isOn()
	{
		return ConductorRouteAction.getInstance().isEnableNetRouting();
	}

	protected void setEnableRouting(boolean togglingValue)
	{
		ConductorRouteAction.getInstance().setEnableNetRouting(togglingValue);
	}

	@Override public void updateUI()
	{
		updateSetting();
	}

	private void updateSetting()
	{
		if (isConcordCapture()) {
			updateFunctionDesignSettings();
		}
		else {
			updateLogicDesignSettings();
		}
	}

	private boolean isConcordCapture()
	{
		IDesignContainer design = CAFUtils.getInstance().getActiveDesignContainer();
		if (design == null && AppInfo.getAppInfo().getApp() == AppInfo.App.CONCORDFUNCTIONS) {
			return true;
		}
		return design instanceof IFunctionLogicDesign;
	}
}
