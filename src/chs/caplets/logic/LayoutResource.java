/*
 * Copyright 2002-2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.AppAction;
import chs.caf.cafmain.BaseResource;
import chs.caf.cafmain.MainResources;
import chs.caf.cafmain.actions.BrowseSelectedObjectActionUI;
import chs.caf.cafmain.actions.DesignInspectorAction;
import chs.caf.cafmain.actions.DumpOriginActionUI;
import chs.caf.cafmain.actions.EditLeaderLineJustificationActionUI;
import chs.caf.cafmain.actions.ExportAsSymbolAction;
import chs.caf.cafmain.actions.FindReplaceSelectionActionUI;
import chs.caf.cafmain.actions.MoveToAction;
import chs.caf.cafmain.actions.MoveToGridAction;
import chs.caf.cafmain.actions.ReplaceInstanceSymbolActionUI;
import chs.caf.cafmain.actions.ToggleOptionDescriptionActionUI;
import chs.caf.cafmain.actions.UpdateBorderActionUI;
import chs.caf.cafmain.actions.UpdateCompositeTextActionUI;
import chs.caf.cafmain.actions.UpdateInstanceActionUI;
import chs.caf.cafmain.actions.ViewsCAFUtils;
import chs.caf.cafmain.actions.bridges.BridgeCAFUtils;
import chs.caf.cafmain.actions.bridges.xSCBridgeImportAndUpdateAllActionUI;
import chs.caf.cafmain.actions.ela.ELADataManagerAction;
import chs.caf.cafmain.actions.ela.ELANodeAction;
import chs.caf.cafmain.actions.link.AddLinkActionUI;
import chs.caf.cafmain.actions.servicedoc.ExportServiceDocumentationActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.helpers.DimensionFlipActionUI;
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
import chs.caplets.logic.actions.AddChainActionUI;
import chs.caplets.logic.actions.AddCommentSymbolToMCActionUI;
import chs.caplets.logic.actions.AddConductorNameActionUI;
import chs.caplets.logic.actions.AddIndicatorsActionUI;
import chs.caplets.logic.actions.AddInstanceActionUI;
import chs.caplets.logic.actions.AddInterconnectOverbraidActionUI;
import chs.caplets.logic.actions.AddInterconnectShieldActionUI;
import chs.caplets.logic.actions.AddInterconnectWireActionUI;
import chs.caplets.logic.actions.AddPinListActionUI;
import chs.caplets.logic.actions.AutoRouteActionUI;
import chs.caplets.logic.actions.BatchDevicePlacementActionUI;
import chs.caplets.logic.actions.CheckSchematicConductorConnection;
import chs.caplets.logic.actions.CompareDiagramsAction;
import chs.caplets.logic.actions.ConnectIndicatorsActionUI;
import chs.caplets.logic.actions.ConvertSymbolToParamActionUI;
import chs.caplets.logic.actions.CreateDuctWithoutPartAndSymbolActionUI;
import chs.caplets.logic.actions.CreateLayoutComponentInstanceActionUI;
import chs.caplets.logic.actions.CreateModularSchematicsActionUI;
import chs.caplets.logic.actions.CreateMountWithoutPartAndSymbolActionUI;
import chs.caplets.logic.actions.CreateOtherComponentOnlyWithPartActionUI;
import chs.caplets.logic.actions.CreateOtherComponentOnlyWithSymbolActionUI;
import chs.caplets.logic.actions.CreateOtherComponentWithPartAndSymbolActionUI;
import chs.caplets.logic.actions.CreateOtherComponentWithoutPartAndSymbolActionUI;
import chs.caplets.logic.actions.CreateSectorActionUI;
import chs.caplets.logic.actions.DeleteActionUI;
import chs.caplets.logic.actions.DeleteLayoutBOMIDsActionUI;
import chs.caplets.logic.actions.ExpandSelectionAction;
import chs.caplets.logic.actions.GenerateWiringDiagramInteractiveAction;
import chs.caplets.logic.actions.MoveConnectorActionUI;
import chs.caplets.logic.actions.OptionFilterSettingsActionUI;
import chs.caplets.logic.actions.PlaceAssemblyTreeActionUI;
import chs.caplets.logic.actions.RegenerateLayoutBOMIDsActionUI;
import chs.caplets.logic.actions.RemoveDeviceConnectorsActionUI;
import chs.caplets.logic.actions.RemoveToDoItemActionUI;
import chs.caplets.logic.actions.RerouteSegmentActionUI;
import chs.caplets.logic.actions.ResetAssemblyActionUI;
import chs.caplets.logic.actions.RouteIntoHighwayActionUI;
import chs.caplets.logic.actions.SaveAssemblyConnectivityToLibraryActionUI;
import chs.caplets.logic.actions.SetPinNonReferenceActionUI;
import chs.caplets.logic.actions.SetPinReferenceActionUI;
import chs.caplets.logic.actions.StripAtSpliceActionUI;
import chs.caplets.logic.actions.TabularEditActionUI;
import chs.caplets.logic.actions.TerminateAtSpliceActionUI;
import chs.caplets.logic.actions.ToggleHomeActionUI;
import chs.caplets.logic.actions.ToggleShowXRefActionUI;
import chs.caplets.logic.actions.UnRouteHighwayActionUI;
import chs.caplets.logic.actions.UnplaceActionUI;
import chs.caplets.logic.actions.UpdateLayoutBOMIDsActionUI;
import chs.caplets.logic.actions.UpdatePartActionUI;
import chs.caplets.logic.actions.analysis.QualitativeSimulationModeActionUI;
import chs.caplets.logic.actions.analysis.ResetActionUI;
import chs.caplets.logic.actions.analysis.SimulateActionUI;
import chs.caplets.logic.actions.analysis.SpiceSimulationModeActionUI;
import chs.caplets.logic.actions.debug.BuildICDFromJsonActionUI;
import chs.caplets.logic.actions.debug.DumpICDDeviceActionUI;
import chs.caplets.logic.actions.debug.DumpICDSignalsActionUI;
import chs.caplets.logic.actions.debug.DumpSelectedActionUI;
import chs.caplets.logic.actions.debug.DumpSelectedSharedPinMatingActionUI;
import chs.caplets.logic.actions.debug.VariantICDTogglePinConstraintActionUI;
import chs.caplets.logic.actions.layout.LayoutAssociateDesignsActionUI;
import chs.caplets.logic.actions.layout.LayoutDesignResyncActionUI;
import chs.caplets.logic.actions.layout.LayoutXDimensionActionUI;
import chs.caplets.logic.actions.layout.LayoutXYDimensionActionUI;
import chs.caplets.logic.actions.layout.LayoutYDimensionActionUI;
import chs.caplets.logic.actions.serviceDocumentation.smartflows.ChangeFlowDirectionActionUI;
import chs.caplets.logic.actions.shared.AddGeneralHighwayActionUI;
import chs.caplets.logic.actions.shared.AddSingleLineActionUI;
import chs.caplets.logic.actions.shared.ReplaceSharedCompositeSymbolActionUI;
import chs.caplets.logic.actions.shared.SharedObjectRevisionUsagesActionUI;
import chs.caplets.shared.BaseLogicResource;
import chs.caplets.shared.ViewHelper;
import chs.caplets.shared.actions.SelectActionUI;
import chs.utilities.AppInfo;
import chs.utilities.BuildInfo;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utility.AnalysisHelper;
import com.mentor.chs.plugin.designinspection.IXInspectionPanel;
import com.mentor.chs.plugin.designinspection.IXLayoutDesignInspectionPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Iterator;

/**
 * Resource initialization for CLogic.
 */
