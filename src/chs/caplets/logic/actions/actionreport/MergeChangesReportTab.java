/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.caf.CAFUtils;
import chs.caf.utility.csvreportgenerator.IDataExporter;
import chs.caf.utility.csvreportgenerator.TableDataCSVExporter;
import chs.common.IProjectPreferenceMgr;
import chs.services.ui.IHyperlinkHandler;
import chs.utilities.ResourceMgr;
import chs.utilities.stream.StreamUtils;
import chs.utility.ui.JFXPanelAndKeyEventHandler;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import com.sun.javafx.application.PlatformImpl;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MergeChangesReportTab extends JPanel
{

	public static final int DISPLAY_MESSAGE_FONT_SIZE = 14;
	public static final String MERGE_CHANGES_REPORT_PANEL = "MergeChangesReportPanel";
	public static final String MERGED_OBJECT_HYPERLINK = "MergedObjectLink";
	protected JFXPanel jfxPanel;
	private Button mSavebutton;
	private static final String SAVE_ICON = "/chs/images/app/ico_save_to_csv_enabled.png";
	private Table<IMergeReportTableDataRow> mReportTable;
	private Collection<IMergeReportTableDataRow> mData;
	private String mActionChangeMessage;
	private String mUrl;
	private String mMergedObjectName;
	private String mAttributePropMessage;
	protected Hyperlink hyperlink;

	protected MergeChangesReportTab(String message, String url, String mergedObjectName,
			String attributePropMessage,
			Collection<IMergeReportTableDataRow> data)
	{
		super(new GridBagLayout());
		mData = data;
		mUrl = url;
		mMergedObjectName = mergedObjectName;
		mAttributePropMessage = attributePropMessage;
		mActionChangeMessage = message;
		initFxPanel();
		buildPanel();
	}

	private void initFxPanel()
	{
		if (jfxPanel == null) {
			jfxPanel = new JFXPanelAndKeyEventHandler()
			{
				@Override public boolean canKeyEventExecuteAction()
				{
					//if contextmenu is displayed on any of the column then disallow action execution in key events.
					if (getReportTable() == null) {
						return true;
					}
					boolean isAnyContextMenuOpen =
							getReportTable().columns().map(aCol -> aCol.getTableColumn()).filter(StreamUtils::notNull)
									.map(TableColumnBase::getContextMenu).filter(StreamUtils::notNull)
									.filter(aContext -> aContext.isShowing()).findFirst().isPresent();
					return !isAnyContextMenuOpen;
				}
			};
			jfxPanel.setName(MERGE_CHANGES_REPORT_PANEL);
		}
	}

	protected Table<IMergeReportTableDataRow> getReportTable()
	{
		if (mReportTable != null) {
			return mReportTable;
		}
		mReportTable = new Table<IMergeReportTableDataRow>("ReportTable");

		ColumnInformation<IMergeReportTableDataRow> objectName =
				new ColumnInformation<>(ResourceMgr.getString(this, "MergeChangesReportTab.table.column.object"), "object",
						IMergeReportTableDataRow::getObjectName);
		ColumnInformation<IMergeReportTableDataRow> type =
				new ColumnInformation<>(ResourceMgr.getString(this, "MergeChangesReportTab.table.column.type"), "type",
						IMergeReportTableDataRow::getDisplayInformationType);
		ColumnInformation<IMergeReportTableDataRow> name =
				new ColumnInformation<>(ResourceMgr.getString(this, "MergeChangesReportTab.table.column.attribute"),
						"name", IMergeReportTableDataRow::getKey);
		ColumnInformation<IMergeReportTableDataRow> sourceValue =
				new ColumnInformation<>(ResourceMgr.getString(this, "MergeChangesReportTab.table.column.source"),
						"sourcevalue", IMergeReportTableDataRow::getInitialValue);
		ColumnInformation<IMergeReportTableDataRow> targetValue =
				new ColumnInformation<>(ResourceMgr.getString(this, "MergeChangesReportTab.table.column.target"),
						"targetvalue",
						IMergeReportTableDataRow::getInitialTargetValue);
		ColumnInformation<IMergeReportTableDataRow> mergedValue =
				new ColumnInformation<>(ResourceMgr.getString(this, "MergeChangesReportTab.table.column.merged"),
						"mergedvalue",
						IMergeReportTableDataRow::getTransformedValue);

		List<ColumnInformation<IMergeReportTableDataRow>> columnInformations = new ArrayList<>();
		columnInformations.add(objectName);
		columnInformations.add(type);
		columnInformations.add(name);
		columnInformations.add(sourceValue);
		columnInformations.add(targetValue);
		columnInformations.add(mergedValue);
		if (isDetailsColumnRequired()) {
			ColumnInformation<IMergeReportTableDataRow> detailsColumnInfo = new ColumnInformation<>(
					ResourceMgr.getString(this, "MergeChangesReportTab.table.column.details"), "details",
					IMergeReportTableDataRow::getDetails);
			columnInformations.add(detailsColumnInfo);
		}
		mReportTable.addColumns(columnInformations);
		return mReportTable;
	}

	private boolean isDetailsColumnRequired()
	{
		return Optional.ofNullable(CAFUtils.getInstance().getCurrentProjectPreferences())
				.map(IProjectPreferenceMgr::isMergeDeviceConnectorsEnabled).orElse(false);
	}

	protected void buildPanel()
	{
		final Runnable runnable = () -> {
			Scene scene = createScene();
			jfxPanel.setScene(scene);
			add(jfxPanel, new GridBagConstraints(0, 0, 0, 0, 1.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		};
		PlatformImpl.runAndWait(runnable);
	}

	protected Scene createScene()
	{
		populateTableData();
		BorderPane contentPane = new BorderPane();
		HBox hb = new HBox();
		VBox mergeReportPane = new VBox();
		Scene scene = new Scene(mergeReportPane);

		JFXFlatUIUtils.getInstance().setFlatUIFor(scene);

		mergeReportPane.prefHeightProperty().bind(scene.heightProperty());
		mergeReportPane.prefWidthProperty().bind(scene.widthProperty());

		mergeReportPane.getChildren().add(hb);
		mergeReportPane.getChildren().add(contentPane);
		Pane rightSpacer = new Pane();
		HBox.setHgrow(rightSpacer, Priority.ALWAYS);
		hb.setAlignment(Pos.CENTER);
		hb.getChildren().add(getDisplayMessage());
		hb.getChildren().add(createMergeObjectHyperLink());
		hb.getChildren().add(getSeperator());
		hb.getChildren().add(getAttributePropMessage());
		hb.getChildren().add(rightSpacer);
		hb.getChildren().add(getSaveToCSVButton());
		contentPane.setCenter(getReportTable());

		getReportTable().prefHeightProperty().bind(scene.heightProperty());
		getReportTable().prefWidthProperty().bind(scene.widthProperty());

		return scene;
	}

	private Node createMergeObjectHyperLink()
	{
		hyperlink = new Hyperlink(mMergedObjectName);
		hyperlink.setId(MERGED_OBJECT_HYPERLINK);
		hyperlink.setFont(new Font(hyperlink.getFont().getFamily(), DISPLAY_MESSAGE_FONT_SIZE));
		hyperlink.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override public void handle(ActionEvent event)
			{

				SwingUtilities.invokeLater(new Runnable()
				{
					@Override public void run()
					{
						getHyperLinkHandler().process(mUrl);
					}
				});

			}
		});
		return hyperlink;
	}

	public IHyperlinkHandler getHyperLinkHandler()
	{
		return new HyperLinkHandler();
	}

	public Label getDisplayMessage()
	{
		Label lb = new Label(mActionChangeMessage);
		lb.setAlignment(Pos.CENTER);
		lb.setFont(new Font(lb.getFont().getFamily(), DISPLAY_MESSAGE_FONT_SIZE));
		return lb;
	}

	public Label getAttributePropMessage()
	{
		Label lb = new Label(mAttributePropMessage);
		lb.setAlignment(Pos.CENTER);
		lb.setFont(new Font(lb.getFont().getFamily(), DISPLAY_MESSAGE_FONT_SIZE));
		return lb;
	}

	public Label getSeperator()
	{
		Label lb = new Label(".");
		lb.setAlignment(Pos.CENTER);
		lb.setFont(new Font(lb.getFont().getFamily(), DISPLAY_MESSAGE_FONT_SIZE));
		return lb;
	}

	protected void populateTableData()
	{
		getReportTable().addData(mData);
	}

	protected Button getSaveToCSVButton()
	{
		if (mSavebutton == null) {
			mSavebutton = new Button();
			mSavebutton.setBorder(Border.EMPTY);
			mSavebutton.setTooltip(new Tooltip());
			mSavebutton.setId("SaveButton");
			mSavebutton.getTooltip().getStyleClass().add("tooltip");
			mSavebutton.getTooltip().setText(ResourceMgr.getString(this, "MergeChangesReportTab.savetocsv.tooltip"));
			mSavebutton.setGraphic(getImageIcon(SAVE_ICON));
			mSavebutton.setOnAction(event -> save());
		}
		return mSavebutton;
	}

	@NotNull protected ImageView getImageIcon(String imageName)
	{
		Image image = new Image(getClass().getResource(imageName).toString());
		return new ImageView(image);
	}

	protected void save()
	{
		Runnable saveToCSV = new Runnable()
		{
			@Override public void run()
			{
				getDataExporter().exportData();
			}
		};
		SwingUtilities.invokeLater(saveToCSV);
	}

	protected IDataExporter getDataExporter()
	{
		String[] header = new String[1];
		header[0] = mActionChangeMessage + " " + mMergedObjectName + "." + mAttributePropMessage;
		return new TableDataCSVExporter<IMergeReportTableDataRow>(CAFUtils.getInstance().getDialogFrame(),
				getReportTable(), header);
	}
}
