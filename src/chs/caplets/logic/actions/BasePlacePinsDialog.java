package chs.caplets.logic.actions;

import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionsDialog;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.BasicUIFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

/**
 * Created with IntelliJ IDEA. User: brangan Date: 12/27/13 Time: 2:49 PM To change this template use File | Settings |
 * File Templates.
 */
public abstract class BasePlacePinsDialog extends PinListPlaceOptionsDialog implements IAddPinView
{

	protected Result result = Result.CANCEL;
	protected boolean allowCreation;

	protected JButton placeButton;
	@Nullable protected JButton createButton;

	private static final int MINIMUM_PANEL_WIDTH = 250;
	private static final int MINIMUM_PANEL_HIGHT = 300;

	protected BasePlacePinsDialog(@Nullable Frame frame, boolean allowCreation)
	{
		this(frame, getTheTitle(), allowCreation);
	}

	protected BasePlacePinsDialog(@Nullable Frame frame, String title, boolean allowCreation)
	{
		super(frame, title, true);
		this.allowCreation = allowCreation;
	}

	protected void initialize(@NotNull IPlacementOptionParams params)
	{
		setName("PlacePinsDialog");
		rememberSize(true);
		setMinimumSize(new Dimension(MINIMUM_PANEL_WIDTH, MINIMUM_PANEL_HIGHT));
		addComponents(params);
		hookupComponents();
	}

	/**
	 * Show the dialag and let the user select the pins or cancel.  Use getPins() to get the selected pins after calling
	 * this method.
	 *
	 * @return true if OK was pressed, false otherwise
	 */
	public Result showDialog()
	{
		pack();
		setVisible(true);
		return result;
	}

	/**
	 * Overridden here to set a name for automation
	 *
	 * @param name The name - ignored here
	 */
	@Override public void setName(String name)
	{
		super.setName("PlacePinsDialog");
	}

	protected static String getTheTitle()
	{
		return ResourceMgr.getString(PlacePinsDialog.class, "PlacePinsDialog.title");
	}

	protected void addComponents(@NotNull IPlacementOptionParams params)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JPanel(), BorderLayout.EAST);
		panel.add(new JPanel(), BorderLayout.WEST);
		panel.add(new JPanel(), BorderLayout.NORTH);

		// Violate Mr Liscov with this hideous hack of swapping the OK/Cancel buttons for Place/Create
		placeButton = createPlaceButton();
		setOkButton(placeButton);
		if (allowCreation) {
			createButton = BasicUIFactory.getInstance().createSiemensCustomJButton(
					ResourceMgr.getString(PlacePinsDialog.class, "PlacePinsDialog.CreateButton"));
			createButton.setName("createButton");
		}

		m_optionsPanel = createOptionsPanel();
		panel.add(m_optionsPanel, BorderLayout.SOUTH);
		initOptionsPropertyGroup();
//		createAsReferenceCheckBox();
//		createAsStackOption();
		setCancelButton(createButton);
		// dts0100590703: Create Pin dialog not working with ENTER key
		getRootPane().setDefaultButton(createButton); // Set the default button
		panel.add(getPinTablePanel(), BorderLayout.CENTER);

		getContentPane().add(panel, BorderLayout.CENTER);
	}

	@NotNull protected JButton createPlaceButton()
	{
		JButton button =
				BasicUIFactory.getInstance()
						.createSiemensCustomJButton(ResourceMgr
								.getString(PlacePinsDialog.class, "PlacePinsDialog.PlaceButton"), true);
		button.setName("placeButton");
		button.setEnabled(false);
		return button;
	}

	@NotNull protected JPanel createOptionsPanel()
	{
		JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new BorderLayout());
		optionsPanel.add(new JPanel(), BorderLayout.WEST);
		return optionsPanel;
	}

	public boolean isPlaceAsGroup()
	{
		return m_placeAsGroup;
	}

	public boolean isWithConductor()
	{
		return m_withConductorOption != null && m_withConductorOption.getValue();
	}

	protected void addItemListenerForReferencePin(JCheckBox referenceOption)
	{
	}

	protected void hookupComponents()
	{
		placeButton.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						result = Result.PLACE;
						savePrefs();
						setVisible(false);
						dispose();
					}
				}
		);

		if (createButton != null) {
			// IJ is losing the plot
			//noinspection ConstantConditions
			createButton.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							result = Result.CREATE;
							savePrefs();
							setVisible(false);
							dispose();
						}
					}
			);
		}

		// explicit focus order - Create button should be the first
		setFocusOrder();
		// TODO jacobt FEAT13040 : focus traversal stalls at last component - problem in the base code?
	}

	/**
	 * Overridden here to avoid pressing the Cancel button on close.  Because - er - we set "Cancel" to be "Create".
	 *
	 * @param e The event
	 */
	public void windowClosing(WindowEvent e)
	{
		// Result should be CANCEL anyway but let's be explicit
		result = Result.CANCEL;

		// the rest of it must be done - a bit like in the base class
		storeGeometry();
		savePrefs();
		setVisible(false);
		dispose();
	}

	/**
	 * Overridden here so that hitting Escape does not press the "Cancel" button, which we set to be "Create". Apologies
	 * again, Mr Liskov
	 *
	 * @param e The key event
	 */
	public void keyPressed(KeyEvent e)
	{
		// Result should be CANCEL anyway but let's be explicit
		result = Result.CANCEL;
	}

	protected abstract JPanel getPinTablePanel();

	protected void setFocusOrder()
	{
		Component[] focusOrder;
		if (createButton != null) {
			focusOrder = new Component[]{createButton, getPinTablePanel(), getHelpButton(), placeButton};
		}
		else {
			focusOrder = new Component[]{getPinTablePanel(), getHelpButton(), placeButton};
		}
		setFocusTraversal(focusOrder);
	}
}
