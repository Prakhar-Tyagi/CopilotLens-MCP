package chs.caplets.logic.actions;

import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.ui.IConductorConnectionChangeSavePredicate;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.WrappingRuntimeException;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConductorConnectionChanger
{

	private Map<IDesignDescriptor, Map<IPinProxy, IPinProxy>> designToPinsToSwap = new HashMap<>();
	private ISharedPinList sharedPinList = null;
	private IConductorConnectionChangeSavePredicate designSavePredicate;

	private IPinList pinlist = null;

	public ConductorConnectionChanger(IPinList pinOwner, IConductorConnectionChangeSavePredicate designSavePredicate)
	{
		this.designSavePredicate = designSavePredicate;
		pinlist = pinOwner;
		if (pinlist.getSharedPinList() != null) {
			sharedPinList = pinlist.getSharedPinList();
		}
	}

	public ConductorConnectionChanger(ISharedPinList sharedPinList,
			IConductorConnectionChangeSavePredicate designSavePredicate)
	{
		this.designSavePredicate = designSavePredicate;
		this.sharedPinList = sharedPinList;
		this.designSavePredicate = designSavePredicate;
	}

	public void addConnection(IDesignDescriptor logicDesign, IPinProxy sourcePin, IPinProxy targetPin)
	{
		Map<IPinProxy, IPinProxy> pinsMap = designToPinsToSwap.get(logicDesign);
		if (pinsMap == null) {
			pinsMap = new HashMap<>();
			designToPinsToSwap.put(logicDesign, pinsMap);
		}
		pinsMap.put(sourcePin, targetPin);
	}

	private boolean shouldSaveForeignDesign()
	{
		return designSavePredicate.shouldSaveForeignDesigns();
	}

	public boolean changeConnections()
	{
		if (sharedPinList != null && editingForeignDesign()) {
			return changeConnectionsOfAllDesigns();
		}
		else {
			changeConnectionsOfSingleDesign();
			return true;
		}
	}

	private void changeConnectionsOfSingleDesign()
	{
		try {
			if (!designToPinsToSwap.isEmpty()) {
				IDesignDescriptor currentDesign = designToPinsToSwap.keySet().iterator().next();
				ILogicDesign designContainer = (ILogicDesign) currentDesign.getDesignContainer();
				if (designContainer != null) {
					ConductorConnectionChangerForDesign designChanger;
					if (pinlist != null) {
						designChanger =
								new ConductorConnectionChangerForDesign(designContainer, pinlist,
										designToPinsToSwap.get(currentDesign));
					}
					else {
						designChanger = createConductorConnectionForDesign(designContainer,
								designToPinsToSwap.get(currentDesign));
					}
					designChanger.changeConnectionsOfDesign(false);
				}
			}
		}
		catch (UserSessionException e) {
			throw new WrappingRuntimeException(e);
		}
	}

	@NotNull
	protected ConductorConnectionChangerForDesign createConductorConnectionForDesign(ILogicDesign designContainer,
			Map<IPinProxy, IPinProxy> pinsToSwap)
	{
		return new ConductorConnectionChangerForDesign(designContainer, sharedPinList, pinsToSwap);
	}

	private boolean changeConnectionsOfAllDesigns()
	{
		boolean success = true;
		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		try {
			btm.enterTransactionBoundary(this, IBoundaryTransactionMarshaller.Nesting.NESTED);

			processDesigns();
		}
		catch (UserSessionException e) {
			success = false;
			throw new WrappingRuntimeException(e);
		}

		finally {
			btm.exitTransactionBoundary(this, success);
		}
		return true;
	}

	private void processDesigns() throws UserSessionException
	{
		boolean saveDesigns = editingForeignDesign() && shouldSaveForeignDesign();
		List<ILogicDesign> processedDesigns = new ArrayList<>();
		for (IDesignDescriptor designDescriptor : designToPinsToSwap.keySet()) {
			ILogicDesign logicDesign = (ILogicDesign) designDescriptor.getDesignContainer();
			if (logicDesign != null) {
				ConductorConnectionChangerForDesign designChanger =
						createConductorConnectionForDesign(logicDesign, designToPinsToSwap.get(logicDesign));
				designChanger.changeConnectionsOfDesign(saveDesigns);
				processedDesigns.add(logicDesign);
			}
		}
		if (saveDesigns && !processedDesigns.isEmpty()) {
			Collection<ILogicDesign> effectedOpenedDesigns = designSavePredicate.getOpenedDesignsToBeSaved();
			effectedOpenedDesigns.removeAll(processedDesigns);
			saveDesigns(effectedOpenedDesigns);
			designSavePredicate.doPostSave();
		}
	}

	protected void saveDesigns(Collection<ILogicDesign> designs) throws UserSessionException
	{
		CAFCommandHelper cmdHelper = new CAFCommandHelper();
		for (ILogicDesign design : designs) {
			cmdHelper.saveDesign(design);
			cmdHelper.clearDesignUndoableContainer(design);
			cmdHelper.setDesignModifiedFlag(design, false);
		}
	}

	private boolean editingForeignDesign()
	{
		return designToPinsToSwap.keySet().stream()
				.anyMatch(thisDesign -> !designSavePredicate.isCurrentDesign(thisDesign));
	}
}
