/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caplets.logic.actions.AddFunctionPinActionHelper;
import chs.caplets.logic.actions.AddPinActionHelper;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.system.FactoryMgr;
import org.jetbrains.annotations.NotNull;

public class AddSharedFunctionAction extends AddSharedDeviceAction
{

	public AddSharedFunctionAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
	}

	protected String getCtxCommand()
	{
		return "AddSharedFunction";
	}

	@NotNull @Override protected AddPinActionHelper getAddPinActionHelper()
	{
		return new AddFunctionPinActionHelper(this, false, true);
	}

	@Override protected String getObjectType()
	{
		return "function";
	}

	@Override protected PinListTypeEnum getType()
	{
		return PinListTypeEnum.TypeFunction;
	}

	protected IPinList createLogicDevice()
	{
		return FactoryMgr.getCableFactory().createFunction(FactoryMgr.getCommonFactory().createUID());
	}

	@Override public String getActionUIClass()
	{
		return AddSharedFunctionActionUI.class.getName();
	}

	@Override
	protected String getFeedbackResourceString(){
		return "CreateParameterizedObjectAction.FunctionFeedback.text";
	}

	@Override
	protected String getRotatedFeedbackResourceString()
	{
		return "CreateParameterizedObjectAction.FunctionRotatedFeedback.text";
	}
}
