/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic;

import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.AppAction;
import chs.caf.cafmain.BaseResource;
import chs.caf.cafmain.MainResources;
import chs.caf.cafmain.actions.DesignInspectorAction;
import chs.caf.cafmain.actions.DumpOriginActionUI;
import chs.caf.cafmain.actions.EditLeaderLineJustificationActionUI;
import chs.caf.cafmain.actions.ExportAsSymbolAction;
import chs.caf.cafmain.actions.MoveToAction;
import chs.caf.cafmain.actions.MoveToGridAction;
import chs.caf.cafmain.actions.ReplaceInstanceSymbolActionUI;
import chs.caf.cafmain.actions.ToggleOptionDescriptionActionUI;
import chs.caf.cafmain.actions.UpdateBorderActionUI;
import chs.caf.cafmain.actions.UpdateCompositeTextActionUI;
import chs.caf.cafmain.actions.UpdateInstanceActionUI;
import chs.caf.cafmain.actions.ViewsCAFUtils;
import chs.caf.cafmain.actions.bridges.BridgeCAFUtils;
import chs.caf.cafmain.actions.ela.ELADataManagerAction;
import chs.caf.cafmain.actions.ela.ELANodeAction;
import chs.caf.cafmain.actions.link.AddLinkActionUI;
import chs.caf.cafmain.actions.qa.AddPropertiesToObjectsAction;
import chs.caf.cafmain.actions.servicedoc.ExportServiceDocumentationActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.helpers.ConvertToRingTerminalActionUI;
import chs.caf.caplet.helpers.PropertiesActionUI;
import chs.caf.caplet.helpers.PurgeFunctionalModuleCodeActionUI;
import chs.caf.caplet.helpers.SmartEditActionUI;
import chs.caf.caplet.helpers.SmartEditPropertiesActionUI;
import chs.caf.caplet.helpers.SymbolInvokeFromDesignToolsActionUI;
import chs.caf.caplet.helpers.automation.DumpSelectedObjectDetailsActionUI;
import chs.caf.caplet.helpers.debug.SelectByUIDAction;
import chs.caf.caplet.helpers.graphics.MeasureDistanceActionUI;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsActionUI;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caf.plugin.CustomActionMenuMgr;
import chs.caf.plugin.CustomActionMenuType;
import chs.caplets.capture.actions.ddt.AssignDDTTypesActionUI;
import chs.caplets.capture.actions.ddt.EditDDTTypesActionUI;
import chs.caplets.logic.actions.Add3dDeviceAction;
import chs.caplets.logic.actions.AddChainActionUI;
import chs.caplets.logic.actions.AddCommentSymbolToMCActionUI;
import chs.caplets.logic.actions.AddConductorNameActionUI;
import chs.caplets.logic.actions.AddIndicatorsActionUI;
import chs.caplets.logic.actions.AddInterconnectOverbraidActionUI;
import chs.caplets.logic.actions.AddInterconnectShieldActionUI;
import chs.caplets.logic.actions.AddInterconnectWireActionUI;
import chs.caplets.logic.actions.AddLibraryInnercoreNetActionUI;
import chs.caplets.logic.actions.AddLibraryInnercoreShieldActionUI;
import chs.caplets.logic.actions.AddLibraryInnercoreWireActionUI;
import chs.caplets.logic.actions.AutoRouteActionUI;
import chs.caplets.logic.actions.CheckSchematicConductorConnection;
import chs.caplets.logic.actions.CompareDiagramsAction;
import chs.caplets.logic.actions.ConnectActionUI;
import chs.caplets.logic.actions.ConnectByInterconnectActionUI;
import chs.caplets.logic.actions.ConnectByNetActionUI;
import chs.caplets.logic.actions.ConnectByPinActionUI;
import chs.caplets.logic.actions.ConnectByWireActionUI;
import chs.caplets.logic.actions.ConvertPinTypeActionUI;
import chs.caplets.logic.actions.ConvertSymbolToParamActionUI;
import chs.caplets.logic.actions.CreateAssemblyActionUI;
import chs.caplets.logic.actions.CreateBlockDeviceActionUI;
import chs.caplets.logic.actions.CreateConductorActionUI;
import chs.caplets.logic.actions.CreateGeneralHighwayActionUI;
import chs.caplets.logic.actions.CreateInlineInterconnectConnectorActionUI;
import chs.caplets.logic.actions.CreateInterconnectActionUI;
import chs.caplets.logic.actions.CreateInterconnectConnectorActionUI;
import chs.caplets.logic.actions.CreateInterconnectDeviceActionUI;
import chs.caplets.logic.actions.CreateMulticoreActionUI;
import chs.caplets.logic.actions.CreateNoPinDeviceActionUI;
import chs.caplets.logic.actions.CreateNoPinInlineConnectorActionUI;
import chs.caplets.logic.actions.CreateNoPinJackConnectorActionUI;
import chs.caplets.logic.actions.CreateNoPinPlugConnectorActionUI;
import chs.caplets.logic.actions.CreateOverbraidActionUI;
import chs.caplets.logic.actions.CreateRingTerminalActionUI;
import chs.caplets.logic.actions.CreateSectorActionUI;
import chs.caplets.logic.actions.CreateShieldConductorActionUI;
import chs.caplets.logic.actions.CreateSingleLineActionUI;
import chs.caplets.logic.actions.CreateSpliceActionUI;
import chs.caplets.logic.actions.CreateWireActionUI;
import chs.caplets.logic.actions.DeleteActionUI;
import chs.caplets.logic.actions.DisconnectActionUI;
import chs.caplets.logic.actions.EditHarnessActionUI;
import chs.caplets.logic.actions.EditSharedOverbraidActionUI;
import chs.caplets.logic.actions.EditStackPinActionUI;
import chs.caplets.logic.actions.ExpandSelectionAction;
import chs.caplets.logic.actions.FluidConnectByPinActionUI;
import chs.caplets.logic.actions.FluidMovePinActionUI;
import chs.caplets.logic.actions.FluidSetPinNonReferenceActionUI;
import chs.caplets.logic.actions.FluidSetPinReferenceActionUI;
import chs.caplets.logic.actions.GenerateHarnessConnActionUI;
import chs.caplets.logic.actions.GenerateWiringDiagramInteractiveAction;
import chs.caplets.logic.actions.ICDImportTaskSubmitAction;
import chs.caplets.logic.actions.MergeIntoActionUI;
import chs.caplets.logic.actions.MoveConnectorActionUI;
import chs.caplets.logic.actions.MovePinActionUI;
import chs.caplets.logic.actions.MoveWireEndActionUI;
import chs.caplets.logic.actions.OptionFilterSettingsActionUI;
import chs.caplets.logic.actions.PlaceAssemblyTreeActionUI;
import chs.caplets.logic.actions.PropagateAllHarnessActionUI;
import chs.caplets.logic.actions.PropagateSelectedHarnessActionUI;
import chs.caplets.logic.actions.RemoveDeviceConnectorsActionUI;
import chs.caplets.logic.actions.RemoveToDoItemActionUI;
import chs.caplets.logic.actions.RerouteSegmentActionUI;
import chs.caplets.logic.actions.ResetAssemblyActionUI;
import chs.caplets.logic.actions.ResizeActionUI;
import chs.caplets.logic.actions.RouteIntoHighwayActionUI;
import chs.caplets.logic.actions.SaveAssemblyConnectivityToLibraryActionUI;
import chs.caplets.logic.actions.SetPinNonReferenceActionUI;
import chs.caplets.logic.actions.SetPinReferenceActionUI;
import chs.caplets.logic.actions.StripAtSpliceActionUI;
import chs.caplets.logic.actions.SymbolCreateSharedActionUI;
import chs.caplets.logic.actions.SymbolCreateSharedSpliceActionUI;
import chs.caplets.logic.actions.TabularEditActionUI;
import chs.caplets.logic.actions.TerminateAtSpliceActionUI;
import chs.caplets.logic.actions.ToggleHomeActionUI;
import chs.caplets.logic.actions.ToggleShowXRefActionUI;
import chs.caplets.logic.actions.UnRouteHighwayActionUI;
import chs.caplets.logic.actions.UpdateICDActionUI;
import chs.caplets.logic.actions.UpdatePartActionUI;
import chs.caplets.logic.actions.analysis.DynSimBackgroundActionUI;
import chs.caplets.logic.actions.analysis.DynSimOffActionUI;
import chs.caplets.logic.actions.analysis.DynSimOnDemandActionUI;
import chs.caplets.logic.actions.analysis.EnableRobustNetlistingAction;
import chs.caplets.logic.actions.analysis.MacroRecordingActionUI;
import chs.caplets.logic.actions.analysis.QualitativeSimulationModeActionUI;
import chs.caplets.logic.actions.analysis.SpiceSimulationModeActionUI;
import chs.caplets.logic.actions.analysis.ViewAnalysisConsoleAction;
import chs.caplets.logic.actions.analysis.ViewAnalysisOutputTabAction;
import chs.caplets.logic.actions.debug.BuildICDFromJsonActionUI;
import chs.caplets.logic.actions.debug.DumpICDDeviceActionUI;
import chs.caplets.logic.actions.debug.DumpICDSignalsActionUI;
import chs.caplets.logic.actions.debug.DumpSelectedActionUI;
import chs.caplets.logic.actions.debug.DumpSelectedSharedPinMatingActionUI;
import chs.caplets.logic.actions.debug.VariantICDTogglePinConstraintActionUI;
import chs.caplets.logic.actions.icdbrowser.AddDeviceFromICDActionUI;
import chs.caplets.logic.actions.icdbrowser.AddParametrizedDeviceFromICDActionUI;
import chs.caplets.logic.actions.prototype.LogicPrototypeWireExpressionEditAction;
import chs.caplets.logic.actions.rules.LogicSetAttributesAndPropertiesByRuleActionUI;
import chs.caplets.logic.actions.serviceDocumentation.smartflows.ChangeFlowDirectionActionUI;
import chs.caplets.logic.actions.shared.AddSharedGeneralHighwayActionUI;
import chs.caplets.logic.actions.shared.AddSharedNetActionUI;
import chs.caplets.logic.actions.shared.AddSharedShieldActionUI;
import chs.caplets.logic.actions.shared.AddSharedSingleLineActionUI;
import chs.caplets.logic.actions.shared.AddSharedWireActionUI;
import chs.caplets.logic.actions.shared.CreateSharedConductorGroupActionUI;
import chs.caplets.logic.actions.shared.CreateSharedObjectRevisionActionUI;
import chs.caplets.logic.actions.shared.EditSharedPinListActionUI;
import chs.caplets.logic.actions.shared.EnhancedSwapOutSharedObjectRevisionActionUI;
import chs.caplets.logic.actions.shared.FreezeSharedObjectsActionUI;
import chs.caplets.logic.actions.shared.FreezeUnfreezeSharedObjectActionUI;
import chs.caplets.logic.actions.shared.LogicSharedObjectDeleteActionUI;
import chs.caplets.logic.actions.shared.LogicSharedObjectDeleteUnusedActionUI;
import chs.caplets.logic.actions.shared.ReplaceSharedCompositeSymbolActionUI;
import chs.caplets.logic.actions.shared.ShareActionUI;
import chs.caplets.logic.actions.shared.SharePortActionUI;
import chs.caplets.logic.actions.shared.SharedObjectRevisionUsagesActionUI;
import chs.caplets.logic.actions.shared.SwapOutSharedObjectRevisionActionUI;
import chs.caplets.logic.actions.shared.UnshareActionUI;
import chs.caplets.shared.BaseLogicResource;
import chs.caplets.shared.ViewHelper;
import chs.caplets.shared.actions.AddToStackPinActionUI;
import chs.caplets.shared.actions.CreateStackPinActionUI;
import chs.ctf.ui.form.shareddeletion.DeleteSharedObjectCmd;
import chs.images.CHSImageLoader;
import chs.subsystem.immersedapp.ImmersedAppServices;
import chs.subsystem.immersedapp.action.IImmersedActionsRegistry;
import chs.utilities.AppInfo;
import chs.utilities.BuildInfo;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utility.AnalysisHelper;
import chs.utility.ICDUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Iterator;

