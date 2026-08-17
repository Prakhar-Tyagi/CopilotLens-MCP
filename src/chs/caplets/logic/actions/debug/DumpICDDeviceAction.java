package chs.caplets.logic.actions.debug;

import chs.caf.caplet.ICapletController;
import chs.cof.icd.IICD;
import chs.cof.icd.IICDBackshell;
import chs.cof.icd.IICDBackshellTermination;
import chs.cof.icd.IICDConnector;
import chs.cof.icd.IICDConnectorCavity;
import chs.cof.icd.IICDDeviceConnector;
import chs.cof.icd.IICDDeviceFootprint;
import chs.cof.icd.IICDHarnessConnector;
import chs.cof.icd.IICDHarnessFootprint;
import chs.cof.icd.IICDPinSignalAssociation;
import chs.cof.icd.IICDPinToCavityMapping;
import chs.cof.icd.IICDUsageDefinition;
import chs.cof.icd.ISystemICDMgr;
import chs.common.criteria.ICriteria;
import chs.system.FactoryMgr;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author chandras on 06-02-2017.
 */
public class DumpICDDeviceAction extends AbstractDumpICDAction
{

	private static final String PANENAME = "ICDDevices";

	public DumpICDDeviceAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return DumpICDDeviceActionUI.class.getName();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		List<String> dumpData = new ArrayList<>();
		dumpICDDevices(dumpData);
		dumpData(PANENAME, dumpData);
		return true;
	}

	private void dumpICDDevices(@NotNull List<String> dumpedData)
	{
		ISystemICDMgr icdMgr = FactoryMgr.getCHSSystem().getSystemData().getICDMgr();
		ICriteria<IICD> criteria = UtilsHelper.getCHSUtils().getCriteriaFactory().createCriteria(IICD.class);
		dumpedData.add("ICD(s)------------->");
		for (IICD icd : icdMgr.getMatchingICDs(criteria)) {
			dumpedData.add(getTabString(1) + icd.getRole() + " (NAME-> " + icd.getName() + ", REV-> " +
					icd.getRevision() + ", CUST-PART-> " + icd.getCustomerPartNumber() + ", SUPL-PART-> " +
					icd.getSupplierPartNumber() + ", FP-> " + icd.getFootprintName() + ")");
			IICDUsageDefinition definition = icd.getICDUsageDefinition();

            if (definition == null) {
                continue;
            }
            
			StringBuilder pinNames = new StringBuilder();
			Collection<IICDPinSignalAssociation> associations = definition.getPinSignalAssociations();
			for (IICDPinSignalAssociation association : associations) {
				if (pinNames.length() != 0) {
					pinNames.append(", ");
				}
				pinNames.append(association.getPinName());
			}
			dumpedData.add(getTabString(2) + "Pins(" + associations.size() + ")--> " + pinNames);

			dumpedData.add(getTabString(2) + "DeviceConnectors ->");
			for (IICDDeviceConnector deviceConnector : definition.getDeviceConnectors()) {
				dumpConnector(dumpedData, deviceConnector);
			}

			dumpedData.add(getTabString(2) + "HarnessConnectors ->");
			for (IICDHarnessConnector harnessConnector : definition.getHarnessConnectors()) {
				dumpConnector(dumpedData, harnessConnector);
			}

			IICDDeviceFootprint deviceFootprint = definition.getDeviceFootprint();
			if (deviceFootprint != null) {
				Set<IICDPinToCavityMapping> icdPinToCavityMappings = deviceFootprint.getICDPinToCavityMappings();
				dumpedData.add(getTabString(2) + "DeviceFootprint(" + icdPinToCavityMappings.size() + ") ->");
				for (IICDPinToCavityMapping mapping : icdPinToCavityMappings) {
					dumpFPMapping(dumpedData, mapping);
				}
			}
			else {
				dumpedData.add(getTabString(2) + "DeviceFootprint -> (null)");
			}

			IICDHarnessFootprint harnessFootprint = definition.getHarnessFootprint();
			if (harnessFootprint != null) {
				Set<IICDPinToCavityMapping> icdPinToCavityMappings = harnessFootprint.getICDPinToCavityMappings();
				dumpedData.add(getTabString(2) + "HarnessFootprint(" + icdPinToCavityMappings.size() + ") ->");
				for (IICDPinToCavityMapping mapping : icdPinToCavityMappings) {
					dumpFPMapping(dumpedData, mapping);
				}
			}
			else {
				dumpedData.add(getTabString(2) + "HarnessFootprint -> (null)");
			}
		}
	}

	private void dumpFPMapping(List<String> dumpedData, IICDPinToCavityMapping mapping)
	{
		StringBuilder builder = new StringBuilder();
		IICDConnector connector = mapping.getConnector();
		IICDConnectorCavity cavity = mapping.getCavity();
		builder.append(getTabString(3))
				.append(mapping.getPinName())
				.append(", ")
				.append(connector != null ? connector.getName() : "<null>")
				.append(", ")
				.append(cavity != null ? cavity.getName() : "<null>");
		dumpedData.add(builder.toString());
	}

	private void dumpConnector(@NotNull List<String> dumpedData, IICDConnector deviceConnector)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(getTabString(3))
				.append(deviceConnector.getName())
				.append("(")
				.append("PartNo-> ")
				.append(deviceConnector.getPartNumber())
				.append(", PartRev-> ")
				.append(deviceConnector.getPartRevision())
				.append(")");
		dumpedData.add(builder.toString());

		StringBuilder pinNames = new StringBuilder();
		Set<IICDConnectorCavity> cavities = deviceConnector.getICDConnectorCavities();
		for (IICDConnectorCavity cavity : cavities) {
			if (pinNames.length() != 0) {
				pinNames.append(", ");
			}
			pinNames.append(cavity.getName());
		}
		dumpedData.add(getTabString(4) + "Cavities(" + cavities.size() + ")--> " + pinNames);

		IICDBackshell backshell = deviceConnector.getBackshell();
		if (backshell != null) {
			builder = new StringBuilder();
			builder.append(getTabString(4))
					.append("Backshell -> ")
					.append(backshell.getName())
					.append("(")
					.append("PartNo-> ")
					.append(backshell.getPartNumber())
					.append(", PartRev-> ")
					.append(backshell.getPartRevision())
					.append(")");
			dumpedData.add(builder.toString());

			StringBuilder termNames = new StringBuilder();
			Collection<IICDBackshellTermination> terminations = backshell.getTerminations();
			for (IICDBackshellTermination termination : terminations) {
				if (termNames.length() != 0) {
					termNames.append(", ");
				}
				termNames.append(termination.getName());
			}
			dumpedData.add(getTabString(5) + "Terminations(" + terminations.size() + ")--> " + termNames);
		}
	}
}
