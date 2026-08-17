/*
 * Copyright 2006-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.COFTypeEnum;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.PortHelper;
import chs.utility.Replicator;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.FunctionConductorUpdateHelper;
import chs.utility.helpers.revisioning.CreateCloneOfSharedFunctionMessages;
import chs.utility.helpers.revisioning.CreateCloneOfSharedFunctionsSignals;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

/**
 * Created by IntelliJ IDEA. User: jamesmw Date: 24-Nov-2006 Time: 14:23:17 To change this template use File | Settings
 * | File Templates.
 */
public class UnshareConductorActionHelper extends UnshareSegmentContainerActionHelper implements IShareActionHelper
{

	private chs.cof.logical.cable.IConductor cableConductor;
	private Collection<IConductor> schemConductors;
	private IUID sharedCableConductorUid;

	public UnshareConductorActionHelper(IDesign theDesign)
	{
		super(theDesign);
		m_design = theDesign;
	}

	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		cableConductor = operands.getCableConductor();
		if (cableConductor == null) {
			return IActionEnum.eCanceled;
		}

		sharedCableConductorUid = cableConductor.getSharedObjectUID();
		schemConductors = operands.getConductorRepresentations();

		// dts0100582131 - It is possible to create wires with > 2 terminations
		// just disallow unshare would result in Wire with >2 pins
		// note that we must check the selected schematic wires, not just the connectivity wire
		if (cableConductor instanceof IWireConductor && cableConductor.getNumPins() > 2) {
			// count the number of different pins connected to selected schem conductors
			Set<IAbstractPin> schemConnected = new HashSet<IAbstractPin>();
			for (IConductor schemCond : schemConductors) {
				for (IPin pin : schemCond.getPins()) {
					schemConnected.add(pin.getConnectivity());
				}
				if (schemConnected.size() > 2) {
					break;
				}
			}

			if (schemConnected.size() > 2) {
				Class<UnshareConductorActionHelper> cls = UnshareConductorActionHelper.class;
				String heading = ResourceMgr.getString(cls, "UnshareConductorActionHelper.CannotUnshare",
						cableConductor.getName());
				String error =
						ResourceMgr.getString(cls, "UnshareConductorActionHelper.WireHasMoreThanTwoTerminations");
				showErrorPrompt(heading, error);
				return IActionEnum.eCanceled;
			}
		}

		ILogicDesign design = cableConductor.getLogicDesign();
		assert design != null;
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		int usageCount = dwum.getDesignSharedUsageCount(cableConductor);
		boolean bEnableAllInstances = true;
		boolean bValue = true;
		Class<UnshareConductorActionHelper> cls = UnshareConductorActionHelper.class;
		String strToolTip = ResourceMgr.getString(cls, "UnshareConductorActionHelper.UnshareAllInstances",
				COFTypeEnum.from_object(cableConductor).toString().toLowerCase());
		if (usageCount == 1 || usageCount == schemConductors.size()) {
			bEnableAllInstances = false;
			bValue = true;
		}
		if (cableConductor instanceof IWireConductor && cableConductor.getNumPins() > 2) {
			bEnableAllInstances = false;
			bValue = false;
			strToolTip = ResourceMgr.getString(cls, "UnshareConductorActionHelper.WireHasMoreThanTwoTerminations");
		}

		if (!promptRenameLocalObject(cableConductor, bEnableAllInstances, bValue, strToolTip)) {
			return IActionEnum.eCanceled;
		}

