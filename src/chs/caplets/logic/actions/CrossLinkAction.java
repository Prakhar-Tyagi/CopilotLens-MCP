/*
 * Copyright 2004-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ViewActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Feb 12, 2004 Time: 10:08:36 AM To change this template use Options |
 * File Templates.
 */
public class CrossLinkAction extends ViewActionRT implements ICtxMenuProvider
{

	private CrossLinkHelper m_crossLinkHelper;
	private HyperlinkProducer m_hyperlinkProducer;

	public CrossLinkAction(ICapletView view)
	{
		super(view);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		getHyperlinkProducer().reset();
		return getCrossLinkHelper().onActivate();
	}

	protected boolean onTerminate(boolean successful)
	{
		return getCrossLinkHelper().onTerminate(successful);
	}

	public boolean isEnabled()
	{
		return isValidSelection(getCurrentSelections()) && super.isEnabled();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public void populateCtxMenu(@NotNull ActionContainer container, @NotNull SelectSet selections)
	{
		if (isValidSelection(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	boolean isValidSelection(@NotNull SelectSet selections)
	{
		return getOperand(selections) != null;
	}

	@Nullable IRepresentedObject getOperand(@NotNull SelectSet selections)
	{
		return getHyperlinkProducer().getOperand(selections);
	}

	public String getActionUIClass()
	{
		return CrossLinkActionUI.class.getName();
	}

	@Nullable public Cursor getCursor()
	{
		return null;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.StatusBar.text");
	}

	@NotNull @Override public ICapletController getController()
	{
		// this returns the controller that is active in the current view.
		return CAFUtils.getInstance().getActiveCapletController();
	}

	@NotNull private CrossLinkHelper getCrossLinkHelper()
	{
		// lazily create the cross link helper
		if (m_crossLinkHelper == null) {
			Model model = (Model) getCapletModel();

			IDesign design = model.getDesign();
			IProject project = design.getProject();
			m_crossLinkHelper = new CrossLinkHelper(project, getHyperlinkProducer())
			{
				@Override public String getViewRelatedDialogTitle()
				{
					return getHyperlinkProducer().getViewRelatedDialogTitle();
				}

				@Override public Frame getViewRelatedDialogParent()
				{
					return getHyperlinkProducer().getParentDialogFrame();
				}

				@Override public String getConfirmDialogTitle()
				{
					return (String) getActionUI().getValue(Action.NAME);
				}
			};
		}
		return m_crossLinkHelper;
	}

	@NotNull private HyperlinkProducer getHyperlinkProducer()
	{
		// lazily create the hyperlink producer
		if (m_hyperlinkProducer == null) {
			Model model = (Model) getCapletModel();

			IDesign design = model.getDesign();
			IProject project = design.getProject();
			ISchemDiagram diagram = model.getDiagram();
			assert project != null;
			m_hyperlinkProducer = new HyperlinkProducer(project, design, diagram)
			{
				@Override protected SelectSet getCurrentSelections()
				{
					return CrossLinkAction.this.getCurrentSelections();
				}
			};
		}

		return m_hyperlinkProducer;
	}
}
