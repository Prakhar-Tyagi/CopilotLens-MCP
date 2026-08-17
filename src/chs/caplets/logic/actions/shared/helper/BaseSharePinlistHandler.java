package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ReverseMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.SortedListModel;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.ui.SharedPinListSymbolInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeListener;
import java.util.Collection;
import java.util.Map;

public class BaseSharePinlistHandler
{

	@NotNull private EditSharedPinListModel mESPLModel;
	@NotNull private ILogicDesign mDesign;
	@Nullable private IShareMessageContextReporter mMessageReporter;
	private final boolean mIsBulkShare;

	public BaseSharePinlistHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@Nullable IShareMessageContextReporter reporter, boolean isBulkPromotion)
	{
		mESPLModel = model;
		mDesign = design;
		mMessageReporter = reporter;
		mIsBulkShare = isBulkPromotion;
	}

	@NotNull public EditSharedPinListModel getModel()
	{
		return mESPLModel;
	}

	@NotNull public ILogicDesign getDesign()
	{
		return mDesign;
	}

	public void reportError(@Nullable String message)
	{
		if (mMessageReporter != null && !StringUtils.isBlank(message)) {
			mMessageReporter.report(PromptSeverity.ERROR, message, mMessageReporter.getMessageContext());
		}
	}

	@Nullable public IProject getProject()
	{
		return getDesign().getProject();
	}

	public boolean isBulkShare()
	{
		return mIsBulkShare;
	}

	@Nullable public IPinList getCablePinlist()
	{
		return getModel().getCablePinlist();
	}

	public void setSharedPinListNameGenerated(boolean value)
	{
		getModel().setSharedPinListNameGenerated(value);
	}

	public String getSharedPinListName()
	{
		return getModel().getSharedPinListName();
	}

	public void setSharedPinListName(String value)
	{
		getModel().setSharedPinListName(value);
	}

	public String getSharedPinListRevision()
	{
		return getModel().getSharedPinListRevision();
	}

	public void setSharedPinListRevision(String value)
	{
		getModel().setSharedPinListRevision(value);
	}

	public void setSharedPinListMateRevision(String value)
	{
		getModel().setSharedPinListMateRevision(value);
	}

	@Nullable public String getSharedPinListMateName()
	{
		return getModel().getSharedPinListMateName();
	}

	public void setSharedPinListMateName(String value)
	{
		getModel().setSharedPinListMateName(value);
	}

	public void setSharedPinListMateNameGenerated(boolean value)
	{
		getModel().setSharedPinListMateNameGenerated(value);
	}

	public ISharedPinList getSharedPinList()
	{
		return getModel().getSharedPinList();
	}

	public void setSharedPinList(ISharedPinList spl)
	{
		getModel().setSharedPinList(spl);
	}

	public void setModularConnectorTreeValidity(boolean treeValidity)
	{
		getModel().setModularConnectorTreeValidity(treeValidity);
	}

	public void putModularConnectorToSharedNamesMap(IConnector connector, String name)
	{
		getModel().putModularConnectorToSharedNamesMap(connector, name);
	}

	public Map<IConnector, Boolean> getModularConnectorToSharedNameGeneratedMap()
	{
		return getModel().getModularConnectorToSharedNameGeneratedMap();
	}

	public Map<IConnector, String> getModularConnectorToSharedNamesMap()
	{
		return getModel().getModularConnectorToSharedNamesMap();
	}

	public void putModularConnectorToSharedNameGeneratedMap(IConnector connector, Boolean nameGenerated)
	{
		getModel().putModularConnectorToSharedNameGeneratedMap(connector, nameGenerated);
	}

	@NotNull public SortedListModel<IPinProxy> getProxies()
	{
		return getModel().getProxies();
	}

	public ReverseMap<IAbstractPin, IPinProxy> getConnectivityToSharedMap()
	{
		return getModel().getConnectivityToSharedMap();
	}

	public ReverseMap<IAbstractPin, IPinProxy> getConnectivityToSharedMap(ISymbolDef def)
	{
		return getModel().getConnectivityToSharedMap(def);
	}

	public boolean isShare()
	{
		return getModel().isShare();
	}

	public boolean allowAddPins()
	{
		return getModel().allowAddPins();
	}

	public SortedListModel<IPinProxy> getReusableProxies()
	{
		return getModel().getReusableProxies();
	}

	public ISymbolDef getSymbolDef()
	{
		return getModel().getSymbolDef();
	}

	public boolean pinlistTypeIsDevice()
	{
		return getModel().pinlistTypeIsDevice();
	}

	public void addReuseChangeListener(ChangeListener reuseChangeListener)
	{
		getModel().addReuseChangeListener(reuseChangeListener);
	}

	public void addSharedChangeListener(ChangeListener changeListener)
	{
		getModel().addSharedChangeListener(changeListener);
	}

	public void addPinChangeListener(ChangeListener listener)
	{
		getModel().addPinChangeListener(listener);
	}

	public void addNameChangeListener(ChangeListener listener)
	{
		getModel().addNameChangeListener(listener);
	}

	public void addSchemChangeListener(ChangeListener listener)
	{
		getModel().addSchemChangeListener(listener);
	}

	public void addMapChangeListener(ChangeListener listener)
	{
		getModel().addMapChangeListener(listener);
	}

	public void addRemovalListener(ChangeListener listener)
	{
		getModel().addRemovalListener(listener);
	}

	public Collection<String> getHiddenCavities()
	{
		return getModel().getHiddenCavities();
	}

	public boolean getModularConnectorTreeValidity()
	{
		return getModel().getModularConnectorTreeValidity();
	}

	public Collection<ISymbolDef> getSymbolDefsForAddition()
	{
		return getModel().getSymbolDefsForAddition();
	}

	public Collection<SharedPinListSymbolInstance> getSymbolInstancesForDeletion()
	{
		return getModel().getSymbolInstancesForDeletion();
	}

	@Nullable public String getSharedPinListMateRevision()
	{
		return getModel().getSharedPinListMateRevision();
	}

	public Map<IPinProxy, IAbstractPin> getSharedToConnectivityMap()
	{
		return getModel().getSharedToConnectivityMap();
	}

	public ISharedPin getConnectedPinToMakeReuable(ISharedPin spin)
	{
		return getModel().getConnectedPinToMakeReuable(spin);
	}

	public Collection<ISharedPin> getConnectedPinsToMakeReusableValues()
	{
		return getModel().getConnectedPinsToMakeReusableValues();
	}

	public Map<ISharedPin, ISharedPin> getConnectedPinsToMakeReusable()
	{
		return getModel().getConnectedPinsToMakeReusable();
	}

	public boolean isSharedPinListNameGenerated()
	{
		return getModel().isSharedPinListNameGenerated();
	}

	public boolean isSharedPinListMateNameGenerated()
	{
		return getModel().isSharedPinListMateNameGenerated();
	}

	public Map<ISymbolDef, ReverseMap<IAbstractPin, IPinProxy>> getSymbolDefsToPinProxyMap()
	{
		return getModel().getSymbolDefsToPinProxyMap();
	}

	public boolean preserveInternalConnectivity()
	{
		//dont preserve in case of share-into (dts0101015765 - share-into of a device with no pins.
		// Hence, previous code fails to identify this as share-into and trying to preserve internal connectivity -
		// leading to duplicate links)
		return !(isShare() && getSharedPinList() != null);
	}

	public boolean isModularConnectorWithAtLeastOneFilledPosition(@Nullable IPinList pinlist)
	{
		return ModularConnectorHelper.doesModularConnectorHasAtleastOneFilledPosition(pinlist);
	}

	public boolean isSymbolDefEditable()
	{
		return getModel().isSymbolDefEditable();
	}

	public boolean allowPinReuseManagement()
	{
		return getModel().allowPinReuseManagement();
	}
}