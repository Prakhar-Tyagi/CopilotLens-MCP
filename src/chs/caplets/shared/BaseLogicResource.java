/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.shared;

import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.ICapitalAnalysis;
import chs.analysis.scope.ScopeContainerNameUpdater;
import chs.bridges.BridgesIntegrationServices;
import chs.bridges.adaptors.AdaptorFactory;
import chs.bridges.adaptors.IAdaptorFactory;
import chs.bridges.adaptors.IAdaptorPluginMgr;
import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.DeveloperExtensionAppAction;
import chs.caf.IFIB;
import chs.caf.QAExtensionAppAction;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.BaseResource;
import chs.caf.cafmain.MainResources;
import chs.caf.cafmain.actions.ApplyStyleOnDiagramObjectActionUI;
import chs.caf.cafmain.actions.ApplyStyleToAllActionUI;
import chs.caf.cafmain.actions.EditStyleActionUI;
import chs.caf.cafmain.actions.FindReplaceSelectionActionUI;
import chs.caf.cafmain.actions.ForceApplyStyleActionUI;
import chs.caf.cafmain.actions.LogicGenerateNamesActionUI;
import chs.caf.cafmain.actions.ReloadIndicatorActionUI;
import chs.caf.cafmain.actions.ResetOrderActionUI;
import chs.caf.cafmain.actions.SelectByNameAction;
import chs.caf.cafmain.actions.SelectByPropertyAction;
import chs.caf.cafmain.actions.StyleFlyOutActionUI;
import chs.caf.cafmain.actions.WriteToStyleActionUI;
import chs.caf.cafmain.actions.analysis.AttachSVModelActionUI;
import chs.caf.cafmain.actions.analysis.EditModelActionUI;
import chs.caf.cafmain.actions.analysis.ExportNetlistActionUI;
import chs.caf.cafmain.actions.analysis.SubsystemFMEAActionUI;
import chs.caf.cafmain.actions.analysis.SubsystemSCAActionUI;
import chs.caf.cafmain.actions.analysis.SubsystemSimulatorActionUI;
import chs.caf.cafmain.actions.analysis.SubsystemViewNetlistActionUI;
import chs.caf.cafmain.actions.analysis.SubsystemViewProjectNetlistActionUI;
import chs.caf.cafmain.actions.analysis.derating.LogicDeratingActionUI;
import chs.caf.cafmain.actions.bridges.BridgeCAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.designinspector.DesignInspector;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ExcludeFromApplyStyleActionUI;
import chs.caf.caplet.helpers.FillColorPickerActionUI;
import chs.caf.caplet.helpers.FillPatternControlActionUI;
import chs.caf.caplet.helpers.FixPositionActionUI;
import chs.caf.caplet.helpers.FontControlActionUI;
import chs.caf.caplet.helpers.LineStyleAndWeightControlActionUI;
import chs.caf.caplet.helpers.ModifyBorderActionUI;
import chs.caf.caplet.helpers.ModifyGridActionUI;
import chs.caf.caplet.helpers.PrimaryColorPickerActionUI;
import chs.caf.caplet.helpers.SecondaryColorPickerActionUI;
import chs.caf.caplet.helpers.SelectAllLockedDecorationActionUI;
import chs.caf.caplet.helpers.SplitTableActionUI;
import chs.caf.caplet.helpers.TextColorPickerActionUI;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolActionUI;
import chs.caf.caplet.helpers.graphics.CreateImageActionUI;
import chs.caf.caplet.helpers.graphics.CreateTextActionUI;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsActionUI;
import chs.caf.caplet.helpers.graphics.ZOrderDesignInspectorUI;
import chs.caf.caplet.helpers.segment.ToggleChamferActionUI;
import chs.caf.caplet.helpers.snapping.SnapToObjectAction;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caplets.logic.ILogicCaplet;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.AddBackshellActionUI;
import chs.caplets.logic.actions.AddBackshellTerminationActionUI;
import chs.caplets.logic.actions.AddChainActionUI;
import chs.caplets.logic.actions.AddDeviceFromLibraryPartActionUI;
import chs.caplets.logic.actions.AddDeviceFromLibraryWithPinsActionUI;
import chs.caplets.logic.actions.AddDeviceListTableActionUI;
import chs.caplets.logic.actions.AddDiagramListTableActionUI;
import chs.caplets.logic.actions.AddInstanceActionUI;
import chs.caplets.logic.actions.AddLibraryMulticoreActionUI;
import chs.caplets.logic.actions.AddPinActionUI;
import chs.caplets.logic.actions.AddPinListActionUI;
import chs.caplets.logic.actions.AddPinWNAccelActionUI;
import chs.caplets.logic.actions.AddShieldConductorActionUI;
import chs.caplets.logic.actions.AddSpliceFromLibraryPartActionUI;
import chs.caplets.logic.actions.AddWireListTableActionUI;
import chs.caplets.logic.actions.ApplyStyleToSegmentsActionUI;
import chs.caplets.logic.actions.AssociateConnectorActionUI;
import chs.caplets.logic.actions.AssociateSymbolActionUI;
import chs.caplets.logic.actions.AutoRouteSettingsActionUI;
import chs.caplets.logic.actions.ConductorRouteActionImpl;
import chs.caplets.logic.actions.ConnectIndicatorsActionUI;
import chs.caplets.logic.actions.ConvertInlineToPlugJackPairActionUI;
import chs.caplets.logic.actions.CreateBlockDeviceActionUI;
import chs.caplets.logic.actions.CreateChamferedNetInstanceActionUI;
import chs.caplets.logic.actions.CreateChamferedWireInstanceActionUI;
import chs.caplets.logic.actions.CreateCoaxialSheathMulticoreActionUI;
import chs.caplets.logic.actions.CreateCoaxialShieldMulticoreActionUI;
import chs.caplets.logic.actions.CreateConductorActionUI;
import chs.caplets.logic.actions.CreateGeneralHighwayActionUI;
import chs.caplets.logic.actions.CreateICDFromPlacedICDActionUI;
import chs.caplets.logic.actions.CreateInlineInterconnectConnectorActionUI;
import chs.caplets.logic.actions.CreateInterconnectActionUI;
import chs.caplets.logic.actions.CreateInterconnectConnectorActionUI;
import chs.caplets.logic.actions.CreateInterconnectDeviceActionUI;
import chs.caplets.logic.actions.CreateModularSchematicsActionUI;
import chs.caplets.logic.actions.CreateMulticoreActionUI;
import chs.caplets.logic.actions.CreateMultipleNetsActionUI;
import chs.caplets.logic.actions.CreateMultipleWiresActionUI;
import chs.caplets.logic.actions.CreateNoPinDeviceActionUI;
import chs.caplets.logic.actions.CreateNoPinInlineConnectorActionUI;
import chs.caplets.logic.actions.CreateNoPinJackConnectorActionUI;
import chs.caplets.logic.actions.CreateNoPinPlugConnectorActionUI;
import chs.caplets.logic.actions.CreateOverbraidActionUI;
import chs.caplets.logic.actions.CreateOverbraidSheathActionUI;
import chs.caplets.logic.actions.CreateRingTerminalActionUI;
import chs.caplets.logic.actions.CreateSectorActionUI;
import chs.caplets.logic.actions.CreateShieldConductorActionUI;
import chs.caplets.logic.actions.CreateSingleLineActionUI;
import chs.caplets.logic.actions.CreateSpliceActionUI;
import chs.caplets.logic.actions.CreateTwistedSheathMulticoreActionUI;
import chs.caplets.logic.actions.CreateWireActionUI;
import chs.caplets.logic.actions.CrossLinkActionUI;
import chs.caplets.logic.actions.DisassociateConnectorActionUI;
import chs.caplets.logic.actions.EditFootprintActionUI;
import chs.caplets.logic.actions.EditSharedOverbraidActionUI;
import chs.caplets.logic.actions.FluidAddPinActionUI;
import chs.caplets.logic.actions.FluidCreateMultipleWiresActionUI;
import chs.caplets.logic.actions.FluidCreateNoPinDeviceActionUI;
import chs.caplets.logic.actions.FluidCreateWireActionUI;
import chs.caplets.logic.actions.JoinPinlistsActionUI;
import chs.caplets.logic.actions.ManageConnectorsActionUI;
import chs.caplets.logic.actions.SelectFootprintActionUI;
import chs.caplets.logic.actions.ShowStackUsageActionUI;
import chs.caplets.logic.actions.SliceActionUI;
import chs.caplets.logic.actions.ToggleIndicatorConstraintsActionUI;
import chs.caplets.logic.actions.UnplaceActionUI;
import chs.caplets.logic.actions.analysis.DynSimBackgroundActionUI;
import chs.caplets.logic.actions.analysis.DynSimOffActionUI;
import chs.caplets.logic.actions.analysis.DynSimOnDemandActionUI;
import chs.caplets.logic.actions.analysis.EnableRobustNetlistingAction;
import chs.caplets.logic.actions.analysis.LogicAttachModelActionUI;
import chs.caplets.logic.actions.analysis.LogicBuildModelActionUI;
import chs.caplets.logic.actions.analysis.LogicStressActionUI;
import chs.caplets.logic.actions.analysis.MacroRecordingActionUI;
import chs.caplets.logic.actions.analysis.QualitativeSimulationModeActionUI;
import chs.caplets.logic.actions.analysis.ResetActionUI;
import chs.caplets.logic.actions.analysis.SimulateActionUI;
import chs.caplets.logic.actions.analysis.SpiceSimulationModeActionUI;
import chs.caplets.logic.actions.analysis.ViewAnalysisConsoleAction;
import chs.caplets.logic.actions.analysis.ViewAnalysisOutputTabAction;
import chs.caplets.logic.actions.bridges.BridgeOutFilterActionUI;
import chs.caplets.logic.actions.shared.AddConductorActionUI;
import chs.caplets.logic.actions.shared.AddGeneralHighwayActionUI;
import chs.caplets.logic.actions.shared.AddSharedDeviceActionUI;
import chs.caplets.logic.actions.shared.AddSharedICDActionUI;
import chs.caplets.logic.actions.shared.AddSharedInlineConnectorActionUI;
import chs.caplets.logic.actions.shared.AddSharedInterconnectConnectorActionUI;
import chs.caplets.logic.actions.shared.AddSharedInterconnectDeviceActionUI;
import chs.caplets.logic.actions.shared.AddSharedJackConnectorActionUI;
import chs.caplets.logic.actions.shared.AddSharedPlugConnectorActionUI;
import chs.caplets.logic.actions.shared.AddSharedRingTerminalActionUI;
import chs.caplets.logic.actions.shared.AddSharedSpliceActionUI;
import chs.caplets.logic.actions.shared.AddSingleLineActionUI;
import chs.caplets.logic.actions.shared.CreateSharedConductorGroupActionUI;
import chs.caplets.logic.actions.shared.ShareActionUI;
import chs.caplets.logic.actions.tests.HackVisibilityActionUI;
import chs.caplets.shared.actions.LogicAutoRouteAction;
import chs.caplets.shared.actions.SelectActionUI;
import chs.caplets.shared.actions.ToggleSubGridAction;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.symbol.IZoneIdentifier;
import chs.cofUtils.scrubber.OnTheFlyScrubber;
import chs.common.IDesignContainer;
import chs.images.CHSImageLoader;
import chs.utilities.BuildInfo;
import chs.utilities.CapabilityHelper;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.SupportedFeatureInfo;
import chs.utility.AnalysisHelper;
import chs.utility.persist.DesignPersistenceUtils;
import com.mentor.capital.ui.IToggleAction;
import com.mentor.chs.plugin.designinspection.IXInspectionPanel;
import com.mentor.chs.plugin.designinspection.IXLogicInspectionPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