/**
 * Resource initialization for CLogic.
 */
public class LogicResource extends BaseLogicResource implements ISharedObjectToolbarProvider
{

	private ActionContainer sharedObjectToolbar = null;
	private ActionContainer analysisToolbar = null;

	public LogicResource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		super.initActions();

		new AddLibraryInnercoreWireActionUI(caplet);
		new AddLibraryInnercoreNetActionUI(caplet);
		new AddLibraryInnercoreShieldActionUI(caplet);
		new AddSharedNetActionUI(caplet);
		new AddSharedWireActionUI(caplet);
		new AddSharedShieldActionUI(caplet);
		new AddSharedGeneralHighwayActionUI(caplet);
		new AddSharedSingleLineActionUI(caplet);
		new LogicSharedObjectDeleteActionUI(caplet);
		new LogicSharedObjectDeleteUnusedActionUI(caplet);
		new FreezeUnfreezeSharedObjectActionUI(caplet);
		new SharePortActionUI(caplet);
		new FreezeSharedObjectsActionUI(caplet);
		new EditSharedPinListActionUI(caplet);
		new AddInterconnectWireActionUI(caplet);
		new AddInterconnectShieldActionUI(caplet);
		new AddInterconnectWireActionUI(caplet);
		new AddInterconnectOverbraidActionUI(caplet);
		new RemoveToDoItemActionUI(caplet);
		new SymbolCreateSharedActionUI(caplet);
		new SymbolPlaceAsGraphicsActionUI(caplet);
		new SymbolCreateSharedSpliceActionUI(caplet);
		new SymbolInvokeFromDesignToolsActionUI(caplet);
		new EditLeaderLineJustificationActionUI(caplet);
		new EnhancedSwapOutSharedObjectRevisionActionUI(caplet);
		new SharedObjectRevisionUsagesActionUI(caplet);
		new SmartEditActionUI(caplet);
		new SmartEditPropertiesActionUI(caplet);

