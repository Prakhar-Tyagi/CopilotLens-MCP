/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caplets.logic.ApplyLibraryPartOnPinlist;
import chs.caplets.logic.actions.ghc.ConnectivityGHCHelper;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.caplets.logic.updateSymbol.DeviceSymbolInformationUpdator;
import chs.caplets.logic.updateSymbol.SharedPinListSymbolInfomationUpdator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.ISecondaryRepresentation;
import chs.cof.icd.IDeviceICD;
import chs.cof.library.ILibrariedObject;
import chs.cof.library.ILibraryPartMatchCriteriaProvider;
import chs.cof.library.PSDLibraryPartSelection;
import chs.cof.logical.ConvertPinTypeLogEnum;
import chs.cof.logical.GeneralReportValidationHandler;
import chs.cof.logical.IAssociateLibraryPartCommand;
import chs.cof.logical.IDesign;
import chs.cof.logical.IInternalPositionBase;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IDevicePinAttributeProvider;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.cable.IInternalPositionsContainer;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.concurrency.ConcurrencySymbolReplacementHelper;
import chs.cof.logical.footprint.IReportValidationHandler;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPartUpdateable;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinOwner;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemOtherComponent;
import chs.cof.logical.schem.ISymboledSchemPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryAssembly;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryCustomerPartNumber;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryDeviceFootprintObject;
import chs.cof.parts.ILibraryDevicePin;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibrarySolderSleeve;
import chs.cof.parts.ILibrarySupplierPartNumber;
import chs.cof.parts.ILibraryTerminal;
import chs.cof.parts.ILibraryUltrasonicWeld;
import chs.cof.parts.LibraryBooleanType;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.LibraryDevicePinPropertiesLoader;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.DefaultLibraryPartSelection;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.ILibrarySelectionFilter;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.project.IProject;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.ISymboledObject;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.CommandContext;
import chs.cofUtils.cmd.CommandHelper;
import chs.cofUtils.logical.concurrency.LibraryControlConcurrencyHelper;
import chs.cofUtils.parameterized.DefaultGeneratorDCFeedback;
import chs.cofUtils.parameterized.DeviceConnectorBackshellSyncOnRegeneration;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parts.PartNumberHelper;
import chs.common.DesignUtils;
import chs.common.IAnalysable;
import chs.common.IBOMObject;
import chs.common.ICommandHelper;
import chs.common.IDeletedObject;
import chs.common.IDesignContainer;
import chs.common.IDesignMgr;
import chs.common.IPrivilegedDesignMgr;
import chs.common.IProjectPreferenceMgr;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolCmd;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolOptions;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.common.styles.IStyleableObject;
import chs.ctf.caf.ui.LibraryPinMapperDialog;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.ctf.caf.utils.IGenericPinProxy;
import chs.ctf.caf.utils.IPinMapperHelper;
import chs.ctf.caf.utils.IPinMappings;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinMappingInfoHelper;
import chs.ctf.caf.utils.PinMappings;
import chs.ctf.drc.logic.ValidLibraryPinsCheck;
import chs.subsystem.structure.StructureMatchService;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.WrappingRuntimeException;
import chs.utilities.ui.MessageHelper;
import chs.utility.AssemblyUtils;
import chs.utility.DiagramHelper;
import chs.utility.PinTypeConversionChecker;
import chs.utility.SymbolUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LogTabType;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PropertyCopier;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.PropertyTemplateHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.logic.StudPinUtils;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A command to perform an (un)association of a library part with an object (typically a pinlist).
 * <p>
 * This command can be used to Add, Update or Remove a library part.
 */

public class AssociateLibraryPartCommand extends CHSCommand implements IAssociateLibraryPartCommand
{

	private ILibraryObject libObj;
	private ISchemDiagram diagram;
	private IUIDObject editObj;
	private ILibraryPartSelection librarySelectedObject;
	protected Predicate<IDesignContainer> mIsSkeletonDesign;

	private ISymbolDef libSymDef = null;
	private ISymbolDef devSymDef = null;
	private ILibraryGraphic.ContextType libSymContext = ILibraryGraphic.ContextType.ELECTRICAL;
	private ILibraryDeviceFootprint deviceFootprint = null;
	private ReplaceInstanceSymbolCmd replaceSymbolCmd = null;
	private ReplaceInstanceSymbolParams params = null;

	@Nullable private IPinMappings mPinMappings = null;
	private IPinMapperHelper pinMapper;
	private boolean allowAutoMapAllByName = false; // If true will not show dialog if all pins/cavities can be mapped

	// temp - flags to stop calling prepare more than once, should probably be fixed in CommandClientAction
	private boolean cmdPrepared = false;
	private boolean cmdPreparedOK = false;
	private boolean isSilent = false; // should the command operate without user intervention...
	private GenerateHarnessConnActionHelper generateHarnessConnActionHelper;
	private boolean m_syncModularConnector = true;
	private boolean m_syncPropertiesWithSource = true;
	private Set<String> missingPartNumbers = new HashSet<String>();
	private boolean validateLibGrptypeAttrBeforePartAssign = true;
	private boolean preserveIncludeOnBOM = false;

	/**
	 * Command to Add, Update or remove a library part from an object (that can have a part associated).
	 *
	 * @param commandHelper All commands must be constructed with a CommandHelper
	 * @param diag The diagram on which the object with the library part exists
	 * @param obj The object with the library part - this is probably not an ILibrariedObject, but has access to one
	 * @param part The library part, pass null to remove the part
	 */
	public AssociateLibraryPartCommand(CommandHelper commandHelper, ISchemDiagram diag, IUIDObject obj,
			ILibraryObject part)
	{
		this(commandHelper, diag, obj, part, null);
	}

	public AssociateLibraryPartCommand(CommandHelper commandHelper, ISchemDiagram diag, IUIDObject obj,
			ILibraryObject part, @Nullable ILibraryPartSelection selObj)
	{
		super(commandHelper);
		diagram = diag;
		libObj = part;
		editObj = obj;
		librarySelectedObject = selObj;
		generateHarnessConnActionHelper = new GenerateHarnessConnActionHelper(diagram);
		init();
	}

	public AssociateLibraryPartCommand(IPinList pinList, ILibraryObject libraryObject)
	{
		this(new CAFCommandHelper(), pinList.getDiagram(), pinList, libraryObject);
	}

	public AssociateLibraryPartCommand(IPinList pinList, ILibraryObject libraryObject, ILibraryPartSelection selection)
	{
		this(new CAFCommandHelper(), pinList.getDiagram(), pinList, libraryObject, selection);
	}

	@Nullable public IDeviceICD getDeviceICD() {
		if (librarySelectedObject instanceof IICDSelection) {
			return ((IICDSelection) librarySelectedObject).getICD();
		}
		return null;
	}

	public void setAllowAutoMapAllByName(boolean b)
	{
		allowAutoMapAllByName = b;
	}

	public void setPreserveIncludeOnBOM(boolean b)
	{
		preserveIncludeOnBOM = b;
	}

	public void setLibGrpTypeValidationBeforePartAssignment(boolean validate)
	{
		validateLibGrptypeAttrBeforePartAssign = validate;
	}

	private void init()
	{
		//Todo moattia-MultiSymbolledDevicesOM: Must change this.
		if (libObj != null) {
			// Symbol update now depends on project preference setting (dts0100706456)
			IProject project = diagram.getDesign().getProject();
			assert project != null;
			IProjectPreferenceMgr projectPreferenceMgr = project.getPreferences();
			boolean symbolUpdate = false;
			if (projectPreferenceMgr != null) {
				symbolUpdate = projectPreferenceMgr.getUpdateSymbolOnPartUpdate();
			}

			if (editObj instanceof IPinList && symbolUpdate) {
				// Update symbol is not allowed if pinlist has any stacked pins
				symbolUpdate = ((IPinOwner) editObj).getStackPins().stream().filter(e -> e.getNumPins() > 0).toList()
						.isEmpty();
			}
			if (symbolUpdate) {
				libSymContext = determineLibSymbolContext(editObj);
				devSymDef = loadSymbolDef(editObj);
				libSymDef = getLibrarySymbolDef(devSymDef);
			}

			if (libSymDef != null) {
				// we need a sub-command to update or replace symbol
				// default options to start with
				IUID uid = editObj.getUID();
				ISymbolRef symRef = determineSymbolRef(editObj);
				params = new ReplaceInstanceSymbolParams(diagram, uid, doGetReplaceInstanceSymbolOptions(), symRef);
				ConcurrencySymbolReplacementHelper helper = new ConcurrencySymbolReplacementHelper();
				params.setSymbolRef(UtilsHelper.getCHSUtils().getSymbolFactory().constructSymbolRef(libSymDef));
				helper.attemptLocksForSymbolReplacement(Collections.singleton(params));

				if (devSymDef == null || !libSymDef.getUID().equals(devSymDef.getUID())
						|| (editObj instanceof IPinList && !isSymbolPresentForThisInstance((IPinList) editObj))) {
					// replace symbol
					replaceSymbolCmd = new ReplaceInstanceSymbolCmd(getCommandHelper(), params,
							ConductorRouteAction.getInstance());
					// pin-mapping is setup on the params later
				}
				else {
					// update symbol
					// dts0100523575-Error occurs when adding a library part to the device
					List<ReplaceInstanceSymbolParams> paramsList = new ArrayList<ReplaceInstanceSymbolParams>(1);
					paramsList.add(params);
					replaceSymbolCmd = new LibraryPartUpdateSymbolCommand(getCommandHelper());
					replaceSymbolCmd.setParams(paramsList);
				}
				replaceSymbolCmd.setApplyStyle(false);
			}
		}
	}

	private ILibraryGraphic.ContextType determineLibSymbolContext(IUIDObject editObj)
	{
		if (editObj instanceof IPartUpdateable) {
			return ((IPartUpdateable) editObj).getLibrarySymbolContext();
		}
		return ILibraryGraphic.ContextType.ELECTRICAL;
	}

	@Nullable protected ISymbolDef getLibrarySymbolDef(@Nullable ISymbolDef aDevSymDef)
	{
		if (canUpdateSymbol()) {
			return getLibrarySymbolIncludingUserPref(aDevSymDef);
		}
		return null;
	}

	@Nullable private ISymbolDef getLibrarySymbolIncludingUserPref(@Nullable ISymbolDef aDevSymDef)
	{
		// not all pinlists need to worry about updating/replacing symbol with that on the part
		IUID libSymUID = aDevSymDef == null ? null : aDevSymDef.getUID();
		if (librarySelectedObject instanceof DefaultLibraryPartSelection) {
			if (((DefaultLibraryPartSelection) librarySelectedObject).getApplyUserPreferences()) {
				IUID partLibSymUID = getSpecifiedLibrarySymbolUID();
				if (partLibSymUID != null) {
					libSymUID = partLibSymUID;
				}
			}
		}
		return ValidLibraryPinsCheck.loadLibrarySymbolDef(libObj, libSymUID, libSymContext);
	}

