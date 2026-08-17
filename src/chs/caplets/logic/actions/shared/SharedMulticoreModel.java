package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.ui.MCNode;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.ValidityChangeEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

public class SharedMulticoreModel implements IPropertyValidityListener
{

	@Nullable private ISharedMulticore m_sharedMulticore;
	private IMulticore m_multicore;
	private List<ChangeListener> sharedChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> nameChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> nodeChangeListeners = new ArrayList<ChangeListener>();
	private List<IPropertyValidityListener> propertyValidityChangeListeners =
			new ArrayList<IPropertyValidityListener>();
	private MCNode rootMCNode;
	private boolean m_sharedMulticoreNameGenerated;
	@Nullable private String sharedMulticoreName;
	@Nullable private String shareMulticoreRevision;

	public SharedMulticoreModel(IMulticore multicore, @Nullable ISharedMulticore sharedMulticore)
	{
		m_multicore = multicore;
		m_sharedMulticore = sharedMulticore;
	}

	@Override public void validityChanged(ValidityChangeEvent evt)
	{
		for (IPropertyValidityListener propertyValidityChangeListener : propertyValidityChangeListeners) {
			propertyValidityChangeListener.validityChanged(evt);
		}
	}

	@Override public void invalidReasonChanged(IProperty property)
	{

	}

	public IMulticore getMulticore()
	{
		return m_multicore;
	}

	@Nullable public ISharedMulticore getSharedMulticore()
	{
		return m_sharedMulticore;
	}

	public MCNode getRootProxy()
	{
		return rootMCNode;
	}

	public void setRootMCNode(MCNode mcNode)
	{
		rootMCNode = mcNode;
	}

	public void setSharedMulticore(@Nullable ISharedMulticore sharedMulticore)
	{
		if (m_sharedMulticore != sharedMulticore) {
			m_sharedMulticore = sharedMulticore;
			fireSharedChangeEvent(new ChangeEvent(this));
		}
	}

	private void fireSharedChangeEvent(ChangeEvent e)
	{
		for (ChangeListener sharedChangeListener : sharedChangeListeners) {
			sharedChangeListener.stateChanged(e);
		}
	}

	private void fireNameChangeEvent(ChangeEvent e)
	{
		for (ChangeListener nameChangeListener : nameChangeListeners) {
			nameChangeListener.stateChanged(e);
		}
	}

	public void addSharedChangeListener(ChangeListener listener)
	{
		sharedChangeListeners.add(listener);
	}

	public void addNameChangeListener(ChangeListener listener)
	{
		nameChangeListeners.add(listener);
	}

	public void addPropertyValidityChangeListener(IPropertyValidityListener listener)
	{
		propertyValidityChangeListeners.add(listener);
	}

	public void addNodeChangeListener(ChangeListener listener)
	{
		nodeChangeListeners.add(listener);
	}

	public void fireNodeChangeEvent(ChangeEvent e)
	{
		for (ChangeListener nodeChangeListener : nodeChangeListeners) {
			nodeChangeListener.stateChanged(e);
		}
	}

	public void addChangeListener(ChangeListener listener)
	{
		addSharedChangeListener(listener);
		addNameChangeListener(listener);
		addNodeChangeListener(listener);
	}

	public void setSharedMulticoreNameGenerated(boolean value)
	{
		m_sharedMulticoreNameGenerated = value;
	}

	public void setSharedMulticoreName(@Nullable String mcName)
	{
		sharedMulticoreName = mcName;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	public void setSharedMulticoreRevision(@Nullable String mcRevision)
	{
		shareMulticoreRevision = mcRevision;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	@Nullable public String getSharedMulticoreName()
	{
		return sharedMulticoreName;
	}

	@Nullable public String getSharedMulticoreRevision()
	{
		return shareMulticoreRevision;
	}

	public boolean isSharedMulticoreNameGenerated()
	{
		return m_sharedMulticoreNameGenerated;
	}
}
