package chs.caplets.shared;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.helpers.ui.common.CAFToolBar;
import chs.caf.helpers.ui.common.ResourceHolder;
import chs.caplets.shared.actions.GroupAttributeAddHierarchyAction;
import chs.cof.logical.IDesign;
import chs.ctf.ui.form.CTFLabel;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.RegExUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupingByAttributesPanel extends JPanel
{

	protected GroupingByAttributesTree groupingByAttributesTree;

	public GroupingByAttributesPanel(IDesign design, BaseController baseController)
	{
		GroupAttributeConfigurator configurator = new GroupAttributeConfigurator();
		UIDMgr.addObject(configurator);
		//configurator.addAttributePath(Arrays.asList(IXMLTags.HARNESS, IXMLTags.NAME));
		//configurator.addAttributePath(Arrays.asList(IXMLTags.HARNESS));

		Preferences preferences = Preferences.userNodeForPackage(GroupingByAttributesTree.class);

		try {
			if (!preferences.nodeExists("GroupingByAttributeTree")) {

				List<IGroupAttributeAddEntry> defaultAttributeValues = new ArrayList<>();

				IGroupAttributeAddEntry thisEntry =
						new GroupingByAttributesTree.AttributeConfigured(GroupingByAttributesTree.ObjectTypeAttribute, true);
				defaultAttributeValues.add(thisEntry);
				configurator.addAttributePath(defaultAttributeValues);
			}
			else {
				preferences = preferences.node("GroupingByAttributeTree");

				String[] childNodes = preferences.childrenNames();
				int index = 0;
				for (String aChild : childNodes) {
					if (aChild.equals("path" + index)) {
						Preferences childPref = preferences.node(aChild);
						Preferences attributePref = childPref.node("attributes");

						List<IGroupAttributeAddEntry> attributeAddEntries = new ArrayList<>();
						List<String> givenAttributePath = Arrays.asList(attributePref.keys());

						for (String anAnttribute : givenAttributePath) {

							String storedSeperator = attributePref.get(anAnttribute, "");
							String prefixForProperty = "Property";
							boolean isProperty = anAnttribute.startsWith(prefixForProperty);
							if (isProperty) {
								anAnttribute = anAnttribute.substring(prefixForProperty.length());
							}
							IGroupAttributeAddEntry thisEntry =
									new GroupingByAttributesTree.AttributeConfigured(anAnttribute, !isProperty);
							thisEntry.setSeperator(getSeperators(storedSeperator));
							attributeAddEntries.add(thisEntry);
						}

						configurator.addAttributePath(attributeAddEntries);
						index++;
					}
				}
			}
		}
		catch (BackingStoreException ignored) {
		}
		GroupingAttributesClient browserClient = new GroupingAttributesClient(baseController, design, configurator);
		groupingByAttributesTree = new GroupingByAttributesTree(browserClient, "groupingbyattributes", configurator);
	}

	JPanel addToPanel(@Nullable JPanel panel)
	{
		JPanel requiredTabPanel = panel;
		if (panel == null) {
			requiredTabPanel = new JPanel();
		}

		requiredTabPanel.setLayout(new GridBagLayout());

		GridBagConstraints gridBagConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE,
				GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 0, 4), 0, 0
		);

		requiredTabPanel.add(createToolbar(), gridBagConstraints);

		gridBagConstraints = new GridBagConstraints(0, 1, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0
		);
		JPanel treePanel = groupingByAttributesTree.buildContentPanel(null);

		requiredTabPanel.add(treePanel, gridBagConstraints);

		return requiredTabPanel;
	}

	private JPanel createToolbar()
	{
		JPanel toolbarPanel = new JPanel();

		toolbarPanel.setLayout(new GridBagLayout());
		ActionContainer groupAttributesToolbar = new ActionContainer("groupattributestoolbar");
		groupAttributesToolbar.add(new ActionEntry(new GroupAttributeAddHierarchyAction(
				new GroupAttributeAddHierarchyAction.AttributeParams(groupingByAttributesTree)))
		{
			@Override public boolean shouldDisplay()
			{
				return true;
			}
		});
		CAFToolBar toolbar = ResourceHolder.createToolBar((String) groupAttributesToolbar.getValue(Action.NAME),
				groupAttributesToolbar.getMembers(), null);
		toolbar.setBorder(null);

		GridBagConstraints gridBagConstraints = new GridBagConstraints(0,
				0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 0, 4), 0, 0
		);

		toolbarPanel.add(toolbar, gridBagConstraints);
		JLabel helperTextLabel = new CTFLabel();
		helperTextLabel.setText(
				ResourceMgr
						.getString(GroupingByAttributesPanel.class, "GroupingByAttributesPanel.helper.text"));
		gridBagConstraints = new GridBagConstraints(1,
				0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 0, 4), 0, 0
		);
		toolbarPanel.add(helperTextLabel, gridBagConstraints);

		toolbarPanel.repaint();
		return toolbarPanel;
	}

	@Nullable Collection<String> getSeperators(String storedSeperator)
	{
		String searchPattern = FactoryMgr.getRegExUtils().escapeSpecialCharacters("|");
		if (StringUtils.isBlank(storedSeperator)) {
			return null;
		}
		Pattern pattern = Pattern.compile(searchPattern);
		Matcher matcher = pattern.matcher(storedSeperator);
		List<String> splitSeperators = new ArrayList<>();
		int index = 0;
		StringBuilder appendToPrevious = null;
		while (matcher.find()) {

			String seperator = storedSeperator.substring(index, matcher.start());
			index = matcher.end();
			if (!seperator.isEmpty() && "\\".equals(seperator.substring(seperator.length() - 1))) {
				int endIndex = seperator.length() - 1;
				seperator = (endIndex > 0 ? seperator.substring(0, endIndex) : "");

				if (appendToPrevious == null) {
					appendToPrevious = new StringBuilder(seperator + "|");
				}
				else {
					appendToPrevious.append(seperator).append("|");
				}
			}
			else {
				if (appendToPrevious != null) {
					seperator = appendToPrevious + seperator;
				}
				appendToPrevious = null;
			}
			if (appendToPrevious == null) {
				splitSeperators.add(seperator);
			}
		}

		StringBuilder residualString =
				new StringBuilder((appendToPrevious != null ? appendToPrevious.toString() : ""));
		if (index < storedSeperator.length()) {
			if (index == 0) { //avoin substring operation.
				splitSeperators.add(storedSeperator);
			}
			else {
				residualString.append(storedSeperator.substring(index));
			}
		}
		if (!residualString.toString().isEmpty()) {
			splitSeperators.add(residualString.toString());
		}
		return splitSeperators;
	}

	GroupingByAttributesTree getTree()
	{
		return groupingByAttributesTree;
	}
}
