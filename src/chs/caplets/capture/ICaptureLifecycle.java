/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture;

import chs.caf.caplet.ICapletModel;
import chs.caplets.logic.Model;
import chs.cof.logical.IDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolder;

import java.util.List;

/**
 * @author Matt Boyd
 */

public interface ICaptureLifecycle
{

	/**
	 * Closes a design and its diagrams.  Goes through each diagram to see if there have been any changes.
	 *
	 * @param project
	 * @param design
	 *
	 * @return <code>true</code> if design was closed; <code>false</code> otherwise.
	 */
	boolean closeDesign(IProject project, IDesign design);

	/**
	 * Opens a diagram.  Create a model and a view for the diagram.
	 *
	 * @param project
	 * @param design
	 * @param diagram
	 *
	 * @return The new model.
	 */
	Model openDiagram(IProject project, ISchemDiagram diagram);

	/**
	 * Creates a design and diagram. If there is a folder, the design is created in the folder
	 *
	 * @param project
	 *
	 * @return
	 */
	ISchemDiagram createDiagram(IProject project, IFolder folder);

	/**
	 * Creates a diagram.  Also creates a design if <i>design</i> is <code>null</code>. If there is a folder, the design is
	 * created in the folder
	 *
	 * @param project
	 * @param design
	 *
	 * @return
	 */
	ISchemDiagram createDiagram(IProject project, IDesign design, IFolder folder);

	/**
	 * Creates a diagram with the given name without displaying the new diagram dialog.  If the name is already used then
	 * an exception is thrown.
	 *
	 * @param project
	 * @param design
	 * @param name
	 *
	 * @return
	 *
	 * @throws RuntimeException thrown if name is already used.
	 */
	ISchemDiagram createDiagramWithName(IProject project, IDesign design, String name);

	boolean delete(List context);

	boolean deleteDesign(IProject project, IDesign design);

	boolean deleteDiagram(IProject project, ISchemDiagram diagram);

	ICapletModel getModel(ISchemDiagram diagram);
}