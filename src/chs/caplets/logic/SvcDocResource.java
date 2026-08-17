/*
 * Copyright 2010-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.ActionContainer;
import chs.caf.ActionSeparator;
import chs.caf.cafmain.MainResources;
import chs.caf.cafmain.actions.HydraCopyAction;
import chs.caf.cafmain.actions.HydraMoveToAction;
import chs.caf.cafmain.actions.PublisherCutAction;
import chs.caf.cafmain.actions.UpdateBorderActionUI;
import chs.caf.cafmain.actions.UpdateCompositeTextActionUI;
import chs.caf.cafmain.actions.UpdateInstanceActionUI;
import chs.caf.cafmain.actions.UpdatePrintRegionsActionUI;
import chs.caf.cafmain.actions.ela.ELADataManagerAction;
import chs.caf.cafmain.actions.ela.ELANodeAction;
import chs.caf.cafmain.actions.servicedoc.ExportServiceDocumentationActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IPublisherResource;
import chs.caf.caplet.helpers.graphics.FlipActionUI;
import chs.caf.caplet.helpers.graphics.PivotTextActionUI;
import chs.caf.caplet.helpers.graphics.RotateActionUI;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caf.plugin.CustomActionMenuMgr;
import chs.caf.plugin.CustomActionMenuType;
import chs.caplets.logic.actions.AddConductorNameActionUI;
import chs.caplets.logic.actions.AutoRouteActionUI;
import chs.caplets.logic.actions.ConvertSymbolToParamActionUI;
import chs.caplets.logic.actions.MoveConnectorActionUI;
import chs.caplets.logic.actions.MovePinActionUI;
import chs.caplets.logic.actions.ToggleHomeActionUI;
import chs.caplets.logic.actions.ToggleShowXRefActionUI;
import chs.caplets.logic.actions.rules.LogicSetAttributesAndPropertiesByRuleActionUI;
import chs.caplets.logic.actions.serviceDocumentation.DeleteActionUI;
import chs.caplets.logic.actions.serviceDocumentation.PublisherReplaceInstanceSymbolActionUI;
import chs.caplets.logic.actions.serviceDocumentation.PublisherSmartEditPropertiesActionUI;
import chs.caplets.logic.actions.serviceDocumentation.PublisherUnPlaceActionUI;
import chs.caplets.logic.actions.serviceDocumentation.SelectActionUI;
import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageConnectivityAction;
import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchWithOnlyPinsInSignalAction;
import chs.caplets.logic.actions.serviceDocumentation.shared.PublisherAddBackshellTerminationAction;
import chs.caplets.logic.actions.serviceDocumentation.shared.PublisherAddPinAction;
import chs.caplets.logic.actions.serviceDocumentation.smartflows.ChangeFlowDirectionActionUI;
import chs.caplets.logic.actions.shared.ReplaceSharedCompositeSymbolActionUI;
import chs.caplets.publisher.PublisherPropertiesActionUI;
import chs.caplets.shared.BaseLogicResource;
import chs.utilities.ResourceMgr;

import java.util.Locale;

/**
 * FEAT14997 - Offline Service Documentation User: kayyagar Date: Oct 12, 2010 Time: 7:28:07 PM
 */
public class SvcDocResource extends LogicResource implements IPublisherResource

{

