/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
package chs.caplets.shared.properties;

import chs.bridges.BridgeHelper;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.GfxEditFormBuilder;
import chs.caf.caplet.helpers.IGfxEditFormProvider;
import chs.caf.caplet.helpers.LibraryControl;
import chs.caf.caplet.helpers.PropertiedSetHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.capitalmanager.appserver.LockException;
import chs.caplets.logic.actions.UpdatePartAction;
import chs.caplets.logic.actions.shared.SharedObjectAvailabilityReporter;
import chs.caplets.logic.commands.AssociateLibraryPartCommand;
import chs.caplets.logic.commands.LogicObjectRegenerateHandler;
import chs.caplets.logic.commands.RemoveLibraryPartHandler;
import chs.cof.changepolicy.IChangePolicyMgr;
import chs.cof.draw.FontStyleEnum;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IFont;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectContainer;
import chs.cof.draw.IGrid;
import chs.cof.draw.IText;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IPropertiedCommentSymbol;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IPropertiedText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.ISegmentCollector;
import chs.cof.drawplus.IXRefText;
import chs.cof.drawplus.layout.ISymbolDatumLayout;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.IFunctionPin;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IPanelLayoutDuctComponent;
import chs.cof.logical.cable.IPanelLayoutOtherComponent;
import chs.cof.logical.cable.IPanelLayoutRailComponent;
import chs.cof.logical.cable.IRingTerminal;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IFunctionLogicDiagram;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemSector;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedDevicePin;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDevicePin;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cof.project.objectinfo.IObjectTypeInfo;
import chs.cof.project.objectinfo.IObjectTypeInfoIterator;
import chs.cof.project.objectinfo.IObjectTypeInfoMgr;
import chs.cof.topology.diagram.ITopoBundleRegion;
import chs.cofUtils.logical.concurrency.IPropertiesConcurrencySet;
import chs.cofUtils.logical.concurrency.PropertiesConcurrencyHelper;
import chs.cog.ICOGLockable;
import chs.common.ILockable;
import chs.common.IProjectPreferenceMgr;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.common.IUIDProvider;
import chs.common.PreferenceContext;
import chs.common.UIDObjectCollection;
import chs.common.styles.IStyleableDiagram;
import chs.ctf.caf.ui.EditProperty;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.caf.utils.PropertiesClientUtilsBase;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.AppInfo;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.ui.MessageHelper;
import chs.utility.DiagramHelper;
import chs.utility.ProjectHelper;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.persist.promise.IPromiseBatchLock;
import chs.utility.persist.promise.PromiseFactory;
import chs.utility.preferences.PropertyStyleAppearance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * A PropertiedSet of logic objects.
 */
public class PropertiedSet extends PropertiedSetHelper
{

	static {

		GfxEditFormBuilder.getInstance().registerProvider(
				IGfxEditFormProvider.class,
				GfxEditFormBuilder.GfxEditFormBuilderContext.createInstance(PropertiedSet.class),
				new SharedGfxEditFormProvider());
	}

	protected String m_setString = null;
	private IObjectTypeInfoMgr m_objectTypePropertiesMgr;
	private boolean m_hasSharedPin;
	private boolean m_hasSharedPinList;
	private boolean m_hasSharedConductor;
	private boolean m_sharedObjectLockFailure = false;
	private Set<ISharedObject> m_sharedObjects = Collections.emptySet();
	private Set<ISharedObject> m_refreshedSharedObjects = Collections.emptySet();
	private ILogicModel m_model;
	private ISchemDiagram m_diagram;
	protected SetMap<IPropertiedObject, IProperty> m_deletedSharedObjects = new SetMap<IPropertiedObject, IProperty>();

	@Nullable
	private Class<?> m_propertiedClassRep = null;

	private Collection<AssociateLibraryPartCommand> associatePartCommands;
	private boolean m_isConnArtificiallyReadOnly = false;
	private boolean m_isGfxArtificiallyReadOnly = false;
	private static boolean m_mimicRefreshAtLock =false;
	public PropertiedSet(SelectSet selections, ICapletModel safemodel)
	{
		this(selections, safemodel, true, true);
	}

	public PropertiedSet(SelectSet selections, ICapletModel safemodel, boolean willEditSharedObjects,
			boolean checkArtificallyReadOnly)
	{
		m_model = (ILogicModel) safemodel;
		m_diagram = m_model.getDiagram();
		IProject project = CAFUtils.getInstance().getCurrentProject();
		m_objectTypePropertiesMgr = project == null ? null : project.getObjectTypeInfoMgr();

		SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects();
		Set<IUIDObject> filtereObjectSet = new LinkedHashSet<>();
		while (iter.hasNext()) {
			// make sure this is an IDiagramObject, there are selectable objects that
			// are not
			IUIDObject obj = iter.getNext();
			if (obj == null ||
					// If object is IDiagramText, it should not be added to editset if its parent exists in selection(dt725697)
					shouldExcludeFromSelection(selections, obj)) {

				continue;
			}
			filtereObjectSet.add(obj);
		}

		if (checkArtificallyReadOnly && m_model.isEditable()) {
			IPropertiesConcurrencySet filteredSet =
					PropertiesConcurrencyHelper.filterSelections(filtereObjectSet);
			filtereObjectSet.clear();
			filtereObjectSet.addAll(filteredSet.getObjects());
			m_isConnArtificiallyReadOnly = filteredSet.isConnectivityArtificiallyReadOnly();
			m_isGfxArtificiallyReadOnly = filteredSet.isGfxArtificiallyReadOnly();
		}

		for (IUIDObject obj : filtereObjectSet) {
			m_propertiedClassRep = getRepresentativePropertiedClass(m_propertiedClassRep, obj);

			// Get the name and class of the object
			Pair<String, Class<?>> nameClassList = getSetNameAndClass(obj);
			// Keep the object class
			m_setClassColl.add(nameClassList.getSecond());

			if (obj instanceof IDiagramObject) {
				if (obj instanceof IShieldBodyHookup) {
					continue;
				}
				IDiagramObject sobj = (IDiagramObject) obj;
				sortObject(sobj);
				determineSetString(sobj);
			}
			else if (obj instanceof ILogicObject) {
				ILogicObject lobj = (ILogicObject) obj;
				sortObject(lobj);
				determineSetString(lobj);
			}
		}
		if (willEditSharedObjects) {
			determineSharedObjects();
		}
	}

	@Override public boolean isArtificallyReadOnly()
	{
		return m_isConnArtificiallyReadOnly && m_isGfxArtificiallyReadOnly;
	}