		new AddDeviceFromICDActionUI(caplet);
		new AddParametrizedDeviceFromICDActionUI(caplet);
		new ConvertPinTypeActionUI(caplet);
		new PlaceAssemblyTreeActionUI(caplet);
		new ELANodeAction.UI(caplet);
		new LogicPrototypeWireExpressionEditAction.UI(caplet);

		IImmersedActionsRegistry registry = ImmersedAppServices.queryExtension(caplet, IImmersedActionsRegistry.class);
		if (registry != null) {
			registry.addAction(new Add3dDeviceAction(caplet.getFIB()));
		}
	}

	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initEditMenu(rb, menu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new DeleteActionUI(caplet), menu);

		ActionContainer modulecodeMenu = CapletResourceBuilder.createSubContainer("ModuleCodes", BaseResource.class);
		menu.add(modulecodeMenu);
		modulecodeMenu.add(new ActionSeparator());
		// Purge Functional Module Codes
		rb.addActionUI(new PurgeFunctionalModuleCodeActionUI(caplet), modulecodeMenu);

		menu.add(new ActionSeparator());
		Class<?> cls = BaseLogicResource.class;
		ActionContainer editMulticoreMenu = CapletResourceBuilder.createSubContainer("EditMulticoreSubmenu", cls);
		rb.addActionUI(new CreateMulticoreActionUI(caplet), editMulticoreMenu);
		rb.addActionUI(new CreateSharedConductorGroupActionUI(caplet), editMulticoreMenu);
		rb.addActionUI(new AddIndicatorsActionUI(caplet), editMulticoreMenu);
		rb.addActionUI(new AddCommentSymbolToMCActionUI(caplet), editMulticoreMenu);
		menu.add(editMulticoreMenu);

		ActionContainer editOverbraidMenu = CapletResourceBuilder.createSubContainer("EditOverbraidSubmenu", cls);
		String menuName =
				ResourceMgr.getStringForMenu(cls, "Resource.EditOverbraidAction.Title.text");
		rb.addActionUI(new CreateOverbraidActionUI(caplet), editOverbraidMenu, menuName);
		rb.addActionUI(new EditSharedOverbraidActionUI(caplet), editOverbraidMenu);
		menu.add(editOverbraidMenu);

		ActionContainer editAssemblyMenu = CapletResourceBuilder.createSubContainer("EditAssemblySubmenu", cls);
		rb.addActionUI(new CreateAssemblyActionUI(caplet), editAssemblyMenu);

		if (AppInfo.isCapitalLogic()) {
			CustomActionMenuMgr.getInstance()
					.populateActionsForMainMenuType(caplet, editAssemblyMenu, CustomActionMenuType.LOGIC_ASSEMBLY);
		}
		menu.add(editAssemblyMenu);

		menu.add(new ActionSeparator());
		AppAction selectByNameAction = new SelectByNameActionImpl(caplet.getFIB());
