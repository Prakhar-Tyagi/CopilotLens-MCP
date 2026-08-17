/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */
package chs.caplets.shared.properties;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caf.caplet.helpers.AssignmentButtons;
import chs.caf.caplet.helpers.BlockAssignmentControl;
import chs.caf.caplet.helpers.ILogicPropertiesClient;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.LibraryControl;
import chs.caf.caplet.helpers.PropertiesClientHelper;
import chs.caf.caplet.helpers.SectorHierarchyFinder;
import chs.caf.caplet.helpers.ValidateNameResult;
import chs.caf.caplet.helpers.connectionstab.ConnectionsTab;
import chs.caf.caplet.helpers.connectionstab.SingleLineConnectionsTab;
import chs.caf.caplet.properties.CustomTextControl;
import chs.caf.caplet.properties.EditImageURL;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caplets.logic.LogicBlockAssignmentControl;
import chs.caplets.logic.Model;
import chs.caplets.logic.properties.LogicSectorControl;
import chs.caplets.shared.LogicCapletUtils;
import chs.cof.draw.IText;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.table.IBasicTable;
import chs.cof.icd.IICD;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.IInternalPositionBase;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.AssemblyTypeEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IBlockDevicePin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IPrivilegedDevice;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemSector;
import chs.cof.logical.schem.ISymboledSchemPinList;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedHighway;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryUtilityHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IBaseModuledObject;
import chs.cof.project.IFunctionalModuleCode;
import chs.cof.project.IFunctionalModuleCodeFamily;
import chs.cof.project.IFunctionalModuleCodeFolder;
import chs.cof.project.IFunctionalModuleCodeSet;
import chs.cof.project.IOption;
import chs.cof.project.IOptionedObject;
import chs.cof.project.IProject;
import chs.cof.project.IProjectOptionMgr;
import chs.cof.project.ITagMgr;
import chs.cof.project.naming.IIndexedNamedObject;
import chs.cof.project.naming.INameMgr;
import chs.cof.project.naming.INameValidator;
import chs.cof.project.naming.NameMgr;
import chs.cof.project.objectinfo.IObjectTypeInfo;
import chs.cof.project.objectinfo.IObjectTypeInfoMgr;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.cof.project.objectinfo.names.INameTemplateIterator;
import chs.cog.ICOGLockable;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IBasePreferencesKeys;
import chs.common.IDesignContainer;
import chs.common.IDesignObject;
import chs.common.INamedObject;
import chs.common.IObjectFilter;
import chs.common.IProjectPreferenceMgr;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IReadOnlyNamedObject;
import chs.common.IReadOnlyOptionedDesign;
import chs.common.IRevisionedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.common.PropertyStabilityEnum;
import chs.common.UIDObjectCollection;
import chs.common.UnitTypeEnum;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.IReadOnlyFacet;
import chs.ctf.caf.interfaces.IFunctionalModuleCodeClient;
import chs.ctf.caf.ui.EditProperty;
import chs.ctf.caf.ui.FunctionalModuleCodeClient;
import chs.ctf.caf.ui.IPropertyEditor;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.editui.IModularConnectorClient;
import chs.ctf.editui.PartAssignmentFailureReason;
import chs.system.UIDMgr;
import chs.utilities.AppInfo;
import chs.utilities.CapabilityHelper;
import chs.utilities.CommonUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.SupportedFeatureInfo;
import chs.utilities.ui.ThreeStateCheckBox;
import chs.utilities.ui.combobox.autocomplete.AutoCompleteComboBox;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.IMessageContent;
import chs.utilities.ui.messaging.IMessagingChoices;
import chs.utilities.ui.messaging.IPromptSeverityProvider;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utility.DiagramHelper;
import chs.utility.GfxObjectUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.harness.HarnessUtils;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.SharedSingleLineHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.preferences.StyleSetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class PropertiesClient extends PropertiesClientHelper implements IFunctionalModuleCodeClient,
		ILogicPropertiesClient
{

	private Model m_model;

	private LibraryControl m_libraryControl;
	private BlockAssignmentControl m_blockAssignmentControl;

	private Set<IUID> m_sharedEditSet = null;
	private Set<IUID> m_pccSharedEditSet = null;
	private Set<INameTemplate> m_templateSet;
	private IFunctionalModuleCodeClient m_functionalModuleCodeClient = null;
	private ModularTab m_modulartab;
	private boolean willEditSharedObjects;

	public PropertiesClient(Model model)
	{
		this(model, true);
	}

	public PropertiesClient(Model model, boolean willEditSharedObjects)
	{
		m_model = model;
		this.willEditSharedObjects = willEditSharedObjects;
		m_propertiedSet = null;

		m_clientComponents.add(new LogicCustomTextControl());
		if (allowParameterControl()) {
			m_clientComponents.add(new EditParameterControl());
		}
		if (allowInterconnectMemberControl()) {
			m_clientComponents.add(new InterconnectMemberControl());
		}

		m_libraryControl = getLibraryControl();
		m_clientComponents.add(m_libraryControl);
		m_blockAssignmentControl = getBlockAssignmentControl();
		m_clientComponents.add(m_blockAssignmentControl);
		if (allowSymbolControl()) {
			m_clientComponents.add(new SymbolControl());
		}
		m_clientComponents.add(new MiscPropertiesComponent(model));
		if (allowPokeHomeControl()) {
			m_clientComponents.add(new EditPokeHome());
		}

		Predicate<Object> isTabEditable = (Object) -> shouldShowEditableConnectionsTab();
		m_clientComponents.add(new ConnectionsTab(isTabEditable));

		m_clientComponents.add(new SingleLineConnectionsTab());

		m_modulartab = new ModularTab(model);
		m_clientComponents.add(m_modulartab);

		m_clientComponents.add(new EditImageURL(this));
		m_clientComponents.add(new LogicSectorControl(this));
		m_templateSet = new HashSet<INameTemplate>();
		initFunctionalModuleCodeClient(model);
	}

	private void initFunctionalModuleCodeClient(final Model model)
	{
		final ILogicDesign logicDesign = model.getDesign();
		if (logicDesign != null) {
			m_functionalModuleCodeClient =
					new FunctionalModuleCodeClient(logicDesign, model.isEditable(),
							false, true)
					{

						protected boolean isEditable()
						{
							return m_model.isEditable();
						}

						public Set<IBaseModuledObject> getModuledObjects()
						{
							return getSelectedModuledObjects();
						}

						public boolean allowMultipleObjectEdit()
						{
							return true;
						}

						public boolean allowPercentageEdit()
						{
							return false;
						}

						private Set<IBaseModuledObject> getSelectedModuledObjects()
						{
							Set<IBaseModuledObject> moduledObjects = new HashSet<IBaseModuledObject>();
							Set<INamedObject> selectedObjects = getNamedObjects();
							for (IReadOnlyNamedObject obj : selectedObjects) {
								IBaseModuledObject moduledObject = ReferenceHelper.reduceToLogicModuledObject(obj);
								if (moduledObject != null) {
									moduledObjects.add(moduledObject);
								}
							}
							return moduledObjects;
						}
					};
		}
	}

	protected BlockAssignmentControl getBlockAssignmentControl()
	{
		return new LogicBlockAssignmentControl(getDesign());
	}

	protected LibraryControl getLibraryControl()
	{
		return new LibraryControl();
	}

	protected boolean allowPorts()
	{
		return true;
	}

	public ILogicDesign getDesign()
	{
		return m_model.getDesign();
	}

	protected ICapletModel getModel()
	{
		return m_model;
	}

	private ILogicModel getLogicModel()
	{
		return m_model;
	}

	/**
	 * Initialization to begin changing the properties on selected objects.
	 */
	@Override protected boolean doStartEditingProperties(SelectSet selections)
	{
		m_propertiesLogicEdited = false;
		m_optionExpressionEdited = false;

		boolean success;
		try {
			m_propertiedSet = doGetPropertiedSet(selections, true);
			setSelectedFootprintDes(m_propertiedSet.getPreExistingFootprintDesc());
			m_sharedEditSet = new HashSet<>();
			m_propertiedSet.getSharedObjects().forEach(o -> m_sharedEditSet.add(o.getUID()));
			m_pccSharedEditSet = null;
			m_libraryControl.reset(m_propertiedSet);
			setObject(m_propertiedSet);
			success = true;
		}
		catch (MissingResourceException ignored) {
			success = false;
		}

		return success;
	}

	public Collection<Component> getAdditionalComponents()
	{
		m_pccSharedEditSet = null;
		return super.getAdditionalComponents();
	}

	public Collection<JComponent> getAdditionalGeneralPageComponents()
	{
		m_pccSharedEditSet = null;
		return super.getAdditionalGeneralPageComponents();
	}

	public boolean editAdditionalComponents()
	{
		super.editAdditionalComponents();
		if (!commitModularTabChanges()) {
			return false;
		}
		return true;
	}

	@Override protected boolean specificAttributeClientRequired(@NotNull Set<IAttributeProvider> attributeProviders)
	{
		if (!attributeProviders.isEmpty() && areSameAttributeProviders()) {
			for (IAttributeProvider att : attributeProviders) {
				if (!super.specificAttributeClientRequiredForSingleProvider(att)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean areSameAttributeProviders()
	{
		return m_propertiedSet.getRepresentativePropertiedClass() != IPropertiedObject.class;
	}

	protected boolean isConnectorReadyToSyncWithAssembly(@NotNull IConnector connector)
	{
		IAssembly assembly = connector.getAssembly();
		return connector.isEditable() && (assembly == null || assembly.isEditable());
	}

	@NotNull @Override
	public ValidateNameResult validateNameProperty(@NotNull String name, @Nullable IReadOnlyNamedObject object)
	{
		// LOGIC-10548 - Editing name of shared object in hiragana script triggers a validation after the dialog is disposed, by that time Model is destroyed in SelectSharedPanel
		// which will result in NPE at model.getDesign(). Disable validations once the client has a model that is already destroyed
		if(m_model.isDestroyed()){
			return new ValidateNameResult();
		}
		return super.validateNameProperty(name, object);
	}

	public void stopEditingProperties(boolean successful)
	{
		super.stopEditingProperties(successful);

		// Save any changes to shared objects immediately.
		if (m_propertiedSet != null) {
			Map<IPropertiedObject, Set<IProperty>> sharedDeletions = m_propertiedSet.getDeletedSharedProperties();
			for (Map.Entry<IPropertiedObject, Set<IProperty>> setEntry : sharedDeletions.entrySet()) {
				IPropertiedObject owner = setEntry.getKey();
				assert owner instanceof IUIDObject;
				//noinspection ConstantConditions
				if (owner instanceof IUIDObject) {
					if (owner instanceof ISharedSingleLine sharedSingleLine) {
						owner = sharedSingleLine.getSharedSingleLineMulticores().next();
					}
					PropertyHelper.deletePersistedProperties((IUIDObject) owner, setEntry.getValue());
				}
			}
		}

		for (IPropertiesClientComponent pcc : m_clientComponents) {
			if (!pcc.getEditedSharedObjects().isEmpty()) {
				m_propertiesLogicEdited = true;
				break;
			}
		}

		// DR 353742: We save the shared objects if and only if the action was successful. This prevents saves
		// for edits of frozen shared objects, for edits made from a read only design and when the user
		// cancelled the dialog.
		//
		// Note that we no longer attempt to determine whether a change was made, the marker flags are not
		// reliable, e.g. they are not set when the name changes. This means m_propertiesLogicEdited is
		// assigned but not used. Shuould it be removed or retained in case of future need?
		boolean save = successful;

		String projectUid = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getUID().getString();
		IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
		if (m_sharedEditSet != null) {
			for (IUID sharedUid : m_sharedEditSet) {
				ISharedObject shared = UIDMgr.getObjectOfType(sharedUid, ISharedObject.class);
				assert shared != null;
				ISharedObject sharedObjectToUnlock;
				if (shared instanceof ISharedPin) {
					sharedObjectToUnlock = ((ISharedPin) shared).getOwner();
				}
				else {
					sharedObjectToUnlock = shared;
				}

				new LockUpdateHelper(sharedObjectToUnlock).flushAndUnlock(save);
				//  TODO  merge conflict.    DRs  dts359779  and  dts353742    .flushAndUnlock(wereSharedObjectsEdited());
				if (sharedObjectToUnlock instanceof ISharedMulticore) {
					Set<IUID> sharedDeps =
							SharedConductorGroupHelper.findAllDependents((ISharedMulticore) sharedObjectToUnlock);
					flushDependents(sharedDeps, save);
				}
				if (sharedObjectToUnlock instanceof ISharedConductor) {
					SharedConductorHelper.flushSharedMulticore((ISharedConductor) sharedObjectToUnlock, save);
				}
				if (sharedObjectToUnlock instanceof ISharedMulticore) {
					SharedConductorHelper.flushSharedMulticore((ISharedMulticore) sharedObjectToUnlock, save);
				}

				if (sharedObjectToUnlock instanceof IRevisionedObject) {
					if (successful) {
						IRevisionedObject sharedObject = (IRevisionedObject) sharedObjectToUnlock;
						auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null, projectUid,
								sharedObject.getFullName(), sharedObject.getUID().getString());
					}
				}
				if (sharedObjectToUnlock instanceof ISharedHighway) {
					if (sharedObjectToUnlock instanceof ISharedSingleLine sharedSingleLine) {
						Set<IUID> sharedDependents = SharedSingleLineHelper.findAllDependents(sharedSingleLine);
						flushDependents(sharedDependents, save);
					}
					if (successful) {
						auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null, projectUid,
								sharedObjectToUnlock.getName(), sharedObjectToUnlock.getUID().getString());
					}
				}
			}
			// if we were editing a shared inline half then we may also have changed the mate - flush this too
			if (!m_sharedEditSet.isEmpty()) {
				Set<ISharedPinList> sharedMates = new HashSet<ISharedPinList>();
				Set<IGenericInlineConnector> inlines =
						m_propertiedSet.getFilteredObjects(IGenericInlineConnector.class);
				for (IGenericInlineConnector inline : inlines) {
					ISharedPinList sharedInline = inline.getSharedPinList();
					if (sharedInline instanceof ISharedConnector) {
						sharedMates.addAll(((ISharedConnector) sharedInline).getMates());
					}
				}
				for (ISharedPinList sharedMate : sharedMates) {
					if (!m_sharedEditSet.contains(sharedMate.getUID())) {
						new LockUpdateHelper((ICOGLockable) sharedMate).flushAndUnlock(save);
						auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null, projectUid,
								sharedMate.getFullName(), sharedMate.getUID().getString());
					}
				}
			}
		}

		if (m_functionalModuleCodeClient != null) {
			// reset the selected object to null
			m_functionalModuleCodeClient.setObject(null);
		}
		if (m_modulartab != null) {
			m_modulartab.stopEditing(null);
		}
		m_propertiedSet = null;
		m_sharedEditSet = null;
		m_pccSharedEditSet = null;
	}

	private static void flushDependents(@NotNull Set<IUID> sharedDeps, boolean save)
	{
		for (IUIDObjectIterator dit = new UIDObjectCollection(sharedDeps).getUIDObjects();
				dit.hasNext(); ) {
			ISharedObject sharedDep = (ISharedObject) dit.next();
			new LockUpdateHelper(sharedDep).flushAndUnlock(save);
		}
	}

	private boolean commitModularTabChanges()
	{

		IModularConnectorClient modularClient = getModularConnectorClient();
		if (modularClient != null) {

			modularClient.commitChanges();
		}

		return true;
	}

	/**
	 * Were any instances of ISharedObject changed? As opposed to graphical data associated with a shared object. Used
	 * to determine if it is necessary to flush the shared objects, save the design, and clear the undo queue.
	 *
	 * @return true if any chages were made to shared objects. TODO This implementation needs to be refined. Right now
	 * it returns true if shared objects might have been edited, which does not fulfill its contract and causes the
	 * design to be saved whenever there are shared objects in the propertied set. Have to cover all possibilities -
	 * properties and option expression can be tested within this class, attribute modification must be gotten from
	 * attribute client, and name modification must be gotten from the caller. But onec this is done right, look out for
	 * undo problems when editing graphics on shared objects.
	 */
	public boolean wereSharedObjectsEdited()
	{
		return isConnectivityEditable() && !getSharedObjects().isEmpty();
	}

	public EditProperty getName()
	{
		boolean editable = true;
		boolean deletable = true;
		PropertyStabilityEnum stab = PropertyStabilityEnum.TypeEditable;
		if (!m_model.isEditable()) {
			editable = false;
			deletable = false;
			stab = PropertyStabilityEnum.TypeFixed;
		}
		NameCalcModifier modifier = new NameCalcModifier(editable, deletable, stab)
		{
			public void process(@Nullable IRepresentedObject namedObjOwner, @Nullable IReadOnlyNamedObject namedObj)
			{
				if (namedObj instanceof IShieldBody) {
					// Delegate to multicore
					namedObj = ((IShieldBody) namedObj).getMulticore();
				}

				if (namedObj instanceof IDeviceConnector) {
					setDeletable(false);
					setEditable(false);
					setStability(PropertyStabilityEnum.TypeFixed);
				}
				else if (namedObj instanceof IPinList) {
					IPinList pl = (IPinList) namedObj;
					if (namedObjOwner instanceof chs.cof.logical.schem.IPinList &&
							((ISymboledSchemPinList) namedObjOwner).getSymbolRef() != null) {
						setDeletable(false);
					}
					if (pl.getSharedPinList() != null) {
						setDeletable(false);
						// FEAT2720: no longer prevent editing.
						setStability(PropertyStabilityEnum.TypeFixed);
					}
				}
				else if (namedObj instanceof IAbstractPin) {
					IAbstractPin pin = (IAbstractPin) namedObj;
					// if this is a shared pin, get the attributres right
					if (!allowRenamePin(pin)) {
						setDeletable(false);
						// FEAT2720: not yet supporting editing.
						setEditable(false);
						setStability(PropertyStabilityEnum.TypeFixed);
					}
				}
				else if (namedObj instanceof IMulticore) {
					IMulticore mc = (IMulticore) namedObj;
					// if this is a shared multicore, get the attributres right
					if (mc.getSharedMulticore() != null) {
						setDeletable(false);
						// FEAT2720: no longer prevent editing.
						setStability(PropertyStabilityEnum.TypeFixed);
					}
				}
				else if (namedObj instanceof IConductor) {
					IConductor conductor = (IConductor) namedObj;
					setStability(PropertyStabilityEnum.TypeFixed);
					if (conductor.getSharedConductor() != null) {
						setDeletable(false);
						// FEAT2720: no longer prevent editing.
					}
				}
				else if (namedObj instanceof ISchemDiagram) {
					setDeletable(false);
					setEditable(false);
				}
				else if (!(namedObj instanceof INamedObject)) {
					setDeletable(false);
					setEditable(false);
					setStability(PropertyStabilityEnum.TypeFixed);
				}
			}
		};
		return calculateName(modifier);
	}

	protected boolean allowRenamePin(IAbstractPin pin)
	{
		if (!pin.isShared()) {
			// if not shared. let's check if it has a library part.
			// we don't want to check the symbol since this is allowed now.
			if (pin.isPartAssigned()) {
				return false;
			}
		}
		// if shared, return false any way.
		return !pin.isShared();
	}

	public boolean arePropertiesAddOrDeleteable()
	{
		return m_propertiedSet.isConnectivityEditable();
	}

	public boolean isModelEditable()
	{
		return m_model.isEditable() && !m_propertiedSet.isArtificallyReadOnly();
	}

	/**
	 * Does the properties client allow options, module code and/or harness level participation editing?
	 *
	 * @return true if there's an optioned object in the propertied set
	 */
	public boolean allowOptionModuleLevelEditing()
	{
		return allowOptionEditing() || isFunctionalModuleUIViewable();
	}

	/**
	 * Check the PropertiedSet to see if it has any IOptionedObject. If there is, then return true; otherwise return
	 * false
	 *
	 * @return true if the PropertiedSet has an Optioned Object; false otherwise
	 */
	public boolean allowOptionEditing()
	{
		if (containNonOptionedObjects(m_propertiedSet)) {
			return false;
		}

		// PW - 02/27/03 - Defect#1422
		// If any object in the PropertiedSet is IOptionedObject then return true,
		// otherwise return false
		boolean anyOptionedObject = false;
		Iterator<IUID> iter = m_propertiedSet.iterator();
		// While there is more object and no optionedObject yet
		while (!anyOptionedObject && iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject editObj = UIDMgr.getObject(uid);
			IOptionedObject optObj = ReferenceHelper.reduceToOptionedObject(editObj);
			// skip any propertied graphics for now.  We don't want to assign options to these in Logic
			if (optObj instanceof IPropertiedGraphic) {
				continue;
			}
			if (optObj != null) {    // Found an optionedObject
				anyOptionedObject = true;
			}
		}
		if (!anyOptionedObject) {
			return false;
		}

		IDesign des = m_model.getDesign();
		IProject proj = des.getProject();
		return proj != null && proj.getPreferences().getBoolean(determineConfigPanelOptionsPath(),
				IBasePreferencesKeys.AllowOptionExpressionSelection, true);
	}

	public Iterator<IOption> getOptionIterator()
	{
		IProjectOptionMgr optionMgr = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getOptionMgr();
		Iterator<IOption> oit;
		if (!m_propertiedSet.getSharedObjects().isEmpty()) {
			oit = optionMgr.getElements();
		}
		else {
			//
			// Get the options from the design - if there were none, get them from
			// the project.
			//
			oit = getLogicModel().getDesign().getOptions();
			if (!oit.hasNext()) {
				oit = optionMgr.getElements();
			}
		}
		return oit;
	}

	public boolean allowLibraryIntegration(IPropertyEditor propEditor, IPropertyGroup attrGroup)
	{
		m_libraryControl.setPropEditor(propEditor);
		m_libraryControl.setAttrGroup(attrGroup);
		return m_libraryControl.acceptsSet(m_propertiedSet);
	}

	public void doPartView()
	{

		m_libraryControl.showPart(getPartNumber(), getPartRevision(),
				ConfigurationTypeEnum.fromDesignType(getDesign().getDesignType()));
	}

	public void doPartSelection()
	{
		m_libraryControl.doPartSelection(m_propertiedSet, shouldShowCommentSymbolDuringPartSelection(), getDesign());
		if (m_modulartab != null) {
			ILibraryPartSelection libraryPartSelection = m_libraryControl.getLibrarySelection();
			if (libraryPartSelection != null) {
				m_modulartab.doPartUpdate(libraryPartSelection.getSelectedObject());
			}
		}
	}

	public void doPartRemoval()
	{
		if (!isOkeyToRemovePart(m_propertiedSet)) {
			return;
		}
		removeLibraryPart();
	}

	protected void removeLibraryPart()
	{
		m_libraryControl.doPartRemoval(m_propertiedSet);
		if (m_modulartab != null) {
			m_modulartab.doPartUpdate(null);
		}
	}

	private boolean isOkeyToRemovePart(IPropertiedSet propSet)
	{
		ILogicObject assembliedObject = getAssembliedObject(propSet);
		boolean isCOTS = false;
		if (isCOTSAssembly(assembliedObject)) {
			isCOTS = true;
		}
		else if (assembliedObject != null) {
			IAssembly parentAssembly = assembliedObject.getAssembly();
			if (parentAssembly != null && parentAssembly.isCOTSAssembly()) {
				isCOTS = true;
			}
		}
		return !isCOTS || showWarningAndGetUserInput(assembliedObject);
	}

	private boolean isCOTSAssembly(ILogicObject assembliedObject)
	{
		return assembliedObject instanceof IAssembly && ((IAssembly) assembliedObject).isCOTSAssembly();
	}

	protected boolean showWarningAndGetUserInput(ILogicObject logicObject)
	{
		String resourceKeyRoot;
		if (isCOTSAssembly(logicObject)) {
			resourceKeyRoot = "PropertiesClient.COTSAssembly.removePart.cannotReassign";
		}
		else {
			resourceKeyRoot = "PropertiesClient.COTSAssemblyChild.removePart.cannotReassign";
		}

		ResourceBasedMessageContent content = new ResourceBasedMessageContent(PropertiesClient.class, resourceKeyRoot);

		Choice proceedChoice = new Choice(content.getResourceReader(), "continueButton");
		Choice cancelChoice =
				new Choice(IMessagingChoices.class, "messaging.choices.cancel", Choice.DefaultSetting.DEFAULT);
		Choice response = Question.show(() -> PromptSeverity.WARNING, content, proceedChoice, cancelChoice);
		return response == proceedChoice;
	}

	@Nullable protected ILogicObject getAssembliedObject(IPropertiedSet propSet)
	{
		Iterator<IUID> iter = propSet.iterator();
		Set<INamedObject> objects = propSet.getNamedObjects();
		if (objects.size() == 1) {
			IUID uid = iter.next();
			IUIDObject object = UIDMgr.getObjectOfType(uid, IUIDObject.class);
			return ReferenceHelper.reduceToLogicObject(object);
		}
		return null;
	}

	public void doPartUpdate()
	{
		m_libraryControl.doPartUpdate(m_propertiedSet);
		if(getAttributesClient() != null ) {
			getAttributesClient().libraryPartUpdated(m_libraryControl.getLibrarySelection());
		}
		if (m_modulartab != null) {
			ILibraryPartSelection libraryPartSelection = m_libraryControl.getLibrarySelection();
			if (libraryPartSelection != null) {
				m_modulartab.doPartUpdate(libraryPartSelection.getSelectedObject());
			}
		}
	}

	public boolean disableRemove()
	{
		//
		// Check for the part number changer
		//
		if (!allowPartNumberChanges()) {
			return true;
		}
		if (isOneAssemblyInSelectionWithTypeConnectorAssembly()) {
			return true;
		}
		// If the connectivity is not editable,
		// then disable the remove button. (i.e. frozen shared component.
		if (!isConnectivityEditable()) {
			return true;
		}

		if (m_libraryControl.disableRemove(m_propertiedSet)) {
			return true;
		}

		//
		// We have properties on a device connector - do not allow the user to remove the
		// library part - this must be done via the main (owning) device.
		//
		return containsDeviceConnector();
	}

	private boolean containsDeviceConnector()
	{
		for (Iterator<IUID> itr = m_propertiedSet.iterator(); itr.hasNext(); ) {
			IUID uid = itr.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			ILogicObject lObj = ReferenceHelper.reduceToLogicObject(obj);
			if (lObj instanceof IDeviceConnector) {
				return true;
			}
			if (lObj instanceof IInterconnectDevice) {
				return true;
			}
		}

		return false;
	}

	protected boolean shouldShowCommentSymbolDuringPartSelection()
	{
		final Set<ILibrariedObject> librariedObjects = m_propertiedSet.getFilteredObjects(ILibrariedObject.class);
		if (librariedObjects.isEmpty()) {
			return false;
		}
		for (ILibrariedObject librariedObject : librariedObjects) {
			if (!(librariedObject instanceof ILogicOtherComponent)) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean doNonUIPartSelection(String partnumber)
	{
		boolean partSelection = m_libraryControl.doPartSelection(m_propertiedSet,
				shouldShowCommentSymbolDuringPartSelection(), getDesign(), partnumber,
				Library.getInstance().getLibraryPartSelector(), new StringBuilder(), true); // todo jmy warning msg lost
		if (m_modulartab != null) {
			ILibraryPartSelection libraryPartSelection = m_libraryControl.getLibrarySelection();
			if (libraryPartSelection != null) {
				m_modulartab.doPartUpdate(libraryPartSelection.getSelectedObject());
			}
		}
		return partSelection;
	}

	@Override public ILibraryObject.GroupType getGroupType()
	{
		return m_libraryControl.determineGroupType(m_propertiedSet);
	}

	public boolean disableReassign()
	{
		return !allowPartNumberChanges() ||
				m_libraryControl.disableReassign(m_propertiedSet);
	}

	private boolean isOneAssemblyInSelectionWithTypeConnectorAssembly()
	{
		Set<IAttributeProvider> iAttributeProviderSet = m_propertiedSet.getFilteredObjects(IAttributeProvider.class);
		if (iAttributeProviderSet.size() == 1) {
			IAttributeProvider iAttributeProvider = iAttributeProviderSet.iterator().next();
			if (iAttributeProvider instanceof IAssembly &&
					((IAssembly) iAttributeProvider).getAssemblyType() == AssemblyTypeEnum.CONNECTORASSEMBLY) {
				return true;
			}
		}
		return false;
	}

	public boolean disableUpdate()
	{
		if (isOneAssemblyInSelectionWithTypeConnectorAssembly()) {
			return true;
		}
		// If the connectivity is not editable,
		// then disable the remove button. (i.e. frozen shared component.
		if (!isConnectivityEditable()) {
			return true;
		}

		return m_libraryControl.disableUpdate(m_propertiedSet);
	}

	public boolean disablePartSelection()
	{
		if (!allowPartNumberChanges()) {
			return true;
		}
		if (isOneAssemblyInSelectionWithTypeConnectorAssembly()) {
			return true;
		}
		// If the connectivity is not editable,
		// then disable the remove button. (i.e. frozen shared component.
		if (!isConnectivityEditable()) {
			return true;
		}

		if (m_libraryControl.disablePartSelection(m_propertiedSet)) {
			return true;
		}

		if (!m_libraryControl.acceptsSet(m_propertiedSet)) {
			return true;
		}
		//
		// Are the objects of a different type.
		//
		Class<? extends Object> setClass = m_propertiedSet.getSetClass();
		if (setClass == null || setClass == Object.class) {
			return true;
		}

		if (getGroupType() == ILibraryObject.GroupType.UNDEFINED) {
			return true;
		}

		//
		// We have properties on a device connector - do not allow the user to remove the
		// library part - this must be done via the main (owning) device.
		//

		for (Iterator<IUID> itr = m_propertiedSet.iterator(); itr.hasNext(); ) {
			IUID uid = itr.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			ILogicObject lObj = ReferenceHelper.reduceToLogicObject(obj);
			if (lObj instanceof IDeviceConnector) {
				return true;
			}
			if (lObj instanceof IInterconnectDevice) {
				return true;
			}
			if (lObj instanceof IInterconnectConductor) {
				return true;
			}
			if (lObj instanceof IAbstractPin) {
				return true;
			}
		}

		return !disableRemove();
	}

	static final INameValidator DevicePinNameValidator = new INameValidator()
	{
		public boolean nameExists(String name, IReadOnlyNamedObject obj)
		{
			IDevicePin pin = (IDevicePin) obj;
			IDevice device = (IDevice) pin.getOwner();
			if (device != null) {
				INameMgr pinListNameMgr = device.getNameMgr();
				if (pinListNameMgr.nameExists(name, obj)) {
					return true;
				}

				if (((IPrivilegedDevice) device).hasInternalPinByName(name, false)) {
					return true;
				}
			}
			return false;
		}

		public boolean nameExists(String name, String className, int objType)
		{
			return false;
		}
	};

	//Duplicate name of signal is allowed if ir conflicts with the signal associated to message
	static final INameValidator FunctionConductorNameValidator = new INameValidator()
	{
		public boolean nameExists(String name, IReadOnlyNamedObject obj)
		{
			assert obj instanceof IFunctionConductor;
			NameMgr nameMgr = (NameMgr) ((IDesignObject) obj).getDesignContainer().getNameMgr();
			IIndexedNamedObject namedObjectHolder = ((IIndexedNamedObject) obj).getNamedObjectHolder();
			Set<IUID> objectIDs = nameMgr.getObjectsWithName(name, namedObjectHolder);
			Set<IUID> conflictingObjectIds = objectIDs.stream()
					.filter(filterMessageSignals())
					.collect(Collectors.toSet());
			if (conflictingObjectIds.isEmpty()) {
				return false;
			}
			if (conflictingObjectIds.size() > 1) {
				return true;
			}
			IUID uid = conflictingObjectIds.iterator().next();
			return !nameMgr.areSameObjects(namedObjectHolder, uid);
		}

		@NotNull private Predicate<IUID> filterMessageSignals()
		{
			return (objID) -> {
				IUIDObject uidObject = objID.getObject();
				return uidObject instanceof IFunctionConductor &&
						!((IFunctionConductor) uidObject).isAssociatedMessageSignal();
			};
		}

		public boolean nameExists(String name, String className, int objType)
		{
			return false;
		}
	};

	protected INameValidator getNameValidator()
	{
		if (m_propertiedSet != null) {
			if (m_propertiedSet.getSingleNamedObject() instanceof IDevicePin) {
				return DevicePinNameValidator;
			}
			else if (m_propertiedSet.getSingleNamedObject() instanceof IFunctionConductor) {
				return FunctionConductorNameValidator;
			}
		}
		return getLogicModel().getDesign().getNameMgr();
	}

	public boolean allowNameTextAttributesEdit()
	{
		if (!m_model.isEditable()) {
			return false;
		}
		if (!m_propertiedSet.isConnectivityEditable()) {
			//the option expression/module code expression clients are dependent upon this
			//and if this is allowed they try to edit the connectivity also. so need to disable them.
			return false;
		}
		return super.allowNameTextAttributesEdit();
	}

	public boolean allowNameTextVisibilityEdit()
	{
		for (IReadOnlyNamedObject namedObj : m_propertiedSet.getNamedObjects()) {
			if (namedObj instanceof ISchemDiagram) {
				return false;
			}
		}
		return super.allowNameTextVisibilityEdit();
	}

	/**
	 * Is an object invisible on the diagram?
	 *
	 * @param obj - the UIDObject we are testing
	 * @return -
	 */
	private boolean isInvisibleObject(IUIDObject obj)
	{
		// Note: instanceof IOverbraid implies instanceof IMulticore.
		return obj instanceof IMulticore
				|| obj instanceof IAssembly || obj instanceof chs.cof.logical.schem.IAssembly;
	}

	/**
	 * Are all objects we're editing properties for invisible on the diagram?
	 *
	 * @return -
	 */
	private boolean allInvisibleObjects()
	{
		Iterator<IUID> iter = m_propertiedSet.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			if (!isInvisibleObject(obj)) {
				// We have at least one visible object.
				return false;
			}
		}

		// All objects we're setting properties on are invisible.
		// This would also happen if we're given an empty set of objects.
		// That's correct behaviour, though the case shouldn't arise.
		return true;
	}

	public boolean allowPropertyTextAttributesEdit()
	{
		if (!areGraphicsEditable()) {
			return false;
		}
		if (m_propertiedSet.getSingleNamedObject() instanceof ISchemDiagram || allInvisibleObjects()) {
			return false;
		}

		return super.allowPropertyTextAttributesEdit();
	}

	public void doPartAssignment()
	{
		m_libraryControl.doPartAssignment(m_propertiedSet);
	}

	/**
	 * @see IPropertiesClient#destroy()
	 */
	public void destroy()
	{
		super.destroy();
		m_libraryControl = null;
		m_blockAssignmentControl = null;
		m_model = null;
	}

	/**
	 * Should we allow the 'fixed' checkbox to appear on the name field of the general tab?
	 */
	public boolean allowFixedName()
	{
		return !(!m_propertiedSet.getSharedObjects().isEmpty() ||
				m_propertiedSet.getSetClass() == ISchemDiagram.class ||
				// For multiple objects, we do not allow fixed changes as the name can not be modified.
				m_propertiedSet.getNamedObjects().size() > 1);
	}

	public Set<ISharedObject> getSharedObjects()
	{
		if (m_pccSharedEditSet == null) {
			m_pccSharedEditSet = new HashSet<IUID>();
			for (IPropertiesClientComponent pcc : m_clientComponents) {
				pcc.getSharedObjects().forEach(o -> m_pccSharedEditSet.add(o.getUID()));
			}
			m_sharedEditSet.addAll(m_pccSharedEditSet);
		}

		Set<ISharedObject> sharedObjects = new HashSet<>();
		for (IUID uid : m_sharedEditSet) {
			ISharedObject sharedObject = UIDMgr.getObjectOfType(uid, ISharedObject.class);
			if (sharedObject != null) {
				sharedObjects.add(sharedObject);
			}
		}
		return sharedObjects;
	}

	public Set<INameTemplate> getNameTemplates()
	{
		if (m_model == null) {
			return Collections.emptySet();
		}

		m_templateSet.removeAll(m_templateSet);

		IAttributeText text = null;
		for (Iterator<IUID> iter = m_propertiedSet.iterator(); iter.hasNext(); ) {
			IUID uid = iter.next();
			IUIDObject obj = UIDMgr.getObject(uid);

			if (AttributeUtils.isNameText(obj)) {
				text = (IAttributeText) obj;
				break;
			}
		}

		IReadOnlyNamedObject namedObj;
		if (text == null) {
			namedObj = m_propertiedSet.getSingleNamedObject();
		}
		else {
			namedObj = (IReadOnlyNamedObject) text.getAttributeProvider();
		}
		if (namedObj == null) {
			return Collections.emptySet();
		}

		IDesign des = m_model.getDesign();
		IProject proj = des.getProject();
		IObjectTypeInfoMgr typeMgr = proj.getObjectTypeInfoMgr();

		IObjectTypeInfo typeInfo = typeMgr.getByObject(namedObj);
		if (typeInfo != null) {
			INameTemplateIterator iter = typeInfo.getNameTemplates();
			while (iter.hasNext()) {
				INameTemplate template = iter.getNext();
				m_templateSet.add(template);
			}
		}

		return m_templateSet;
	}

	public String getPartNumber()
	{
		return m_libraryControl.getPartNumber();
	}

	@Nullable public String getPartRevision()
	{
		return m_libraryControl.getPartRevision();
	}

	@Nullable public String getPartNumberAttributeValue()
	{
		return m_libraryControl.getPartNumberAttributeValue();
	}

	public ILibraryPartSelection getLibrarySelection()
	{
		return m_libraryControl.getLibrarySelection();
	}

	public ILibraryDeviceFootprint.FootprintType getFootprintType()
	{
		return m_libraryControl.getFootprintType();
	}

	public String getSelectedLibFootprintDesc()
	{
		return m_libraryControl.getFootprintDescription();
	}

	public String getSelectedPartNumber()
	{
		return m_libraryControl.getSelectedPartNumber();
	}

	protected void notifyDiagramEdited(IBaseDiagram diag)
	{
		if (diag instanceof ISchemDiagram) {
			IDesign design = ((ISchemDiagram) diag).getDesign();
			assert design != null;
			IProject project = design.getProject();
			List context = CAFUtils.getInstance().getCAFProjectMgr().getContextForDesign(project, design);
			//context.add(diag);
			CAFUtils.getInstance().getCAFProjectMgr().projectEdited(project, context);
			LogicCapletUtils.setWindowTitle((ISchemDiagram) diag, design);
		}
	}

	@Override public boolean doSelectionsHaveProperties(SelectSet selections)
	{
		//properties action must be disabled if design is save locked. otherwise this would try to create
		//another transaction boundary and would meshup the release of savelock from the design.
		IPrivilegedCOGManagedLockableChildrenContainer lockableChildrenContainer =
				CommonUtils.cast(getDesign(), IPrivilegedCOGManagedLockableChildrenContainer.class);
		if (lockableChildrenContainer != null && lockableChildrenContainer.isSaveLocked()) {
			return false;
		}

		if (!super.doSelectionsHaveProperties(selections)) {
			return false;
		}

		if (hasPrintRegionSelection(selections)) {
			return false;
		}

		List<Selection> validSelections = getValidSelections(selections);
		return !validSelections.isEmpty();
	}

	@NotNull protected List<Selection> getValidSelections(@NotNull SelectSet selections)
	{
		SelectionFilter sf = getSelectionFilter();
		return selections.getFilteredSelections(sf);
	}

	@NotNull private SelectionFilter getSelectionFilter()
	{
		// Cannot select [for properties] certain object types
		SelectionFilter sf = new SelectionFilter();
		sf.addExceptClass(IInterconnectMember.class);
		sf.addExceptClass(IBasicTable.class);
		sf.addExceptClass(ILogicDesign.class);
		sf.addExceptClass(IInternalPositionBase.class);
		sf.addExceptClass(IICD.class);
		return sf;
	}

	public boolean match(@Nullable IReadOnlyNamedObject named, @Nullable ISharedObject shared)
	{
		return matchShared(named, shared);
	}

	public boolean isOptionEditingEnabledForDesign()
	{
		IDesignContainer iDesignContainer = getDesign();
		assert iDesignContainer != null;
		IProject project = iDesignContainer.getProject();
		assert project != null;
		IProjectPreferenceMgr prefMgr = project.getPreferences();
		boolean isMandateAppOptionOn = isMandateApplicableOptionPrefOn(iDesignContainer, prefMgr);
		return !(isMandateAppOptionOn && ((IReadOnlyOptionedDesign) iDesignContainer).getNumOptions() == 0);
	}

	private boolean isMandateApplicableOptionPrefOn(IDesignContainer iDesignContainer, IProjectPreferenceMgr prefMgr)
	{
		if (iDesignContainer instanceof IFunctionLogicDesign) {
			return prefMgr.getFunctionMandateApplicableOptions();
		}
		if (iDesignContainer instanceof ILayoutLogicDesign) {
			return prefMgr.getLayoutMandateApplicableOptions();
		}
		return prefMgr.getLogicMandateApplicableOptions();
	}

	protected final boolean containNonOptionedObjects(IPropertiedSet propertiedSet)
	{
		Set<IHighway> highways = propertiedSet.getFilteredObjects(IHighway.class);
		if (!highways.isEmpty()) {
			return true;
		}
		Set<ISchemSector> sectors = propertiedSet.getFilteredObjects(ISchemSector.class);
		if (!sectors.isEmpty()) {
			return true;
		}

		Set<IBlockDevice> blockDevice = propertiedSet.getFilteredObjects(IBlockDevice.class);
		if (!blockDevice.isEmpty()) {
			return true;
		}
		Set<IBlockDevicePin> blockDevicePin = propertiedSet.getFilteredObjects(IBlockDevicePin.class);
		return !blockDevicePin.isEmpty();
	}

	// dts0100652007 Does not update composite text unless Apply Style

	public boolean isUpdateCompositeTextRequired()
	{
		return true;
	}

	@Override public UnitTypeEnum getDistanceUnit()
	{
		return StyleSetUtils.getDistanceUnit(m_model.getDiagram());
	}

	@Override public boolean supportsFunctionalModuleCodes()
	{
		return (CapabilityHelper.supports(SupportedFeatureInfo.Feature.MODULE_CODES) && (AppInfo.isCapitalLogic() ||
				AppInfo.isSvcDoc())) && m_functionalModuleCodeClient != null;
	}

	@Override public boolean allowFunctionalModuleEditing()
	{
		if (supportsFunctionalModuleCodes()) {
			// if the object is not set to the client during start of action; then this returns false
			return m_functionalModuleCodeClient.allowFunctionalModuleEditing();
		}
		return false;
	}

	public Collection<IFunctionalModuleCode> getFunctionalModuleCodes()
	{
		return m_functionalModuleCodeClient.getFunctionalModuleCodes();
	}

	public Collection<IFunctionalModuleCode> getAvailableFunctionalModuleCodes()
	{
		return m_functionalModuleCodeClient.getAvailableFunctionalModuleCodes();
	}

	public ITagMgr<IFunctionalModuleCode, IFunctionalModuleCodeFolder, IFunctionalModuleCodeFamily, IFunctionalModuleCodeSet> getFunctionalModuleCodeMgr()
	{
		return m_functionalModuleCodeClient.getFunctionalModuleCodeMgr();
	}

	public boolean setFunctionalModuleCodes(Set<IFunctionalModuleCode> chosenApplicableTags)
	{
		return m_functionalModuleCodeClient.setFunctionalModuleCodes(chosenApplicableTags);
	}

	@Override public boolean setFunctionalModuleCodes(Set<IFunctionalModuleCode> chosenApplicableTags,
			Boolean isVisible)
	{
		m_functionalModuleCodeClient.setFunctionalModuleCodes(chosenApplicableTags);

		Iterator<IUID> iter = getPropertiedSet().iterator();

		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject editObj = UIDMgr.getObject(uid);
			if (!(editObj instanceof IRepresentedObject)) {
				continue;
			}
			IBaseModuledObject logicObject = ReferenceHelper.reduceToLogicModuledObject(editObj);
			if (logicObject == null) {
				continue;
			}

			IAttribute attribute = logicObject.getAttribute(IAttributeTypes.USER_FM_CODE);
			if (attribute != null) {
				if (chosenApplicableTags.isEmpty()) {
					// Commenting below code as it is causing defect dts0101239773
					// Delete any text if necessary
//					String objectModule = attribute.getAsString();
//					if (objectModule != null) {
//						IDiagramText text = TextHelper.getTextRepresentation((IRepresentedObject) editObj,
//								IAttributeTypes.USER_FM_CODE);
//						if (text != null) {
//							text.delete();
//						}
//					}
				}
				else {
					if (isVisible != ThreeStateCheckBox.THIRD_STATE) {
						IText text = TextHelper.getTextRepresentationWithCreate((IRepresentedObject) editObj,
								IAttributeTypes.USER_FM_CODE);
						if (text != null) {
							text.setMarkedVisible(isVisible);
						}
					}
				}
			}
		}

		return true;
	}

	public Collection<IFunctionalModuleCode> getGeneratedFunctionalModuleCodes()
	{
		return m_functionalModuleCodeClient.getGeneratedFunctionalModuleCodes();
	}

	@Override public void setObject(Object object)
	{
		assert object instanceof IPropertiedSet;
		if (m_functionalModuleCodeClient != null) {
			Set<INamedObject> selectedObjects = m_propertiedSet.getNamedObjects();
			if (!selectedObjects.isEmpty() && selectedObjects.size() == 1) {
				IBaseModuledObject obj = ReferenceHelper.reduceToLogicModuledObject(selectedObjects.iterator().next());
				if (obj != null) {
					m_functionalModuleCodeClient.setObject(obj);
				}
			}
			else {
				m_functionalModuleCodeClient.setObject(null);
			}
		}
	}

	@Override public boolean shouldValidateFunctionalModuleCodes()
	{
		return m_functionalModuleCodeClient != null &&
				m_functionalModuleCodeClient.shouldValidateFunctionalModuleCodes();
	}

	@Override public boolean isFunctionalModuleUIViewable()
	{
		IDesign des = m_model.getDesign();
		IProject proj = des.getProject();
		boolean isFMCodesToDisplay = proj != null &&
				proj.getPreferences().getBoolean(determineConfigPanelOptionsPath(),
						IBasePreferencesKeys.AllowFunctionalModuleCodeSelection, false);

		return supportsFunctionalModuleCodes() && !getModuledObjects().isEmpty() && isFMCodesToDisplay;
	}

	@NotNull protected String determineConfigPanelOptionsPath()
	{
		IDesign des = m_model.getDesign();
		return des instanceof ILayoutLogicDesign ? IBasePreferencesKeys.LayoutConfigurationPanelOptionsPath :
				IBasePreferencesKeys.LogicConfigurationPanelOptionsPath;
	}

	@Override public boolean allowFunctionalModuleVisibilityEdit()
	{
		IText text = getTextRepresentation(IAttributeTypes.USER_FM_CODE);
		return text == null || GfxObjectUtils.isVisibilityOverridable(text);
	}

	@Override public Boolean isFunctionalModuleCodeVisible()
	{
		return isAttributeTextVisible(IAttributeTypes.USER_FM_CODE, new IObjectFilter<IUIDObject>()
		{
			@Override public boolean accept(IUIDObject obj)
			{
				ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(obj);
				return logicObject != null;
			}
		});
	}

	@Override public boolean ignoreAttribute(IReadOnlyFacet f)
	{
		if (super.ignoreAttribute(f)) {
			return true;
		}

		String attrName = f.getName();
		return attrName.equals(IAttributeTypes.USER_FM_CODE) || attrName.equals(IAttributeTypes.USER_PM_CODE) ||
				attrName.equals(IAttributeTypes.GENERATED_FM_CODE) ||
				attrName.equals(IAttributeTypes.GENERATED_PM_CODE);
	}

	public Set<IBaseModuledObject> getModuledObjects()
	{
		return m_functionalModuleCodeClient.getModuledObjects();
	}

	public boolean allowMultipleObjectEdit()
	{
		return m_functionalModuleCodeClient.allowFunctionalModuleEditing();
	}

	@Override public boolean disableFunctionalGeneratedCodes()
	{
		return m_functionalModuleCodeClient != null && m_functionalModuleCodeClient.disableFunctionalGeneratedCodes();
	}

	public boolean allowPercentageEdit()
	{
		return m_functionalModuleCodeClient == null || m_functionalModuleCodeClient.allowPercentageEdit();
	}

	@Override public Set<AssignmentButtons> getAvailableBlockButtons()
	{
		return m_blockAssignmentControl.getAvailableButtons(m_propertiedSet);
	}

	@Override public void doBlockAssignment()
	{
		m_blockAssignmentControl.doBlockAssignment(m_propertiedSet);
	}

	@Override public void doBlockSelection()
	{
		m_blockAssignmentControl.doBlockSelection(m_propertiedSet);
	}

	@Override public void doBlockRemoval()
	{
		m_blockAssignmentControl.doBlockRemoval(m_propertiedSet);
	}

	@Override public void doBlockUpdate()
	{
		m_blockAssignmentControl.doBlockUpdate(m_propertiedSet);
	}

	@Override public void doBlockView()
	{
		m_blockAssignmentControl.doBlockView(m_propertiedSet);
	}

	@Override public boolean disableBlockView()
	{
		return m_blockAssignmentControl.disableBlockView(m_propertiedSet);
	}

	@Override public boolean disableBlockReassign()
	{
		return !isConnectivityEditable() || m_blockAssignmentControl.disableBlockReassign(m_propertiedSet);
	}

	@Override public boolean disableBlockRemove()
	{
		return !isConnectivityEditable() || m_blockAssignmentControl.disableBlockRemove(m_propertiedSet);
	}

	@Override public boolean disableBlockUpdate()
	{
		return !isConnectivityEditable() || m_blockAssignmentControl.disableBlockUpdate(m_propertiedSet);
	}

	@Override public boolean disableBlockSelection()
	{
		return !isConnectivityEditable() || m_blockAssignmentControl.disableBlockSelection(m_propertiedSet);
	}

	@Override public boolean doNonUIDesignSelection(String designName)
	{
		return m_blockAssignmentControl.doBlockSelection(m_propertiedSet, designName);
	}

	@Override public List<String> getMatchingDesignList(String partialText)
	{
		return m_blockAssignmentControl.getMatchingDesignList(m_propertiedSet, partialText);
	}

	@Override @Nullable public String getDesignName()
	{
		return m_blockAssignmentControl.getDesignName(m_propertiedSet);
	}

	@Override public String getSelectedDesignAttribute(String attrName)
	{
		return m_blockAssignmentControl.getSelectedDesignAttribute(attrName);
	}

	@NotNull @Override public Set<String> getSelectedDesignAttributeNames()
	{
		return m_blockAssignmentControl.getSelectedDesignAttributeNames();
	}

	@NotNull @Override public IPropertiedSet getPropertiedSet(@NotNull SelectSet selections)
	{
		return doGetPropertiedSet(selections, false);
	}

	@NotNull
	protected IPropertiedSet doGetPropertiedSet(@NotNull SelectSet selections, boolean checkArtificiallyReadOnly)
	{
		// Need to store the propertied set in case any client wants to check if the
		// graphics in the propertied set are editable.
		m_propertiedSet = new PropertiedSet(selections, getModel(), willEditSharedObjects, checkArtificiallyReadOnly);
		return m_propertiedSet;
	}

	@Nullable @Override public IModularConnectorClient getModularConnectorClient()
	{
		return m_modulartab.getModularConnectorClient();
	}

	@Nullable @Override public Choice preparedForCommit()
	{

		IModularConnectorClient modularConnectorClient = getModularConnectorClient();
		if (modularConnectorClient != null) {
			PartAssignmentFailureReason failureReason = modularConnectorClient.canCommitChanges();
			if (failureReason != null) {

				IMessageContent messageContent =
						PartAssignmentFailureReason.createDefaultMessageContent(failureReason,
								ResourceMgr.getString(PropertiesClient.class, "PropertiesClient.PropertiesAction"));

				IPromptSeverityProvider severity = new IPromptSeverityProvider()
				{
					@Override public PromptSeverity getSeverity()
					{
						return PromptSeverity.ERROR;
					}
				};

				Choice result =
						Question.show(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), severity,
								messageContent,
								ILogicPropertiesClient.retry,
								ILogicPropertiesClient.cancel);

				return result;
			}
		}

		return null;
	}

	@Override public void rollbackCommitPreperation()
	{
		IModularConnectorClient modularConnectorClient = getModularConnectorClient();
		if (modularConnectorClient != null) {
			modularConnectorClient.rollbackCommitPreperation();
		}
	}

	@Override public AutoCompleteComboBox.IFilteredList getAutoCompleteSearchFilter()
	{
		IDesign design = m_model.getDesign();
		assert design != null : "Unexpected that no active design is available by this time";
		ConfigurationTypeEnum type = ConfigurationTypeEnum.fromDesignType(design.getDesignType());
		assert type != null : "Unexpected that no Library configuration type could be determined for design type " +
				design.getDesignType();
		return LibraryUtilityHelper.getPartNumberAutoCompleteFilter(type, getGroupType());
	}

	private class LogicCustomTextControl extends CustomTextControl
	{

		@Override protected boolean isReadOnly(@NotNull IAttribute att)
		{
			if (super.isReadOnly(att) || att.getOwner() == null) {
				return true;
			}
			return !att.getOwner().isFacetEditable(att);
		}

		@Nullable private IECAttributeResolver mAttributeResolver = null;
		@Nullable private ILogicDesign logicDesign = null;
		private SectorHierarchyFinder hierarchyFinder = null;

		@NotNull private IECAttributeResolver getAttributeResolver(SectorHierarchyFinder finder)
		{
			if (mAttributeResolver == null) {
				mAttributeResolver = new IECAttributeResolver(finder, true);
			}
			return mAttributeResolver;
		}

		@NotNull private SectorHierarchyFinder getHierarchyFinder(ISchemDiagram diagram)
		{
			if (hierarchyFinder == null) {
				hierarchyFinder = new SectorHierarchyFinder(diagram);
			}
			return hierarchyFinder;
		}

		private LogicCustomTextControl()
		{
			super(PropertiesClient.this);
		}

		@Override protected void editingCompleted(boolean changeMade)
		{
			Set<ISharedPinList> flushedPinLists = new HashSet<ISharedPinList>();
			for (ISharedObject dshared : m_lockedObjects) {
				if (dshared instanceof ISharedPin) {
					if (!flushedPinLists.contains(((ISharedPin) dshared).getOwner())) {
						new LockUpdateHelper(dshared).flushAndUnlock(changeMade);
						flushedPinLists.add(((ISharedPin) dshared).getOwner());
					}
				}
				else {
					new LockUpdateHelper(dshared).flushAndUnlock(changeMade);
				}
			}
			m_lockedObjects.clear();
			if (logicDesign != null && mAttributeResolver != null) {
				mAttributeResolver.resolveAttributes(logicDesign);
			}
			mAttributeResolver = null;
		}

		@Override protected void updateAttribute(@NotNull IAttributeText textObj)
		{
			addObjectsToUpdate(textObj);
			super.updateAttribute(textObj);
			handleAttributeChange(textObj);
		}

		private void handleAttributeChange(@NotNull IAttributeText textObj)
		{
			IConnector connector = CommonUtils.cast(textObj.getAttributeProvider(), IConnector.class);
			if (connector != null && textObj.getOMAttribute() != null &&
					IAttributeTypes.CONNECTOR_ASSEMBLY.equals(textObj.getOMAttribute().getName())) {
				HarnessUtils.syncConnectorWithAssembly(connector,
						connector.isConnectorAssembly() && connector.getAssembly() == null);
			}
		}

		@Override protected void updateText(IText textObj)
		{
			addObjectsToUpdate(textObj);
			super.updateText(textObj);
		}

		private void addObjectsToUpdate(IText textObj)
		{
			ISchemSector sector = CommonUtils.cast(textObj.getContainer(), ISchemSector.class);
			ISchemDiagram diagram = DiagramHelper.getDiagram(sector);
			if (sector != null && diagram != null) {
				logicDesign = diagram.getDesign();
				SectorHierarchyFinder finder = getHierarchyFinder(diagram);
				IECAttributeResolver attributeResolver = getAttributeResolver(finder);
				finder.getObjectsContainedInSector(sector).stream()
						.forEach(gfxObject -> attributeResolver.addGfxObject(gfxObject, false));
			}
		}
	}

	protected boolean willEditSharedObjects()
	{
		return willEditSharedObjects;
	}
}
