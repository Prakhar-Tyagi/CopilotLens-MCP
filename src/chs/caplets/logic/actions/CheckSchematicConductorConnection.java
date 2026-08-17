package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.QAExtensionAppAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ui.BaseProgressBar;
import chs.utilities.ui.BasicUIFactory;
import chs.common.IIncLoadable;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: svsn Date: Jan 31, 2011 Time: 10:05:43 AM To change this template use File | Settings
 * | File Templates.
 */
@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner, Application.ArtisanFunction},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner, Application.ArtisanFunction}
)
public class CheckSchematicConductorConnection extends QAExtensionAppAction
{

	private BaseProgressBar progressBar;
	private JButton startButton;
	private JTextArea taskOutput;
	private Task task;
	private int numDigrams = 0;

	public CheckSchematicConductorConnection(IFIB fib)
	{
		super(fib);
		putValue(Action.NAME, "Validate Conductor Schematics...");
		putValue(Action.LONG_DESCRIPTION,
				"Check for each diagram if a conductor is schematically connected to at least one schematic pin");
	}

	public void actionPerformed(ActionEvent e)
	{
		ISchemDiagram activeDiagram = (ISchemDiagram) CAFUtils.getInstance().getActiveDiagram();
		if (activeDiagram != null) {
			numDigrams = activeDiagram.getDesign().getNumDiagrams();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					createAndShowGUI();
				}
			});
		}
	}

	public boolean isEnabled()
	{
		return (CAFUtils.getInstance().getActiveDiagram() != null);
	}

	public void updateUI()
	{
		setEnabled(isEnabled());
	}

	private void createAndShowGUI()
	{
		//Create and set up the window.
		JFrame frame = new JFrame(" Validate Conductor Schematics");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JPanel newContentPane = new JPanel(new BorderLayout());
		startButton = BasicUIFactory.getInstance().createSiemensCustomJButton("Start");
		startButton.setActionCommand("start");

		startButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				startButton.setEnabled(false);

				task = new Task();
				task.addPropertyChangeListener(new PropertyChangeListener()
				{
					@Override public void propertyChange(PropertyChangeEvent evt)
					{
						if (evt.getPropertyName() == "progress") {
							int progress = (Integer) evt.getNewValue();
							progressBar.setValue(progress);
						}
					}
				});
				task.execute();
			}
		});

		progressBar = new BaseProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		JPanel panel = new JPanel();
		panel.add(startButton);
		panel.add(progressBar);

		taskOutput = new JTextArea(5, 20);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);

		newContentPane.add(panel, BorderLayout.PAGE_START);
		newContentPane.add(new JScrollPane(taskOutput), BorderLayout.CENTER);

		//Create and set up the content pane.
		newContentPane.setOpaque(true); //content panes must be opaque
		frame.setContentPane(newContentPane);

		//Display the window.
		frame.setLocation(400, 400);
		frame.setSize(500, 300);
		frame.setVisible(true);
	}

	class Task extends SwingWorker<Void, Void>
	{

		@Nullable public Void doInBackground()
		{
			long startTime = System.currentTimeMillis();
			ISchemDiagram activeDiagram = (ISchemDiagram) CAFUtils.getInstance().getActiveDiagram();
			if (activeDiagram != null) {
				ILogicDesign design = activeDiagram.getDesign();
				taskOutput.append("Design Name: " + design.getName() + "\n");
				taskOutput.append("-------------------------------------------------\n");

				try {
					CAFUtils.getInstance().getWindowMgr().getDialogFrame()
							.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					int i = 0;
					setProgress(i);
					for (ISchemDiagram diagram : design.getDiagrams()) {
						boolean wasSkeleton = ((IIncLoadable) diagram).isSkeleton();
						boolean printDiagramName = false;
						for (IConductor conductor : diagram.getConductors()) {
							boolean connected = false;
							if (!conductor.getPins().isEmpty()) {
								Set<IAbstractPin> allPins = conductor.getConnectivity().getAllPins();
								for (IAbstractPin pin : allPins) {
									IDiagramObjectIterator representations = diagram.getRepresentations(pin.getUID());
									if (representations != null) {
										for (IDiagramObject rep : representations) {
											if (conductor.getPins().contains(rep)) {
												connected = true;
												break;
											}
										}
									}
									if (connected) {
										break;
									}
								}
							}
							if (!connected) {
								if (!printDiagramName) {
									printDiagramName = true;
									taskOutput.append("Diagram Name: " + diagram.getName() + "\n");
									taskOutput.append("-------------------------------------\n");
								}
								taskOutput.append("	Conductor: " + conductor.getConnectivity().getName());
								taskOutput.append("\n");
							}
						}

						if (wasSkeleton) {
							diagram.unloadChildren();
						}
						i++;
						setProgress(100 * i / numDigrams);
					}
					taskOutput.append("\n");
					taskOutput.append("Total Number of Diagrams Processed: " + i);

					long totalTime = (System.currentTimeMillis() - startTime) / 1000;
					taskOutput.append("\n");
					taskOutput.append("Total Time: " + totalTime + " Seconds");
				}
				finally {

					CAFUtils.getInstance().getWindowMgr().getDialogFrame()
							.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}

			return null;
		}

		public void done()
		{
			//startButton.setEnabled(true);
			//taskOutput = null;
		}
	}
}
