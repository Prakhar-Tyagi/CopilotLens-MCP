package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.SharedPinListAddRemoveButtons;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.shared.ISharedFunction;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.ctf.caf.utils.PortProxy;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListModel;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddRemovePinHandler extends BaseSharePinlistHandler
{

	@Nullable private Map<String, Integer> m_pinNameToCountMap;
	private EditSharedPinListModel.ProxyList m_proxyListModel;

	public AddRemovePinHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@Nullable Map<String, Integer> pinNameToCountMap,
			ListModel<IPinProxy> proxyListModel)
	{
		this(model, design, pinNameToCountMap, proxyListModel, null, false);
	}

	public AddRemovePinHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@Nullable Map<String, Integer> pinNameToCountMap,
			ListModel<IPinProxy> proxyListModel,
			@Nullable IShareMessageContextReporter reporter, boolean isBulkShare)
	{
		super(model, design, reporter, isBulkShare);
		m_pinNameToCountMap = pinNameToCountMap;
		m_proxyListModel = (EditSharedPinListModel.ProxyList) proxyListModel;
	}

	public void updatePinCountMap(@Nullable String oldName, @Nullable String newName)
	{
		if (m_pinNameToCountMap != null) {
			if (oldName != null) {
				Integer oldPinCount = m_pinNameToCountMap.get(oldName);
				assert oldPinCount != null;
				m_pinNameToCountMap.put(oldName, oldPinCount - 1);
			}

			if (newName != null) {
				Integer newPinCount = m_pinNameToCountMap.get(newName);
				if (newPinCount == null) {
					m_pinNameToCountMap.put(newName, 1);
				}
				else {
					m_pinNameToCountMap.put(newName, newPinCount + 1);
				}
			}
		}
	}

	public boolean isRingTerminal()
	{
		ISharedPinList spl = getSharedPinList();
		//on share of ringterminal objects delete option is always disabled.
		boolean isRingTerminal;
		if (spl != null) {
			isRingTerminal = PinListTypeEnum.TypeRingTerminal.equals(spl.getType());
		}
		else {
			IPinList cablePinList = getCablePinlist();
			isRingTerminal = IConnector.Statics.isRingTerminalTypeConnector(cablePinList);
		}
		return isRingTerminal;
	}

	public void removeSharedPin(@NotNull IPinProxy ppp)
	{
		m_proxyListModel.remove(ppp);
		updatePinCountMap(ppp.getName(), null);
		if (getConnectivityToSharedMap().containsValue(ppp)) {
			getConnectivityToSharedMap().removeValue(ppp);
		}
	}

	public void addPin(@NotNull String pname)
	{
		updatePinCountMap(null, pname);
		if (getSharedPinList() instanceof ISharedFunction) {
			m_proxyListModel.add(new PortProxy(pname, true));
		}
		else {
			m_proxyListModel.add(new PinProxy(pname, true));
		}
	}

	public boolean isFrozenSharedPinList()
	{
		ISharedPinList spl = getSharedPinList();
		return spl != null && spl.isFrozen();
	}

	@NotNull public Set<String> getProxyNames()
	{
		final Set<String> proxyNames = new HashSet<String>(m_proxyListModel.getSize());
		for (int i = 0; i < m_proxyListModel.getSize(); i++) {
			String name = m_proxyListModel.getElementAt(i).getName();
			proxyNames.add(name);
		}
		return proxyNames;
	}

	public void rename(@NotNull IPinProxy selected, @NotNull String newName)
	{
		updatePinCountMap(selected.getName(), newName);
		selected.setName(newName);
		m_proxyListModel.fireChangeEvent();
	}

	@Nullable public String checkValidName(@Nullable String newName, @Nullable Set<String> existingProxyNames)
	{
		if (StringUtils.getTrimmed(newName) == null) {
			return "";
		}
		//dts0100642752-Software allows duplicate names to be added to a shared pin list
		if (existingProxyNames != null && existingProxyNames.contains(newName)) {
			return ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
					getRenameButtonNameAlreadyExistsErrorText(), newName);
		}
		if (newName.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
			return ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
					getRenameButtonNameTooLongText(),
					String.valueOf(CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH));
		}
		// Other than that, always return true - any name is valid
		return null;
	}

	public boolean allowRemovePin(@NotNull List<IPinProxy> toBeRemoved, @NotNull StringBuilder message)
	{
		ISharedPinList spl = getSharedPinList();
		for (IPinProxy aToBeRemoved : toBeRemoved) {
			PinProxy ppp = (PinProxy) aToBeRemoved;
			if (!ppp.isDeletable()) {
				ISharedPin spin = ppp.getSharedPin();
				if (spin == null) {
					return false;
				}
				Set<IDesign> designs = new HashSet<IDesign>();
				SharedPinHelper.getLoadedDesignsWithUnplacedUsage(spin, designs);
				if (!designs.isEmpty() || getModel().isSharedPinUsed(spin)) {
					message.append(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
							getRemoveButtonDisabeledTooltip()));
					return false;
				}
				if(sharedPinlistHasTransientUsages(spl)){
					//LOGIC-10136
					message.append(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
							getRemoveButtonDisabeledDueToTransientUsageTooltip()));
					return false;
				}
				IPin pin = ppp.getSchemPin();
				if (pin != null && pin.getSymbolPin() != null) {
					message.append(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
							getRemoveButtonWithSymbolTooltip()));
					return false;
				}
				if (spin.isAssociatedWithSymbolPin()) {
					message.append(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
							getRemoveButtonWithSymbolTooltip()));
					return false;
				}
				//dts0100686308: We are able to remove unused pins from shared pinlist even when library part is assigned to it
				// What about rename button
				final ILibraryObject libraryObject = (ILibraryObject) spl.getLibraryObject();
				if (libraryObject != null) {
					message.append(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
							getRemoveButtonWithLibraryPartTooltip()));
					return false;
				}
			}
		}
		final boolean frozenSharedObj = isFrozenSharedPinList();
		if (frozenSharedObj) {
			message.append(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
					getRemoveButtonNotAllowedToRemoveFrozenTooltip()));
			return false;
		}
		return true;
	}

	private boolean sharedPinlistHasTransientUsages(@Nullable ISharedPinList sharedPinList)
	{
		if (sharedPinList == null) {
			return false;
		}
		IProject project = sharedPinList.getProject();
		if (project != null) {
			return !LogicUtils
					.getLogicDesignsWithTransientUsagesOfSharedObjects(project, Collections.singleton(sharedPinList))
					.isEmpty();
		}
		return false;
	}

	@NotNull protected String getRemoveButtonNotAllowedToRemoveFrozenTooltip()
	{
		return "SharedPinListAddRemoveButtons.removeButton.notAllowedToRemoveFrozen.tooltip";
	}

	@NotNull protected String getRemoveButtonWithLibraryPartTooltip()
	{
		return "SharedPinListAddRemoveButtons.removeButton.withLibraryPart.tooltip";
	}

	@NotNull protected String getRemoveButtonWithSymbolTooltip()
	{
		return "SharedPinListAddRemoveButtons.removeButton.withSymbol.tooltip";
	}

	@NotNull protected String getRemoveButtonDisabeledTooltip()
	{
		return "SharedPinListAddRemoveButtons.removeButton.disabled.tooltip";
	}

	@NotNull protected String getRemoveButtonDisabeledDueToTransientUsageTooltip()
	{
		return "SharedPinListAddRemoveButtons.removeButton.disabled.mayBeUnplaced.tooltip";
	}

	@NotNull protected String getRenameButtonNameTooLongText()
	{
		return "SharedPinListAddRemoveButtons.renameButton.tooLong";
	}

	@NotNull protected String getRenameButtonNameAlreadyExistsErrorText()
	{
		return "SharedPinListAddRemoveButtons.renameButton.NameExistsError";
	}
}
