/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.AssociateConnectorAction;
import chs.caplets.logic.actions.AssociateConnectorDialog;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.IInternalPositionsContainer;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryHousingDefinition;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.NoOpCmdHelper;
import chs.cog.IPrivilegedCOGManaged;
import chs.common.IDesignAbstraction;
import chs.common.IUID;
import chs.common.UIDObjectCollection;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.ctf.editui.IInternalPositionUsageManager;
import chs.ctf.editui.logic.LogicInternalPositionUsageManager;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.AssemblyUtils;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.LogicObjectUtils;

import javax.swing.JLabel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssociateConnectorCommand extends CHSCommand
{

	private UIDObjectCollection m_validConnectors = null;
	private IInternalPosition m_position;
	private ISharedInternalPosition m_sharedposition;

	private ISchemDiagram m_diagram;
	private IConnector m_selectedConnector = null;
	private ISharedConnector m_selectedSharedConnector = null;
	private ISharedConnector m_parentSharedConnector = null;
	private COMMAND_STATUS m_cmdStatus = COMMAND_STATUS.OK;

	public enum COMMAND_STATUS
	{
		OK,
		TARGETCONNECTORS_NOTFOUND,
		SHAREDCONNECTOR_FROZEN,
		SHAREDCONNECTOR_ALREADYASSOCIATED,
		SHAREDCONNECTOR_DELETED,
		SHAREDCONNECTOR_LOCKFAILURE,
		SHAREDPOSITION_NOTAVAILABLE,
		CANNOT_BREAK_REVISION,
		SHAREDCONNECTOR_DOMAIN_ACCESS_FAILURE
	}

	public AssociateConnectorCommand(ISchemDiagram diagram, IConnector selectedPhyConnector)
	{
		super(new NoOpCmdHelper());
		m_diagram = diagram;

		m_validConnectors = new UIDObjectCollection();

		ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(selectedPhyConnector);
		if (logicObject instanceof IConnector) {
			m_selectedConnector = (IConnector) logicObject;
			m_selectedSharedConnector = (ISharedConnector) m_selectedConnector.getSharedObject();
		}
		m_cmdStatus = COMMAND_STATUS.OK;

		populateValidConnector();
	}

	public COMMAND_STATUS getCommandStatus()
	{
		return m_cmdStatus;
	}

	private void populateValidConnector()
	{
		if (m_selectedSharedConnector == null) {
			populateNonSharedConnector();
		}
		else {
			populateSharedConnector();
		}
		if (getValidConnectors().isEmpty() && m_cmdStatus == COMMAND_STATUS.OK) {
			m_cmdStatus = COMMAND_STATUS.TARGETCONNECTORS_NOTFOUND;
		}
	}

	private boolean checkAbstraction(ISharedPinList sharedPinList1, ISharedPinList sharedPinList2)
	{
		IDesignAbstraction designAbstractionSharedPL1 = sharedPinList1.getDesignAbstraction();
		IDesignAbstraction designAbstractionSharedPL2 = sharedPinList2.getDesignAbstraction();
		if (designAbstractionSharedPL1 == null && designAbstractionSharedPL2 == null) {
			return true;
		}

		if (designAbstractionSharedPL1 != null && designAbstractionSharedPL2 != null) {
			return designAbstractionSharedPL1.equals(designAbstractionSharedPL2);
		}

		return false;
	}

	public void addTargetParentConnector(IConnector childConnector, IConnector parentConnector)
	{
		ILibraryObject libObj = (ILibraryObject) parentConnector.getLibraryObject();
		if (libObj != null) {
			Set<ILibraryHousingDefinition> housingDefs = libObj.getHousingDefinitions();
			for (Object hosDef : housingDefs) {
				String partNum = ((ILibraryHousingDefinition) hosDef).getLibraryObject().getPartNumber();
				String revision = ((ILibraryHousingDefinition) hosDef).getLibraryObject().getPartRevision();
				ILibraryBaseObject childConnectorLibraryObject = childConnector.getLibraryObject();
				if (childConnectorLibraryObject instanceof ILibraryObject) {
					ILibraryObject logicObjPartDetails = (ILibraryObject) childConnector.getLibraryObject();
					String logicObjPartNumber = logicObjPartDetails.getPartNumber();
					String logicObjPartRevision = logicObjPartDetails.getPartRevision();
					//dts0101229826 ST161BashXSEESA1: Associate child connector succeeds even if incorrect revision part number is assigned to connector being associated to modular parent
					if ((partNum.equalsIgnoreCase(logicObjPartNumber) && (logicObjPartRevision==null || logicObjPartRevision.equalsIgnoreCase(revision)))
							&& (parentConnector.isPlug() == childConnector.isPlug())
							&& (compareAssembly(parentConnector, childConnector))) {
						m_validConnectors.add(parentConnector);
						break;
					}
				}
			}
		}
	}

	protected void addTargetParentSharedConnector(ISharedPinList childConnector, ISharedPinList parentConnector)
	{
		ILibraryObject libObj = (ILibraryObject) parentConnector.getLibraryObject();
		if (libObj != null) {
			Set<ILibraryHousingDefinition> housingDefs = libObj.getHousingDefinitions();
			for (Object hosDef : housingDefs) {
				String partNum = ((ILibraryHousingDefinition) hosDef).getLibraryObject().getPartNumber();
				ILibraryBaseObject childConnectorLibraryObject = childConnector.getLibraryObject();
				if (childConnectorLibraryObject instanceof ILibraryObject) {
					if (partNum.equalsIgnoreCase(((ILibraryObject) childConnectorLibraryObject).getPartNumber())
							&& (parentConnector.getType() == childConnector.getType())
							&& isValidRevisionToAdd((ISharedConnector) parentConnector)
							&& checkAbstraction(parentConnector, childConnector)) {
						m_validConnectors.add(parentConnector);
						break;
					}
				}
			}
		}
	}

	protected void populateSharedConnector()
	{
		// We can't be sure whether any child got associated to this parent in some
		// other session. Hence we have to refresh the sharedpinlistManager to
		// get the changes if any from other session.
		ISharedPinListMgr splMgr = m_diagram.getProject().getSharedPinListMgr();
		if (splMgr == null) {
			return;
		}

		splMgr.refresh();

		List<IPrivilegedCOGManaged> connectorHierarchy = new ArrayList<IPrivilegedCOGManaged>();
		connectorHierarchy.addAll(((IPrivilegedCOGManaged) m_selectedSharedConnector).siblings());

		if (m_selectedSharedConnector.isFrozen()) {
			m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_FROZEN;
			return;
		}

		if (m_selectedSharedConnector.getOccupiedPosition() != null) {
			m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_ALREADYASSOCIATED;
			return;
		}
		if(!m_selectedSharedConnector.isEditable()){
			m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_DOMAIN_ACCESS_FAILURE;
			return;
		}

		for (ISharedPinList sharedPinList : ((ISharedFullyLoadedPinListMgr)splMgr).getEditableSharedPinLists()) {
			if (!(sharedPinList instanceof ISharedConnector)) {
				continue;
			}
			ISharedConnector sharedConnector = (ISharedConnector) sharedPinList;

			if (sharedConnector.isFrozen()) {
				continue;
			}

			// We should not allow to associate connectors such that they form a chain.
			if (connectorHierarchy.contains(sharedConnector)) {
				continue;
			}

			if (sharedConnector != m_selectedSharedConnector) {
				addTargetParentSharedConnector(m_selectedSharedConnector, sharedConnector);
			}
		}
	}

	private void populateNonSharedConnector()
	{
		ILogicDesign design = m_diagram.getDesign();
		if (design == null) {
			return;
		}

		IConnectivity connectivity = design.getConnectivity();
		if (connectivity == null) {
			return;
		}

		Collection<IConnector> connectorHierarchy = m_selectedConnector.getAllConnectorsInHierarchy();
		for (IConnector connector : connectivity.getConnectors()) {
			if (!LogicObjectUtils.isValidPositionContainer(connector)) {
				continue;
			}

			if (connector.getSharedObject() != null) {
				continue;
			}

			// We should not allow to associate connectors such that they form a chain.
			if (connectorHierarchy.contains(connector)) {
				continue;
			}

			if (connector != m_selectedConnector) {
				addTargetParentConnector(m_selectedConnector, connector);
			}
		}
	}

	private boolean compareAssembly(IConnector connector1, IConnector connector2)
	{
		IAssembly connector1Assembly = connector1.getAssembly();
		IAssembly connector2Assembly = connector2.getAssembly();
		if (connector1Assembly == null && connector2Assembly == null) {
			return true;
		}
		if (connector1Assembly != null && connector2Assembly != null) {
			return connector1Assembly.equals(connector2Assembly);
		}
		return false;
	}

	private void updateRevision()
	{
		removeRevisionStructure(m_selectedSharedConnector);
		m_selectedSharedConnector.setRevision(m_parentSharedConnector.getRevision());
		m_selectedSharedConnector.setBaseId(m_selectedSharedConnector.getUID());
		m_selectedSharedConnector.setParentId(null);
	}

	private void removeRevisionStructure(IRevisionedSharedObject revisionedObject)
			throws SharedObjectRevisionHelper.SharedObjectLockException
	{

		Set<IRevisionedSharedObject> modifiedObjs = null;
		Set<IRevisionedSharedObject> lockedObjs = new HashSet<IRevisionedSharedObject>();
		try {
			modifiedObjs = SharedObjectRevisionHelper.removeFromRevisionStructure(revisionedObject, lockedObjs);
			revisionedObject.setParentId(null);
			revisionedObject.setBaseId(revisionedObject.getUID());
		}
		finally {
			if (modifiedObjs != null) {
				for (IRevisionedSharedObject revObject : modifiedObjs) {
					if (revObject instanceof ISharedConnector) {
						if (lockedObjs.contains(revObject)) {
							((ISharedConnector) revObject).saveAndUnlock();
						}
						else {
							((ISharedConnector) revObject).save();
						}
					}
				}
				lockedObjs.removeAll(modifiedObjs);
			}

			for (IRevisionedSharedObject lockedObj : lockedObjs) {
				lockedObj.unlock();
			}
		}
	}

	private boolean isPositionStillAvailable(ISharedInternalPosition selectedPosition)
	{
		IInternalPositionUsageManager m_positionUsageManager = new LogicInternalPositionUsageManager(
				m_parentSharedConnector);
		Collection<ISharedInternalPosition> positions = m_parentSharedConnector.getPositions();
		for (ISharedInternalPosition position : positions) {
			if (position.equals(selectedPosition) && !position.isOccupied() &&
					AssociateConnectorDialog
							.sharedHousingDefMatches(position, m_parentSharedConnector, m_selectedConnector)) {
				//Check if there are any blocked positions
				if (m_positionUsageManager.canAssignPositionTo(position.getName(), m_selectedConnector,
						m_parentSharedConnector.getLibraryObject())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean updateSharedConnector()
	{
		ISharedInternalPosition position = getSelectedSharedPosition();
		if (m_selectedSharedConnector == null) {
			return true;
		}

		m_selectedSharedConnector.refresh();
		m_parentSharedConnector.refresh();
		Set<ISharedConnector> lockedChildRevisions = new HashSet<ISharedConnector>();
		try {
			//lock this shared connector
			if (!m_selectedSharedConnector.lock()) {
				if (m_selectedSharedConnector.isDeleted()) {
					m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_DELETED;
				}
				else {
					m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_LOCKFAILURE;
				}
				return false;
			}
			if (!m_parentSharedConnector.lock()) {
				if (m_parentSharedConnector.isDeleted()) {
					m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_DELETED;
				}
				else {
					m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_LOCKFAILURE;
				}
				return false;
			}

			if (m_parentSharedConnector.isFrozen() || m_selectedSharedConnector.isFrozen()) {
				m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_FROZEN;
				return false;
			}
			if (!isPositionStillAvailable(position)) {
				m_cmdStatus = COMMAND_STATUS.SHAREDPOSITION_NOTAVAILABLE;
				return false;
			}

			if (!m_parentSharedConnector.isEditable() || !m_selectedSharedConnector.isEditable()) {
				m_cmdStatus = COMMAND_STATUS.SHAREDCONNECTOR_DOMAIN_ACCESS_FAILURE;
				return false;
			}

			Set<ISharedConnector> objectsFailedToLock = new HashSet<ISharedConnector>();
			SharedObjectRevisionHelper.lockRevisionsDependentRevisions(m_selectedSharedConnector, lockedChildRevisions,
					objectsFailedToLock);

			if (!objectsFailedToLock.isEmpty()) {
				showErrorMessageForCannotBreakRevision(objectsFailedToLock);
				m_cmdStatus = COMMAND_STATUS.CANNOT_BREAK_REVISION;
				return false;
			}

			m_selectedSharedConnector.setOccupiedPosition(position);
			position.populateBlockedCavitiesFromLibrary(m_selectedSharedConnector);

			updateRevision();

			syncCableConnectors();

			m_parentSharedConnector.save();
			m_selectedSharedConnector.save();
		}
		finally {
			if (m_selectedSharedConnector.isLocked()) {
				m_selectedSharedConnector.unlock();
			}
			if (m_parentSharedConnector.isLocked()) {
				m_parentSharedConnector.unlock();
			}

			for(ISharedConnector rev : lockedChildRevisions){
				rev.unlock();
			}
		}

		return true;
	}

	private static void showErrorMessageForCannotBreakRevision(Set<ISharedConnector> objectsFailedToLock)
	{
		if (objectsFailedToLock.isEmpty()) {
			return;
		}
		ILockInfo lockInfo = getLockInfo(objectsFailedToLock.iterator().next());

		String messageBody;
		if (lockInfo != null) {
			messageBody = ResourceMgr.getString(AssociateConnectorDialog.class,
					"AssociateConnectorAction.cannotbreakrevision.lockerror.Body1", lockInfo.getUserName(),
					lockInfo.getTimeStamp());
		}
		else {
			messageBody = ResourceMgr.getString(AssociateConnectorDialog.class,
					"AssociateConnectorAction.cannotbreakrevision.lockerror.Body2");
		}

		MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.error.headline"),
				messageBody, getSuggestionForCannotBreakRevision());
	}

	private static ILockInfo getLockInfo(ISharedConnector connector)
	{
		ILockInfo lockInfo = null;
		try {
			lockInfo = CAFUtils.getInstance().getUserSession().getLockInfo(connector.getUID().toString());
		}
		catch (UserSessionException e) {
			// write msg to log.
			System.out.println(e.aError);
			e.printStackTrace();
		}
		return lockInfo;
	}

	private static JLabel getSuggestionForCannotBreakRevision()
	{
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateConnectorAction.class,
				"AssociateConnectorAction.lockerror.Guidance"));
		return actionLabel;
	}

	protected void syncCableConnectors()
	{
		Set<IUID> impactedSharedConnectors = new HashSet<IUID>();
		impactedSharedConnectors.add(m_parentSharedConnector.getUID());
		impactedSharedConnectors.add(m_selectedSharedConnector.getUID());
		ISharedPinListMgr splMgr = m_parentSharedConnector.getManager();
		if (splMgr != null) {
			splMgr.fireChangeEvent(impactedSharedConnectors);
		}
	}

	protected boolean doExecute()
	{
		if (m_selectedSharedConnector != null) {
			return updateSharedConnector();
		}

		IInternalPosition position = getSelectedPosition();
		m_selectedConnector.setOccupiedPosition(position);
		position.populateBlockedCavitiesFromLibrary(m_selectedConnector);

		IInternalPositionsContainer owningConnector = position.getInternalPositionContainer();
		assert (owningConnector != null);
		assert (owningConnector instanceof IConnector);
		IConnector conn = (IConnector) owningConnector;
		IAssembly assembly = conn.getAssembly();
		if (assembly != null) {
			IConnector modConn = m_selectedConnector;
			AssemblyUtils.updateBackshellAsPerPrefs(modConn, false);
		}
		return true;
	}

	public UIDObjectCollection getValidConnectors()
	{
		return m_validConnectors;
	}

	public void setSelectedPosition(IInternalPosition position)
	{
		m_position = position;
	}

	public IInternalPosition getSelectedPosition()
	{
		return m_position;
	}

	public void setSelectedSharedPosition(ISharedInternalPosition position)
	{
		m_sharedposition = position;
	}

	public void setSelectedSharedParent(ISharedConnector connector)
	{
		m_parentSharedConnector = connector;
	}

	public ISharedConnector getSelectedSharedParent()
	{
		return m_parentSharedConnector;
	}

	public ISharedInternalPosition getSelectedSharedPosition()
	{
		return m_sharedposition;
	}

	public IConnector getSelectedConnector()
	{
		return m_selectedConnector;
	}

	protected boolean isValidRevisionToAdd(ISharedConnector parentConnector)
	{
		Set<String> designsInWhichSelectedObjectIsUsed = new HashSet<String>();
		designsInWhichSelectedObjectIsUsed.add(m_selectedConnector.getDesignContainer().getUID().toString());

		Set<String> designsInWhichTargetConnIsRestricted =
				getDesignsParentConnector(parentConnector.getTopLevelConnector());
		if (!designsInWhichTargetConnIsRestricted.isEmpty() && !designsInWhichSelectedObjectIsUsed.isEmpty()) {
			designsInWhichSelectedObjectIsUsed.retainAll(designsInWhichTargetConnIsRestricted);
			return designsInWhichSelectedObjectIsUsed.isEmpty();
		}
		return true;
	}

	protected Set<String> getDesignsParentConnector(ISharedConnector parentConnector)
	{
		return getUsagesOfOtherRevisionsOfSharedObject(parentConnector);
	}

	public boolean canAdd(ISharedConnector parentConnector)
	{
		return isValidRevisionToAdd(parentConnector);
	}

	public static Set<ISharedConnector> getOtherRevisionsOfSharedObject(ISharedConnector revObject)
	{
		IProject project = revObject.getProject();

		Set<ISharedConnector> sharedRevisions = new HashSet<ISharedConnector>();
		for (ISharedPinList otherRev : ((ISharedFullyLoadedPinListMgr)project.getSharedPinListMgr()).getSharedPinLists()) {
			if (!revObject.getUID().equals(otherRev.getUID()) &&
					revObject.getBaseId().equals(otherRev.getBaseId())) {
				sharedRevisions.add((ISharedConnector) otherRev);
			}
		}
		return sharedRevisions;
	}

	public static Set<String> getUsagesOfOtherRevisionsOfSharedObject(ISharedConnector revObject)
	{
		Set<String> result = new HashSet<String>();
		Set<ISharedConnector> sharedRevisions = getOtherRevisionsOfSharedObject(revObject);

		for (ISharedConnector otherRevision : sharedRevisions) {
			result.addAll(CapitalProjectDataServices.getDataServices()
					.getDesignsWhereUsedOrUnPlaced(CAFUtils.getInstance().getCurrentProject(), otherRevision));
		}
		return result;
	}
}
