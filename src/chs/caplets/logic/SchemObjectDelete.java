/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.cof.logical.ISchemObjectDelete;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class SchemObjectDelete implements ISchemObjectDelete
{

	@NotNull final ISchemDiagram mDiagram;

	public SchemObjectDelete(@NotNull ISchemDiagram diagram)
	{
		mDiagram = diagram;
	}

	@Override public void delete(@NotNull Collection<? extends IUIDObject> objectsToDelete, boolean deleteConnectivity)
	{
		DeleteHelper.getInstance().delete(mDiagram, objectsToDelete, deleteConnectivity);
	}
}
