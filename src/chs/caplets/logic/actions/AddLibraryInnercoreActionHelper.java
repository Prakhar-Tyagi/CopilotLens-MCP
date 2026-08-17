/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ISpecialSelection;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.IUserSession;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.parts.ILibraryInnerCore;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.ILibraryWire;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 1, 2004 Time: 1:16:46 PM
 */
public class AddLibraryInnercoreActionHelper
{

	ISpecialSelection m_specialSelect;
	private ILogicDesign m_design;
	private IInterconnectToDoItem m_icxToDoItem;
	private ILibraryInnerCore m_innercore;
	private ILibraryWire m_libraryWire;
	private List m_ancestors;
	private IConductor m_existingConductor;

	private ISharedConductor m_sharedConductor;
	private Class m_conductorClass;

	AddLibraryInnercoreActionHelper(ISpecialSelection libSelectMgr, ILogicDesign design, Class conductorClass)
	{
		m_specialSelect = libSelectMgr;
		m_design = design;
		m_conductorClass = conductorClass;
		m_icxToDoItem = null;
		m_innercore = null;
		m_libraryWire = null;
		m_ancestors = null;
		m_existingConductor = null;
		m_sharedConductor = null;
	}

	public boolean getOperands()
	{
		m_icxToDoItem = null;
		m_innercore = null;
		m_libraryWire = null;
		m_ancestors = null;
		m_existingConductor = null;
		m_sharedConductor = null;
		if (m_specialSelect.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_specialSelect.getSelectedObjects().getNext();
			if (uidObj instanceof ILibraryInnerCore) {
				ILibraryInnerCore innercore = (ILibraryInnerCore) uidObj;
				if (innercore.getSheathType() == ILibraryInnerCore.SheathTypeEnum
						.SINGLE) { // Is the selection a conductor, i.e. not a multicore?
					// If we are dealing with a shared multicore, check to see if there is existing connectivity.
					// If there is existing connectivity, then the connectivity for the conductor's immediate parent
					// multicore also exists. If the conductor's immediate parent multicore exists, then it will
					// be the first element in the List returned by getConnectivityAncestors(). Therefore, if there is
					// existing connectivity, it will be found as a member of that shared multicore.
					List ancestors = getConnectivityAncestors(innercore);
					return prepareForInnercoreWire(innercore, ancestors);
				}
			}
			else if (uidObj instanceof IInterconnectToDoItem
					&& !IShieldConductor.class.isAssignableFrom(m_conductorClass)
					&& ((IInterconnectToDoItem) uidObj).getPartClass() == IInterconnectMember.TYPE_WIRE
					&& ((IInterconnectToDoItem) uidObj).getLibraryObject() instanceof ILibraryWire) {
				m_icxToDoItem = (IInterconnectToDoItem) uidObj;
				m_libraryWire = (ILibraryWire) m_icxToDoItem.getLibraryObject();
				return true;
			}
		}
		return false;
	}

	public boolean prepareForInnercoreWire(ILibraryInnerCore innercore, List ancestors)
	{
		if (ancestors.isEmpty()) {
			return false;
		}
		IConductor existingConductor = null;
		if (IShieldConductor.class.isAssignableFrom(m_conductorClass)
				&& !canAddShield(ancestors.get(ancestors.size() - 1))) {
			return false;
		}
		if (ancestors.get(0) instanceof IMulticore
				&& ((IMulticore) ancestors.get(0)).getSharedMulticore() != null) {
			IMulticore mc = (IMulticore) ancestors.get(0);
			ISharedMulticore smc = mc.getSharedMulticore();
			ISharedConductor scond = (ISharedConductor) SharedConductorGroupHelper
					.findSharedMemberByLibraryUID(smc, innercore.getUID());
			if (scond == null) {
				return false;
			}
			if (scond != null) {
				existingConductor = m_design.getSharedUsageMgr().getConductor(scond);
				if (existingConductor != null &&
						!m_conductorClass.isAssignableFrom(existingConductor.getClass())) {
					return false;
				}
			}
			m_sharedConductor = scond;
		}
		m_innercore = innercore;
		m_ancestors = ancestors;
		m_existingConductor = existingConductor;
		return true;
	}

