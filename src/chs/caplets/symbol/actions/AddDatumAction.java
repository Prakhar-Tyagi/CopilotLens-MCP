/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
package chs.caplets.symbol.actions;

import chs.caf.AbstractContextAction;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.IApplicationSpecificationAction;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.COFTypeEnum;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IEngineeringDatum;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.reln.IRelatedEntityType;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.reln.RelatedEntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Creates RED datum
 */
public class AddDatumAction extends AbstractAddDatumAction implements KeyListener
{

	//Related entity type just wraps three enums, so it's is okay to hold on to this in action.
	protected IRelatedEntityType m_relatedEntityType = IRelatedEntityType.Unknown;
	private List<String> datumLocationsToCreate = new ArrayList<String>();
	private COFTypeEnum m_type;
	private IDatum m_selectedDatum = null;

	public AddDatumAction(ICapletController controller)
	{
		super(controller, null);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (super.onActivate(e) == IActionEnum.eCanceled) {
			return IActionEnum.eCanceled;
		}
		return showDialog();
	}

	@NotNull protected IActionEnum showDialog()
	{
		IFIB fib = getController().getCaplet().getFIB();
		Frame owner = fib.getWindowMgr().getDialogFrame();

		COFTypeEnum selectedType = getSelectedEntityType();

		REDDatumDialog dialog =
				new REDDatumDialog(owner,
						ResourceMgr.getString(AddDatumAction.class, "AddDatumAction.REDDatumDialog.name"), true,
						selectedType);
		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			return IActionEnum.eCanceled;
		}
		m_relatedEntityType = dialog.getRelatedEntityType();
		return IActionEnum.eActivated;
	}

	@Nullable protected COFTypeEnum getSelectedEntityType()
	{
		if (m_selectedDatum != null) {
			ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
			IRelatedEntityType selectedRET = symDef.getRelatedEntityType(m_selectedDatum);
			return selectedRET == null ? null : selectedRET.getTargetEntityType();
		}
		return null;
	}

	@NotNull @Override protected IBaseDatum newDatum()
	{
		return FactoryMgr.getCommonFactory().createDatum(FactoryMgr.createUID(), m_type);
	}

	@Override protected void addDatumToStamp(@NotNull IStamp stamp, @NotNull IBaseDatum datum)
	{
		stamp.addDatum(getRelatedEntityType(), (IDatum) datum, m_selectedDatum, -1);
	}

	public boolean onTerminate(boolean successful)
	{
		cleanUpTransientGraphics();

		m_type = m_relatedEntityType.getTargetEntityType();

		if (successful) {
			for (String currPointString : datumLocationsToCreate) {
				StringTokenizer sToken = new StringTokenizer(currPointString, ".");
				assert sToken.countTokens() == 2;
				String xString = sToken.nextToken();
				String yString = sToken.nextToken();
				Point currPoint = new Point(Integer.parseInt(xString), Integer.parseInt(yString));

				// Add the datum...
				createPositionnedDatum(currPoint, null);
			}
		}

		datumLocationsToCreate.clear();
		m_relatedEntityType = IRelatedEntityType.Unknown;

		refreshUIOnTerminate();

		return true;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddDatumActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		if (super.isEnabled()) {
			SelectSet selections = getController().getSelectMgr().getCurrentSelections();
			if (validSelection(selections)) {
				if (m_selectedDatum != null) {
					ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
					IRelatedEntityType selectedRET = symDef.getRelatedEntityType(m_selectedDatum);
					if (selectedRET != null) {
						COFTypeEnum targetEntityType = selectedRET.getTargetEntityType();
						List<IRelatedEntityType> entityTypes =
								RelatedEntityUtils.getRelatedEntities(targetEntityType.value(), getSupportedRETypes());
						return !entityTypes.isEmpty();
					}
					return false;
				}
				return true;
			}
		}
		return false;
	}

	@NotNull private IObjectFilter<IRelatedEntityType> getSupportedRETypes()
	{
		return new IObjectFilter<IRelatedEntityType>()
		{
			public boolean accept(IRelatedEntityType obj)
			{
				return !(obj.getTargetEntityType() == COFTypeEnum.WireEnd);
			}
		};
	}

	private boolean validSelection(SelectSet selections)
	{
		m_selectedDatum = null;
		if (selections.getSelectCount() == 1) {
			IUIDObject sel = selections.getSingleSelectedUIDObject();
			assert sel != null;
			if (IDatumRepresentation.class.isAssignableFrom(sel.getClass())) {
				IBaseDatum datum = ((IDatumRepresentation) sel).getDatum();
				if (datum instanceof IEngineeringDatum) {
					return false;
				}
				else {
					if (datum instanceof IDatum) {
						m_selectedDatum = (IDatum) datum;
						return true;
					}
				}
			}
		}
		else {
			if (selections.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		boolean populate = isEnabled() && validSelection(selections);
		if (populate) {
			if (m_selectedDatum != null) {
				ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
				IRelatedEntityType selectedRET = symDef.getRelatedEntityType(m_selectedDatum);

				if (selectedRET != null) {
					COFTypeEnum targetEntityType = selectedRET.getTargetEntityType();
					List<IRelatedEntityType> entityTypes =
							RelatedEntityUtils.getRelatedEntities(targetEntityType.value(), getSupportedRETypes());
					if (!entityTypes.isEmpty()) {
						ActionEntry actionEntry = new ActionEntry(getActionUI());
						actionEntry.setName(ResourceMgr.getStringForMenu(AddDatumAction.class,
								"AddDatumAction.AddAssociateDatum.action.name"));
						container.add(actionEntry);
					}
				}
			}
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		AbstractAction act = new CommitAbstractAction(this);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		act.putValue(Action.NAME,
				ResourceMgr.getString(AddDatumAction.class, "AddDatumAction.commit.action.name"));
		act.putValue(Action.SHORT_DESCRIPTION,
				ResourceMgr.getString(AddDatumAction.class, "AddDatumAction.commit.action.name"));
		act.putValue(Action.LONG_DESCRIPTION, ResourceMgr.getString(AddDatumAction.class,
				"AddDatumAction.commit.action.description"));
		act.putValue(Action.ACCELERATOR_KEY, accel);

		container.add(new ActionEntry(act));
	}

	public void mouseReleased(MouseEvent e)
	{
		if (m_currPoint != null && e.getClickCount() == 1) {
			addLocationToCreate(m_currPoint);
			IDrawFactory drawFact = FactoryMgr.getDrawFactory();
			IGfxObject gfxObj = drawFact.constructCircle(m_currPoint.x, m_currPoint.y, IGrid.GRID_SIZE / 2);
			gfxObj.setAttribute(drawFact.constructAttribute(drawFact.lookupColor("select")));
			m_dynamics.addTransientGfx(gfxObj);
		}
	}

	protected void addLocationToCreate(@NotNull Point point)
	{
		String key = point.x + "." + point.y;
		datumLocationsToCreate.add(key);
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2) {
			if (!datumLocationsToCreate.isEmpty()) {//IESCD-2768
				datumLocationsToCreate.remove(datumLocationsToCreate.size() - 1);
			}
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		m_currPoint = createValidPointUnderMouse(e);

		String key = computeCurrentPointKey(m_currPoint);
		if (!m_prevLocations.contains(key) && !datumLocationsToCreate.contains(key)) {
			updateDynamicGraphics(m_currPoint);
		}

		redrawView();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddDatumAction.statusbar.text");
	}

	public IRelatedEntityType getRelatedEntityType()
	{
		return m_relatedEntityType;
	}

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

	private class CommitAbstractAction extends AbstractContextAction
	{

		protected CommitAbstractAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(AddDatumAction.class,
					"AddDatumAction.commit.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			getController().getActionMgr().terminateActiveAction(true);
		}

		public boolean isEnabled()
		{
			return !datumLocationsToCreate.isEmpty();
		}
	}
}
