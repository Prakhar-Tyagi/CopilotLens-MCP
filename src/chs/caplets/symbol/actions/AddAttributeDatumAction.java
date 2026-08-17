/*
 * Copyright 2008-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.symbol.properties.AddAttributeDatumDialog;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.symbol.IStamp;
import chs.common.DatumTypeEnum;
import chs.common.IAttributeDatum;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.WindowConstants;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AddAttributeDatumAction extends AbstractAddDatumAction
{

	private AddAttributeDatumDialog m_dialog;
	private boolean m_completed = false;
	private COFTypeEnum m_type;
	private String m_name;
	private DatumTypeEnum m_subType;
	private IDatum m_redDatum;

	public AddAttributeDatumAction(ICapletController controller)
	{
		super(controller, null);
	}

	protected void showDialog()
	{
		Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();

		m_dialog = new AddAttributeDatumDialog(owner, true, m_type);
		m_dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_dialog.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				m_dialog.dispose();
			}
		});

		m_dialog.getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setCompleted(true);
				m_dialog.setCancelled(false);
				m_dialog.dispose();
			}
		});

		m_dialog.getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setCompleted(false);
				m_dialog.setCancelled(true);
				m_dialog.dispose();
			}
		}
		);

		m_dialog.pack();
		m_dialog.setVisible(true);
	}

	@Nullable protected String getSelectedObjectName()
	{
		return m_dialog == null ? null : m_dialog.getSelectedObjectName();
	}

	@Nullable protected COFTypeEnum getSelectedCOFType()
	{
		return m_dialog == null ? null : m_dialog.getSelectedCOFType();
	}

	@Nullable protected DatumTypeEnum getSelectedObjectSubType()
	{
		return m_dialog == null ? null : m_dialog.getSelectedObjectSubType();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_redDatum = getCurrentDatumSelected();
		m_type = m_redDatum == null ? null : m_redDatum.getType();

		// Check symbol
		if (!checkSymbol()) {
			return IActionEnum.eCanceled;
		}

		showDialog();

		if (m_completed && getSelectedObjectName() != null) {
			return super.onActivate(e);
		}

		return IActionEnum.eCanceled;
	}

	@Nullable private IDatum getCurrentDatumSelected()
	{
		IDatum selectedDatum = null;
		SelectSet selectSet = m_model.getController().getSelectMgr().getCurrentSelections();
		IUIDObject sel = selectSet.getSingleSelectedUIDObject();
		if (sel instanceof IDatumRepresentation && ((IDatumRepresentation) sel).getDatum() instanceof IDatum) {
			selectedDatum = (IDatum) ((IDatumRepresentation) sel).getDatum();
		}
		return selectedDatum;
	}

	@NotNull @Override protected IBaseDatum newDatum()
	{
		IAttributeDatum datum = FactoryMgr.getCommonFactory().createAttributeDatum(FactoryMgr.createUID(),
				m_type, m_name);
		datum.setName(m_type.toString() + "_" + m_subType.getName() + "_" + m_name);
		datum.setDatumSubType(m_subType);
		return datum;
	}

	@Override protected void addDatumToStamp(@NotNull IStamp stamp, @NotNull IBaseDatum datum)
	{
		IAttributeDatum attrDatum = (IAttributeDatum) datum;
		stamp.addDatum(attrDatum);
		if (m_redDatum != null) {
			stamp.addAttributeDatumAssociation(m_redDatum, attrDatum);
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		m_type = getSelectedCOFType();
		m_name = getSelectedObjectName();
		m_subType = getSelectedObjectSubType();

		cleanUpTransientGraphics();

		if (successful && m_type != null && m_subType != null && m_name != null && m_currPoint != null) {
			createPositionnedDatum(m_currPoint, m_name);
		}

		refreshUIOnTerminate();

		return true;
	}

	public String getActionUIClass()
	{
		return AddAttributeDatumActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isSelectionsWorkable(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	private boolean isSelectionsWorkable(SelectSet selections)
	{
		boolean success = false;
		if (selections.getSelectCount() == 1) {
			IUIDObject sel = selections.getSingleSelectedUIDObject();
			if (sel instanceof IDatumRepresentation && ((IDatumRepresentation) sel).getDatum() instanceof IDatum) {
				success = true;
			}
		}
		return success;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddAttributeDatumAction.statusbar.text");
	}

	protected void setCompleted(boolean completed)
	{
		m_completed = completed;
	}
}
