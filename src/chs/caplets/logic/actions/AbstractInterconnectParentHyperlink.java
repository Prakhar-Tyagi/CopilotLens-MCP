/*
 * Copyright 2005-2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.helpers.GfxViewHelper;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ICHSIterator;
import chs.common.IUID;
import chs.system.FactoryMgr;

import java.util.Collections;

public abstract class AbstractInterconnectParentHyperlink extends AbstractInterconnectHyperlink
{

	protected double m_confidence;
	protected IUID m_sourceUID;

	protected AbstractInterconnectParentHyperlink(ISchemDiagram currentDiagram, IDesign design)
	{
		super(currentDiagram, design);
	}

	protected void init(ISchemDiagram diagram)
	{
		if (diagram != null) {
			m_linkDiagram = diagram;
			if (!isFullyLoaded(m_linkDiagram.getUID()) || getObjectUID() == null) {
				// It's probably on the requested diagram
				m_confidence = 0.7;
			}
			else {
				IDiagramObjectIterator reps = m_linkDiagram.getRepresentations(getObjectUID());
				if (reps != null && reps.getSize() > 0) {
					// It's definitely on the requested diagram
					m_confidence = 1.0;
				}
				else {
					// It's definitely not on the requested diagram
					m_confidence = 0.0;
				}
			}
		}
		else {
			int uncheckedDiagramCount;
			if (getObjectUID() == null) {
				uncheckedDiagramCount = m_linkDesign.getNumDiagrams();
			}
			else {
				uncheckedDiagramCount = 0;
				IInterconnectSourceInfo isi = m_currentDesign.getInterconnectSourceInfo();
				for (ICHSIterator<ISchemDiagram> diagIter = ((ILogicDesign)m_linkDesign).getDiagrams(); diagIter.hasNext();) {
					ISchemDiagram anyDiagram = diagIter.getNext();

					// If we're looking for any diagram we can find that fits, then the sourceDiagram has already been looked at.
					if (anyDiagram.getUID().isEquiv(isi.getSourceDiagramUID())) {
						continue;
					}

					// Only look at open diagrams
					if (!isFullyLoaded(anyDiagram.getUID())) {
						uncheckedDiagramCount++;
					}
					else {
						IDiagramObjectIterator reps = anyDiagram.getRepresentations(getObjectUID());
						if (reps != null && reps.getSize() > 0) {
							m_linkDiagram = anyDiagram;
							break;
						}
					}
				}
			}
			if (m_linkDiagram != null) {
				// We found a diagram with representations - guaranteed success.
				m_confidence = 1.0;
			}
			else if (uncheckedDiagramCount == 0) {
				// We looked in every diagram and none had a representation - guaranteed failure.
				m_confidence = 0.0;
			}
			else {
				// There are unopened diagrams that we didn't search.
				m_confidence = 0.49;
			}
		}
	}

	protected IUID getObjectUID()
	{
		return m_sourceUID;
	}

	public double getConfidence()
	{
		return m_confidence;
	}

	protected ISchemDiagram getDiagram()
	{
		if (m_linkDiagram == null) {
			m_linkDiagram = GfxViewHelper.findAndLoadDiagramWithRepresentation(getDesign(), getObjectUID()).getDiagram();
		}
		return m_linkDiagram;
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		if (getDiagram() != null) {
			return getDiagram().getRepresentations(getObjectUID());
		}
		else {
			return FactoryMgr.getDrawPlusFactory().createDiagramObjectIterator(Collections.EMPTY_LIST);
		}
	}
}
