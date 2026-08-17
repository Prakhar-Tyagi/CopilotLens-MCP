/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.NewOtherComponentInputResult;
import chs.caplets.logic.actions.layout.DeviceLayoutHelper;
import chs.caplets.logic.actions.layout.IComponentPhysicalDetails;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.LogicOtherComponentTypeEnum;
import chs.cof.logical.cable.PanelLayoutLengthComponentType;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.parameterized.LengthWiseOtherComponentDimensionAdjustment;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utility.logic.LogicUtils;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author chandras on 3-10-2019.
 */
public class CreateLayoutComponentInstanceAction extends AbstractCreateOtherComponentAction
{

	public CreateLayoutComponentInstanceAction(ICapletController controller)
	{
		super(controller, CreateLayoutComponentInstanceActionUI.class.getName());
	}

	@Nullable private ILogicOtherComponent getOperand()
	{
		ListSet<ILogicOtherComponent> otherComponents = new ListSet<>();
		for (Object uidObject : getController().getSelectMgr().getCurrentSelections().getUIDObjects()) {
			final ILogicOtherComponent otherComponent = CommonUtils.cast(uidObject, ILogicOtherComponent.class);
			if (otherComponent != null && !LogicUtils.hasUsage(otherComponent)) {
				otherComponents.add(otherComponent);
			}
		}
		return otherComponents.size() == 1 ? otherComponents.iterator().next() : null;
	}

	@Override public boolean isEnabled()
	{
		return getOperand() != null && super.isEnabled();
	}

	@Nullable @Override protected ILibraryObject acquireSelectedLibraryPart()
	{
		final ILogicOtherComponent operand = getOperand();
		assert operand != null;
		return CommonUtils.cast(operand.getLibraryObject(), ILibraryObject.class);
	}

	@Override protected List<ISymbolDef> acquireSelectedSymbols()
	{
		return getSymbolDefsFromPart(acquireSelectedLibraryPart());
	}

	@NotNull protected Pair<IComponentPhysicalDetails, Boolean> determineParameterized()
	{
		final ILogicOtherComponent operand = getOperand();
		assert operand != null;
		return new Pair<>(DeviceLayoutHelper.getComponentPhysicalDetails(operand), false);
	}

	@Override protected boolean shouldInitPhysicalDimensionAttributes()
	{
		return false;
	}

	@NotNull protected LogicOtherComponentTypeEnum determineDefaultComponentType()
	{
		final ILogicOtherComponent operand = getOperand();
		assert operand != null;
		return operand.getType();
	}

	@NotNull protected ILogicOtherComponent constructConnectivityComponent(@NotNull IConnectivity connectivity,
			@NotNull NewOtherComponentInputResult newOtherComponentInputResult)
	{
		final ILogicOtherComponent operand = getOperand();
		assert operand != null;
		return operand;
	}

	@Nullable protected PanelLayoutLengthComponentType getLengthWiseSubComponentType(
			@NotNull LogicOtherComponentTypeEnum type)
	{
		final ILogicOtherComponent operand = getOperand();
		assert operand != null;
		return LengthWiseOtherComponentDimensionAdjustment.determineDefaultLengthWiseComponentType(type,
				operand.getComponentTypeAsXML());
	}
}