package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.IPinListShareContextProvider;
import chs.caplets.logic.actions.shared.helper.EditSharedPinlistHandler;
import chs.caplets.logic.actions.shared.helper.IEditSharedPinlistAdapter;
import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ui.property.IStringProperty;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.revisioning.ValidationObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoSharePinlistView
		implements ChangeListener, IPinListShareContextProvider, IEditSharedPinlistAdapter
{

	// Delegate to perform model changes and other business logic, reused in Auto-share flow.
	@NotNull private final EditSharedPinlistHandler mHandler;
	@Nullable protected AutoSelectSharedView mSelectSharedView = null;
	@Nullable protected AutoReuseView mReuseView = null;
	@Nullable protected AutoMapView mMapView = null;
	@NotNull private IShareMessageContextReporter mMessageReporter;
	private final boolean mIsBulkShare;
	@NotNull protected AutoShareParams m_params;

	public AutoSharePinlistView(@Nullable chs.cof.logical.cable.IPinList cpl, @Nullable IPinList pl,
			@NotNull ILogicDesign design, @NotNull IMessageReporterWithContext reporter, boolean isBulkShare,
			@NotNull AutoShareParams params)
	{
		mIsBulkShare = isBulkShare;
		m_params = params;
		final EditSharedPinListModel esplModel = new EditSharedPinListModel(pl, cpl, null);
		esplModel.addChangeListener(this);
		mMessageReporter = new AutoShareMessageReporterWrapper(reporter, () -> getMessageContext(pl, cpl));
		mHandler = new EditSharedPinlistHandler(esplModel, design, cpl, this, mMessageReporter, isBulkShare);
		mHandler.initializeComponents(null, cpl, pl, false);
		initPanels();
		mHandler.refreshNameMgr();
	}

	private void initPanels()
	{
		if (mMapView != null) {
			mMapView.init();
		}
		if (mReuseView != null) {
			mReuseView.init();
		}
		if (mSelectSharedView != null) {
			mSelectSharedView.init();
		}
	}

	public boolean execute()
	{
		final EditSharedPinlistHandler.Status status = mHandler.evaluateStatus();
		if (status.isOkEnabled()) {
			return mHandler.onCompletion((mgr) -> {
			}, (prop) -> true, null);
		}
		cancel();
		mHandler.reportError(status.getOkStatusMessage());
		return false;
	}

	protected void cancel()
	{
		mHandler.onCancel();
	}

	protected boolean extendedPinMatch()
	{
		return true;
	}

	protected boolean mateCompatibilityCheck()
	{
		return m_params.getMateCompatibilityCheck();
	}

	public void stateChanged(ChangeEvent e)
	{
		final EditSharedPinlistHandler.Status status = mHandler.evaluateStatus();
		if (mMapView != null) {
			mMapView.setEnabled(status.isMapperEnabled());
		}
		if (mReuseView != null) {
			mReuseView.setEnabled(status.isReuseEnabled());
		}
	}

	@NotNull protected IMessageContext getMessageContext(@Nullable IPinList schemPinlist,
			@Nullable chs.cof.logical.cable.IPinList cablePinlist)
	{
		return AutoSharePinListActionHelper.determineMessageContext(schemPinlist, cablePinlist);
	}

	@Override
	public void initSelectSharedComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design,
			boolean fromSymbol)
	{
		mSelectSharedView =
				new AutoSelectSharedView(esplModel, design, fromSymbol, mMessageReporter, isShareInto(), mIsBulkShare);
		final IStringProperty nameProperty = mSelectSharedView.getNameProperty();
		if (fromSymbol && nameProperty != null) {
			nameProperty.setValue(esplModel.getSymbolDef().getName());
		}
	}

	protected boolean isShareInto()
	{
		return false;
	}

	@Override public void initEditSymbolComponent(@NotNull EditSharedPinListModel esplModel)
	{

	}

	@Override public void initMapperComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design)
	{
		mMapView = new AutoMapView(esplModel, design, mMessageReporter, extendedPinMatch(), mateCompatibilityCheck(),
				mIsBulkShare);
	}

	@Override public void initReuseComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design)
	{
		mReuseView = new AutoReuseView(esplModel, design, mMessageReporter, mIsBulkShare);
	}

	@Override public void initAnalysisComponent(@NotNull ISharedPinList spl, @NotNull ILogicDesign design)
	{

	}

	@Override public void initModularComponent(@NotNull ISharedPinList spl)
	{

	}

	@Override public boolean hasSelectSharedComponent()
	{
		return mSelectSharedView != null;
	}

	@Override public boolean hasEditSymbolComponent()
	{
		return false;
	}

	@Override public boolean hasMapperComponent()
	{
		return mMapView != null;
	}

	@Override public boolean hasReuseComponent()
	{
		return mReuseView != null;
	}

	@Override public boolean hasAnalysisComponent()
	{
		return false;
	}

	@Override public boolean isModularClientModified()
	{
		return false;
	}

	@Override public boolean hasAnalysisComponentChanged()
	{
		return false;
	}

	@NotNull @Override public ValidationObject getModularErrors()
	{
		return new ValidationObject();
	}

	@Override public void updateAnalysisPinMap(@Nullable Map<ISharedPin, String> sharedPinAndTransientNameMap)
	{

	}

	@Override public boolean isBackshellCompatible(@NotNull ISharedPinList spl)
	{
		return mSelectSharedView != null && mSelectSharedView.isBackshellCompatible(spl);
	}

	@Override public void initSharedDomainComponent(@NotNull ISharedPinList spl)
	{

	}

	@Override public boolean hasSharedDomainChanged()
	{
		return false;
	}

	@Nullable @Override public Map<IAbstractPin, IPinProxy> getInstanceToSharedMap()
	{
		return mHandler.getConnectivityToSharedMap();
	}

	@Override public boolean preserveInternalConnectivity()
	{
		return mHandler.preserveInternalConnectivity();
	}

	@Nullable @Override public List<IPinProxy> getPlugMapInfo()
	{
		return new ArrayList<IPinProxy>(mHandler.getProxies());
	}

	@Nullable @Override public Set<IPinProxy> getReusablePins()
	{
		return new HashSet<IPinProxy>(mHandler.getReusableProxies());
	}

	@Nullable @Override public ISharedPinList getSharedPinList()
	{
		return mHandler.getSharedPinList();
	}

	@Nullable @Override public String getSharedObjectMateName()
	{
		return mHandler.getSharedPinListMateName();
	}

	@Override public boolean isSharedMateNameGenerated()
	{
		return mHandler.isSharedPinListMateNameGenerated();
	}

	@Nullable @Override public String getSharedObjectMateRevision()
	{
		return mHandler.getSharedPinListMateRevision();
	}

	@Nullable @Override public String getSharedPinListName()
	{
		return mHandler.getSharedPinListName();
	}

	@Nullable @Override public String getSharedPinListRevision()
	{
		return mHandler.getSharedPinListRevision();
	}

	@Override public boolean isSharedNameGenerated()
	{
		return mHandler.isSharedPinListNameGenerated();
	}

	@Nullable @Override public Map<IPinProxy, IAbstractPin> getSharedToInstanceMap()
	{
		return mHandler.getSharedToConnectivityMap();
	}

	@Nullable @Override public String getAnalysisModel()
	{
		return mHandler.getAnalysisModel();
	}

	@Nullable @Override public String getAnalysisFunctionRealiser()
	{
		return mHandler.getAnalysisFunctionRealiser();
	}

	@Nullable @Override public String getOverriddenAnalysisInterfaces()
	{
		return mHandler.getOverriddenAnalysisInterfaces();
	}

	@Nullable @Override public String getOverriddenAnalysisFailureModes()
	{
		return mHandler.getOverriddenAnalysisFailureModes();
	}

	@Override public boolean reusablePinErrors()
	{
		return mHandler.reusablePinErrors();
	}

	@Override public Map<ISharedPin, ISharedPin> getConnectedPinsToMakeReusable()
	{
		return mHandler.getConnectedPinsToMakeReusable();
	}

	@Nullable @Override public Map<IConnector, String> getModularConnectorToSharedNamesMap()
	{
		return mHandler.getModularConnectorToSharedNamesMap();
	}

	@Nullable @Override public Map<IConnector, Boolean> getModularConnectorToSharedNameGeneratedMap()
	{
		return mHandler.getModularConnectorToSharedNameGeneratedMap();
	}

	@Nullable @Override public ISymbolDef getSymbolDef()
	{
		return mHandler.getSymbolDef();
	}

	@Override public boolean canMakePinsReserved()
	{
		return false;
	}
}
