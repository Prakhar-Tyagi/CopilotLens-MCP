/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.utils;

import chs.caplets.logic.actions.actionreport.HyperLinkHandler;
import chs.ctf.ui.utility.statusmessage.IStatus;
import chs.images.CHSImageLoader;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.helpers.EditControl;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;

/**
 * Supports rendering IStatus as icon label and node as it is
 */
public class StatusMessageTableControlCreator implements IControlCreator
{

	@NotNull @Override public EditControl createEditor(ITableCell<?> cell)
	{
		return new EditControl()
		{
		};
	}

	@Override public void updateValue(Node control, IGenericTableCell<?> cell)
	{

	}

	@Nullable @Override public Node createRenderer(IGenericTableCell<?> cell)
	{
		Object value = cell.getValue();
		if (value instanceof Node) {
			return (Node) value;
		}
		if (value instanceof IStatus) {
			IStatus status = (IStatus) value;
			Label label = new Label();
			label.setTooltip(new Tooltip(status.getText()));
			Image image = CHSImageLoader.loadJFXImage(status.getIconPath());
			if (image != null) {
				label.setGraphic(new ImageView(image));
			}
			HBox hBox = new HBox(label);
			hBox.getStyleClass().add("capital-table-hBox-image");
			return hBox;
		}
		if (value instanceof HyperlinkInfo) {
			HyperlinkInfo hyperlinkInfo = (HyperlinkInfo) value;
			if(hyperlinkInfo.isShowPlainText()){
				Label label = new Label(hyperlinkInfo.getDisplayText());
				label.getStyleClass().add("capital-table-label");
				return label;
			}
			Hyperlink hyperlink = new Hyperlink(hyperlinkInfo.getDisplayText());
			final String linkText = hyperlinkInfo.getLinkText();
			hyperlink.setOnAction(e ->
					SwingUtilities.invokeLater(() -> new HyperLinkHandler().process(linkText)));
			return hyperlink;
		}
		return null;
	}

	@Nullable @Override public String stringify(@Nullable Object item)
	{
		String text = null;
		if (item instanceof Labeled) {
			text = ((Labeled) item).getText();
		}
		if (item instanceof IStatus) {
			text = ((IStatus) item).getText();
		}
		if (item instanceof HyperlinkInfo) {
			text = ((HyperlinkInfo) item).getDisplayText();
		}
		return StringUtils.emptyIfBlank(text);
	}
}