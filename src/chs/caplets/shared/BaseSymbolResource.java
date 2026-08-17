/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.shared;

import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.cafmain.BaseResource;
import chs.caf.cafmain.actions.qa.SpecifyGraphicDimensionsAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.FillColorPickerActionUI;
import chs.caf.caplet.helpers.FillPatternControlActionUI;
import chs.caf.caplet.helpers.FontControlActionUI;
import chs.caf.caplet.helpers.LineStyleAndWeightControlActionUI;
import chs.caf.caplet.helpers.ModifyGridActionUI;
import chs.caf.caplet.helpers.PrimaryColorPickerActionUI;
import chs.caf.caplet.helpers.PropertiesActionUI;
import chs.caf.caplet.helpers.SecondaryColorPickerActionUI;
import chs.caf.caplet.helpers.TextColorPickerActionUI;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolActionUI;
import chs.caf.caplet.helpers.graphics.CreateImageActionUI;
import chs.caf.caplet.helpers.graphics.CreateTextActionUI;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsActionUI;
import chs.caf.caplet.helpers.snapping.SnapToObjectAction;
import chs.caf.helpers.ui.common.CapletResourceBuilder;
import chs.caplets.shared.actions.ToggleSubGridAction;
import chs.caplets.symbol.actions.CreateNameTextActionUI;
import chs.caplets.symbol.actions.CreateXRefTextActionUI;
import chs.caplets.symbol.actions.SelectActionUI;
import chs.caplets.symbol.actions.SymbolPropertiesActionUI;
import chs.utilities.ResourceMgr;

/**
 * Common base class for Symbol related resources (i.e. Symbol & Border)
 */
public abstract class BaseSymbolResource extends BaseResource
{

	protected BaseSymbolResource(ICaplet theCaplet)
	{
		super(theCaplet);
	}

	@SuppressWarnings({"ResultOfObjectAllocationIgnored"}) protected void initActions()
	{
		super.initActions();

		new SelectActionUI(caplet);
	}

	protected PropertiesActionUI getPropertiesActionUI()
	{
		return new SymbolPropertiesActionUI(caplet);
	}

	protected void initFileMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		super.initFileMenu(rb, menu);
		super.initPrintRegionMenu(rb, menu);
	}	

	protected void initGraphicsMenu(CapletResourceBuilder rb, ActionContainer menu)
	{
		// Graphics Menu
		ActionContainer addTextMenu = CapletResourceBuilder.createSubContainer("AddText", BaseResource.class);
		rb.addActionUI(new CreateTextActionUI(caplet), addTextMenu);
		rb.addActionUI(new CreateNameTextActionUI(caplet), addTextMenu);
		rb.addActionUI(new CreateXRefTextActionUI(caplet), addTextMenu);

		menu.add(addTextMenu);


		rb.addActionUI(new FillColorPickerActionUI(caplet),menu);
		rb.addActionUI(new TextColorPickerActionUI(caplet),menu);
		rb.addActionUI(new PrimaryColorPickerActionUI(caplet),menu);
		rb.addActionUI(new SecondaryColorPickerActionUI(caplet),menu);
		rb.addActionUI(new FontControlActionUI(caplet),menu);
		rb.addActionUI(new LineStyleAndWeightControlActionUI(caplet),menu);
		rb.addActionUI(new FillPatternControlActionUI(caplet),menu);


		ActionContainer addShapeMenu = CapletResourceBuilder.createSubContainer("AddShape", BaseResource.class);
		initAddShapeMenus(rb, addShapeMenu);
		menu.add(addShapeMenu);

		ActionContainer addDraftMenu = CapletResourceBuilder.createSubContainer("AddDrafting", BaseResource.class);
		initAddDraftingMenus(rb, addDraftMenu);
		menu.add(addDraftMenu);


		String menuName = ResourceMgr.getString(BaseResource.class, "Resource.AddImage.menu.name");
		rb.addActionUI(new CreateImageActionUI(caplet), menu, menuName);
		menuName = ResourceMgr.getString(BaseResource.class, "Resource.AddCommentSymbol.menu.name");
		rb.addActionUI(new AddCommentSymbolActionUI(caplet), menu, menuName);
		// Fix for defect dts0100904706
		menuName = ResourceMgr.getString(BaseResource.class, "Resource.SymbolPlaceAsGraphics.menu.name");
		rb.addActionUI(new SymbolPlaceAsGraphicsActionUI(caplet), menu, menuName);
		menu.add(new ActionEntry(new SpecifyGraphicDimensionsAction()));

		initGraphicsPointSubMenu(rb, menu); // this is a submenu
		// no Grip points for symbol
		initGroupingMenus(rb, menu); // not a submenu
		initZOrderSubMenu(rb, menu);
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
	}

	protected void initToolbars(CapletResourceBuilder rb)
	{
		super.initToolbars(rb);

		// just the Graphics toolbar is common to Symbol + Border
		ActionContainer graphicsToolBar = new ActionContainer("Graphics");
		initGraphicsToolbar(rb, graphicsToolBar);
		toolbars.add(graphicsToolBar);
	}

	protected Class<?> getSelectActionClass()
	{
		return SelectActionUI.class;
	}
}
