package chs.caplets.logic.actions.ui;

import chs.cof.library.ILibrariedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.List;
import java.util.StringJoiner;

public abstract class AbstractMCProxyTree<T extends IUIDObject> extends DefaultMutableTreeNode
{

	protected String m_name;
	protected String m_nameForCompare;
	@Nullable protected T m_ref;
	@Nullable private IUID libraryRef;
	private static MCNodeComparator m_mcNodeComparator = new MCNodeComparator();


	protected AbstractMCProxyTree(@Nullable String name, @Nullable T ref)
	{
		init(name, ref);
	}

	public void init(@Nullable String name, @Nullable T ref)
	{
		m_name = getDefaultName(name, ref);
		m_ref = ref;
		m_nameForCompare = m_name;
		if (m_ref instanceof ILibrariedObject) {
			libraryRef = ((ILibrariedObject) m_ref).getLibraryRef();
		}
	}

	@Override public void insert(MutableTreeNode newChild, int childIndex)
	{
		super.insert(newChild, childIndex);
		children.sort(m_mcNodeComparator);
	}

	@NotNull public abstract String getConductorRole();

	protected String getDefaultName(@Nullable String name, @Nullable T ref)
	{
		String defaultName = "<unknown>";

		if (name != null) {
			defaultName = name;
		}
		return defaultName;
	}

	@Nullable public Icon getIcon(){
		if(m_ref == null){
			return null;
		}
		return IconUtils.getIcon(m_ref);
	}

	@Nullable public abstract String getSheathType();

	public boolean hasRef()
	{
		return (m_ref != null);
	}

	public String getName()
	{
		return m_name;
	}

	@Nullable public T getRef()
	{
		return m_ref;
	}

	public String toString()
	{
		return getName();
	}

	public abstract List<String> getAttributes();

	public abstract boolean isAssigned();

	public abstract boolean isOverbraid();

	@Nullable public IUID getLibraryRef()
	{
		return libraryRef;
	}

	public boolean hasLibraryRef()
	{
		return libraryRef != null;
	}

	@Nullable public String getAttributesListAsString()
	{
		if (!getAttributes().isEmpty()) {
			StringJoiner joiner = new StringJoiner(",", " (", ")");
			for (String attribute : getAttributes()) {
				if (attribute != null && !attribute.isEmpty()) {
					joiner.add(attribute);
				}
			}
			return joiner.toString();
		}
		return null;
	}
}