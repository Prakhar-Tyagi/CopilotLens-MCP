/*
 * Copyright 2010-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.function;

import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.AppAction;
import chs.caf.cafmain.BaseResource;
import chs.caf.cafmain.MainResources;
import chs.caf.cafmain.actions.DesignInspectorAction;
import chs.caf.cafmain.actions.EditLeaderLineJustificationActionUI;
import chs.caf.cafmain.actions.ExportAsFunctionSymbolAction;
import chs.caf.cafmain.actions.MoveToAction;
import chs.caf.cafmain.actions.ReplaceInstanceSymbolActionUI;
import chs.caf.cafmain.actions.ToggleOptionDescriptionActionUI;
import chs.caf.cafmain.actions.UpdateBorderActionUI;
import chs.caf.cafmain.actions.UpdateCompositeTextActionUI;
import chs.caf.cafmain.actions.UpdateInstanceActionUI;
import chs.caf.cafmain.actions.bridges.BridgeOptionActionUI;
import chs.caf.cafmain.actions.bridges.TCBridgeOutActionUI;
import chs.caplets.logic.actions.CreateGeneralHighwayActionUI;
import chs.caplets.logic.actions.UpdateDictionaryActionUI;
import chs.caf.cafmain.actions.link.AddLinkActionUI;
import chs.caf.cafmain.actions.topology.networks.AvionicsExportActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.PropertiesActionUI;
import chs.caf.caplet.helpers.SmartEditActionUI;
import chs.caf.caplet.helpers.SmartEditPropertiesActionUI;
import chs.caf.caplet.helpers.SymbolInvokeFromDesignToolsActionUI;
import chs.caf.caplet.helpers.automation.DumpSelectedObjectDetailsActionUI;
import chs.caf.caplet.helpers.debug.SelectByUIDAction;
import chs.caf.caplet.helpers.graphics.MeasureDistanceActionUI;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsActionUI;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caplets.capture.actions.CaptureDeleteActionUI;
import chs.caplets.logic.ISharedObjectToolbarProvider;
import chs.caplets.logic.actions.AddConductorNameActionUI;
import chs.caplets.logic.actions.AddFunctionPortActionUI;
import chs.caplets.logic.actions.AddInstanceActionUI;
import chs.caplets.logic.actions.AutoRouteActionUI;
import chs.caplets.logic.actions.CheckSchematicConductorConnection;
import chs.caplets.logic.actions.CompareDiagramsAction;
import chs.caplets.logic.actions.ConnectActionUI;
import chs.caplets.logic.actions.ConnectByNetActionUI;
import chs.caplets.logic.actions.ConvertSymbolToParamActionUI;
import chs.caplets.logic.actions.CreateFunctionBlockActionUI;
import chs.caplets.logic.actions.CreateFunctionConductorActionUI;
import chs.caplets.logic.actions.CreateFunctionMessageActionUI;
import chs.caplets.logic.actions.CreateNoPinFunctionActionUI;
import chs.caplets.logic.actions.CreateSectorActionUI;
import chs.caplets.logic.actions.DisconnectFunctionPortActionUI;
import chs.caplets.logic.actions.ExpandSelectionAction;
import chs.caplets.logic.actions.ManageSignalsActionUI;
import chs.caplets.logic.actions.MergeIntoActionUI;
import chs.caplets.logic.actions.MoveFunctionPortActionUI;
import chs.caplets.logic.actions.OptionFilterSettingsActionUI;
import chs.caplets.logic.actions.ResizeActionUI;
import chs.caplets.logic.actions.ToggleHomeActionUI;
import chs.caplets.logic.actions.ToggleShowXRefActionUI;
import chs.caplets.logic.actions.debug.DumpSelectedActionUI;
import chs.caplets.logic.actions.shared.AddSharedFunctionActionUI;
import chs.caplets.logic.actions.shared.AddSharedMessageActionUI;
import chs.caplets.logic.actions.shared.AddSharedSignalActionUI;
import chs.caplets.logic.actions.shared.CreateSharedObjectRevisionActionUI;
import chs.caplets.logic.actions.shared.EditSharedPinListActionUI;
import chs.caplets.logic.actions.shared.EnhancedSwapOutSharedObjectRevisionActionUI;
import chs.caplets.logic.actions.shared.FreezeSharedObjectsActionUI;
import chs.caplets.logic.actions.shared.FreezeUnfreezeSharedObjectActionUI;
import chs.caplets.logic.actions.shared.LogicSharedObjectDeleteActionUI;
import chs.caplets.logic.actions.shared.LogicSharedObjectDeleteUnusedActionUI;
import chs.caplets.logic.actions.shared.ReplaceSharedCompositeSymbolFunctionActionUI;
import chs.caplets.logic.actions.shared.ShareActionUI;
import chs.caplets.logic.actions.shared.SharedObjectRevisionUsagesActionUI;
import chs.caplets.logic.actions.shared.SwapOutSharedObjectRevisionActionUI;
import chs.caplets.logic.actions.shared.UnshareActionUI;
import chs.caplets.shared.BaseLogicResource;
import chs.utilities.BuildInfo;
import chs.utilities.ResourceMgr;
import com.mentor.chs.plugin.designinspection.IXFunctionDesignInspectionPanel;
import com.mentor.chs.plugin.designinspection.IXInspectionPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class FunctionResource extends BaseLogicResource implements ISharedObjectToolbarProvider
{

	private ActionContainer sharedObjectToolbar = null;

	public FunctionResource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	protected Class<? extends IXInspectionPanel> getInspectionPanelClazz()
	{
		return IXFunctionDesignInspectionPanel.class;
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		super.initActions();
		new CreateFunctionConductorActionUI(caplet);
		new CreateFunctionMessageActionUI(caplet);
		new CreateNoPinFunctionActionUI(caplet);
		new CreateFunctionBlockActionUI(caplet);
		new SymbolPlaceAsGraphicsActionUI(caplet);
		new SymbolInvokeFromDesignToolsActionUI(caplet);
		new SmartEditActionUI(caplet);
		new SmartEditPropertiesActionUI(caplet);
		new ToggleHomeActionUI(caplet, ToggleHomeActionUI.MARK_HOME);
		new ToggleHomeActionUI(caplet, ToggleHomeActionUI.REMOVE_HOME);
		new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.SHOW_XREF);
		new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.HIDE_XREF);
		new LogicSharedObjectDeleteActionUI(caplet);
		new LogicSharedObjectDeleteUnusedActionUI(caplet);
		new FreezeUnfreezeSharedObjectActionUI(caplet);
		new EditSharedPinListActionUI(caplet);
		new FreezeSharedObjectsActionUI(caplet);
		new AddSharedSignalActionUI(caplet);
		new EnhancedSwapOutSharedObjectRevisionActionUI(caplet);
		new SharedObjectRevisionUsagesActionUI(caplet);
		new SwapOutSharedObjectRevisionActionUI(caplet);
		new CreateSharedObjectRevisionActionUI(caplet);
		new EditLeaderLineJustificationActionUI(caplet);
		new AvionicsExportActionUI(caplet);
		new TCBridgeOutActionUI(caplet);
		new BridgeOptionActionUI(caplet);
	}

	protected void initLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(CreateNoPinFunctionActionUI.class, toolbar);
		rb.addActionUIEntry(CreateFunctionBlockActionUI.class, toolbar);
		rb.addActionUIEntry(CreateFunctionConductorActionUI.class, toolbar);
		rb.addActionUIEntry(CreateGeneralHighwayActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSectorActionUI.class, toolbar);

		ActionContainer addGraphicsToolBar = new ActionContainer("Add Graphics", true);
		initGraphicsToolbar(rb, addGraphicsToolBar);
		toolbar.add(addGraphicsToolBar);
	}

	protected void initAddMenuForLogicObjects(CapletResourceBuilder rb, ActionContainer menu)
	{
		// these names are repeated on several menus...
		Class<?> cls = BaseLogicResource.class;
		String functionMenuName = ResourceMgr.getString(cls, "Resource.Function.menu.name");
		rb.addActionUI(new CreateNoPinFunctionActionUI(caplet), menu, functionMenuName);
		String functionCondMenuName = ResourceMgr.getString(cls, "Resource.FunctionConductor.menu.name");
		rb.addActionUI(new CreateFunctionConductorActionUI(caplet), menu, functionCondMenuName);
		rb.addActionUI(new CreateFunctionMessageActionUI(caplet), menu);
		String funcBlockMenuName = ResourceMgr.getString(cls, "Resource.FunctionBlock.menu.name");
		rb.addActionUI(new CreateFunctionBlockActionUI(caplet), menu, funcBlockMenuName);
		rb.addActionUI(new AddFunctionPortActionUI(caplet), menu);
		rb.addActionUI(new CreateGeneralHighwayActionUI(caplet), menu);
		rb.addActionUI(new CreateSectorActionUI(caplet), menu);
		rb.addActionUI(new AddInstanceActionUI(caplet), menu);
		menu.add(new ActionSeparator());
		ActionContainer sharedMenu = CapletResourceBuilder.createSubContainer("Shared", cls);
		rb.addActionUI(new AddSharedFunctionActionUI(caplet), sharedMenu, "AddSharedFunction");
		rb.addActionUI(new AddSharedMessageActionUI(caplet), sharedMenu, "AddSharedMessage");
		menu.add(sharedMenu);
	}

	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
		menu.add(new ActionCheckBox(new ToggleOptionDescriptionActionUI(caplet)));
		rb.addAppAction(new DesignInspectorAction(caplet.getFIB()), menu);
		rb.addActionUI(new OptionFilterSettingsActionUI(caplet), menu);
	}

	@Override protected void initLayoutMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		rb.addActionUI(new ResizeActionUI(caplet), menu);
		super.initLayoutMenu(rb, menu);
	}

	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initEditMenu(rb, menu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new CaptureDeleteActionUI(caplet), menu);

		menu.add(new ActionSeparator());
		AppAction selectByNameAction = new SelectByNameActionImpl(caplet.getFIB());
		rb.addAppAction(selectByNameAction, menu);

		Class<?> cls = BaseLogicResource.class;
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
	}

	protected void initAnalysisBrowserToolbar()
	{
	}

	protected void addAnalysisActions()
	{
	}

	protected void initActionsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initActionsMenu(rb, menu);

		rb.addActionUI(new MergeIntoActionUI(caplet), menu);
		rb.addActionUI(new ManageSignalsActionUI(caplet), menu);
		rb.addActionUI(new ConvertSymbolToParamActionUI(caplet), menu);
		rb.addActionUI(new ShareActionUI(caplet), menu);
		rb.addActionUI(new UnshareActionUI(caplet), menu);
		rb.addAppAction(new MoveToAction(caplet.getFIB()), menu);
		Class<?> cls = BaseLogicResource.class;

		// Actions > Pin > Sub menu
		menu.add(new ActionSeparator());
		ActionContainer pinMenu = CapletResourceBuilder.createSubContainer("FunctionPort", cls);
		String menuName = ResourceMgr.getString(cls, "Resource.MoveFunctionPorts.menu.name");
		rb.addActionUI(new MoveFunctionPortActionUI(caplet), pinMenu, menuName);
		menu.add(pinMenu);

		// Actions > Symbol > Sub menu
		ActionContainer symbolMenu = CapletResourceBuilder.createSubContainer("Symbol", cls);
		menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Update.menu.name");
		rb.addActionUI(new UpdateInstanceActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Replace.menu.name");
		rb.addActionUI(new ReplaceInstanceSymbolActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.ReplaceSharedComposite.menu.name");
		rb.addActionUI(new ReplaceSharedCompositeSymbolFunctionActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getStringForMenu(cls, "Resource.ExportAsFunctionSymbol.menu.name");
		rb.addActionUI(new ExportAsFunctionSymbolAction.FunctionUI(caplet), symbolMenu, menuName);
		menu.add(symbolMenu);

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

		// Actions > Conductor > Sub menu
		ActionContainer conductorActionMenu = CapletResourceBuilder.createSubContainer("FunctionConductor", cls);
		rb.addActionUI(new AddConductorNameActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new AutoRouteActionUI(caplet), conductorActionMenu);
		menu.add(conductorActionMenu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new DisconnectFunctionPortActionUI(caplet), menu);
		rb.addActionUI(new UpdateDictionaryActionUI(caplet), menu);

		// Actions > Connect > Sub menu
		ActionContainer connectActionMenu = new ActionContainer(new ConnectActionUI(caplet));
		rb.addActionUI(new ConnectByNetActionUI(caplet), connectActionMenu);

		menu.add(new ActionSeparator());
		ActionContainer updateMenu = CapletResourceBuilder.createSubContainer("Actions.Update", MainResources.class);
		rb.addActionUI(new UpdateBorderActionUI(caplet), updateMenu);
		rb.addActionUI(new UpdateCompositeTextActionUI(caplet), updateMenu);
		// dts0100802895 When this is merged with main resource, maintain the relative position of this
		updateMenu.setPositionRelatively(true);
		menu.add(updateMenu);
	}

	protected void initToolsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initToolsMenu(rb, menu);

		menu.add(new ActionSeparator());

		ActionContainer namingMenu = CapletResourceBuilder.createSubContainer("Naming", BaseResource.class);
		initNamingToolsMenus(rb, namingMenu);
		menu.add(namingMenu);

		menu.add(new ActionSeparator());
		rb.addActionUI(new MeasureDistanceActionUI(caplet), menu);

		// some QA extensions that have moved from elsewhere...
		menu.add(new ActionSeparator());
		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
				BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			menu.add(new ActionEntry(new CompareDiagramsAction(caplet.getFIB())));
			menu.add(new ActionEntry(new CheckSchematicConductorConnection(caplet.getFIB())));
		}

		rb.addAppAction(new SelectByUIDAction(getCaplet().getFIB()), menu);
		rb.addActionUI(new DumpSelectedActionUI(caplet), menu);
		rb.addActionUI(new DumpSelectedObjectDetailsActionUI(caplet), menu);
		rb.addActionUI(new AddLinkActionUI(caplet), menu);
	}

	@NotNull @Override public ActionContainer getSharedToolbar()
	{
		return sharedObjectToolbar;
	}

	protected void initToolbars(CapletResourceBuilder rb)
	{
		super.initToolbars(rb);
		sharedObjectToolbar = new ActionContainer("LogicSharedObject");
		initSharedObjectBrowserLogicToolbar(rb, sharedObjectToolbar);
	}

	protected void initSharedObjectBrowserLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(EditSharedPinListActionUI.class, toolbar);
		rb.addActionUIEntry(CreateSharedObjectRevisionActionUI.class, toolbar);
		rb.addActionUIEntry(FreezeSharedObjectsActionUI.class, toolbar);
		rb.addActionUIEntry(LogicSharedObjectDeleteUnusedActionUI.class, toolbar);
		rb.addActionUIEntry(LogicSharedObjectDeleteActionUI.class, toolbar);
	}
}
