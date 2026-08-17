package chs.caplets.logic.actions;

import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionsDialog;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.BasicUIFactory;
import chs.utility.ui.PinSelectionAbstractPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by IntelliJ IDEA. User: bkadukun Date: Feb 28, 2011 Time: 6:57:22 PM To change this template use File |
 * Settings | File Templates.
 */
public class EditStackPinDialog extends PinListPlaceOptionsDialog
{

	/**
	 * The result of invoking the dialog.
	 */
	public enum Result
	{

		DELETE,
		UNSTACK,
		CANCEL
	}

	private Result result = Result.CANCEL;
	private ConnectivityCommonPinSelectionPanel psp;

	@Nullable private JButton deleteButton = null;

	@Nullable private JButton unStackButton;

	private static final int MinimumPanelWidth = 550;
	private static final int MinimumPanelHeight = 300;
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);

	public EditStackPinDialog(Frame frame, ISchemStackPin stackPin, @NotNull IPlacementOptionParams params)
	{
		super(frame, getTheTitle(), true);
		setName("EditStackPinDialog");
		rememberSize(true);
		addComponents(stackPin, params);
		hookupComponents();

		setMinimumSize(MinimumPanelSize);
	}

	public Result selectPins()
	{
		pack();
		setVisible(true);
		return result;
	}

	public List<IPinProxy> getPins()
	{

		return psp.getPins();
	}

	@Override public void setName(String name)
	{
		super.setName(name);
	}

	private static String getTheTitle()
	{
		return ResourceMgr.getString(EditStackPinDialog.class, "EditStackPinDialog.title");
	}

	private void addComponents(ISchemStackPin stackPin, @NotNull IPlacementOptionParams params)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JPanel(), BorderLayout.EAST);
		panel.add(new JPanel(), BorderLayout.WEST);
		panel.add(new JPanel(), BorderLayout.NORTH);
		getContentPane().add(panel, BorderLayout.CENTER);
		m_optionsPanel = new JPanel();
		m_optionsPanel.setLayout(new BorderLayout());
		m_optionsPanel.add(new JPanel(), BorderLayout.WEST);
		m_optionsPanel.add(new JPanel(), BorderLayout.EAST);
		panel.add(m_optionsPanel, BorderLayout.SOUTH);
		initOptionsPropertyGroup();

		// Violate Mr Liscov with this hideous hack of swapping the OK/Cancel buttons for Place/Create
		deleteButton = BasicUIFactory.getInstance().createSiemensCustomJButton(
				ResourceMgr.getString(EditStackPinDialog.class, "EditStackPinDialog.Button.Delete"), true);
		deleteButton.setName("deleteButton");
		setOkButton(deleteButton);

		unStackButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(
						ResourceMgr.getString(EditStackPinDialog.class, "EditStackPinDialog.Button.UnStack"));
		unStackButton.setName("removeButton");

		setCancelButton(unStackButton);

		getRootPane().setDefaultButton(unStackButton);
		psp = new ConnectivityCommonPinSelectionPanel(stackPin, this,
				new Consumer<List<?>>()
				{
					@Override public void accept(List<?> objects)
					{
						updateEnablementOfButtons(objects);
					}
				}
				, getEscapeListener());
		panel.add(psp, BorderLayout.CENTER);
		getContentPane().add(panel, BorderLayout.CENTER);
		buildTypeSpecificOptions(PinListTypeEnum.TypeAny, params);
	}

	public boolean getPlaceAsGroup()
	{
		return m_placeAsGroup;
	}

	private void hookupComponents()
	{
		if (deleteButton != null) {
			// IJ is losing the plot
			//noinspection ConstantConditions
			deleteButton.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							result = Result.DELETE;
							setVisible(false);
							savePrefs();
							dispose();
						}
					}
			);
		}

		if (unStackButton != null) {
			// IJ is losing the plot
			//noinspection ConstantConditions
			unStackButton.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							result = Result.UNSTACK;
							setVisible(false);
							savePrefs();
							dispose();
						}
					}
			);
		}

		Component[] focusOrder = new Component[]{unStackButton, psp, getHelpButton(), unStackButton};

		setFocusTraversal(focusOrder);
	}

	private void updateEnablementOfButtons(List<?> selectedPins)
	{
		unStackButton.setEnabled(!selectedPins.isEmpty());
		deleteButton.setEnabled(!selectedPins.isEmpty());
	}

	public void windowClosing(WindowEvent e)
	{
		result = Result.CANCEL;

		storeGeometry();
		savePrefs();
		setVisible(false);
		dispose();
	}

	public void keyPressed(KeyEvent e)
	{
		result = Result.CANCEL;
	}

	@Override @NotNull protected JRootPane createRootPane()
	{
		return new JRootPane();
	}

	@Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
	{
		return psp;
	}
}
