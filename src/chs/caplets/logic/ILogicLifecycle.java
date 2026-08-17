/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ILogicCapletLifecycle;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import org.jetbrains.annotations.Nullable;

/**
 * @author Matt Boyd
 */
public interface ILogicLifecycle extends ILogicCapletLifecycle
{

	/**
	 * Opens a diagram.  Create a model and a view for the diagram.
	 *
	 * @param project the associated project
	 * @param diagram the doiagram to open
	 *
	 * @return The new model.
	 */
	ICapletModel openDiagram(IProject project, ISchemDiagram diagram);

	/**
	 * Creates a diagram with the given name without displaying the new diagram dialog.  If the name is already used
	 * then an exception is thrown.
	 *
	 * @param project the associated project
	 * @param design the associated design
	 * @param name the diagram name
	 *
	 * @return ISchemDiagram
	 *
	 * @throws RuntimeException thrown if name is already used.
	 */
	ISchemDiagram createDiagramWithName(ILogicDesign design, String name);

	@Nullable ICapletModel getModel(ISchemDiagram diagram);
}