/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.shared.properties;

import chs.cof.logical.cable.IInterconnectMember;
import chs.common.IUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MemberProxy
{

	private static Map existingMap = new HashMap();
	IUID m_uid;
	List m_realObjects;
	String m_partNum;
	IUID m_lref;
	int m_type;

	static MemberProxy create(IInterconnectMember im)
	{
		return create(im, im.getLibraryRef(), im.getPartNumber(), im.getPartClass());
	}

	static MemberProxy create(IUID lref, String partNum, int type)
	{
		return create(null, lref, partNum, type);
	}

	static MemberProxy create(IInterconnectMember robj, IUID lref, String partNum, int type)
	{
		String key = getMapHash(partNum, type);
		MemberProxy mp = (MemberProxy) existingMap.get(key);
		if (mp == null) {
			mp = new MemberProxy(lref, partNum, type);
			existingMap.put(key, mp);
		}

		mp.addRealObject(robj);
		return mp;
	}

	static void remove(MemberProxy mp)
	{
		String key = getMapHash(mp);
		existingMap.remove(key);
	}

	static void reset()
	{
		existingMap.clear();
	}

	private static String getMapHash(MemberProxy mp)
	{
		return mp.getPartNumber() + "-" + mp.getPartClass();
	}

	private static String getMapHash(String partNum, int type)
	{
		return partNum + "-" + type;
	}

	private MemberProxy(IUID lref, String partNum, int type)
	{
		m_realObjects = new ArrayList();
		m_lref = lref;
		m_partNum = partNum;
		m_type = type;
	}

	public String getPartNumber()
	{
		return m_partNum;
	}

	public int getPartClass()
	{
		return m_type;
	}

	public List getRealObjects()
	{
		return m_realObjects;
	}

	public void addRealObject(IInterconnectMember m)
	{
		m_realObjects.add(m);
	}

	public void resizeTo(int newSize)
	{
		int size = m_realObjects.size();
		if (newSize < 1 || newSize == size) {
		}
		else if (newSize > size) {
			for (; size < newSize; size++) {
				m_realObjects.add(null);
			}
		}
		else {
			m_realObjects = m_realObjects.subList(0, newSize);
		}
	}

	public IUID getLibraryRef()
	{
		return m_lref;
	}
}