	public boolean isConnectivityEditable()
	{
		// Connectivity is editable if the model is editable and there are no frozen shaeed object in the set.
		if (!m_model.isEditable() || m_sharedObjectLockFailure) {
			return false;
		}
		else {
			if (PropertiesClientUtilsBase.isRestrictedByFrozen(m_sharedObjects, hasSharedPin())) {
				return false;
			}
			return !m_isConnArtificiallyReadOnly;
		}
	}

	public boolean areGraphicsEditable()
	{
		// Graphics are editable if the model is editable.
		return m_model.isEditable() && !m_isGfxArtificiallyReadOnly;
	}

	@Nullable
	public Class<?> getRepresentativePropertiedClass()
	{
		return m_propertiedClassRep;
	}

	@Nullable
	private Class<?> getRepresentativePropertiedClass(@Nullable Class<?> pClass, Object o2)
	{
		Class<?> oClass = getPropertiedClass(o2);

		// UXFWK-751 - defensive fix
		if (pClass == null || oClass == null) {
			return oClass;
		}

		if (pClass.isAssignableFrom(oClass)) {
			return pClass;
		}
		if (oClass.isAssignableFrom(pClass)) {
			return oClass;
		}

		return IPropertiedObject.class;
	}

	@Nullable
	protected Class<?> getPropertiedClass(Object o)
	{
		if (o instanceof ISegment) {
			o = ((ISegment) o).getConductor();
		}
		if (o instanceof IHighwaySegment) {
			o = ((IHighwaySegment) o).getHighway();
		}

		if (o instanceof IRepresentedObject) {
			o = ((IRepresentedObject) o).getRawConnectivity();
		}

		// UXFWK-751 - defensive fix
		if (o == null) {
			return null;
		}
		Class<?> oClass = o.getClass();

		// Overbraid is not included in object type properties!
		if (IOverbraid.class.isAssignableFrom(oClass)) {
			return IOverbraid.class;
		}
		if (ISchemSector.class.isAssignableFrom(oClass)) {
			return ISchemSector.class;
		}
		if ((IConnector.Statics.isRingTerminalTypeConnector((IUIDProvider) o))) {
			return IRingTerminal.class;
		}

		Class<?> propertiedClass = IPropertiedObject.class;
		for (IObjectTypeInfoIterator otpIt = m_objectTypePropertiesMgr.getAll(); otpIt.hasNext(); ) {
			IObjectTypeInfo otp = otpIt.getNext();
			Class<?> association = otp.getAssociation();
			if (association != null && association.isAssignableFrom(oClass)) {
				// found a specific one. Is this more specific than previous?
				if (propertiedClass.isAssignableFrom(association)) {
					propertiedClass = association;
				}
			}
		}
		return propertiedClass;
	}

	public PartAssociationStatus preparePartAssociation(ILibraryObject libObj, ILibraryPartSelection selObj)
	{
		// prepare some commands to do the association
		associatePartCommands = new ArrayList<AssociateLibraryPartCommand>();

		// Currently we only use commands to prepare part associations for pinlists
		// Multicores are still handled here for now
		// TODO jacobt FEAT3083.1 Should use AssocicateLibraryPartCommand for objects other than pinlists (i.e. multicores)

		// dts0100500680 - commented out as Pins may pass this way and
		// preparePinListPartAssociation does an instanceof on IPinList anyway.
		// assert chs.cof.logical.cable.IPinList.class.isAssignableFrom(getRepresentativePropertiedClass());

		return preparePinListPartAssociation(libObj, selObj);
	}

	/**
	 * Prepare a command to perform the library part association for the pinlist(s) in this propertied set.
	 */
	private PartAssociationStatus preparePinListPartAssociation(ILibraryObject libObj, ILibraryPartSelection selObj)
	{
		// do the basic preparation for each command
		Set<ISharedPinList> doneShared = new LinkedHashSet<ISharedPinList>();
		// TODO jacobt QSP : Part association should be per connectivity
		// we still pass the schematic here but actually all schems (should) get updated when passing a connectivity pinlist
		Set<chs.cof.logical.cable.IPinList> donePinLists = new HashSet<chs.cof.logical.cable.IPinList>();
		for (Iterator<IUID> iter = iterator(); iter.hasNext(); ) {
			IUID uid = iter.next();
			IUIDObject editObj = UIDMgr.getObject(uid);
			ILogicObject logic = ReferenceHelper.reduceToLogicObject(editObj);
			// We need the editable object to be a schem not a connectivity object.
			// If the proposed editObj is also a connectivity object then we can't
			// create the associate command. dts0100532569 && dts0100529174
			if (logic instanceof chs.cof.logical.cable.IPinList && (!logic.equals(editObj)|| CommonUtils.cast(logic,IBackshell.class)!=null)) {
				chs.cof.logical.cable.IPinList pinlist = (chs.cof.logical.cable.IPinList) logic;
				if (!donePinLists.add(pinlist)) {
					continue;
				}
				ISharedPinList spl = pinlist.getSharedPinList();
				if (spl != null && !doneShared.add(spl)) {
					continue;
				}

				// use a command to do basic preparation for the pinlist - shared or otherwise
				// return on first failure
				if (logic instanceof IDevice && libObj != null) {
					int nStudCavities = 0;
					for (ILibraryCavity object : LibraryHelper.getCavities(libObj)) {
						if (object instanceof ILibraryDevicePin &&
								((ILibraryDevicePin) object).getStud().toBoolean()) {
							nStudCavities++;
						}
					}
					int nStudPins = 0;
					IDevice device = (IDevice) logic;
					if (device.isShared()) {
						ISharedDevice sharedDevice = (ISharedDevice) device.getSharedPinList();
						assert sharedDevice != null;
						for (ISharedPin sharedDevicePin : sharedDevice.getPins()) {
							if (((ISharedDevicePin) sharedDevicePin).isStud()) {
								nStudPins++;
							}
						}
					}
					else {
						for (IAbstractPin devicePin : device.getPinCollection()) {
							if (devicePin.isStudPin()) {
								nStudPins++;
							}
						}
					}

					if (nStudPins > nStudCavities) {
						JLabel actionLabel = new JLabel();
						Font newLabelFont =
								actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
						actionLabel.setFont(newLabelFont);
						actionLabel.setText(ResourceMgr.getString(PropertiedSet.class,
								"PropertiedSet.InvalidPartAssigned.Guidance"));
						MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
								ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.InvalidPartAssigned.title"),
								ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.InvalidPartAssigned.heading"),
								ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.InvalidPartAssigned.Message"),
								actionLabel);
						return PartAssociationStatus.CANCELLED;
					}
				}

				if (spl != null && libObj != null) {
					final ILogicDesign logicDesign = logic.getLogicDesign();
					// Cancel 'Update Part' action in properties dialog on a shared pinlist (all types) instance which is restricted to current user
					final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
					if (!new SharedObjectAvailabilityChecker().check(spl, logicDesign, reporter, false)) {
						return PartAssociationStatus.CANCELLED;
					}
				}

				AssociateLibraryPartCommand cmd =
						new AssociateLibraryPartCommand(new CAFCommandHelper(), m_diagram, editObj, libObj, selObj);
				UpdatePartAction.setupPinMapper(cmd);
				if (!cmd.prepare()) {
					if (cmd.isCancelled()) {
						return PartAssociationStatus.CANCELLED;
					}
					return PartAssociationStatus.FAILURE;
				}

				// command is prepared - execute it later if required
				associatePartCommands.add(cmd);
			}
		}

