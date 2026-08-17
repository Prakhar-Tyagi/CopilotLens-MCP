/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.actions.AddPinActionHelper;
import chs.caplets.logic.actions.IAddBlockPinActionModel;
import chs.caplets.logic.actions.IAddPinView;
import chs.caplets.logic.actions.PlaceBlockPinDialog;
import chs.caplets.logic.actions.PlacePinsDialog;
import chs.caplets.logic.shared.AddSharedPinDialog;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.ISupplementaryObject;
import chs.cof.logical.ILogicObjectDesignContainer;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.common.IUIDObject;
import chs.common.styles.IStyleableObject;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.system.FactoryMgr;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.logic.sharedpinconnection.ISharedPinMatingsProvider;
import chs.utility.logic.sharedpinconnection.SharedPinConnectionFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.Point;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * This action helper makes sure that we only place pins for shared pinlists Also when pins are placed, we make sure
 * that the connectivity is not altered at a project/active buildlist level
 */
public class PublisherAddPinActionHelper extends AddPinActionHelper
{

	protected SharedPinConnectivityHelper m_sharedPinConnectivityHelper;

	public PublisherAddPinActionHelper(ControllerActionRT action, boolean requirePlacement,
			boolean useBoundaryExtensions)
	{
		super(action, requirePlacement, useBoundaryExtensions);
		BiFunction<ISharedPinList, ILogicObjectDesignContainer, ISharedPinMatingsProvider> sharePinMatingsProvider =
				createSharedPinsMatingProvider();
		m_sharedPinConnectivityHelper = new SharedPinConnectivityHelper(sharePinMatingsProvider);
	}

	@NotNull protected BiFunction<ISharedPinList, ILogicObjectDesignContainer, ISharedPinMatingsProvider> createSharedPinsMatingProvider()
	{
		return (sharedPinList, design) -> {
			ISharedPinReservationView sharedPinReservationView =
					FactoryMgr.getCommonFactory().constructSharedPinReservationView(sharedPinList);
			return new SharedPinConnectionFinder(sharedPinList, design.getUID(), sharedPinReservationView);
		};
	}

	void cacheSharedPinDetails(Set<chs.cof.logical.schem.IPinList> pinLists)
	{
		m_sharedPinConnectivityHelper.cacheSharedPinDetails(pinLists);
	}

	protected boolean isPinCreationAllowed(IPinList cablePinList)
	{
		return false;
	}

	@Override @NotNull protected IAddPinView getPlacePinDialog(IPinList cpl, @NotNull IPlacementOptionParams params)
	{
		if (cpl instanceof IBlockDevice) {
			return new PlaceBlockPinDialog(CAFUtils.getInstance().getDialogFrame(),
					(IAddBlockPinActionModel) m_pinActionModel, false, params);
		}
		return new PlacePinsDialog(CAFUtils.getInstance().getDialogFrame(), cpl, isPinCreationAllowed(cpl),
				params);
	}

	@NotNull @Override
	protected AddSharedPinDialog getSharedPinSelectionDialog(Frame parentFrame, ISharedPinList sharedPinList)
	{
		return new PublisherAddSharedPinDialog(parentFrame, getDialogTitle(), sharedPinList);
	}

	protected boolean isStackPinAllowed()
	{
		return false;
	}

	protected void addConnectivity(chs.cof.logical.schem.IPinList pinList)
	{
		super.addConnectivity(pinList);
		PublisherPinPlacementController pinPlacementController =
				(PublisherPinPlacementController) getPinPlacementController();
		boolean reference = isReference();
		if (reference) {
			return;
		}
		AutoAddSharedMatedPinHelper autoAddSharedMatedPinHelper =
				createAutoAddSharedMatedPinHelper(pinPlacementController);
		autoAddSharedMatedPinHelper.addSharedConnectivityPinsToMates(pinList);
	}

