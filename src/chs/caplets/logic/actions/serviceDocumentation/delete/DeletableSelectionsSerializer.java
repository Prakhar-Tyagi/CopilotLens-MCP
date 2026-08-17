package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class DeletableSelectionsSerializer
{

	private final OutputStream m_streamForDebugging;
	private final SelectionObjects m_selections;
	private final ConnectivityObjectProvider m_connectivityObjectProvider;

	DeletableSelectionsSerializer(SelectionObjects selections, @Nullable OutputStream streamForDebugging,
			ConnectivityObjectProvider connectivityObjectProvider)
	{
		m_selections = selections;
		m_streamForDebugging = streamForDebugging;
		m_connectivityObjectProvider = connectivityObjectProvider;
	}

	void serialize()
	{
		if (m_streamForDebugging != null) {
			String deletables = toString(m_selections.getDeletables());
			String lastInstances = toString(m_selections.getLastInstances());
			String connChnages = toString(m_selections.getObjectsWhoseDeleteLeadsToConnectivityChange());
			String notFetchedObjects = toString(m_selections.getNotFetchedObjects());
			String otherReasons = toString(m_selections.getNotDeletablesDueToOtherReasons());
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(m_streamForDebugging))) {
				writer.append("DELETABLES:");
				writer.append(System.lineSeparator());
				writer.append(deletables);
				writer.append(System.lineSeparator());
				writer.append("NONFETCHED:");
				writer.append(System.lineSeparator());
				writer.append(notFetchedObjects);
				writer.append(System.lineSeparator());
				writer.append("LASTINSTANCES:");
				writer.append(System.lineSeparator());
				writer.append(lastInstances);
				writer.append(System.lineSeparator());
				writer.append("CHANGESCONN:");
				writer.append(System.lineSeparator());
				writer.append(connChnages);
				writer.append(System.lineSeparator());
				writer.append("OTHER:");
				writer.append(System.lineSeparator());
				writer.append(otherReasons);
				writer.flush();
			}
			catch (IOException ignored) {
			}
		}
	}

	private String toString(Set<? extends IUIDObject> iuidObjects)
	{
		return getSortedUIDs(iuidObjects)
				.stream()
				.collect(Collectors.joining(System.lineSeparator()));
	}

	private Set<String> getSortedUIDs(Set<? extends IUIDObject> iuidObjects)
	{
		List<String> uids =
				iuidObjects.stream().map(this::toString).collect(Collectors.toList());
		Collections.sort(uids);
		return new LinkedHashSet<>(uids);
	}

	private String toString(IUIDObject iuidObject)
	{
		IDiagramObject connectivityRefDiagramObject =
				m_connectivityObjectProvider.getConnectivityRefDiagramObject(iuidObject);
		if (connectivityRefDiagramObject == null) {
			return iuidObject.getClass().toString() + "--" + iuidObject.getUID().getString();
		}
		Collection<ILogicObject> connectivity =
				m_connectivityObjectProvider.getConnectivity(connectivityRefDiagramObject);
		return iuidObject.getClass().toString() + "--" + iuidObject.getUID().getString() + "--" +
				PublisherDeleteMessageLogger.getName(connectivity);
	}
}