		// basic prepare was OK - now for setup pin mapping for each command
		// TODO jacobt FEAT2145: Pin mapping is done via callback from commands - need to replace this functionality:
//		doneShared.clear();
//		for (Iterator<AssociateLibraryPartCommand> it = associatePartCommands.iterator(); it.hasNext();) {
//			AssociateLibraryPartCommand cmd = it.next();
//			if (cmd.pinMappingRequired()) {
//				String knownChanges =
//						LibraryPinMapperDialog.createChangesMessage(cmd.getHarnessConnectorsToDisconnect());
//				Map<INamedObject, IPinProxy> mapping =
//						promptPinMapping(libObj, cmd.getPinLists(), doneShared, it.hasNext(), knownChanges);
//				if (mapping == null) {
//					// dialog was cancelled
//					associatePartCommands = null;
//					return false;
//				}
//				cmd.setPinMapping(mapping);
//			}
//		}

		return PartAssociationStatus.SUCCESS;
	}

	public void completePartAssociation()
	{
		// we must have prepared the commands previously (done pin-mapping, etc..)
		if (associatePartCommands == null) {
			assert false : "preparePartAssociation should be called before completePartAssociation";
			return;
		}

		// just execute the commands we have previously prepared
		boolean bSyncModularConnectorsFromLibraryObject = (associatePartCommands.size() > 1);
		for (AssociateLibraryPartCommand cmd : associatePartCommands) {
			cmd.setLibGrpTypeValidationBeforePartAssignment(false);
			cmd.setSyncModularConnectors(bSyncModularConnectorsFromLibraryObject);
			cmd.setSyncPropertiesWithSource(false);
			cmd.execute();
		}
	}

	public void associateLibraryPart(@Nullable ILibraryObject libObj, @Nullable LibraryControl libraryControl)
	{
		for (Object obj : m_editSet) {
			IUID uid = (IUID) obj;
			IUIDObject editObj = UIDMgr.getObject(uid);
			ILogicObject logObj = ReferenceHelper.reduceToLogicObject(editObj);
			if (logObj != null) {
				// Part association for pinlists is handled in prepare/completePartAssociation
				// think the only other object that can get here is a multicore?
				assert !(editObj instanceof IPinList);

				if (libObj != null) {
					// Note, libCtl.getLibrarySelection() will be null if we are doing an 'update'.
					if (libraryControl == null || libraryControl.getLibrarySelection() == null) {
						// This will set PartNumber and LibraryRef, as well as copy over library attributes and properties
						logObj.assignLibraryPart(libObj);
					}
					else {
						// This will do all that, and also set customer/supplier info
						logObj.assignLibraryDetails(libraryControl.getLibrarySelection());
						if (logObj instanceof IDevice) {
							BridgeHelper.associateSymbolUsingLib((chs.cof.logical.cable.IPinList) logObj);
							BridgeHelper.associateFootPrintUsingLib((IDevice) logObj);
						}
					}

					// copy across any analysis model attribute that is set on the library
					// part
					if (!AssociateLibraryPartCommand.isAnalysisModelUnset(libObj)) {
						logObj.setAnalysisModel(libObj.getAnalysisModel());
					}
				}
				else {
					if (AppInfo.isLogic()) {
						new RemoveLibraryPartHandler().removeLibraryPart(logObj, false, (device) -> false, null);
						new LogicObjectRegenerateHandler()
								.regenerate(logObj, false, this::regenerateDeviceConnector, null, null);
					}
					else {
						// this will clear the libraryref & part number (& customer/supplier info)
						logObj.assignLibraryDetails(null);

						// copy across any analysis model attribute that is set on the library
						// part
						if (!AppInfo.isLogic()) {
							logObj.setAnalysisModel(null);
						}
					}
				}
			}
		}
	}

	public boolean regenerateDeviceConnector(@NotNull IDevice device)
	{
		return device.getFootprint() != null;
	}

	public boolean hasSharedPinList()
	{
		return m_hasSharedPinList;
	}

	public boolean hasSharedPin()
	{
		return m_hasSharedPin;
	}

	public boolean hasSharedConductor()
	{
		return m_hasSharedConductor;
	}

	protected Set<ISharedObject> getRefreshedSharedObjects()
	{
		return m_refreshedSharedObjects;
	}

	/**
	 * Return true if there is a shared object in the edit set; false otherwise
	 */
	public void determineSharedObjects()
	{
		if (!m_model.isEditable() || m_isConnArtificiallyReadOnly) {
			performOperationOnSharedObjects();
		} else {
			performLockOperation();
		}
	}

	void performOperationOnSharedObjects()
	{
		m_hasSharedPin = false;
		m_hasSharedPinList = false;
		m_hasSharedConductor = false;
		m_sharedObjectLockFailure = false;
		m_sharedObjects = new HashSet<ISharedObject>();
		m_refreshedSharedObjects = new HashSet<ISharedObject>();
		m_deletedSharedObjects = new SetMap<IPropertiedObject, IProperty>();

		Set<ISharedPinList> sharedMates = new HashSet<ISharedPinList>(); // used for mated inlines
		Set<IUID> sharedMCDependents = new HashSet<IUID>();

		for (IUID uid : m_editSet) {
			IUIDObject editObj = UIDMgr.getObject(uid);

			// If we are editing text, get the owning object so we can detect any shared object it's on
			if (editObj instanceof IText) {
				IGfxObjectContainer container = ((IGfxObject) editObj).getContainer();
				editObj = CommonUtils.cast(container, IUIDObject.class);
			}

			// If we are editing the indicator, get through to the multicore so we can do the correct shared checks
			if (editObj instanceof IShieldBody) {
				editObj = ((IShieldBody) editObj).getConnectivity().getMulticore();
			}

			IPropertiedObject propObj = ReferenceHelper.reduceToPropertiedObject(editObj);
			if (propObj instanceof ISharedObject) {
				ISharedObject shared = (ISharedObject) propObj;
				// dts0100782734 - ensured we only lock SPLs once :-)
				lockAndAddToSharedObjectList(shared);
				if (shared instanceof ISharedMulticore) {
					sharedMCDependents.addAll(fetchDependentMCtoLock((ISharedMulticore) shared));
				}
				if (shared instanceof ISharedPin) {
					m_hasSharedPin = true;
				}
				if (shared instanceof ISharedPinList) {
					m_hasSharedPinList = true;
				}
				if (shared instanceof ISharedConductor) {
					m_hasSharedConductor = true;
				}
				// if editing a shared inline half we must make sure the mate gets locked too
				if (shared instanceof ISharedConnector) {
					sharedMates.addAll(fetchMatesForSCtoLock((ISharedConnector) shared));
				}
			}
		}

		lockMCDependentsAndSharedMates(sharedMCDependents, sharedMates);
	}

	/**
	 * Ensures that the specified shared object is locked and added to the collection of locked shared objects.
	 *
	 * <p>This method checks if the given {@code shared} object is not already present in the
	 * collection {@code m_sharedObjects}. If it's not present, the method locks the object
	 * and then adds it to the collection.</p>
	 *
	 * @param shared the shared object to be locked and added
	 */
	protected void lockAndAddToSharedObjectList(ISharedObject shared)
	{
		if (!m_sharedObjects.contains(shared)) {
			lockShared(shared);
			m_sharedObjects.add(shared);
		}
	}

	@NotNull protected Set<IUID> fetchDependentMCtoLock(ISharedMulticore shared)
	{
		return SharedConductorGroupHelper.findAllDependents(shared);
	}

	@NotNull
	protected Set<ISharedConnector> fetchMatesForSCtoLock(ISharedConnector shared)
	{
		return shared.getMates();
	}

	protected void lockMCDependentsAndSharedMates(@NotNull Set<IUID> sharedMCDependents,
			@NotNull Set<ISharedPinList> sharedMates)
	{

		for (IUIDObjectIterator it = new UIDObjectCollection(sharedMCDependents).getUIDObjects(); it.hasNext(); ) {
			ISharedObject sharedDep = (ISharedObject) it.getNext();
			lockShared(sharedDep);
		}

		for (ISharedPinList sharedMate : sharedMates) {
			if (!m_sharedObjects.contains(sharedMate)) {
				lockShared(sharedMate);
			}
		}

		// To account for any implicit refresh of these shared objects we need to notify the shared object listeners
		LogicUtils.fireChangeEvent(m_refreshedSharedObjects);
		// if there were any shared objects refreshed we should clear the undo queue now
		if (!m_refreshedSharedObjects.isEmpty()) {
			ICapletModel model = CommonUtils.cast(m_model, ICapletModel.class);
			if (model != null) {
				model.getController().clearUndoQueue();
			}
		}
	}

	/**
	 * Perform pre lock operation on the shared objects and store the lock status corresponding to each object in promise map.
	 * So, while actually trying to lock the objects, it will take the lock status from memory itself and will not be going
	 * to DB.
	 */
	protected void performLockOperation()
	{
		m_sharedObjects = new HashSet<ISharedObject>();

		// batch lock
		Set<ISharedObject> sharedObjectsToLock = new HashSet<>();
		Set<ISharedPinList> sharedMates = new HashSet<ISharedPinList>(); // used for mated inlines
		Set<IUID> sharedMCDependents = new HashSet<IUID>();

		for (IUID uid : m_editSet) {
			IUIDObject editObj = UIDMgr.getObject(uid);
			// If we are editing text, get the owning object so we can detect any shared object it's on
			if (editObj instanceof IText) {
				IGfxObjectContainer container = ((IGfxObject) editObj).getContainer();
				editObj = CommonUtils.cast(container, IUIDObject.class);
			}
			// If we are editing the indicator, get through to the multicore so we can do the correct shared checks
			if (editObj instanceof IShieldBody) {
				editObj = ((IShieldBody) editObj).getConnectivity().getMulticore();
			}
			IPropertiedObject propObj = ReferenceHelper.reduceToPropertiedObject(editObj);
			if (propObj instanceof ISharedObject) {
				ISharedObject shared = (ISharedObject) propObj;
				// dts0100782734 - ensured we only lock SPLs once :-)
				if (!m_sharedObjects.contains(shared)) {
					sharedObjectsToLock.add(shared);
					m_sharedObjects.add(shared);
				}
				if (shared instanceof ISharedMulticore) {
					sharedMCDependents.addAll(SharedConductorGroupHelper.findAllDependents((ISharedMulticore) shared));
				}
				// if editing a shared inline half we must make sure the mate gets locked too
				if (shared instanceof ISharedConnector) {
					sharedMates.addAll(((ISharedConnector) shared).getMates());
				}
			}
		}

		Set<ISharedObject> sharedMCDependentsToLock = new HashSet<>();
		for (IUIDObjectIterator it = new UIDObjectCollection(sharedMCDependents).getUIDObjects(); it.hasNext(); ) {
			ISharedObject sharedDep = (ISharedObject) it.getNext();
			sharedMCDependentsToLock.add(sharedDep);
		}

		Set<ISharedPinList> sharedMatesToLock = new HashSet<ISharedPinList>();
		for (ISharedPinList sharedMate : sharedMates) {
			if (!m_sharedObjects.contains(sharedMate)) {
				sharedMatesToLock.add(sharedMate);
			}
		}

		Collection<ISharedObject> allSharedObjectsToLock = new HashSet<>();
		allSharedObjectsToLock.addAll(sharedObjectsToLock);
		allSharedObjectsToLock.addAll(sharedMCDependentsToLock);
		allSharedObjectsToLock.addAll(sharedMatesToLock);

		Collection<ISharedLockableUpdateableObject> lockableUpdateAbleObjectsToLock =
				BatchLockRefreshHelper.getLockableUpdateAbleObjectsToLock(allSharedObjectsToLock);

		BatchLockRefreshHelper.filterAndLockCOGObjects(lockableUpdateAbleObjectsToLock);
		Set<ILockable> filteredNonCOGSharedObjects =
				BatchLockRefreshHelper.getFilteredNonCOGObjects(allSharedObjectsToLock);
		batchLockNonCOGObjects(filteredNonCOGSharedObjects);
	}

	private void batchLockNonCOGObjects(@NotNull Set<ILockable> nonCOGObjects)
	{
		IPromiseBatchLock promise = PromiseFactory.createPromiseBatchLock();
		promise
				.requestLockAndRefreshOf(nonCOGObjects)
				.issue()
				.thenApply(() -> {
					performOperationOnSharedObjects();
				});
	}

	void lockShared(@NotNull ISharedObject shared)
	{
		ISharedLockableUpdateableObject sharedObjectToLock = shared.getLockableUpdateableRoot();
		if (sharedObjectToLock == null) {
			throw new IllegalArgumentException("ISharedLockableUpdateableObject not found");
		}
		long preLockTimeStamp = sharedObjectToLock.getTimeModified();
		//(SP1202) dts0100831773 In a read-only design, a user cannot open properties of a shared object. It works ok for a non shared object.
		if (!m_model.isEditable() || m_sharedObjectLockFailure || m_isConnArtificiallyReadOnly) {
			sharedObjectToLock.refresh();
			registerIfRefreshed(sharedObjectToLock, preLockTimeStamp, false);
			return;
		}

		if (isCOGObjectLocked(sharedObjectToLock)) {
			registerIfRefreshed(sharedObjectToLock, preLockTimeStamp, true);
			return;
		}

		if (!LockUpdateHelper.lockRefreshAndCheckForDomain(sharedObjectToLock, true)) {
			for (ISharedObject dshared : m_sharedObjects) {
				ISharedObject sharedObjectToUnlock;
				if (dshared instanceof ISharedPin) {
					sharedObjectToUnlock = ((ISharedPin) dshared).getOwner();
				}
				else {
					sharedObjectToUnlock = dshared;
				}
				new LockUpdateHelper(sharedObjectToUnlock).flushAndUnlock(false);
			}
			m_sharedObjectLockFailure = true;
			LockException lockException = sharedObjectToLock.getLockException();
			if (lockException == null && sharedObjectToLock.isEditable()) {
				throw new MissingResourceException(sharedObjectToLock.getName(),
						sharedObjectToLock.getClass().toString(), sharedObjectToLock.getUID().getString());
			}
		}
		else {
			registerIfRefreshed(sharedObjectToLock, preLockTimeStamp, false);
		}
	}

	private boolean isCOGObjectLocked(@NotNull ISharedLockableUpdateableObject sharedObjectToLock)
	{
		if (sharedObjectToLock instanceof ICOGLockable) {
			ICOGLockable sharedCOGObjectToLock = CommonUtils.cast(sharedObjectToLock, ICOGLockable.class);
			if (sharedCOGObjectToLock != null) {
				return sharedCOGObjectToLock.isLocked();
			}
		}
		return false;
	}

	private void registerIfRefreshed(@NotNull ISharedLockableUpdateableObject sharedObjectToLock, long preLockTimeStamp,
			boolean isPreLocked)
	{
		//we can rely on separate call of needsRefresh and lock. The locking would implicitly
		//refresh the object. Since there can be false positive because of window between the
		//two calls and hence blindly treating as refreshed if lock passes.
		// Note this does not currently work correctly for COG objects such as SharedPinList
		// since they always needsRefresh() == true
		if (isPreLocked) {
			postLock(sharedObjectToLock, preLockTimeStamp);
		}

		long postLockTimeStamp = sharedObjectToLock.getTimeModified();
		if (postLockTimeStamp != preLockTimeStamp) {
			m_refreshedSharedObjects.add(sharedObjectToLock);
		}
	}

	public static void updateTimeStamp()
	{
		m_mimicRefreshAtLock = true;
	}

	private void postLock(@NotNull ISharedLockableUpdateableObject sharedObjectToLock, long preLockTimeStamp)
	{
		if (m_mimicRefreshAtLock) {
			sharedObjectToLock.setTimeModified(preLockTimeStamp + 1L);
		}
	}

	/**
	 * Internal sort method to put an object in the proper bin
	 */
	private void sortObject(ILogicObject logObj)
	{
		// FEAT13040 : Properties UI can now be shown for Unplaced objects
		addObject(logObj);
	}

	/**
	 * Given a schematic object, this determines which editset to put the object in (and perhaps what object to use). We
	 * are always storing the schem object.  The corresponding cable object can easily be obtained from the schem
	 * object.
	 */
	private void sortObject(IDiagramObject sobj)
	{
		// treat highways the same as conductor segments.
		// Test: logic/Properties/highWaysObjActions/gripAddUndoRedosaveCloseReopenPrj.csv
		if (sobj instanceof ILogicSegment) {
			m_gfxEditSet.add(sobj.getUID());
		}
		else if (sobj instanceof ILogicSegmentContainer) {
			addObject(sobj);
		}
		else if (sobj != null) {
			if (!(sobj instanceof IText)) {
				m_gfxEditSet.add(sobj.getUID());
			}
			addObject(sobj);
		}
	}

	/**
	 * Based on the type of the object passed, decide the string name and class to use to represent the object Return
	 *
	 * @param sobj
	 *
	 * @return a List of 2: 1. The object string name, 2. The object class PW - 05/30/03
	 */
	protected Pair<String, Class<?>> getSetNameAndClass(IUIDObject sobj)
	{
		String objName = "Unknown";
		Class<?> objClass = Object.class;

		//dts0100522186  Can not add library part to Splice in C-Logic
		// Adding the connectivity object to the list
		ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(sobj);
		if (logicObject instanceof INetConductor) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.conductor.name");
			objClass = INetConductor.class;
		}
		else if (logicObject instanceof IWireConductor) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.conductor.name");
			objClass = IWireConductor.class;
		}
		else if (logicObject instanceof IShieldConductor) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.conductor.name");
			objClass = IShieldConductor.class;
		}
		else if (logicObject instanceof IFunctionConductor) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.FunctionConductor.name");
			objClass = IFunctionConductor.class;
		}
		else if (logicObject instanceof IFunctionMessage) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.FunctionMessage.name");
			objClass = IFunctionMessage.class;
		}
		else if (logicObject instanceof chs.cof.logical.cable.IConductor) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.conductor.name");
			objClass = IConductor.class;
		}
		else if (IConnector.Statics.isRingTerminalTypeConnector(logicObject)) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.ringterminal.name");
			objClass = IConnector.class;
		}
		else if (logicObject instanceof IDeviceConnector) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.deviceConnector.name");
			objClass = IDeviceConnector.class;
		}
		else if (logicObject instanceof IGroundDevice) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.ground.name");
			objClass = IGroundDevice.class;
		}
		else if (logicObject instanceof IDevice) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.device.name");
			objClass = IDevice.class;
		}
		else if (logicObject instanceof IBlockDevice) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.block.name");
			objClass = IBlockDevice.class;
		}
		else if (logicObject instanceof IFunction) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.function.name");
			objClass = IFunction.class;
		}
		else if (logicObject instanceof IConnector) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.connector.name");
			objClass = IConnector.class;
		}
		else if (logicObject instanceof ISplice) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.splice.name");
			objClass = ISplice.class;
		}
		else if (logicObject instanceof IBackshell) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.backshell.name");
			objClass = IBackshell.class;
		}
		else if (logicObject instanceof IOverbraid) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.overbraid.name");
			objClass = IOverbraid.class;
		}
		else if (logicObject instanceof IMulticore) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.multicore.name");
			objClass = IMulticore.class;
		}
		else if (logicObject instanceof IAssembly)   // gdh 12/04/03 re: 6168
		{
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.assembly.name");
			objClass = IAssembly.class;
		}
		else if (logicObject instanceof IPanelLayoutOtherComponent)   // gdh 12/04/03 re: 6168
		{
			objName = ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.othercomponent.name");
			objClass = IPanelLayoutOtherComponent.class;
		}
		else if (logicObject instanceof IPanelLayoutDuctComponent)   // gdh 12/04/03 re: 6168
		{
			objName = ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.duct.name");
			objClass = IPanelLayoutDuctComponent.class;
		}
		else if (logicObject instanceof IPanelLayoutRailComponent)   // gdh 12/04/03 re: 6168
		{
			objName = ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.mount.name");
			objClass = IPanelLayoutRailComponent.class;
		}
		else if (logicObject instanceof IGeneralHighway) {
				objName = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.highway.name");
				objClass = IGeneralHighway.class;
			}
		else if (logicObject instanceof ISingleLine) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.single_line.name");
			objClass = ISingleLine.class;
		}
		else if (sobj instanceof chs.cof.logical.cable.IShieldBody) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.indicator.name");
			objClass = IShieldBody.class;
		}
		else if (sobj instanceof IPin) {
			IAbstractPin cablePin = ((IPin) sobj).getConnectivity();
			if (cablePin instanceof IBackshellTermination) {
				objName = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.backshellTermination.name");
				objClass = IBackshellTermination.class;
			}
			else if (cablePin instanceof IFunctionPin) {
				objName = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.port.name");
				objClass = IFunctionPin.class;
			}
			else {
				objName = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.pin.name");
				objClass = IPin.class;
			}
		}
		else if (sobj instanceof ISchemStackPin) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.stackedPin.name");
			objClass = ISchemStackPin.class;
		}
		else if (sobj instanceof IPropText) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.propertyText.name");
			objClass = IPropText.class;
		}
		else if (AttributeUtils.isNameText(sobj)) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.nameText.name");
			objClass = IAttributeText.class;
		}
		else if (sobj instanceof IAttributeText) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.attributeText.name");
			objClass = IAttributeText.class;
		}
		else if (sobj instanceof IXRefText) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.crossreferenceText.name");
			objClass = IXRefText.class;
		}
		else if (sobj instanceof IPropertiedGraphic) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.propertiedGraphic.name");
			objClass = IPropertiedGraphic.class;
		}
		else if (sobj instanceof ISchemDiagram) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.diagram.name");
			objClass = ISchemDiagram.class;
		}
		else if (sobj instanceof ISchemSector) {
			objName = ResourceMgr.getString(PropertiedSet.class, "PropertiedSet.sector.name");
			objClass = ISchemSector.class;
		}
		else if (sobj instanceof IGfxObject) {
			objName = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.graphicsObject.name");
			objClass = IGfxObject.class;
		}
		else {
			System.err.println("PropertiedSet.getSetNameAndClass() unrecognized: " + sobj);
		}

		return new Pair<String, Class<?>>(objName, objClass);
	}

	/**
	 * Get a string representing the object(s) that are in this set.
	 */
	private void determineSetString(IUIDObject sobj)
	{
		setFootprintInfo(sobj);
		if ((m_setString != null) && ("Objects".equals(m_setString))) {
			return;
		}

		Pair<String, Class<?>> nameClassList = getSetNameAndClass(sobj);
		String objStr = nameClassList.getFirst();
		Class objCls = nameClassList.getSecond();

		// set the Set class
		setSetClass(objCls);

		if ((m_setString != null) && (objStr.equals(m_setString) == false)) {
			objStr = "Objects";
		}

		m_setString = objStr;
	}

	/**
	 * Returns an object that represents all the schematic objects. Returns null if nothing is selected.
	 */
	public IUIDObject getCommonRepresentingObject()
	{
		IDiagramObject sobj = null;
		Iterator<IUID> iter = m_gfxEditSet.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			Object obj = UIDMgr.getObject(uid);
			if (obj instanceof IDiagramObject) {
				if (sobj != null) {
					return null;
				}
				sobj = (IDiagramObject) obj;
			}
		}
		return sobj;
	}

	/**
	 * Returns a conductor that represents the conductors in this set. null is returned if more than one is found.
	 */
	public IRepresentedObject getConductorRep()
	{
		IConductor cond = null;
		Iterator<IUID> iter = m_editSet.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			Object obj = UIDMgr.getObject(uid);
			if (obj instanceof IConductor) {
				if (cond != null) {
					return null;
				}
				cond = (IConductor) obj;
			}
		}
		return cond;
	}

	public void editProperty(EditProperty ep)
	{
		Set<IUIDObject> editObjects = new HashSet<IUIDObject>();
		for (Iterator<IUID> iter = m_editSet.iterator(); iter.hasNext(); ) {
			IUID uid = iter.next();
			IUIDObject editObj = UIDMgr.getObject(uid);
			editObjects.add(editObj);
		}
		editProperty(ep, editObjects);
	}

	private boolean editProperty(EditProperty ep, Set<IUIDObject> editObjects)
	{
		boolean allChanged = true;
		for (IUIDObject editObj : editObjects) {
			IPropertiedObject propObj = ReferenceHelper.reduceToPropertiedObject(editObj);
			if (propObj == null) {
				allChanged = false;
				continue;
			}

			IProperty prop = propObj.findPropertyByName(ep.getName());
			if (prop == null) {
				allChanged = false;
				continue;
			}

			//
			// If it is a single/distinct value, then we set it. If it is 'Multiples', then we skip it.
			//
			if (ep.isSingleValue()) {

				// Check this property is valid for this object
				if (!validateProperty(ep, editObj)) {
					allChanged = false;
					continue;
				}

				prop = ep.replaceProperty(prop, propObj);
			}

			if (isGraphicalObject(editObj) && ep.wasGraphicsEdited()) {
				// Fix for defect 916877
				if (!(editObj instanceof chs.cof.logical.schem.IAssembly)) {
					// Apply any graphical attributes to the properties on the current sheet
					boolean found =
							updatePropertyGraphics((IDiagramObject) editObj, prop, ep.getGfxText(), ep.getVisible());
					if (!found && ep.getVisible() != null) {
						// Means that the property doesn't have a graphical representation so we should creation one.
						addPropertyGraphics(prop, editObj, ep.getGfxText(), ep.getVisible());
						IBaseDiagram diagram = getActiveDiagram();
						diagram.refreshRepresentations();
					}
				}
			}
		}
		return allChanged;
	}

	protected boolean validateProperty(EditProperty ep, IUIDObject editObj)
	{
		return validateProperty(m_objectTypePropertiesMgr, ep, editObj);
	}

	protected boolean isGraphicalObject(IUIDObject editObj)
	{
		return super.isGraphicalObject(editObj) && !(editObj instanceof ISchemDiagram);
	}

	public void addProperty(EditProperty ep)
	{
		Iterator<IUID> iter = m_editSet.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject editObj = UIDMgr.getObject(uid);
			addProperty(ep, editObj);
		}
		IBaseDiagram diagram = getActiveDiagram();
		diagram.refreshRepresentations();
	}

	private static IBaseDiagram getActiveDiagram()
	{
		//noinspection ConstantConditions
		return CAFUtils.getInstance().getActiveDiagram();
	}

	private boolean addProperty(EditProperty ep, IUIDObject editObj)
	{
		IPropertiedObject propObj = ReferenceHelper.reduceToPropertiedObject(editObj);
		if (propObj == null) {
			return false;
		}
		//
		// Can't add properties to text.
		//
		if (propObj instanceof IText) {
			return false;
		}

		// PW - 03/31/03 - defect #3817
		// When adding a property which is already in the PropertiedObject
		// treat it the same as edit (same for multiple objects)
		String propName = ep.getName();
		if (propObj.findPropertyByName(propName) != null) {
			//
			// If it's already there, check for change.
			//
			if (!IChangePolicyMgr.Statics.allowsPropertyChange(propObj, propName)) {
				return false;
			}
			// This propertyObject already has the property - edit the property
			return editProperty(ep, Collections.singleton(editObj));
		}
		//
		// Not there - check for addition - Note, have to be more strict with creation as the property
		// editor allows editing of partial objects - and to apply them to ALL in the end.
		//
		if (!IChangePolicyMgr.Statics.allowsPropertyAddition(propObj, propName)) {
			return false;
		}

		// Check this property is valid for this object
		if (!validateProperty(ep, editObj)) {
			return false;
		}

		IProperty prop = ep.constructProperty(propObj);
		prop.setUserDefined(true);
		propObj.addProperty(prop);

		// If it is a graphical object (as opposed to a logical-only one) then add graphical
		// properties.
		if (isGraphicalObject(editObj)) {
			// Fix for defect 916877
			if (!(editObj instanceof chs.cof.logical.schem.IAssembly)) {
				boolean newVis = !(ep.getVisible() == Boolean.FALSE);
				addPropertyGraphics(prop, editObj, ep.getGfxText(), newVis);
			}
		}
		return true;
	}

	public void delProperty(EditProperty ep)
	{
		for (Object i : m_editSet) {
			IUID uid = (IUID) i;
			IUIDObject editObj = UIDMgr.getObject(uid);
			delProperty(ep, editObj);
		}
	}

	private void delProperty(EditProperty ep, IUIDObject editObj)
	{
		IUIDObject passedObj = editObj;
		if (editObj instanceof IShieldBody) {
			passedObj = ((IShieldBody) editObj).getConnectivity().getMulticore();
		}
		IPropertiedObject propObj = ReferenceHelper.reduceToPropertiedObject(passedObj);
		if (propObj != null) {
			IProperty prop = propObj.findPropertyByName(ep.getName());
			if (prop != null) {
				// PW - 07/09/03
				// Move the codes to PropertiesClientUtils so it can be shared
				removeProperty(passedObj, prop);
				if (propObj instanceof ISharedObject) {
					m_deletedSharedObjects.add(propObj, prop);
				}
			}
		}
	}

	/**
	 * Given a property and attribute, this updates all the corresponding graphics on the current diagram to reflect the
	 * new property and attribute data.
	 */
	private boolean updatePropertyGraphics(IDiagramObject editObj, IProperty prop, IText attObj, Boolean visible)
	{
		boolean found = false;
		Collection<IDiagramObject> representations = getAllRepresentations(editObj);
		for (IDiagramObject propOwner : representations) {
			// Need to check if the property owner is the editObj
			boolean samePropOwner = false;
			if (propOwner instanceof ILogicSegment) {   // This is a segment - The conductor/highway is the one that hold property
				// Compare the conductor/highway
				ILogicSegment segment = (ILogicSegment) propOwner;
				IGfxObjectContainer container = segment.getContainer();
				if (container == editObj) {
					samePropOwner = true;
				}
			}
			else if (propOwner == editObj) {
				samePropOwner = true;
			}
			// Check if the property owner are the same
			if (samePropOwner) {
				found = getAndApplyTextChanges(prop, attObj, visible, propOwner);
			}
		}

		if (!found && (editObj instanceof ISymbolDatumLayout)) {
			found = getAndApplyTextChanges(prop, attObj, visible, editObj);
		}
		return found;
	}

	@NotNull private Collection<IDiagramObject> getAllRepresentations(IDiagramObject editObj)
	{
		IUIDObject connectivityObj = getConnectivity(editObj);
		IBaseDiagram diagram = DiagramHelper.getBaseDiagram(editObj);
		if (connectivityObj != null && diagram != null) {
			IDiagramObjectIterator iter = diagram.getRepresentations(connectivityObj.getUID());
			return CollectionUtils.createList(iter);
		}
		return Collections.singleton(editObj);
	}

	private boolean getAndApplyTextChanges(IProperty prop, IText attObj, Boolean visible, IDiagramObject propOwner)
	{
		boolean found = false;
		Collection<IPropText> texts = TextHelper.getPropTexts(propOwner, prop.getName());
		for (IPropText ptext : texts) {
			found = true;
			// If in third state (null) -> leave alone.
			Boolean vis = visible;
			if (visible == null) {
				vis = ptext.isVisible();
			}
			TextHelper.applyTextChanges(attObj, ptext, vis);
			IDiagramObject parent = ptext.getParent();
			if (!(parent instanceof IPropertiedCommentSymbol)) {
				ptext.setString(prop.getAsString());
			}


		}
		return found;
	}

	/**
	 * The initial implementation for harness connector footprints was only designed to allow the footprint to be added
	 * with a symbol. Consequently, it was only to be edited for symbols. Thus, the check below for IParameterized
	 * devices made sense.
	 * <p>
	 * However, this is no longer the case with the introduction of device-side fooprint introduction for 2004.2.
	 * <p>
	 * Bottom line is that logic now has code to deal with footprints changing.  Therefore, the footprint tab should be
	 * enabled on the properties dialog. So, we just return 'true' now.
	 *
	 * @return True if the set accepts a footprint.
	 */
	public boolean acceptsFootprint()
	{
		return true;
	}

	/**
	 * Returns the string representing the propertiedSet.
	 */
	public String getSetString()
	{
		return m_setString;
	}

	public IText getTextRep()
	{
		IText repText = super.getTextRep();

		// Get some default values - use preferences if they exist, otherwise use something reasonable.
		IProject project = CAFUtils.getInstance().getCurrentProject();
		IProjectPreferenceMgr prefMgr = ProjectHelper.getProjectPreferences(project);
		IGrid grid = null;
		// edit harness attrib unit test, grid will be null
		//
		final IBaseDiagram baseDiagram = getParentDiagram();
		if (baseDiagram != null) {
			grid = baseDiagram.getGrid();
		}
		// TODO (FunctionDesign) refactor this code
		IText prefText;
		final PreferenceContext context = PreferenceContext.determineContext(baseDiagram);
		if (repText instanceof IPropText) {
			if (baseDiagram instanceof IFunctionLogicDiagram) {
				prefText = prefMgr.getFunctionDesignPropertyTextPreferences(grid);
			}
			else {
				prefText = prefMgr.getLogicPropertyTextPreferences(context, grid);
			}
		}
		else if (AttributeUtils.isNameText(repText)) {
			// TODO (ConcordFX) Need to FunctionAttributeTextPreferences?
			prefText = prefMgr.getLogicAttributeTextPreferences(context, grid);
		}
		else if (repText instanceof IPropertiedText) {
			if (baseDiagram instanceof IFunctionLogicDiagram) {
				prefText = prefMgr.getFunctionDesignCommentTextPreferences(grid);
			}
			else {
				prefText = prefMgr.getLogicCommentTextPreferences(context, grid);
			}
		}
		else if (repText instanceof IXRefText) {
			IRepresentedObject repObj = ((IXRefText) repText).getRepObject();
			if (repObj instanceof IPinList) {
				prefText = prefMgr.getPinListXRefTextPreferences(grid);
			}
			else if (repObj instanceof IConductor) {
				prefText = prefMgr.getConductorXRefTextPreferences(grid);
			}
			else if (repObj instanceof IPin) {
				prefText = prefMgr.getPinXRefTextPreferences(grid);
			}
			else if (repObj instanceof IHighwaySchematic) {
				prefText = prefMgr.getHighwayXRefTextPreferences(grid);
			}
			else {
				throw new RuntimeException("Unexpected XRefText represented object: " + repObj);
			}
		}
		else { // No preferences - use some resonable values.
			prefText = FactoryMgr.getDrawFactory().constructText(0, 0, TextHelper.getDefaultTextHeight(), 0, "");
			prefText.setFont(FactoryMgr.getDrawFactory().constructFont("Stroke", FontStyleEnum.FontStylePlain));
			prefText.setHorizontalJustification(HorizJustificationEnum.JustMiddle);
			prefText.setVerticalJustification(VertJustificationEnum.JustCenter);
		}

		if (repText == null)    // gdh re:6438
		{
			return null;
		}

		// If anything is undefined in the representative text, give it a default value.
		if (repText.getHeight() < 0) {
			repText.setHeight(prefText.getHeight());
		}
		if (repText.getHorizontalJustification() == null) {
			repText.setHorizontalJustification(prefText.getHorizontalJustification());
		}
		if (repText.getVerticalJustification() == null) {
			repText.setVerticalJustification(prefText.getVerticalJustification());
		}

		if (repText.getFont() == null) {
			repText.setFont(prefText.getFont());
		}
		else {
			IFont repFont = repText.getFont();
			if (IFont.UNKNOWN_FONT_NAME.equals(repFont.getName()) ||
					FontStyleEnum.FontStyleUknnown.equals(repFont.getType())) {
				String fontName = !IFont.UNKNOWN_FONT_NAME.equals(repFont.getName()) ? repFont.getName() :
						prefText.getFont().getName();
				FontStyleEnum fontStyle = !FontStyleEnum.FontStyleUknnown.equals(repFont.getType()) ?
						repFont.getType() : prefText.getFont().getType();
				repText.setFont(FactoryMgr.getDrawFactory().constructFont(fontName, fontStyle));
			}
		}
		return repText;
	}

	public Set<ISharedObject> getSharedObjects()
	{
		return m_sharedObjects;
	}

	public SetMap<IPropertiedObject, IProperty> getDeletedSharedProperties()
	{
		return m_deletedSharedObjects;
	}

	@Override public void addPropertyGraphics(IProperty prop, Object associatedObj, IText attObj, boolean visibility)
	{
		IBaseDiagram diagram = getParentDiagram();
		IProjectPreferenceMgr prefs = CAFUtils.getInstance().getCurrentProjectPreferences();
		Set<IUID> uidSet = m_gfxEditSet;
		if (associatedObj instanceof ITopoBundleRegion) {
			// Special things must be done for segments of topoBundleRegion
			ISegmentCollector segColl = (ISegmentCollector) associatedObj;
			uidSet = new HashSet<IUID>();
			IConnected seg = TextHelper.getMainSegment(segColl);
			uidSet.add(seg.getUID());
		}

		if (diagram instanceof IStyleableDiagram) {
			Pair<Boolean, Set<IUID>> styleResult = PropertyStyleAppearance
					.setStylingDefaultAppearance(uidSet, prop, associatedObj, visibility, (IStyleableDiagram) diagram);
			boolean isStyled = styleResult.getFirst();
			Set<IUID> noStyleSegmentUIDs = styleResult.getSecond();
			if (!isStyled || !noStyleSegmentUIDs.isEmpty()) {
				addPropertyGraphics(noStyleSegmentUIDs, prop, associatedObj, attObj, visibility, diagram, prefs);
			}
		}
		else {
			addPropertyGraphics(uidSet, prop, associatedObj, attObj, visibility, diagram, prefs);
		}
	}

	@Override protected IBaseDiagram getParentDiagram()
	{
		return m_diagram;
	}

	@Override public boolean containsTextObjects()
	{
		return !TextHelper.getTexts(iterator()).isEmpty();
	}
}
