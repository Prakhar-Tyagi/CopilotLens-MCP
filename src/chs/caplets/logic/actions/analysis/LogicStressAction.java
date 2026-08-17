/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.analysis.IAnalysisBackAnnoProcessor;
import chs.analysis.IAnalysisDesignProvider;
import chs.caf.cafmain.actions.analysis.SubsystemStressAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisBackAnnoProcessor;

import java.awt.event.ActionEvent;

/**
 * @author rharring
 */
public class LogicStressAction extends SubsystemStressAction
{

	// ////////////////// //
	// Instance variables //
	// ////////////////// //

	/**
	 * The model we'return working on
	 */
	protected Model m_model;

	/**
	 * The back annotation processor
	 */
	protected IAnalysisBackAnnoProcessor m_processor;

	// //////////// //
	// Constructors //
	// //////////// //

	/**
	 * Creates a new instance of LogicStressAction
	 *
	 * @param controller, the caplet controller
	 * @param processor, the back annotation processor
	 * @param designProvider, the object that provides design and netlist references
	 */
	public LogicStressAction(ICapletController controller,
			IAnalysisBackAnnoProcessor processor,
			IAnalysisDesignProvider designProvider)
	{
		super(controller, null, processor, designProvider);
		m_model = (Model) controller.getCapletModel();
		m_processor = processor;

		// ensure back propogation is enabled
		setBackPropagateEnabled(true);
	}

	// ////////////// //
	// Action methods //
	// ////////////// //

	public IActionEnum onActivate(ActionEvent e)
	{
		setUndoableAction(false);
		return super.onActivate(e);
	}

	public boolean onTerminate(boolean successful)
	{
		if (m_processor != null) {
			//m_processor.applyEdits( success ) ;

			if (m_processor instanceof LogicAnalysisBackAnnoProcessor) {
				((LogicAnalysisBackAnnoProcessor) m_processor).applyEdits(successful);
			}
		}
		return super.onTerminate(successful);
	}

	public String getActionUIClass()
	{
		return LogicStressActionUI.class.getName();
	}
}
   
