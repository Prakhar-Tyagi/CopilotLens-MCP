/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.ReplaceInstanceSymbolAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.CommandClientCtxMenuAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.shared.SharedObjectAvailabilityReporter;
import chs.caplets.logic.commands.AssociateLibraryPartCommand;
import chs.cof.icd.IDeviceICD;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IPartUpdateable;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryConnector;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.cofUtils.parts.PartNumberHelper;
import chs.common.IReadOnlyNamedObject;
import chs.common.IRevisionedObject;
import chs.common.SetUtils;
import chs.common.cmd.replacesymbol.ISymbolPinMapperHelper;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolCmd;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolOptions;
import chs.common.cmd.replacesymbol.UpdateInstanceSymbolCmd;
import chs.ctf.caf.ui.LibraryPinMapperDialog;
import chs.ctf.caf.utils.IPinMapperHelper;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.LibraryPinMapProvider;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.caf.utils.PinMapProviderFactory;
import chs.ctf.caf.utils.PinMapper;
import chs.ctf.caf.utils.PinMappingInfoHelper;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.CavityProxy;
import chs.utility.DiagramHelper;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.ILibPartProjUsageValidityReporter;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Action to update a library part for a single object on a logic diagram.
 */
public class UpdatePartAction extends CommandClientCtxMenuAction<AssociateLibraryPartCommand>
{

	private Set<ISharedObject> sharedObjects = new HashSet<ISharedObject>();
	private boolean sharedObjectsModified = false;

	/**
	 * If the selection contains only a single pinlist, return it.  Otherwise return null
	 *
	 * @param sel The selection
	 *
	 * @return The schem pinlist or null if there is not a single selection
	 */
	@Nullable
	public static IPartUpdateable getSingleSelectedPinList(SelectSet sel)
	{
		// we probably don't need this method now that we have the SelectSet method...
		return sel.getSingleSelectedObject(IPartUpdateable.class);
	}

	/*
	 * Get a libraried object from a UID object, returns null if none is found
	 */
	@Nullable public static ILibrariedObject getLibrariedObject(@Nullable IPartUpdateable editObj)
	{
		return editObj != null ? editObj.getConnectivity() : null;
	}

	/**
	 * Get a library object (Part) from a libraried object, returns null if none is found
	 *
	 * @param libraried - the object that may have a library part assigned.
	 * @param updatePart - should the latest version of the part be obtained?
	 *
	 * @return the library object.
	 */
	@Nullable
	protected ILibraryObject getLibraryObject(ILibrariedObject libraried, boolean updatePart)
	{
		ILibraryBaseObject uidObj = PartNumberHelper.getLibraryObject(libraried);
		if (updatePart) {
			uidObj = FactoryMgr.getSystemFactory().getCHSSystem().getPartsLibrary().update(uidObj);
		}
		return CommonUtils.cast(uidObj, ILibraryObject.class);
	}

	private static boolean isCompatibleLibraryObject(@NotNull ILibrariedObject librariedObject,
			@NotNull ILibraryObject libraryObject)
	{
		ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(librariedObject);
		return logicObject != null && LibraryHelper.isLibraryGroupTypeSame(logicObject, libraryObject);
	}

	public enum PinMappingOperationContext
	{
		LIBRARY,
		ICD
	}

	@NotNull
	private static IPinMapperHelper getPinMappingHelper(PinMapper pinMapper, PinMappingInfoHelper infoHelper,
			AssociateLibraryPartCommand cmd, PinMappingOperationContext context)
	{
		switch (context) {
			case ICD:
				return new ICDPinMapperHelper(cmd, pinMapper, infoHelper);
			case LIBRARY:
			default:
				return new LibraryPinMapperHelper(pinMapper, infoHelper);
		}
	}

