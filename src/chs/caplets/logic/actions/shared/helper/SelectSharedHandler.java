package chs.caplets.logic.actions.shared.helper;

import chs.capitalmanager.appserver.IUserSessionRemotePackage.SharedPinListInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.SelectSharedPanel;
import chs.caplets.logic.shared.AbstractLockedSharedObjectFilter;
import chs.caplets.logic.shared.LockedCablePinListFilter;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListIterator;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.LogicalDesignsCache;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedObjectsFinderInDesigns;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IOptionExpression;
import chs.cof.project.IOptionedObject;
import chs.cof.project.IProject;
import chs.cof.project.naming.IIndexedNamedObject;
import chs.cof.symbol.ISymbolRef;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.CHSConstants;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.WrappingRuntimeException;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.validator.AbstractPropertyValidator;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import chs.utility.NamedObjectListUtils;
import chs.utility.ValidateDuplicateSharedNames;
import chs.utility.helpers.BlockDeviceConnectionHelper;
import chs.utility.helpers.DesignSharedUsageHelper;
import chs.utility.helpers.IPinListShareHelper;
import chs.utility.logic.ModularConnectorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class SelectSharedHandler extends BaseSharePinlistHandler
{

	private static final String DEFAULT_REVISION = "1";
	private static final Class<SelectSharedPanel> ResourceClass = SelectSharedPanel.class;
	@NotNull private IPinList pinlist;
	@Nullable private IStringProperty m_nameProp = null;
	@Nullable private IStringProperty m_nameMateProp = null;
	@NotNull protected IStringProperty m_revisionProp;
	@Nullable private Runnable m_nameGenerated = null;
	@Nullable private Runnable m_mateNameGenerated = null;
	@NotNull private AbstractLockedSharedObjectFilter m_sharedLockFilter;
	@NotNull private LockedCablePinListFilter m_cableLockFilter;
	@Nullable private IProject mCurrentProject;
	private final boolean reportNameValidation;
	@NotNull private BiFunction<PropertyValidationErrorEnum, IPinList, String> mMessageConverter;

	public SelectSharedHandler(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design,
			boolean fromSymbol, @NotNull ISelectSharedAdapter creator, @Nullable IProject currentProject)
	{
		this(esplModel, design, fromSymbol, creator, currentProject, null, false,
				(v, p) -> defaultMessageConverter(v));
	}

	public SelectSharedHandler(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design,
			boolean fromSymbol, @NotNull ISelectSharedAdapter creator, @Nullable IProject currentProject,
			@Nullable IShareMessageContextReporter reporter, boolean isBulkShare,
			@NotNull BiFunction<PropertyValidationErrorEnum, IPinList, String> messageConverter)
	{
		super(esplModel, design, reporter, isBulkShare);
		final IPinList cablePinlist = esplModel.getCablePinlist();
		if (cablePinlist == null) {
			throw new IllegalArgumentException();
		}
		pinlist = cablePinlist;
		reportNameValidation = creator.shouldReportNameValidation();
		mCurrentProject = currentProject;
		m_cableLockFilter = new LockedCablePinListFilter(pinlist);
		m_sharedLockFilter = creator.getLockedSharedObjectFilter(isBulkShare);
		m_sharedLockFilter.setDesign(design);
		mMessageConverter = messageConverter;

		if (!ModularConnectorHelper.doesModularConnectorHasAtleastOneFilledPosition(pinlist)) {
			m_nameProp = creator.createNameProperty(pinlist instanceof IInlineJackConnector);
			m_nameProp.setObject(pinlist.getName());
			m_nameProp.setDefaultValueObject(pinlist);
			m_nameProp.addValidator(
					creator.createNamePropertyValidator(getNamedObject(pinlist), getDesign().getNameMgr()));

			createGeneratedNameProperty(creator);
		}
		if (pinlist instanceof IInlineJackConnector) {
			m_nameMateProp = creator.createMateNameProperty();
			m_nameMateProp.addValidator(null);
			if (!fromSymbol) {
				m_nameMateProp.setValue(
						NamedObjectListUtils.convertNamedObjectListToString(((IConnector) pinlist).getMates()));
			}

			IConnector mateConnector = ((IConnector) pinlist).getMates().iterator().next();
			final boolean isEnabled = isGeneratedPropEnabled(mateConnector);
			final boolean generatedMateDefaultValue =
					IPinListShareHelper.generatedDefaultStateInteractive(mateConnector);
			final IBooleanProperty generatedMateProp =
					creator.createMateGeneratedProperty(generatedMateDefaultValue, isEnabled);

			generatedMateProp.addPropertyChangeListener(
					(evt -> setSharedPinListMateNameGenerated(generatedMateProp.getValue())));
			generatedMateProp.touch();
			m_mateNameGenerated = () -> {
				generatedMateProp.setEnabled(isGeneratedPropEnabled(mateConnector));
			};
			m_mateNameGenerated.run();
		}

		m_revisionProp = creator.createRevisionProperty();

		if (!fromSymbol && m_nameProp != null) {
			m_nameProp.setValue(pinlist.getName());
		}

		String revision = getSharedPinList() != null ? getSharedPinList().getRevision() : DEFAULT_REVISION;
		m_revisionProp.setValue(revision);

		setupValidatorsAndListeners(esplModel);
	}

	protected void createGeneratedNameProperty(@NotNull ISelectSharedAdapter creator)
	{
		final boolean isEnabled = isGeneratedPropEnabled(pinlist);
		final boolean generatedNameDefaultValue = IPinListShareHelper.generatedDefaultStateInteractive(pinlist);
		final IBooleanProperty generatedProp =
				creator.createGeneratedProperty(generatedNameDefaultValue, isEnabled);

		generatedProp.addPropertyChangeListener((evt) -> setSharedPinListNameGenerated(generatedProp.getValue()));
		generatedProp.touch();

		m_nameGenerated = () -> {
			generatedProp.setEnabled(isGeneratedPropEnabled(pinlist));
		};
		m_nameGenerated.run();
	}

	private void setupValidatorsAndListeners(@NotNull EditSharedPinListModel esplModel)
	{
		IPropertyValidator nameValidator = createNameValidator();
		IPropertyValidator mateValidator = createMateValidator();
		IPropertyValidator revisionValidator = createRevisionValidator();
		PropertyChangeListener nameRevisionListener = createNameRevisionListener();
		if (m_nameProp != null) {
			//add the library validation before duplicate name validation
			appendLibraryDevicePartValidation();
			m_nameProp.addValidator(nameValidator);
			m_nameProp.addValidityListener(esplModel);
			m_nameProp.addPropertyChangeListener(nameRevisionListener);
		}
		m_revisionProp.addValidator(revisionValidator);
		m_revisionProp.addPropertyChangeListener(nameRevisionListener);

		if (pinlist instanceof IGenericInlineConnector) {
			IConnector mate = ((IGenericInlineConnector) pinlist).getMate();
			if (mate != null && m_nameMateProp != null) {
				m_nameMateProp.addPropertyChangeListener(nameRevisionListener);
				m_nameMateProp.addValidator(mateValidator);
				m_nameMateProp.setDefaultValueObject(mate);
			}
		}
	}

	@NotNull @Override public IPinList getCablePinlist()
	{
		return pinlist;
	}

	@NotNull private PropertyChangeListener createNameRevisionListener()
	{
		return new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				IStringProperty p = (IStringProperty) evt.getSource();
				String value = StringUtils.trim(p.getValue());

				// if there is a valiator and the validator determines value is not valid, then set the value to null
				if ((p.getValidator() != null) && (!p.getValidator().validate(p))) {
					if (reportNameValidation) {
						reportError(p.getValidityReason());
					}
					value = null;
				}

				if (p == m_nameProp) {
					setSharedPinListName(value);
				}
				else if (p == m_revisionProp) {
					setSharedPinListRevision(value);
					setSharedPinListMateRevision(value);
				}
				else if (p == m_nameMateProp) {
					setSharedPinListMateName(value);
				}
			}
		};
	}

	@NotNull private AbstractPropertyValidator createRevisionValidator()
	{
		return new AbstractPropertyValidator()
		{
			public boolean validate(IProperty property)
			{
				String revision = m_revisionProp.getValue();
				final PropertyValidationErrorEnum propertyValidationEnum = validateRevision(revision);

				//duplicate name
				if (propertyValidationEnum == null) {
					setReason(null);
				}
				else {
					final String error = getValidationMessage(propertyValidationEnum);
					setReason(error);
				}
				return propertyValidationEnum == null;
			}

			@Nullable private PropertyValidationErrorEnum validateRevision(String revision)
			{
				if (revision == null || StringUtils.isEmpty(revision.trim()) ||
						(revision.length() > CHSConstants.SHAREDOBJECT_REVISION_LENGTH)) {
					return PropertyValidationErrorEnum.InvalidRevision;
				}

				return null;
			}
		};
	}

	@NotNull protected AbstractPropertyValidator createMateValidator()
	{
		return new AbstractPropertyValidator()
		{

			public boolean validate(IProperty property)
			{
				if (pinlist instanceof IGenericInlineConnector) {
					IConnector mate = ((IGenericInlineConnector) pinlist).getMate();
					if (mate != null) {
						final PropertyValidationErrorEnum nameValidityStatus = validateName(property, mate);
						final String reason = getValidationMessage(nameValidityStatus, mate);
						setReason(reason);
						// Valid if there was no error string
						return nameValidityStatus == null;
					}
				}
				return false;
			}
		};
	}

	@NotNull private AbstractPropertyValidator createNameValidator()
	{
		return new AbstractPropertyValidator()
		{
			public boolean validate(IProperty property)
			{
				final PropertyValidationErrorEnum nameValidityStatus = validateName(property, pinlist);
				final String reason = getValidationMessage(nameValidityStatus);
				setReason(reason);
				// Valid if there was no error string
				return nameValidityStatus == null;
			}
		};
	}

	private boolean isGeneratedPropEnabled(@NotNull IPinList pl)
	{
		return pl.isGeneratedName();
	}

	private void appendLibraryDevicePartValidation()
	{
		if (m_nameProp != null && pinlist instanceof IDevice && !(pinlist instanceof IInterconnectDevice) &&
				(((IDevice) pinlist).getNumDeviceConnectors() > 0)) {
			IDevice device = (IDevice) pinlist;
			if (!device.checkFootprintReferenceLineSyncWithSource(null)) {
				final String reason = getValidationMessage(PropertyValidationErrorEnum.LibraryPartOutOfDateByPins);
				IPropertyValidator libDevicePartValidator = new AbstractPropertyValidator()
				{
					public boolean validate(IProperty property)
					{
						setReason(reason);
						return false;
					}
				};
				m_nameProp.addValidator(libDevicePartValidator);
			}
		}
	}

	public void onCreateNew()
	{
		if (getSharedPinList() != null) {
			unlock(getSharedPinList());
			setSharedPinList(null);
		}
		if (m_nameProp != null) {
			m_nameProp.touch();
		}
		if (m_nameMateProp != null) {
			m_nameMateProp.touch();
		}

		if (m_nameGenerated != null) {
			m_nameGenerated.run();
		}

		if (m_mateNameGenerated != null) {
			m_mateNameGenerated.run();
		}
	}

	public void preShareInto()
	{
		setSharedPinListName(null);
		setSharedPinListMateName(null);
	}

	public void setSharedPinListOnModel(@Nullable ISharedPinList newSPL)
	{
		setSharedPinList(newSPL);
	}

	public boolean isValidPinlistToShareInto(@NotNull ISharedPinList spl)
	{
		if (mCurrentProject == null) {
			return false;
		}
		final ISharedPinListMgr splmgr = mCurrentProject.getSharedPinListMgr();
		final SharedPinListInfo sharedPinListInfo = getSharedPinListInfos(splmgr).get(spl);
		return isValidPinlistToShareInto(mapRevisions(), sharedPinListInfo, spl);
	}

	public boolean isValidPinlistToShareInto(@NotNull Map<IUID, String> usedRevisions,
			@Nullable SharedPinListInfo sharedPinListInfo, @NotNull ISharedPinList spl)
	{
		if (!isPartCompatible(spl)) {
			reportError(
					ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.shareInto.partnotCompatible"));
			return false;
		}

		if (!isOwnerCompatible(spl)) {
			reportError(
					ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.shareInto.ownerNotCompatible"));
			return false;
		}

		// If a revision is already used on the design, cannot share into another revision
		// of the same family
		if (isRevisionAlreadyUsed(usedRevisions, spl)) {
			final String pinlisType = COFTypeEnum.getDisplayableTypeName(pinlist);
			reportError(ResourceMgr
					.getString(SelectSharedPanel.class, "SelectSharedPanel.shareInto.revisionAlreadyUsed",
							StringUtils.toLowerCase(pinlisType)));
			return false;
		}
		int pinCount = 0;
		if (sharedPinListInfo != null) {
			pinCount = sharedPinListInfo.pinCount;
		}

		if (pinlist instanceof IGroundDevice) {
			// ground devices cannot maintain multiple symbols, therefore can only share into shared ground with same symbol
			if (pinlist.getSymbolRef() != null && !doSymbolRefMatch(spl, pinlist.getSymbolRef())) {
				return false;
			}
		}

		if (pinlist instanceof ISplice) {
			// TODO jacobt FEAT13040 : test this - used to be a schem pinlist check
			// Parameterized splices have just one pin, therefore can only share into shared splices with one pin
			if (pinlist.getSymbolRef() != null) {
				// If splice has a symbol, can only share into into shared splices with the same symbol
				return doSymbolRefMatch(spl, pinlist.getSymbolRef());
			}
			else {
				return spl.getNumPins() == 1;
			}
		}
		if (spl.isPartAssigned()) {
			if (spl.isPartAssigned() && pinlist.isPartAssigned() &&
					spl.getLibraryRef() == pinlist.getLibraryRef()) {
				return true;
			}
			else {
				// Shared Devices and Connectors with library part, and connectors with plug map, have a fixed number of
				// pins, therefore can't share into these if the pinlist being shared has more pins
				return pinCount >= pinlist.getNumPins();
			}
		}
		if (pinlist.isPartAssigned()) {
			return pinCount <= pinlist.getNumPins();
		}

		return true;
	}

	private boolean doSymbolRefMatch(@NotNull ISharedPinList spl, @NotNull ISymbolRef symbolRef)
	{
		return (spl.getSymbolRef() != null) && (spl.getSymbolRef() == symbolRef.getSymbolUID());
	}

	private boolean isPartCompatible(@NotNull ISharedPinList spl)
	{
		if (pinlist.isPartAssigned()) {
			if (!spl.isPartAssigned()) {
				return false;
			}
			return pinlist.getLibraryRef() == spl.getLibraryRef();
		}
		return true;
	}

	private boolean isOwnerCompatible(@NotNull ISharedPinList spl)
	{
		// If the pinlist is owned by a device, then make sure any instances of the object we would share into
		// also belong to that device
		boolean canShareInto = true;
		if (pinlist instanceof IDeviceOwned) {
			final IConnectivity connectivity = getDesign().getConnectivity();
			assert null != connectivity;
			final IPinList otherLogicInstance = connectivity.findSharedPinList(spl);
			// Make sure this pinlist is something than can be owned by a device
			if (otherLogicInstance instanceof IDeviceOwned) {
				if (!isOwnerCompatible((IDeviceOwned) otherLogicInstance)) {
					canShareInto = false;
				}
			}
		}
		return canShareInto;
	}

	private boolean isOwnerCompatible(@NotNull IDeviceOwned otherLogicInstance)
	{
		IBaseDevice otherInstanceOwner = otherLogicInstance.getOwner();
		IBaseDevice pinListOwner = ((IDeviceOwned) pinlist).getOwner();
		if (otherInstanceOwner == null) {
			return pinListOwner == null;
		}

		if (pinListOwner == null) {
			return false;
		}

		if (otherInstanceOwner != pinListOwner) {
			return false;
		}

		if (pinListOwner instanceof IBlockDevice) {
			Pair<String, String> pinListOwnerConnectionType =
					BlockDeviceConnectionHelper.getPinConnectionType(pinListOwner, pinlist);
			Pair<String, String> otherInstanceOwnerConnectionType =
					BlockDeviceConnectionHelper.getPinConnectionType(pinListOwner, (IPinList) otherLogicInstance);

			return BlockDeviceConnectionHelper.areBlockPinOfSameType(pinListOwnerConnectionType,
					otherInstanceOwnerConnectionType);
		}
		return true;
	}

	@NotNull public Map<IUID, String> mapRevisions()
	{
		// Map all used revisions to their base id: only one revision of the family can
		// be used
		Map<IUID, String> usedRevisions = new HashMap<IUID, String>();
		for (ISharedPinListIterator iter = getDesign().getSharedUsageMgr().getSharedPinLists(); iter.hasNext(); ) {
			ISharedPinList spl = iter.getNext();
			usedRevisions.put(spl.getBaseId(), spl.getRevision());
			//If this is a modualar connector, only the root will have same base-id across revisions. store it.
			if (spl instanceof ISharedConnector && ((ISharedConnector) spl).getOccupiedPosition() != null) {
				ISharedConnector root = ((ISharedConnector) spl).getTopLevelConnector();
				usedRevisions.put(root.getBaseId(), root.getRevision());
			}
		}
		return usedRevisions;
	}

	public boolean shareInto(@NotNull ISharedPinList spl)
	{
		if (!isShareIntoAllowed()) {
			reportError(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.shareInto.notSupported"));
			return false;
		}
		preShareInto();
		if (isValidPinlistToShareInto(spl)) {
			return onShareIntoPinlist(spl);
		}
		return false;
	}

	public boolean onShareIntoPinlist(@Nullable ISharedPinList newSPL)
	{
		return onShareIntoPinlist(newSPL, this::setSharedPinListOnModel);
	}

	public boolean onShareIntoPinlist(@Nullable ISharedPinList newSPL,
			@NotNull Consumer<ISharedPinList> newPinlistSetter)
	{
		ISharedPinList oldSPL = getSharedPinList();
		if (newSPL == oldSPL) {
			return false;
		}
		unlockOldSharedPinlist(oldSPL);
		final ISharedPinList verifiedNewSPL = verifySharedIntoPinlist(newSPL);
		if (verifiedNewSPL == null) {
			return false;
		}
		Set<ISharedObject> candidateSharedObjects =
				collectCandidateSharedObjectsForScope(verifiedNewSPL);
		try {
			// Populate cache for this dialog/action
			LogicalDesignsCache.populateLogicDesigns(candidateSharedObjects);
			newPinlistSetter.accept(verifiedNewSPL);
			return true;
		}
		finally {
			// Ensure cache is always discarded
			LogicalDesignsCache.discardLogicDesignSet();
		}
	}

	/**
	 * Collects all shared objects needed to compute the design scope
	 * for the given Shared PinList (SPL).
	 *
	 * Includes:
	 *  - Shared objects currently used in the active design
	 *  - The selected SPL itself
	 */
	private Set<ISharedObject> collectCandidateSharedObjectsForScope(
			@NotNull ISharedPinList sharedPinList) {

		Set<ISharedObject> candidateSharedObjects = new HashSet<>();

		// Include all shared objects used in the current design
		SharedObjectsFinderInDesigns.gatherUsedSharedObjects(
				Collections.singletonList(getDesign()),
				candidateSharedObjects
		);

		// Include the SPL itself
		candidateSharedObjects.add(sharedPinList);

		return candidateSharedObjects;
	}

	@Nullable private ISharedPinList verifySharedIntoPinlist(@Nullable ISharedPinList spl)
	{
		if (spl != null) {
			for (IObjectUIFilterOption objectFilter : getFilters()) {
				if (!objectFilter.selected(spl)) {
					reportError(objectFilter.getDescription(spl));
					return null;
				}
			}
		}
		return spl;
	}

	private void unlockOldSharedPinlist(@Nullable ISharedPinList oldSPL)
	{
		if (oldSPL != null) {
			unlock(oldSPL);
			// if this is an inline we have to unlock its mate.
			if (oldSPL instanceof ISharedConnector) {
				for (ISharedConnector mate : SharedPinListHelper.collectAllMates((ISharedConnector) oldSPL)) {
					unlock(mate);
				}
			}
		}
	}

	private void unlock(@NotNull ISharedPinList sharedPinlist)
	{
		if (!isBulkShare()) {
			sharedPinlist.unlock();
		}
	}

	private boolean isRevisionAlreadyUsed(@NotNull Map<IUID, String> usedRevisions, @NotNull ISharedPinList spl)
	{
		String usedRevision = getRevisionAlreadyUsed(usedRevisions, spl);
		return usedRevision != null && !usedRevision.equals(spl.getRevision());
	}

	/**
	 * In case of modualar connector, if atleast one connector in the hierarchy is placed, treat that this connector
	 * itself is placed
	 *
	 * @param usedRevisions - map of baseID vs revision placed in the design
	 * @param spl - any revision of this shared pinlist has been placed?
	 *
	 * @return null if no other revision of this shared object is used in the design "or" valid revision of this shared
	 * object which is instantiated in this design
	 */
	@Nullable private String getRevisionAlreadyUsed(@NotNull Map<IUID, String> usedRevisions,
			@NotNull ISharedPinList spl)
	{
		String usedRevision = usedRevisions.get(spl.getBaseId());
		if (usedRevision == null && spl instanceof ISharedConnector &&
				((ISharedConnector) spl).getOccupiedPosition() != null) {
			//this is part of shared modular connector
			ISharedConnector root = ((ISharedConnector) spl).getTopLevelConnector();
			usedRevision = usedRevisions.get(root.getBaseId());
		}
		return usedRevision;
	}

	public boolean isBackshellCompatible(@Nullable ISharedPinList targetSharedPinlist)
	{
		IBackshell sourceBackshell = (pinlist instanceof IConnector) ? ((IConnector) pinlist).getBackshell() : null;
		return isBackshellCompatible(targetSharedPinlist, sourceBackshell);
	}

	public boolean isBackshellCompatible(@Nullable ISharedPinList targetSharedPinlist,
			@Nullable IBackshell sourceBackshell)
	{
		if (sourceBackshell != null && targetSharedPinlist instanceof ISharedConnector) {
			ISharedBackshell targetSharedBackshell = ((ISharedConnector) targetSharedPinlist).getBackshell();
			if (targetSharedBackshell != null) {

				if (targetSharedBackshell.isPartAssigned()) {
					if (sourceBackshell.isPartAssigned()) {
						return sourceBackshell.getLibraryRef() == targetSharedBackshell.getLibraryRef();
					}
					else {
						return sourceBackshell.getNumBackshellTerminations() <=
								targetSharedBackshell.getNumBackshellTerminations();
					}
				}
				else {
					if (sourceBackshell.isPartAssigned()) {
						return false;
					}
					else {
						return sourceBackshell.getNumBackshellTerminations() <=
								targetSharedBackshell.getNumBackshellTerminations();
					}
				}
			}
			else {
				return !targetSharedPinlist.isFrozen();
			}
		}
		return true;
	}

	@NotNull
	public Map<ISharedPinList, SharedPinListInfo> getSharedPinListInfos(@NotNull ISharedPinListMgr mgr)
	{
		final PinListTypeEnum type = PinListTypeEnum.from_connectivity(pinlist);
		try {
			return SharedPinListHelper.getSharedPinListInfos(mgr, type);
		}
		catch (UserSessionException e) {
			throw new WrappingRuntimeException(e);
		}
	}

	@Nullable private PropertyValidationErrorEnum validateName(@NotNull IProperty property, @NotNull IPinList pinList)
	{
		// If not enabled, then don't check validity. Just return true.

		String name = ((IStringProperty) property).getValue();
		IReadOnlyNamedObject namedObject = getNamedObject(pinList);
		PropertyValidationErrorEnum validationErrorEnum = validateName(name);

		//duplicate name
		if (validationErrorEnum == null) {
			final ValidateDuplicateSharedNames findDuplicateShared = new ValidateDuplicateSharedNames(getDesign());
			//check if it's a valid duplicate name
			if (findDuplicateShared.isDuplicateName(StringUtils.trim(name), namedObject, mCurrentProject)) {
				if (namedObject instanceof IOptionedObject) {
					IOptionedObject optionedObject = (IOptionedObject) namedObject;
					IOptionExpression opExpression = optionedObject.getOptionExpression();
					if (opExpression != null && opExpression.getExpression() != null &&
							!opExpression.getExpression().isEmpty()) {
						validationErrorEnum = PropertyValidationErrorEnum.NameAndOptionAlreadyUsed;
					}
				}
				if (validationErrorEnum == null) {
					validationErrorEnum = PropertyValidationErrorEnum.NameAlreadyUsed;
				}
				//in all cases, register a warning
				final String note = getValidationMessage(validationErrorEnum, pinList);
				if (note != null) {
					property.addNote(note, IProperty.NoteTagLevel.Warning);
				}
				return validationErrorEnum;
			}
			else {
				return null;
			}
		}
		else {
			return validationErrorEnum;
		}
	}

	public void init()
	{
		// Trigger checking and initialization of esplModel
		if (m_nameProp != null) {
			m_nameProp.touch();
		}
		if (m_nameMateProp != null) {
			m_nameMateProp.touch();
		}

		m_revisionProp.touch();

		// TODO - Control focus of property component
//		sharedPinListName.requestFocus();
	}

	@Nullable private PropertyValidationErrorEnum validateName(@Nullable String name)
	{
		if (name == null || StringUtils.isEmpty(name.trim())) {
			return PropertyValidationErrorEnum.EmptyName;
		}
		return null;
	}

	@Nullable private String getValidationMessage(@Nullable PropertyValidationErrorEnum validationEnum)
	{
		return getValidationMessage(validationEnum, getCablePinlist());
	}

	@Nullable
	private String getValidationMessage(@Nullable PropertyValidationErrorEnum validationEnum, @NotNull IPinList pinList)
	{
		if (validationEnum == null) {
			return null;
		}
		return mMessageConverter.apply(validationEnum, pinList);
	}

	@Nullable private static String defaultMessageConverter(@NotNull PropertyValidationErrorEnum errorEnum)
	{
		switch (errorEnum) {
			case EmptyName:
				return ResourceMgr.getString(ResourceClass, "SelectSharedPanel.InvalidName.text");
			case NameAndOptionAlreadyUsed:
				return ResourceMgr
						.getString(ResourceClass, "SelectSharedPanel.nameandoptionexpressionalreadyused.text");
			case NameAlreadyUsed:
				return ResourceMgr.getString(ResourceClass, "SelectSharedPanel.nameused.text");
			case InvalidRevision:
				return ResourceMgr.getString(ResourceClass, "SelectSharedPanel.InvalidRevision.text");
			case LibraryPartOutOfDateByPins:
				return ResourceMgr.getString(ResourceClass, "SelectSharedPanel.OutOfDatePartByPins.text");
		}
		return null;
	}

	@Nullable
	private IReadOnlyNamedObject getNamedObject(@NotNull IUIDObject iUIDObject)
	{
		IReadOnlyNamedObject namedObject = null;
		if (iUIDObject instanceof IRepresentedObject) {
			IRepresentedObject repObj = (IRepresentedObject) iUIDObject;
			IUIDObject logicObj = repObj.getRawConnectivity();
			if (logicObj instanceof IIndexedNamedObject) {
				namedObject = (IReadOnlyNamedObject) logicObj;
			}
		}
		else if (iUIDObject instanceof IReadOnlyNamedObject) {
			// This change for Topology - has several non-graphical named objects
			namedObject = (IReadOnlyNamedObject) iUIDObject;
		}
		return namedObject;
	}

	public int getNumberOfUnloadedDSUM()
	{
		Set<ILogicDesign> designScope =
				SharedPinHelper.getDesignsInTheScope(getDesign(), ILogicDesign.class);
		return DesignSharedUsageHelper.getNumberOfUnLoadedDSUM(designScope);
	}

	public boolean isModularConnectorWithAtLeastOneFilledPosition()
	{
		return isModularConnectorWithAtLeastOneFilledPosition(pinlist);
	}

	public boolean isShareIntoAllowed()
	{
		return !isModularConnectorWithAtLeastOneFilledPosition();
	}

	@Nullable public IStringProperty getNameProperty()
	{
		return m_nameProp;
	}

	@NotNull public IObjectUIFilterOption[] getFilters()
	{
		return new IObjectUIFilterOption[]{m_sharedLockFilter, m_cableLockFilter};
	}

	public enum PropertyValidationErrorEnum
	{
		EmptyName,
		NameAndOptionAlreadyUsed,
		NameAlreadyUsed,
		InvalidRevision,
		LibraryPartOutOfDateByPins
	}
}
