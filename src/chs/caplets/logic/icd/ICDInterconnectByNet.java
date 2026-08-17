package chs.caplets.logic.icd;

import chs.caplets.logic.actions.AddPinAction;
import chs.caplets.logic.actions.shared.UnshareConductorActionHelper;
import chs.cof.draw.IColor;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDPinSignalAssociation;
import chs.cof.icd.IICDSignalsContainer;
import chs.cof.logical.GeneralReportValidationHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.footprint.IReportValidationHandler;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedObject;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.permission.PermissionEnum;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.ICDUtils;
import chs.utility.helpers.LogTabType;
import chs.utility.security.PermissionHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;


public class ICDInterconnectByNet extends ICDSingleEndedConnectStrategy
{

	public ICDInterconnectByNet(@NotNull PersistenceHandler persistenceHandler)
	{
		super(persistenceHandler);
	}

	@NotNull @Override protected String getConductorType()
	{
		return ISharedConductor.NET_TYPE;
	}

	@Override protected Class<? extends IConductor> getCableConductorType()
	{
		return INetConductor.class;
	}

	@Override
	protected IConductor constructNewCableConductor(ISchemDiagram diagram, IICDAssociatedSignal associatedSignal)
	{
		INetConductor net = FactoryMgr.getCablePropertiedFactory().createNetConductor(FactoryMgr.createUID());
		registerToDesign(diagram, net);
		ICDUtils.setSourceICDSignal(net, associatedSignal.getNetName());
		net.setName(associatedSignal.getNetName());
		return net;
	}

	@Nullable protected IConductor getCableConductorToJoinWithPin(ISchemDiagram diagram, IICDAssociatedSignal associatedSignal)
	{
		ILogicDesign design = diagram.getDesign();
		assert design != null;
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		INetConductor netConductorByName = connectivity.findNetConductorByName(associatedSignal.getNetName());
		if(netConductorByName == null) {
			return constructNewCableConductor(diagram, associatedSignal);
		}
		if(netConductorByName.isShared() && !PermissionHelper.hasPermission(PermissionEnum.SHARED_OBJECTS)) {
			reportSharedPermissionRestrictedMessage(netConductorByName.getSharedConductor(), associatedSignal);
			return null;
		}
		ShieldBuilder.syncConductorWithSignal(netConductorByName, associatedSignal);
		return netConductorByName;
	}

	private static void reportSharedPermissionRestrictedMessage(ISharedObject sharedConductor, IICDAssociatedSignal associatedSignal)
	{
		IReportValidationHandler handler = GeneralReportValidationHandler.getHandle(LogTabType.TAB_ICD);
		IICDSignalsContainer icdSignalsContainer = associatedSignal.getSignalContainer();
		String pinName = icdSignalsContainer instanceof IICDPinSignalAssociation pinSignalAssociation ? pinSignalAssociation.getPinName() : "";
		String message = ResourceMgr.getString(AddPinAction.class,
				"AddPinAction.output.isShared", HTMLHelper.link(sharedConductor), pinName);
		handler.report(PromptSeverity.ERROR, HTMLHelper.color(IColor.RED, message));
	}

	@NotNull @Override
	protected ListSet<IMulticore> getMulticoresNotTerminatingAtMoreThanTwoPls(IICDAssociatedSignal signal,
			ListSet<IMulticore> multicores,
			Set<IPinList> connectedCablePlsToCond)
	{
		return multicores;
	}

	@Override public boolean isWiringAbstraction()
	{
		return false;
	}

	/**
	 * This is used to update the connectivity on an existing schematic conductor to use the ICD signal name.
	 * If it is shared instance then it is unshared and the new cable is updated and otherwise the new cable is created to bear the name of the ICD signal.
	 *
	 * @param conductor schematic conductor whose connectivity should represent ICD signal
	 * @param signal    ICD signal to be represented by the schematic conductor
	 * @param diagram   diagram in which ICD connections are being processed
	 */
	@Override
	protected void updateToNewConnectivity(chs.cof.logical.schem.IConductor conductor, IICDAssociatedSignal signal,
			ISchemDiagram diagram)
	{
		if (!conductor.getConnectivity().getName().equals(signal.getNetName())) {
			IConductor oldCableConductor = conductor.getConnectivity();
			ILogicDesign design = oldCableConductor.getLogicDesign();
			assert design != null;
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			UnshareConductorActionHelper.createNewConductorAndResetConnections(oldCableConductor, design, dwum,
					signal.getNetName(), conductor);
			INetConductor newCableConductor = CommonUtils.cast(conductor.getConnectivity(), INetConductor.class);
			if (newCableConductor != null) {
				mPersistenceHandler.collectReplacedConductors(oldCableConductor);
				mPersistenceHandler.collectEmptyMulticores(Collections.singleton(oldCableConductor));
				ICDUtils.setSourceICDSignal(newCableConductor, signal.getNetName());
				newCableConductor.setName(signal.getNetName());
			}
		}
		else {
			ICDUtils.setSourceICDSignal(conductor.getConnectivity(), signal.getNetName());
		}
		addNewConductorToContext(signal, conductor);
	}
}