	@NotNull
	private static ISymbolPinMapperHelper getReplaceSymbolPinMappingHelper(AssociateLibraryPartCommand cmd,
			PinMappingOperationContext context, boolean isUpdate)
	{
		ReplaceInstanceSymbolOptions applicableOptions = createReplaceInstanceSymbolOptions();

		switch (context) {
			case ICD:
				IDeviceICD icd = cmd.getDeviceICD();
				String sourceName = icd != null ? icd.getRole() : StringUtils.EMPTY_STRING;
				return new ICDUpdatePartReplaceSymbolPinMapper(applicableOptions,
						ResourceMgr.getStringForLabel(PinMapper.class, "PinMapper.icd.text"), sourceName, isUpdate);
			case LIBRARY:
			default:
				return new UpdatePartReplaceSymbolPinMapper(applicableOptions,
						ResourceMgr.getStringForLabel(PinMapper.class, "PinMapper.libraryPart.text"),
						cmd.getLibraryObject().getPartNumber(), isUpdate);
		}
	}

	public static void setupPinMapper(AssociateLibraryPartCommand cmd)
	{
		setupPinMapper(cmd, PinMappingOperationContext.LIBRARY);
	}

	/**
	 * Setup UI pin-mapping that may be used for this command.  We need to callback into the UI from the command.
	 *
	 * @param cmd The command.
	 */
	public static void setupPinMapper(AssociateLibraryPartCommand cmd, PinMappingOperationContext context)
	{
		// never any pin mapping if no library object (Remove Part)
		ILibraryObject libObj = cmd.getLibraryObject();
		if (libObj == null) {
			return;
		}

		IPinList pl = cmd.getPinList();
		ReplaceInstanceSymbolCmd replaceSymbolCmd = cmd.getReplaceSymbolCmd();
		if (replaceSymbolCmd != null) {
			// if there is an underlying replace or update symbol commmand, we delegate the pin mapping to that
			// this is because the pin mapping is required to handle symbol pins rather than library pins
			boolean isUpdate = replaceSymbolCmd instanceof UpdateInstanceSymbolCmd;
			replaceSymbolCmd.setPinMapper(getReplaceSymbolPinMappingHelper(cmd, context, isUpdate));
		}
		else if (pl != null) {
			// in other cases we (may) use the regular library pin mapper
			// TODO jacobt FEAT2145 : need to handle multiple shared objects in the context of the command
			Set<ISharedPinList> doneShared = new HashSet<ISharedPinList>();
			PinMapper pinMapper =
					LibraryPinMapperHelper.createPinMapper(pl, libObj, doneShared, cmd.getSpecifiedLibraryFootprint());
			PinMappingInfoHelper infoHelper = cmd.createPinMappingInfoHelper(pl, "");
			cmd.setPinMapper(getPinMappingHelper(pinMapper, infoHelper, cmd, context));
		}
	}

	protected ReplaceInstanceSymbolOptions createReplaceInstanceSymbolObjectsForTest()
	{
		return createReplaceInstanceSymbolOptions();
	}

	@NotNull private static ReplaceInstanceSymbolOptions createReplaceInstanceSymbolOptions()
	{
		return new ReplaceInstanceSymbolOptions(true, true, true, true, false, false);
	}

	public UpdatePartAction(ICapletController controller)
	{
		super(controller);
	}

	/**
	 * This action is enabled for single selections of objects with a library part (same as Update button in
	 * Properties).
	 */
	public boolean isEnabled()
	{
		if (getController().getCapletModel().isEditable()) {
			IPartUpdateable editObj = getSingleSelectedPinList(getController().getSelectMgr().getPreSelections());
			if (editObj != null) {
				ISchemDiagram diagram = DiagramHelper.getDiagram(editObj);
				if (diagram == null || !diagram.isEditable()) {
					return false;
				}

				ILogicObject logObj = editObj.getConnectivity();
				if (DesignType.LAYOUT.equals(logObj.getDesignContainer().getDesignType()) &&
						!ILogicOtherComponent.class.isInstance(logObj)) {
					return false;
				}
				// has this object got a shared object? If so, we can't update the part
				// if the shared object is frozen...
				ISharedObject sharedObj = logObj.getSharedObject();
				if (sharedObj != null && sharedObj.isFrozen()) {
					return false;
				}

				ILibrariedObject libraried = getLibrariedObject(editObj);
				if ((libraried != null) && isValidObjectType(libraried)) {
					return (libraried.getLibraryRef() != null || !StringUtils.isBlank(libraried.getPartNumber())) &&
							super.isEnabled();
				}
			}
		}
		return false;
	}