	List getConnectivityAncestors(ILibraryInnerCore innercoreWire)
	{

		List ancestors = new ArrayList();
		// Get the closest connectivity multicore ancestor
		IUIDObject ancestor;

		// Create the innercore's lineage, beginning with the closest ancestor that is a real multicore, followed by
		// any unrealized innercore multicores that descend from the real multicore to the innercore conductor that will
		// be created.

		// Start with the closest ancestor (parent), and ascend, placing each new ancestor at the front of the list.
		for (ancestor = m_specialSelect.getParent(innercoreWire);
				ancestor instanceof ILibraryInnerCore;
				ancestor = m_specialSelect.getParent(ancestor)) {
			ancestors.add(0, ancestor);
		}

		if (ancestor instanceof IInterconnectToDoItem) {
			m_icxToDoItem = (IInterconnectToDoItem) ancestor;
			ancestor = m_icxToDoItem.getLibraryObject();
		}

		if (ancestor instanceof IMulticore || ancestor instanceof ILibraryMulticore) {
			ancestors.add(0, ancestor);
		}

		assert (!ancestors.isEmpty()
				&& (ancestors.get(0) instanceof IMulticore || ancestors.get(0) instanceof ILibraryMulticore));

		return ancestors;
	}

	private static boolean canAddShield(Object obj)
	{
		if (obj instanceof ILibraryMulticore) {
			return "sheath".equalsIgnoreCase(LibraryHelper.getSheathType(((ILibraryMulticore) obj)));
		}
		else if (obj instanceof ILibraryInnerCore) {
			return ((ILibraryInnerCore) obj).getSheathType() == ILibraryInnerCore.SheathTypeEnum.SHEATH;
		}
		else if (obj instanceof IMulticore) {
			IMulticore mc = (IMulticore) obj;
			if (mc.getSharedMulticore() != null) {
				return mc.getShield() == null && canAddShield(mc.getSharedMulticore());
			}
			if (mc.getLibraryObject() != null && !canAddShield(mc.getLibraryObject())) {
				return false;
			}
			if (mc.getInnercoreLibraryObject() != null && !canAddShield(mc.getInnercoreLibraryObject())) {
				return false;
			}
			return mc.getShield() == null; // Can't make connectivity if already exists.
		}
		else if (obj instanceof ISharedMulticore) {
			ISharedMulticore smc = (ISharedMulticore) obj;
			if (smc.getLibraryObject() != null && !canAddShield(smc.getLibraryObject())) {
				return false;
			}
			return smc.getShield() != null; // Can't make connectivity if not defined in shared
		}
		else {
			return false;
		}
	}

	/**
	 * If the parent is an innercore multicore that has not yet been created, create it now, and any other innercore
	 * multicores in the line of descent from the closest realized multicore.
	 *
	 * @param diagram diagram to produce the multicore on
	 *
	 * @return A parent multicore for the cable conductor to be assigned innercoreWire
	 */
	IMulticore produceMulticore(ISchemDiagram diagram)
	{

		IUIDObject topObject = (IUIDObject) m_ancestors.get(0);
		assert (topObject instanceof IMulticore || topObject instanceof ILibraryMulticore);
		IMulticore topMC;
		if (topObject instanceof ILibraryMulticore) {
			topMC = MulticoreLibraryHelper.createLibrariedMulticore(topObject, m_design, false);
			if (m_icxToDoItem != null) {
				//dont want to retain the automatically created shield in case of icxToDOItem
				IShieldConductor shield = topMC.getShield();
				if (shield != null) {
					topMC.setShield(null);
					IConnectivity conn = topMC.getConnectivity();
					if (conn != null) {
						conn.removeShieldConductor(shield);
					}
					//(SP1208) to fix logic/Multicore/DesignateShield_RegTestAS00/DesignateShield_RegTestAS00.csv
					CreationDeletionHelper.getTheCreationHelper().addDeletionObject(shield);
				}
				m_design.getInterconnectSourceInfo().addConductorDerivation(m_icxToDoItem, topMC);
				topMC.setHarness(m_design.getInterconnectSourceInfo().getHarness(m_icxToDoItem));
			}
		}
		else if (topObject instanceof IMulticore) { // When assertions are disables, this condition is not always true
			topMC = (IMulticore) topObject;
		}
		else {
			return null;
		}

		IMulticore parentMC = topMC;
		for (int i = 1; i < m_ancestors.size(); i++) {
			ILibraryInnerCore innercoreMC = (ILibraryInnerCore) m_ancestors.get(i);
			IMulticore newMC = MulticoreLibraryHelper.createLibrariedMulticore(innercoreMC, m_design, false);
			if (parentMC.getSharedMulticore() != null) {
				newMC.setSharedMulticore((ISharedMulticore) SharedConductorGroupHelper
						.findSharedMemberByLibraryUID(parentMC.getSharedMulticore(), innercoreMC.getUID()));
			}
			parentMC.addMulticore(newMC);
			parentMC = newMC;
		}
		return parentMC;
	}

	public List getAncestors()
	{
		return m_ancestors;
	}

	public IConductor getExistingConductor()
	{
		return m_existingConductor;
	}

	public IInterconnectToDoItem getIcxToDoItem()
	{
		return m_icxToDoItem;
	}

	public ILibraryInnerCore getInnercore()
	{
		return m_innercore;
	}

