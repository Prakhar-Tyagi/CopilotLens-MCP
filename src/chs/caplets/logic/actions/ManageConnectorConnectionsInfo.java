/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caplets.logic.MoveSwapErrorCode;
import chs.caplets.logic.validators.BackshellTerminationValidator;
import chs.caplets.logic.validators.IPinMateValidator;
import chs.caplets.logic.validators.PlacedPinMateValidator;
import chs.caplets.logic.validators.SharedPinMateValidator;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.cable.PinTypeEnum;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryObject;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.utils.ConductorWrapper;
import chs.ctf.caf.utils.IConductorProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ManageConnectorConnectionsInfo extends Pair<IPinProxy, IConductorProxy>
{

	private final Object hashCodeProvider = new Object();
	@Nullable private IUID m_sharedMateUID;

	public static class Connection
	{

		private IPinProxy pin;
		private IConductorProxy conductor;
		private IPinProxy originalPin;

		public Connection(IPinProxy pin, @Nullable IConductor conductor)
		{
			originalPin = pin;
			setPin(pin);
			if (conductor != null) {
				this.conductor = new ConductorWrapper(conductor);
			}
		}

		public Connection(IPinProxy pin, @Nullable IConductorProxy conductor)
		{
			originalPin = pin;
			setPin(pin);
			if (conductor != null) {
				this.conductor = conductor;
			}
		}

		private void setPin(IPinProxy pin)
		{
			this.pin = pin;
		}

		public IPinProxy getPin()
		{
			return pin;
		}

		public IConductorProxy getConductor()
		{
			return conductor;
		}

		public ManageConnectorChange getConnectionChange()
		{
			return new ManageConnectorChange(getOriginalPin(), pin);
		}

		public IPinProxy getOriginalPin()
		{
			return originalPin;
		}
	}

	private Connection mConnection;

	private ManageConnectorPinSelections manageConnectorPinSelections;

	private IDesignDescriptor design;

	private boolean mIsNewConnection = false;

	private boolean mIsLibraried = false;

	private DisableReason disableReason = DisableReason.NONE;

	// NOTE: preserve the order of validators.
	private final List<IPinMateValidator> pinMateValidators = Arrays.asList(
			new BackshellTerminationValidator(),
			new SharedPinMateValidator(),
			new PlacedPinMateValidator()
	);

	public enum DisableReason
	{
		NONE(true, true, null, null, null),
		INSTANCEOFSHAREDPINUSABLEINCURRENTDESIGN(false, true, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.OtherDesignSharedInstance"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableConductorDnDReason"), null) {
			@Override public DisableReason designLocked(boolean success)
			{
				if (success) {
					return NONE;
				}
				return DESIGNCANNOTBELOCKED;
			}

			@Override public DisableReason designNotEditable()
			{
				return DESIGNCANNOTBEEDITED;
			}
		},
		INSTANCEOFSHAREDPINNOTUSABLEINCURRENTDESIGN(false, false,
				ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.SharedInstanceNotUsableInCurrentDesign"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableConductorDnDReason"), ResourceMgr
				.getString(ManageConnectorsAction.class, "ManageConnectorsAction.dialog.tooltip.DisablePinDnDReason")) {
			@Override public DisableReason designLocked(boolean sucess)
			{
				if (sucess) {
					return NONE;
				}
				return DESIGNCANNOTBELOCKED;
			}

			@Override public DisableReason designNotEditable()
			{
				return DESIGNCANNOTBEEDITED;
			}
		},
		UNPLACEDINSTANCEOFSHAREDPINUSABLEINCURRENTDESIGN(false, true,
				ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.OtherDesignSharedInstance"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableConductorDnDReason"), null),
		UNPLACEDINSTANCEOFSHAREDPINNOTUSABLEINCURRENTDESIGN(false, false,
				ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.SharedInstanceNotUsableInCurrentDesign"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableConductorDnDReason"), ResourceMgr
				.getString(ManageConnectorsAction.class, "ManageConnectorsAction.dialog.tooltip.DisablePinDnDReason")),
		PINHASNOCONDUCTOR(false, true, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.NoConductors"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableNoConductorDndReason"), null),
		LIBRARYCAVITYNOTUSED(false, true, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.librarycavitynotused"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableNoConductorDndReason"), null),
		DESIGNCANNOTBELOCKED(false, false, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.designcannotbelocked"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.designcannotbelockedDnDConductor"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.designcannotbelockedDndPin")),

		DESIGNCANNOTBEEDITED(false, false, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.designcannotbeedited"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.designcannotbeeditedDnDConductor"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.designcannotbeeditedDndPin")),
		CONNECTEDSHAREDPINLISTCANNOTBELOCKED(false, false, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.connectedSharedPinListCannotBeLocked"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.connectedSharedPinListCannotBeLocked"), ResourceMgr
				.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.connectedSharedPinListCannotBeLocked")),
		PINMATEUSAGEISREFERENCE(false, true, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.pinMateUsageIsReference"), ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.pinMateUsageIsReference"), ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.pinMateUsageIsReference")),
		PINUSAGEISREFERENCE(false, true, ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.pinUsageIsReference"), ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.pinUsageIsReference"), ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.pinUsageIsReference"));


		private boolean isEditable;
		private boolean isUsableInCurrentDesign;
		private String disableReason;
		private String disableConductorDnDReason;
		private String disablePinDnDReason;

		boolean isEditable()
		{
			return isEditable;
		}

		boolean isUsableInCurrentDesign()
		{
			return isUsableInCurrentDesign;
		}

		String getDisableReason()
		{
			return disableReason;
		}

		DisableReason(boolean isEditable, boolean isUsableInCurrentDesign, String disableReason,
				String disableConductorReason, String disablePinDnDReason)
		{
			this.isEditable = isEditable;
			this.isUsableInCurrentDesign = isUsableInCurrentDesign;
			this.disableReason = disableReason;
			disableConductorDnDReason = disableConductorReason;
			this.disablePinDnDReason = disablePinDnDReason;
		}

		public String getDisableConductorDnDReason()
		{
			return disableConductorDnDReason;
		}

		public String getDisablePinDnDReason()
		{
			return disablePinDnDReason;
		}

		public DisableReason designLocked(boolean success)
		{
			return this;
		}

		public DisableReason designNotEditable()
		{
			return this;
		}

	}

	public ManageConnectorConnectionsInfo(Connection connection,
			ManageConnectorPinSelections manageConnectorPinSelections,
			@Nullable IDesignDescriptor design)
	{
		super(connection.getOriginalPin(), connection.getConductor());

		mConnection = connection;
		this.manageConnectorPinSelections = manageConnectorPinSelections;
		this.design = design;
	}

	public ManageConnectorConnectionsInfo setDisabledReason(DisableReason disabledReason)
	{
		disableReason = disabledReason;
		return this;
	}

	public ManageConnectorConnectionsInfo setIsNewConnection()
	{
		mIsNewConnection = true;
		return this;
	}

	public boolean isNewConnection()
	{
		return mIsNewConnection;
	}

	public ManageConnectorConnectionsInfo setLibraried(boolean isLibraried)
	{
		mIsLibraried = isLibraried;
		return this;
	}

	@NotNull public ManageConnectorConnectionsInfo setSharedPinMateUID(@Nullable IUID mateUID)
	{
		m_sharedMateUID = mateUID;
		return this;
	}


	public boolean isLibraried()
	{
		return mIsLibraried;
	}

	public ManageConnectorChange getChange()
	{
		return mConnection.getConnectionChange();
	}

	public boolean updateConnection(String pinName, IDesignDescriptor designDescriptor)
	{
		if (!isEditable()) {
			return false;
		}

		IDesignDescriptor thisRowDesign = getDesign();
		if (thisRowDesign == null || !thisRowDesign.equals(designDescriptor)) {
			return false;
		}

		IPinProxy pinProxy = manageConnectorPinSelections.getPinByName(pinName, thisRowDesign);
		IPinProxy currentPinProxy = mConnection.getPin();
		if (pinProxy != null && !areEquivalent(pinProxy, currentPinProxy)) {
			mConnection.setPin(pinProxy);
			setFirst(pinProxy);

			return true;
		}

		return false;
	}

	@Nullable public IDesignDescriptor getDesign()
	{
		if (design != null) {
			return design;
		}
		//use cable pin of pinproxy to get the design.
		IAbstractPin originalPin = mConnection.getOriginalPin().getCablePin();
		IDesignDescriptor currentDesign =
				originalPin != null ? originalPin.getLogicDesign() : null;
		if (currentDesign != null) {
			return currentDesign;
		}
		if (getSecond() != null) {
			return getSecond().getDesignDescriptor();
		}
		return null;
	}

	public boolean updateConnection(String oldValue, String newValue,
			@NotNull IDesignDescriptor designInWhichPinChanged)
	{
		if (!isEditable()) {
			return false;
		}
		IDesignDescriptor thisRowDesign = getDesign();
		if (thisRowDesign != null && designInWhichPinChanged.getUID().isEquiv(getDesign().getUID())) {
			IPinProxy pinProxyToSearch = manageConnectorPinSelections.getPinByName(oldValue, thisRowDesign);

			IPinProxy currentPinProxy = mConnection.getOriginalPin();
			if (areEquivalent(currentPinProxy, Objects.requireNonNull(pinProxyToSearch))) {
				return updateConnection(newValue, thisRowDesign);
			}
		}

		return false;
	}

	public boolean isEditable()
	{
		return disableReason.isEditable();
//		if (disableReason.isEditable()) {
//			IPinProxy currentPinProxy = mConnection.getOriginalPin();
//			return currentPinProxy.getCablePin() != null;
//		}
//		return false;
	}

	public void setEditable()
	{
		IDesignDescriptor designDescriptor = getDesign();
		IDesign thisDesign = designDescriptor != null ? (IDesign) designDescriptor.getDesignContainer() : null;
		String connectedPinListId = getConnectedPinOwnerId();
		ISharedPinList connectedSharedPinList = StringUtils.isBlank(connectedPinListId) ? null :
				UIDMgr.getObjectOfType(connectedPinListId, ISharedPinList.class);
		if (thisDesign != null) {
			boolean isDesignEditable =
					(thisDesign.isEditable() && !CAFUtils.getInstance().isDesignOpenReadOnly(thisDesign));
			if (!isDesignEditable) {
				disableReason = disableReason.designNotEditable();
			}
			else if (!thisDesign.isLocked()) {
				disableReason = disableReason.designLocked(false);
			}
			else if (connectedSharedPinList != null && !connectedSharedPinList.isLocked()) {
				disableReason = DisableReason.CONNECTEDSHAREDPINLISTCANNOTBELOCKED;
			}
			else {
				disableReason = DisableReason.NONE;
			}
			validatePinForReferenceUsage();
		}
	}

	public boolean isUsableInCurrentDesign()
	{
		return disableReason.isUsableInCurrentDesign();
	}

	public IPinProxy getOriginalPin()
	{
		return mConnection.getOriginalPin();
	}

	public String getOriginalValue()
	{
		return getOriginalPin().getName();
	}

	public String applyOnPin(Function<IPinProxy, String> applyMethod)
	{
		IPinProxy pin = mConnection.getPin();
		return applyMethod.apply(pin);
	}

	public String applyOnWire(Function<IConductorProxy, String> applyMethod)
	{
		IConductorProxy conductor = mConnection.getConductor();
		return applyMethod.apply(conductor);
	}

	private boolean areEquivalent(IPinProxy pinProxy, IPinProxy anotherPinProxy)
	{

		return Objects.equals(pinProxy.getName(), anotherPinProxy.getName());
	}

	public String toString()
	{
		return mConnection.getPin().getName() +
				(mConnection.getConductor() != null ?
						mConnection.getConductor().getValueOfAttribute(IAttributeTypes.NAME) : "");
	}

	@Override public IPinProxy getFirst()
	{
		return mConnection.getPin();
	}

	@Override public IConductorProxy getSecond()
	{
		return mConnection.getConductor();
	}

	public String getCurrentStatus()
	{
		if (isEditable()) {
			return getChange().getChange();
		}
		return disableReason.getDisableReason();
	}

	@Nullable public String getDisableConductorDnDReason()
	{
		if (mConnection.getConductor() == null) {
			return ResourceMgr.getString(ManageConnectorsAction.class,
					"ManageConnectorsAction.dialog.tooltip.DisableNoConductorDndReason");
		}

		return disableReason.getDisableConductorDnDReason();
	}

	@Nullable public String getDisablePinDnDReason()
	{

		return disableReason.getDisablePinDnDReason();
	}

	@Override public int hashCode()
	{
		return hashCodeProvider.hashCode();
	}

	public String getConnectedPinOwnerId()
	{
		return getOriginalPin().getAttribute(getDesign(), IPinProxy.MATED_PIN_OWNER_ID);
	}

	@NotNull private MoveSwapErrorCode checkPinMateValid(@NotNull IPinProxy srcProxyPin,
			@NotNull IPinProxy targetProxyPin, @Nullable IDesignDescriptor targetDesign, boolean isSwap)
	{
		for (IPinMateValidator validator : pinMateValidators) {
			MoveSwapErrorCode moveSwapErrorCode =
					validator.validate(srcProxyPin, targetProxyPin, getDesign(), targetDesign, isSwap);
			if (moveSwapErrorCode != null) {
				return moveSwapErrorCode;
			}
		}
		return MoveSwapErrorCode.NoError;
	}

	public MoveSwapErrorCode checkPinChangeValid(@NotNull IPinProxy srcProxyPin, @NotNull IPinProxy targetProxyPin,
			@Nullable IDesignDescriptor targetDesign)
	{
		return checkPinChangeValid(srcProxyPin, targetProxyPin, targetDesign, false);
	}

	public MoveSwapErrorCode checkPinChangeValid(@NotNull IPinProxy srcProxyPin, @NotNull IPinProxy targetProxyPin,
			@Nullable IDesignDescriptor targetDesign, boolean isSwap)
	{
		final MoveSwapErrorCode moveSwapErrorCode = checkPinMateValid(srcProxyPin, targetProxyPin, targetDesign, isSwap);
		if (moveSwapErrorCode != MoveSwapErrorCode.NoError) {
			return moveSwapErrorCode;
		}

		return isJumperPin(targetProxyPin) ?
				isSwap ? MoveSwapErrorCode.SwappedPinIsJumperType : MoveSwapErrorCode.MovedToPinIsJumperType :
				MoveSwapErrorCode.NoError;
	}

	public MoveSwapErrorCode checkPinSwapValid(@NotNull IPinProxy srcProxyPin, @NotNull IPinProxy targetProxyPin,
			@Nullable IDesignDescriptor targetDesign)
	{
		final MoveSwapErrorCode moveSwapErrorCode =
				checkPinChangeValid(srcProxyPin, targetProxyPin, targetDesign, true);
		if (moveSwapErrorCode != MoveSwapErrorCode.NoError) {
			return moveSwapErrorCode;
		}

		return isJumperPin(srcProxyPin) ? MoveSwapErrorCode.SwappedPinIsJumperType : MoveSwapErrorCode.NoError;
	}

	private boolean isJumperPin(@NotNull IPinProxy targetProxyPin)
	{
		final PinTypeEnum pinType = targetProxyPin.getPinType();
		return pinType != null && PinTypeEnum.PINTYPE_JUMPER == pinType;
	}

	@Nullable public String isSwapAcceptableWith(ManageConnectorConnectionsInfo item)
	{
		MoveSwapErrorCode backshellSwapCheck = checkIfBackshellPinSwapOrMoveValid(item, item.getDesign(), true);
		if (backshellSwapCheck != null && !backshellSwapCheck.isSuccess()) {
			return ResourceMgr.getString(ManageConnectorsAction.class, backshellSwapCheck.getKey());
		}
		if (backshellSwapCheck != null && backshellSwapCheck.isSuccess()) {
			return null;
		}

		if (mConnection.getOriginalPin().getSharedPin() == null) {

			final MoveSwapErrorCode pinSwapCheck = checkPinSwapValid(getFirst(), item.getFirst(), getDesign());
			if (!pinSwapCheck.isSuccess()) {
				return ResourceMgr.getString(ManageConnectorsAction.class, pinSwapCheck.getKey());
			}
			return null;
		}
		IDesignDescriptor thisDesign = getDesign();
		IDesignDescriptor otherDesign = item.getDesign();

		if (thisDesign != null && otherDesign != null) {
			if (thisDesign.equals(otherDesign)) {
				final MoveSwapErrorCode pinSwapCheck = checkPinSwapValid(getFirst(), item.getFirst(), otherDesign);
				if (!pinSwapCheck.isSuccess()) {
					return ResourceMgr.getString(ManageConnectorsAction.class, pinSwapCheck.getKey());
				}
				return null;
			}

			ISharedPin thisCurrentPin = getFirst().getSharedPin();
			ISharedPin otherCurrentPin = item.getFirst().getSharedPin();

			if (thisCurrentPin.getReservationType() == ISharedPin.ReservationType.MANUAL &&
					!thisCurrentPin.getDesignReservationList().contains(otherDesign.getUID())) {
				return ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableDnDOnReservedPins");
			}
			if (otherCurrentPin.getReservationType() == ISharedPin.ReservationType.MANUAL &&
					!otherCurrentPin.getDesignReservationList().contains(thisDesign.getUID())) {
				return ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableDnDOnReservedPins");
			}
			if ((thisCurrentPin.getReservationType() == ISharedPin.ReservationType.AUTOMATIC ||
					otherCurrentPin.getReservationType() == ISharedPin.ReservationType.AUTOMATIC) &&
					!thisDesign.equals(otherDesign)) {
				return ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableDnDOnAutomaticReservedPins");
			}

			final MoveSwapErrorCode pinSwapCheck = checkPinSwapValid(getFirst(), item.getFirst(), otherDesign);
			if (!pinSwapCheck.isSuccess()) {
				return ResourceMgr.getString(ManageConnectorsAction.class, pinSwapCheck.getKey());
			}
			return null;
		}
		return ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.tooltip.DisableDnDWithUnplacedPins");
	}

	@Nullable
	private MoveSwapErrorCode checkIfBackshellPinSwapOrMoveValid(@NotNull ManageConnectorConnectionsInfo item,
			@Nullable IDesignDescriptor otherDesign, boolean isSwap)
	{
		return new BackshellTerminationValidator().validate(getFirst(), item.getFirst(),
				getDesign(), otherDesign, isSwap);
	}

	@Nullable public String isMoveAcceptableWith(ManageConnectorConnectionsInfo item)
	{

		MoveSwapErrorCode backshellSwapCheck =
				checkIfBackshellPinSwapOrMoveValid(item, item.getDesign(), false);
		if (backshellSwapCheck != null && !backshellSwapCheck.isSuccess()) {
			return ResourceMgr.getString(ManageConnectorsAction.class, backshellSwapCheck.getKey());
		}
		if (backshellSwapCheck != null && backshellSwapCheck.isSuccess()) {
			return null;
		}

		if (mConnection.getOriginalPin().getSharedPin() == null) {
			final MoveSwapErrorCode pinMoveCheck = checkPinChangeValid(getFirst(), item.getFirst(), getDesign());
			if (!pinMoveCheck.isSuccess()) {
				return ResourceMgr.getString(ManageConnectorsAction.class, pinMoveCheck.getKey());
			}
			return null;
		}
		IDesignDescriptor thisDesign = getDesign();
		IDesignDescriptor otherDesign = item.getDesign();

		if (thisDesign != null) {
			if (thisDesign.equals(otherDesign)) {
				final MoveSwapErrorCode pinMoveCheck = checkPinChangeValid(getFirst(), item.getFirst(), otherDesign);
				if (!pinMoveCheck.isSuccess()) {
					return ResourceMgr.getString(ManageConnectorsAction.class, pinMoveCheck.getKey());
				}
				return null;
			}

			if (!manageConnectorPinSelections.canUseThePinInDesign(item.getFirst(), thisDesign)) {
				return ResourceMgr.getString(ManageConnectorsAction.class,
						"ManageConnectorsAction.dialog.tooltip.DisableMoveOnReservedPins");
			}

			final MoveSwapErrorCode pinMoveCheck =
					checkPinChangeValid(getFirst(), item.getFirst(), otherDesign != null ? otherDesign : thisDesign);
			if (!pinMoveCheck.isSuccess()) {
				return ResourceMgr.getString(ManageConnectorsAction.class, pinMoveCheck.getKey());
			}
		}
		return null;
	}

	public static String cellOutOfRange()
	{
		return ResourceMgr.getString(ManageConnectorConnectionsInfo.class,
				"ManageConnectorConnectionsInfo.CellOutOfRange");
	}

	public void validatePinForReferenceUsage()
	{
		ILogicDesign logicDesign = CommonUtils.cast(design.getDesignContainer(), ILogicDesign.class);
		if (disableReason != DisableReason.NONE || logicDesign == null) {
			return;
		}
		IPinProxy pinProxy = getFirst();
		if (pinProxy.getCablePin() != null) {
			checkReferenceInstance(pinProxy.getCablePin(), logicDesign);
		}
		else if (pinProxy.getSharedPin() != null) {
			checkReferenceInstance(pinProxy.getSharedPin(), logicDesign);
		}
	}

	public boolean isBackshellTermination()
	{
		IPinProxy pin = getFirst();
		if (pin.getSharedPin() != null) {
			return pin.getSharedPin() instanceof ISharedBackshellTermination;
		}
		if (pin.getCablePin() != null) {
			return pin.getCablePin() instanceof IBackshellTermination;
		}
		if (pin.getLibraryCavity() != null) {
			return pin.getLibraryCavity().getOwner().getGroupName() == ILibraryObject.GroupType.BACKSHELL;
		}
		return false;
	}

	private boolean checkReferenceInstance(@NotNull ISharedPin sharedPin, @NotNull ILogicDesign logicDesign)
	{
		ISharedPinList sharedPinList = sharedPin.getOwner();
		if (sharedPinList instanceof ISharedConnector) {
			ISharedPin matePin = getMatePin(sharedPin, logicDesign);
			if (matePin != null) {
				 if(isSharedPinUsageReference(sharedPin, logicDesign)){
					 disableReason = DisableReason.PINUSAGEISREFERENCE;
				 }
				 else if(isSharedPinUsageReference(matePin, logicDesign)){
					 disableReason = DisableReason.PINMATEUSAGEISREFERENCE;
				 }
			}
		}
		return false;
	}

	@Nullable private ISharedPin getMatePin(@NotNull ISharedPin sharedPin, @NotNull ILogicDesign logicDesign)
	{
		if (sharedPin.getMatePin() != null) {
			return sharedPin.getMatePin();
		}
		if (logicDesign.getLoadedConnectivity() != null) {
			return getMateFromConnectivity(logicDesign.getLoadedConnectivity(), sharedPin);
		}
		return getMatePinFromPinInfo();
	}

	@Nullable
	private ISharedPin getMateFromConnectivity(@NotNull IConnectivity connectivity, @NotNull ISharedPin sharedPin)
	{
		IAbstractPin cablePin = CommonUtils.cast(connectivity.findLogicObjectForShared(sharedPin), IAbstractPin.class);
		if (cablePin != null) {
			Iterator<IAbstractPin> iterator = cablePin.getConnectedPins().iterator();
			IAbstractPin matePin = iterator.hasNext() ? iterator.next() : null;
			if (matePin != null) {
				return matePin.getSharedPin();
			}
		}
		return null;
	}

	@Nullable private ISharedPin getMatePinFromPinInfo()
	{
		if (m_sharedMateUID != null) {
			return UIDMgr.getObjectOfType(m_sharedMateUID, ISharedPin.class);
		}
		return null;
	}

	private boolean isSharedPinUsageReference(@NotNull ISharedPin sharedPin, @NotNull ILogicDesign logicDesign)
	{
		return logicDesign.getSharedUsageMgr().
				getUsages(sharedPin).
				stream()
				.filter(usage -> usage.isReference())
				.findAny().isPresent();
	}

	private void checkReferenceInstance(@NotNull IAbstractPin abstractPin, @NotNull ILogicDesign logicDesign)
	{
		IPinList pinList = abstractPin.getOwner();
		if (pinList instanceof IJackConnector || pinList instanceof IPlugConnector) {
			Iterator<IAbstractPin> iterator = abstractPin.getConnectedPins().iterator();
			IAbstractPin matePin = iterator.hasNext() ? iterator.next() : null;
			if (matePin != null) {
				if (isPinUsageReference(abstractPin, logicDesign)) {
					disableReason = DisableReason.PINUSAGEISREFERENCE;
				}
				else if (isPinUsageReference(matePin, logicDesign)) {
					disableReason = DisableReason.PINMATEUSAGEISREFERENCE;
				}
			}
		}
	}

	private boolean isPinUsageReference(@NotNull IAbstractPin pin, @NotNull ILogicDesign logicDesign)
	{
		return logicDesign.getDesignWideUsageMgr().
				getUsages(pin).stream()
				.filter(usage -> usage.isReference())
				.findAny().isPresent();
	}
}