	protected void initLogicToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		//Add tool bar items specific to svc doc app.
		// just the Graphics toolbar is common to Symbol + Border
		ActionContainer graphicsToolBar = new ActionContainer("Graphics");
		initGraphicsToolbar(rb, graphicsToolBar);
		toolbars.add(graphicsToolBar);
		//Add publisher toolbar which has reports and generate svc doc actions
		initPublisherToolbar(rb, toolbars);
	}

	public SvcDocResource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	public void init(Locale locale)
	{
		// some actions are created here
		initActions();

		// most actions are created with the menus
		// NOTE: initMenus must be called first - creates the actions used by other init... methods
		CapletResourceBuilder rb = new CapletResourceBuilder(caplet);
		initMenus(rb);

		// add toolbars for actions
		initToolbars(rb);

		// add strokes for actions
		initStrokes(rb);

		// analysis & bridges have not yet been refactored - do all the action/menu/toolbar setup in one for these
		addAnalysisActions();
		addBridgesActions(rb);
		initAnalysisBrowserToolbar();

		// do this after the analysis actions so analysis still gets first place in the design inspector
		initDesignInspectors();
	}

	/**
	 * Create actions that are not otherwise created on construction of menus/toolbars.
	 */
	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		new SelectActionUI(caplet);
		new PublisherUnPlaceActionUI(caplet);
		super.initActions();
		new PublisherSmartEditPropertiesActionUI(caplet);
	}

	@SuppressWarnings({"NoopMethodInAbstractClass"})
	protected void initDesignInspectors()
	{
		super.initDesignInspectors();
	}

	/**
	 * Create pulldown menus, constructs actions as required by menus.
	 *
	 * @param rb - CapletResourceBuilder
	 */
	protected void initMenus(CapletResourceBuilder rb)
	{
		// File
		ActionContainer menu = CapletResourceBuilder.createActionContainer("File", false, null, MainResources.class);
		initFileMenu(rb, menu);
		menus.add(menu);

		// Edit
		menu = CapletResourceBuilder.createActionContainer("Edit", false, null, MainResources.class);
		initEditMenu(rb, menu);
		menus.add(menu);

		// View
		menu = CapletResourceBuilder.createActionContainer("View", false, null, MainResources.class);
		initViewMenu(rb, menu);
		menus.add(menu);

		// Actions
		menu = CapletResourceBuilder.createActionContainer("Actions", false, null, MainResources.class);
		initActionsMenu(rb, menu);
		menus.add(menu);

		// Tools
/*		if (getCustomClass() != null) {
			menu.add(CAFCustomActionMgr.getInstance().createPublicCustomMainMenu(caplet));
			menu.add(CAFCustomActionMgr.getInstance().createPrivateCustomMainMenu(caplet));
		}*/
		menu = CapletResourceBuilder.createActionContainer("Tools", false, null, MainResources.class);
		initToolsMenu(rb, menu);
		menus.add(menu);

		// Graphics
		menu = CapletResourceBuilder.createActionContainer("Graphics", false, null, MainResources.class);
		initGraphicsMenu(rb, menu);
		menus.add(menu);

		// Layout
		menu = CapletResourceBuilder.createActionContainer("Layout", false, null, MainResources.class);
		initLayoutMenu(rb, menu);
		menus.add(menu);

		// Window
		menu = CapletResourceBuilder.createActionContainer("Window", false, null, MainResources.class);
		initWindowMenu(rb, menu);
		menus.add(menu);

		// Help
		menu = CapletResourceBuilder.createActionContainer("Help", false, null, MainResources.class);
		initHelpMenu(rb, menu);
		menus.add(menu);

		// DevTest
		menu = CapletResourceBuilder.createDeveloperExtensionMenu();
		initDevTestMenu(rb, menu);
		menus.add(menu);
	}

	/**
	 * Add File actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initFileMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initFileMenu(rb, menu);
	}

	/**
	 * Add Edit actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initEditMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initEditMenu(rb, menu);
		rb.addActionUI(new PublisherPropertiesActionUI(caplet), menu);
		menu.add(new ActionSeparator());
		rb.addActionUI(new DeleteActionUI(caplet), menu);
	}

	/**
	 * Add View actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initViewMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initViewMenu(rb, menu);
	}

	/**
	 * Add Actions actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initActionsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		//super.initActionsMenu(rb,menu);
		rb.addAppAction(new FetchWithOnlyPinsInSignalAction.UIWithOnlyPinsInSignal(caplet), menu);
		rb.addAppAction(new FetchOffPageConnectivityAction.UI(caplet), menu);
		rb.addActionUI(new PublisherAddPinAction.UI(caplet), menu);
//		rb.addActionUI(new PublisherAddPinWNAccelAction.UI(caplet), menu);
//		rb.addActionUI(new PublisherAddBackshellAction.UI(caplet), menu);
		rb.addActionUI(new PublisherAddBackshellTerminationAction.UI(caplet), menu);
		rb.addAppAction(new HydraMoveToAction(caplet.getFIB()), menu);
		rb.addActionUI(new LogicSetAttributesAndPropertiesByRuleActionUI(caplet), menu);

		rb.addActionUI(new ConvertSymbolToParamActionUI(caplet), menu);
		// Actions > Conductor > Sub menu
		Class<?> cls = BaseLogicResource.class;
		// Actions > Symbol > Sub menu
		ActionContainer symbolMenu = CapletResourceBuilder.createSubContainer("Symbol", cls);
		String menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Update.menu.name");
		rb.addActionUI(new UpdateInstanceActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.Actions.Symbol.Replace.menu.name");
		rb.addActionUI(new PublisherReplaceInstanceSymbolActionUI(caplet), symbolMenu, menuName);
		menuName = ResourceMgr.getString(cls, "Resource.ReplaceSharedComposite.menu.name");
		rb.addActionUI(new ReplaceSharedCompositeSymbolActionUI(caplet), symbolMenu, menuName);
		menu.add(symbolMenu);

		rb.addActionUI(new MoveConnectorActionUI(caplet), menu);
		// Actions > Pin > Sub menu
		ActionContainer pinMenu = CapletResourceBuilder.createSubContainer("Pin", cls);
		menuName = ResourceMgr.getString(cls, "Resource.MovePins.menu.name");
		rb.addActionUI(new MovePinActionUI(caplet), pinMenu, menuName);

		// Actions > Shared > Sub menu
		ActionContainer sharedMenu = createSharedMenu(cls);
		rb.addActionUI(new ToggleHomeActionUI(caplet, ToggleHomeActionUI.MARK_HOME), sharedMenu);
		rb.addActionUI(new ToggleHomeActionUI(caplet, ToggleHomeActionUI.REMOVE_HOME), sharedMenu);
		rb.addActionUI(new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.SHOW_XREF), sharedMenu);
		rb.addActionUI(new ToggleShowXRefActionUI(caplet, ToggleShowXRefActionUI.HIDE_XREF), sharedMenu);
		sharedMenu.add(new ActionSeparator());
		menu.add(sharedMenu);
		rb.addActionUI(new ExportServiceDocumentationActionUI(caplet), menu);
		rb.addActionUI(new ELANodeAction.UI(caplet), menu);
		rb.addActionUI(new ELADataManagerAction.UI(caplet), menu);
		rb.addActionUI(new UpdatePrintRegionsActionUI(caplet), menu);
		//Actions > Conductor > Sub menu
		ActionContainer conductorActionMenu = CapletResourceBuilder.createSubContainer("Conductor", cls);
		rb.addActionUI(new AutoRouteActionUI(caplet), conductorActionMenu);
		rb.addActionUI(new AddConductorNameActionUI(caplet), conductorActionMenu);
		menu.add(conductorActionMenu);
		menu.add(new ActionSeparator());
		//menu needs to be added with service documentation action
		ActionContainer updateMenu = CapletResourceBuilder.createSubContainer("Actions.Update", MainResources.class);
		rb.addActionUI(new UpdateBorderActionUI(caplet), updateMenu);
		rb.addActionUI(new UpdateCompositeTextActionUI(caplet), updateMenu);

		rb.addActionUI(new ChangeFlowDirectionActionUI(caplet), menu);

		CustomActionMenuMgr.getInstance()
				.populateActionsForMainMenuType(caplet, menu, CustomActionMenuType.SMART_FLOW_CONTEXT);

		menu.add(updateMenu);
	}

	/**
	 * Add Graphics actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initGraphicsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initGraphicsMenu(rb, menu);
	}

	/**
	 * Add Layout actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initLayoutMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initLayoutMenu(rb, menu);
	}

	/**
	 * Add Window actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initWindowMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initWindowMenu(rb, menu);
	}

	/**
	 * Add Help actions/menus common to all active caplets.
	 * <p/>
	 * Derived clases can override this to add caplet specific actions/menus.
	 *
	 * @param rb   - CapletResourceBuilder
	 * @param menu - ActionContainer
	 */
	protected void initHelpMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initHelpMenu(rb, menu);
	}

	/**
	 * Construct toolbars, assumes that actions have already been constructed.
	 *
	 * @param rb - CapletResourceBuilder
	 */
	protected void initToolbars(CapletResourceBuilder rb)
	{
		super.initToolbars(rb);
	}

	@Override protected void initEditToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addAppActionEntry(PublisherCutAction.class, toolbar);
		rb.addAppActionEntry(HydraCopyAction.class, toolbar);
		super.initEditToolbar(rb, toolbar);
	}

	protected void initOperationToolbar(CapletResourceBuilder rb, ActionContainer toolbar)
	{
		rb.addActionUIEntry(getSelectActionClass().getName(), toolbar);
		rb.addActionUIEntry(FlipActionUI.class.getName(), toolbar);
		rb.addActionUIEntry(RotateActionUI.class.getName(), toolbar);
		rb.addActionUIEntry(PivotTextActionUI.class.getName(), toolbar);

		addAlignMenuToToolbar(rb, toolbar);
		addDistributeMenuToToolbar(rb, toolbar);
	}

	protected Class<?> getSelectActionClass()
	{
		return SelectActionUI.class;
	}

	/**
	 * Construct analysis actions, menus & toolbars.
	 * <p/>
	 * Analysis setup does not create these in separate methods.
	 */
	protected void addAnalysisActions()
	{
		super.addAnalysisActions();
	}

	protected void initAnalysisBrowserToolbar()
	{
		super.initAnalysisBrowserToolbar();
	}

	/**
	 * Construct bridges actions, menus & toolbars.
	 * <p/>
	 * Bridges setup does not create these in separate methods.
	 *
	 * @param rb
	 */
	protected void addBridgesActions(CapletResourceBuilder rb)
	{
		super.addBridgesActions(rb);
	}
}