	public String getActionUIClass()
	{
		return UpdatePartActionUI.class.getName();
	}

	protected boolean doOnActivateCmdInitialize()
	{
		// create & prepare the underlying command
		AssociateLibraryPartCommand cmd = createCommand();
		if (cmd != null && cmd.prepare()) {
			setCommand(cmd);
			return true;
		}
		return false;
	}

	/**
	 * Overridden here because we might have to do our own thing for making sure shared objects get changed.
	 * <p>
	 * This should really be done by the Command? Can't do that here because a save is already done in the
	 * PropertiesClient, when this is performed from the UI.
	 */
	protected boolean onTerminate(boolean successful)
	{
		if (!successful) {
			return true; // dialog cancelled, nothing else to do here
		}

		// for shared objects we must:
		// * attempt to lock shared objects
		// * perform the action if the shared obects were locked
		// * flush the shared object changes and unlock the shared objects
		if (!lockSharedObjects()) {
			unlockSharedObjects(false); // unlock any that we managed to lock
			return false;
		}

		boolean terminated = super.onTerminate(successful);
		sharedObjectsModified = false;
		unlockSharedObjects(successful);
		sharedObjects.clear();

		clearUndoableContainer();
		return terminated;
	}

	// TODO creddy(2015.1): Make this private
	protected void clearUndoableContainer()
	{
		if (sharedObjectsModified) {
			// Editing of shared objects is not undoable
			getController().getUndoableContainer().endEdit();
			getController().getUndoableContainer().clear();
		}
	}

	protected boolean lockSharedObjects()
	{
		sharedObjects.clear();

		// this action is currently only enabled for pinlists
		// may need to refactor this or hopefully remove it
		// e.g. when we do FEAT3248 Reduce saves on edit shared objects in 7.2?
		IPartUpdateable pinList = getSingleSelectedPinList(getController().getSelectMgr().getPreSelections());
		if (pinList != null) {
			ISharedObject sharedObject = pinList.getSharedObject();
			if (sharedObject != null) {
				sharedObjects.add(sharedObject);

				// if we are editing a shared inline, we need to lock the mate
				if (pinList.getConnectivity() instanceof IGenericInlineConnector &&
						sharedObject instanceof ISharedConnector) {
					sharedObjects.addAll(((ISharedConnector) sharedObject).getMates());
				}
			}
		}

		// now attempt the actual locking back out on the first failure
		for (ISharedObject sharedObject : sharedObjects) {
			LockUpdateHelper luh = new LockUpdateHelper(sharedObject);
			if (!luh.lockAndRefresh()) { // prompts user that it went wrong
				return false;
			}
		}
		return true;
	}

