package chs.caplets.logic.actions.debug;

import chs.caf.caplet.ICapletController;
import chs.cof.icd.IICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDBackshellSignalAssociation;
import chs.cof.icd.IICDPinSignalAssociation;
import chs.cof.icd.IICDUsageDefinition;
import chs.cof.icd.ISystemICDMgr;
import chs.common.criteria.ICriteria;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author chandras on 06-02-2017.
 */
public class DumpICDSignalsAction extends AbstractDumpICDAction
{

	private static final String PANENAME = "ICDSignals";

	public DumpICDSignalsAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return DumpICDSignalsActionUI.class.getName();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		List<String> dumpData = new ArrayList<>();
		dumpICDSignals(dumpData);
		dumpData(PANENAME, dumpData);
		return true;
	}

	private void dumpICDSignals(@NotNull List<String> dumpedData)
	{
		ISystemICDMgr icdMgr = FactoryMgr.getCHSSystem().getSystemData().getICDMgr();
		ICriteria<IICD> criteria = UtilsHelper.getCHSUtils().getCriteriaFactory().createCriteria(IICD.class);
		SetMap<String, IICDAssociatedSignal> signalData = new SetMap<>(TreeMap.class);
		for (IICD icd : icdMgr.getMatchingICDs(criteria)) {
			IICDUsageDefinition definition = icd.getICDUsageDefinition();
			buildSignalData(definition, signalData);
		}
		dumpedData.add("SINGAL(s)------------->");
		for (Map.Entry<String, Set<IICDAssociatedSignal>> entry1 : signalData.entrySet()) {
			dumpedData.add(getTabString(1) + entry1.getKey());
			SetMap<String, IICDAssociatedSignal> signals = new SetMap<>(TreeMap.class);
			for (IICDAssociatedSignal associatedSignal : entry1.getValue()) {
				signals.add(associatedSignal.getNetName() + (associatedSignal.isShieldWire() ? "(shield)" : ""),
						associatedSignal);
			}

			for (Map.Entry<String, Set<IICDAssociatedSignal>> entry2 : signals.entrySet()) {
				dumpedData.add(getTabString(2) + entry2.getKey());
				Set<String> pins = new TreeSet<>();
				for (IICDAssociatedSignal associatedSignal : entry2.getValue()) {
					IICDPinSignalAssociation pinAssociation =
							CommonUtils.cast(associatedSignal.getSignalContainer(), IICDPinSignalAssociation.class);
					if (pinAssociation != null) {
						pins.add(toString(pinAssociation));
					}
					else {
						IICDBackshellSignalAssociation termAssociation = CommonUtils
								.cast(associatedSignal.getSignalContainer(), IICDBackshellSignalAssociation.class);
						if (termAssociation != null) {
							pins.add(toString(termAssociation));
						}
						else {
							assert false;
						}
					}
				}
				for (String pin : pins) {
					dumpedData.add(getTabString(3) + pin);
				}
			}
		}
	}

	private void buildSignalData(@Nullable IICDUsageDefinition definition,
			@NotNull SetMap<String, IICDAssociatedSignal> signalData)
	{
		if (definition == null) {
			return;
		}

		for (IICDBackshellSignalAssociation association : definition.getBackshellSignalAssociations()) {
			for (IICDAssociatedSignal signal : association.getICDAssociatedSignals()) {
				signalData.add(toSignalPath(signal), signal);
			}
		}
		for (IICDPinSignalAssociation association : definition.getPinSignalAssociations()) {
			for (IICDAssociatedSignal signal : association.getICDAssociatedSignals()) {
				signalData.add(toSignalPath(signal), signal);
			}
		}
	}

	private String toSignalPath(@NotNull IICDAssociatedSignal signal)
	{
		String signalGroupPath = StringUtils.nonNull(signal.getSignalGroupPath());
		return StringUtils.isBlank(signalGroupPath) ? "<No SignalGroupPath>" : signalGroupPath;
	}

	private String toString(@NotNull IICDBackshellSignalAssociation association)
	{
		return association.getICDUsageDefinition().getICD().getName() + ":" +
				association.getTermination().getBackshell().getName() + ":" + association.getTermination().getName();
	}

	private String toString(@NotNull IICDPinSignalAssociation association)
	{
		return association.getICDUsageDefinition().getICD().getName() + ":" + association.getPinName();
	}
}
