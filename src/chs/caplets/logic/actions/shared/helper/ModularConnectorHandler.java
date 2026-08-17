package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.SelectSharedPanel;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.common.IReadOnlyNamedObject;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.ValidateDuplicateSharedNames;
import chs.utility.helpers.IPinListShareHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModularConnectorHandler extends BaseSharePinlistHandler
{

	@NotNull private IConnector m_Connector;
	@NotNull private Supplier<Collection<IConnectorNode>> mConnectorNodesSupplier;

	public ModularConnectorHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@NotNull IConnector connector, @NotNull Supplier<Collection<IConnectorNode>> nodesSupplier,
			@Nullable IShareMessageContextReporter reporter, boolean isBulkShare)
	{
		super(model, design, reporter, isBulkShare);
		mConnectorNodesSupplier = nodesSupplier;
		m_Connector = connector;
	}

	public ModularConnectorHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@NotNull IConnector connector, @NotNull Supplier<Collection<IConnectorNode>> nodesSupplier)
	{
		this(model, design, connector, nodesSupplier, null, false);
	}

	@NotNull public List<IConnectorNode> createChildrenNodes(@NotNull IConnector connector)
	{
		final List<IConnectorNode> childern = new ArrayList<IConnectorNode>();
		for (IInternalPosition position : connector.getPositions()) {
			for (IInternalPositionedObject obj : position.getAssociatedObjects()) {
				if (obj instanceof IConnector) {
					childern.add(createNode((IConnector) obj));
				}
			}
		}
		return childern;
	}

	/**
	 * When a node is edited, 1. update node's valid status 2. update the model with this new name 3. now, update the
	 * valid status of rest of the tree nodes(other node's which were invalid earlier might now become valid etc) 4.
	 * Finally, set the tree's validity in the model
	 *
	 * @param node - node that is edited
	 * @param newName - value entered in the node
	 */
	public void onConnectorNameChange(@NotNull IConnectorNode node, String newName)
	{
		//update the model with new name
		putModularConnectorToSharedNamesMap(node.getConnector(), newName);
		//check if the new name is valid & update node's validity
		node.validate(newName);
		//this inturn may change the validity of other nodes. Hence, validate & update the remaining tree nodes
		//If they change their validity status, nodeChangedEvent will trigger & the node is repainted
		validateModularHierarchy(node);
		//Now update the model if tree is valid
		setModularConnectorTreeValidity(getModularHierarchyValidity());
	}

	public String getSharedName(@NotNull IConnectorNode cNode)
	{
		return getModularConnectorToSharedNamesMap().get(cNode.getConnector());
	}

	public Boolean getSharedNameGenerated(@NotNull IConnectorNode cNode)
	{
		return getModularConnectorToSharedNameGeneratedMap().get(cNode.getConnector());
	}

	public void setSharedName(@NotNull IConnectorNode cNode, @NotNull String name)
	{
		putModularConnectorToSharedNamesMap(cNode.getConnector(), name);
	}

	public void setSharedNameGenerated(@NotNull IConnectorNode cNode, @NotNull Boolean isGenerated)
	{
		putModularConnectorToSharedNameGeneratedMap(cNode.getConnector(), isGenerated);
	}

	public void onConnectorNodeAdd(@NotNull IConnectorNode cNode)
	{
		IConnector connector = cNode.getConnector();
		putModularConnectorToSharedNamesMap(connector, connector.getName());
		putModularConnectorToSharedNameGeneratedMap(connector,
				IPinListShareHelper.generatedDefaultStateInteractive(connector));
		validateInExistingSharedObjectsAndUpdateNodeValidity(cNode);
		validateInExistingNonSharedTreeAndUpdateNodeValidity(cNode);
	}

	@NotNull public IConnectorNode createRootNode()
	{
		final IConnector connector = m_Connector;
		IConnectorNode rNode = createNode(connector);
		putModularConnectorToSharedNamesMap(connector, connector.getName());
		putModularConnectorToSharedNameGeneratedMap(connector,
				IPinListShareHelper.generatedDefaultStateInteractive(connector));
		validateInExistingSharedObjectsAndUpdateNodeValidity(rNode);
		return rNode;
	}

	private void validateModularHierarchy(@Nullable IConnectorNode connectorNode)
	{
		validateModularHierarchy(connectorNode, false, null);
	}

	private void validateModularHierarchy(@Nullable IConnectorNode connectorNode, boolean reportError,
			@Nullable Function<ModularConnectorShareErrors, String> nodeErrorToMessageConverter)
	{
		for (IConnectorNode cNode : mConnectorNodesSupplier.get()) {
			if (cNode != connectorNode) {
				cNode.validate(cNode.toString());
			}
			if (reportError && !cNode.isValid()) {
				reportError(cNode.getMessage(nodeErrorToMessageConverter));
			}
		}
	}

	public void evaluateModularHierarchyValidity(@Nullable
			Function<ModularConnectorShareErrors, String> nodeErrorToMessageConverter)
	{
		validateModularHierarchy(null, true, nodeErrorToMessageConverter);
		setModularConnectorTreeValidity(getModularHierarchyValidity());
	}

	/**
	 * Get all the nodes & if all are valid, return true;  Else return false if atleast one node is invalid
	 *
	 * @return - true if all nodes are valid; false otherwise
	 */
	private boolean getModularHierarchyValidity()
	{
		for (IConnectorNode cNode : mConnectorNodesSupplier.get()) {
			if (!cNode.isValid()) {
				return false;
			}
		}
		return true;
	}

	@NotNull public IConnector getRootConnector()
	{
		return m_Connector;
	}

	private IConnectorNode createNode(@NotNull IConnector connector)
	{
		return new ConnectorNode(connector);
	}

	private void validateInExistingSharedObjectsAndUpdateNodeValidity(@NotNull IConnectorNode cNode)
	{
		cNode.validateInExistingSharedObjectsAndUpdateNodeValidity();
		if (!cNode.isValid()) {
			setModularConnectorTreeValidity(false);
		}
	}

	private void validateInExistingNonSharedTreeAndUpdateNodeValidity(@NotNull IConnectorNode cNode)
	{
		cNode.validateInExistingNonSharedTreeAndUpdateNodeValidity();
		if (!cNode.isValid()) {
			setModularConnectorTreeValidity(false);
			validateModularHierarchy(cNode);
		}
	}

	public enum ModularConnectorShareErrors
	{
		DuplicateNameInModularHierarchy,
		ModularHierarchySharedNameConflict,
		ModularConnectorInvalidName,
		ModularConnectorNameTooLong
	}

	public interface IConnectorNode
	{

		void validate(String newName);

		boolean isGeneratedName();

		boolean isValid();

		@NotNull String getMessage(@Nullable Function<ModularConnectorShareErrors, String> nodeErrorToMessageConverter);

		IConnector getConnector();

		void validateInExistingSharedObjectsAndUpdateNodeValidity();

		void validateInExistingNonSharedTreeAndUpdateNodeValidity();
	}

	protected class ConnectorNode extends DefaultMutableTreeNode implements IConnectorNode
	{

		private boolean m_valid = true;
		@Nullable private ModularConnectorShareErrors validityMessageCode;

		protected ConnectorNode(@NotNull IConnector connector)
		{
			super(connector);
		}

		public IConnector getConnector()
		{
			return (IConnector) getUserObject();
		}

		@Override public String toString()
		{
			String displayName = "";
			if (getUserObject() instanceof IConnector) {
				displayName = getModularConnectorToSharedNamesMap().get(getConnector());
			}
			return displayName;
		}

		private void setValidityMessageCode(
				@Nullable ModularConnectorShareErrors validityMessageCode)
		{
			this.validityMessageCode = validityMessageCode;
		}

		@NotNull public String getMessage(
				@Nullable Function<ModularConnectorShareErrors, String> nodeErrorToMessageConverter)
		{
			if (validityMessageCode == null) {
				return StringUtils.EMPTY_STRING;
			}
			final String message =
					nodeErrorToMessageConverter != null ? nodeErrorToMessageConverter.apply(validityMessageCode) :
							defaultMessageSupplier(validityMessageCode);
			return message != null ? message : StringUtils.EMPTY_STRING;
		}

		@Nullable String defaultMessageSupplier(@NotNull ModularConnectorShareErrors errorEnum)
		{
			switch (errorEnum) {
				case DuplicateNameInModularHierarchy:
					return ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.nameused.text");
				case ModularHierarchySharedNameConflict:
					return ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.nameused.text");
				case ModularConnectorInvalidName:
					return ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.InvalidName.text");
				case ModularConnectorNameTooLong:
					return ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.nametoolong.text",
							String.valueOf(CHSConstants.MAX_NAME_LENGTH));
			}
			return null;
		}

		public void validateInExistingSharedObjectsAndUpdateNodeValidity()
		{
			if (sharedObjExistsWithSameName(toString(), getConnector())) {
				m_valid = false;
				setValidityMessageCode(ModularConnectorShareErrors.ModularHierarchySharedNameConflict);
			}
		}

		public void validateInExistingNonSharedTreeAndUpdateNodeValidity()
		{
			IConnector currentNodeConnector = getConnector();
			String name = toString();
			for (Map.Entry<IConnector, String> entry : getModularConnectorToSharedNamesMap().entrySet()) {
				IConnector key = entry.getKey();
				if (key != currentNodeConnector) {
					String val = entry.getValue();
					if (name.compareToIgnoreCase(val) == 0) {
						m_valid = false;
						setValidityMessageCode(ModularConnectorShareErrors.DuplicateNameInModularHierarchy);
					}
				}
			}
		}

		public void validate(String newName)
		{
			boolean valid = validateName(newName);
			if (m_valid != valid) {
				m_valid = valid;
				//tell the model that this node is changed. It will ask the renderer to repaint this node
				//NodeChanged(this);
			}
		}

		/**
		 * checks if new name provided is valid or not. Empty name is not valid Name which matches with any other
		 * connector in this hierarchy is not valid Name which matched with any shared object of this type is not valid
		 * Rest all cases is valid name
		 *
		 * @param newName - the newName whose validity has to be checked
		 *
		 * @return true if name is valid
		 */
		private boolean validateName(String newName)
		{
			final ModularConnectorShareErrors nameLengthCheckCode = checkNameLength(newName);
			if (nameLengthCheckCode != null) {
				setValidityMessageCode(nameLengthCheckCode);
				return false;
			}
			IConnector currentNodeConnector = (IConnector) getUserObject();
			for (Map.Entry<IConnector, String> entry : getModularConnectorToSharedNamesMap().entrySet()) {
				IConnector key = entry.getKey();
				if (key != currentNodeConnector) {
					String val = entry.getValue();
					if (newName.compareToIgnoreCase(val) == 0) {
						setValidityMessageCode(ModularConnectorShareErrors.DuplicateNameInModularHierarchy);
						return false;
					}
				}
			}
			if (sharedObjExistsWithSameName(newName, currentNodeConnector)) {
				setValidityMessageCode(ModularConnectorShareErrors.ModularHierarchySharedNameConflict);
				return false;
			}
			return true;
		}

		public boolean isValid()
		{
			return m_valid;
		}

		private boolean sharedObjExistsWithSameName(String name, IReadOnlyNamedObject object)
		{
			ValidateDuplicateSharedNames validateDuplicateSharedNames = new ValidateDuplicateSharedNames(getDesign());
			return validateDuplicateSharedNames.isDuplicateName(name, object, getProject());
		}

		@Nullable private ModularConnectorShareErrors checkNameLength(@Nullable String name)
		{
			if (name == null || StringUtils.isEmpty(name.trim())) {
				return ModularConnectorShareErrors.ModularConnectorInvalidName;
			}
			if (name.length() > CHSConstants.MAX_NAME_LENGTH) {
				return ModularConnectorShareErrors.ModularConnectorNameTooLong;
			}

			return null;
		}

		public boolean isGeneratedName()
		{
			return getConnector().isGeneratedName();
		}
	}
}