//				new SelectByNameAction(caplet.getFIB())
//		{
//			public Iterator<ICapletView> getViews()
//			{
//				return ViewHelper.getAllActiveDesignViews();
//			}
//		};
		rb.addAppAction(selectByNameAction, menu);

		AppAction selectByPropertyAction = new LogicSelectByPropertyAction(caplet);
		selectByPropertyAction.putValue(Action.MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(cls, "Resource.Edit.SelectByAttrProp.menu.mnmonic"));
		rb.addAppAction(selectByPropertyAction, menu);
		rb.addAppAction(new ExpandSelectionAction(caplet.getFIB()), menu);

		// styling actions
		menu.add(new ActionSeparator());
		ActionContainer styleMenu = initStyleSubMenu(rb);
		// When this is merged with main resource, maintain the relative position of this
		styleMenu.setPositionRelatively(true);
		menu.add(styleMenu);

		// properties action
		menu.add(new ActionSeparator());
		rb.addActionUI(new PropertiesActionUI(caplet), menu);
		rb.addActionUI(new TabularEditActionUI(caplet), menu);
	}

	protected void initFileMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initFileMenu(rb, menu);

		BridgeCAFUtils.createImportExportCapletMenuItems(caplet, menu, true);
	}

	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
		menu.add(new ActionCheckBox(new ToggleOptionDescriptionActionUI(caplet)));
		rb.addAppAction(new DesignInspectorAction(caplet.getFIB()), menu);
		rb.addActionUI(new OptionFilterSettingsActionUI(caplet), menu);
	}

