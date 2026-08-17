package chs.caplets.logic.actions;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.ModularSchemPinListInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA. User: brangan Date: 1/9/14 Time: 12:34 PM To change this template use File | Settings |
 * File Templates.
 */
public class AddPinActionModel
{

	@Nullable private IUID m_reference;
	@NotNull private Map<IUID, NewPinRegistry> m_pinLists;

	public AddPinActionModel(@Nullable IPinList pinList)
	{
		//consider modular connector hierarchy for add pin.
		m_pinLists = new HashMap<>();
		if (pinList != null) {
			ModularSchemPinListInfo modularSchemPinListInfo = new ModularSchemPinListInfo(pinList);
			//this must be the selected pinlist. not the aero modular root.
			//this is being used in pin selection dialog to show pins of
			//only children and this connector not the parent.
			m_reference = pinList.getUID();
			for (IPinList candidate : modularSchemPinListInfo.getCandidates()) {
				m_pinLists.put(candidate.getUID(), new NewPinRegistry());
			}
		}
	}

	@Nullable public IPinList getReference()
	{
		return UIDMgr.getObjectOfType(m_reference, IPinList.class);
	}

	@NotNull public Set<IPinList> getPinLists()
	{
		Set<IPinList> pinLists = new HashSet<>();
		for (IUID key : m_pinLists.keySet()) {
			IPinList pinList = UIDMgr.getObjectOfType(key, IPinList.class);
			if (pinList != null) {
				pinLists.add(pinList);
			}
		}
		return Collections.unmodifiableSet(pinLists);
	}

	@NotNull public Set<IPinList> getMatePinLists(@Nullable IPinList pinList)
	{
		if (pinList != null) {
			NewPinRegistry newPinRegistry = m_pinLists.get(pinList.getUID());
			if (newPinRegistry != null) {
				return newPinRegistry.getMatePinLists();
			}
		}
		return Collections.emptySet();
	}

	public void reBuild()
	{
		IPinList reference = UIDMgr.getObjectOfType(m_reference, IPinList.class);
		if (reference != null) {
			for (IPinList candidate : new ModularSchemPinListInfo(reference).getCandidates()) {
				m_pinLists.computeIfAbsent(candidate.getUID(), p -> new NewPinRegistry());
			}
		}
	}

	public void addMatePinList(@NotNull IPinList pinList, @Nullable IPinList matePinList)
	{
		NewPinRegistry newPinRegistry = m_pinLists.get(pinList.getUID());
		if (newPinRegistry != null && matePinList != null) {
			newPinRegistry.addMatePinList(matePinList);
		}
	}

	public void cleanUp()
	{
		m_reference = null;
		m_pinLists.clear();
	}

	@Nullable private static IPinList getPinList(@NotNull IAbstractSchemPin schemPin)
	{
		IDiagramObject parent = schemPin.getParent();
		if (parent instanceof IPinList) {
			return (IPinList) parent;
		}
		return null;
	}

	public void registerNewPin(@NotNull IAbstractSchemPin schematicPin, @Nullable IAbstractSchemPin mateSchematicPin)
	{
		IPinList pinList = getPinList(schematicPin);
		if (pinList != null) {
			NewPinRegistry newPinRegistry = m_pinLists.get(pinList.getUID());
			if (newPinRegistry != null) {
				newPinRegistry.addPin(schematicPin, mateSchematicPin);
			}
		}
	}

	@Nullable public IAbstractSchemPin getMatedPin(@NotNull IAbstractSchemPin schemPin)
	{
		IPinList pinList = getPinList(schemPin);
		if (pinList != null) {
			NewPinRegistry newPinRegistry = m_pinLists.get(pinList.getUID());
			if (newPinRegistry != null) {
				return newPinRegistry.getMatedPin(schemPin);
			}
		}
		return null;
	}

	@Override public String toString()
	{
		StringBuilder builder = new StringBuilder();
		boolean singleEntry = m_pinLists.size() == 1;
		for (Map.Entry<IUID, NewPinRegistry> entry : m_pinLists.entrySet()) {
			if (!singleEntry) {
				builder.append("{");
			}

			IPinList pinList = UIDMgr.getObjectOfType(entry.getKey(), IPinList.class);
			if (pinList != null) {
				builder.append(pinList.getConnectivity().getName());
			}
			String mateDesc = entry.getValue().toString();
			if (!StringUtils.isBlank(mateDesc)) {
				builder.append(" -- ").append(mateDesc);
			}

			if (!singleEntry) {
				builder.append("}");
			}
		}
		return builder.toString();
	}