	public ILibraryWire getLibraryWire()
	{
		return m_libraryWire;
	}

	public ISharedConductor getSharedConductor()
	{
		return m_sharedConductor;
	}

	public IInterconnectToDoItem getToDoItem()
	{
		return m_icxToDoItem;
	}

	public static int thresholdedPlacedWireCount(IMulticore multicore, ISchemDiagram diagram, int threshold)
	{
		return thresholdedPlacedWireCount(multicore, diagram, 0, threshold);
	}

	private static int thresholdedPlacedWireCount(IMulticore multicore, ISchemDiagram diagram, int count, int threshold)
	{
		for (IConductorIterator condIt = multicore.getConductors(); condIt.hasNext(); ) {
			IConductor cond = condIt.getNext();
			count += diagram.getRepresentations(cond.getUID()).getSize();
			if (count == threshold) {
				return count;
			}
		}
		for (IMulticoreIterator mcIt = multicore.getMulticores(); mcIt.hasNext(); ) {
			IMulticore mc = mcIt.getNext();
			count = thresholdedPlacedWireCount(mc, diagram, count, threshold - count);
			if (count == threshold) {
				return count;
			}
		}
		return count;
	}

	public boolean lockNearestParentMulticore(ILogicDesign logicDesign, String outWindowPrefix,
			Consumer<String> outputMessageHandler, String objectType)
	{
		IUIDObject multicoreToBeLocked = (IUIDObject) getAncestors().get(0);

		Set<IUID> failedObjects =
				LogicObjectLockFinder.tryEdit(logicDesign, Collections.singleton(multicoreToBeLocked));
		if (!failedObjects.isEmpty()) {
			LogicConcurrencyLogger.getInstance().reportLockFailure(logicDesign, outWindowPrefix,
					failedObjects, message -> outputMessageHandler.accept(message));
			return false;
		}

		return true;
	}

	private void displayErrorMessage(Collection<IUID> lockFailedUIDs, IUIDObject multicoreToBeLocked, String objectType)
	{

		ILockInfo[] lockInfo = LogicConcurrencyLogger.getInstance().getUsersLockInfo(lockFailedUIDs);
		if (lockInfo != null && multicoreToBeLocked instanceof IMulticore) {
			ResourceBasedMessageContent content =
					new ResourceBasedMessageContent(AbstractAddLibraryWireAction.class,
							"AbstractAddLibraryWireAction.ParentIsLocked.conductor");
			content.setContextParameters(objectType);
			content.setMessageParameters(objectType.toLowerCase());
			content.setImplicationsParameters(((IReadOnlyNamedObject) multicoreToBeLocked).getName(),
					lockInfo[0].getUserName(),
					lockInfo[0].getTimeStamp());
			Message.show(PromptSeverity.WARNING, content);
		}
	}

	protected IUserSession getUserSession()
	{
		return UtilsHelper.getCHSSystem().getUserSession();
	}

	public boolean checkLibraryInnercoreAvailability(String actionDisplayName)
	{
		Object ancestor = getAncestors().get(0);
		if (ancestor instanceof IMulticore) {
			IMulticore multicore = (IMulticore) ancestor;
			for (IConductor conductor : multicore.getConductorsIncludingShields()) {
				if (conductor.getInnercoreLibraryObject() != null &&
						conductor.getInnercoreLibraryObject().getUID() == getInnercore().getUID()) {
					displayMessageForAlreadyPlacedByOtherUser(conductor, actionDisplayName);
					return false;
				}
			}
		}
		return true;
	}

	protected void displayMessageForAlreadyPlacedByOtherUser(IConductor conductor, String objectType)
	{
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(AddLibraryInnercoreActionHelper.class,
						"AddLibraryInnercoreActionHelper.libraryinnercorealreadyused");
		content.setContextParameters(objectType);
		setImplecation(conductor, content, getDiagram(conductor));
		content.setGuidanceParameters(conductor.getName());
		Message.show(PromptSeverity.WARNING, content);
	}

	private void setImplecation(IConductor conductor, ResourceBasedMessageContent content, @Nullable String diagram)
	{
		if (diagram != null) {
			content.setImplicationsParameters(conductor.getName(), diagram);
		}
		else {
			content.setImplications(AddLibraryInnercoreActionHelper.class,
					"AddLibraryInnercoreActionHelper.libraryinnercorealreadyused.implications_2", conductor.getName());
		}
	}

	@Nullable private String getDiagram(IConductor conductor)
	{
		ILogicDesign design = conductor.getLogicDesign();
		if (design != null) {
			IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
			Collection<ISchemDiagram> diagrams = designWideUsageMgr.getUsageDiagrams(conductor);
			if (!diagrams.isEmpty()) {
				return diagrams.iterator().next().getName();
			}
		}
		return null;
	}
}
