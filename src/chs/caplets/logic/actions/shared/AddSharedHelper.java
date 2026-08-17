/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caplets.logic.Model;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.system.FactoryMgr;
import chs.utilities.permission.PermissionEnum;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.IMessagingChoices;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.logic.SizeHelper;
import chs.utility.security.PermissionHelper;
import org.jetbrains.annotations.NotNull;

public class AddSharedHelper
{

	private AddSharedHelper()
	{
	}

	protected static void generateConnectorGraphics(IPinList schem_conn, PinListTypeEnum subType,
			SizeHelper sizeH, Model model)
	{
		// Create actual graphics for instantiation.
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IExtent sharedObjExt = commonFactory.constructExtent(0, 0, sizeH.getModelWidth(), sizeH.getModelHeight());
		IParameterized params = commonFactory.createParameterized();
		params.setExtent(sharedObjExt);
		schem_conn.setParameterized(params);

		Generator generator = Generator.getGenerator();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(model.getDiagram());
		gp.setNewObject(true);
		GeneratorStyle gs = generator.getStyle();

		ConnectorHelper.addDefaults(params, gs, subType);

		generator.generateConnector(schem_conn, gp, Generator.REGENERATE_PROPERTIES);

		// Create transient graphic for adding pins.
		IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		IGfxObject rect = drawFactory.constructRectangle(0, 0, 0, 0);
		ILocation sharedObjLoc = schem_conn.getLocation();
		rect.getLocation().setLocation(sharedObjLoc.getX(), sharedObjLoc.getY() - gp.getGrid().getGridSpacing());
		rect.getExtent().setBounds(sharedObjExt.getX(), sharedObjExt.getY(), sharedObjExt.getWidth(),
				sharedObjExt.getHeight() + (2 * gp.getGrid().getGridSpacing()));

		// FEAT 3079: no longer need to offset rectangle when pin edge reversed since
		// CreateConnectorAction.createparamObject() now flips around centre not edge of pinlist.

		sizeH.rotateModel(rect);

		model.getDynamicGfxService().addTransientGfx(rect);
	}

	public static boolean isSharedObjectPermissionDenied()
	{
		if (PermissionHelper.hasPermission(PermissionEnum.SHARED_OBJECTS)) {
			return false;
		}
		//Generate information message when Shared Objects permission is disabled
		ResourceBasedMessageContent messageContent = new ResourceBasedMessageContent(AddSharedHelper.class,
				"AddSharedHelper.NoSharedPermission")
		{
			@Override @NotNull public Choice choice()
			{
				return IMessagingChoices.OK;
			}
		};
		messageContent.setImplicationsParameters(PermissionEnum.SHARED_OBJECTS.value());
		Message.show(PromptSeverity.INFORMATION, messageContent);
		return true;
	}
}
