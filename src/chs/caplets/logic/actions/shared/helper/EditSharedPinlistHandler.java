/*
 * Copyright 2019-2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.helper;

import chs.analysis.CapitalAnalysisFactory;
import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.EditSharedPinlistDialog;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IProject;
import chs.cof.security.IDomain;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.ui.BaseSharedDomainPanel;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.utilities.ResourceMgr;
import chs.utilities.ReverseMap;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.ValidityChangeEvent;
import chs.utility.AnalysisHelper;
import chs.utility.SymbolUtils;
import chs.utility.helpers.revisioning.ValidationObject;
import chs.utility.logic.LogicObjectUtils;
import chs.utility.logic.ModularConnectorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class EditSharedPinlistHandler extends BaseSharePinlistHandler implements IPropertyValidityListener
{

	private boolean reusablePinErrors;
	@Nullable private IStringProperty m_nameProperty;
	@Nullable private final IPinList cableSource;
	@NotNull private final IEditSharedPinlistAdapter mEditSharedPinlistAdapter;

	/**
	 * This field holds the original Analysis simulation model path and name, used when a logic object is being shared.
	 */
	@Nullable private String analysisModel;
	/**
	 * This holds the analysis functional realiser
	 */
	@Nullable private String analysisRealiser;
	@Nullable private String overriddenAnalysisInterfaces;
	@Nullable private String overriddenAnalysisFailureModes;

	public EditSharedPinlistHandler(
			@NotNull EditSharedPinListModel esplModel,
			@NotNull ILogicDesign design,
			@Nullable IPinList cpl,
			@NotNull IEditSharedPinlistAdapter adapter,
			@Nullable IShareMessageContextReporter reporter, boolean isBulkShare)
	{
		super(esplModel, design, reporter, isBulkShare);
		mEditSharedPinlistAdapter = adapter;
		reusablePinErrors = false;
		cableSource = cpl;
		esplModel.addPropertyValidityChangeListener(this);
	}

	public EditSharedPinlistHandler(
			@NotNull EditSharedPinListModel esplModel,
			@NotNull ILogicDesign design,
			@Nullable IPinList cpl,
			@NotNull IEditSharedPinlistAdapter adapter)
	{
		this(esplModel, design, cpl, adapter, null, false);
	}

	public void initializeComponents(@Nullable ISharedPinList spl, @Nullable IPinList cpl,
			@Nullable chs.cof.logical.schem.IPinList pl, boolean fromSymbol)
	{
		boolean isSplice = isSplice(spl, pl);

		if (isShare()) {
			mEditSharedPinlistAdapter.initSelectSharedComponent(getModel(), getDesign(), fromSymbol);
			if (fromSymbol) {
				setSharedPinListName(getSymbolDef().getName());
			}
		}
		else if ((!isSplice && isSymbolDefEditable()) || getSharedPinList().hasSymbols()) {
			mEditSharedPinlistAdapter.initEditSymbolComponent(getModel());
		}

		if (!isModularConnectorWithAtLeastOneFilledPosition(cpl) &&
				(allowAddPins() || mEditSharedPinlistAdapter.hasSelectSharedComponent() ||
						mEditSharedPinlistAdapter.hasEditSymbolComponent())) {
			mEditSharedPinlistAdapter.initMapperComponent(getModel(), getDesign());
		}

		if (!isModularConnectorWithAtLeastOneFilledPosition(cpl) && !isSplice && allowPinReuseManagement()) {
			mEditSharedPinlistAdapter.initReuseComponent(getModel(), getDesign());
		}
		if (cpl != null && isModularConnectorWithAtLeastOneFilledPosition(cpl)) {
			Map<IPinProxy, IAbstractPin> tempsharedToInstance = new HashMap<IPinProxy, IAbstractPin>();
			List<IPinProxy> pinProxies =
					ModularConnectorHelper.createProxiesForModularConnector((IConnector) cpl, tempsharedToInstance);
			getProxies().addAll(pinProxies);
			for (Map.Entry<IPinProxy, IAbstractPin> entry : tempsharedToInstance.entrySet()) {
				getConnectivityToSharedMap().put(entry.getValue(), entry.getKey());
			}
		}

		// Do Analysis stuff.
		analysisModel = null;
		analysisRealiser = null;
		if (isAnalysisInstalled()) {
			// If Analysis is installed and the object is already shared, add the Analysis simulation model attachment panel.
			if (spl != null) {
				// We only get here if the object is already shared, i.e. it is NOT being shared.
				mEditSharedPinlistAdapter.initAnalysisComponent(spl, getDesign());
			}
			// If Analysis is installed and the object is being shared via Create New, store the current object's Analysis
			// simulation model for copying (on successful completion) into the new shared object.
			else if (pl != null) {
				// We only get here if the object is NOT already shared, i.e. it is being shared.
				if (pl.getConnectivity() != null) {
					ILogicObject logObj = pl.getConnectivity();
					analysisModel = logObj.getAnalysisModel();
					analysisRealiser = logObj.getAnalysisFunctionRealiser();
					overriddenAnalysisInterfaces = logObj.getOverriddenAnalysisInterfaces();
					overriddenAnalysisFailureModes = logObj.getOverriddenAnalysisFailureModes();
				}
			}
		}

		if (spl != null && LogicObjectUtils.isValidPositionContainer(spl.getType())) {
			mEditSharedPinlistAdapter.initModularComponent(spl);
		}
		if (spl != null && spl.supportsDomain()) {
			mEditSharedPinlistAdapter.initSharedDomainComponent(spl);
		}
	}

	protected boolean isAnalysisInstalled()
	{
		return CapitalAnalysisFactory.getAnalysisInterface() != null;
	}

	public boolean isSplice(@Nullable ISharedPinList spl,
			@Nullable chs.cof.logical.schem.IPinList pl)
	{
		return (spl != null && PinListTypeEnum.TypeSplice.equals(spl.getType()))
				|| (pl != null && pl.getConnectivity() instanceof ISplice);
	}

	public boolean onCompletion(@NotNull Consumer<ISharedPinListMgr> pinListMgrLockWarn, @NotNull
			Function<IStringProperty, Boolean> duplicateNameWarn, @Nullable Set<IDomain> domains)
	{
		final IProject project = getDesign().getProject();
		if (project == null) {
			throw new IllegalArgumentException();
		}
		ISharedPinListMgr pinListMgr = project.getSharedPinListMgr();
		ISharedPinList sharedPinList = getSharedPinList();
		boolean isShare = isShare();
		//(10.2)dts0100741001 SharedPinList is Locked in Capital Project when we do share into in Logic
		//In case of share-into, unlock the selected spl...Not required for "EditSharedPinListAction"
		if (isShare && sharedPinList != null) {
			unlock(sharedPinList);
		}
		boolean complete = true;
		if (isShare && sharedPinList == null && !lock(pinListMgr)) {
			pinListMgrLockWarn.accept(pinListMgr);
			complete = false;
		}

		final IStringProperty nameProperty = getNameProperty();
		if (complete && nameProperty != null &&
				sharedPinList == null && nameProperty.getNoteString() != null &&
				nameProperty.getMaxNoteLevel() == IProperty.NoteTagLevel.Warning) {
			//display dialog to confirm duplicate names
			complete = duplicateNameWarn.apply(nameProperty);
		}

		if (complete && domains != null) {
			complete = BaseSharedDomainPanel.canChangeDomainAccess(domains, sharedPinList);
		}

		if (complete) {
			sanitizeProxies();
		}
		return complete;
	}

	private boolean lock(@NotNull ISharedPinListMgr pinListMgr)
	{
		return isBulkShare() ? pinListMgr.isLocked() : pinListMgr.lock();
	}

	private void unlock(@NotNull ISharedPinList sharedPinList)
	{
		if (!isBulkShare()) {
			SharedPinListHelper.unlock(sharedPinList);
		}
	}

	public void onCancel()
	{
		ISharedPinList spl = getSharedPinList();
		if (spl != null) {
			unlock(spl);
		}
	}

	public boolean reusablePinErrors()
	{
		return reusablePinErrors;
	}

	/**
	 * This method calculates the display name of the cable source's type.
	 * <p>
	 * Inlines are given a generic name, other names are retrieved directly from their COF types..
	 *
	 * @return String, a string containing the display name..
	 */
	public String getObjectTypeDisplayName()
	{
		// if cableSource is null we're in trouble..
		assert cableSource != null;

		if (cableSource instanceof IInlineJackConnector || cableSource instanceof IInlinePlugConnector) {
			return ResourceMgr.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.inline");
		}
		else {
			return COFTypeEnum.from_object(cableSource).toString();
		}
	}

	// todo The first loop in this function is necessary. The second loop probably isn't, though it gives this function
	// its name. the second part should probably be removed and this function's name changed to reflect the first part.

	private void sanitizeProxies()
	{
		// If we changed the symbol def, and it has more pins than the shared pinlist, add enough extra proxies
		// to fill it out.
		if (getSymbolDef() != null && getSharedPinList() != null
				&& getSharedPinList().getType() != PinListTypeEnum.TypeDevice
				&& !getSharedPinList().isFunctionType()
				&& getSymbolDef().getNumPins() > getSharedPinList().getPins().getSize()) {
			for (IAbstractPin pin : SymbolUtils.getConnectivityPins(getSymbolDef())) {
				if (!getConnectivityToSharedMap().containsKey(pin)) {
					PinProxy proxy = createPinProxy(pin);
					getProxies().add(proxy);
					getConnectivityToSharedMap().put(pin, proxy);
				}
			}
		}
		// todo This was here to clean up data problems caused by a bug that probably doesn't exist anymore
		for (IPinProxy pp : getProxies()) {
			if (pp.getSharedPin() == null && pp.getCablePin() != null) {
				pp.setCablePin(null);
			}
		}
	}

	@NotNull protected PinProxy createPinProxy(IAbstractPin pin)
	{
		return new PinProxy(pin.getName(), false);
	}

	@NotNull public Status evaluateStatus()
	{
		final Status status = new Status(mEditSharedPinlistAdapter.hasMapperComponent());

		// removed assertion that:
		//  esplModel.getConnectivityToSharedMap().size() == esplModel.getConnectivityToSharedMap().getReverseMap().size();
		// Use of the Remove button causes this failure, the Swing listener mechanism masks the exception
		// and the exception prevents execution of the logic that disables the OK button.

		if (mEditSharedPinlistAdapter.hasReuseComponent()) {
			for (int i = 0; !status.isReuseEnabled() && i < getProxies().size(); i++) {
				PinProxy proxy = (PinProxy) getProxies().get(i);
				if (proxy.getSharedPin() == null || !proxy.getSharedPin().isReusable() || canMakePinReserved(proxy)) {
					status.setReuseEnabled(true);
				}
			}
		}

		// Shared pinlist selected or named?
		IPinList cpl = getCablePinlist();
		ISharedPinList spl = getSharedPinList();
		if (spl == null
				&& (getSharedPinListName() == null || getSharedPinListRevision() == null
				|| ((cpl instanceof IInlineJackConnector || cpl instanceof IInlinePlugConnector)
				&& getSharedPinListMateName() == null))) {
			status.setOkEnabled(false);
			status.setMapperEnabled(false);
			status.setReuseEnabled(false);
		}
		if (spl == null && ModularConnectorHelper.doesModularConnectorHasAtleastOneFilledPosition(cpl)) {
			status.setMapperEnabled(false);
			status.setReuseEnabled(false);
			status.setOkEnabled(getModularConnectorTreeValidity() && (getSharedPinListRevision() != null));
		}
		// Pin mapping done where required?
		boolean validPinMapping = true;
		if (status.isOkEnabled() && cpl != null) {
			validPinMapping = checkPinMapping(status, cpl);
		}

		if (status.isOkEnabled() && mEditSharedPinlistAdapter.isModularClientModified()) {
			ValidationObject modularErrors = mEditSharedPinlistAdapter.getModularErrors();
			status.setOkEnabled(!modularErrors.hasWarningsOrErrors());
			if (!status.isOkEnabled()) {
				status.setOkStatusMessage(getDisplayName(modularErrors));
			}
			status.setModularModified(true);
		}

		// The data is good. But has anything changed? If this is a share, or a symbol was added, then from
		// the previous test we know changes were made. Otherwise, some pins have to have been added or made reusable
		// or a symbol instance deletion for any change to have been made.
		if (status.isOkEnabled() && cpl == null && spl != null) {
			if (getSymbolInstancesForDeletion().isEmpty() &&
					getSymbolDefsForAddition().isEmpty() &&
					getProxies().size() == spl.getPins().getSize() &&
					getReusableProxies().size() == spl.getUnrestrictedPins().getSize() && !status.isModularModified()) {
				status.setOkEnabled(false);
				for (IPinProxy pinProxy : getProxies()) {
					if (pinProxy.getSharedPin() == null) {
						status.setOkEnabled(
								true);        ////this is a change, a pin was added and an existing pin was deleted.
					}
					else if (!pinProxy.getSharedPin().getName().equals(pinProxy.getName())) {
						status.setOkEnabled(true);
					}
				}
			}
		}

		// Ensure that the attachment panel knows about any pin changes.
		updateAnalysisPinMap(spl);

		// If any data on the Analysis tab attachment panel has been changed, ensure ok is enabled.
		if (validPinMapping && mEditSharedPinlistAdapter.hasAnalysisComponentChanged()) {
			status.setOkEnabled(true);
		}

		// If any data on the Shared Domain Field has been changed, ensure ok is enabled.
		if (validPinMapping && mEditSharedPinlistAdapter.hasSharedDomainChanged()) {
			status.setOkEnabled(true);
		}

		//Check if there are duplicate Pin Names : possible with regard to Composite Symbol
		if (hasDuplicatePinNames()) {
			status.setOkStatusMessage(ResourceMgr
					.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.duplicatepin.tooltip"));
			status.setOkEnabled(false);
			status.setbHasDuplicatePins(true);
		}

		if (status.isOkEnabled() && spl != null && mEditSharedPinlistAdapter.hasSelectSharedComponent()) {
			status.setOkEnabled(mEditSharedPinlistAdapter.isBackshellCompatible(spl));
			if (!status.isOkEnabled()) {
				status.setOkStatusMessage(ResourceMgr.getString(EditSharedPinlistDialog.class,
						"EditSharedPinlistDialog.backshell.tooltip"));
			}
		}

		if (status.isMapperEnabled()) {
			status.setMapperEnabled(allowAddPins() || cpl != null);
		}
		return status;
	}

	private boolean canMakePinReserved(@NotNull PinProxy proxy)
	{
		return mEditSharedPinlistAdapter.canMakePinsReserved() && allowRemove(proxy.getSharedPin());
	}

	private boolean allowRemove(@Nullable ISharedPin spin)
	{
		return spin != null && spin.isReusable() && !getModel().isSharedPinUsed(spin);
	}

	@NotNull private String getDisplayName(@NotNull ValidationObject validationObj)
	{
		StringBuilder errorMessage = new StringBuilder();
		if (validationObj.hasWarningsOrErrors()) {
			errorMessage.append("<html>");
			for (String error : validationObj.getErrors()) {
				errorMessage.append(error).append("<br>");
			}
			for (String warning : validationObj.getWarnings()) {
				errorMessage.append(warning).append("<br>");
			}
			errorMessage.append("</html>");
		}
		return errorMessage.toString();
	}

	public boolean hasDuplicatePinNames()
	{
		// Get the current list of pin names from the pin proxies.
		Set<String> pinNames = new HashSet<String>();
		for (IPinProxy pp : getProxies()) {
			pinNames.add(pp.getName());
		}
		return pinNames.size() < getProxies().size();
	}

	/**
	 * @param itr       - iterator on pins on symbol
	 * @param symbolDef - symbol for which we need to check compatibility
	 * @return true if the symboldef is compatible with shared pinlist
	 */
	private boolean areSymbolPinMappingCompatible(@NotNull Iterator<IAbstractPin> itr, @Nullable ISymbolDef symbolDef)
	{
		while (itr.hasNext()) {
			IAbstractPin pin = itr.next();
			IPinProxy pp = null;
			if (symbolDef != null) {
				ReverseMap<IAbstractPin, IPinProxy> connectivityToSharedMap = getConnectivityToSharedMap(symbolDef);
				if (connectivityToSharedMap != null) {
					pp = connectivityToSharedMap.get(pin);
				}
				assert connectivityToSharedMap != null;
			}
			else {
				pp = getConnectivityToSharedMap().get(pin);
			}

			if (pp != null) {/* Moattia-FEAT14427: We now allow reusable pin connection with a non-reusable pin.
				// dts0100618323
				// don't allow a share if it would result in a non-reusable shared pin connected to a non-reusable pin
				IAbstractPin connectedPin = pin.getConnectedPin();
				if (connectedPin != null && connectedPin.isShared()) {
					boolean connectedReusable = connectedPin.getSharedPin().isReusable();
					boolean reusable = esplModel.getReusableProxies().contains(pp);
					if (connectedReusable != reusable) {

						//return false;
					}
				}*/
			}
			else {
				return false;
			}
		}
		return true;
	}

	private boolean checkPinMapping(@NotNull Status status, @NotNull IPinList cpl)
	{
		Iterator<IAbstractPin> itr;
		if (isShare()) {
			itr = cpl.getPins();
			status.setOkEnabled(areSymbolPinMappingCompatible(itr, null));
			return status.isOkEnabled();
		}

		// dts0100590894 - CALL home - If I add one symbol in "edit shared folder" where i have already one symbol
		// To fix the above defect, we need to check all the added symbols are compatible, even if one of them
		// is incompatible then disable OK Button
		Collection<ISymbolDef> addedSymbolDefs = getSymbolDefsForAddition();
		for (ISymbolDef symbolDef : addedSymbolDefs) {
			itr = SymbolUtils.getConnectivityPins(symbolDef).iterator();
			status.setOkEnabled(areSymbolPinMappingCompatible(itr, symbolDef));
			if (!status.isOkEnabled()) {
				return false;
			}
		}
		return true;
	}

	private void updateAnalysisPinMap(@Nullable ISharedPinList spl)
	{
		if (mEditSharedPinlistAdapter.hasAnalysisComponent() && spl != null) {
			if (AnalysisHelper.getInstance().isLegacyAnalysisMode() && getProxies().size() == spl.getPins().getSize()) {
				mEditSharedPinlistAdapter.updateAnalysisPinMap(null);
			}
			else {
				// Get the current list of pin names from the pin proxies.
				Map<ISharedPin, String> sharedPinAndTransientNameMap = new HashMap<>();
				for (IPinProxy pp : getProxies()) {
					sharedPinAndTransientNameMap.put(pp.getSharedPin(), pp.getName());
				}
				mEditSharedPinlistAdapter.updateAnalysisPinMap(sharedPinAndTransientNameMap);
			}
		}
	}

	public void refreshNameMgr()
	{
		final IProject project = getDesign().getProject();
		if (project != null) {
			project.getNameMgr().refresh();
		}
	}

	public void validityChanged(ValidityChangeEvent evt)
	{
		//set the name property
		m_nameProperty = (IStringProperty) evt.getProperty();
	}

	public void invalidReasonChanged(IProperty property)
	{
		// No action required.
	}

	@Nullable public IStringProperty getNameProperty()
	{
		return m_nameProperty;
	}

	@Nullable public String getAnalysisModel()
	{
		return analysisModel;
	}

	@Nullable public String getAnalysisFunctionRealiser()
	{
		return analysisRealiser;
	}

	@Nullable public String getOverriddenAnalysisInterfaces()
	{
		return overriddenAnalysisInterfaces;
	}

	@Nullable public String getOverriddenAnalysisFailureModes()
	{
		return overriddenAnalysisFailureModes;
	}

	public static class Status
	{

		private boolean mapperEnabled;
		private boolean reuseEnabled = false;
		private boolean okEnabled = true;
		private boolean modularModified = false;
		private boolean bHasDuplicatePins = false;
		@Nullable private String okStatusMessage = null;

		public Status(boolean m)
		{
			mapperEnabled = m;
		}

		public boolean isMapperEnabled()
		{
			return mapperEnabled;
		}

		public boolean isReuseEnabled()
		{
			return reuseEnabled;
		}

		public boolean isOkEnabled()
		{
			return okEnabled;
		}

		public boolean isModularModified()
		{
			return modularModified;
		}

		public boolean hasDuplicatePins()
		{
			return bHasDuplicatePins;
		}

		@Nullable public String getOkStatusMessage()
		{
			return okStatusMessage;
		}

		public void setMapperEnabled(boolean mapperEnabled)
		{
			this.mapperEnabled = mapperEnabled;
		}

		public void setReuseEnabled(boolean reuseEnabled)
		{
			this.reuseEnabled = reuseEnabled;
		}

		public void setOkEnabled(boolean okEnabled)
		{
			this.okEnabled = okEnabled;
		}

		public void setModularModified(boolean modularModified)
		{
			this.modularModified = modularModified;
		}

		public void setbHasDuplicatePins(boolean bHasDuplicatePins)
		{
			this.bHasDuplicatePins = bHasDuplicatePins;
		}

		public void setOkStatusMessage(@Nullable String okStatusMessage)
		{
			this.okStatusMessage = okStatusMessage;
		}
	}
}