	@NotNull @Override protected IPlacementOptionParams createPlacementOptionParams(@NotNull IPinList cpl)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(cpl);
		params.enableAsStackOption(false);
		return params;
	}

	@NotNull protected AutoAddSharedMatedPinHelper createAutoAddSharedMatedPinHelper(
			PublisherPinPlacementController pinPlacementController)
	{
		return new AutoAddSharedMatedPinHelper(m_pinActionModel, m_sharedPinConnectivityHelper, pinPlacementController,
				false);
	}

	@Override public void commit()
	{
		super.commit();
		Iterator<IUIDObject> newObjects = CreationDeletionHelper.getTheCreationHelper().getNewObjectsToProcess();
		while (newObjects.hasNext()) {
			IUIDObject obj = newObjects.next();
			if (ISupplementaryObject.class.isInstance(obj)) {
				ISupplementaryObject supplementaryObject = ISupplementaryObject.class.cast(obj);
				supplementaryObject.markAsSupplementary();
				if (IStyleableObject.class.isInstance(obj)) {
					IStyleableObject styleableObject = IStyleableObject.class.cast(obj);
					styleableObject.forceApplyStyle();
				}
			}
		}
	}

	@NotNull @Override protected PinPlacementController createPinPlacementController()
	{
		return new PublisherPinPlacementController();
	}

	protected class PublisherPinPlacementController extends PinPlacementController
	{

		protected int allow(Point currPt, @Nullable IAbstractPin placementPin,
				boolean addingBackshell, boolean editingStack, boolean assumeInfiniteExtBoundary,
				PinPlacementConstraintsHolder constraintsHolder)
		{
			int allow = super.allow(currPt, placementPin, addingBackshell, editingStack,
					assumeInfiniteExtBoundary, constraintsHolder);
			return allow(currPt, placementPin, constraintsHolder, allow);
		}

		protected int allow(Point currPt, @Nullable IAbstractPin placementPin,
				PinPlacementConstraintsHolder constraintsHolder, int allow)
		{
			//if the super says no placement, no need to go further
			if (allow == PinPlacementConstraintsHolder.PLACEMENT_NO) {
				return allow;
			}
			if (placementPin != null) {
				chs.cof.logical.schem.IPinList anchor = getAnchor();
				IPinList connectivityPinList = anchor.getConnectivity();
				//only do the additional checks for device or device owned
				if (!(connectivityPinList instanceof IDeviceOwned || connectivityPinList instanceof IDevice)) {
					return allow;
				}
				IGfxObject objectAt = constraintsHolder.getObjectAt(currPt);
				IGfxObject matchingObject = constraintsHolder.getMatchingObject(objectAt);
				if (matchingObject == null) {
					if (isReference()) {
						return allow;
					}
					if (!m_sharedPinConnectivityHelper.allowPlacePinWithNoConnection(placementPin, anchor)) {
						return PinPlacementConstraintsHolder.PLACEMENT_NO;
					}
					//if there is no matching object --> only allow pins if it is a device
					return allow;
				}
				if ((matchingObject instanceof IPinPlaceholder)) {
					if (isReference()) {
						return allow;
					}
					IPinPlaceholder toMatePinPlaceHolder = (IPinPlaceholder) matchingObject;
					IDiagramObject mateOwner = toMatePinPlaceHolder.getOwner();
					if (mateOwner instanceof chs.cof.logical.schem.IPinList) {
						//if there is a mated plug, then check if it is allowed to add the pin
						chs.cof.logical.schem.IPinList matedSchemPL = (chs.cof.logical.schem.IPinList) mateOwner;
						if (!m_sharedPinConnectivityHelper
								.allowConnectionWithPlaceHolder(placementPin, anchor, matedSchemPL)) {
							return PinPlacementConstraintsHolder.PLACEMENT_NO;
						}
					}
				}
				else if (matchingObject instanceof IPin) {
					//if there is a pin at matching location, check if the connection is allowed by checking shared pin connections
					IPin toMatePin = (IPin) matchingObject;
					if (!m_sharedPinConnectivityHelper
							.allowConnectionWithPin(placementPin, connectivityPinList, toMatePin.getConnectivity())) {
						return PinPlacementConstraintsHolder.PLACEMENT_NO;
					}
				}
			}
			return allow;
		}

		@Nullable protected PinPlacementConstraintsHolder getAnchorPinPlacementConstraints()
		{
			return m_constraints;
		}
	}
}
