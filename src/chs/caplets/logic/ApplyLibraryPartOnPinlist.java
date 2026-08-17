package chs.caplets.logic;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.IPinMappings;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.logic.LogicUtils;

import java.util.ArrayList;
import java.util.Collection;


public class ApplyLibraryPartOnPinlist
{

	public boolean applyPinMapping(IPinList conn, IPinMappings pinMapping,
			final Collection<IAbstractPin> unMappedPins)
	{
		ISharedPinList spl = null;
		if (conn != null) {
			spl = conn.getSharedPinList();
		}
		if (spl != null) {
			// pin mapping for shared pinlists is completely different
			// this is currently handled elsewhere, regardless of what the pin mapping returns
			return true;
		}

		return doApplyPinMapping(conn, pinMapping, unMappedPins);
	}

	protected boolean doApplyPinMapping(IPinList pinList, IPinMappings pinMapping,
			final Collection<IAbstractPin> unMappedPins)
	{
		ISharedPinList spl = pinList.getSharedPinList();
		if (spl != null) {
			// pin mapping for shared pinlists is completely different
			// this is currently handled elsewhere, regardless of what the pin mapping returns
			return false;
		}

		for (IAbstractPin cpin : pinList.getPins()) {
			String pinName = cpin.getName();
			String newPinName = pinMapping != null ? pinMapping.getName(pinName) : null;
			if (newPinName == null) {
				unMappedPins.add(cpin);
			}
			else if (!newPinName.equals(pinName)) {
				cpin.setName(newPinName);
			}
			else {
				// Presumably we do this to cause some side effect to pins that were not renamed?
				cpin.setName("");
				cpin.setName(newPinName);
			}
			LogicUtils.setMatchingShortDescriptionFromOTI(cpin, cpin.getProject());
		}
		return true;
	}

	public void deletePins(Collection<IAbstractPin> pins, ISchemDiagram diagram)
	{
		// use DeleteHelper to disconnect + delete the connectivity pins.
		// we must also gather up all the schems from the design at this point because the DeleteHelper no longer does this for us
		Collection<IUIDObject> toDelete = new ArrayList<IUIDObject>();
		toDelete.addAll(pins);
		for (IAbstractPin pin : pins) {
			ILogicDesign design = pin.getLogicDesign();
			assert design != null;
			for (IDiagramObject rep : design.getDesignWideUsageMgr().getRepresentations(pin)) {
				toDelete.add(rep);
			}
		}
		// LOGIC-11652 Performance issue (2600s) Batch Update of Library parts->Ctrl A on the diagram and click on Batch Update->Library Parts in the Automate Tab in ribbon on ASML 9x data
		if (!toDelete.isEmpty()) {
			DeleteHelper.getInstance().delete(diagram, toDelete, true); // todo : What if toDelete is empty?
			CreationDeletionHelper.getTheCreationHelper().processObjects(); // need this to remove the cable pins now (?!)
		}
	}
}