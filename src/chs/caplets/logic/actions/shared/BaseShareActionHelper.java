/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.autoshare.DeltaShareIntoOperandStrategy;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.IFunctionObject;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IInlineInterconnectJackConnector;
import chs.cof.logical.cable.IInlineInterconnectPlugConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper.MulticoreShareabilityReason;
import chs.common.INamedUIDObject;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseShareActionHelper
{

	private BaseShareActionHelper()
	{
	}

	/**
	 * Performs various tests to see if the objects in the given collection can be either shared or unshared. This
	 * function will only return a operands that is guarrentteed to be either shareable or unshareable.
	 *
	 * @param uidObjects Collection of UIDObjects.
	 *
	 * @return BaseShareActionOperands containing actionable UIDObject and it's mate pinlist if applicable.
	 */
	@NotNull public static BaseShareActionOperands getOperands(@NotNull Collection<IUIDObject> uidObjects)
	{
		final BaseShareActionOperands operands = new BaseShareActionOperands();
		final OperandShareabilityStatus operandShareabilityStatus = buildOperand(uidObjects, operands);
		operands.setShareabilityStatus(operandShareabilityStatus);
		return operands;
	}

	@NotNull private static OperandShareabilityStatus buildOperand(@NotNull Collection<IUIDObject> uidObjects,
			@NotNull BaseShareActionOperands operands)
	{
		ILogicObject logicObject = null;
		IInlinePlugConnector logicMate = null;
		Set<IRepresentedObject> reps = new HashSet<IRepresentedObject>();
		Set<IPinList> mates = new HashSet<IPinList>();
		for (IUIDObject uidObj : uidObjects) {
			ILogicObject lobj = ReferenceHelper.reduceToLogicObject(uidObj);
			if (lobj instanceof IConnector && ((IConnector) lobj).getOccupiedPosition() != null) {
				lobj = ((IConnector) lobj).getTopLevelConnector();
			}

			if (lobj instanceof IFunctionObject && !isSharableObject((IFunctionObject) lobj)) {
				return OperandShareabilityStatus.NonShareable;
			}
			if (!isIndependentlyShareableLogicObject(lobj)) {
				return OperandShareabilityStatus.NonShareable;
			}

			// treat shield body as multicore from here
			if (lobj instanceof chs.cof.logical.cable.IShieldBody) {
				lobj = ((chs.cof.logical.cable.IShieldBody) lobj).getMulticore();
			}

			// don't allow different logic objects to be selected (other than inline halves)
			if (lobj instanceof IInlinePlugConnector) {
				// inline halves must be selected together for sharing (dont ask me why)
				// plug connector is always considered to be the mate object of the jack
				IInlinePlugConnector plug = (IInlinePlugConnector) lobj;
				if (logicMate != null && logicMate != plug) {
					return OperandShareabilityStatus.WrongSelection;
				}
				else {
					logicMate = plug;
				}
			}
			else {
				// other than for inline halves, selections of schems must have the same logic object or non
				if (lobj != null && logicObject != null && lobj != logicObject) {
					return OperandShareabilityStatus.WrongSelection;
				}
				else {
					//if condition added to fix -dts0100797151 Select paradigm inconsistency when attempting to share an inline connector pair
					if (lobj != null) {
						logicObject = lobj;
					}
				}
			}

			if (uidObj instanceof IPinList) {
				IPinList pinList = (IPinList) uidObj;
				if (lobj instanceof IInlinePlugConnector) {
					mates.add(pinList);
					continue;
				}
				else if (lobj instanceof chs.cof.logical.cable.IPinList) {
					if (lobj instanceof IConnector &&
							(((IConnector) lobj).getOccupiedPosition() != null ||
									((IConnector) lobj).getNumPosition() != 0)) {
						continue;
					}
					reps.add(pinList);
					continue;
				}
			}
			else if (uidObj instanceof IMulticore) {
				if (operands.target == null) {
					operands.target = uidObj;
					continue;
				}
			}
			else if (uidObj instanceof IShieldBody) {
				IShieldBody sb = (IShieldBody) uidObj;
				chs.cof.logical.cable.IShieldBody logicalShieldBody = sb.getConnectivity();
				if (logicalShieldBody == null) {
					return OperandShareabilityStatus.ErroneousObject;
				}
				IMulticore mc = logicalShieldBody.getMulticore();
				if (mc != null && (operands.target == null || operands.target == mc)) {
					operands.target = mc;
					continue;
				}
			}
			else if (uidObj instanceof IConductor || uidObj instanceof IHighwaySchematic) {
				IRepresentedObject repObject = (IRepresentedObject) uidObj;
				reps.add(repObject);
				// all conductors now ported, we simply use the connectivity as the target
				operands.target = repObject.getRawConnectivity();
				continue;
			}
			else if (uidObj instanceof ILogicSegment) {
				continue; // schem conductors are included in the selection if a single segment is
			}
			else if (uidObj instanceof IDiagramText) {
				continue; // Safely ignore diagram text - could be invisible xref text... grrr
			}
			else if (uidObj instanceof IPort) {
				continue; // Safely ignore port of the conductor
			}
			else if (uidObj instanceof chs.cof.logical.cable.IConductor ||
					uidObj instanceof chs.cof.logical.cable.IPinList ||
					uidObj instanceof chs.cof.logical.cable.IShieldBody ||
					uidObj instanceof IHighway) {
				// these connectivity can now be in the browser selection
				// ignore them for now so that Share will continue to work from the browser
				continue;
			}
			return OperandShareabilityStatus.NonShareable; // anything else with connectivity in the selection (e.g. Pin) ==> not currently allowed
		}

		// make sure we have all instances of a logic object (and it's mate) selected
		if (!setupOperands(logicObject, logicMate, reps, mates, operands)) {
			return OperandShareabilityStatus.ErroneousObject;
		}

		// currently we can't have shared objects in an assembly - so this is valid for shared & unshared:
		if (logicObject.getAssembly() != null) {
			return OperandShareabilityStatus.AssembliedObject;
		}
		if (logicMate != null && logicMate.getAssembly() != null) {
			// corrupt state to have half an inline in assembly? - probably wan't get coverage here
			return OperandShareabilityStatus.AssembliedObject;
		}

		// Now the gating checks.
		if (logicObject instanceof chs.cof.logical.cable.IPinList) {
			chs.cof.logical.cable.IPinList cpl = (chs.cof.logical.cable.IPinList) logicObject;
			// Ambiguous or inconsistent selection?
			if ((cpl instanceof IInlineJackConnector && logicMate == null)
					|| (!(cpl instanceof IInlineJackConnector) && logicMate != null)
					|| (logicMate != null && logicMate.getMate() != cpl)) {
				return OperandShareabilityStatus.WrongSelection; // assert?
			}
			if (logicObject instanceof IConnector && ((IConnector) logicObject).hasAssemblyAssociation()) {
				return OperandShareabilityStatus.AssembliedObject;
			}
			if (logicMate != null && logicMate.hasAssemblyAssociation()) {
				return OperandShareabilityStatus.AssembliedObject;
			}
		}
		else if (logicObject instanceof IMulticore) {
			IMulticore mc = (IMulticore) logicObject;
			return getMulticoreShareabilityStatus(mc);
		}
		else if (logicObject instanceof chs.cof.logical.cable.IConductor) {
			chs.cof.logical.cable.IConductor connectivityConductor = (chs.cof.logical.cable.IConductor) logicObject;
			if (ShareConcurrencyHelper.isConductorNonShareable(connectivityConductor)) {
				return OperandShareabilityStatus.NonShareable;
			}
		}
		else if (operands.getLogicObject() != null) {
			// explicitly specified logic connectivity object ==> action based on all (possibly multiple or none) reps
			// actually, the Share action is the same for any selection of only the logic object
		}
		else {
			// Empty selection?
			return OperandShareabilityStatus.NonShareable;
		}
		// Made it through the minefield...
		return OperandShareabilityStatus.Shareable;
	}

	@NotNull private static OperandShareabilityStatus getMulticoreShareabilityStatus(@NotNull IMulticore mc)
	{
		final MulticoreShareabilityReason multicoreShareability = ShareConcurrencyHelper.getMulticoreShareability(mc);
		if (multicoreShareability == MulticoreShareabilityReason.PartialPlacedLibrariedMulticore) {
			return OperandShareabilityStatus.PartialPlacedMulticore;
		}
		if (multicoreShareability == MulticoreShareabilityReason.AssembliedOverbraid) {
			return OperandShareabilityStatus.AssembliedObject;
		}
		if (multicoreShareability == MulticoreShareabilityReason.EmptyMulticore) {
			return OperandShareabilityStatus.EmptyMulticore;
		}
		if (multicoreShareability != MulticoreShareabilityReason.Shareable) {
			return OperandShareabilityStatus.ErroneousObject;
		}
		return OperandShareabilityStatus.Shareable;
	}

	private static boolean isSharableObject(IFunctionObject lobj)
	{
		return lobj instanceof IFunctionConductor || lobj instanceof IFunction || lobj instanceof IFunctionMessage;
	}

	public static boolean isIndependentlyShareableLogicObject(ILogicObject lobj)
	{
		// IDeviceConnector inherently belongs to a Device and cannot be shared.
		// The other exclusions represent current limitations of the implementation.
		return !(lobj instanceof IBackshell
				|| lobj instanceof IDeviceConnector
				|| lobj instanceof IInlineInterconnectJackConnector
				|| lobj instanceof IInlineInterconnectPlugConnector
				|| lobj instanceof IInterconnectConductor
				|| lobj instanceof IBlockDevice);
	}

	/**
	 * Attempt to setup the share operands bases on logic and schematic objects identified from the selection
	 *
	 * @param logicObject The logic object that should be common to all representations found in the selection
	 * @param logicMate The logic mate is only used for inline jacks.  Null in all other cases
	 * @param reps The representations of the logic object.  Possibly non or multiple representations
	 * @param mates The reprentations of the mate object.  Possibly non or multiple representations
	 * @param operands The share operands, used for the share-based actions - may be modified by this method
	 *
	 * @return true if the operands were setup for a valid share-based action
	 */
	private static boolean setupOperands(ILogicObject logicObject, @Nullable ILogicObject logicMate,
			Set<IRepresentedObject> reps, Set<IPinList> mates,
			BaseShareActionOperands operands)
	{
		// logicObject must always be specified
		if (logicObject == null) {
			return false;
		}

		// logic object and mate must always be specified for inlines
		if (logicObject instanceof IGenericInlineConnector ^ logicMate instanceof IGenericInlineConnector) {
			return false;
		}

		if (logicMate instanceof IGenericInlineConnector) {
			IGenericInlineConnector inline = (IGenericInlineConnector) logicMate;
			if (inline.getMate() != logicObject) {
				return false;
			}

			// for some reason we must select both halves of any inline
			// quick check for now - it probably doesn't matter if it gets past this anyway
			if (reps.size() != mates.size()) {
				return false;
			}

			//dts0100708899 we will disable the share action on inlines which have un-equal no. of pins.
			IGenericInlineConnector inlineMate = (IGenericInlineConnector) logicObject;
			if (inline.getPins().getSize() != inlineMate.getPins().getSize()) {
				return false;
			}
		}

		// that's good enough - Share now works the same for 0, 1 or multiple representations
		// i.e. the connectivity object gets shared and all schems are updated
		operands.setLogicObject(logicObject);
		operands.setRepresentations(reps);

		// if a single representation was specified, also use the old-style share operand
		// not even sure if this is needed any more - shouldn't matter for Share at least
		if (reps.size() == 1) {
			operands.target = (IUIDObject) reps.iterator().next();
		}
		if (mates.size() == 1) {
			operands.mate = mates.iterator().next();
		}
		// else don't worry about setting the mate, we'll get it from the logic object if we need it

		return true;
	}

	@NotNull public static BaseShareActionOperands getShareOperands(@NotNull List<IUIDObject> uidObjects, @NotNull
			IShareOperandStrategy operandStrategy)
	{
		final BaseShareActionOperands operands = getOperands(uidObjects);
		if (!operandStrategy.isShareable(operands.getShareabilityStatus())) {
			return operands;
		}
		final OperandShareabilityStatus shareabilityStatus = operandStrategy instanceof DeltaShareIntoOperandStrategy?
				evaluateDeltaShareOperand(operands) : evaluateShareOperand(operands);
		operands.setShareabilityStatus(shareabilityStatus);
		return operands;
	}
	@NotNull private static OperandShareabilityStatus evaluateDeltaShareOperand(@NotNull BaseShareActionOperands operands)
	{
		return evaluateShareOperand(operands);
	}
	@NotNull private static OperandShareabilityStatus evaluateShareOperand(@NotNull BaseShareActionOperands operands)
	{
		if (operands.target instanceof IPinList) {
			IPinList pinList = (IPinList) operands.target;
			// Already shared?
			chs.cof.logical.cable.IPinList cpl = pinList.getConnectivity();
			if (cpl.getSharedPinList() != null) {
				return OperandShareabilityStatus.NonShareable;
			}
		}
		else if (operands.target instanceof IMulticore) {
			IMulticore mc = (IMulticore) operands.target;
			// Already shared?
			if (mc.getSharedMulticore() != null) {
				return OperandShareabilityStatus.NonShareable;
			}
			else if (mc.getSuperordinate() instanceof IAssembly) {
				return OperandShareabilityStatus.AssembliedObject;
			}
		}
		else if (operands.target instanceof IConductor) {
			IConductor conductor = (IConductor) operands.target;
			chs.cof.logical.cable.IConductor connectivityConductor = conductor.getConnectivity();
			if (connectivityConductor.getSharedConductor() != null) {
				return OperandShareabilityStatus.NonShareable;
			}
			if (!conductor.getConnectivity().isShareable()) {
				return OperandShareabilityStatus.NonShareable;
			}
		}
		else if (operands.getLogicObject() != null) {
			// explicitly specified logic connectivity object ==> action based on all (possibly multiple or none) reps
			ILogicObject logicObject = operands.getLogicObject();
			assert logicObject != null;
			if (logicObject.isShared()) {
				return OperandShareabilityStatus.NonShareable;
			}
			if (logicObject instanceof chs.cof.logical.cable.IConductor) {
				// can't directly share conductors in a multicore - must select the multicore
				chs.cof.logical.cable.IConductor cond = (chs.cof.logical.cable.IConductor) logicObject;
				if (cond.getMulticore() != null) {
					return OperandShareabilityStatus.NonShareable;
				}
				// If the conductor is not shareable (maybe a shield) we can't share...
				if (!cond.isShareable()) {
					return OperandShareabilityStatus.NonShareable;
				}
			}
		}
		else if (operands.getLogicObject() instanceof IHighway) {
			IHighway highway = (IHighway) operands.getLogicObject();
			if (highway.isShared()) {
				return OperandShareabilityStatus.NonShareable;
			}
		}
		else {
			// Empty selection?
			return OperandShareabilityStatus.NonShareable;
		}
		return OperandShareabilityStatus.Shareable;
	}

	public static boolean shouldDisableShareForUnplaced(@Nullable BaseShareActionOperands operands,
			@NotNull StringBuilder disableReason,
			boolean isShareOfUnplacedObjectsDisabled, boolean allowShareOfPlacedPinListWithUnplacedPins)
	{
		//dts0100809894: if we have a shared conductor part of a shared multicore which has a special usage
		//such that multicore is used in the design but not the shared conductor because this shared cond
		//has no diagram usage and the design is also not save yet. in this case we shouldn't be able to delete
		//the shared conductor otherwise there will be left a cable conductor whose shared conductor is deleted.
		//The above mentioned issue is on all logic objects. we can face serious issues because we will be able
		//to delete shared objects if there are no representations in saved state or no reference to the shared
		//object in design connectivity. so we will be disabling share operation which could potentially
		//lead to such scenarions. chandras
		ILogicObject logicObject = operands != null ? operands.getLogicObject() : null;
		return logicObject != null && UnplacedObjectShareabilityControl.shouldDisableShareIfUnplaced(logicObject,
				disableReason, isShareOfUnplacedObjectsDisabled, allowShareOfPlacedPinListWithUnplacedPins);
	}

	public static OperandShareabilityStatus getUnplacedObjectShareabilityStatus(
			@Nullable BaseShareActionOperands operands,
			boolean isShareOfUnplacedObjectsDisabled, boolean allowShareOfPlacedPinListWithUnplacedPins)
	{
		StringBuilder disableReason = new StringBuilder();
		final boolean disableShare =
				shouldDisableShareForUnplaced(operands, disableReason, isShareOfUnplacedObjectsDisabled,
						allowShareOfPlacedPinListWithUnplacedPins);
		if (!disableShare) {
			return OperandShareabilityStatus.Shareable;
		}
		if (operands.getLogicObject() instanceof IMulticore) {
			return OperandShareabilityStatus.PartialPlacedMulticore;
		}
		return OperandShareabilityStatus.Unplaced;
	}

	public static boolean isFrozenSharedObjectsRequired(@NotNull ILogicDesign design)
	{
		return design.getReleaseLevel().isFrozenSharedObjectsRequired();
	}

	@Nullable public static Pair<INamedUIDObject, IShareActionHelper> determineActionHelper(
			@NotNull BaseShareActionOperands operand,
			@Nullable IShareActionHelper pinListHelper, @Nullable IShareActionHelper condGroupHelper,
			@Nullable IShareActionHelper conductorHelper, @Nullable IShareActionHelper highwayHelper,
			@Nullable IShareActionHelper singleLineHelper,
			@Nullable IShareActionHelper functionMessageHelper, @Nullable IShareActionHelper functionConductorHelper)
	{
		//TODO: Should we update target instead of using logic object
		if (operand.getLogicObject() != null && operand.getLogicObject() instanceof IFunctionMessage) {
			IFunctionMessage functionMessage = (IFunctionMessage) operand.getLogicObject();
			return new Pair<>(functionMessage, functionMessageHelper);
		}
		if (operand.getLogicObject() != null && operand.getLogicObject() instanceof IFunctionConductor) {
			IFunctionConductor signal = (IFunctionConductor) operand.getLogicObject();
			return new Pair<>(signal, functionConductorHelper);
		}
		return determineActionHelper(operand, pinListHelper, condGroupHelper,
				conductorHelper, highwayHelper, singleLineHelper);
	}

	@Nullable public static Pair<INamedUIDObject, IShareActionHelper> determineActionHelper(
			@NotNull BaseShareActionOperands operand,
			@Nullable IShareActionHelper pinListHelper, @Nullable IShareActionHelper condGroupHelper,
			@Nullable IShareActionHelper conductorHelper, @Nullable IShareActionHelper highwayHelper,
			@Nullable IShareActionHelper singleLineHelper)
	{
		if (operand.target instanceof IPinList) {
			IPinList pinList = (IPinList) operand.target;
			return new Pair<>(pinList.getConnectivity(), pinListHelper);
		}
		if (operand.target instanceof IMulticore) {
			IMulticore multicore = (IMulticore) operand.target;
			return new Pair<>(multicore, condGroupHelper);
		}
		if (operand.target instanceof IConductor) {
			IConductor conductor = (IConductor) operand.target;
			return new Pair<>(conductor.getConnectivity(), conductorHelper);
		}
		if (operand.target instanceof IHighwaySchematic) {
			IHighwaySchematic highwaySchematic = (IHighwaySchematic) operand.target;
			IHighway highway = highwaySchematic.getConnectivity();
			if (SingleLineHelper.isSingleLine(highway)) {
				return new Pair<>(highway, singleLineHelper);
			}
			return new Pair<>(highway, highwayHelper);
		}
		if (operand.getLogicObject() != null) {
			ILogicObject logicObject = operand.getLogicObject();
			if (logicObject != null) {
				IShareActionHelper helper = null;
				if (logicObject instanceof chs.cof.logical.cable.IPinList) {
					helper = pinListHelper;
				}
				else if (logicObject instanceof chs.cof.logical.cable.IConductor) {
					helper = conductorHelper;
					operand.target = logicObject; // reuse the old functionality for sharing ports
				}
				else if (logicObject instanceof IMulticore) {
					helper = condGroupHelper;
					operand.target = logicObject;
				}
				else if (logicObject instanceof ISingleLine) {
					helper = singleLineHelper;
					operand.target = logicObject;
				}
				else if (logicObject instanceof IGeneralHighway) {
					helper = highwayHelper;
					operand.target = logicObject;
				}
				return new Pair<>(logicObject, helper);
			}
		}

		return null;
	}

	@NotNull public static String getNewlySharedObjectName(@NotNull String sharedObjName,@NotNull String sharedObjectUID)
	{
		//
		// Connectivity will be null if composite -> use the name of the shared object
		//
		ILogicObject newSharedObjConnectivityObject = null;
		if (!sharedObjectUID.isEmpty()) {
			newSharedObjConnectivityObject = CommonUtils
					.cast(UIDMgr.getObject(FactoryMgr.getCommonFactory().constructUID(sharedObjectUID)),
							ILogicObject.class);
		}
		String name = sharedObjName;
		if (newSharedObjConnectivityObject == null) {
			return name;
		}
		IRevisionedSharedObject revSO = null;
		if (newSharedObjConnectivityObject instanceof chs.cof.logical.cable.IPinList) {
			revSO = ((chs.cof.logical.cable.IPinList) newSharedObjConnectivityObject).getSharedPinList();
		}
		else if (newSharedObjConnectivityObject instanceof IMulticore) {
			revSO = ((IMulticore) newSharedObjConnectivityObject).getSharedMulticore();
		}
		else if (newSharedObjConnectivityObject instanceof chs.cof.logical.cable.IConductor) {
			revSO = ((chs.cof.logical.cable.IConductor) newSharedObjConnectivityObject).getSharedConductor();
		}
		else if (newSharedObjConnectivityObject instanceof ISingleLine singleLine) {
			revSO = singleLine.getSharedSingleLine();
		}
		if (revSO != null) {
			name = revSO.getFullName();
		}
		return name;
	}
}
