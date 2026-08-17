/*
 * Copyright 2010-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.symbol.properties.VariableShapeDatumUpdater;
import chs.cof.draw.IGfxObjectContainer;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IDiagramObject;
import chs.common.IBaseDatum;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeTypes;
import chs.ctf.editui.ICommonUIClient;
import chs.ctf.editui.SingleAttributeClient;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Collections;

@SuppressWarnings({"ClassNameSameAsAncestorName"})

public class SmartEditAction extends chs.caplets.logic.actions.SmartEditAction
{

	public SmartEditAction(ICapletController controller)
	{
		super(controller);
	}

	@Nullable protected ICommonUIClient createSmartClient()
	{
		if (m_object instanceof IAttributeText) {
			final IAttributeText attText = (IAttributeText) m_object;
			m_att = attText.getOMAttribute();
			String name = attText.getName();
			m_objectAttProvider = attText.getAttributeProvider();

			if (name.equals(IAttributeTypes.NAME)) {
				m_uiClient = createNameClient();
			}
			else {
				m_uiClient = new SingleAttributeClient(m_att)
				{
					@Override public boolean isGraphicsEditable()
					{
						return false;
					}

					@Override public void commitChanges()
					{
						super.commitChanges();

						// Update datum representation if we have changed dimension attribute
						if (m_objectAttProvider instanceof IBaseDatum &&
								((IBaseDatum) m_objectAttProvider).hasVariableShape()) {
							IGfxObjectContainer textOwner = attText.getContainer();
							if (textOwner instanceof IDatumRepresentation) {
								updateDatumRepresentation((IDatumRepresentation) textOwner);
							}
						}
					}

					private void updateDatumRepresentation(@NotNull IDatumRepresentation datumRep)
					{
						ISymbolModel model = getModel(ISymbolModel.class);
						if (model != null) {
							new VariableShapeDatumUpdater(model).updateRepresentations(Collections.singleton(datumRep));
						}
					}
				};
			}
		}
		return m_uiClient;
	}

	@Override protected void initializeSelectedObject()
	{
		super.initializeSelectedObject();
	}

	protected IAction getDelegateAction(IUIDObject object)
	{
		ICapletController controller = getController();
		SelectSet preSelections = controller.getSelectMgr().getPreSelections();
		preSelections.remove(m_object.getUID());
		preSelections.add(new Selection(object));
		return controller.getAction(SymbolPropertiesAction.class);
	}

	@Override protected void invokeEditDialog()
	{
		if ((m_objectAttProvider != null) && (m_objectAttProvider instanceof IUIDObject)) {
			SelectSet selections = getController().getSelectMgr().getPreSelections();
			IDiagramObject parent = ((IDiagramObject) m_object).getParent();
			if (parent != null) {
				IAction action = getDelegateAction(parent);
				if (action != null) {
					ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, action.getActionName(), 0);
					getController().getActionMgr().actionPerformed(action, ae);
					registerIfInnerActionCompleted(action);
				}
				selections.remove(parent.getUID());
			}
		}
	}
}
