package chs.caplets.logic.actions;

import chs.cof.logical.shared.PinListTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.PropertyPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: brangan Date: 12/27/13 Time: 4:10 PM To change this template use File | Settings |
 * File Templates.
 */
public class PlaceBlockPinDialog extends BasePlacePinsDialog
{

	private BlockPinSelectionPanel m_Panel = null;
	private IAddBlockPinActionModel m_model;
	private boolean m_placingBlock = false;
	protected boolean m_showUsedPins;
	protected IBooleanProperty m_showUsedPinsOption;

	public PlaceBlockPinDialog(@Nullable Frame frame, IAddBlockPinActionModel model, boolean placingBlock, @NotNull IPlacementOptionParams params)
	{
		super(frame, false);
		m_model = model;
		m_placingBlock = placingBlock;
		if (m_placingBlock) {
			setTitle(ResourceMgr.getString(PlacePinsDialog.class, "PlaceBlockPinDialog.placeBlock.text"));
		}
		initialize(params);
		rememberSize(true);
		validate();
	}

	@Override protected void addComponents(@NotNull IPlacementOptionParams params)
	{
		super.addComponents(params);
		buildOptionsPanel(PinListTypeEnum.TypeDevice.getNonI18NName(), params);
	}

	@Override protected void doBuildOptionsPanel(@NotNull IPlacementOptionParams params)
	{
		m_Panel.add(m_optionsPanel, BorderLayout.SOUTH);
		initOptionsPanel(params);
		PropertyPanel rootPanel = createPropertyPanel();
		m_optionsPanel.add(rootPanel, BorderLayout.CENTER);
		loadPrefs();
		loadOptionPref(m_showUsedPinsOption);
	}

	@Override protected void initOptionsPanel(@NotNull IPlacementOptionParams params)
	{
		super.initOptionsPanel(params);
		if (params.isShowUsedPinsOptionEnabled()) {
			createShowUsedPinsCheckBox();
		}
	}

	private void createShowUsedPinsCheckBox()
	{
		String name = ResourceMgr.getString(BlockPinSelectionPanel.class,
				"BlockPinSelectionPanel.ShowUsedPins.CheckBox.Text");
		m_showUsedPinsOption = buildOption(name, name, false);
		m_showUsedPinsOption.setValue(m_model.isProcessUsedPins());
		m_showUsedPinsOption.
				addPropertyChangeListener(
						new PropertyChangeListener()
						{
							@Override public void propertyChange(PropertyChangeEvent evt)
							{
								m_showUsedPins = (boolean) evt.getNewValue();
								if (m_showUsedPins) {
									m_model.setProcessUsedPins(true);
									m_Panel.initTableData();
								}
								else {
									m_showUsedPins = false;
									m_model.setProcessUsedPins(false);
									m_Panel.initTableData();
								}
							}
						}
				);
//			if (m_placingBlock) {
//				m_optionsPanel.add(checkbox, BorderLayout.PAGE_END);
//			}
//			else {
//				pinlistPanel.add(m_isProcessUsedPinsChkBox, BorderLayout.CENTER);
//			}
	}

	@Override protected void createAsStackOption()
	{
		m_placeAsStackOption = buildOption(AS_STACK_OPTION, PLACEASSTACK_TOOLTIP, true);
		m_placeAsStackOption.setName("PlaceBlockPin_PlaceAsStackOption");
		m_placeAsStackOption.setMnemonic(ResourceMgr.getMnemonic(BlockPinSelectionPanel.class,
				"BlockPinSelectionPanel.placeAsStackCheckBox.mnemonic"));
		m_placeAsStackOption.setEnabled(true);
		m_placeAsStackOption.addPropertyChangeListener(evt -> m_placeAsStack = (boolean) evt.getNewValue());
	}

	@Override protected void savePrefs()
	{
		super.savePrefs();
		saveOptionPrefs(m_showUsedPinsOption, m_showUsedPins);
	}

	@Override protected boolean canAutogeneratePins()
	{
		return true;
	}

	public void validate()
	{
		super.validate();
		updatePlaceButton();
	}

	protected JPanel getPinTablePanel()
	{
		if (m_Panel == null) {
			m_Panel = new BlockPinSelectionPanel(this, m_model, m_placingBlock);
		}
		return m_Panel;
	}

	public List<IPinProxy> getPins()
	{
		return m_Panel.getPins();
	}

	public boolean isReference()
	{
		return false;
	}

	public boolean getAutogenerate()
	{
		return m_autoGenerate;
	}

	public boolean isPlaceAsStack()
	{
		return m_placeAsStack;
	}

	private void updatePlaceButton()
	{
		placeButton.setEnabled(m_placingBlock || m_Panel.getTable().getSelectedRowCount() > 0);
		if (!m_model.isValid(m_Panel.getPins())) {
			placeButton.setEnabled(false);
			placeButton.setToolTipText(m_model.getInvalidityReason());
		}
		else {
			placeButton.setToolTipText(null);
		}
	}

	@Override @NotNull protected String getAutoGenerationOptionComponentName()
	{
		return "PlaceBlockPin_AutoGenerateOption";
	}
}
