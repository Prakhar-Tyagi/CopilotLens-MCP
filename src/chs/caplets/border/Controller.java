/*
 * Copyright 2003-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border;

import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolAction;
import chs.caf.caplet.helpers.graphics.CreateArcAction;
import chs.caf.caplet.helpers.graphics.CreateCircleAction;
import chs.caf.caplet.helpers.graphics.CreateCurveAction;
import chs.caf.caplet.helpers.graphics.CreateImageAction;
import chs.caf.caplet.helpers.graphics.CreatePolygonAction;
import chs.caf.caplet.helpers.graphics.CreatePolylineAction;
import chs.caf.caplet.helpers.graphics.CreateRectangleAction;
import chs.caf.caplet.helpers.graphics.CreateTextAction;
import chs.caf.caplet.helpers.graphics.DeleteGfxPointAction;
import chs.caf.caplet.helpers.graphics.FlipAction;
import chs.caf.caplet.helpers.graphics.GroupGfxAction;
import chs.caf.caplet.helpers.graphics.InsertGfxPointAction;
import chs.caf.caplet.helpers.graphics.PivotTextAction;
import chs.caf.caplet.helpers.graphics.PolylineModifier;
import chs.caf.caplet.helpers.graphics.RotateAction;
import chs.caf.caplet.helpers.graphics.SetGraphicDimensionAction;
import chs.caf.caplet.helpers.graphics.UngroupGfxAction;
import chs.caplets.border.actions.CreateFormboardRegionDatumAction;
import chs.caplets.border.actions.EditUserDefinedZonesAction;
import chs.caplets.border.actions.UserZonePropertiesAction;
import chs.caplets.border.properties.BorderPropertiesClient;
import chs.caplets.shared.actions.ModifyZoneAreaAction;
import chs.caplets.symbol.actions.AddDrillPointDatumAction;
import chs.caplets.symbol.actions.AddGenericDatumAction;
import chs.caplets.symbol.actions.AlignAction;
import chs.caplets.symbol.actions.DeleteAction;
import chs.caplets.symbol.actions.DistributeAction;
import chs.caplets.symbol.actions.SelectAction;
import chs.caplets.symbol.actions.SmartEditAction;
import chs.cof.draw.Transform;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IStamp;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

@SuppressWarnings({"ClassNameSameAsAncestorName"})
public class Controller extends chs.caplets.symbol.Controller
{

	public Controller(ICaplet caplet, IAbstractLibrary library, IStamp symdef, boolean isModelEditable)
	{
		super(caplet, library, symdef, isModelEditable);
	}

	@SuppressWarnings({"OverlyLongMethod"})
	protected void createControllerActions()
	{
		// Add the text font and Color Actions to the Controller
		IPropertiesClient propertiesClient = new chs.caplets.symbol.properties.PropertiesClient(m_model);
		addCommonApplicableActions(propertiesClient);

		// Create the controller actions
		IAction selectAction = new SelectAction(this);
		IAction deleteAction = new DeleteAction(this);
		IAction flipAction = new FlipAction(this);
		IAction rotateAction = new RotateAction(this);
		IAction pivotTextAction = new PivotTextAction(this);
		IAction circleCreateAction = new CreateCircleAction(this);
		IAction lineCreateAction = new CreatePolylineAction(this);
		IAction rectangleCreateAction = new CreateRectangleAction(this);
		IAction polygonCreateAction = new CreatePolygonAction(this);
		IAction imageCreateAction = new CreateImageAction(this);
		IAction commentCreateAction = new AddCommentSymbolAction(this);
		IAction arcCreateAction = new CreateArcAction(this);
		IAction curveCreateAction = new CreateCurveAction(this);
		IAction textCreateAction = new CreateTextAction(this);
		IAction modifyZoneAreaAction = new ModifyZoneAreaAction(this);
		IAction insertGfxPtAction = new InsertGfxPointAction(this, new PolylineModifier(this));
		IAction deleteGfxPtAction = new DeleteGfxPointAction(this, new PolylineModifier(this));
		IAction groupGfxAction = new GroupGfxAction(this);
		IAction ungroupGfxAction = new UngroupGfxAction(this);
		IAction smartEditAction = new SmartEditAction(this);

		// Add the actions to the controller
		addAction(new SetGraphicDimensionAction(this));
		addAction(selectAction);
		addAction(deleteAction);
		addAction(circleCreateAction);
		addAction(lineCreateAction);
		addAction(polygonCreateAction);
		addAction(rectangleCreateAction);
		addAction(imageCreateAction);
		addAction(commentCreateAction);
		addAction(arcCreateAction);
		addAction(curveCreateAction);
		addAction(textCreateAction);
		addAction(modifyZoneAreaAction);
		addAction(insertGfxPtAction);
		addAction(deleteGfxPtAction);
		addAction(groupGfxAction);
		addAction(ungroupGfxAction);

		addAction(flipAction);
		addAction(rotateAction);
		addAction(pivotTextAction);

		createDraftingActions();
		createPrintRegionActions();

		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.LEFT));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.RIGHT));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.TOP));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.BOTTOM));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.VERTICAL_CENTER));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.HORIZONTAL_CENTER));
		addAction(new DistributeAction(this, chs.caf.caplet.helpers.graphics.DistributeAction.HORIZONTAL));
		addAction(new DistributeAction(this, chs.caf.caplet.helpers.graphics.DistributeAction.VERTICAL));

		addAction(new CreateFormboardRegionDatumAction(this));
		addAction(new EditUserDefinedZonesAction(this));
		addAction(new UserZonePropertiesAction(this));
		addAction(new AddGenericDatumAction(this));
		addAction(new AddDrillPointDatumAction(this));

		// Add the display construction graphics option.
		Resource resource = (Resource) getCaplet().getResource();
		getCaplet().getFIB().getAppActionMgr().addAction(resource.getDisplayConstructionGraphics());

		createZOrderActions();

		// PW - 04/21/03 - Use the correct TextAttributeEditor
		//addAction(new PropertiesAction(this, new PropertiesClient(m_model)));
		addAction(getPropertiesAction());

		addAction(smartEditAction);

		// Register Strokes
//        addStroke("741236987", deleteAction);
//        addStroke("7412687", deleteAction);	// Aliased
		// add the plugin action if necessary
		// Add App Actions defined by Resource - extend selection action and show functional source action.
		processResourceAppActions();

		// Set the Select Action as the base action in the
		// action manager.
		getActionMgr().setBaseAction(selectAction);
	}

	@NotNull @Override public BorderPropertiesClient createPropertiesClient()
	{
		return new BorderPropertiesClient(m_model);
	}

	public JComponent getBrowser()
	{
		return m_objectBrowser; // Defined in Symbol controller
	}

	@NotNull protected IBrowserClient createBrowserClient()
	{
		return new BorderBrowserClient(this);
	}
}