		return IActionEnum.eCompleted;
	}

	protected void showErrorPrompt(String heading, String error)
	{
		MessageHelper.showErrorMessage(CAFUtils.getInstance().getDialogFrame(), heading, error);
	}

	public boolean doEdit()
	{
		return unshareConductors(cableConductor, schemConductors, m_newName, shortDescription,
				shouldIncludeAllInstances(), super.getM_nameTemplate(), super.getM_signalMesssageTemplate());
	}

	protected boolean shouldIncludeAllInstances()
	{
		return m_bIncludeAllInstances;
	}

	/**
	 * Unshares schematic conductor instances.  Either breaks links to the shared object in the connectivity, or if
	 * there is another isntance of this shared object in the design, add a new connectivity object and make the
	 * existing schem object point to that
	 * <p>
	 * Current functionality is limited to work only when a single instance is selected (as was previously the case) or
	 * when all are (new functionality for unsharing all instances).
	 *
	 * @param conductor                The connectivity conductor to be unshared
	 * @param schemConds The schematic representations of the conductor to be unshared, could be 0,1 or multiple
	 * @param newName                  The name for the new conductor - may be null if the default name will suffice
	 * @param shortDesc The short description for the new conductor - may be null if the default name will suffice
	 * @param m_nameTemplate           The nameTemplate for signal and messages
	 * @param m_signalMesssageTemplate The nameTemplate for associated message of a signal
	 * @return success
	 */
	public static boolean unshareConductors(chs.cof.logical.cable.IConductor conductor,
			Collection<IConductor> schemConds, String newName, String shortDesc, boolean bIncludeAllInstances,
			@Nullable INameTemplate m_nameTemplate,
			@Nullable INameTemplate m_signalMesssageTemplate)
	{
		ILogicDesign design = conductor.getLogicDesign();
		assert design != null;

		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		Replicator replicator = new Replicator(Replicator.COPY);

		// If all usages are getting unshared, then make the connectivity local
		// If not, then we can only cope with single schem selection and we create a new connectivity in that case
		ISharedConductor sharedCond = conductor.getSharedConductor();
		boolean allUnshared = bIncludeAllInstances || (dwum.getDesignSharedUsageCount(conductor) == schemConds.size());

		String key = allUnshared ? "UnshareConductorActionHelper.UnshareAllInstancesFailureInMU.Message.text" :
				"UnshareConductorActionHelper.UnshareFailureInMU.Message.text";
		String failureMsg = ResourceMgr.getString(UnshareConductorActionHelper.class, key);
		if (!ShareConcurrencyHelper.attemptLockToUnshare(conductor, schemConds, allUnshared, design, failureMsg)) {
			return false;
		}

		if (allUnshared) {
			List<IDesignSharedUsage> usages = dwum.getUsages(conductor);
			// we will be using this connectivity for the unshared conductors, just set the shared to null
			conductor.setSharedConductor(null);
			if (conductor instanceof IFunctionMessage) {
				unshareAllSignalsInsideThisMessage((IFunctionMessage) conductor, replicator);
			}
			// to regenerate new usages properly in usage manager, all diagrams where the conductor is used must be loaded(fix for dts0100898788) 
			loadDiagramsOftheUsges(usages);

			// transfer of properties etc must be done *after* nulling the shared ref
			replicator.replicateCopyableObject(sharedCond, conductor);

			if (m_nameTemplate != null &&
					(conductor instanceof IFunctionMessage || conductor instanceof IFunctionConductor)) {
				updateMessageAndSignals(conductor, m_nameTemplate, m_signalMesssageTemplate);
			}
		}
		else {
			if (schemConds.size() > 1) {
				// limitation - we can't handle this case yet
				assert false : "Unshare action should not be enabled for this case";
				return false;
			}
		}

		chs.cof.logical.cable.IConductor unsharedCond = conductor;
		for (IConductor schemConductor : schemConds) {

			if (allUnshared) {
				//(SP1206)-dts0100877321 Unshare shared wire and make it design wide causes the xref not to be displayed
				// Unassign ported m_conductor will not replicate the connectivity, since we are re-using it.
				// It will remove the m_design usage
				if (dwum.getDesignSharedUsageCount(conductor) == 1) {
					PortHelper.unassignPortedConductor(schemConductor, false);
					schemConductor.setHome(true);
				}

				// update all proptexts to refer to any new ones put on the connectivity by the replicator
				PropertyHelper.updatePropertyTexts(schemConductor, conductor);
			}
			else {
				// if we're in this branch, it should only be for selection of a single instance
				assert schemConds.size() == 1;
				unsharedCond =
						createNewConductorAndResetConnections(conductor, design, dwum, sharedCond.getName(),
								schemConductor);
				if (m_nameTemplate != null &&
						(conductor instanceof IFunctionMessage || conductor instanceof IFunctionConductor)) {
					updateMessageAndSignals(unsharedCond, m_nameTemplate, m_signalMesssageTemplate);
				}
			}
		}

		if (newName != null) {
			unsharedCond.setName(newName);
		}

		if (m_nameTemplate != null) {
			unsharedCond.setShortDescription(m_nameTemplate.getShortDescription());
		}
		else if (shortDesc != null) {
			unsharedCond.setShortDescription(shortDesc);
		}

		return true;
	}

	public static void updateMessageAndSignals(chs.cof.logical.cable.IConductor conductor,
			@NotNull INameTemplate m_nameTemplate,
			@Nullable INameTemplate m_signalMesssageTemplate)
	{
		if (conductor instanceof IFunctionMessage &&
				!CreateCloneOfSharedFunctionMessages.isTheNewDictMessageSameAsBefore(conductor, m_nameTemplate)) {
			FunctionConductorUpdateHelper
					.updateMsgAccordingToOTI((IFunctionMessage) conductor, m_nameTemplate);
		}
		if (conductor instanceof IFunctionConductor && !CreateCloneOfSharedFunctionsSignals
				.isTheNewDictSignalSameAsBefore(conductor, m_nameTemplate, m_signalMesssageTemplate)) {
			FunctionConductorUpdateHelper
					.updateSignalAccordingToOTI((IFunctionConductor) conductor, m_nameTemplate,
							m_signalMesssageTemplate);
		}
	}

	private static void unshareAllSignalsInsideThisMessage(IFunctionMessage conductor,
			Replicator replicator)
	{
		conductor.getActiveSignals().stream().forEach(signal -> unshareSignal(replicator, signal));
	}

	private static void unshareSignal(Replicator replicator, IFunctionConductor signal)
	{
		ISharedConductor sharedConductor = signal.getSharedConductor();
		signal.setSharedConductor(null);
		replicator.replicateCopyableObject(sharedConductor, signal);
		signal.setName(sharedConductor.getName());
	}

	@NotNull public static chs.cof.logical.cable.IConductor createNewConductorAndResetConnections(
			chs.cof.logical.cable.IConductor conductor, ILogicDesign design,
			IDesignWideUsageMgr dwum, String name, IConductor schemConductor)
	{
		// If there are multiple instances of this shared object then we need to leave the connectivity where it
		// is (i.e. refering back to the shared obj) and replicate connectivity so we have a local one.
		// Happily this unassign function does all that for us.  It's wonderful like that.
		PortHelper.unassignPortedConductor(schemConductor, true, true);
		chs.cof.logical.cable.IConductor unsharedCond = schemConductor.getConnectivity();
		assert unsharedCond != conductor;
		schemConductor.setHome(true);
		// replicate attribs, properties etc from the shared to the connectivity conductor
		//Below line commented to Fix - dts0100608835 - CH generated while saving a diagram after sharing and unsharing wires using "Use existing" option
		//replicator.replicateCopyableObject(sharedCond, unsharedCond);

		// for some unknown reason the replicator doesn't set the name for us
		// if the user chose something else it will be set below
		unsharedCond.setName(name);

		// The new connectivity object only connects to the pins it is physically connected to.
		resetConnectedPins(conductor, design, dwum, unsharedCond, schemConductor);

		// FEAT13672 - Highways: Now add the new unshared conductor connectivity to the Highway schems
		// this conductor schematic is connected too
		resetHighwayConnections(conductor, unsharedCond, schemConductor);
		return unsharedCond;
	}

	/**
	 * Loads diagrams of the usages if not already loaded
	 *
	 * @param usages usages whose diagrams to loaded
	 */
	private static void loadDiagramsOftheUsges(List<IDesignSharedUsage> usages)
	{
		for (IDesignSharedUsage usage : usages) {
			ISchemDiagram diagram = usage.getDiagram();
			if (diagram != null) {
				diagram.loadToMemory();
			}
		}
	}

	private static void resetHighwayConnections(chs.cof.logical.cable.IConductor conductor,
			chs.cof.logical.cable.IConductor unsharedCond, IConductor schemConductor)
	{
		if (unsharedCond instanceof IHighwayConductor) {
			Set<IHighwaySchematic> highwaySchems = HighwayHelper.getSchematicHighways(schemConductor);
			for (IHighwaySchematic highwaySchem : highwaySchems) {
				Set<IHighwaySchematic> excludedHWs = Collections.emptySet();
				Set<IConductor> excludedSchemConds = new HashSet<IConductor>();
				excludedSchemConds.add(schemConductor);

				IGeneralHighway cableHighway = HighwayHelper.toGeneralHighway(highwaySchem);
				if (cableHighway != null) {
					if (!HighwayHelper.hasOtherHighwayConnection(conductor, cableHighway, excludedSchemConds,
							excludedHWs)) {
						cableHighway.removeConductor((IHighwayConductor) conductor);
					}
					cableHighway.addConductor((IHighwayConductor) unsharedCond);
				}
			}
		}
	}

	private static void resetConnectedPins(chs.cof.logical.cable.IConductor conductor, ILogicDesign design,
			IDesignWideUsageMgr dwum, chs.cof.logical.cable.IConductor unsharedCond, IConductor schemConductor)
	{
		unsharedCond.removeAllPins();
		for (Object obj : schemConductor.getPins()) {
			IPin pin = (IPin) obj;
			unsharedCond.addPin(pin.getConnectivity());
		}

		// The existing connectivity conductor needs to update itself so it only refers to the pins it is physically
		// connected to
		Set<IAbstractPin> allPins = new HashSet<IAbstractPin>();
		for (IDesignSharedUsage usage : dwum.getUsages(conductor)) {
			IBaseShareableDiagramObject diagramObj = usage.getDiagramObject();
			if (diagramObj == null) {
				// If we don't have the schem object, force a fully reload for this diagram... we really need it.
				ISchemDiagram diag = design.getDiagram(usage.getDiagramUID());
				assert diag != null;
				diag.getBackground();
				diagramObj = usage.getDiagramObject();
				assert diagramObj != null;
			}
			if (diagramObj instanceof IConductor) {
				IConductor otherSchemCond = (IConductor) diagramObj;
				if (otherSchemCond != schemConductor) {
					for (Object pinObj : otherSchemCond.getPins()) {
						allPins.add(((IPin) pinObj).getConnectivity());
					}
				}
			}
			else if (diagramObj instanceof IHighwaySchematic) {
				IHighwaySchematic hwSchem = (IHighwaySchematic) diagramObj;
				for (IUID stackPinUID : hwSchem.getConnectedStackPins()) {
					ISchemStackPin pinStack = UIDMgr.getObjectOfType(stackPinUID, ISchemStackPin.class);
					assert pinStack != null;
					allPins.addAll(pinStack.getConnectedPins(conductor));
				}
			}
		}

		//To FIX :dts0100603694 Call home generated while adding a shared same net wire at center strip splice
		//dts0100604647 Call home generated while unsharing another instance of center stripped wire
		conductor.removeAllPins();
		Set<ISplice> centreStripSplices = Collections.emptySet();
		if (conductor instanceof IWireConductor) {
			IWireConductor iWireConductor = (IWireConductor) conductor;
			centreStripSplices = iWireConductor.getCenterStripSplicesAsSet();
		}
		for (IAbstractPin pin : allPins) {
			if (!centreStripSplices.isEmpty()) {
				// DO NOT Add Center Strip Splices...
				// These are still in the 'conductor.getCenterStripSplices()' at this point and
				// should not be re-added to the pins of the IWireConductor
				IPinList owner = pin.getOwner();
				if (owner instanceof ISplice) {
					ISplice spliceOwner = (ISplice) owner;
					if (!centreStripSplices.contains(spliceOwner)) {
						conductor.addPin(pin);
					}
				}
				else {
					conductor.addPin(pin);
				}
			}
			else {
				conductor.addPin(pin);
			}
		}
	}

	public void cleanup()
	{
		cableConductor = null;
		if (schemConductors != null) {
			schemConductors.clear();
		}
		sharedCableConductorUid = null;
	}

	/**
	 * Inherrited from interface.
	 *
	 * @return Will always return false
	 */
	public boolean isNewSharedObject()
	{
		return false;
	}

	public void setSchemConductorsForUT(Collection<IConductor> conductors)
	{
		schemConductors = conductors;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return sharedCableConductorUid;
	}

	@Override public boolean isShareInto()
	{
		return false;
	}
}
