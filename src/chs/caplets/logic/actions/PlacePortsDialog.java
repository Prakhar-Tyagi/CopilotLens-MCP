package chs.caplets.logic.actions;

import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utility.ui.PinSelectionAbstractPanel;
import chs.utility.ui.PinSelectionCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Frame;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PlacePortsDialog extends BasePlacePinsDialog
{

	private ConnectivityCommonPinSelectionPanel psp;
	private IPinList m_pinList;

	public PlacePortsDialog(@Nullable Frame frame, IPinList pinlist, boolean allowCreation, @NotNull IPlacementOptionParams params)
	{
		super(frame, getDialogTitle(), allowCreation);
		m_pinList = pinlist;
		initialize(params);
		buildTypeSpecificOptions(PinListTypeEnum.TypeAny, params);
	}

	protected static String getDialogTitle()
	{
		return ResourceMgr.getString(PlacePortsDialog.class, "PlacePortsDialog.title");
	}

	/**
	 * Get the list of pins that were selected in the dialog
	 *
	 * @return A possibly empty list of pins, sorted as they appear selected in the dialog
	 */
	public List<IPinProxy> getPins()
	{
		return psp.getPins();
	}

	public boolean isReference()
	{
		return false;
	}

	public boolean isPlaceAsStack()
	{
		return false;
	}

	protected void addItemListenerForReferencePin(JCheckBox referenceOption)
	{
		referenceOption.addItemListener(e -> psp.reset());
	}

	protected JPanel getPinTablePanel()
	{
		if (psp == null) {
			psp = new ConnectivityCommonPinSelectionPanel(m_pinList, null, this, new Consumer<List<?>>()
			{
				@Override public void accept(List<?> objects)
				{
					placeButton.setEnabled(objects != null && !objects.isEmpty());
				}
			},
					getEscapeListener(),
					Collections.singleton(PinSelectionCapabilities.SymbolTreeCapability));
		}
		return psp;
	}

	@Override @Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
	{
		return psp;
	}
}
