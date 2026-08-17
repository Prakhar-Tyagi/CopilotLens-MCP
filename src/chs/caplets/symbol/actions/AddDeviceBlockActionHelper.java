package chs.caplets.symbol.actions;

import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IInternalPinIterator;
import chs.cof.logical.schem.IPinList;
import chs.cof.symbol.IBlock;
import chs.utilities.ResourceMgr;

import javax.swing.JLabel;
import java.awt.Font;
import java.util.List;

public class AddDeviceBlockActionHelper extends AddBlockActionHelper
{

	public AddDeviceBlockActionHelper(IBlock m_block)
	{
		super(m_block);
	}

	@Override public void setPinNames()
	{
		super.setPinNames();
		IDevice blockConnectivity = (IDevice) m_compositeBlock.getConnectivity();
		IInternalPinIterator ipiniter = blockConnectivity.getInternalPins();
		while (ipiniter.hasNext()) {
			IInternalPin pin = ipiniter.getNext();
			// set the name so that it isn't ever under default naming
			pin.setName(pin.getName());
		}
	}

	@Override public void connectInternalLinks(IPinList pinList)
	{
		pinList.connectInternalLinks(false);
	}

	@Override protected void CreateAndShowDuplicatePinNamesWarningMsg(List<String> duplicatePinNames)
	{
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr
				.getString(AddBlockInstanceAction.class, "AddDeviceBlockActionHelper.DuplicatePinNames.action"));

		String title =
				ResourceMgr.getString(AddBlockInstanceAction.class, "AddBlockActionHelper.DuplicatePinNames.title");
		String heading = ResourceMgr
				.getString(AddBlockInstanceAction.class, "AddDeviceBlockActionHelper.DuplicatePinNames.body");
		String message =
				ResourceMgr.getString(AddBlockInstanceAction.class, "AddDeviceBlockActionHelper.DuplicatePinNames.text",
						m_compositeBlock.getName(), duplicatePinNames.toString());

		showWarningMessage(actionLabel,
				title,
				heading,
				message);
	}
}
