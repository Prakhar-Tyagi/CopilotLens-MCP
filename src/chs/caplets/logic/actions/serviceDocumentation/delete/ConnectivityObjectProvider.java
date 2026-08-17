package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageConnectivityAction;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IMultipleConnectivityRef;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

public class ConnectivityObjectProvider
{

	@Nullable IDiagramObject getConnectivityRefDiagramObject(IUIDObject obj)
	{
		IDiagramObject originalObject = null;
		if (obj instanceof IDiagramObject) {
			IDiagramObject diagramObject = (IDiagramObject) obj;
			originalObject = diagramObject;
			if (diagramObject instanceof IConnectivityRef || diagramObject instanceof IMultipleConnectivityRef) {
				return diagramObject;
			}
			while (diagramObject != null) {
				IDiagramObject parent = diagramObject.getParent();
				if (parent instanceof IConnectivityRef || diagramObject instanceof IMultipleConnectivityRef) {
					return parent;
				}
				diagramObject = parent;
			}
		}
		return originalObject;
	}

	@NotNull Collection<ILogicObject> getConnectivity(@NotNull IDiagramObject diagramObject)
	{
		Collection<ILogicObject> allConnectivities = new LinkedHashSet<>();
		IConnectivityRef connectivityRef = FetchOffPageConnectivityAction.getConnectivityRef(diagramObject);
		if (connectivityRef != null) {
			ILogicObject connectivity = connectivityRef.getConnectivity();
			if (connectivity != null) {
				allConnectivities.add(connectivity);
			}
		}
		else {
			IMultipleConnectivityRef multipleConnectivityRef =
					FetchOffPageConnectivityAction.getMultipleConnectivityRef(diagramObject);
			if (multipleConnectivityRef != null) {
				Collection<? extends ILogicObject> allConnectivity = multipleConnectivityRef.getAllConnectivity();
				allConnectivities.addAll(allConnectivity);
			}
		}
		return allConnectivities;
	}
}