	/**
	 * Save (if required) and unlock the shared objects that were locked by this action.
	 *
	 * @param save Save the shared object or not before unlocking it
	 */
	protected void unlockSharedObjects(boolean save)
	{
		for (ISharedObject sharedObjectToUnlock : sharedObjects) {
			new LockUpdateHelper(sharedObjectToUnlock).flushAndUnlock(save);
			if (sharedObjectToUnlock instanceof IRevisionedObject) { // it will be a SPL
				IRevisionedObject sharedObject = (IRevisionedObject) sharedObjectToUnlock;
				IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
				String projectUid =
						CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getUID().getString();
				auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null, projectUid,
						sharedObject.getFullName(), sharedObject.getUID().getString());
				sharedObjectsModified = true;
			}
		}
	}

	/**
	 * Create a command appropriate for this action
	 *
	 * @return A newly constructed command or null if for some reason this fails (e.g. the library part is not
	 * available)
	 */
	@Nullable protected AssociateLibraryPartCommand createCommand()
	{
		ISchemDiagram diagram = (ISchemDiagram) getController().getCapletModel().getModelRoot();
		if (diagram == null) {
			assert false : "Null diagram!!!";
			return null;
		}
		IProject project = diagram.getProject();
		if (project == null) {
			assert false : "Null project!!!";
			return null;
		}
		IPartUpdateable editObj = getSingleSelectedPinList(getController().getSelectMgr().getPreSelections());
		final ILibrariedObject librariedObject = getLibrariedObject(editObj);
		ILibraryObject libObj = null;
		CAFCommandHelper commandHelper = new CAFCommandHelper();

		if (librariedObject != null) {
			libObj = getLibraryObject(librariedObject, true); // Flush cache, need up to date cavities etc.
			if (libObj != null) {
				if (!isCompatibleLibraryObject(librariedObject, libObj)) {
					// library part referenced on the instance is not compatible
					String heading =
							ResourceMgr.getString(UpdatePartAction.class, "UpdatePartAction.IncompatiblePart.heading");
					String message =
							ResourceMgr.getString(UpdatePartAction.class, "UpdatePartAction.IncompatiblePart.message");
					commandHelper.showErrorMessage(heading, message);

					return null;
				}
				if (librariedObject.getLibraryRef() == null) {
					// Domain of the library part is not accessible by current project. Do we want this only when assigning and not updating?
					ILibPartProjUsageValidityReporter validityReporter = new ILibPartProjUsageValidityReporter()
					{
						private void showErrorMessage(String key)
						{
							String headingResourceKey = "UpdatePartAction.notUsableInProject.heading";
							String messageResourceKey = "UpdatePartAction." + key + ".message";
							String heading = ResourceMgr.getString(UpdatePartAction.class, headingResourceKey);
							String message = ResourceMgr.getString(UpdatePartAction.class, messageResourceKey);
							commandHelper.showErrorMessage(heading, message);
						}

						@Override
						public void partStatusObsolete(@NotNull IProject project, @NotNull ILibraryObject libraryObject)
						{
							showErrorMessage("partStatusObsolete");
						}

						@Override
						public void partStatusNotCurrent(@NotNull IProject project,
								@NotNull ILibraryObject libraryObject)
						{
							showErrorMessage("partStatusNotCurrent");
						}

						@Override
						public void domainNotAllowed(@NotNull IProject project, @NotNull ILibraryObject libraryObject)
						{
							showErrorMessage("domainInaccessible");
						}

						@Override
						public void notPreferredPart(@NotNull IProject project, @NotNull ILibraryObject libraryObject)
						{
							showErrorMessage("notPreferredPart");
						}
					};
					if (!LibraryHelper.isPartUsableForProject(project, libObj, validityReporter)) {
						return null;
					}
				}
				if (librariedObject instanceof ILogicObject) {
					ISharedObject sharedObject = ((ILogicObject) librariedObject).getSharedObject();
					if (sharedObject != null) {
						final IDesign logicDesign = ((ILogicObject) librariedObject).getLogicDesign();
						// Cancel 'Update Part' action on a shared object (all types) instance which is restricted to current user
						final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
						if (!new SharedObjectAvailabilityChecker().check(sharedObject, logicDesign, reporter, false)) {
							return null;
						}
					}
					ILibraryConnector libraryConnector = CommonUtils.cast(libObj, ILibraryConnector.class);

					IConnector connector = CommonUtils.cast(librariedObject, IConnector.class);
					//For shared the following use cases cannot be handled by update part, Library pin mapping used in update part cannot handle these cases:
					//a) Existing blocked cavity by child connector updated from say "1" to "2".
					//b) Number of blocked cavities by child connector decreased from say "1,2,3" to "4,5".
					//c) Number of blocked cavities by child connector increased from say "1,2" to "4,5,6".
					//d)When a blocked cavity is added first time in the connector.

					//for non-shared if the new library blocked cavity after update part is not part of the logic object then update part has no issue
					//However if the connector cavity is already placed on the design then the update part fails for pin in design error.
					//Hence the following check is limited to only shared connector.
					if (libraryConnector != null && connector != null && connector.getSharedPinList() != null) {
						Set<String> libraryConnectorBlockedCavities =
								ModularConnectorHelper.getLibraryCavitiesToBeBlockedForConnector(connector);
						Set<String> blockedCavitiesOnInstance = connector.getBlockedCavities();
						if (!SetUtils.equal(libraryConnectorBlockedCavities, blockedCavitiesOnInstance)) {
							ResourceBasedMessageContent blockedCavitiesMismatch =
									new ResourceBasedMessageContent(UpdatePartAction.class,
											"UpdatePartAction.mismatchedblockedcavities");

							commandHelper.showErrorMessage(blockedCavitiesMismatch);
							return null;
						}
					}
				}
			}
		}

		if (libObj == null) {
			// library part referenced on the instance is no longer available
			String heading = ResourceMgr.getString(UpdatePartAction.class, "UpdatePartAction.MissingPart.heading");
			String message = ResourceMgr.getString(UpdatePartAction.class, "UpdatePartAction.MissingPart.message");
			commandHelper.showErrorMessage(heading, message);
			return null;
		}

		AssociateLibraryPartCommand cmd =
				new AssociateLibraryPartCommand(commandHelper, diagram, editObj, libObj);
		setupPinMapper(cmd);
		return cmd;
	}

	/**
	 * Returns true iff aLibrariedObject is valid for update
	 *
	 * @param aLibrariedObject Object to check
	 *
	 * @return True iff object is valid for update
	 */
	private static boolean isValidObjectType(ILibrariedObject aLibrariedObject)
	{
		if (aLibrariedObject == null) {
			return false;
		}
		return !((aLibrariedObject instanceof IDeviceConnector) || (aLibrariedObject instanceof IInterconnectObject));
	}

	/**
	 * This class implements the IPinMapperHelper interface by using the LibraryPinMapperDialog on the objects with
	 * which it is constructed
	 */
	public static class LibraryPinMapperHelper implements IPinMapperHelper

	{

		private PinMapper pinMapper;
		protected PinMappingInfoHelper infoHelper;

		public LibraryPinMapperHelper(PinMapper mapper, PinMappingInfoHelper helper)
		{
			pinMapper = mapper;
			infoHelper = helper;
		}

		public Map<IReadOnlyNamedObject, IPinProxy> promptPinMapping(boolean hasNext)
		{
			Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
			return LibraryPinMapperDialog.promptPinMapping(owner, pinMapper, infoHelper, hasNext);
		}

		protected PinMapper getPinMapper()
		{
			return pinMapper;
		}

		public static PinMapper createPinMapper(IPinList conn, ILibraryObject libObj,
				Set<ISharedPinList> doneShared)
		{
			return createPinMapper(conn, libObj, doneShared, null);
		}

		/**
		 * Create a PinMapper UI component suitable for shared pinlists.  May take account of shared connector mates.
		 *
		 * @param conn Connectivity pinlist
		 * @param libObj Library part
		 * @param doneShared Shared pinlists already processed.  Affects handling of shared connector mates.
		 *
		 * @return The PinMapper UI component
		 */
		public static PinMapper createPinMapper(IPinList conn, ILibraryObject libObj,
				Set<ISharedPinList> doneShared, @Nullable ILibraryDeviceFootprint footprint)
		{
			PinMapper pm;
			ISharedPinList spl = conn.getSharedPinList();
			if (spl == null) {
				pm = getPinMapper(conn, libObj, footprint);
			}
			else {
				// If we have processed the mate of an inline, then we add the additional number of pins to the list.
				List<String> augmentPins = new ArrayList<String>();
				if (spl instanceof ISharedConnector) {
					//
					// Is the mate included?
					//
					ISharedConnector sc = (ISharedConnector) spl;
					ISharedConnector scmate = sc.getMate();
					if (scmate != null && doneShared.contains(scmate)) {
						//
						// We need to add some temporary pins as the mate has had some added.
						//
						int additionalPins = (LibraryHelper.getCavities(libObj).size() - scmate.getPins().getSize());
						for (int i = 0; i < additionalPins; i++) {
							// string concat is harmless here
							//noinspection StringContatenationInLoop
							augmentPins.add(
									ResourceMgr.getString(PinMapper.class, "PinMapper.autopin.text") + (i + 1));
						}
					}
				}

				pm = getPinMapper(conn, libObj, spl, augmentPins, footprint);
			}
			return pm;
		}

		private static PinMapper getPinMapper(IPinList conn, ILibraryObject libObj,
				@NotNull ISharedPinList spl, @Nullable List<String> augmentPins,
				@Nullable ILibraryDeviceFootprint footprint)
		{

			LibraryPinMapProvider pinMapProvider = PinMapProviderFactory.instance()
					.createLibraryPinMapperProvider(libObj, conn, spl, augmentPins, footprint);
			PinMapper pm = new PinMapper(pinMapProvider, libObj.getPartNumber(), spl.getName());

			return pm;
		}

		private static PinMapper getPinMapper(IPinList conn, ILibraryObject libObj,
				@Nullable ILibraryDeviceFootprint footprint)
		{
			LibraryPinMapProvider pinMapProvider = PinMapProviderFactory
					.instance().createLibraryPinMapperProvider(libObj, conn, footprint);
			PinMapper pm = new PinMapper(pinMapProvider, libObj.getPartNumber(), conn.getName());

			return pm;
		}
	}

	public static class ICDPinMapperHelper extends LibraryPinMapperHelper
	{

		public ICDPinMapperHelper(AssociateLibraryPartCommand cmd, PinMapper mapper, PinMappingInfoHelper helper)
		{
			super(mapper, helper);
			IDeviceICD icd = cmd.getDeviceICD();
			mapper.setSourceLabel(ResourceMgr.getStringForLabel(PinMapper.class, "PinMapper.icd.text"),
					icd != null ? icd.getRole() : StringUtils.EMPTY_STRING);
			helper.showConnectivityInfo(false);
		}
	}

	public static class UpdatePartReplaceSymbolPinMapper extends ReplaceInstanceSymbolAction.ReplaceSymbolPinMapper
	{

		private Map<IGenericSchemPin, String> m_mappedSymPinNames = new HashMap<IGenericSchemPin, String>();

		public UpdatePartReplaceSymbolPinMapper(ReplaceInstanceSymbolOptions options, String sourceLabel,
				String sourceName,
				boolean isUpdate)
		{
			super(options, sourceLabel, sourceName, isUpdate);
		}

		protected void recievePinMapper(@NotNull PinMapper pinMapper)
		{
			m_mappedSymPinNames.clear();
			for (IReadOnlyNamedObject srcObj : pinMapper.getFromList()) {
				if (srcObj instanceof CavityProxy) {
					m_mappedSymPinNames.put(((CavityProxy) srcObj).getPin(), srcObj.getName());
				}
			}
		}

		@Override public String getMappedNameForSymPin(IGenericSchemPin symPin)
		{
			String mappedSymPinName = m_mappedSymPinNames.get(symPin);
			if (mappedSymPinName != null && !StringUtils.isBlank(mappedSymPinName)) {
				return mappedSymPinName;
			}
			return super.getMappedNameForSymPin(symPin);
		}
	}

	public static class ICDUpdatePartReplaceSymbolPinMapper extends UpdatePartReplaceSymbolPinMapper
	{

		public ICDUpdatePartReplaceSymbolPinMapper(ReplaceInstanceSymbolOptions options, String sourceLabel,
				String sourceName,
				boolean isUpdate)
		{
			super(options, sourceLabel, sourceName, isUpdate);
		}

		@Override
		@NotNull
		protected PinMappingInfoHelper createSymbolPinMappingInfoHelper(@Nullable chs.cof.logical.schem.IPinList pl,
				String knownChanges)
		{
			PinMappingInfoHelper mappingInfoHelper = super.createSymbolPinMappingInfoHelper(pl, knownChanges);
			mappingInfoHelper.showConnectivityInfo(false);
			return mappingInfoHelper;
		}
	}
}
