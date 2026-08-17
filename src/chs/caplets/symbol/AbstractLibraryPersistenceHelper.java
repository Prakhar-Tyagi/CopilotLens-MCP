/*
 * Copyright 2015-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IPSMStamp;
import chs.cof.symbol.IStamp;
import chs.cog.ICOGLockable;
import chs.cog.IPrivilegedCOGManaged;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.validation.ValidationHelper;
import chs.system.UIDMgr;
import chs.utility.helpers.UtilsHelper;
import chs.utility.persist.PersistPayload;
import chs.utility.persist.ServerUpdateHelper;
import chs.utility.persist.SymbolLibraryStorageHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class AbstractLibraryPersistenceHelper
{

	static boolean saveSymbols(IAbstractLibrary lib, Collection<IPSMStamp> symbols)
	{
		boolean result = updateSymbolLibraryWithNewSymbols(lib, symbols);
		for (IPSMStamp aSymbol : symbols) {
			ValidationHelper.validateBeforeSave(aSymbol);
			aSymbol.setServerTimeModified(UtilsHelper.getServerTime());

			aSymbol.save();
		}
		return result;
	}

	static boolean unlockSymbols(Collection<IUID> symbols)
	{
		for (IUID aSymbolUID : symbols) {
			IUIDObject aSymbol = UIDMgr.getObject(aSymbolUID);
			if (aSymbol instanceof ICOGLockable) {
				((ICOGLockable) aSymbol).unlock();
			}
		}
		return true;
	}

	private static boolean updateSymbolLibraryWithNewSymbols(IAbstractLibrary lib,
			Collection<IPSMStamp> symbolsModified)
	{
		Set<IStamp> newObjectsToSave = new HashSet<IStamp>(symbolsModified.size());
		for (IStamp aStamp : symbolsModified) {
			if (aStamp instanceof IPrivilegedCOGManaged) {
				IPrivilegedCOGManaged cogManagedStamp = (IPrivilegedCOGManaged) aStamp;
				if (cogManagedStamp.getPersistenceState().isNew()) {
					newObjectsToSave.add(aStamp);
				}
			}
		}
		if (!newObjectsToSave.isEmpty()) {
			PersistPayload payload = SymbolLibraryStorageHelper.saveSymbolLibraryRequest(lib, newObjectsToSave);
			return ServerUpdateHelper.updateServerData(payload);
		}
		return true;
	}
}