	@NotNull public Set<IAbstractSchemPin> getPins(@NotNull IPinList pinList)
	{
		NewPinRegistry registry = m_pinLists.get(pinList.getUID());
		return registry != null ? registry.getPins() : Collections.emptySet();
	}

	private static class NewPinRegistry
	{

		private Set<MateRegistry> m_mates = new HashSet<>();
		private Set<IUID> m_unmatedPins = new LinkedHashSet<>();

		private NewPinRegistry()
		{
		}

		public void addPin(@NotNull IAbstractSchemPin schematicPin, @Nullable IAbstractSchemPin mateSchematicPin)
		{
			boolean added = false;
			if (mateSchematicPin != null) {
				IPinList matePinList = getPinList(mateSchematicPin);
				if (matePinList != null) {
					for (MateRegistry value : m_mates) {
						if (matePinList.getUID() == value.getMatePinList()) {
							value.addPin(schematicPin, mateSchematicPin);
							added = true;
							break;
						}
					}
				}
			}
			if (!added) {
				m_unmatedPins.add(schematicPin.getUID());
			}
		}

		@Nullable public IAbstractSchemPin getMatedPin(@NotNull IAbstractSchemPin schemPin)
		{
			for (MateRegistry value : m_mates) {
				IAbstractSchemPin matedPin = value.getMatedPin(schemPin);
				if (matedPin != null) {
					return matedPin;
				}
			}
			assert m_unmatedPins.contains(schemPin.getUID());
			return null;
		}

		@NotNull Set<IAbstractSchemPin> getPins()
		{
			Set<IAbstractSchemPin> schemPins = new HashSet<>();
			for (IUID unmatedPin : m_unmatedPins) {
				IAbstractSchemPin pin = UIDMgr.getObjectOfType(unmatedPin, IAbstractSchemPin.class);
				if (pin != null) {
					schemPins.add(pin);
				}
			}
			for (MateRegistry value : m_mates) {
				schemPins.addAll(value.getPins());
			}
			return Collections.unmodifiableSet(schemPins);
		}

		@NotNull public Set<IPinList> getMatePinLists()
		{
			Set<IPinList> matedPinLists = new HashSet<>();
			for (MateRegistry value : m_mates) {
				IPinList matePL = UIDMgr.getObjectOfType(value.getMatePinList(), IPinList.class);
				if (matePL != null) {
					matedPinLists.add(matePL);
				}
			}
			return Collections.unmodifiableSet(matedPinLists);
		}

		public void addMatePinList(@NotNull IPinList matePinList)
		{
			boolean alreadyExists = false;
			for (MateRegistry value : m_mates) {
				if (matePinList.getUID() == value.getMatePinList()) {
					alreadyExists = true;
					break;
				}
			}
			if (!alreadyExists) {
				m_mates.add(new MateRegistry(matePinList));
			}
		}

		@Override public String toString()
		{
			StringBuilder builder = new StringBuilder();
			boolean singleEntry = m_mates.size() == 1;
			for (MateRegistry entry : m_mates) {
				if (!singleEntry) {
					builder.append("[");
				}

				IPinList matePL = UIDMgr.getObjectOfType(entry.getMatePinList(), IPinList.class);
				if (matePL != null) {
					builder.append(matePL.getConnectivity().getName());
				}

				if (!singleEntry) {
					builder.append("]");
				}
			}
			return builder.toString();
		}
	}

	private static class MateRegistry
	{

		@NotNull private IUID m_matePinList;
		@NotNull private Map<IUID, IUID> m_registeredPins = new HashMap<>();

		private MateRegistry(@NotNull IPinList matePinList)
		{
			m_matePinList = matePinList.getUID();
		}

		@NotNull public IUID getMatePinList()
		{
			return m_matePinList;
		}

		@Nullable public IAbstractSchemPin getMatedPin(@NotNull IAbstractSchemPin schemPin)
		{
			return UIDMgr.getObjectOfType(m_registeredPins.get(schemPin.getUID()), IAbstractSchemPin.class);
		}

		@NotNull public Set<IAbstractSchemPin> getPins()
		{
			Set<IAbstractSchemPin> pins = new HashSet<>();
			for (IUID key : m_registeredPins.keySet()) {
				IAbstractSchemPin pin = UIDMgr.getObjectOfType(key, IAbstractSchemPin.class);
				if (pin != null) {
					pins.add(pin);
				}
			}
			return Collections.unmodifiableSet(pins);
		}

		public void addPin(@NotNull IAbstractSchemPin schematicPin, @NotNull IAbstractSchemPin mateSchematicPin)
		{
			m_registeredPins.put(schematicPin.getUID(), mateSchematicPin.getUID());
		}
	}
}
