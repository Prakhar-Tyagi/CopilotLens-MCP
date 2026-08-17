/*
 * Copyright 2007-2013 Mentor Graphics Corporation
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
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caplets.symbol.Model;
import chs.caplets.symbol.properties.ReorderDatumDialog;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.IAttributeDatum;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IUIDObject;
import chs.common.reln.IRelatedEntityType;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.WindowConstants;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReorderDatumAction extends ControllerActionRT implements ICtxMenuProvider//, MouseListener
{

	//Related entity type just wraps three enums, so it's is okay to hold on to this in action.
	private IRelatedEntityType m_relatedEntityType = IRelatedEntityType.Unknown;
	private Model m_model;
	private ReorderDatumDialog m_dialog;
	private boolean m_completed = false;
	private List<IDatum> m_relatedDatumsofType = null;
	private IDatum m_selectedDatum = null;
	private IDatum m_parentDatum = null;

	public ReorderDatumAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		IStamp stamp = m_model.getSymbolDef();
		if (!(stamp instanceof ISymbolDef)) {
			return IActionEnum.eCanceled;
		}
		m_relatedEntityType = stamp.getRelatedEntityType(m_selectedDatum);

		m_parentDatum = stamp.getParentDatum(m_selectedDatum);
		Map<IRelatedEntityType, List<IDatum>> relatedDatums;
		if (m_parentDatum != null) {
			relatedDatums = stamp.getAssociatedDatums(m_parentDatum);
		}
		else {
			relatedDatums = stamp.getTopLevelDatums();
		}

		m_relatedDatumsofType = relatedDatums.get(m_relatedEntityType);
		if (m_relatedDatumsofType == null || m_relatedDatumsofType.isEmpty()) {
			return IActionEnum.eCanceled;
		}

		showReorderDialog();

		if (m_completed) {
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected void showReorderDialog()
	{
		List<String> datumNames = getReorderListFromDatums(m_relatedDatumsofType);
		IFIB fib = getController().getCaplet().getFIB();
		assert fib != null;
		Frame owner = fib.getWindowMgr().getDialogFrame();

		m_dialog = new ReorderDatumDialog(owner, datumNames, true);
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

	@Nullable private static List<String> getReorderListFromDatums(List<IDatum> datumList)
	{
		if (datumList != null && !datumList.isEmpty()) {
			List<String> datumNames = new ArrayList<String>(datumList.size());
			for (IDatum aDatumList : datumList) {
				datumNames.add(aDatumList.getName());
			}
			return datumNames;
		}
		return null;
	}

	@Nullable IDatumRepresentation getPreselectedDatum()
	{
		SelectSet selectSet = getController().getSelectMgr().getPreSelections();
		List<Selection> selectedDatums =
				selectSet.getFilteredSelections(new SelectionFilter(IDatumRepresentation.class));
		if (!selectedDatums.isEmpty()) {
			return ((IDatumRepresentation) selectedDatums.get(0).getObject());
		}
		return null;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (!successful) {
			return false;
		}
		IStamp stamp = m_model.getSymbolDef();

		Map<IDatum, Collection<IAttributeDatum>> associatedAttributeDatums = new HashMap<IDatum, Collection<IAttributeDatum>>();
		if (m_parentDatum != null) {
			stamp.removeAssociatedDatums(m_parentDatum, m_relatedEntityType);
		}
		else {

			Map<IRelatedEntityType, List<IDatum>> relatedDatumsMap = stamp.getTopLevelDatums();
			if (relatedDatumsMap.containsKey(m_relatedEntityType)) {
				m_relatedDatumsofType = relatedDatumsMap.get(m_relatedEntityType);
				for (IDatum obj : m_relatedDatumsofType) {
					Collection<IAttributeDatum> associateAttributeDatumCollection =
							stamp.getAssociatedAttributesDatums(obj);
					if (!associateAttributeDatumCollection.isEmpty()) {
						associatedAttributeDatums.put(obj, associateAttributeDatumCollection);
					}
					stamp.removeDatum(obj);
				}
			}
		}

		List<String> reorderedList = getReorderedDatumNames();
		int size = reorderedList.size();
		List<IDatum> reorderedDatums = new ArrayList<IDatum>(size);

		for (String name : reorderedList) {
			reorderedDatums.add(getDatumByName(name));
		}
		m_relatedDatumsofType.clear();
		m_relatedDatumsofType.addAll(reorderedDatums);

		if (m_parentDatum != null) {

			for (IDatum aM_relatedDatumsofType : m_relatedDatumsofType) {
				stamp.addDatum(m_relatedEntityType, aM_relatedDatumsofType, m_parentDatum, -1);
			}
		}
		else {

			for (IDatum aM_relatedDatumsofType : m_relatedDatumsofType) {
				stamp.addDatum(m_relatedEntityType, aM_relatedDatumsofType, null, -1);

				if (associatedAttributeDatums.get(aM_relatedDatumsofType) != null) {
					for (IAttributeDatum attributeDatum : associatedAttributeDatums.get(aM_relatedDatumsofType)) {
						stamp.addAttributeDatumAssociation(aM_relatedDatumsofType, attributeDatum);
					}
				}
			}
		}

		return true;
	}

	@NotNull protected List<String> getReorderedDatumNames()
	{
		List<String> reorderedList = m_dialog.getReorderdDatumNames();
		return reorderedList == null ? Collections.<String>emptyList() : reorderedList;
	}

	@Nullable private IDatum getDatumByName(String name)
	{
		for (IDatum datum : m_relatedDatumsofType) {
			if (datum.getName().equals(name)) {
				return datum;
			}
		}
		return null;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return ReorderDatumActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return m_selectedDatum != null && super.isEnabled();
	}

	private boolean validSelection(SelectSet selections)
	{
		m_selectedDatum = null;
		if (selections.getSelectCount() == 1) {
			IUIDObject sel = selections.getSingleSelectedUIDObject();
			assert sel != null;
			if (IDatumRepresentation.class.isAssignableFrom(sel.getClass())) {
				IBaseDatum baseDatum = ((IDatumRepresentation) sel).getDatum();
				if (baseDatum instanceof IDatum) {
					m_selectedDatum = (IDatum) ((IDatumRepresentation) sel).getDatum();
				}
			}
		}
		return m_selectedDatum != null;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		boolean populate = validSelection(selections);
		if (populate) {
			IStamp stamp = m_model.getSymbolDef();
			m_relatedEntityType = stamp.getRelatedEntityType(m_selectedDatum);
			Action ui = getActionUI();
			ui.putValue(Action.NAME,
					ResourceMgr.getString(this, "ReorderDatumAction.Reorder.Datum", m_relatedEntityType.getRelation()));
			container.add(new ActionEntry(ui));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "ReorderDatumAction.statusbar.text");
	}

	/**
	 * Return the cursor for this action
	 */
	@Nullable public Cursor getCursor()
	{
		return CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);
	}

	protected void setCompleted(boolean completed)
	{
		m_completed = completed;
	}
}