public class LayoutResource extends BaseLogicResource
		implements ISharedObjectToolbarProvider, ILayoutDesignToolbarProvider
{

	private ActionContainer sharedObjectToolbar = null;
	private ActionContainer layoutDesignLeftToolbar = null;
	private ActionContainer layoutDesignRightToolbar = null;
	private ActionContainer analysisToolbar = null;

	public LayoutResource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	protected Class<? extends IXInspectionPanel> getInspectionPanelClazz()
	{
		return IXLayoutDesignInspectionPanel.class;
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		new xSCBridgeImportAndUpdateAllActionUI(caplet);
		new BrowseSelectedObjectActionUI(caplet);

		new FindReplaceSelectionActionUI(caplet);
		new SelectActionUI(caplet);
		new ConnectIndicatorsActionUI(caplet);

		new ResetActionUI(caplet);
		new SimulateActionUI(caplet);
		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			new SpiceSimulationModeActionUI(caplet);
			new QualitativeSimulationModeActionUI(caplet);
		}
		new AddGeneralHighwayActionUI(caplet);
		new AddSingleLineActionUI(caplet);
		new UnplaceActionUI(caplet);
		new AddPinListActionUI(caplet);
		new CreateModularSchematicsActionUI(caplet);

		new AddInterconnectWireActionUI(caplet);
		new AddInterconnectWireActionUI(caplet);
		new AddInterconnectShieldActionUI(caplet);
		new AddInterconnectOverbraidActionUI(caplet);
		new SymbolPlaceAsGraphicsActionUI(caplet);
		new RemoveToDoItemActionUI(caplet);
		new SymbolInvokeFromDesignToolsActionUI(caplet);
		new SharedObjectRevisionUsagesActionUI(caplet);
		new EditLeaderLineJustificationActionUI(caplet);
		new SmartEditPropertiesActionUI(caplet);
		new SmartEditActionUI(caplet);

		new PlaceAssemblyTreeActionUI(caplet);
		new CreateMountWithoutPartAndSymbolActionUI(caplet);
		new CreateOtherComponentWithPartAndSymbolActionUI(caplet);
		new CreateOtherComponentWithoutPartAndSymbolActionUI(caplet);
		new CreateDuctWithoutPartAndSymbolActionUI(caplet);
		new CreateOtherComponentOnlyWithPartActionUI(caplet);
		new CreateLayoutComponentInstanceActionUI(caplet);
		new CreateOtherComponentOnlyWithSymbolActionUI(caplet);
/*		new UpdateLayoutBOMIDsActionUI(caplet);
		new DeleteLayoutBOMIDsActionUI(caplet);
		new RegenerateLayoutBOMIDsActionUI(caplet);*/
		new ELANodeAction.UI(caplet);
	}

	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		CapletResourceBuilder resb = rb;
		ActionContainer editMenu = menu;
		super.initEditMenu(resb, editMenu);

		editMenu.add(new ActionSeparator());
		resb.addActionUI(new DeleteActionUI(caplet), editMenu);

		ActionContainer modulecodeMenu = CapletResourceBuilder.createSubContainer("ModuleCodes", BaseResource.class);
		editMenu.add(modulecodeMenu);
		modulecodeMenu.add(new ActionSeparator());
		// Purge Functional Module Codes
		resb.addActionUI(new PurgeFunctionalModuleCodeActionUI(caplet), modulecodeMenu);

		editMenu.add(new ActionSeparator());
		Class<?> cls = BaseLogicResource.class;
		ActionContainer editMulticoreMenu = CapletResourceBuilder.createSubContainer("EditMulticoreSubmenu", cls);
		resb.addActionUI(new AddIndicatorsActionUI(caplet), editMulticoreMenu);
		resb.addActionUI(new AddCommentSymbolToMCActionUI(caplet), editMulticoreMenu);
		editMenu.add(editMulticoreMenu);
		AppAction selectByNameAction = new SelectByNameActionImpl(caplet.getFIB());
		resb.addAppAction(selectByNameAction, editMenu);

		AppAction selectByPropertyAction = new LogicSelectByPropertyAction(caplet);
		selectByPropertyAction.putValue(Action.MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(cls, "Resource.Edit.SelectByAttrProp.menu.mnmonic"));
		resb.addAppAction(selectByPropertyAction, editMenu);
		resb.addAppAction(new ExpandSelectionAction(caplet.getFIB()), editMenu);

		// styling actions
		editMenu.add(new ActionSeparator());
		ActionContainer styleMenu = initStyleSubMenu(resb);
		// When this is merged with main resource, maintain the relative position of this
		styleMenu.setPositionRelatively(true);
		editMenu.add(styleMenu);

		// properties action
		editMenu.add(new ActionSeparator());
		resb.addActionUI(new PropertiesActionUI(caplet), editMenu);
		resb.addActionUI(new TabularEditActionUI(caplet), editMenu);
	}

	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
		menu.add(new ActionCheckBox(new ToggleOptionDescriptionActionUI(caplet)));
		rb.addAppAction(new DesignInspectorAction(caplet.getFIB()), menu);
		rb.addActionUI(new OptionFilterSettingsActionUI(caplet), menu);
	}

	protected void initFileMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initFileMenu(rb, menu);

		BridgeCAFUtils.createImportExportCapletMenuItems(caplet, menu, true);

		// add print regions menu
		super.initPrintRegionMenu(rb, menu);
	}

	protected void initActionsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		CapletResourceBuilder resb = rb;
		ActionContainer actionsMenu = menu;
		super.initActionsMenu(resb, actionsMenu);
		resb.addActionUI(new ConvertSymbolToParamActionUI(caplet), actionsMenu);
		resb.addActionUI(new ExportServiceDocumentationActionUI(caplet), actionsMenu);

		resb.addAppAction(new MoveToAction(caplet.getFIB()), actionsMenu);
		resb.addActionUI(new ELANodeAction.UI(caplet), actionsMenu);
		resb.addActionUI(new ELADataManagerAction.UI(caplet), actionsMenu);

		resb.addActionUI(new MoveConnectorActionUI(caplet), actionsMenu);
		resb.addActionUI(new LayoutXDimensionActionUI(caplet), actionsMenu);
		resb.addActionUI(new LayoutYDimensionActionUI(caplet), actionsMenu);
		resb.addActionUI(new LayoutXYDimensionActionUI(caplet), actionsMenu);
		resb.addActionUI(new DimensionFlipActionUI(caplet), actionsMenu);
		resb.addActionUI(new LayoutAssociateDesignsActionUI(caplet), actionsMenu);
		resb.addActionUI(new LayoutDesignResyncActionUI(caplet), actionsMenu);
		resb.addActionUI(new CreateMountWithoutPartAndSymbolActionUI(caplet), actionsMenu);
		resb.addActionUI(new CreateDuctWithoutPartAndSymbolActionUI(caplet), actionsMenu);
		resb.addActionUI(new CreateOtherComponentWithoutPartAndSymbolActionUI(caplet), actionsMenu);
		resb.addActionUI(new UpdateLayoutBOMIDsActionUI(caplet), actionsMenu);
		resb.addActionUI(new DeleteLayoutBOMIDsActionUI(caplet), actionsMenu);
		resb.addActionUI(new RegenerateLayoutBOMIDsActionUI(caplet), actionsMenu);
		// Actions > Pin > Sub menu
		actionsMenu.add(new ActionSeparator());
		Class<?> cls = BaseLogicResource.class;
		ActionContainer pinMenu = CapletResourceBuilder.createSubContainer("Pin", cls);
		resb.addActionUI(new SetPinReferenceActionUI(caplet), pinMenu);
		resb.addActionUI(new SetPinNonReferenceActionUI(caplet), pinMenu);
		// Actions > Symbol > Sub menu
		ActionContainer symbolMenu = CapletResourceBuilder.createSubContainer("Symbol", cls);
		String menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Update.menu.name");
		resb.addActionUI(new UpdateInstanceActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Replace.menu.name");
		resb.addActionUI(new ReplaceInstanceSymbolActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.ReplaceSharedComposite.menu.name");
		resb.addActionUI(new ReplaceSharedCompositeSymbolActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getStringForMenu(cls, "Resource.ExportAsSymbol.menu.name");
		resb.addActionUI(new ExportAsSymbolAction.UI(caplet), symbolMenu, menuName);
		actionsMenu.add(symbolMenu);

		ActionContainer libraryPartMenu = CapletResourceBuilder.createSubContainer("LibraryPart", cls);
		resb.addActionUI(new UpdatePartActionUI(caplet), libraryPartMenu);
		resb.addActionUI(new SaveAssemblyConnectivityToLibraryActionUI(caplet), libraryPartMenu);
		actionsMenu.add(libraryPartMenu);

		// Actions > Shared > Sub menu
		ActionContainer sharedMenu = createSharedMenu(cls);
		resb.addActionUI(new ToggleHomeActionUI(caplet, ToggleHomeActionUI.MARK_HOME), sharedMenu);
		resb.addActionUI(new ToggleHomeActionUI(caplet, ToggleHomeActionUI.REMOVE_HOME), sharedMenu);
		resb.addActionUI(new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.SHOW_XREF), sharedMenu);
		resb.addActionUI(new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.HIDE_XREF), sharedMenu);
		actionsMenu.add(sharedMenu);

		// Actions > Splice > Sub menu
		ActionContainer spliceMenu = CapletResourceBuilder.createSubContainer("Splice", cls);
		spliceMenu
				.putValue(Action.MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(cls, "Resource.Actions.Splice.mnemonic"));
		resb.addActionUI(new StripAtSpliceActionUI(caplet), spliceMenu);
		resb.addActionUI(new TerminateAtSpliceActionUI(caplet), spliceMenu);
		actionsMenu.add(spliceMenu);

		// Actions > Conductor > Sub menu
		ActionContainer conductorActionMenu = CapletResourceBuilder.createSubContainer("Conductor", cls);
		resb.addActionUI(new AutoRouteActionUI(caplet), conductorActionMenu);
		resb.addActionUI(new RouteIntoHighwayActionUI(caplet), conductorActionMenu);
		resb.addActionUI(new UnRouteHighwayActionUI(caplet), conductorActionMenu);
		resb.addActionUI(new RerouteSegmentActionUI(caplet), conductorActionMenu);
		resb.addActionUI(new AddConductorNameActionUI(caplet), conductorActionMenu);
		actionsMenu.add(conductorActionMenu);

		actionsMenu.add(new ActionSeparator());
		ActionContainer updateMenu = CapletResourceBuilder.createSubContainer("Actions.Update", MainResources.class);
		resb.addActionUI(new UpdateBorderActionUI(caplet), updateMenu);
		resb.addActionUI(new UpdateCompositeTextActionUI(caplet), updateMenu);
		// dts0100802895 When this is merged with main resource, maintain the relative position of this
		updateMenu.setPositionRelatively(true);
		actionsMenu.add(updateMenu);

		if (Environment.isVeSysMigrationPathAllowed()) {
			resb.addActionUI(new MoveToGridAction.UI(caplet), actionsMenu);
		}

		resb.addActionUI(new ResetAssemblyActionUI(caplet), actionsMenu);
		resb.addActionUI(new RemoveDeviceConnectorsActionUI(caplet), actionsMenu);
		resb.addActionUI(new ChangeFlowDirectionActionUI(caplet), actionsMenu);
		if (AppInfo.isCapitalLogic()) {
			CustomActionMenuMgr.getInstance()
					.populateActionsForMainMenuType(caplet, actionsMenu, CustomActionMenuType.SMART_FLOW_CONTEXT);
		}
	}

	protected void initAddMenuForLogicObjects(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new AddInstanceActionUI(caplet), menu);
		rb.addActionUI(new AddChainActionUI(caplet), menu);
		rb.addActionUI(new CreateSectorActionUI(caplet), menu);
		menu.add(new ActionSeparator());
		rb.addActionUI(new BatchDevicePlacementActionUI(caplet), menu);
	}

	protected void initToolsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		CapletResourceBuilder resb = rb;
		ActionContainer toolsMenu = menu;
		super.initToolsMenu(resb, toolsMenu);

		ViewsCAFUtils.initAutoViewMenu(caplet.getFIB(), rb, menu);

		toolsMenu.add(new ActionSeparator());
		if (Environment.isInterconnectFlowAllowed()) {
			Class<?> cls = BaseLogicResource.class;
			ActionContainer interconnectDesignMenu =
					CapletResourceBuilder.createSubContainer("InterconnectDesign", cls);
			resb.addAppAction(new GenerateWiringDiagramInteractiveAction(caplet.getFIB()), interconnectDesignMenu);
			toolsMenu.add(interconnectDesignMenu);
		}

		ActionContainer namingMenu = CapletResourceBuilder.createSubContainer("Naming", BaseResource.class);
		initNamingToolsMenus(resb, namingMenu);
		toolsMenu.add(namingMenu);

		toolsMenu.add(new ActionSeparator());
		resb.addActionUI(new AssignDDTTypesActionUI(caplet), toolsMenu);
		resb.addActionUI(new EditDDTTypesActionUI(caplet), toolsMenu);

		toolsMenu.add(new ActionSeparator());
		resb.addActionUI(new MeasureDistanceActionUI(caplet), toolsMenu);

		// some QA extensions that have moved from elsewhere...
		toolsMenu.add(new ActionSeparator());
		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
				BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			toolsMenu.add(new ActionEntry(new CompareDiagramsAction(caplet.getFIB())));
			toolsMenu.add(new ActionEntry(new CheckSchematicConductorConnection(caplet.getFIB())));
		}

		resb.addAppAction(new SelectByUIDAction(getCaplet().getFIB())
		{
			@Override public Iterator<ICapletView> getViews()
			{
				return ViewHelper.getAllActiveDesignViews();
			}
		}, toolsMenu);
		resb.addActionUI(new DumpSelectedActionUI(caplet), toolsMenu);
		resb.addActionUI(new DumpSelectedObjectDetailsActionUI(caplet), toolsMenu);
		resb.addActionUI(new DumpOriginActionUI(caplet), toolsMenu);
		resb.addActionUI(new AddLinkActionUI(caplet), toolsMenu);
		resb.addActionUI(new DumpSelectedSharedPinMatingActionUI(caplet), toolsMenu);
		resb.addAppAction(new DumpICDDeviceActionUI(caplet), toolsMenu);
		resb.addAppAction(new DumpICDSignalsActionUI(caplet), toolsMenu);
		resb.addAppAction(new BuildICDFromJsonActionUI(caplet), toolsMenu);
		resb.addAppAction(new VariantICDTogglePinConstraintActionUI(caplet), toolsMenu);
	}

	protected void initToolbars(CapletResourceBuilder rb)
	{
		super.initToolbars(rb);
		sharedObjectToolbar = new ActionContainer("LogicSharedObject");
		initSharedObjectBrowserLogicToolbar(rb, sharedObjectToolbar);

		layoutDesignLeftToolbar = new ActionContainer("LayoutDesignLeftToolbar");
		initLayoutDesignLeftTabToolbar(rb, layoutDesignLeftToolbar);

		layoutDesignRightToolbar = new ActionContainer("LayoutDesignRightToolbar");
		initLayoutDesignRightTabToolbar(rb, layoutDesignRightToolbar);
	}

	protected void initLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(AddChainActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSectorActionUI.class, toolbar);
		ActionContainer addGraphicsToolBar = new ActionContainer("Add Graphics", true);
		initGraphicsToolbar(rb, addGraphicsToolBar);
		toolbar.add(addGraphicsToolBar);
	}

	protected void initSharedObjectBrowserLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
	}

	private void initLayoutDesignLeftTabToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(LayoutAssociateDesignsActionUI.class, toolbar);
		toolbars.add(toolbar);
	}

	private void initLayoutDesignRightTabToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(LayoutDesignResyncActionUI.class, toolbar);
		toolbars.add(toolbar);
	}

	public ActionContainer getAnalysisToolbar()
	{
		return analysisToolbar;
	}

	@NotNull public ActionContainer getSharedToolbar()
	{
		return sharedObjectToolbar;
	}

	@NotNull @Override public ActionContainer getLayoutDesignLeftToolbar()
	{
		return layoutDesignLeftToolbar;
	}

	@NotNull @Override public ActionContainer getLayoutDesignRightToolbar()
	{
		return layoutDesignRightToolbar;
	}
}
