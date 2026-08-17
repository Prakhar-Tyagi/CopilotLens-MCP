package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IPolyline;
import chs.cof.draw.LineStyle;
import chs.cof.drawplus.IBorderHolder;
import chs.cof.drawplus.IDrawPlusFactory;
import chs.cof.symbol.Border;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IPSMBorder;
import chs.cof.symbol.IPSMStamp;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IPropertiedObject;
import chs.common.IUID;
import chs.common.IUnit;
import chs.common.UnitTypeEnum;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.GfxUtils;
import chs.utility.SymbolUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.UnitHelper;
import chs.utility.ui.CoordPanel;
import chs.utility.ui.MarginChooserPanel;
import chs.utility.ui.PaperSizeChooserPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public class BorderCreationParameterHolder implements StampCreationParameterHolder
{

	protected String borderName;
	protected IAbstractLibrary borderLibrary;
	protected IUnit width;
	protected IUnit height;
	protected IUnit[] margins;
	protected UnitTypeEnum unitType;
	protected boolean selectedCreatedGfx;

	public BorderCreationParameterHolder(IAbstractLibrary library)
	{
		borderLibrary = library;
	}

	public void collectParamsForCreation(Frame frame)
	{

		BorderDialog dialog = new BorderDialog(frame);
		dialog.setVisible(true);
		if (dialog.isCancelled() || dialog.getSelectedName().isEmpty()) {
			return;
		}
		borderName = dialog.getSelectedName();
		width = dialog.getSelectedWidth();
		height = dialog.getSelectedHeight();
		margins = dialog.getSelectedMargins();
		unitType = dialog.getSelectedWidth().getType();
		selectedCreatedGfx = dialog.getSelectedCreateGfx();
	}

	@Override public IPSMStamp createStampBasedOnParameters()
	{
		IUID buid = CAFUtils.getInstance().getCommonFactory().createUID();
		//
		IPSMBorder border = FactoryMgr.getSymbolFactory().constructBorder(buid, borderName);
		//
		IPropertiedObject conn = FactoryMgr.getCommonFactory().createPropertiedObject(FactoryMgr.createUID());
		border.setPropertyHolder(conn);
		IBorderHolder pl = FactoryMgr.getDrawPlusFactory().createBorderHolder(FactoryMgr.createUID(), conn, null, null, 0, 0);
		border.setGfx(pl);
		pl.setOwner(border);

		border.setEdited(true);
		borderLibrary.addSymbol(border);
        IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
        auditLogger.postEvent(AuditableEventType.SYMBOL_CREATED,
                ResourceMgr.getString(BorderCreationParameterHolder.class, "BorderCreationParameterHolder.AuditTrail.Border"),
                borderLibrary.getUID().getString(),
                border.getName(), border.getUID().getString());

		//
		// Now we add graphics onto the border to initialize it...
		//

		IUnit page = border.getGrid().getRealMapping();
		//
		int wscale = (int) Math.round(width.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
		int hscale = (int) Math.round(height.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
		int xpos = wscale / 2;
		int ypos = hscale / 2;
		int mtop = (int) (margins[0].getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
		int mright = (int) (margins[1].getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
		int mbottom = (int) (margins[2].getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
		int mleft = (int) (margins[3].getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
		//
		IExtent paperSize =
				CAFUtils.getInstance().getCommonFactory().constructExtent(-xpos, -ypos, wscale, hscale);
		border.setPaperSize(paperSize);
		border.setUnits(UnitHelper.enumToString(unitType));

		//
		// Create the border markers...
		//
		ICommonFactory commonFac = FactoryMgr.getCommonFactory();
		IDrawFactory drawfac = FactoryMgr.getDrawFactory();
		IDrawPlusFactory drawplusfac = FactoryMgr.getDrawPlusFactory();
		//
		// Create the page border as lines, so we don't have any dodgy selection issues...
		//
		ICompoundObject bco = border.getGfx();
		IGfxAttribute bAttr = drawfac.constructGfxAttribute(drawfac.constructColor("border"), 1, LineStyle.SOLID);
		bco.setAttribute(bAttr);
		//
		// Construction graphics
		//
		IExtent containingArea = commonFac.constructExtent(-xpos, -ypos, wscale, hscale);
		if (selectedCreatedGfx) {
			int w1 = -xpos + mleft;
			int w2 = xpos - mright;
			int h1 = -ypos + mbottom;
			int h2 = ypos - mtop;
			containingArea.setBounds(w1, h1, w2 - w1, h2 - h1);
			//
			// Have to add as separate polylines, as this is a border and a closed polyline will intersect any view
			// and add to the calculations on each render.
			//
			List<Point> pointColl =
					Arrays.asList(new Point(w1, h1), new Point(w1, h2), new Point(w2, h2), new Point(w2, h1),
							new Point(w1, h1));
			for (int i = 0; i < 4; i++) {
				List<Point> pair = pointColl.subList(i, i + 2);
				IUID u1 = commonFac.createUID();
				IPolyline line = drawplusfac.constructPropPolyline(u1, pair, false);
				bco.addObject(line);
			}
		}

		border.setUsableArea(Border.calculateUsableArea(containingArea));

		return border;
	}

	@Nullable @Override public UserActionFailureReason validateCreationParameters()
	{
		if (StringUtils.isBlank(borderName)) {
			return UserActionFailureReason.IGNOREFAILURE;
		}
		if (SymbolUtils.doesBorderNameAlreadyExists(borderLibrary, borderName)) {
			return UserActionFailureReason.DUPLICATEBORDERNAME;
		}
		return null;
	}

	@Override public String getName()
	{
		return borderName;
	}

    @Override public boolean canceledAction()
    {
        return false;
    }

    public static class BorderDialog extends CAFOkCancelDialog
	{

		private static final int DIALOG_MIN_SIZE_WIDTH = 300;
		private static final int DIALOG_MIN_SIZE_HEIGHT = 400;
		protected JTextField m_nameField;
		private PaperSizeChooserPanel m_paperSizeChooser;
		private MarginChooserPanel m_marginChooser;

		public BorderDialog(final Frame owner)
		{
			super(owner, ResourceMgr
					.getString(BorderCreationParameterHolder.class, "BorderCreationParameterHolder.BorderNew.Label"),
					true);
			Container c = getContentPane();
			JPanel main = new JPanel();
			main.setLayout(new GridBagLayout());
			setMinimumSize(new Dimension(DIALOG_MIN_SIZE_WIDTH, DIALOG_MIN_SIZE_HEIGHT));
			main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			JPanel nameChooser = new JPanel();
			m_nameField = new JTextField(10);
			m_nameField.setName("bordercreate.name");

			m_paperSizeChooser = new PaperSizeChooserPanel();
			m_marginChooser = new MarginChooserPanel();

			DocumentListener overallCheck = new DocumentListener()
			{
				public void changedUpdate(DocumentEvent e)
				{
					doCheckAll();
				}

				public void insertUpdate(DocumentEvent e)
				{
					doCheckAll();
				}

				public void removeUpdate(DocumentEvent e)
				{
					doCheckAll();
				}

				private void doCheckAll()
				{
					String errmsg = validName(m_nameField.getText());
					if (errmsg != null) {
						m_nameField.setForeground(Color.RED);
						m_nameField.setToolTipText(!errmsg.isEmpty() ? errmsg : null);
					}
					else {
						m_nameField.setForeground(Color.BLACK);
						m_nameField.setToolTipText(null);
					}
					boolean b = errmsg == null;
					b &= m_paperSizeChooser.isPanelValid();
					b &= m_marginChooser.isPanelValid();
					getOkButton().setEnabled(b);
				}
			};
			m_nameField.getDocument().addDocumentListener(overallCheck);
			m_paperSizeChooser.addDocumentListener(overallCheck);
			m_marginChooser.addDocumentListener(overallCheck);
			//
			nameChooser.setLayout(new BorderLayout(5, 0));
			JLabel label = new JLabel(ResourceMgr.getStringForLabel(BorderCreationParameterHolder.class,
					"BorderCreationParameterHolder.label.text"));
			nameChooser.add(label, BorderLayout.WEST);
			nameChooser.add(m_nameField, BorderLayout.CENTER);

			m_paperSizeChooser.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					m_marginChooser.setWidthField(m_paperSizeChooser.getUnitType());
				}
			});
			m_marginChooser.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					//
					// When changing a value on the page size chooser, reset the margins.
					//
					UnitTypeEnum unitType = m_marginChooser.getUnitType();
					if (unitType != UnitTypeEnum.TypeLogical) {
						m_paperSizeChooser.setWidthField(m_marginChooser.getUnitType());
					}
				}
			});
			CoordPanel wf = m_paperSizeChooser.getWidthField();
			CoordPanel hf = m_paperSizeChooser.getHeightField();
			m_marginChooser.reset(wf.getType(), hf.getType());

			addPanelsToBorderPanel(main, nameChooser);

			getOkButton().setEnabled(false);
			c.add(main);

			getOkButton().addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setCancelled(false);
					setVisible(false);
				}
			});

			getCancelButton().addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setCancelled(true);
					setVisible(false);
				}
			});
			pack();
		}

		private void addPanelsToBorderPanel(JPanel main, JPanel nameChooser)
		{
			GridBagConstraints gbcon = new GridBagConstraints();
			gbcon.anchor = GridBagConstraints.WEST;
			gbcon.insets = new Insets(2, 4, 2, 1);

			gbcon.weightx = 1;
			gbcon.fill = GridBagConstraints.HORIZONTAL;

			int row = 1;
			gbcon.gridy = row;
			main.add(nameChooser, gbcon);

			row++;
			gbcon.gridy = row;
			main.add(m_paperSizeChooser, gbcon);

			row++;
			gbcon.gridy = row;
			main.add(m_marginChooser, gbcon);
		}

		public boolean getSelectedCreateGfx()
		{
			return m_marginChooser.createGraphics();
		}

		public IUnit getSelectedWidth()
		{
			IUnit u = CAFUtils.getInstance().getCommonFactory().createUnit();
			return m_paperSizeChooser.getWidthField().getUnit(u);
		}

		public IUnit getSelectedHeight()
		{
			IUnit u = CAFUtils.getInstance().getCommonFactory().createUnit();
			return m_paperSizeChooser.getHeightField().getUnit(u);
		}

		public IUnit[] getSelectedMargins()
		{
			IUnit ut = CAFUtils.getInstance().getCommonFactory().createUnit();
			IUnit ur = CAFUtils.getInstance().getCommonFactory().createUnit();
			IUnit ub = CAFUtils.getInstance().getCommonFactory().createUnit();
			IUnit ul = CAFUtils.getInstance().getCommonFactory().createUnit();
			return new IUnit[]{m_marginChooser.getTopField().getUnit(ut),
					m_marginChooser.getRightField().getUnit(ur),
					m_marginChooser.getBottomField().getUnit(ub),
					m_marginChooser.getLeftField().getUnit(ul)};
		}

		public String getSelectedName()
		{
			return m_nameField.getText().trim();
		}

		// Return null if valid; errmsg otherwise.  Check name is not empty and not a duplicate
		@Nullable public String validName(String newName)
		{
			if (StringUtils.getTrimmed(newName) == null) {
				return ResourceMgr
						.getString(BorderCreationParameterHolder.class, "BorderCreationParameterHolder.Empty.Name");
			}
			if (newName.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
				return ResourceMgr
						.getString(BorderCreationParameterHolder.class, "BorderCreationParameterHolder.NameTooLong.Msg",
								String.valueOf(CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH));
			}
			return null;
		}
	}
}