	/**
	 * Set a pin mapper for callbacks to prompt for pin mapping if required
	 *
	 * @param mapper Knows how to prompt for pin mappings
	 */
	public void setPinMapper(IPinMapperHelper mapper)
	{
		pinMapper = mapper;
	}

	// TODO jacobt FEAT3083.1 : showError should be available on base command (for unit test and other clients)
	//                          prob. should be handled via CommandListener?  Need to check with alanp

	public void showError(String msg)
	{
		getOutputWindow().sendMessage(msg,
				ResourceMgr.getString(AssociateLibraryPartCommand.class, "AssociateLibraryPartCommand.output.tab"),
				true);
//		String heading =
//				ResourceMgr.getString(AssociateLibraryPartCommand.class, "AssociateLibraryPartCommand.error.title");
//		getCommandHelper().showErrorMessage(heading, msg);
	}

	/**
	 * Is the command running in silent mode, i.e. without user intervention.
	 *
	 * @return boolean, true if we're running in silent mode.
	 */
	public boolean isSilent()
	{
		return isSilent;
	}

	/**
	 * Should we run without user intervention? i.e. without prompting the user for information through a ui
	 *
	 * @param b, are we silent?
	 */
	public void setSilent(boolean b)
	{
		isSilent = b;
	}

	/**
	 * For assigning symbols and footprints where the library part has multiple symbols and/or footprints we select in
	 * the following way:
	 * <p>
	 * If a client has called this method we use the symbol and footprint specified, this is typically from the UI.
	 * <p>
	 * else
	 * <p>
	 * If the instance already has a symbol or footprint that matches one from the library part we use those ones.
	 * <p>
	 * else
	 * <p>
	 * We use the defaults from the library part.
	 *
	 * @param selObj The ILibrarySelectedObject to specify symbol, footprint etc.
	 */
//	public void setLibrarySelectedObject(ILibraryPartSelection selObj)
//	{
//		librarySelectedObject = selObj;
//	}

	/**
	 * Returns the IUID of a specified symbol, normally the source is the Part Selection Dialog
	 *
	 * @return IUID The UID of the symbol specified by librarySelectedObject
	 */
	@Nullable public IUID getSpecifiedLibrarySymbolUID()
	{
		if ((librarySelectedObject != null) && (librarySelectedObject.getSelectedSymbol() != null)) {
			ILibraryGraphic librarySymbol = librarySelectedObject.getSelectedSymbol();
			IStamp stamp = librarySymbol.getLibrarySymbol();
			if (stamp != null) {
				return stamp.getUID();
			}
			return null;
		}
		return null;
	}

	/**
	 * Returns the a specified ILibraryDeviceFootprint, normally the source is the Part Selection Dialog. If the
	 * footprint isn't set on librarySelectedObject we'll use the default for the library part,
	 *
	 * @return ILibraryDeviceFootprint The footprint specified by librarySelectedObject
	 */
	@Nullable public ILibraryDeviceFootprint getSpecifiedLibraryFootprint()
	{
		if (librarySelectedObject != null) {
			return librarySelectedObject.getSelectedFootprint();
		}
		return null;
	}

	/**
	 * If a pinlist was used to initialize this command, return it
	 *
	 * @return The (possibly null) connectivity pinlist used to initialize this command
	 */
	@Nullable public chs.cof.logical.cable.IPinList getPinList()
	{
		ILogicObject obj = ReferenceHelper.reduceToLibrariedLogicObject(editObj);
		if (obj instanceof chs.cof.logical.cable.IPinList) {
			return (chs.cof.logical.cable.IPinList) obj;
		}
		return null;
	}

	/**
	 * Access to underlying replace (or update) symbol cmd, if we use one
	 *
	 * @return The underlying replace (or update) symbol cmd or null if no cmd is required (e.g. no symbols involved)
	 */
	@Nullable public ReplaceInstanceSymbolCmd getReplaceSymbolCmd()
	{
		return replaceSymbolCmd;
	}

	/**
	 * Get the library object used to initialize this command.
	 *
	 * @return The (possibly null) library object
	 */
	public ILibraryObject getLibraryObject()
	{
		return libObj;
	}

	/**
	 * Set the pin mapping (from library cavities --> pins).
	 *
	 * @param mapping Pin mappings
	 */
	public void setPinMapping(Map<IReadOnlyNamedObject, IPinProxy> mapping)
	{
		mPinMappings = new PinMappings(mapping);
		assert verifyPinMapping();
	}

	/**
	 * Attempt to prepare this command.
	 *
	 * @return true iff we should be able to associate the library part, according to various checks for different
	 * situations.
	 */
	public boolean prepare()
	{
		// TODO jacobt FEAT12145: remove this temp hack
		// temp for dts0100389771 for PSA - should fix by refactoring CommandClientAction such that
		// template method for create command is seperate from doOnActivateCmdInitialize, and we do
		// not call cmd.prepare directly from CommandClientAction
		if (cmdPrepared) {
			return cmdPreparedOK;
		}
		cmdPrepared = true;
		// end temp

		boolean preparedOK = true;

		if (editObj != null) {
			preparedOK = new LibraryControlConcurrencyHelper().prepareSourceForLibraryPartEdit(editObj);
		}

		// Load any symbol assigned to the part,
		// check for mismatch between cavities on part and pins on default symbol
		if (libObj != null && preparedOK) {

			assert editObj != null;
			CommandContext.setObject(editObj);

			// If command wasn't supplied with library selected object create one from the edit object.
			if (!ensureHaveLibrarySelectedObjectForUpdate()) {
				preparedOK = false;
			}
			else {
				if (canUpdateSymbol()) {
					// not all pinlists need to worry about updating/replacing symbol with that on the part
					preparedOK = checkPartSymbolMismatch(libObj, libSymDef);
				}

				if (preparedOK) {
					chs.cof.logical.cable.IPinList pl = getPinList();
					if (pl instanceof IConnector && ((IConnector) pl).isModularParent()) {
						preparedOK = canModularConnectorBeUpdated((IConnector) pl);
					}
				}

				// general preparation for parts with/without symbol
				if (preparedOK) {
					if (libSymDef == null) {
						preparedOK = prepareUpdatePartWithoutSymbol(); // update from part without symbol
					}
					else {
						preparedOK = prepareUpdatePartWithSymbol(); // update from part with symbol
					}
				}

				// is the library part a connector and the logic object a device (known MCD translator defect)
				if (preparedOK) {
					chs.cof.logical.cable.IPinList pl = getPinList();
					if (pl instanceof IDevice) {
						ILibraryDevice libDev = libObj instanceof ILibraryDevice ? (ILibraryDevice) libObj : null;
						if (libDev == null) {
							String msg = ResourceMgr.getString(AssociateLibraryPartCommand.class,
									"AssociateLibraryPartCommand.error.DeviceLibraryPartMismatch", pl.getName());
							showError(msg);
							preparedOK = false;
						}
					}
				}

				// prepare to disconnect harness connectors, if required
				if (preparedOK) {
					chs.cof.logical.cable.IPinList pl = getPinList();
					if (pl instanceof IDevice) {
						// determines which footprint will be used for the update, might have to disconnect HC
						prepareHarnessConnectors((IDevice) pl);
					}
				}
			}
		} // else we're removing a part - no specific checks for this
		cmdPreparedOK = preparedOK;
		return preparedOK;
	}

	private boolean canModularConnectorBeUpdated(IConnector connector)
	{
		IInternalPositionsContainer<IInternalPositionBase> positionsContainer = getPositionContainer(connector);
		if (positionsContainer != null) {
			return canUpdateModularBeConnector(connector, positionsContainer);
		}
		return true;
	}

	@Nullable
	private IInternalPositionsContainer<IInternalPositionBase> getPositionContainer(@NotNull IConnector connector)
	{
		IUIDObject positionContainer = connector;
		if (connector.getSharedPinList() != null) {
			positionContainer = connector.getSharedPinList();
		}
		return CommonUtils.cast(positionContainer, IInternalPositionsContainer.class);
	}

	@NotNull private <T extends ILibrariedObject> Set<String> getCavitiesToBeBlocked(@NotNull T parentConnector,
			@NotNull T connectorInsert, @NotNull String positionName)
	{
		ILibraryBaseObject childLibConn = connectorInsert.getLibraryObject();
		ILibraryBaseObject parentLibConnector = parentConnector.getLibraryObject();
		if (parentLibConnector instanceof ILibraryBaseConnector && childLibConn instanceof ILibraryBaseConnector) {
			return LibraryHelper.getCavitiesToBeBlocked((ILibraryBaseConnector) parentLibConnector, positionName,
					(ILibraryBaseConnector) childLibConn);
		}
		return Collections.emptySet();
	}

