/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.actions;

import chs.caf.CAFUtils;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.logical.concurrency.LogicConcurrencyFactory;
import chs.common.IProjectPreferenceMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.EndLineStyleUtils;
import chs.utility.helpers.SchemConductorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PokeHomeDialog // extends CAFOkCancelDialog
{

	private List<ConductorPanel> m_conductorPanels = null;
	private int m_gridSpacing;

	public PokeHomeDialog()
	{
	}

	public JPanel getPanel(List<chs.cof.logical.schem.IConductor> schemCondList)
	{
		JPanel mainBorderPanel = new JPanel();
		mainBorderPanel.setLayout(new BorderLayout(5, 5));
		mainBorderPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Set the allCondsPanel
		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BorderLayout());

		JPanel allCondsPanel = new JPanel();
		// Get all cond panels
		m_conductorPanels = new ArrayList<ConductorPanel>();
		for (chs.cof.logical.schem.IConductor cond : schemCondList) {
			//Create a new condPanel
			ConductorPanel condPanel = constructConductorPanel(cond);
			m_conductorPanels.add(condPanel);
		}
		IBaseDiagram diagram = DiagramHelper.getDiagram(schemCondList.get(0));
		assert diagram != null;
		m_gridSpacing = diagram.getGrid().getGridSpacing();
		// Sort the panels...
		AlphaNumComparator<ConductorPanel> anc = new AlphaNumComparator<ConductorPanel>(true)
		{
			protected String stringify(ConductorPanel object)
			{
				return object.getName();
			}
		};
		Collections.sort(m_conductorPanels, anc);

		allCondsPanel.setLayout(new GridLayout(m_conductorPanels.size(), 1, 5, 5));
		allCondsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		containerPanel.add(allCondsPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(containerPanel);
		// Add the scrollPane
		mainBorderPanel.add(scrollPane, BorderLayout.CENTER);

		for (ConductorPanel condPanel : m_conductorPanels) {
			// Add this to the allCondsPanel
			//
			JPanel p1 = new JPanel();
			p1.setLayout(new GridLayout(1, 2));
			List<Component> clst = condPanel.getComponents();
			JPanel lp = new JPanel();
			lp.setLayout(new GridLayout(3, 1));
			lp.add(clst.get(0));
			p1.add(lp);
			JPanel rbp = new JPanel();
			rbp.setLayout(new GridLayout(3, 1));
			rbp.add(clst.get(1));
			rbp.add(clst.get(2));
			rbp.add(clst.get(3));
			p1.add(rbp);
			p1.setBorder(BorderFactory.createLineBorder(Color.lightGray));
			allCondsPanel.add(p1);
		}
		return mainBorderPanel;
	}

	@NotNull protected ConductorPanel constructConductorPanel(chs.cof.logical.schem.IConductor cond)
	{
		return new ConductorPanel(cond);
	}

	/**
	 * Return a list of conductorPanels which have selection have changed
	 *
	 * @return - changed conductor panels
	 */
	public List<ConductorPanel> getChangedConductorPanels()
	{
		List<ConductorPanel> changedList = new ArrayList<ConductorPanel>();
		for (ConductorPanel condPanel : m_conductorPanels) {
			if (condPanel.hasChangedSelection()) {
				changedList.add(condPanel);
			}
		}
		return changedList;
	}

	public void editModel()
	{
		// Get the information from the dialog
		List<ConductorPanel> changedCondPanelList = getChangedConductorPanels();

		// For each change conductor Panel - get the conductor & pinSelected
		for (ConductorPanel condPanel : changedCondPanelList) {
			chs.cof.logical.schem.IConductor schemCond = condPanel.getConductor();
			IConductor cableCond = schemCond.getConnectivity();
			// Get the pin selected
			IPin schemPinSelected = condPanel.getPinSelected();

			IAbstractPin oldPokeHomePin = null;
			if (cableCond instanceof IPhysicalConductor) {
				oldPokeHomePin = ((IPhysicalConductor) cableCond).getExplicitPokeHomePin();
			}

			IAbstractPin newPokeHomePin = null;
			if (schemPinSelected != null) {
				newPokeHomePin = schemPinSelected.getConnectivity();
			}

			if (newPokeHomePin == oldPokeHomePin) {
				continue;
			}

			if (oldPokeHomePin != null) {
				removePokeHomeGraphics(cableCond, oldPokeHomePin);
			}

			if (newPokeHomePin != null) {
				newPokeHomePin = schemPinSelected.getConnectivity();
				addPokeHomeGraphics(cableCond, newPokeHomePin);
			}

			// Set the poke home ref
			if (cableCond instanceof IPhysicalConductor) {
				((IPhysicalConductor) cableCond).setExplicitPokeHomePin(
						newPokeHomePin != null ? newPokeHomePin.getUID() : null);
			}
		}
	}

	@Nullable
	private static ISegment findPokeHomeSegment(chs.cof.logical.schem.IConductor schemCond, IAbstractPin pokeHomePin)
	{
		IPin pin = findPokeHomePin(schemCond, pokeHomePin);
		return findPokeHomeSegment(schemCond, pin);
	}

	@Nullable
	private static ISegment findPokeHomeSegment(chs.cof.logical.schem.IConductor schemCond, @Nullable IPin pin)
	{
		if (pin != null) {
			IJoint joint = pin.getJoint();
			Collection<ISegment> segs = joint.getAssociations(ISegment.class);
			for (ISegment seg : segs) {
				if (seg.getConductor() == schemCond) {
					return seg;
				}
			}
		}
		return null;
	}

	@Nullable private static IPin findPokeHomePin(chs.cof.logical.schem.IConductor schemCond, IAbstractPin pokeHomePin)
	{
		for (IPin pin : SchemConductorHelper.getPins(schemCond)) {
			if (pin.getConnectivity() == pokeHomePin) {
				return pin;
			}
		}
		return null;
	}

	private void removePokeHomeGraphics(IConductor cableCond, IAbstractPin pokeHomePin)
	{
		ILogicDesign design = cableCond.getLogicDesign();
		assert design != null;
		LogicConcurrencyFactory.getInstance().geDefaultDiagramRepresentationUpdateStrategy(design)
				.getDiagramProcessor(Collections.singleton(cableCond)).processDiagrams((diagram) -> {
			for (IDiagramObject diagramObject : diagram.getRepresentations(cableCond.getUID())) {
				if (diagramObject instanceof chs.cof.logical.schem.IConductor) {
					chs.cof.logical.schem.IConductor schemCond =
							(chs.cof.logical.schem.IConductor) diagramObject;
					IPin pin = findPokeHomePin(schemCond, pokeHomePin);
					if (pin != null) {
						ISegment phSeg = findPokeHomeSegment(schemCond, pin);
						if (phSeg != null) {
							EndLineStyleUtils.clearPokeHomeEndAtPin(phSeg, true, pin);
						}
					}
				}
			}
		});
	}

	private void addPokeHomeGraphics(IConductor cableCond, IAbstractPin pokeHomePin)
	{
		IProjectPreferenceMgr prefs = CAFUtils.getInstance().getCurrentProjectPreferences();

		ILogicDesign design = cableCond.getLogicDesign();
		assert design != null;
		LogicConcurrencyFactory.getInstance().geDefaultDiagramRepresentationUpdateStrategy(design)
				.getDiagramProcessor(Collections.singleton(cableCond)).processDiagrams((diagram) -> {
			for (IDiagramObject diagramObject : diagram.getRepresentations(cableCond.getUID())) {
				if (diagramObject instanceof chs.cof.logical.schem.IConductor) {
					chs.cof.logical.schem.IConductor schemCond = (chs.cof.logical.schem.IConductor) diagramObject;
					IPin pin = findPokeHomePin(schemCond, pokeHomePin);
					if (pin != null) {
						ISegment phSeg = findPokeHomeSegment(schemCond, pokeHomePin);
						for (IPin schemPin : schemCond.getPins()) {
							if (schemPin.getConnectivity() == pokeHomePin) {
								if (prefs != null && phSeg != null) {
									EndLineStyleUtils.updatePokeHomeEndLineStyle(phSeg, schemPin, prefs, m_gridSpacing);
								}
							}
						}
					}
				}
			}
		});
	}

	/**
	 * ConductorPanel holds information about a conductor and its poke home selection
	 */
	public static class ConductorPanel implements ItemListener
	{

		public static final int COND_NAME_LABEL_LENGTH = 15;
		private chs.cof.logical.schem.IConductor m_conductor;
		private List<PinButtonModel> m_pinButtonModel;
		private IPin m_pinSelected;
		private boolean m_initial = true;
		private JRadioButton m_noneRB;
		private List<Component> m_comps;

		public ConductorPanel(chs.cof.logical.schem.IConductor conductor)
		{
			m_conductor = conductor;
			initPanel();
		}

		@SuppressWarnings("OverlyLongMethod") private void initPanel()
		{
			m_comps = new ArrayList<Component>();
			m_noneRB = new JRadioButton(ResourceMgr.getString(PokeHomeDialog.class, "PokeHomeDialog.None.Label"));
			m_noneRB.setName(ResourceMgr.getString(PokeHomeDialog.class, "PokeHomeDialog.None.Label"));

			String name = m_conductor.getConnectivity().getName();
			JTextField condNameLabel = new JTextField(name, COND_NAME_LABEL_LENGTH);
			condNameLabel.setEditable(false);
			condNameLabel.setCaretPosition(0);
			m_comps.add(condNameLabel);

			ButtonGroup buttonGroup = new ButtonGroup();
			m_noneRB.addItemListener(this);
			buttonGroup.add(m_noneRB);
			m_comps.add(m_noneRB);

			// just how many pins are we expecting on a single conductor anyway???
			Set<ISplice> seenOwners = new HashSet<ISplice>();
			List<IPin> pinList = new ArrayList<IPin>();
			for (IPin pin : SchemConductorHelper.getPins(m_conductor)) {
				IAbstractPin ap = pin.getConnectivity();
				//
				// Is it a splice? and is it a CSS?
				//
				IPinList apo = ap.getOwner();
				if (apo instanceof ISplice) {
					ISplice sap = (ISplice) apo;
					if (seenOwners.contains(sap)) {
						continue; // Already have this splice - this is really an error state [and this is the handler to hide it]
					}
					seenOwners.add(sap);
					if (sap.getNumCenterStrippedWires() > 0) {
						continue; // it is -> skip. It should not be displayed in the poke home.
					}
				}
				pinList.add(pin);
			}
			AlphaNumComparator<IPin> anc = new AlphaNumComparator<IPin>(true)
			{
				@Override protected String stringify(IPin object)
				{
					IPinList owner = object.getConnectivity().getOwner();
					assert owner != null;
					return owner.getName();
				}
			};
			Collections.sort(pinList, anc);

			m_pinButtonModel = new ArrayList<PinButtonModel>();
			for (IPin aPinList : pinList) {
				PinButtonModel pinButtonModel = new PinButtonModel(aPinList);
				JRadioButton pinRB = new JRadioButton(pinButtonModel.getName());
				pinRB.setName(pinButtonModel.getName());
				pinRB.setModel(pinButtonModel);
				pinRB.addItemListener(this);
				buttonGroup.add(pinRB);
				m_pinButtonModel.add(pinButtonModel);
				m_comps.add(pinRB);
			}

			for (int index = 0; index < 2; index++) {   // the conductor doesn't have two pins - fill in with blank
				JLabel blankLabel = new JLabel(" ");
				m_comps.add(blankLabel);
			}

			// Get the conductor poke home
			IAbstractPin pokeHomePin = null;
			if (m_conductor.getConnectivity() instanceof IPhysicalConductor) {
				pokeHomePin = ((IPhysicalConductor) m_conductor.getConnectivity()).getExplicitPokeHomePin();
			}

			// Set the selection based on the conductor's poke home
			if (pokeHomePin == null) {   // Not a poke home
				m_noneRB.setSelected(true);
			}
			else {
				// find this pin in the pin model
				boolean found = false;
				for (PinButtonModel pinButtonModel : m_pinButtonModel) {
					IPin pin = pinButtonModel.getUserObject();
					if (pin.getConnectivity() == pokeHomePin) {   // This is the pin - select it
						pinButtonModel.setSelected(true);
						m_pinSelected = pinButtonModel.getUserObject(); // oops, somebody forgot this
						found = true;
						break;
					}
				}
				if (!found) {
					m_noneRB.setSelected(true);
				}
			}
		}

		List<Component> getComponents()
		{
			return m_comps;
		}

		/**
		 * Receive item changed from the radio button
		 *
		 * @param e -
		 */
		public void itemStateChanged(ItemEvent e)
		{
			// Something is changed - check is selected only
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (m_initial) {   // First time is for setting the radio button selection - ignore this

					m_initial = false;
					return;
				}

				JRadioButton source = (JRadioButton) e.getSource();
				if (source == m_noneRB) {   // None is selected
					m_pinSelected = null;
				}
				else {   // The source model got to be PinButtonModel
					PinButtonModel pinButtonModel = (PinButtonModel) source.getModel();
					m_pinSelected = pinButtonModel.getUserObject();
				}
			}
		}

		/**
		 * Check to see if this panel's poke home selection has changed
		 *
		 * @return -
		 */
		public boolean hasChangedSelection()
		{
			return (!m_initial);
		}

		/**
		 * Return the selected pin
		 *
		 * @return -
		 */
		public IPin getPinSelected()
		{
			return m_pinSelected;
		}

		/**
		 * Need for sorting.
		 */
		public String getName()
		{
			return m_conductor.getConnectivity().getName();
		}

		/**
		 * Return the cable conductor
		 *
		 * @return -
		 */
		public chs.cof.logical.schem.IConductor getConductor()
		{
			return m_conductor;
		}
	}

	/**
	 * Extends the JToggleButton.ToggleButtonModel to keep IAbstractPin as its user object
	 */
	private static class PinButtonModel extends JToggleButton.ToggleButtonModel
	{

		private IPin m_pin;

		private PinButtonModel(IPin pin)
		{
			m_pin = pin;
		}

		/**
		 * Return the name to use for display
		 *
		 * @return -
		 */
		public String getName()
		{
			return getPokeHomeRefName(m_pin.getConnectivity());
		}

		private static String getPokeHomeRefName(IAbstractPin pin)
		{
			if (pin == null) {
				return "";
			}

			StringBuffer buffer = new StringBuffer();
			IPinList pinOwner = pin.getOwner();
			if (pinOwner != null) {
				buffer.append(pinOwner.getName());
			}
			return buffer.toString();
		}

		/**
		 * Return the user object
		 *
		 * @return -
		 */
		public IPin getUserObject()
		{
			return m_pin;
		}
	}
}