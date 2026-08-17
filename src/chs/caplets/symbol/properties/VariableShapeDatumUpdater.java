/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.properties;

import chs.cof.draw.ICompoundObject;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.symbol.IStamp;
import chs.common.IBaseDatum;
import chs.utility.Replicator;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class VariableShapeDatumUpdater
{

	private final ISymbolModel m_model;

	public VariableShapeDatumUpdater(@NotNull ISymbolModel model)
	{
		m_model = model;
	}

	public void updateRepresentations(@NotNull Collection<IDatumRepresentation> datumReps)
	{
		if (datumReps.isEmpty()) {
			return;
		}

		IStamp symbolDef = m_model.getSymbolDef();
		ICompoundObject symbolGfx = symbolDef == null ? null : symbolDef.getGfx();

		if (symbolGfx != null) {
			Replicator replicator = new Replicator(Replicator.Mode.INSTANTIATE);
			for (IDatumRepresentation datumRep : datumReps) {
				IBaseDatum datum = datumRep.getDatum();
				if (datum != null && datum.hasVariableShape()) {
					// Create new rep
					IDatumRepresentation newDatumRep = replicator.replicateDatumRepresentation(1,
							datum, datumRep);

					// Delete the old one
					symbolGfx.removeObject(datumRep);
					datumRep.delete();

					// Add the new one to the parent graphics
					symbolGfx.addObject(newDatumRep);
					newDatumRep.generateRepresentation();
				}
			}
		}
	}
}