//	protected void initAddMenu(CapletResourceBuilder rb, ActionContainer menu)
//	{
//		super.initAddMenu(rb, menu);
//		// nothing happens in the base class
//
//		// these names are repeated on several menus...
//		Class<?> cls = BaseLogicResource.class;
//		String deviceMenuName = ResourceMgr.getString(cls, "Resource.Device.menu.name");
//		String plugMenuName = ResourceMgr.getString(cls, "Resource.Plug.menu.name");
//		String inlineMenuName = ResourceMgr.getString(cls, "Resource.Inline.menu.name");
//		String jackMenuName = ResourceMgr.getString(cls, "Resource.Jack.menu.name");
//		String multicoreMenuName = ResourceMgr.getStringForMenu(cls, "Resource.Multicore.menu.name");
//		String overbraidMenuName = ResourceMgr.getStringForMenu(cls, "Resource.Overbraid.menu.name");
//		String spliceMenuName = ResourceMgr.getString(cls, "Resource.Splice.menu.name");
//
//		rb.addActionUI(new CreateNoPinDeviceActionUI(caplet), menu, deviceMenuName);
//		rb.addActionUI(new AddPinActionUI(caplet), menu);
//		rb.addActionUI(new AddInstanceActionUI(caplet), menu);
//		rb.addActionUI(new CreateNoPinPlugConnectorActionUI(caplet), menu, plugMenuName);
//		rb.addActionUI(new CreateNoPinInlineConnectorActionUI(caplet), menu, inlineMenuName);
//		rb.addActionUI(new CreateNoPinJackConnectorActionUI(caplet), menu, jackMenuName);
//		rb.addActionUI(new CreateConductorActionUI(caplet), menu);
//		rb.addActionUI(new CreateWireActionUI(caplet), menu);
//		rb.addActionUI(new CreateShieldConductorActionUI(caplet), menu);
//		rb.addActionUI(new AddChainActionUI(caplet), menu);
//		// need a different mnemonic here because the same action is (deliberately) available in 2 different menus
//		// Edit > Multicores > Generic and Add > Multicore
//		// seems funny to me but marketing are sure ...
//		ActionUI actionUI = new CreateMulticoreActionUI(caplet);
//		actionUI.putValue(Action.MNEMONIC_KEY,
//				(int) ResourceMgr.getMnemonic(cls, "Resource.Add.Multicore.menu.mnemonic"));
//		rb.addActionUI(actionUI, menu, multicoreMenuName);
//		rb.addActionUI(new CreateOverbraidActionUI(caplet), menu);
//
//		rb.addActionUI(new CreateSpliceActionUI(caplet), menu);
//		rb.addActionUI(new AddBackshellActionUI(caplet), menu);
//
//		// Add > Shared Submenu
//		menu.add(new ActionSeparator());
//		ActionContainer sharedMenu = CapletResourceBuilder.createSubContainer("Shared", cls);
//		rb.addActionUI(new AddSharedDeviceActionUI(caplet), sharedMenu, deviceMenuName);
//		rb.addActionUI(new AddSharedInlineConnectorActionUI(caplet), sharedMenu, inlineMenuName);
//		rb.addActionUI(new AddSharedPlugConnectorActionUI(caplet), sharedMenu, plugMenuName);
//		rb.addActionUI(new AddSharedJackConnectorActionUI(caplet), sharedMenu, jackMenuName);
//		// need a different mnemonic here because the same action is (deliberately) available in 2 different menus
//		actionUI = new CreateSharedConductorGroupActionUI(caplet);
//		actionUI.putValue(Action.MNEMONIC_KEY,
//				(int) ResourceMgr.getMnemonic(cls, "Resource.Add.Shared.Multicore.menu.mnemonic"));
//		rb.addActionUI(actionUI, sharedMenu, multicoreMenuName);
//		actionUI = new EditSharedOverbraidActionUI(caplet);
//		actionUI.putValue(Action.MNEMONIC_KEY,
//				(int) ResourceMgr.getMnemonic(cls, "Resource.Add.Shared.Overbraid.menu.mnemonic"));
//		rb.addActionUI(actionUI, sharedMenu, overbraidMenuName);
//		rb.addActionUI(new AddSharedSpliceActionUI(caplet), sharedMenu, spliceMenuName);
//		menu.add(sharedMenu);
//
//		// Add > From Library Submenu
//		ActionContainer fromLibraryMenu = CapletResourceBuilder.createSubContainer("FromLibrary", cls);
//		rb.addActionUI(new AddDeviceFromLibraryPartActionUI(caplet), fromLibraryMenu, deviceMenuName);
//		rb.addActionUI(new AddLibraryMulticoreActionUI(caplet), fromLibraryMenu, multicoreMenuName);
//		rb.addActionUI(new AddSpliceFromLibraryPartActionUI(caplet), fromLibraryMenu, spliceMenuName);
//		menu.add(fromLibraryMenu);
//
//		// Add > From Interconnect Submenu
//		ActionContainer interconnectMenu =
//				CapletResourceBuilder.createSubContainer("Interconnect", cls);
//		rb.addActionUI(new CreateInterconnectDeviceActionUI(caplet), interconnectMenu);
//		rb.addActionUI(new CreateInterconnectConnectorActionUI(caplet), interconnectMenu);
//		rb.addActionUI(new CreateInlineInterconnectConnectorActionUI(caplet), interconnectMenu);
//		rb.addActionUI(new CreateInterconnectActionUI(caplet), interconnectMenu);
//
//		interconnectMenu.add(new ActionSeparator());
//		ActionContainer sharedInterconnectMenu = CapletResourceBuilder.createSubContainer("Shared", cls);
//		rb.addActionUI(new AddSharedInterconnectDeviceActionUI(caplet), sharedInterconnectMenu);
//		rb.addActionUI(new AddSharedInterconnectConnectorActionUI(caplet), sharedInterconnectMenu);
//		interconnectMenu.add(sharedInterconnectMenu);
//		menu.add(interconnectMenu);
//	}

	@Override protected void initLayoutMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new ResizeActionUI(caplet), menu);
		super.initLayoutMenu(rb, menu);
	}

	protected void initActionsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initActionsMenu(rb, menu);

		rb.addActionUI(new MergeIntoActionUI(caplet), menu);

		rb.addActionUI(new ConvertSymbolToParamActionUI(caplet), menu);

		rb.addActionUI(new ShareActionUI(caplet), menu);
		rb.addActionUI(new UnshareActionUI(caplet), menu);
		rb.addActionUI(new ExportServiceDocumentationActionUI(caplet), menu);

		rb.addAppAction(new MoveToAction(caplet.getFIB()), menu);
		rb.addActionUI(new GenerateHarnessConnActionUI(caplet), menu);
		rb.addActionUI(new ConvertToRingTerminalActionUI(caplet), menu);
		rb.addActionUI(new PropagateSelectedHarnessActionUI(caplet), menu);
		rb.addActionUI(new PropagateAllHarnessActionUI(caplet), menu);
		Class<?> cls = BaseLogicResource.class;
		String menuName = ResourceMgr.getString(cls, "Resource.SetHarnessAttribute.menu.name");
		rb.addActionUI(new EditHarnessActionUI(caplet), menu, menuName);
		rb.addActionUI(new ELANodeAction.UI(caplet), menu);
		rb.addActionUI(new ELADataManagerAction.UI(caplet), menu);
		rb.addActionUI(new LogicSetAttributesAndPropertiesByRuleActionUI(caplet), menu);

		rb.addActionUI(new MoveConnectorActionUI(caplet), menu);
		// Actions > Pin > Sub menu
		menu.add(new ActionSeparator());
		ActionContainer pinMenu = CapletResourceBuilder.createSubContainer("Pin", cls);
		rb.addActionUI(new SetPinReferenceActionUI(caplet), pinMenu);
		rb.addActionUI(new FluidSetPinReferenceActionUI(caplet), pinMenu);
		rb.addActionUI(new SetPinNonReferenceActionUI(caplet), pinMenu);
		rb.addActionUI(new FluidSetPinNonReferenceActionUI(caplet), pinMenu);
		menuName = ResourceMgr.getString(cls, "Resource.MovePins.menu.name");
		rb.addActionUI(new MovePinActionUI(caplet), pinMenu, menuName);
		rb.addActionUI(new FluidMovePinActionUI(caplet), pinMenu);
		rb.addActionUI(new CreateStackPinActionUI(caplet), pinMenu);
		rb.addActionUI(new AddToStackPinActionUI(caplet), pinMenu);
		rb.addActionUI(new EditStackPinActionUI(caplet), pinMenu);
		menu.add(pinMenu);

		// Actions > Symbol > Sub menu
		ActionContainer symbolMenu = CapletResourceBuilder.createSubContainer("Symbol", cls);
		menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Update.menu.name");
		rb.addActionUI(new UpdateInstanceActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Replace.menu.name");
		rb.addActionUI(new ReplaceInstanceSymbolActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.ReplaceSharedComposite.menu.name");
		rb.addActionUI(new ReplaceSharedCompositeSymbolActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getStringForMenu(cls, "Resource.ExportAsSymbol.menu.name");
		rb.addActionUI(new ExportAsSymbolAction.UI(caplet), symbolMenu, menuName);
		menu.add(symbolMenu);

		ActionContainer libraryPartMenu = CapletResourceBuilder.createSubContainer("LibraryPart", cls);
		rb.addActionUI(new UpdatePartActionUI(caplet), libraryPartMenu);
		rb.addActionUI(new SaveAssemblyConnectivityToLibraryActionUI(caplet), libraryPartMenu);
		menu.add(libraryPartMenu);

		// Actions > Shared > Sub menu
		ActionContainer sharedMenu = createSharedMenu(cls);
		rb.addActionUI(new ToggleHomeActionUI(caplet, ToggleHomeActionUI.MARK_HOME), sharedMenu);
		rb.addActionUI(new ToggleHomeActionUI(caplet, ToggleHomeActionUI.REMOVE_HOME), sharedMenu);
		rb.addActionUI(new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.SHOW_XREF), sharedMenu);
		rb.addActionUI(new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.HIDE_XREF), sharedMenu);
		sharedMenu.add(new ActionSeparator());
		rb.addActionUI(new SwapOutSharedObjectRevisionActionUI(caplet), sharedMenu);
		rb.addActionUI(new CreateSharedObjectRevisionActionUI(caplet), sharedMenu);
		menu.add(sharedMenu);

		// Actions > Splice > Sub menu
		ActionContainer spliceMenu = CapletResourceBuilder.createSubContainer("Splice", cls);
		spliceMenu
				.putValue(Action.MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(cls, "Resource.Actions.Splice.mnemonic"));
		rb.addActionUI(new StripAtSpliceActionUI(caplet), spliceMenu);
		rb.addActionUI(new TerminateAtSpliceActionUI(caplet), spliceMenu);
		menu.add(spliceMenu);

		// Actions > Conductor > Sub menu
		ActionContainer conductorActionMenu = CapletResourceBuilder.createSubContainer("Conductor", cls);
		rb.addActionUI(new AutoRouteActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new RouteIntoHighwayActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new UnRouteHighwayActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new RerouteSegmentActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new AddConductorNameActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new MoveWireEndActionUI(caplet), conductorActionMenu);
		menu.add(conductorActionMenu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new DisconnectActionUI(caplet), menu);

		// Actions > Connect > Sub menu
		ActionContainer connectActionMenu = new ActionContainer(new ConnectActionUI(caplet));
		rb.addActionUI(new ConnectByWireActionUI(caplet), connectActionMenu);
		rb.addActionUI(new ConnectByNetActionUI(caplet), connectActionMenu);
		if (Environment.isInterconnectFlowAllowed()) {
			rb.addActionUI(new ConnectByInterconnectActionUI(caplet), connectActionMenu);
		}
		rb.addActionUI(new ConnectByPinActionUI(caplet), connectActionMenu);
		rb.addActionUI(new FluidConnectByPinActionUI(caplet), connectActionMenu);
		menu.add(connectActionMenu);

		menu.add(new ActionSeparator());
		ActionContainer updateMenu = CapletResourceBuilder.createSubContainer("Actions.Update", MainResources.class);
		rb.addActionUI(new UpdateBorderActionUI(caplet), updateMenu);
		rb.addActionUI(new UpdateCompositeTextActionUI(caplet), updateMenu);
		// dts0100802895 When this is merged with main resource, maintain the relative position of this
		updateMenu.setPositionRelatively(true);
		menu.add(updateMenu);

		//rb.addActionUI(new ReloadIndicatorActionUI(caplet), menu); - to fix dts0100717716
		if (Environment.isVeSysMigrationPathAllowed()) {
			rb.addActionUI(new MoveToGridAction.UI(caplet), menu);
		}

		rb.addActionUI(new ResetAssemblyActionUI(caplet), menu);
		if (ICDUtils.areICDsSupported()) {
			rb.addActionUI(new UpdateICDActionUI(caplet), menu);
		}
		rb.addActionUI(new RemoveDeviceConnectorsActionUI(caplet), menu);
		rb.addActionUI(new ChangeFlowDirectionActionUI(caplet), menu);
		if (AppInfo.isCapitalLogic()) {
			CustomActionMenuMgr.getInstance()
					.populateActionsForMainMenuType(caplet, menu, CustomActionMenuType.SMART_FLOW_CONTEXT);
		}
	}

	protected void initToolsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initToolsMenu(rb, menu);

        ViewsCAFUtils.initAutoViewMenu(caplet.getFIB(), rb, menu);

        menu.add(new ActionSeparator());
        if (Environment.isInterconnectFlowAllowed()) {
            Class<?> cls = BaseLogicResource.class;
            ActionContainer interconnectDesignMenu =
                    CapletResourceBuilder.createSubContainer("InterconnectDesign", cls);
            rb.addAppAction(new GenerateWiringDiagramInteractiveAction(caplet.getFIB()), interconnectDesignMenu);
            menu.add(interconnectDesignMenu);
        }

		ActionContainer namingMenu = CapletResourceBuilder.createSubContainer("Naming", BaseResource.class);
		initNamingToolsMenus(rb, namingMenu);
		menu.add(namingMenu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new AssignDDTTypesActionUI(caplet), menu);
		rb.addActionUI(new EditDDTTypesActionUI(caplet), menu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new MeasureDistanceActionUI(caplet), menu);

		// some QA extensions that have moved from elsewhere...
		menu.add(new ActionSeparator());
		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
				BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			menu.add(new ActionEntry(new CompareDiagramsAction(caplet.getFIB())));
			menu.add(new ActionEntry(new CheckSchematicConductorConnection(caplet.getFIB())));
		}

		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
				BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			menu.add(new ActionEntry(new ICDImportTaskSubmitAction(caplet.getFIB())));
		}

		rb.addAppAction(new SelectByUIDAction(getCaplet().getFIB())
		{
			@Override public Iterator<ICapletView> getViews()
			{
				return ViewHelper.getAllActiveDesignViews();
			}
		}, menu);
		rb.addActionUI(new DumpSelectedActionUI(caplet), menu);
		rb.addActionUI(new DumpSelectedObjectDetailsActionUI(caplet), menu);
		rb.addActionUI(new DumpOriginActionUI(caplet), menu);
		rb.addActionUI(new AddLinkActionUI(caplet), menu);
		rb.addActionUI(new DumpSelectedSharedPinMatingActionUI(caplet), menu);
		rb.addAppAction(new DumpICDDeviceActionUI(caplet), menu);
		rb.addAppAction(new DumpICDSignalsActionUI(caplet), menu);
		rb.addAppAction(new BuildICDFromJsonActionUI(caplet), menu);
		rb.addAppAction(new VariantICDTogglePinConstraintActionUI(caplet), menu);
		rb.addAppAction(new AddPropertiesToObjectsAction.UI(caplet), menu);
	}

    protected void initToolbars(CapletResourceBuilder rb) {
        super.initToolbars(rb);
        sharedObjectToolbar = new ActionContainer("LogicSharedObject");
        initSharedObjectBrowserLogicToolbar(rb, sharedObjectToolbar);
    }

	protected void initLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(CreateNoPinDeviceActionUI.class, toolbar);
		rb.addActionUIEntry(CreateNoPinPlugConnectorActionUI.class, toolbar);
		rb.addActionUIEntry(CreateNoPinInlineConnectorActionUI.class, toolbar);
		rb.addActionUIEntry(CreateNoPinJackConnectorActionUI.class, toolbar);
		rb.addActionUIEntry(CreateRingTerminalActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSpliceActionUI.class, toolbar);

		rb.addActionUIEntry(CreateConductorActionUI.class, toolbar);
		rb.addActionUIEntry(CreateWireActionUI.class, toolbar);
		rb.addActionUIEntry(CreateGeneralHighwayActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSingleLineActionUI.class, toolbar);
		rb.addActionUIEntry(CreateShieldConductorActionUI.class, toolbar);
		rb.addActionUIEntry(AddChainActionUI.class, toolbar);
		rb.addActionUIEntry(CreateMulticoreActionUI.class, toolbar);
		rb.addActionUIEntry(CreateOverbraidActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSectorActionUI.class, toolbar);

		ActionContainer addInterconnectToolBar = new ActionContainer("Add Interconnect", true);
		rb.addActionUIEntry(CreateInterconnectDeviceActionUI.class, addInterconnectToolBar);
		rb.addActionUIEntry(CreateInterconnectConnectorActionUI.class, addInterconnectToolBar);
		rb.addActionUIEntry(CreateInlineInterconnectConnectorActionUI.class, addInterconnectToolBar);
		rb.addActionUIEntry(CreateInterconnectActionUI.class, addInterconnectToolBar);
		toolbar.add(addInterconnectToolBar);

		rb.addActionUIEntry(CreateAssemblyActionUI.class, toolbar);
		rb.addActionUIEntry(CreateBlockDeviceActionUI.class, toolbar);
		ActionContainer addGraphicsToolBar = new ActionContainer("Add Graphics", true);
		initGraphicsToolbar(rb, addGraphicsToolBar);
		toolbar.add(addGraphicsToolBar);
	}

	protected void initSharedObjectBrowserLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(EditSharedPinListActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSharedObjectRevisionActionUI.class, toolbar);
		rb.addActionUIEntry(FreezeSharedObjectsActionUI.class, toolbar);
		if (DeleteSharedObjectCmd.userHasDeleteUnusedSharedObjectsPermission()) {
			rb.addActionUIEntry(LogicSharedObjectDeleteUnusedActionUI.class, toolbar);
			rb.addActionUIEntry(LogicSharedObjectDeleteActionUI.class, toolbar);
		}
	}

	protected void initAnalysisBrowserToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		ActionContainer asScopeAction = new ActionContainer("ScopeAction", true);
		asScopeAction.putValue(Action.SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/as_set_scope.gif"));
		toolbar.add(asScopeAction);

		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			ActionContainer simType = new ActionContainer("Simulation Type");
			toolbar.add(simType);

			simType.add(new ActionEntry(new QualitativeSimulationModeActionUI(caplet)));
			simType.add(new ActionEntry(new SpiceSimulationModeActionUI(caplet)));
		}

		ActionEntry robustNetlisting = new ActionEntry(
				new EnableRobustNetlistingAction(caplet.getFIB()));
		toolbar.add(robustNetlisting);

		toolbar.add(new ActionSeparator());

		toolbar.add(new ActionEntry(new DynSimBackgroundActionUI(caplet)));
		toolbar.add(new ActionEntry(new DynSimOnDemandActionUI(caplet)));
		toolbar.add(new ActionEntry(new DynSimOffActionUI(caplet)));

		toolbar.add(new ActionEntry(new MacroRecordingActionUI(caplet)));

		toolbar.add(new ActionSeparator());

		toolbar.add(new ActionEntry(new ViewAnalysisConsoleAction(caplet.getFIB())));
		toolbar.add(new ActionEntry(new ViewAnalysisOutputTabAction(caplet.getFIB())));
	}

	@NotNull public ActionContainer getSharedToolbar()
	{
		return sharedObjectToolbar;
	}

	public ActionContainer getAnalysisToolbar()
	{
		return analysisToolbar;
	}
}
