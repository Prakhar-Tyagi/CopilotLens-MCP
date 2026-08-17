/*
 * Copyright 2003-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ILifecycleType;
import chs.caf.caplet.helpers.FileTypeHolder;
import chs.caf.caplet.helpers.LifecycleTypeHolder;
import chs.caplets.symbol.Caplet;
import chs.cof.project.folder.INormalFolder;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IBorderLibrary;
import chs.cof.symbol.IStamp;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

public class Lifecycle extends chs.caplets.symbol.Lifecycle
{

	public Lifecycle(Caplet caplet)
	{
		super(caplet);
	}

	@Override protected void init()
	{
		//
		// Add the file types.
		//
		FileTypeHolder xml = new FileTypeHolder(ResourceMgr.getString(Lifecycle.class, "Lifecycle.BorderFile.Title"),
				"xml", "application/x-CapitalBorder-xml");
		addFileTypeForOpen(xml);
		addFileTypeForSave(xml);

		// Add the Project Types
		//
		// New Types
		addTypeForNew(getLifecycleType(IBorderLibrary.class, "Lifecycle.BorderNew."));
		addTypeForNew(getLifecycleType(INormalFolder.class, "Lifecycle.BorderNew.", IBorderLibrary.class));

		// Open Types
		addTypeForOpen(getLifecycleType(IBorder.class, "Lifecycle.BorderOpen."));

		// Delete Types
		addTypeForDelete(getLifecycleType(IBorderLibrary.class, "Lifecycle.BorderDelete."));
		addTypeForDelete(getLifecycleType(IBorder.class, "Lifecycle.BorderDelete."));

		// Edit & Rename Types
		addTypeForEdit(getLifecycleType(IBorderLibrary.class, "Lifecycle.BorderEditLibrary."));
		addTypeForRename(getLifecycleType(IBorder.class, "Lifecycle.BorderRename."));

		// Move Types
		addTypeForMove(getLifecycleType(IBorder.class, "Lifecycle.Move."));

		// Duplicate Types
		addTypeForDuplicate(getLifecycleType(IBorder.class, "Lifecycle.Duplicate."));
	}

	private ILifecycleType getLifecycleType(Class<?> actionClass, String prefix, @Nullable Class<?> ancestor)
	{
		return new LifecycleTypeHolder(actionClass,
				ResourceMgr.getString(Lifecycle.class, prefix + "Label"),
				ResourceMgr.getMnemonic(Lifecycle.class, prefix + "mnemonic"),
				ancestor,
				null);
	}

	private ILifecycleType getLifecycleType(Class<?> actionClass, String prefix)
	{
		return getLifecycleType(actionClass, prefix, null);
	}

	@Override
	protected chs.caplets.symbol.Controller createController(ICaplet caplet, IAbstractLibrary library, IStamp symbol)
	{
		return new Controller(caplet, library, symbol, getLibraryEditableStatus(library).isValid());
	}
}