/**
 * Resource initialization common to Logic and Capture
 */
public abstract class BaseLogicResource extends BaseResource
{

	protected BaseLogicResource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	protected void initMenus(CapletResourceBuilder rb)
	{
		super.initMenus(rb);

		ActionContainer tempMenu = new ActionContainer("Temp");
		menus.add(tempMenu);
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		super.initActions();

		new SelectActionUI(caplet);
		new FindReplaceSelectionActionUI(caplet);
		new ConnectIndicatorsActionUI(caplet);

		new SimulateActionUI(caplet);
		new ResetActionUI(caplet);
		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			new QualitativeSimulationModeActionUI(caplet);
			new SpiceSimulationModeActionUI(caplet);
		}
		new AddConductorActionUI(caplet);
		new AddGeneralHighwayActionUI(caplet);
		new AddSingleLineActionUI(caplet);
		new AddPinListActionUI(caplet);
		new CreateICDFromPlacedICDActionUI(caplet);
		new UnplaceActionUI(caplet);
		new AddPinWNAccelActionUI(caplet);
		new SliceActionUI(caplet);
		new JoinPinlistsActionUI(caplet);
		new AssociateConnectorActionUI(caplet);
		new DisassociateConnectorActionUI(caplet);
		new ShareActionUI(caplet);
		new AddSharedICDActionUI(caplet);
		new ManageConnectorsActionUI(caplet);
		new SelectFootprintActionUI(caplet);
		new EditFootprintActionUI(caplet);
		new CreateChamferedNetInstanceActionUI(caplet);
		new CreateChamferedWireInstanceActionUI(caplet);
		new ConvertInlineToPlugJackPairActionUI(caplet);
		new CreateModularSchematicsActionUI(caplet);
	}

	/**
	 * Overridden here to provide Tools actions specific to Logic derivatives
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	@Override protected void initToolsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initToolsMenu(rb, menu);
		if (qaExtensions) {
			rb.addAppAction(new CorruptUsagesAction(caplet.getFIB()), menu);
		}
	}

	@SuppressWarnings({"UNUSED_SYMBOL"}) protected void addAnalysisActions()
	{
		// Analysis Menu (if analysis is available)
		ActionContainer analysisMenu =
				CapletResourceBuilder.createAnalysisMenu();

		ActionContainer analysisComponentMenu = new ActionContainer(
				ResourceMgr.getString(BaseLogicResource.class, "Resource.AnalysisComponentSubmenu.Title.text"),
				true,
				CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"),
				KeyEvent.VK_C,
				"Analysis Component Menu");
		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			analysisComponentMenu.add(new ActionEntry(new LogicAttachModelActionUI(caplet)));
			analysisComponentMenu.add(new ActionEntry(new LogicBuildModelActionUI(caplet)));
			analysisComponentMenu.add(new ActionEntry(new EditModelActionUI(caplet)));
		}
		else {
			analysisComponentMenu.add(new ActionEntry(new AttachSVModelActionUI(caplet)));
		}
		analysisComponentMenu.add(new ActionEntry(new AssociateSymbolActionUI(caplet)));
		analysisMenu.add(analysisComponentMenu);

		ActionContainer analysisSubsystemMenu = new ActionContainer(
				ResourceMgr.getString(BaseLogicResource.class, "Resource.AnalysisSubsystemSubmenu.Title.text"),
				true,
				CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"),
				KeyEvent.VK_D,
				"Analysis Subsystem Menu");
		analysisSubsystemMenu.add(new ActionEntry(new SubsystemSimulatorActionUI(caplet)));
		analysisSubsystemMenu.add(new ActionEntry(new SubsystemFMEAActionUI(caplet)));
		analysisSubsystemMenu.add(new ActionEntry(new SubsystemSCAActionUI(caplet)));
		analysisSubsystemMenu.add(new ActionEntry(new LogicStressActionUI(caplet)));
		analysisSubsystemMenu.add(new ActionEntry(new LogicDeratingActionUI(caplet)));
		analysisSubsystemMenu.add(new ActionEntry(new ExportNetlistActionUI(caplet)));
		analysisMenu.add(analysisSubsystemMenu);

		analysisMenu.add(new ActionEntry(new SubsystemViewNetlistActionUI(caplet)));
		analysisMenu.add(new ActionEntry(new SubsystemViewProjectNetlistActionUI(caplet)));

		menus.add(analysisMenu);

		ICapitalAnalysis capitalAnalysis = CapitalAnalysisFactory.getAnalysisInterface();
		if (capitalAnalysis != null) {
			new ScopeContainerNameUpdater(
					analysisSubsystemMenu); // this creates and registers a listener for the change in scopes.
		}
	}

	protected ActionContainer analysisBrowserToolbar;
	protected ActionContainer scopeAction;
	protected IActionUI spiceSimAction;
	protected IActionUI qualSimAction;

	public IActionUI getQualSimAction()
	{
		return qualSimAction;
	}

	public IActionUI getSpiceSimAction()
	{
		return spiceSimAction;
	}

	protected void initAnalysisBrowserToolbar()
	{
		// create the container for all the UI actions...
		analysisBrowserToolbar = new ActionContainer("Analysis");
		// create the container for all the scope selection UIs
		scopeAction = new ActionContainer("ScopeAction", true);
		scopeAction.putValue(Action.SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/as_set_scope.gif"));
		// add the scope action to the browser toolbar
		analysisBrowserToolbar.add(scopeAction);

		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			// create the simulation type drop down
			ActionContainer simType = new ActionContainer("Simulation Type");
			analysisBrowserToolbar.add(simType);

			// Add the available sim types
			spiceSimAction = new SpiceSimulationModeActionUI(caplet);
			qualSimAction = new QualitativeSimulationModeActionUI(caplet);
			simType.add(new ActionEntry(spiceSimAction));
			simType.add(new ActionEntry(qualSimAction));
		}

		// separate the possible modes of simulation from the other scopes.
		analysisBrowserToolbar.add(new ActionSeparator());

		// add the possible modes of simulation
		analysisBrowserToolbar.add(new ActionEntry(new DynSimBackgroundActionUI(caplet)));
		analysisBrowserToolbar.add(new ActionEntry(new DynSimOnDemandActionUI(caplet)));
		analysisBrowserToolbar.add(new ActionEntry(new DynSimOffActionUI(caplet)));

		// add the ability to record a macro
		analysisBrowserToolbar.add(new ActionEntry(new MacroRecordingActionUI(caplet)));

		// add a separator before showing the console and output tabs...
		analysisBrowserToolbar.add(new ActionSeparator());

		analysisBrowserToolbar.add(new ActionEntry(new ViewAnalysisConsoleAction(caplet.getFIB())));
		analysisBrowserToolbar.add(new ActionEntry(new ViewAnalysisOutputTabAction(caplet.getFIB())));

		// add an entry for robust netlisting
		ActionEntry robustNetlisting = new ActionEntry(new EnableRobustNetlistingAction(caplet.getFIB()));
		analysisBrowserToolbar.add(robustNetlisting);

		// add a separator before showing the tools drop down...
		analysisBrowserToolbar.add(new ActionSeparator());
		ActionContainer toolsDropDown = new ActionContainer("Tools");
		toolsDropDown.putValue(Action.SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/as_multi_action.gif"));

		toolsDropDown.add(new ActionEntry(new SubsystemSimulatorActionUI(caplet)));
		if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.ADVANCED_ANALYSIS)) {
			if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
				toolsDropDown.add(new ActionEntry(new SubsystemFMEAActionUI(caplet)));
				toolsDropDown.add(new ActionEntry(new SubsystemSCAActionUI(caplet)));
			}
			toolsDropDown.add(new ActionEntry(new LogicStressActionUI(caplet)));
		}
		toolsDropDown.add(new ActionEntry(new ExportNetlistActionUI(caplet)));
		analysisBrowserToolbar.add(toolsDropDown);
		//Add this to the menu in order for the Ribbon framework to parse the actions.
		menus.add(analysisBrowserToolbar);
	}

	public ActionContainer getAnalysisBrowserToolbar()
	{
		return analysisBrowserToolbar;
	}

	public ActionContainer getScopeAction()
	{
		return scopeAction;
	}

	protected void addBridgesActions(CapletResourceBuilder rb)
	{
		// Bridge menu
		// Use non short-circuited "or" for licensing checks
		//noinspection NonShortCircuitBooleanExpression
		boolean bridgesAvailable = BridgesIntegrationServices.bridgesAvailable(this) |
				BridgesIntegrationServices.bridgeHandshakesAvailable(this);
		if (bridgesAvailable) {
			ActionContainer bridgeMenu = CapletResourceBuilder.createBridgesMenu();
			bridgeMenu.add(new ActionEntry(new BridgeOutFilterActionUI(getCaplet())));
			BridgeCAFUtils.createBridgesCapletMenuItems(getCaplet(), bridgeMenu);
			extendBridgeMenuWithV4Plugin(bridgeMenu);
			menus.add(bridgeMenu);
		}
	}

	protected void initFileMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initFileMenu(rb, menu);

		ActionContainer synchronizeMenu = CapletResourceBuilder.createSubContainer("Synchronize", MainResources.class);
		menu.add(synchronizeMenu);

		super.initPrintRegionMenu(rb, menu);
	}

	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
		menu.add(new ActionSeparator());
		rb.addActionUI(new CrossLinkActionUI(caplet), menu);
		rb.addActionUI(new ShowStackUsageActionUI(caplet), menu);
	}

	protected void initAddMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initAddMenu(rb, menu);
		// nothing happens in the base class
		initAddMenuForLogicObjects(rb, menu);
	}

	protected void initAddMenuForLogicObjects(CapletResourceBuilder rb, ActionContainer menu)
	{
		// these names are repeated on several menus...
		Class<?> cls = BaseLogicResource.class;
		String deviceMenuName = ResourceMgr.getString(cls, "Resource.Device.menu.name");
		String deviceWithPinsMenuname = ResourceMgr.getString(cls, "Resource.DeviceWithPins.menu.name");
		String blockMenuName = ResourceMgr.getString(cls, "Resource.Block.menu.name");
		String plugMenuName = ResourceMgr.getString(cls, "Resource.Plug.menu.name");
		String inlineMenuName = ResourceMgr.getString(cls, "Resource.Inline.menu.name");
		String jackMenuName = ResourceMgr.getString(cls, "Resource.Jack.menu.name");
		String multicoreMenuName = ResourceMgr.getStringForMenu(cls, "Resource.Multicore.menu.name");
		String overbraidMenuName = ResourceMgr.getStringForMenu(cls, "Resource.Overbraid.menu.name");
		String spliceMenuName = ResourceMgr.getString(cls, "Resource.Splice.menu.name");
		String ringterminalMenuName = ResourceMgr.getString(cls, "Resource.RingTerminal.menu.name");

		rb.addActionUI(new CreateNoPinDeviceActionUI(caplet), menu, deviceMenuName);
		rb.addActionUI(new FluidCreateNoPinDeviceActionUI(caplet), menu);
		rb.addActionUI(new CreateBlockDeviceActionUI(caplet), menu, blockMenuName);
		rb.addActionUI(new AddPinActionUI(caplet), menu);
		rb.addActionUI(new FluidAddPinActionUI(caplet), menu);
		rb.addActionUI(new AddInstanceActionUI(caplet), menu);
		rb.addActionUI(new CreateNoPinPlugConnectorActionUI(caplet), menu, plugMenuName);
		rb.addActionUI(new CreateNoPinInlineConnectorActionUI(caplet), menu, inlineMenuName);
		rb.addActionUI(new CreateNoPinJackConnectorActionUI(caplet), menu, jackMenuName);
		rb.addActionUI(new CreateConductorActionUI(caplet), menu);
		rb.addActionUI(new CreateGeneralHighwayActionUI(caplet), menu);
		rb.addActionUI(new CreateSingleLineActionUI(caplet), menu);
		rb.addActionUI(new CreateWireActionUI(caplet), menu);
		rb.addActionUI(new FluidCreateWireActionUI(caplet), menu);

		rb.addActionUI(new CreateShieldConductorActionUI(caplet), menu);
		rb.addActionUI(new AddShieldConductorActionUI(caplet), menu);
		rb.addActionUI(new AddChainActionUI(caplet), menu);
		rb.addActionUI(new CreateSectorActionUI(caplet), menu);
		// need a different mnemonic here because the same action is (deliberately) available in 2 different menus
		// Edit > Multicores > Generic and Add > Multicore
		// seems funny to me but marketing are sure ...
		ActionUI actionUI = new CreateMulticoreActionUI(caplet);
		actionUI.putValue(Action.MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(cls, "Resource.Add.Multicore.menu.mnemonic"));
		rb.addActionUI(actionUI, menu, multicoreMenuName);
		rb.addActionUI(new CreateTwistedSheathMulticoreActionUI(caplet), menu);
		rb.addActionUI(new CreateCoaxialShieldMulticoreActionUI(caplet), menu);
		rb.addActionUI(new CreateCoaxialSheathMulticoreActionUI(caplet), menu);
		rb.addActionUI(new CreateOverbraidActionUI(caplet), menu);
		rb.addActionUI(new CreateOverbraidSheathActionUI(caplet), menu);

		rb.addActionUI(new CreateSpliceActionUI(caplet), menu);

		rb.addActionUI(new CreateRingTerminalActionUI(caplet), menu, ringterminalMenuName);

		rb.addActionUI(new AddBackshellActionUI(caplet), menu);
		rb.addActionUI(new AddBackshellTerminationActionUI(caplet), menu);

		// Add > Multiple conductor Submenu
		menu.add(new ActionSeparator());
		ActionContainer connectionsByMenu = CapletResourceBuilder.createSubContainer("ConnectionsBy", cls);
		rb.addActionUI(new CreateMultipleNetsActionUI(caplet), connectionsByMenu);
		rb.addActionUI(new CreateMultipleWiresActionUI(caplet), connectionsByMenu);
		rb.addActionUI(new FluidCreateMultipleWiresActionUI(caplet), connectionsByMenu);
		menu.add(connectionsByMenu);

		// Add > Shared Submenu
		menu.add(new ActionSeparator());
		ActionContainer sharedMenu = CapletResourceBuilder.createSubContainer("Shared", cls);
		rb.addActionUI(new AddSharedDeviceActionUI(caplet), sharedMenu, deviceMenuName);
		rb.addActionUI(new AddSharedInlineConnectorActionUI(caplet), sharedMenu, inlineMenuName);
		rb.addActionUI(new AddSharedPlugConnectorActionUI(caplet), sharedMenu, plugMenuName);
		rb.addActionUI(new AddSharedJackConnectorActionUI(caplet), sharedMenu, jackMenuName);
		// need a different mnemonic here because the same action is (deliberately) available in 2 different menus
		actionUI = new CreateSharedConductorGroupActionUI(caplet);
		actionUI.putValue(Action.MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(cls, "Resource.Add.Shared.Multicore.menu.mnemonic"));
		rb.addActionUI(actionUI, sharedMenu, multicoreMenuName);
		actionUI = new EditSharedOverbraidActionUI(caplet);
		actionUI.putValue(Action.MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(cls, "Resource.Add.Shared.Overbraid.menu.mnemonic"));
		rb.addActionUI(actionUI, sharedMenu, overbraidMenuName);
		rb.addActionUI(new AddSharedSpliceActionUI(caplet), sharedMenu, spliceMenuName);
		rb.addActionUI(new AddSharedRingTerminalActionUI(caplet), sharedMenu, ringterminalMenuName);
		menu.add(sharedMenu);

		// Add > From Library Submenu
		ActionContainer fromLibraryMenu = CapletResourceBuilder.createSubContainer("FromLibrary", cls);
		rb.addActionUI(new AddDeviceFromLibraryPartActionUI(caplet), fromLibraryMenu, deviceMenuName);
		rb.addActionUI(new AddDeviceFromLibraryWithPinsActionUI(caplet), fromLibraryMenu, deviceWithPinsMenuname);
		rb.addActionUI(new AddLibraryMulticoreActionUI(caplet), fromLibraryMenu, multicoreMenuName);
		rb.addActionUI(new AddSpliceFromLibraryPartActionUI(caplet), fromLibraryMenu, spliceMenuName);
		menu.add(fromLibraryMenu);

		// Add > From Interconnect Submenu
		if (Environment.isInterconnectFlowAllowed()) {
			ActionContainer interconnectMenu =
					CapletResourceBuilder.createSubContainer("Interconnect", cls);
			rb.addActionUI(new CreateInterconnectDeviceActionUI(caplet), interconnectMenu);
			rb.addActionUI(new CreateInterconnectConnectorActionUI(caplet), interconnectMenu);
			rb.addActionUI(new CreateInlineInterconnectConnectorActionUI(caplet), interconnectMenu);
			rb.addActionUI(new CreateInterconnectActionUI(caplet), interconnectMenu);

			interconnectMenu.add(new ActionSeparator());
			ActionContainer sharedInterconnectMenu = CapletResourceBuilder.createSubContainer("Shared", cls);
			rb.addActionUI(new AddSharedInterconnectDeviceActionUI(caplet), sharedInterconnectMenu);
			rb.addActionUI(new AddSharedInterconnectConnectorActionUI(caplet), sharedInterconnectMenu);
			interconnectMenu.add(sharedInterconnectMenu);
			menu.add(interconnectMenu);
		}
	}

	protected void initGraphicsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new FillColorPickerActionUI(caplet), menu);
		rb.addActionUI(new TextColorPickerActionUI(caplet), menu);
		rb.addActionUI(new PrimaryColorPickerActionUI(caplet), menu);
		rb.addActionUI(new SecondaryColorPickerActionUI(caplet), menu);
		rb.addActionUI(new FontControlActionUI(caplet), menu);
		rb.addActionUI(new LineStyleAndWeightControlActionUI(caplet), menu);
		rb.addActionUI(new FillPatternControlActionUI(caplet), menu);

		rb.addActionUI(new ModifyBorderActionUI(caplet), menu);
		ActionContainer tablesMenu = CapletResourceBuilder.createSubContainer("Tables", BaseResource.class);
		rb.addActionUI(new AddDiagramListTableActionUI(caplet), tablesMenu);
		rb.addActionUI(new AddDeviceListTableActionUI(caplet), tablesMenu);
		rb.addActionUI(new AddWireListTableActionUI(caplet), tablesMenu);
		menu.add(tablesMenu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new ToggleIndicatorConstraintsActionUI(caplet), menu);
		rb.addActionUI(new CreateTextActionUI(caplet), menu);

		ActionContainer addShapeMenu = CapletResourceBuilder.createSubContainer("AddShape", BaseResource.class);
		initAddShapeMenus(rb, addShapeMenu);
		menu.add(addShapeMenu);

		ActionContainer addDraftingMenu = CapletResourceBuilder.createSubContainer("AddDrafting", BaseResource.class);
		initAddDraftingMenus(rb, addDraftingMenu);
		addMenu(menu, addDraftingMenu);

		String menuName = ResourceMgr.getString(BaseResource.class, "Resource.AddImage.menu.name");
		rb.addActionUI(new CreateImageActionUI(caplet), menu, menuName);
		menuName = ResourceMgr.getString(BaseResource.class, "Resource.AddCommentSymbol.menu.name");
		rb.addActionUI(new AddCommentSymbolActionUI(caplet), menu, menuName);
		menuName = ResourceMgr.getString(BaseResource.class, "Resource.SymbolPlaceAsGraphics.menu.name");
		rb.addActionUI(new SymbolPlaceAsGraphicsActionUI(caplet), menu, menuName);

		initGraphicsPointSubMenu(rb, menu);
		ActionContainer gripPointActionContainer = initGripPointSubMenu(rb, menu);
		rb.addActionUI(new ToggleChamferActionUI(caplet), gripPointActionContainer);

		initGroupingMenus(rb, menu);
		initZOrderSubMenu(rb, menu);
		initFlyOutGraphicsMenu(rb, menu);
	}

	protected void initLayoutMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		ActionContainer alignOrDistributeMenu =
				CapletResourceBuilder.createSubContainer("AlignOrDistribute", BaseResource.class);
		initAlignOrDistrubuteMenus(rb, alignOrDistributeMenu);
		menu.add(alignOrDistributeMenu);

		ActionContainer rotateOrFlipMenu = CapletResourceBuilder.createSubContainer("RotateOrFlip", BaseResource.class);
		initRotateOrFlipMenus(rb, rotateOrFlipMenu);
		menu.add(rotateOrFlipMenu);

		ActionContainer gridMenu = CapletResourceBuilder.createSubContainer("Grid", BaseResource.class);
		gridMenu.add(new ActionCheckBox(getToggleAppAction(ToggleSubGridAction.class)));
		gridMenu.add(new ActionCheckBox(getToggleAppAction(SnapToObjectAction.class)));

		rb.addActionUI(new ModifyGridActionUI(caplet), gridMenu);
		menu.add(gridMenu);

		ActionContainer routingMenu = CapletResourceBuilder.createSubContainer("Routing", BaseResource.class);
		routingMenu.add(new ActionCheckBox(getToggleAppAction(RouteOrthogonalAction.class)));
		ConductorRouteActionImpl.init();
		routingMenu.add(new ActionCheckBox(getToggleAppAction(DisableAutoRoutingAction.class)));
		routingMenu.add(new ActionCheckBox(getToggleAppAction(LogicAutoRouteAction.class)));
		routingMenu.add(new ActionCheckBox(getToggleAppAction(TraverseRoutingAction.class)));
		routingMenu.add(new ActionCheckBox(getToggleAppAction(FullSignalRoutingAction.class)));
		routingMenu.add(new ActionCheckBox(getToggleAppAction(ThreePhaseRoutingAction.class)));
		if (!BuildInfo.getBuildInfo().isOfficialRelease() && (
				BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
						BuildInfo.getBuildInfo().areQAExtensionsEnabled())) {
			rb.addActionUI(new AutoRouteSettingsActionUI(caplet), routingMenu);
		}
		menu.add(routingMenu);
	}

	protected void initDevTestMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initDevTestMenu(rb, menu);
		ActionContainer validationMenu = new ActionContainer("Validation", true);
		menu.add(validationMenu);
		ActionContainer graphicsMenu = new ActionContainer("Graphics", true);
		menu.add(graphicsMenu);

		menu.add(new ActionCheckBox(new Delay30sOnSaveDEVAction(), false));
		menu.add(new ActionEntry(new HackVisibilityActionUI(caplet)));

		// Control if on the fly scrubbing occurs.  By default - DOES NOT in QA or DEV extentions, but maybe turned on
		// with this menu option.  We would prefer we get validation errors, users would not.
		// Add to Validation submenu
		validationMenu.add(new ActionCheckBox(new OnTheFlyScrubbingDEVAction(), false));

		// Add to Graphics submenu
		graphicsMenu.add(new ActionEntry(new ReloadIndicatorActionUI(caplet)));
	}

	protected void initToolbars(CapletResourceBuilder rb)
	{
		super.initToolbars(rb);
		ActionContainer toolbar = new ActionContainer("Logic");
		initLogicToolbar(rb, toolbar);
		initAnalysisBrowserToolbar();
		toolbars.add(toolbar);
	}

	@Override protected void initEditToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		super.initEditToolbar(rb, toolbar);
		rb.addActionUIEntry(SliceActionUI.class, toolbar);
	}

	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initEditMenu(rb, menu);
		rb.addActionUIEntry(SliceActionUI.class, menu);
		rb.addActionUIEntry(JoinPinlistsActionUI.class, menu);
		rb.addActionUIEntry(ManageConnectorsActionUI.class, menu);
		rb.addActionUIEntry(ConvertInlineToPlugJackPairActionUI.class, menu);
	}

	protected Class<?> getSelectActionClass()
	{
		return SelectActionUI.class;
	}

	protected abstract void initLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar);

	protected void initDesignInspectors()
	{
		super.initDesignInspectors();
		// Register the Z-Order design inspector view...
		if (BuildInfo.getBuildInfo().areDeveloperOrQAExtensionsEnabled()) {
			DesignInspector.registerDefaultView(getInspectionPanelClazz(), new ZOrderDesignInspectorUI());
		}
	}

	protected Class<? extends IXInspectionPanel> getInspectionPanelClazz()
	{
		return IXLogicInspectionPanel.class;
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
					Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction,
					Application.ArtisanArchitect, Application.SEElectricalDesign})
	@ImmersedAction(actionId = "CAPITAL_ORTHOGONAL_ROUTE_ACTION",
			label = "Orthogonal Route",
			tooltip = "Orthogonal Route",
			icon = "orthogonal_route",
			buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
	public static class RouteOrthogonalAction extends AppAction implements IToggleAction
	{

		public RouteOrthogonalAction(IFIB fib)
		{
			super(fib);
			putValue(NAME, ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text"));
			putValue(SHORT_DESCRIPTION, getValue(NAME));
			updateLongDescription();
			putValue(MNEMONIC_KEY, KeyEvent.VK_O);
			putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		}

		public void actionPerformed(ActionEvent e)
		{
			Model model = getInnerLogicModel();
			if (model != null) {
				model.setOrthogonal(!isOn());
			}
		}

		@Nullable private Model getInnerLogicModel()
		{
			ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
			if (capView != null) {
				ICapletModel capModel = capView.getCapletModel();
				if (capModel instanceof Model) {
					return (Model) capModel;
				}
			}
			return null;
		}

		@Override public boolean isOn()
		{
			Model model = getInnerLogicModel();
			if (model != null) {
				return model.getOrthogonal();
			}
			return false;
		}

		@Override public void updateUI()
		{
			updateLongDescription();
		}

		private void updateLongDescription()
		{
			IDesignContainer design = CAFUtils.getInstance().getActiveDesignContainer();
			String longDes;
			if (design instanceof IFunctionLogicDesign) {
				longDes = ResourceMgr.getString(BaseLogicResource.class,
						"Resource.RouteOrthogonalAction.FunctionCond.longDesc.text_1");
			}
			else {
				longDes = ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text_1");
			}
			putValue(LONG_DESCRIPTION, longDes);
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
					Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign})
	@ImmersedAction(actionId = "CAPITAL_AUTO_ROUTE_SIGNAL_ACTION",
			label = "Auto Route Signal",
			tooltip = "Auto Route Signal",
			icon = "auto_route_signal",
			buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
	public static class TraverseRoutingAction extends AppAction implements IToggleAction
	{

		public TraverseRoutingAction(IFIB fib)
		{
			super(fib);
			putValue(NAME, ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text2"));
			putValue(SHORT_DESCRIPTION, getValue(NAME));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text_5"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_autoroute_signal.png"));
		}

		@Override public boolean isOn()
		{
			return ConductorRouteAction.getInstance().isEnableTraverseRouting();
		}

		public void actionPerformed(ActionEvent e)
		{
			ICaplet activeCaplet = CAFUtils.getInstance().getActiveCapletController().getCaplet();
			if (activeCaplet instanceof ILogicCaplet) {
				ConductorRouteAction.getInstance().setEnableTraverseRouting(!isOn());
			}
		}

		@Override public void updateUI()
		{
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
					Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign})
	@ImmersedAction(actionId = "CAPITAL_AUTO_ROUTE_FULL_SIGNAL_ACTION",
			label = "Auto Route Full Signal",
			tooltip = "Auto Route Full Signal",
			icon = "auto_route_full_signal",
			buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
	public static class FullSignalRoutingAction extends AppAction implements IToggleAction
	{

		public FullSignalRoutingAction(IFIB fib)
		{
			super(fib);
			putValue(NAME, ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text3"));
			putValue(SHORT_DESCRIPTION, getValue(NAME));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text_6"));
			//putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_autoroute_full_signal.png"));
		}

		@Override public boolean isOn()
		{
			return ConductorRouteAction.getInstance().isEnableFullSignalRouting();
		}

		public void actionPerformed(ActionEvent e)
		{
			ICaplet activeCaplet = CAFUtils.getInstance().getActiveCapletController().getCaplet();
			if (activeCaplet instanceof ILogicCaplet) {
				ConductorRouteAction.getInstance().setEnableFullSignalRouting(!isOn());
			}
		}

		@Override public void updateUI()
		{
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
					Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign})
	@ImmersedAction(actionId = "CAPITAL_THREE_PHASE_CONNECTION_ACTION",
			label = "Three Phase Connection",
			tooltip = "Three Phase Connection",
			icon = "three_phase_connection",
			buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
	public static class ThreePhaseRoutingAction extends AppAction implements IToggleAction
	{

		public ThreePhaseRoutingAction(IFIB fib)
		{
			super(fib);
			putValue(NAME, ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text4"));
			putValue(SHORT_DESCRIPTION, getValue(NAME));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text_7"));
			//putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_autoroute_3phaseconn.png"));
		}

		@Override public boolean isOn()
		{
			return ConductorRouteAction.getInstance().isThreePhaseRouting();
		}

		public void actionPerformed(ActionEvent e)
		{
			ICaplet activeCaplet = CAFUtils.getInstance().getActiveCapletController().getCaplet();
			if (activeCaplet instanceof ILogicCaplet) {
				ConductorRouteAction.getInstance().setEnableThreePhaseRouting(!isOn());
			}
		}

		@Override public void updateUI()
		{
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
					Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign})
	@ImmersedAction(actionId = "CAPITAL_DISABLE_AUTO_ROUTE_ACTION",
			label = "Disable Auto Route",
			tooltip = "Disable Auto Route",
			icon = "disable_auto_route",
			buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
	public static class DisableAutoRoutingAction extends AppAction implements IToggleAction
	{

		public DisableAutoRoutingAction(IFIB fib)
		{
			super(fib);
			putValue(NAME, ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text5"));
			putValue(SHORT_DESCRIPTION, getValue(NAME));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(BaseLogicResource.class, "Resource.putValue.action.text_8"));
			//putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_autoroute_off.png"));
		}

		@Override public boolean isOn()
		{

			return !(ConductorRouteAction.getInstance().isEnableNetRouting() ||
					ConductorRouteAction.getInstance().isEnableTraverseRouting() ||
					ConductorRouteAction.getInstance().isEnableFullSignalRouting() ||
					ConductorRouteAction.getInstance().isThreePhaseRouting());
		}

		public void actionPerformed(ActionEvent e)
		{
			ICaplet activeCaplet = CAFUtils.getInstance().getActiveCapletController().getCaplet();
			if (activeCaplet instanceof ILogicCaplet) {
				ConductorRouteAction.getInstance().setRoutingOptions(false, false, false, false);
			}
		}

		@Override public void updateUI()
		{
		}
	}

	private void extendBridgeMenuWithV4Plugin(ActionContainer menu)
	{
		// Add plugin specific menus
		IAdaptorFactory adptFact = AdaptorFactory.getFactory();
		Iterator<IAdaptorPluginMgr> plugins = null;
		if (adptFact != null) {
			try {
				plugins = adptFact.getPlugins();
			}
			catch (Exception ignored) {
			}
		}

		if (plugins != null) {
			boolean addedSeparator = false;
			String thisAppli = "logic";
			while (plugins.hasNext()) {
				IAdaptorPluginMgr plugin = plugins.next();
				if (thisAppli.equalsIgnoreCase(plugin.getApplication())) {
					// Insert separator
					if (!addedSeparator) {
						menu.add(new ActionSeparator());
						addedSeparator = true;
					}

					// Create plugin's specific sub-menu
					plugin.setFIB(getCaplet().getFIB());
					plugin.buildMenu(menu);
				}
			}
		}
	}

	/**
	 * This action deliberately corrupts usages in the active diagram, in order to test that on the fly scrubbing and
	 * call-home is working.
	 */
	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
					Application.ArtisanFunction, Application.SEElectricalDesign})
	static class CorruptUsagesAction extends QAExtensionAppAction
	{

		// TODO jacobt FEAT14396 : OTF scrub of usages has been removed in 10.1 - what should we do with this debug action?
		protected CorruptUsagesAction(IFIB fib)
		{
			super(fib);
		}

		public void actionPerformed(ActionEvent e)
		{
			// corrupt the active design
			IBaseDiagram diagram = CAFUtils.getInstance().getActiveDiagram();
			if (diagram instanceof ISchemDiagram) {
				ILogicDesign design = ((ISchemDiagram) diagram).getDesign();
				IDesignSharedUsageMgr dsum = design.getSharedUsageMgr();
				for (IDesignSharedUsage usage : dsum.getAllUsages()) {
					// NOTE : it seems strangely difficult to corrupt usages from a test
					// we'll rely on the only mutator we have on IDesignSharedUsage, which probably shouldn't be there anyway
					usage.setZoneKey(new IZoneIdentifier()
					{

						public String getName()
						{
							return "Zone name deliberate corrupted by CorruptUsagesAction";
						}

						public String getRowName()
						{
							return "Zone row name deliberate corrupted by CorruptUsagesAction";
						}

						public String getColumnName()
						{
							return "Zone column name deliberate corrupted by CorruptUsagesAction";
						}

						@NotNull @Override public String getUserDefinedName()
						{
							return "rubbish";
						}
					});
				}

				// trigger validation - on the fly scrubbing and call-home dialogs should occur here
				// note that this is not a controller action - testing that validation is triggered by controller actions  should be handled elsewhere
				// note also that OTF scrubbing seems only to occur from UndoableContainerHelper.validateObjectChanges
				CAFUtils.getInstance().getActiveCapletController().getUndoableContainer().validateObjectChanges(design);
			}
		}

		public void updateUI()
		{
			// this action is just for testing - no need for i18n
			String desc =
					"Deliberately corrupt usages in the active logic design to test on the fly scrubbing and call-home";
			putValue(NAME, "Corrupt Usages");
			putValue(SHORT_DESCRIPTION, desc);
			putValue(SHORT_DESCRIPTION,
					"Deliberately corrupt usages in the active logic design to test on the fly scrubbing and call-home");
		}
	}

	protected void initGraphicsToolbar(CapletResourceBuilder rb, ActionContainer addGraphicsToolBar)
	{
		super.initGraphicsToolbar(rb, addGraphicsToolBar);
		rb.addActionUIEntry(SymbolPlaceAsGraphicsActionUI.class, addGraphicsToolBar);
	}

	protected ActionContainer initStyleSubMenu(CapletResourceBuilder rb)
	{
		ActionContainer styleMenu = new ActionContainer(new StyleFlyOutActionUI(caplet));
		rb.addActionUI(new ApplyStyleOnDiagramObjectActionUI(caplet), styleMenu);
		rb.addActionUI(new ApplyStyleToAllActionUI(caplet), styleMenu);
		rb.addActionUI(new ApplyStyleToSegmentsActionUI(caplet), styleMenu);
		rb.addActionUI(new EditStyleActionUI(caplet), styleMenu);
		rb.addActionUI(new SelectAllLockedDecorationActionUI(caplet), styleMenu);
		rb.addActionUI(new ExcludeFromApplyStyleActionUI(caplet), styleMenu);
		rb.addActionUI(new FixPositionActionUI(caplet), styleMenu);
		rb.addActionUI(new ForceApplyStyleActionUI(caplet), styleMenu);
		rb.addActionUI(new WriteToStyleActionUI(caplet), styleMenu);
		rb.addActionUI(new ResetOrderActionUI(caplet), styleMenu);
		rb.addActionUI(new SplitTableActionUI(caplet), styleMenu);
		return styleMenu;
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.CapitalCapture,
					Application.CapitalArchitect, Application.SvcDoc, Application.ArtisanFunction,
					Application.SEElectricalDesign})
	@ImmersedAction(actionId = "CAPITAL_SELECT_BY_ATTRIBUTE_PROPERTY_ACTION",
			label = "Select by Attribute/Property",
			tooltip = "Select by Attribute/Property",
			icon = "ico_select_by_property",
			buttonStyle = "SMALL_IMAGE_AND_TEXT")
	protected static class LogicSelectByPropertyAction extends SelectByPropertyAction
	{

		public LogicSelectByPropertyAction(ICaplet caplet)
		{
			super(caplet.getFIB());
		}

		public Iterator<ICapletView> getViews()
		{
			return ViewHelper.getAllActiveDesignViews();
		}
	}

	protected ActionContainer createSharedMenu(Class<?> cls)
	{
		return new SharedMenu(cls);
	}

	@Override protected void initNamingToolsMenus(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new LogicGenerateNamesActionUI(caplet), menu);
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.SvcDoc,
					Application.ArtisanFunction})
	protected static class SharedMenu extends ActionContainer
	{

		public SharedMenu(Class<?> cls)
		{
			super("Shared", true, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"), cls);
		}

		private SharedMenu(String name, boolean subContainer, Icon icon, Integer iMnemonic, String sLongDesc)
		{
			super(name, subContainer, icon, iMnemonic, sLongDesc);
		}

		protected ActionContainer createActionContainer()
		{
			String name = (String) getValue(NAME);
			Integer mnemonic = (Integer) getValue(MNEMONIC_KEY);
			boolean subContainer = isSubcontainer();
			Icon icon = (Icon) getValue(SMALL_ICON);
			String desc = (String) getValue(LONG_DESCRIPTION);

			return new SharedMenu(name, subContainer, icon, mnemonic, desc);
		}
	}

	private static class Delay30sOnSaveDEVAction extends DeveloperExtensionAppAction
	{

		public Delay30sOnSaveDEVAction()
		{
			super("30s delay on save");
		}

		public void actionPerformed(ActionEvent e)
		{
			ActionCheckBox acb = (ActionCheckBox) e.getSource();
			//noinspection AssignmentToStaticFieldFromInstanceMethod,MagicNumber
			DesignPersistenceUtils.ARTIFICIAL_DELAY_ON_SAVE = acb.getState() ? 30000L : 0L;
		}
	}

	@ImmersedAction(actionId = "CAPITAL_SELECT_BY_NAME_ACTION",
			label = "Select by Name",
			tooltip = "Select  by Name",
			icon = "ico_select_by_name",
			buttonStyle = "SMALL_IMAGE_AND_TEXT")
	protected static class SelectByNameActionImpl extends SelectByNameAction
	{

		public SelectByNameActionImpl(IFIB fib)
		{
			super(fib);
		}

		public Iterator<ICapletView> getViews()
		{
			return ViewHelper.getAllActiveDesignViews();
		}
	}

	private static class OnTheFlyScrubbingDEVAction extends DeveloperExtensionAppAction
	{

		public OnTheFlyScrubbingDEVAction()
		{
			super("On The Fly Scrubbing");
		}

		public void actionPerformed(ActionEvent e)
		{
			ActionCheckBox acb = (ActionCheckBox) e.getSource();
			OnTheFlyScrubber.setRunFlyScrubbing(acb.getState());
		}
	}
}
