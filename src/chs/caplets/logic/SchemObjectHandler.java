package chs.caplets.logic;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.ISupplementaryObject;
import chs.cof.drawplus.IReadOnlySupplementaryObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.common.IUIDObject;
import chs.common.styles.IStyleableObject;

import java.util.HashSet;
import java.util.Set;

public class SchemObjectHandler
{

	private Set<ILogicObject> fetchedObjects = new HashSet<>();

	public SchemObjectHandler()
	{

	}

	public void populateOldSchemAttributeMap(Set<IUIDObject> originalSchematics)
	{
		for (IUIDObject iuidObject : originalSchematics) {
			if (iuidObject instanceof ISupplementaryObject && iuidObject instanceof IConnectivityRef) {
				ISupplementaryObject diagramObject = (ISupplementaryObject) iuidObject;
				ILogicObject logicObject = ((IConnectivityRef) diagramObject).getConnectivity();
				if (((IReadOnlySupplementaryObject) iuidObject).isSupplementary()) {
					fetchedObjects.add(logicObject);
				}
			}
		}
	}

	public void updateSchemObject(IDiagramObject diagramObject)
	{
		if (fetchedObjects != null && !fetchedObjects.isEmpty()) {
			if (diagramObject instanceof IConnectivityRef && diagramObject instanceof ISupplementaryObject) {
				ILogicObject connectivity = ((IConnectivityRef) diagramObject).getConnectivity();
				if (fetchedObjects.contains(connectivity)) {
					((ISupplementaryObject) diagramObject).markAsSupplementary();
					if (diagramObject instanceof IStyleableObject) {
						((IStyleableObject) diagramObject).applyStyle();
					}
				}
			}
		}
	}
}