	private boolean canUpdateModularBeConnector(@NotNull IConnector connector,
			IInternalPositionsContainer<IInternalPositionBase> positionsContainer)
	{
		Map<String, String> alreadyBlockedCavities = new HashMap<>();
		for (IInternalPositionBase position : positionsContainer.getPositions()) {
			IInternalPositionedObject<IInternalPositionBase> associatedConnector = getAssociatedConnector(position);
			if (associatedConnector != null) {
				if (!areBlockedCavitiesAreBeingUsed(connector, position, associatedConnector)) {
					return false;
				}
				if (hasConflictingCavitiesBlocked(connector, alreadyBlockedCavities, position, associatedConnector)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean hasConflictingCavitiesBlocked(IConnector connector, Map<String, String> alreadyBlockedCavities,
			IInternalPositionBase position, IInternalPositionedObject<IInternalPositionBase> associatedConnector)
	{
		IInternalPositionsContainer<IInternalPositionBase> positionContainer = covertToPositionContainer(connector);
		if (positionContainer != null) {
			Set<String> cavitiestoBlock = ModularConnectorHelper
					.cavitiesToBeBlockedFromLibrary(positionContainer, associatedConnector, position.getName());
			for (String cavity : cavitiestoBlock) {
				if (alreadyBlockedCavities.containsKey(cavity)) {
					String pos1 = position.getName();
					String pos2 = alreadyBlockedCavities.get(cavity);
					String msg;
					if (pos1.compareTo(pos2) < 1) {
						msg = reportOverloadedCavitiesBlocked(cavity, pos1, pos2);
					}
					else {
						msg = reportOverloadedCavitiesBlocked(cavity, pos2, pos1);
					}
					showError(msg);
					return true;
				}
				alreadyBlockedCavities.put(cavity, position.getName());
			}
		}
		return false;
	}

	private String reportOverloadedCavitiesBlocked(String cavity, String pos1, String pos2)
	{
		String msg;
		msg = ResourceMgr.getString(AssociateLibraryPartCommand.class,
				"AssociateLibraryPartCommand.error.ConnectorInsertBlockingAlreadyBlockedCavity",
				pos1, pos2, cavity);
		return msg;
	}

	private IInternalPositionsContainer<IInternalPositionBase> covertToPositionContainer(IConnector connector)
	{
		return (IInternalPositionsContainer<IInternalPositionBase>) convertToObjectOfType(connector,
				IInternalPositionsContainer.class);
	}

	@Nullable private <T> T convertToObjectOfType(IConnector connector, Class<T> objType)
	{
		if (connector.getSharedPinList() != null) {
			return CommonUtils.cast(connector.getSharedPinList(), objType);
		}
		return CommonUtils.cast(connector, objType);
	}

	private boolean areBlockedCavitiesAreBeingUsed(IConnector connector,
			IInternalPositionBase position, IInternalPositionedObject<IInternalPositionBase> associatedConnector)
	{
		Set<String> cavitiesToBeBlocked = new HashSet<>(getCavitiesToBeBlocked(connector,
				associatedConnector, position.getName()));
		cavitiesToBeBlocked.removeAll(position.getBlockedCavities());
		if (!cavitiesToBeBlocked.isEmpty()) {
			if (!ModularConnectorHelper.canCavitiesBeBlocked(connector, cavitiesToBeBlocked)) {
				String msg = ResourceMgr.getString(AssociateLibraryPartCommand.class,
						"AssociateLibraryPartCommand.error.ConnectorInsertBlockingAlreadyUsedCavity",
						associatedConnector.getName(), connector.getName(), position.getName(),
						StringUtils.concatenate(cavitiesToBeBlocked, ","));
				showError(msg);
				return false;
			}
		}
		return true;
	}

	@Nullable private IInternalPositionedObject getAssociatedConnector(IInternalPositionBase position)
	{
		Collection<IInternalPositionedObject> positionedObjects = position.getPositionedObjects();
		return !positionedObjects.isEmpty() ? positionedObjects.iterator().next() : null;
	}

	protected boolean doesSharedDeviceHasUsageInOtherOpenDesigns(@NotNull Set<String> otherOpenedDesigns,
			@NotNull Set<String> otherLoadedDesigns, @NotNull ILogicObject logicObject)
	{
		ILogicDesign design = logicObject.getLogicDesign();
		ISharedDevice sharedDevice = CommonUtils.cast(logicObject.getSharedObject(), ISharedDevice.class);
		// we only need this limitation to regenerate shared DCs
		if (design != null && sharedDevice != null) {
			return doesSharedDeviceHasUsageInOtherOpenDesigns(otherOpenedDesigns, otherLoadedDesigns, design,
					sharedDevice, mIsSkeletonDesign);
		}
		return false;
	}

	// TODO creddy (2015.1) Get rid of this check and update all the loaded designs and diagrams
	public static boolean doesSharedDeviceHasUsageInOtherOpenDesigns(@NotNull Set<String> otherOpenedDesigns,
			@NotNull Set<String> otherLoadedDesigns, @NotNull IDesign design, @NotNull ISharedDevice sharedDevice,
			@Nullable Predicate<IDesignContainer> isSkeletonDesignCheck)
	{
		IProject project = design.getProject();
		assert project != null;
		IDesignMgr designMgr = project.getDesignMgr();

		Collection<IUID> designUIDs = ((IPrivilegedDesignMgr)designMgr).getLoadedAbstractLogicDesigns();

		boolean hasOtherUsage = false;
		for (IUID designUID : designUIDs) {
			if (designUID.equals(design.getUID())) {
				continue;
			}
			ILogicDesign loadedDesign = DesignUtils.getLoadedDesign(designUID, ILogicDesign.class);
			IConnectivity loadedConnectivity = loadedDesign != null ? loadedDesign.getLoadedConnectivity() : null;
			if(isSkeletonDesignCheck != null && isSkeletonDesignCheck.test(loadedDesign)) {
				loadedConnectivity = null;
			}
			chs.cof.logical.cable.IPinList device =
					loadedConnectivity != null ? loadedConnectivity.findSharedPinList(sharedDevice) : null;

			if (device != null) {
				hasOtherUsage = true;
				if (CAFUtils.getInstance().hasDiagramDisplayed(designUID)) {
					otherOpenedDesigns.add(loadedDesign.getFullName());
				}
				else {
					otherLoadedDesigns.add(loadedDesign.getFullName());
				}
			}
		}
		return hasOtherUsage;
	}

	protected boolean wasDesignSkeletonBeforeAction(@NotNull ILogicDesign loadedDesign)
	{
		boolean wasSkeleton = mIsSkeletonDesign != null && mIsSkeletonDesign.test(loadedDesign);
		return loadedDesign.getLoadedConnectivity() !=null && wasSkeleton;

	}

	/**
	 * Returns the ILibrarySelectedObject as a source for assign/update. Should not be null as the call to
	 * ensureHaveLibrarySelectedObjectForUpdate during the prepare step should create one from the editObj if needed.
	 *
	 * @return ILibrarySelectedObject
	 */
	private ILibraryPartSelection getLibrarySelectedObject()
	{
		return librarySelectedObject;
	}

	/**
	 * If client didn't call setLibrarySelectedObject then a call to this method will create one from libObj. It will
	 * also 'update' the attributes which are copied from ILibrarySelectedObject to ILibrariedObject during the
	 * assign/update. Must be called during prepare as we need the editObj prior to removing the part.
	 *
	 * @return boolean, true if all is ok
	 */
	public boolean ensureHaveLibrarySelectedObjectForUpdate()
	{
		if (librarySelectedObject != null) {
			return true;
		}

		PSDLibraryPartSelection partSelection = new PSDLibraryPartSelection();
		partSelection.setSelectedLibraryObject(libObj);
		librarySelectedObject = partSelection;

		// Note: We are not setting the SymbolDef or Footprint on the ILibrarySelectedObject
		//       See setLibrarySelectedObject comment for details

		ILibrariedObject librariedObject = ReferenceHelper.reduceToLibrariedObject(editObj, true);
		if ((librariedObject != null) && (librariedObject.getLibraryObject() != null)) {
			ILibraryObject libraryObject = CommonUtils.cast(librariedObject.getLibraryObject(), ILibraryObject.class);
			// May be null, not all Library Objects are ILibraryObject, but we're only interested in ones that re
			if (libraryObject != null) {
				final boolean custSuppPartMusMatch =
						verifyCustSuppPartMatch(partSelection, librariedObject, libraryObject);

				if (!custSuppPartMusMatch && libraryObject.getPartNumber() != null && !isSilent) {

					// here we need to prompt the user to continue...
					MessageHelper
							.showInformationMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
									ResourceMgr.getString(AssociateLibraryPartCommand.class,
											"AssociateLibraryCommand.MissingPartNumbers.title"),
									ResourceMgr.getString(AssociateLibraryPartCommand.class,
											"AssociateLibraryCommand.MissingPartNumbers.message"));
					try {

						// init/show the PSD
						ILibraryPartSelector partSelector =
								UtilsHelper.getCHSSystem().getPartsLibrary().getLibraryPartSelector(
										CAFUtils.getInstance().getWindowMgr().getDialogFrame());

						PartSelectionContext partSelectionContext = new PartSelectionContext();
						final IDesignContainer designContainer = CAFUtils.getInstance().getActiveDesignContainer();
						final ILibrarySelectionFilter selectionFilter = LibraryCriteriaHelper
								.determineSelectionFilterForSymbols(false, librariedObject.getClass(), designContainer);
						if (selectionFilter != null) {
							partSelectionContext.setSelectionFilter(selectionFilter);
						}
						final ConfigurationTypeEnum configurationTypeEnum = ConfigurationTypeEnum
								.fromDesign(designContainer, ConfigurationTypeEnum.LOGICAL);
						partSelector.showSelectDetailsDlg(partSelection, partSelectionContext, configurationTypeEnum);
					}
					catch (Exception e) {
						throw new WrappingRuntimeException(e);
					}
				}
			}
		}
		else if (librariedObject instanceof IAssembly) {
			if (!verifyStructureMatch((IAssembly) librariedObject)) {
				return false;
			}
		}

		if (librariedObject != null && librariedObject.getLibraryObject() == null) {
			// LOGIC-12671 - existing object doesn't have part assigned, set the default values from library part
			partSelection.assignDefaultCustomerPartNumber();
			partSelection.assignDefaultSupplierPartNumber();
		}

		return true;
	}

	private boolean verifyCustSuppPartMatch(PSDLibraryPartSelection partSelection, ILibrariedObject librariedObject,
			ILibraryObject libraryObject)
	{
		// Get updated Customer Part Number data using the existing number on the LibrariedObject, could be null
		ILibraryCustomerPartNumber customerPartNum = libraryObject.getCustomerPartNumber(
				librariedObject.getCustomerName(), librariedObject.getCustomerPartNumber());

		// Are we missing a part number and only have one to select? If so, auto map it..
		boolean partNumDirectMapping = customerPartNum == null && libraryObject.getCustomerPartNumbers().size() == 1;
		if (partNumDirectMapping) {
			customerPartNum = libraryObject.getCustomerPartNumbers().iterator().next();
		}

		// Is there a mismatch between our recorded part number and what's in library
		boolean partNumMismatch = customerPartNum == null && !libraryObject.getCustomerPartNumbers().isEmpty();
		//! StringUtils.isBlank( librariedObject.getCustomerPartNumber() ) ;
		// Are we missing a part number entry and there are part numbers in library
		//boolean partNumMissing = StringUtils.isBlank( librariedObject.getCustomerPartNumber() ) &&
		//		! libraryObject.getCustomerPartNumbers().isEmpty() ;

		// Get updated Supplier Part Number data using the existing number on the LibrariedObject, could be null
		ILibrarySupplierPartNumber suppNum = libraryObject.getSupplierPartNumber(
				librariedObject.getSupplierName(), librariedObject.getSupplierPartNumber());

		// Are we missing a part number and only have one to select? If so, auto map it..
		boolean suppNumDirectMapping = suppNum == null && libraryObject.getSupplierPartNumbers().size() == 1;
		if (suppNumDirectMapping) {
			suppNum = libraryObject.getSupplierPartNumbers().iterator().next();
		}

		// Is there a mismatch between our recorded supplier number and what's in library
		boolean suppNumMismatch = suppNum == null && !libraryObject.getSupplierPartNumbers().isEmpty();
		//! StringUtils.isBlank( librariedObject.getSupplierPartNumber() ) ;
		// Are we missing a supplier number entry and there are part numbers in library
		//boolean suppNumMissing = StringUtils.isBlank( librariedObject.getSupplierPartNumber() ) &&
		//		! libraryObject.getCustomerPartNumbers().isEmpty() ;

		if (suppNum == null && !libraryObject.getSupplierPartNumbers().isEmpty()) {
			for (ILibrarySupplierPartNumber suppPartNo : libraryObject.getSupplierPartNumbers()) {
				if (suppPartNo.getPreferred().isTrue()) {
					suppNum = suppPartNo;
					break;
				}
			}
		}
		partSelection.setSelectedCustomerPartNumber(customerPartNum);
		partSelection.setSelectedSupplierPartNumber(suppNum);

		return !(partNumMismatch || suppNumMismatch);
	}

	private boolean verifyStructureMatch(IAssembly assembly)
	{
		if (!(libObj instanceof ILibraryAssembly)) {
			CommandContext.setMessage("libPartIncorrectType");
			return false;
		}

		ILibraryAssembly libraryAssembly = (ILibraryAssembly) libObj;

		if (libraryAssembly.getConnectivityIncluded().isTrue()) {
			CommandContext.setMessage("COTSAssemblyAssignment");
			return false;
		}

		if (!StructureMatchService.getInstance().validateExactMatch(assembly, libraryAssembly)) {
			CommandContext.setMessage("AssemblyStructureMisMatch");
			return false;
		}
		return true;
	}

	/**
	 * Method to get options needed for Update/Replace Symbol commands. To be overriden by clients to get options via
	 * user interface as needed.
	 *
	 * @return ReplaceInstanceSymbolOptions
	 */
	protected ReplaceInstanceSymbolOptions doGetReplaceInstanceSymbolOptions()
	{
		// We only want to prompt for preserve pin positions, attributes and properties come from library part
		return new ReplaceInstanceSymbolOptions(false, false, false, true, false, false);
	}

	protected boolean shouldAssignDefaultFootprintIfNotSpecified()
	{
		return true;
	}

	/**
	 * Determines which footprint we are going to use for the update.
	 *
	 * @param device Device
	 *
	 * @return ILibraryDeviceFootprint FP for the given device or null
	 */
	@Nullable private ILibraryDeviceFootprint determineFootprint(IDevice device)
	{
		ILibraryDevice libDev = libObj instanceof ILibraryDevice ? (ILibraryDevice) libObj : null;
		if (libDev == null) {
			return null;
		}

		ILibraryDeviceFootprint fp = device.getFootprint();
		//dts0100467054 - Find if there exists a selected footprint first
		// use specified (selected) footprint if any
		if (getSpecifiedLibraryFootprint() != null) {
			return getSpecifiedLibraryFootprint();
		}

		if (!shouldAssignDefaultFootprintIfNotSpecified()) {
			return null;
		}

		return LibraryHelper.getValidLibraryDeviceFootprint(libDev, fp);
	}

	/**
	 * Can we ever perform an update or replace symbol on the logic object, as part of the update part?
	 *
	 * @return true iff we can update symbol on the object of this command
	 */
	public boolean canUpdateSymbol()
	{
		if (editObj instanceof ISchemOtherComponent) {
			return true;
		}
		// we can currently do this for all symboled objects except connectors (?)
		if (editObj instanceof IRepresentedObject) {
			IUIDObject conn = ((IRepresentedObject) editObj).getRawConnectivity();

			// dts0100487462 - Shared devices with pins number not matching with the library part does n't allow you to associate the part from library.
			// actually this defect was asking that we don't attempt to replace symbol on a parameterized instance of a shared object, during update part
			if (conn instanceof IDevice && editObj instanceof IPinList) {
				IDevice dev = (IDevice) conn;
				// the cast doesnt really conflict
				//noinspection CastConflictsWithInstanceof
				IPinList pl = (IPinList) editObj;
				if (dev.getSharedPinList() != null && pl.getParameterized() != null) {
					return false;
				}
			}

			// in other cases it's about whether the schematic instance can have a symbol
			if (conn instanceof ISymboledObject) {
				return canUpdateSymbol((ISymboledObject) conn, editObj);
			}
		}
		return false;
	}

	/**
	 * Just because we're updating a symboled object does not necessarily mean that we will - er - Update Symbol...
	 *
	 * @param object The symboled object
	 * @param editObj
	 *
	 * @return true iff we can update the symboled object
	 */
	private static boolean canUpdateSymbol(ISymboledObject object, IUIDObject schemObject)
	{
		// so these are the objects for which symbols are ignored during Update Part:
		// if the schematic object is parameterized and it defines many symbols, then don't update.
		final boolean isParameterized =
				schemObject instanceof IPinList && ((IPinList) schemObject).getParameterized() != null;
		final boolean isSymbolIdentified = !isParameterized || object.getSymbolReferences().size() <= 1;
		return !(object instanceof IConnector || object instanceof IInterconnectDevice || !isSymbolIdentified);
	}

	private void prepareHarnessConnectors(IDevice device)
	{
		deviceFootprint = determineFootprint(device);
		missingPartNumbers.clear(); // this should be empty anyway, no harm to check

		// dts0100415534 - Update Part - Disconnects mated Pins
		// disconnection of harness connectors only applies for Update part on a device with a harness connector footprint
		if (canHaveGeneratedHarnessConnectors(device)) {
			return;
		}

		Map<String, ILibraryDeviceFootprintObject> fpConns = deviceFootprint != null ?
				LibraryHelper.getFootprintConnectors(deviceFootprint, true) : Collections.emptyMap();
		Set<String> terminalMap = new HashSet<String>();
		for (IAbstractPin abstractPin : device.getPins()) {
			for (ILibraryTerminal libraryTerminal : LibraryHelper.getRingTerminalsFromLibraryHousing(abstractPin)) {
				terminalMap.add(libraryTerminal.partFullName());
			}
		}
		//dts0100777111 Scrubbing followed by CH when we close design without saving.
		//Here, only the selected pinlist is processed and its attached GHC Connectors are stored in  "harnessConnectorsToDisconnect" to disconnect them later
		//The scenario mentioned in the DR is - the pinlist has got multiple representations and the other reps too have GHC connectors attached to them.
		//With the existing code, the attached connectors of the selected PL instance are disconnected on updateLibPart action.
		//But, the attached connectors on other reps of this PL are still connected schematically. Hence, the validation error is thrown.
		//To fix this DR, all the representations of the selected pinlist are traversed and store their attached GHC Connectors in "harnessConnectorsToDisconnect"
		ILogicDesign design = diagram.getDesign();
		if (design == null) {
			assert false : "Design not exists for the diagram " + diagram.getName();
			return;
		}

		ILogicObject logicObject = ((IConnectivityRef) editObj).getConnectivity();
		for (ISchemDiagram diag : design.getDesignWideUsageMgr().getDiagramsWithRepresentation(logicObject)) {
			// Since properties don't have UIDs, using getRepresentations() for the Property doesn't work.
			for (IDiagramObject dobj : diag.getRepresentations(logicObject.getUID())) {
				if (dobj instanceof IPinList) {
					IPinList schemPinlist = (IPinList) dobj;
					IDiagramObjectIterator attached = schemPinlist.getAttachedObjects();
					while (attached.hasNext()) {
						IDiagramObject obj = attached.next();
						if (obj instanceof IPinList) {
							IPinList mate = (IPinList) obj;
							chs.cof.logical.cable.IPinList cableMate = mate.getConnectivity();
							if (cableMate instanceof IConnector && !(cableMate instanceof IDeviceConnector)) {
								String partFullName = PartNumberHelper.partFullName(cableMate);
								if (!partFullName.isEmpty() && !fpConns.containsKey(partFullName) &&
										!terminalMap.contains(partFullName)) {
									missingPartNumbers.add(partFullName);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean canHaveGeneratedHarnessConnectors(IDevice device)
	{
		boolean hasFootPrint = deviceFootprint == null ||
				deviceFootprint.getFootprintType() != ILibraryDeviceFootprint.FootprintType.HARNESS_CONNECTOR;
		if (!hasFootPrint) {
			for (IAbstractPin abstractPin : device.getPins()) {
				if (abstractPin instanceof IDevicePin && ((IDevicePin) abstractPin).isStud() &&
						device.getLibraryObject() != null) {
					return true;
				}
			}
		}
		return hasFootPrint;
	}

	/**
	 * Can we Add/Update a library part for a part that has a symbol?
	 *
	 * @return boolean True iff command should be executed
	 */
	private boolean prepareUpdatePartWithSymbol()
	{
		if (replaceSymbolCmd == null || libSymDef == null || libObj == null || params == null || pinMapper != null) {
			assert false;
			return false;
		}

		// set the library object on the parameters -- this will alter the pin mapping behavior
		params.setLibraryObject(libObj);

		// for replace symbol, pin mapping (if required) is deferred to the replace symbol cmd
		// may callback to show pin-mapping UI, sets pin-mapping on params
		if (!replaceSymbolCmd.initPinMapping(params, allowAutoMapAllByName, libSymContext)) {
			cancel();
			return false; // pin mapping not possible or cancelled
		}

		// prepare the rest of the command and issue a general error message if this fails
		if (!replaceSymbolCmd.prepare()) {
			showError(ResourceMgr.getString(AssociateLibraryPartCommand.class,
					"AssociateLibraryPartCommand.error.Failed", libObj.getPartNumber(), libSymDef.getName()));
			return false;
		}
		return true;
	}

	/**
	 * Can we Add/Update a library part for a part that does not have a symbol?
	 *
	 * @return true if no errors occured
	 */
	private boolean prepareUpdatePartWithoutSymbol()
	{
		assert libSymDef == null;
		// always check pin mapping here - might prompt for pin mapping
		return preparePinMapping();
	}

	/**
	 * Check that the pin mapping currently set on this command can be applied.
	 *
	 * @return Either returns true or asserts
	 */
	// string concatenation harmless in strings used for asserts
	@SuppressWarnings({"StringContatenationInLoop"}) private boolean verifyPinMapping()
	{
		if (mPinMappings != null) {
			assert pinMappingRequired();

			// collect cavity names
			Set<String> cavityNames = new HashSet<String>();
			LibraryHelper.collectCavityNames(libObj, cavityNames);

			// collect pin names
			Set<String> pinNames = new HashSet<String>();
			chs.cof.logical.cable.IPinList connectivity = editObj instanceof IBackshell ?
					(chs.cof.logical.cable.IPinList) editObj : ((IPinList) editObj).getConnectivity();
			ValidLibraryPinsCheck.collectPinNames(connectivity, pinNames);

			// check all map keys/values are valid
			Set<String> donePinNames = new HashSet<String>();
			Set<String> doneCavityNames = new HashSet<String>();
			//noinspection ConstantConditions
			Map<String, String> nameMappings = mPinMappings.getNameMappings();
			for (Map.Entry<String, String> e : nameMappings.entrySet()) {
				String cavityName = e.getKey();
				assert cavityNames.contains(cavityName) : "Unknown cavity name " + cavityName;
				assert doneCavityNames.add(cavityName) : "Duplicate cavity name " + cavityName;

				String pinName = e.getValue();
				assert pinNames.contains(pinName) : "Unknown pin name " + pinName; // we never add pins on pin-mapping
				assert donePinNames.add(pinName) : "Duplicate pin name " + pinName;
			}

			// not allowed to remove or rename pins on a shared object definition
			// currently this is only prevented at the action level
			if (connectivity.getSharedPinList() != null) {
				for (String pinName : pinNames) {
					String cavityName = lookupCavityName(pinName);
					if (cavityName == null) {
//						assert false : "Update Part cannot remove a pin from a shared object definition";
					}
				}
			}
		}
		return true;
	}

	protected boolean doExecute()
	{
		ILogicObject logObj = ReferenceHelper.reduceToLibrariedLogicObject(editObj);
		if (logObj != null && logObj.isEditable()) {
			Map<IMulticore, String> oldIndicators = new HashMap<>();
			if (logObj instanceof IMulticore) {
				for (IMulticore multicore : ((IMulticore) logObj).getAllMulticoresInHierarchy()) {
					oldIndicators.put(multicore, multicore.getIndicatorType());
				}
			}

			IPinList schemPinList = CommonUtils.cast(editObj, IPinList.class);
			// dts0100892406 Pin names of a connector are not automatically styled if a library part is assigned with pin graphics
			Map<IAbstractPin, String> pinGraphicCache = getPinGraphicCache(schemPinList);

			if (!associateLibraryPart(logObj)) {
				return false;
			}

			// dts0100580061 - Apply styling to PinList (currently only for device) which have library part containing symbol
//			IPinList schemPinList = CommonUtils.cast(editObj, IPinList.class);
//			if (schemPinList != null && diagram != null && replaceSymbolCmd != null) {
//				PreferenceSetHelper.applyStyleSet(schemPinList.getObjectsForStyling(),
//						PreferenceSetHelper.getStyleSet(diagram), diagram.getGrid(), false);
//			}
			if (schemPinList != null && diagram != null && !pinGraphicCache.isEmpty()) {
				// dts0100892406 Pin names of a connector are not automatically styled if a library part is assigned with pin graphics
				// Fix --> collect all the pins whose pin graphic type has changed and apply style to them
				Collection<IPin> schemPinCollection = schemPinList.getPins().getCollection();
				List<IStyleableObject> pinsToStyle = new ArrayList<IStyleableObject>();
				for (IPin schemPin : schemPinCollection) {
					IAbstractPin pin = schemPin.getConnectivity();
					if (schemPin instanceof IDeletedObject || pin instanceof IDeletedObject) {
						continue; // ignore the deleted pin
					}
					if (pin instanceof IConnectorPin || pin instanceof IDevicePin) {
						String newPinGraphic = getPinGraphic(pin);
						String oldPinGraphic = pinGraphicCache.get(pin);
						newPinGraphic = StringUtils.getTrimmed(newPinGraphic);
						oldPinGraphic = StringUtils.getTrimmed(oldPinGraphic);
						if (!StringUtils.equalsIgnoreCase(newPinGraphic, oldPinGraphic)) {
							pinsToStyle.add(schemPin);
						}
					}
				}
				if (!pinsToStyle.isEmpty()) {
					PreferenceSetHelper.applyStyleSet(pinsToStyle, diagram, false);
				}

				// Current selections could incorrectly rendered because of current edits being cleared incorrectly in
				// replaceSymbolCmd by getCommandHelper().clearDiagramUndoableContainer(diagram) in ReplaceInstanceSymbolCmd.doExecute()
				// Following code is added to render current selections gains with correct values
				if (replaceSymbolCmd != null) {
					ICapletController contr = CAFUtils.getInstance().getActiveCapletController();
					if (contr != null && contr.getCapletModel() instanceof IGfxModel) {
						((IGfxModel) contr.getCapletModel()).getDynamicGfxService().resetSelections();
					}
				}
			}

			//process all the multi-cores in hierarchy
			for (Map.Entry<IMulticore, String> entry : oldIndicators.entrySet()) {
				MulticoreLibraryHelper.redrawSchemIndicators(entry.getKey(), entry.getValue(), diagram);
			}
		}
		return true; // no clients use this result
	}

	public void updateBackShellSymbolRef(@NotNull IBackshell backshell)
	{
		ISymbolDef computedlibrarySymbolDef = getLibrarySymbolIncludingUserPref(loadSymbolDef(backshell));
		if (computedlibrarySymbolDef != null) {
			backshell.setSymbolRef(
					UtilsHelper.getCHSUtils().getSymbolFactory().constructSymbolRef(computedlibrarySymbolDef));
		}
	}

	protected void syncModularLibraryPart(ILogicObject logObj)
	{
		if (!m_syncModularConnector) {
			return;
		}

		ILibraryPartSelection libraryPartSelection = getLibrarySelectedObject();
		ILibraryObject newLibraryObject = null;
		if (libraryPartSelection != null) {
			newLibraryObject = libraryPartSelection.getSelectedObject();
		}
		LogicUtils.syncModularConnectorWithLibraryPart(logObj,newLibraryObject);
	}

	/**
	 * (un)associate the library part with this logic object, changes are also made to the object originally passed.
	 *
	 * @param logObj The logic object, derived from the schem object passed to this command
	 */
	protected boolean associateLibraryPart(ILogicObject logObj)
	{
		// always remove the library part + regenerate - even before add/update
		// this ensures that update == remove + add
		// in particular this ensures that device connectors are properly regenerated on "Update"
		// BUT - we don't do this for shared because this causes problems with blowing away shared device connectors
		// See DR415043 for further details
		ISharedPinList spl = getSharedPinList();

		if (!doLockAndRefresh(spl)) {
			return false;
		}
		try {
			if (!skipActualAssignment()) {
				IDevice dev = logObj instanceof IDevice device ? device : null;
				boolean wasAssignedHarnessFootprint = dev != null && PinListHelper.isHarnessFootprinted(dev);
				if (spl == null) {
					try (DeviceConnectorBackshellSyncOnRegeneration ignored = new DeviceConnectorBackshellSyncOnRegeneration(dev)) {
						if (libObj != null) {
							if (!validatePinAssociations()) {
								return false;
							}
							removeLibraryPart(logObj, true);

							// For UpdateICDAction with a pin mapper, pin reconcilation is yet to happen with the logic object. To prevent
							// Generator.regenerateDeviceConnectors from setting a pin out-of-sync status through the LibraryHelper.areDevicePinsInSyncWithReferencePins,
							// the insync pin validation is temporarily muted for this regeneration
							IReportValidationHandler reporter =
									GeneralReportValidationHandler.getHandle(LogTabType.TAB_DCONN);
							reporter.suspend();
							try {
								regenerate(logObj);
							}
							finally {
								reporter.resume();
							}

							updateLibraryPart(logObj, logicObject -> {}, logicObject -> {}); // add or update
						}
						else {
							removeLibraryPart(logObj, false);
						}
						regenerate(logObj);
					}
				}
				else {
					if (libObj == null) {
						removeLibraryPart(logObj, false);
					}
					else {
						// attempt shared pinlist (pin mapping) change first because this could fail (locking of shared object)
						if (!validatePinAssociations()) {
							return false;
						}
						updateLibraryPart(logObj, (t) -> {
							applySharedPinMapping();
						}, (t) -> {
						});
					}
					regenerate(logObj);
				}
				if (editObj instanceof IPinList) {
					regenerateHarnessConnectors((IPinList) editObj, wasAssignedHarnessFootprint);
				}

				// remove all symbol refs (as part number changed)
				logObj.removeAllSymbolRef();
				// refresh the object so that the correct symbol is used
				RegenerateGraphicsAction.getInstance().addObjectForRefresh(editObj);

				// refresh all connectivity attached representations
				for (ISecondaryRepresentation secRep : logObj.getAssociateRepresentations()) {
					secRep.regenerateDiagramObject();
				}
			}
			doExecutePostAssignmentTasks();
		}
		finally {
			syncModularLibraryPart(logObj);
			doFlushAndUnlock(spl);
		}
		return true;
	}

	protected boolean skipActualAssignment()
	{
		return false;
	}

	protected void doExecutePostAssignmentTasks()
	{
	}

	public boolean doExecuteAllowed()
	{
		boolean okToExecute = super.doExecuteAllowed();
		ILogicObject logicObject = ReferenceHelper.reduceToLibrariedLogicObject(editObj);
		if (okToExecute && libObj != null && logicObject != null) {
			IAssembly parentAssembly = logicObject.getAssembly();
			if (parentAssembly != null) {
				return isPartAssignableOnAssemblyChild(logicObject, parentAssembly);
			}
		}
		return okToExecute;
	}

	private boolean isPartAssignableOnAssemblyChild(ILogicObject obj, IAssembly parentAssembly)
	{
		if (parentAssembly.isPartAssigned() && !parentAssembly.isCOTSAssembly() && obj.getLibraryObject() == null) {
			Set<String> objs = LibraryHelper.getApplicableLibraryParts(obj);
			return objs.contains(libObj.getPartNumber());
		}
		return true;
	}

	private boolean validatePinAssociations()
	{
		final chs.cof.logical.cable.IPinList pinList = getPinList();
		if (pinList  instanceof IDevice && (StudPinUtils.hasStudCavity(libObj) ||
				StudPinUtils.hasStudPin(pinList))) {
			IPinMappingCompatibilityChecker pinMappingCompatibilityChecker = getPinMappingCompatibilityChecker();
			List<IPinProxy> mismatchPins = Collections.emptyList();
			if (pinMappingCompatibilityChecker != null) {
				mismatchPins = pinMappingCompatibilityChecker.getUncomapatiblePins();
			}
			if (!mismatchPins.isEmpty()) {
				PinTypeConversionChecker checker = new PinTypeConversionChecker();
				checker.executeProxyConversionChecker(mismatchPins);
				for (IPinProxy proxy : mismatchPins) {
					List<ConvertPinTypeLogEnum> result =
							checker.getPinConversionStatus(StudPinUtils.getPinFromProxy(proxy));
					if (!result.isEmpty()) {
						ConvertPinTypeLogEnum error = result.get(0);
						if (isSilent()) {
							CommandContext.setMessage(error.toString());
						}
						else {
							showStudPinMisMatchErrorMessage(proxy, error);
						}
						return false;
					}
				}
			}
		}
		return true;
	}

	private void showStudPinMisMatchErrorMessage(IPinProxy proxy, ConvertPinTypeLogEnum err)
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		JLabel actionLabel = new JLabel();
		Font newLabelFont =
				actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateLibraryPartCommand.class,
				"AssociateLibraryPartCommand." + err.toString() + ".Guidance", err.getAdditionalsArgs()));

		MessageHelper.showErrorMessage(owner,
				ResourceMgr.getString(AssociateLibraryPartCommand.class, "AssociateLibraryPartCommand.dialog.title"),
				ResourceMgr.getString(AssociateLibraryPartCommand.class,
						"AssociateLibraryPartCommand.error.headline"),
				ResourceMgr.getString(AssociateLibraryPartCommand.class,
						"AssociateLibraryPartCommand." + err.toString() + ".Info", proxy.getDisplayName(),
						err.getAdditionalsArgs()), actionLabel);
	}

	private static class NonSymbolledPinMappingCompatibilityChecker implements IPinMappingCompatibilityChecker
	{

		private Map<IReadOnlyNamedObject, IPinProxy> pinMappings;

		NonSymbolledPinMappingCompatibilityChecker(Map<IReadOnlyNamedObject, IPinProxy> pinMapping)
		{
			pinMappings = pinMapping;
		}

		@Override
		public List<IPinProxy> getUncomapatiblePins()
		{
			List<IPinProxy> mismatchPins = new ArrayList<IPinProxy>();
			for (Map.Entry<IReadOnlyNamedObject, IPinProxy> entry : pinMappings.entrySet()) {
				boolean bIsLibPinStud = StudPinUtils.isSourceProxyInPinMapperDialogAStud(entry.getKey());
				boolean bIsDevicePinStud = StudPinUtils.isTargerPinProxyInPinMappingDialogAStud(entry.getValue());
				if ((bIsLibPinStud ^ bIsDevicePinStud)) {
					mismatchPins.add(entry.getValue());
				}
			}
			return mismatchPins;
		}
	}

	protected class SymbolledPinMappingCompatibilityChecker implements IPinMappingCompatibilityChecker
	{

		private Map<IReadOnlyNamedObject, ? extends IGenericPinProxy> pinMappings;

		SymbolledPinMappingCompatibilityChecker(Map<IReadOnlyNamedObject, ? extends IGenericPinProxy> pinMapping)
		{
			pinMappings = pinMapping;
		}

		@Override
		public List<IPinProxy> getUncomapatiblePins()
		{
			List<IPinProxy> mismatchPins = new ArrayList<IPinProxy>();
			Map<String, ILibraryCavity> libraryCavityMap = getLibraryCavities();
			for (Map.Entry<IReadOnlyNamedObject, ? extends IGenericPinProxy> entry : pinMappings.entrySet()) {
				boolean bIsLibPinStud = isLibraryPinStud(entry.getKey(), libraryCavityMap);
				boolean bIsDevicePinStud = StudPinUtils.isTargerPinProxyInPinMappingDialogAStud(entry.getValue());
				if ((bIsLibPinStud ^ bIsDevicePinStud) && entry.getValue() instanceof IPinProxy) {
					mismatchPins.add((IPinProxy) entry.getValue());
				}
			}
			return mismatchPins;
		}

		private Map<String, ILibraryCavity> getLibraryCavities()
		{
			Map<String, ILibraryCavity> libraryCavityMap = new HashMap<>();
			if (libObj != null) {
				for (ILibraryCavity cavity : ((ILibraryCavityContainer) libObj).getAllCavities()) {
					libraryCavityMap.put(cavity.getName(), cavity);
				}
			}
			return libraryCavityMap;
		}

		private boolean isLibraryPinStud(IReadOnlyNamedObject pin, Map<String, ILibraryCavity> libraryCavityMap)
		{
			ILibraryCavity libraryCavity = libraryCavityMap.get(pin.getName());
			if (libraryCavity instanceof ILibraryDevicePin) {
				return ((ILibraryDevicePin) libraryCavity).getStud() == LibraryBooleanType.TRUE;
			}
			return StudPinUtils.isSourceProxyInPinMapperDialogAStud(pin);
		}
	}

	protected SymbolledPinMappingCompatibilityChecker getSymbolledPinMappingChecker(
			Map<IReadOnlyNamedObject, ? extends IGenericPinProxy> mapping)
	{
		return new SymbolledPinMappingCompatibilityChecker(mapping);
	}

	private interface IPinMappingCompatibilityChecker
	{

		List<IPinProxy> getUncomapatiblePins();
	}

	@Nullable private IPinMappingCompatibilityChecker getPinMappingCompatibilityChecker()
	{

		if (mPinMappings != null) {
			return new NonSymbolledPinMappingCompatibilityChecker(mPinMappings.getMapping());
		}
		if (replaceSymbolCmd != null) {
			return getSymbolledPinMappingChecker(params.getPinMappings());
		}
		return null;
	}

	private void regenerate(ILogicObject logObj)
	{
		final ICommandHelper commandHelper = getCommandHelper();
		DefaultGeneratorDCFeedback feedback = new DefaultGeneratorDCFeedback()
		{
			public void outputMessage(String s, boolean writtenSomething)
			{
				commandHelper.sendOutputMessage(s, getOutputTabName(), writtenSomething);
			}
		};
		new LogicObjectRegenerateHandler()
				.regenerate(logObj, preserveIncludeOnBOM, this::isRebuildFootprint,
						feedback, editObj, diagram);
	}

	private boolean isRebuildFootprint(IDevice device)
	{
		// ASSUME : Shared DCs must have been successfully updated before we get here

		// Same Symbol? Then we do generate the footprint.
		IUID deviceSymUID = devSymDef == null ? null : devSymDef.getUID();
		IUID libSymUID = libSymDef == null ? null : libSymDef.getUID();
		IPinList editPl = (IPinList) editObj;
		return (device.getFootprint() != null) || (libSymUID != deviceSymUID) ||
				(libSymUID != null && libSymUID.isEquiv(deviceSymUID)) ||
				(editPl.getParameterized() != null);
	}

	protected void regenerateHarnessConnectors(IPinList editPl, boolean wasAssignedHarnessFootprint)
	{
		//FEAT12331 - Auto create harness connectors, using the GenerateHarnessConnActionHelper
		final IDevice device = CommonUtils.cast(editPl.getConnectivity(), IDevice.class);
		if (device == null) {
			return;
		}
		final ILibraryDeviceFootprint deviceFootprintT = device.getFootprint();
		final ILogicDesign design = device.getLogicDesign();
		assert design != null;
		boolean designAllowsAutoCreation = PinListHelper.allowAutoCreation(design);
		final boolean harnessFootprintedAndAllowAutoCreation = isAutoGHCAllowed(editPl);
		final boolean ghcAllowed = harnessFootprintedAndAllowAutoCreation ||
				(deviceFootprintT == null && wasAssignedHarnessFootprint && designAllowsAutoCreation);
		//Do regenerate connectors even if library part has been removed, or footprint has changed
		if (ghcAllowed) {
			if (ConnectivityGHCHelper.isPartOfCOTSAssembly(device)) {
				getOutputWindow().sendMessage(getGHCSkippedMessage(editPl), getOutputTabName(), true);
			}
			else {
				if (editPl.getSharedObject() != null) {
					ISharedObject sharedObject = editPl.getSharedObject();
					generateHarnessConnActionHelper.generateHarnessConnectorsForSharedDevice(
							(ISharedPinList) sharedObject, editPl.getConnectivity());
				}
				else {
					final ISchemDiagram diagramT = DiagramHelper.getDiagram(editPl);
					assert diagramT != null;
					generateHarnessConnActionHelper.setDiagram(diagramT);
					generateHarnessConnActionHelper.generateHarnessConnectorsForDevice(device);
				}
			}
		}
	}

	protected IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	protected boolean isAutoGHCAllowed(IPinList editPl)
	{
		return PinListHelper.isHarnessFootprintedAndAllowAutoCreation(editPl);
	}

	private static String getOutputTabName()
	{
		return ResourceMgr.getString(ConnectivityGHCHelper.class, "UpdatePartAction.output.tab");
	}

	private static String getGHCSkippedMessage(IPinList pinList)
	{
		String link = HTMLHelper.link(pinList.getUID(), pinList.getConnectivity().getDisplayName());
		final IAssembly cotsAssembly = AssemblyUtils.getCOTSAssembly(pinList.getConnectivity());
		return ResourceMgr.getString(
				ConnectivityGHCHelper.class, "UpdatePartAction.output.ghcSkippedForCOTSDevice", link,
				cotsAssembly != null ? cotsAssembly.getName() : "");
	}

	private GeneratorParameters setupGeneratorParameters()
	{
		//
		// Try to get the preferences and use those for initialization...
		//
		GeneratorParameters gp;
		if (diagram == null) {
			gp = new GeneratorParameters(CHSConstants.PIN_SPACING);
		}
		else {
			gp = DiagramHelper.createGeneratorParameters(diagram);
		}
		return gp;
	}

	/**
	 * Add or update the library part.
	 *
	 * @param logObj The logic object, derived from the schem object passed to this command
	 */
	private void updateLibraryPart(ILogicObject logObj, Consumer<ILogicObject> preUpdate,
			Consumer<ILogicObject> postUpdate)
	{
		boolean assignLibPart = isValidToAssign(logObj);

		if (assignLibPart) {
			preUpdate.accept(logObj);
			doUpdateLibraryPart(logObj);
			postUpdate.accept(logObj);
		}
		else {
			CommandContext.setMessage("libPartIncorrectType");
		}
	}

	private void doUpdateLibraryPart(ILogicObject logObj)
	{
		// here's where we attempt to update/replace Symbol
		updatePinListFromLibraryPart(logObj);
		updateLogicObjectFromLibraryPart(logObj);
		updateSymbolInformation(logObj);
		if (m_syncPropertiesWithSource) {
			PropertyHelper.syncPropertiesWithSource(logObj);
		}
	}

	protected void updateSymbolInformation(ILogicObject logObj)
	{
		if (logObj instanceof IDevice) {
			IDevice device = (IDevice) logObj;
			ILibraryObject newLibraryObject = (ILibraryObject) logObj.getLibraryObject();
			boolean canUpdateConnectivity = canUpdateSymbol(device, editObj);
			if (newLibraryObject != null) {
				updateSymbolInfomation(device, newLibraryObject, canUpdateConnectivity);
			}
		}
		IBackshell backshell = CommonUtils.cast(logObj, IBackshell.class);
		if (backshell != null) {
			ILibraryObject libraryObject = CommonUtils.cast(logObj.getLibraryObject(), ILibraryObject.class);
			if (libraryObject != null) {
				updateBackShellSymbolRef(backshell);
			}
		}
	}

	private void updateSymbolInfomation(IDevice device, ILibraryObject newLibraryObject,
			boolean canUpdateConnectivity)
	{
		ISharedPinList spl = device.getSharedPinList();
		ISymbolDef selectedSymbol = getSelectedSymbol();
		boolean allowedUpdateSymbol = replaceSymbolCmd == null && canUpdateConnectivity;
		if (spl != null) {
			SharedPinListSymbolInfomationUpdator symbolInfomationUpdator =
					new SharedPinListSymbolInfomationUpdator(spl, (IPinList) editObj, selectedSymbol,
							allowedUpdateSymbol);
			symbolInfomationUpdator.addLibraryPartAssociatedSymbol(newLibraryObject);
		}
		else if (allowedUpdateSymbol) {
			DeviceSymbolInformationUpdator symbolInfomationUpdator =
					new DeviceSymbolInformationUpdator((IPinList) editObj, selectedSymbol);
			symbolInfomationUpdator.addLibraryPartAssociatedSymbol(newLibraryObject);
		}
	}

	@Nullable private ISymbolDef getSelectedSymbol()
	{
		ISymbolDef currentSymDef = loadSymbolDef(editObj);
		return getLibrarySymbolDef(currentSymDef);
	}

	protected final boolean isValidToAssign(ILogicObject logObj)
	{
		if (!validateLibGrptypeAttrBeforePartAssign) {
			return true;
		}

		if (logObj == null || !LibraryHelper.isLibraryGroupTypeSame(logObj, libObj)) {
			CommandContext.setMessage("libPartIncorrectType");
			return false;
		}
		return true;
	}

	/**
	 * Add/Update library part functionality that applies to all logic objects.
	 *
	 * @param logObj The logic object, derived from the schem object passed to this command
	 */
	private void updateLogicObjectFromLibraryPart(ILogicObject logObj)
	{
		// This will set PartNumber and LibraryRef, copy over library attributes
		// and properties also set customer/supplier info
		logObj.assignLibraryDetails(getLibrarySelectedObject());

		// copy across any analysis model attribute that is set on the library part
		// do not remove existing analysis model from logic object
		if (!isAnalysisModelUnset(libObj)) {
			// moattia-DesignWideShareInto: if the logic object can hold multiple symbols, we don't want to set the normal
			// analysis model string. Instead we want to create an internal analysis block in the device.
			// the reason is that device can also hold multiple analysis models and we want to have a unified place
			// to hold the symbol models in. Note that devices can hold the multiple models due to having multiple symbols
			if (logObj instanceof IDevice && ((ISymboledObject) logObj).canMaintainMultipleSymbols()) {
				PinListHelper.updateDeviceAnalysisModel(libObj.getAnalysisModel(), (IDevice) logObj);
			}
			else {
				logObj.setAnalysisModel(libObj.getAnalysisModel());
			}
		}
		if (logObj instanceof IConnector && libObj instanceof ILibraryBaseConnector) {
			ILibraryBaseConnector libraryBaseConnector = (ILibraryBaseConnector) libObj;
			if (((IConnector) logObj).isModularParent() &&
					libraryBaseConnector.getReferencedConnector() != null) {
				boolean isPurchased = libraryBaseConnector.getPurchasedPart().isTrue();
				ModularConnectorHelper
						.updateIncludeOnBOMOnAeroModularConnector((IBOMObject) logObj, libraryBaseConnector,
								isPurchased, true);
				((IConnector) logObj).getChildConnectors().forEach(aChild -> {
					ILibraryBaseConnector childLibraryPart =
							CommonUtils.cast(aChild.getLibraryObject(), ILibraryBaseConnector.class);
					if (childLibraryPart != null) {
						ModularConnectorHelper
								.updateIncludeOnBOMOnAeroModularConnector(aChild,
										childLibraryPart,
										isPurchased, false);
					}
				});
			}
		}
	}

	/**
	 * Add/update library part functionality that applies only to multicores.
	 *
	 * @param multicore The multicore, derived from the schem object passed to this command
	 */
//	private void updateMulticoreFromLibraryPart(IMulticore multicore)
//	{
//		if (multicore.getSharedMulticore() == null) {
//			diagram.bind(multicore);
//		}
//	}

	/**
	 * Add/update library part functionality that applies only to pinlists.
	 *
	 * @param pinlist The pinlist, derived from the schem object passed to this command
	 */
	private void updatePinListFromLibraryPart(ILogicObject pinlist)
	{
		if (replaceSymbolCmd != null) {
			// update or replace symbol
			replaceSymbolCmd.setShouldRetainBackshellTerminations(true);
			replaceSymbolCmd.execute();
		}
		else if (mPinMappings != null) {
			// Pin mapping can only happen if we are not updating the symbol

			applyPinMapping();
		}

		if (pinlist instanceof IDevice) {
			updateFootprint((IDevice) pinlist);
		}

		//
		// Add the properties onto the device and its pins.
		//
		PropertyCopier.copyAllAsReferencedProperties(pinlist, libObj); // Copy All, and Make them referenced
		// copy over the Library Object's attributes.
		if (pinlist instanceof chs.cof.logical.cable.IPinList) {
			//CS-4741 Properties dialog/AssociateLibraryPartCommand/ Retrieving library cavity info in loop
			LibraryDevicePinPropertiesLoader loader = new LibraryDevicePinPropertiesLoader();
			loader.loadPinProperties(LibraryHelper.getCavities(libObj));

			PropertyCopier.copyCavityAttributesAndProperties((chs.cof.logical.cable.IPinList) pinlist, libObj);
		}
	}

	protected boolean doLockAndRefresh(@Nullable ISharedPinList sharedPinList)
	{
		if (sharedPinList != null) {
			return CTFLockUpdateHelper.lock(sharedPinList);
		}
		return true;
	}

	protected void doFlushAndUnlock(@Nullable ISharedPinList sharedPinList)
	{
		if (sharedPinList != null) {
			//we may have deleted shared device connector and/or pins so
			//this is warranting me to re-create the shared device.
			//otherwise these objects are not getting deleted from DB.
			//we can't recreate if the shared pinlist is in new state or in some other state i.e pending save.
			//In UT also this may happen. so in that case we will save first and then recreate.
			//Will this cause any performance hit? I hope this won't because it would be under transaction boundary.
			//and COG should be handling it. validation on shared device connector is warranting me to do this. chandras.
			//spl.save();
			//SharedPinListHelper.recreatePinlist(sharedPinList);
			//SharedPinListHelper.unlock(sharedPinList);
			sharedPinList.saveAndUnlock();
			ISharedObjectMgr sharedObjectMgr = sharedPinList.getSharedObjectMgr();
			if (sharedObjectMgr != null) {
				sharedObjectMgr.fireChangeEvent(Set.of(sharedPinList.getUID()));
			}
		}
	}

	/**
	 * Update the device, using the default footprint of the part.
	 *
	 * @param device The device, derived from the schem object passed to this command
	 */
	private void updateFootprint(IDevice device)
	{
		// update the device footprint ID/description with the footprint chosen from the part
		if (deviceFootprint == null) {
			device.setFootprintId(null);
			device.setFootprintDescription(null);
		}
		else {
			device.setFootprintId(deviceFootprint.getUID());
			device.setFootprintDescription(deviceFootprint.getFootprintName());
		}

		// if the device has any harness connectors, we might have to disconnect them (we found these during the prepare)
		if (!PinListHelper.isHarnessFootprintedAndAllowAutoCreation((IPinList) editObj)) {
			for (String missingPartNumber : missingPartNumbers) {
				showError(ResourceMgr.getString(AssociateLibraryPartCommand.class,
						"AssociateLibraryPartCommand.MissingPartNumber.message", missingPartNumber, device.getName()));
			}
		}
	}

	/**
	 * Apply the pin-mapping previously specified. Pins that are mapped from a library pin are renamed if required, pins
	 * that are not mapped are deleted.
	 */
	private void applyPinMapping()
	{
		chs.cof.logical.cable.IPinList conn = editObj instanceof IBackshell ? (chs.cof.logical.cable.IPinList) editObj :
				((IPinList) editObj).getConnectivity();
		ISharedPinList spl = conn.getSharedPinList();
		if (spl != null) {
			// pin mapping for shared pinlists is completely different
			// this is currently handled elsewhere, regardless of what the pin mapping returns
			return;
		}
		ApplyLibraryPartOnPinlist applyLibraryPartOnPinList = new ApplyLibraryPartOnPinlist();
		final List<IAbstractPin> mUnmappedPins = new ArrayList<IAbstractPin>();
		if (applyLibraryPartOnPinList.applyPinMapping(conn, mPinMappings, mUnmappedPins)) {
			applyLibraryPartOnPinList.deletePins(mUnmappedPins, diagram);
		}
	}

	/**
	 * If this command is being performed on a shared pinlist, return it
	 *
	 * @return The possibly null SPL
	 */
	@Nullable private ISharedPinList getSharedPinList()
	{
		ISharedPinList spl = null;
		chs.cof.logical.cable.IPinList pl = getPinList();
		if (pl != null) {
			spl = pl.getSharedPinList();
		}
		return spl;
	}

	/**
	 * The library part has pins that are not on the shared object definition.
	 */
	protected void applySharedPinMapping()
	{
		if (mPinMappings == null && replaceSymbolCmd != null) {
			return; // pin mapping deferred to Update/Replace Symbol
		}

		chs.cof.logical.cable.IPinList conn = getPinList();
		if (conn == null) {
			// shouldn't happen
			assert false;
			return;
		}
		ISharedPinList spl = conn.getSharedPinList();
		if (spl == null) {
			assert false;
			return; // shouldn't happen either
		}
		if (libObj == null) {
			assert false;
			return; // shouldn't happen either
		}

		// watch out for shared inlines
		// any pins added to one half must be added to other
		// in this case we just need to pick out one of the mates (but we don't call the naughty getMate method)
		ISharedPinList splMate = null;
		if (spl instanceof ISharedConnector && conn instanceof IGenericInlineConnector) {
			IGenericInlineConnector inline = (IGenericInlineConnector) conn;
			Set<IConnector> mates = inline.getMates();
			if (!mates.isEmpty()) {
				IConnector connMate = mates.iterator().next();
				splMate = connMate.getSharedPinList();
			}
		}

		Map<String, ILibraryCavity> nameToLibraryCavity = new HashMap<String, ILibraryCavity>();
		if (libObj instanceof ILibraryCavityContainer) {
			ILibraryCavityContainer libraryCavityContainer = (ILibraryCavityContainer) libObj;
			for (ILibraryCavity libraryCavity : libraryCavityContainer.getAllCavities()) {
				nameToLibraryCavity.put(libraryCavity.getName(), libraryCavity);
			}
		}
		Map<ISharedPin, ILibraryCavity> sharedPinToCavityLookup = new HashMap<>();
		// rename any existing pins according to the pin mapping
		Set<String> pinNames = new HashSet<String>();
		for (ISharedPinIterator it = spl.getPins(); it.hasNext(); ) {
			ISharedPin spin = it.getNext();
			String pinName = spin.getName();
			String cavityName = lookupCavityName(pinName);
			if (cavityName == null) {
				// normally we cant get here because the UI prevents removal of shared pins
				// but for shared inlines when we get here when we add/update for both halves
			}
			else if (!cavityName.equals(pinName)) {
				spin.setName(cavityName);
			}
			pinNames.add(spin.getName());
			if (!StringUtils.isBlank(cavityName)) {
				ILibraryCavity mappedCavity = nameToLibraryCavity.get(cavityName);
				if (mappedCavity != null) {
					sharedPinToCavityLookup.put(spin, mappedCavity);
				}
			}
		}

		// for shared pin mapping, all cavity names must be mapped to a pin
		// therefore any cavity name that does not exist on the shared definition at this stage must be added as a shared pin
		for (ILibraryCavity cavity : LibraryHelper.getCavities(libObj)) {
			String name = cavity.getName();
			if (!pinNames.contains(name)) {
				ISharedPin sharedPin = createAndAddSharedPin(name, spl);
				if (splMate != null) {
					//we need to associate the correct mated cavity to the mated pin list.
					ISharedPin matePin = createAndAddSharedPin(name, splMate);
					sharedPin.setMatePin(matePin);
					matePin.setMatePin(sharedPin);
					if (!StringUtils.isBlank(name)) {
						sharedPinToCavityLookup.put(matePin, cavity);
					}
				}
				if (!StringUtils.isBlank(name)) {
					sharedPinToCavityLookup.put(sharedPin, cavity);
				}
			}
			//shared pin does not contain the library reference and hence the match is not possible,
			//if the match cannot be established throgh names using mPinMappings.
			//Do not use the name match between cavity and shared pin.

		}
		//CS-4741 Properties dialog/AssociateLibraryPartCommand/ Retrieving library cavity info in loop
		LibraryDevicePinPropertiesLoader loader = new LibraryDevicePinPropertiesLoader();
		loader.loadPinProperties(LibraryHelper.getCavities(libObj));
		for (Map.Entry<ISharedPin, ILibraryCavity> entry : sharedPinToCavityLookup.entrySet()) {
			// dts0101241501 library part on a shared device that has a device side connector footprint, they then noticed that the pin properties were incorrect
			PropertyCopier.copyCavityAttributesAndPropertiesOntoPin(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * This is a bit like the one in SharedPinListEditUtils except that it does not need a "PinProxy".
	 *
	 * @param name The name of the shared pin (if it is taken, a new name will be found)
	 * @param spl The shared pinlist to which the pin must be added
	 *
	 * @return The newly created shared pin
	 */
	private static ISharedPin createAndAddSharedPin(String name, ISharedPinList spl)
	{
		// create a shared new pin to the SPL
		IUID uid = FactoryMgr.getCommonFactory().createUID();
		ISharedPin sharedPin = FactoryMgr.getSharedFactory().createSharedPinForOwner(uid, spl);
		sharedPin.setName(name);
		PropertyTemplateHelper.AssociateAutoAssignProperties(sharedPin, spl.getProject(), false);
		spl.addPin(sharedPin);
		return sharedPin;
	}

	private boolean checkPartSymbolMismatch(ILibraryObject libraryObject, ISymbolDef symbolDef)
	{
		if (symbolDef == null) {
			return true;
		}

		StringBuilder sout = new StringBuilder();
		if (!ValidLibraryPinsCheck.checkPartSymbolMismatch(libraryObject, symbolDef, sout)) {
			showError(sout.toString());
			return false;
		}

		return true;
	}

	@Nullable private static ISymbolDef loadSymbolDef(IUIDObject obj)
	{
		ISymbolDef symDef = null;
		ISymbolRef symRef = determineSymbolRef(obj);
		if (symRef != null) {
			symDef = SymbolUtils.loadSymbolDef(symRef.getSymbolUID());
		}
		return symDef;
	}

	@Nullable private static ISymbolRef determineSymbolRef(IUIDObject obj)
	{
		ISymbolRef symRef = null;
		if (obj instanceof IPinList) {
			symRef = ((ISymboledSchemPinList) obj).getSymbolRef();
		}
		else if (obj instanceof ISymboledObject) {
			symRef = ((ISymboledObject) obj).getSymbolRef();
		}
		return symRef;
	}

	private boolean isFootprintIntact(IDevice device)
	{
		return (deviceFootprint != null && deviceFootprint.getUID() == device.getFootprintId());
	}

	protected void removeLibraryPart(ILogicObject logObj, boolean retainFootprintIfNotChanged)
	{
		new RemoveLibraryPartHandler()
				.removeLibraryPart(logObj, retainFootprintIfNotChanged, this::isFootprintIntact, editObj);
	}

	// TODO jacobt FEAT3083.1 : Move AssociateLibraryPartCommand.isAnalysisModelUnset, available on IAnalysable?

	public static boolean isAnalysisModelUnset(IAnalysable analysable)
	{
		return StringUtils.isBlank(analysable.getAnalysisModel());
	}

	/**
	 * Return the cavity name that is mapped to this pin name, or null if no mapping exists.
	 *
	 * @param pinName The cavity name to lookup
	 *
	 * @return The possibly null cavity name
	 */
	@Nullable protected String lookupCavityName(String pinName)
	{
		String cavityName = null;
		if (mPinMappings != null) {
			cavityName = mPinMappings.getName(pinName);
		}
		return cavityName;
	}

	/**
	 * Is pin-mapping required for this particular command?
	 * <p>
	 * Assumes that prepare() has already been called.  If so, clients must determine the pin-mapping and call
	 * setPinMapping on this object.
	 *
	 * @return true iff pin mapping is required
	 */
	public boolean pinMappingRequired()
	{
		boolean result = false;
		// replace symbol does it's own thing for pin-mapping
		if (libObj != null && libObj.getGroupName() != ILibraryObject.GroupType.ASSEMBLY && libSymDef == null) {
			if (editObj instanceof IPinList || editObj instanceof IBackshell) {
				// IJ gets it wrong here
				//noinspection OverlyStrongTypeCast
				chs.cof.logical.cable.IPinList connectivity =
						editObj instanceof IBackshell ? (IBackshell) editObj : ((IPinList) editObj).getConnectivity();
				if (doesPinListHasPins(connectivity)) {
					// we never add pins via pin mapping, but we may remove pins when the part has none.
					if (!(libObj instanceof ILibrarySolderSleeve || libObj instanceof ILibraryUltrasonicWeld)) {
						result = true;
					}
				}
			}
		}
		return result;
	}

	public static boolean doesPinListHasPins(chs.cof.logical.cable.IPinList connectivity)
	{
		ISharedPinList sharedPinList = connectivity.getSharedPinList();
		return (sharedPinList != null && sharedPinList.getNumPins() > 0) || connectivity.getNumPins() > 0;
	}

	/**
	 * Show pin-mapping prompt if required, set resulting pin-mapping on the cmd.
	 *
	 * @return false if pin-mapping is required but cannot be preformed (e.g dialog cancelled, shared object
	 * restrictions) true otherwise
	 */
	protected boolean preparePinMapping()
	{
		if (pinMappingRequired()) {
			assert replaceSymbolCmd == null; // handled elsewhere
			// sometimes we apply the default mapping without showing the Pin Mapping dialog (e.g. 1-1 mappings)
			chs.cof.logical.cable.IPinList connectivity = editObj instanceof IBackshell ?
					(chs.cof.logical.cable.IPinList) editObj :
					(chs.cof.logical.cable.IPinList) ((IConnectivityRef) editObj).getConnectivity();
			Map<IReadOnlyNamedObject, IPinProxy> mapping = getPinMapping(connectivity);
			if (mapping == null) {
				// dialog was cancelled - back out of command
				cancel();
				return false;
			}
			setPinMapping(mapping);
		}
		return true;
	}

	@Nullable private Map<IReadOnlyNamedObject, IPinProxy> getPinMapping(chs.cof.logical.cable.IPinList pinList)
	{
		if (exactPinNumberMatchRequired(pinList) && !isPinCountSame(libObj, pinList)) {
			if (CommandContext.isContext(CommandContext.class)) {
				CommandContext.setMessage("logic.numberOfcavitiesNotEqual");
			}
			else {
				showError(ResourceMgr.getString(AssociateLibraryPartCommand.class,
						"AssociateLibraryPartCommand.error.PartCavityPinMismatch", libObj.getPartNumber(),
						pinList.getName()));
			}
			return null;
		}

		Map<IReadOnlyNamedObject, IPinProxy> mapping =
				LibraryPinMapperDialog.autoMap(libObj, pinList, allowAutoMapAllByName);
		if (mapping == null && pinMapper != null) {
			// prompt for pin-mapping - typically this is the pin mapping UI
			// we currently don't support Update Part for multiple pinlists
			mapping = pinMapper.promptPinMapping(false);
		}
		return mapping;
	}

	private boolean isPinCountSame(ILibraryObject libraryObject, chs.cof.logical.cable.IPinList pinList)
	{
		return libraryObject.getNumCavities() == pinList.getNumPins();
	}

	private boolean exactPinNumberMatchRequired(chs.cof.logical.cable.IPinList connectivity)
	{
		if (connectivity instanceof ILibraryPartMatchCriteriaProvider) {
			return ((ILibraryPartMatchCriteriaProvider) connectivity).isExactPinCountMatchWithLibraryCavityRequired();
		}
		return false;
	}

	public PinMappingInfoHelper createPinMappingInfoHelper(chs.cof.logical.cable.IPinList pl, String knownChanges)
	{
		// When we are not using update symbol then:
		boolean isShared = pl.getSharedPinList() != null;
		boolean addAllowed = isShared;
		boolean removeAllowed = !isShared;

		if (libSymDef == null) {
			if (devSymDef != null) {
				// Cannot add or remove pins in all cases where the the library part has no symbol but the instance does:
				removeAllowed = false;
			}
		}
		else {
			// actually the client code currently ensures we never get into this case
			// but we would need to do this if we ever got here:
			addAllowed = true;
		}
		PinMappingInfoHelper mappingInfoHelper =
				new PinMappingInfoHelper(isShared, addAllowed, removeAllowed, true, knownChanges);
		mappingInfoHelper.setPartialMappingAllowed(false);
		return mappingInfoHelper;
	}

	public boolean isCancelled()
	{
		return super.isCancelled();
	}

	@NotNull private Map<IAbstractPin, String> getPinGraphicCache(IPinList schemPinList)
	{
		Map<IAbstractPin, String> oldPinVsPinGraphicType = new HashMap<IAbstractPin, String>();
		if (schemPinList != null) {
			// dts0100892406 Pin names of a connector are not automatically styled if a library part is assigned with pin graphics
			Collection<IPin> schemPinCollection = schemPinList.getPins().getCollection();
			oldPinVsPinGraphicType = new HashMap<IAbstractPin, String>(schemPinCollection.size());
			for (IPin schemPin : schemPinCollection) {
				IAbstractPin pin = schemPin.getConnectivity();
				if (pin instanceof IConnectorPin || pin instanceof IDevicePin) {
					String oldPinGraphic = getPinGraphic(pin);
					oldPinVsPinGraphicType.put(pin, oldPinGraphic);
				}
			}
		}
		return oldPinVsPinGraphicType;
	}

	@Nullable private String getPinGraphic(IAbstractPin pin)
	{
		if (pin instanceof IConnectorPin) {
			return ((IConnectorPin) pin).getPinGraphic();
		}
		if (pin instanceof IDevicePinAttributeProvider) {
			return ((IDevicePinAttributeProvider) pin).getPinGraphic();
		}
		return null;
	}

	public boolean isSymbolPresentForThisInstance(@NotNull IPinList pinList)
	{
		// Todo: symbolDef - can't rely on because, not consistently managed. (Not persisted, Non-shared, ...?)
		return pinList.getParameterized() == null;
	}

	public void setSyncModularConnectors(boolean bSyncModularConnectorsFromLibraryObject)
	{
		m_syncModularConnector = bSyncModularConnectorsFromLibraryObject;
	}

	public void setSyncPropertiesWithSource(boolean bSyncPropertiesWithSource)
	{
		m_syncPropertiesWithSource = bSyncPropertiesWithSource;
	}

	public void setIsSkeletonDesignChecker(@NotNull Predicate<IDesignContainer> isSkeletonDesign)
	{
		mIsSkeletonDesign = isSkeletonDesign;
	}
}
