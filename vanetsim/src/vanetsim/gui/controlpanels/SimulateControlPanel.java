package vanetsim.gui.controlpanels;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import vanetsim.ErrorLog;
import vanetsim.VanetSimStart;
import vanetsim.gui.Renderer;
import vanetsim.gui.helpers.ButtonCreator;
import vanetsim.gui.helpers.ReRenderManager;
import vanetsim.localization.Messages;
import vanetsim.map.Map;
import vanetsim.scenario.Scenario;
import vanetsim.simulation.SimulationMaster;

/**
 * This class creates all control elements used in the simulation tab.
 */
public final class SimulateControlPanel extends JPanel implements ActionListener, ChangeListener, ItemListener{

	/** The necessary constant for serializing. */
	private static final long serialVersionUID = 7292404190066585320L;

	/** The slider for zooming. */
	private final JSlider zoomSlider_;

	/** A panel which includes the buttons for starting and stopping the simulation. Uses a <code>CardLayout</code>. */
	private final JPanel startStopJPanel_;
	
	/** An area to display text information. */
	private final JTextArea informationTextArea_;
	
	/** A checkbox to enable/disable the display of circles so that it's possible to see the max. distances the vehicles may communicate. */
	private final JCheckBox communicationDisplayCheckBox_;
	
	/** A checkbox to enable/disable the display of filled circles so that it's possible to see the mix zones. */
	private final JCheckBox mixZoneDisplayCheckBox_;
	
	/** A checkbox to enable/disable the display of the vehicle ID. */
	private final JCheckBox vehicleIDDisplayCheckBox_;
	
	/** The input field for the target step time. Used to increase or decrease simulation speed. */
	private final JFormattedTextField targetStepTime_;
	
	/** A button to apple the target step time. */
	private final JButton targetStepTimeApplyButton_;
	
	/** The input field for the target time to jump to. */
	private final JFormattedTextField jumpToTargetTime_;
	
	/** A button to apply the target time. */
	private final JButton jumpToTargetApplyButton_;
	
	/** If the zooming slider is set externally, no rerender shall be made within the listeners here! */
	private boolean dontReRenderZoom_ = false;
	
	/**
	 * Constructor for this control panel.
	 */
	public SimulateControlPanel(){
		setLayout(new GridBagLayout());
		
		// global layout settings
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.insets = new Insets(5,5,5,5);
		
		// load buttons
		add(ButtonCreator.getJButton("loadmap.png", "loadmap", Messages.getString("SimulateControlPanel.loadMap"), this), c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		c.gridx = 1;
		add(ButtonCreator.getJButton("loadscenario.png", "loadscenario", Messages.getString("SimulateControlPanel.loadScenario"), this), c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		c.gridx = 0;
		c.gridwidth = 2;

		// Panel with panning controls
		JLabel jLabel1 = new JLabel("<html><u><b>" + Messages.getString("SimulateControlPanel.mapControl") + "</u></b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		++c.gridy;
		add(jLabel1, c);		
		JPanel panning = new JPanel();
		panning.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 0.5;
		c1.gridx = 1;
		c1.gridy = 0;
		c1.gridheight = 1;
		panning.add(ButtonCreator.getJButton("up.png", "up", Messages.getString("SimulateControlPanel.upButton"), this), c1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		c1.gridx = 0;
		c1.gridy = 1;
		panning.add(ButtonCreator.getJButton("left.png", "left", Messages.getString("SimulateControlPanel.leftButton"), this), c1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		c1.gridx = 2;
		c1.gridy = 1;
		panning.add(ButtonCreator.getJButton("right.png", "right", Messages.getString("SimulateControlPanel.rightButton"), this), c1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		c1.gridx = 1;
		c1.gridy = 2;
		panning.add(ButtonCreator.getJButton("down.png", "down", Messages.getString("SimulateControlPanel.downButton"), this), c1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		++c.gridy;
		add(panning, c);    	
		
		// zoom slider
		jLabel1 = new JLabel("<html><u><b>" +Messages.getString("SimulateControlPanel.zoom") +"</u></b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		++c.gridy;
		add(jLabel1, c);		
		
		zoomSlider_ = getZoomSlider();
		++c.gridy;
		add(zoomSlider_, c);
		
		// Start and stop simulation
		jLabel1 = new JLabel("<html><u><b>" +Messages.getString("SimulateControlPanel.simulation") +"</u></b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		++c.gridy;
		add(jLabel1, c);
		
		startStopJPanel_ = new JPanel(new CardLayout());
		startStopJPanel_.add(ButtonCreator.getJButton("start.png", "start", Messages.getString("SimulateControlPanel.start"), this), "start"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		startStopJPanel_.add(ButtonCreator.getJButton("pause.png", "pause", Messages.getString("SimulateControlPanel.pause"), this), "pause"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		++c.gridy;
		c.gridwidth = 1;
		add(startStopJPanel_, c);
		
		c.gridx = 1;
		add(ButtonCreator.getJButton("onestep.png", "onestep", Messages.getString("SimulateControlPanel.onestep"), this), c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		c.gridx = 0;
		c.gridwidth = 2;
		JPanel tmpPanel = new JPanel();
		jLabel1 = new JLabel(Messages.getString("SimulateControlPanel.jumpToTime")); //$NON-NLS-1$
		tmpPanel.add(jLabel1);
		jumpToTargetTime_ = new JFormattedTextField(NumberFormat.getIntegerInstance());
		jumpToTargetTime_.setPreferredSize(new Dimension(40,20));
		jumpToTargetTime_.setValue(0);
		tmpPanel.add(jumpToTargetTime_);
		jumpToTargetApplyButton_ = ButtonCreator.getJButton("ok_small.png", "targetTimeApply", Messages.getString("SimulateControlPanel.applyTargetTime"), this); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		tmpPanel.add(jumpToTargetApplyButton_);
		++c.gridy;
		c.insets = new Insets(0,0,0,0);
		add(tmpPanel, c);
		
		tmpPanel = new JPanel();
		jLabel1 = new JLabel(Messages.getString("SimulateControlPanel.targetStepTime")); //$NON-NLS-1$
		tmpPanel.add(jLabel1);
		targetStepTime_ = new JFormattedTextField(NumberFormat.getIntegerInstance());
		targetStepTime_.setPreferredSize(new Dimension(30,20));
		targetStepTime_.setValue(SimulationMaster.TIME_PER_STEP);
		tmpPanel.add(targetStepTime_);
		targetStepTimeApplyButton_ = ButtonCreator.getJButton("ok_small.png", "targetStepTimeApply", Messages.getString("SimulateControlPanel.applyTargetStepTime"), this); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		tmpPanel.add(targetStepTimeApplyButton_);
		++c.gridy;		
		add(tmpPanel, c);
		c.insets = new Insets(5,5,5,5);
		
		// information display checkboxes
		jLabel1 = new JLabel("<html><u><b>" + Messages.getString("SimulateControlPanel.information") +"</u></b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		++c.gridy;
		add(jLabel1, c);
		
		++c.gridy;
		communicationDisplayCheckBox_ = new JCheckBox(Messages.getString("SimulateControlPanel.displayDistance"), false); //$NON-NLS-1$
		communicationDisplayCheckBox_.addItemListener(this);
		add(communicationDisplayCheckBox_,c);
		
		c.insets = new Insets(0,5,5,5);
		++c.gridy;
		mixZoneDisplayCheckBox_ = new JCheckBox(Messages.getString("SimulateControlPanel.hideMixZones"), false); //$NON-NLS-1$
		mixZoneDisplayCheckBox_.addItemListener(this);
		add(mixZoneDisplayCheckBox_,c);
		
		++c.gridy;
		vehicleIDDisplayCheckBox_ = new JCheckBox(Messages.getString("SimulateControlPanel.showVehicleIDs"), false); //$NON-NLS-1$
		vehicleIDDisplayCheckBox_.addItemListener(this);
		add(vehicleIDDisplayCheckBox_,c);
		c.insets = new Insets(5,5,5,5);
		
		//text area for display of information. Consumes all available space
		c.gridwidth = 2;
		informationTextArea_ = new JTextArea(20,1);
		informationTextArea_.setEditable(false);
		informationTextArea_.setLineWrap(true);
		JScrollPane scrolltext = new JScrollPane(informationTextArea_);
		c.weighty = 1.0;
		++c.gridy;
		add(scrolltext, c);
	}
	
	/**
	 * Shows the "start simulation" button after simulation has been stopped externally.
	 */
	public void setSimulationStop(){
		CardLayout cl = (CardLayout)(startStopJPanel_.getLayout());
		cl.show(startStopJPanel_, "start"); //$NON-NLS-1$
	}
	
	/**
	 * Sets the text in the information area.
	 * 
	 * @param newText the new information text
	 */
	public void setInformation(String newText){
		informationTextArea_.setText(newText);
	}
	
	/**
	 * Sets the value of the zooming slider.
	 * 
	 * @param zoom the new zoom value
	 */
	public void setZoomValue(int zoom){
		dontReRenderZoom_ = true;
		zoomSlider_.setValue(zoom);
	}

	/**
	 * Creates a slider for zooming.
	 * 
	 * @return a ready-to-use <code>JSlider</code>
	 */
	private JSlider getZoomSlider() {
		JSlider slider = new JSlider(-75, 212, 1);
		Hashtable<Integer,JLabel> ht = new Hashtable<Integer,JLabel>();
		// the labels correspond to a exponential scale but internally the slider calculates only in a constant scale => need to convert in stateChanged()!
		ht.put(-75, new JLabel("3km"));	//$NON-NLS-1$
		ht.put(-20, new JLabel("1km"));	//$NON-NLS-1$
		ht.put(45, new JLabel("200m"));	//$NON-NLS-1$
		ht.put(96, new JLabel("100m"));	//$NON-NLS-1$
		ht.put(157, new JLabel("30m"));	//$NON-NLS-1$
		ht.put(212, new JLabel("10m"));	//$NON-NLS-1$
		slider.setLabelTable(ht);
		slider.setPaintLabels(true);
		slider.setMinorTickSpacing(10);
		slider.setMajorTickSpacing(40);
		//slider.setPaintTicks(true);
		slider.addChangeListener(this);
		return slider;
	}
	
	/**
	 * An implemented <code>ActionListener</code> which performs all needed actions when a <code>JButton</code>
	 * is clicked.
	 * 
	 * @param e an <code>ActionEvent</code>
	 */		
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("up".equals(command)){ //$NON-NLS-1$
			Renderer.getInstance().pan('u');
			ReRenderManager.getInstance().doReRender();
		} else if ("down".equals(command)){ //$NON-NLS-1$
			Renderer.getInstance().pan('d');
			ReRenderManager.getInstance().doReRender();
		} else if ("left".equals(command)){ //$NON-NLS-1$
			Renderer.getInstance().pan('l');
			ReRenderManager.getInstance().doReRender();
		} else if ("right".equals(command)){ //$NON-NLS-1$
			Renderer.getInstance().pan('r');
			ReRenderManager.getInstance().doReRender();
		} else if ("loadmap".equals(command)){ //$NON-NLS-1$
			VanetSimStart.getMainControlPanel().changeFileChooser(true, true, false);
			int returnVal = VanetSimStart.getMainControlPanel().getFileChooser().showOpenDialog(VanetSimStart.getMainFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION) {           
				Runnable job = new Runnable() {
					public void run() {
						Map.getInstance().load(VanetSimStart.getMainControlPanel().getFileChooser().getSelectedFile(), false);
					}
				};
				new Thread(job).start();
			}	
		} else if ("loadscenario".equals(command)){ //$NON-NLS-1$
			VanetSimStart.getMainControlPanel().changeFileChooser(true, true, false);
			int returnVal = VanetSimStart.getMainControlPanel().getFileChooser().showOpenDialog(VanetSimStart.getMainFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION) { 
				Runnable job = new Runnable() {
					public void run() {
						Scenario.getInstance().load(VanetSimStart.getMainControlPanel().getFileChooser().getSelectedFile(), false);
					}
				};
				new Thread(job).start();
			}
		} else if ("pause".equals(command)){ //$NON-NLS-1$
			CardLayout cl = (CardLayout)(startStopJPanel_.getLayout());
			cl.show(startStopJPanel_, "start"); //$NON-NLS-1$
			Runnable job = new Runnable() {
				public void run() {
					VanetSimStart.getSimulationMaster().stopThread();
				}
			};
			new Thread(job).start();			
		} else if ("start".equals(command)){ //$NON-NLS-1$
			if(VanetSimStart.getMainControlPanel().getEditPanel().getEditMode() == true) ErrorLog.log(Messages.getString("SimulateControlPanel.simulationNotPossibleInEditMode"), 6, this.getName(), "startSim", null); //$NON-NLS-1$ //$NON-NLS-2$
			else {
				CardLayout cl = (CardLayout)(startStopJPanel_.getLayout());
				cl.show(startStopJPanel_, "pause"); //$NON-NLS-1$
				VanetSimStart.getSimulationMaster().startThread();
			}		
		} else if ("onestep".equals(command)){ //$NON-NLS-1$
			if(VanetSimStart.getMainControlPanel().getEditPanel().getEditMode() == true) ErrorLog.log(Messages.getString("SimulateControlPanel.simulationNotPossibleInEditMode"), 6, this.getName(), "oneStep", null); //$NON-NLS-1$ //$NON-NLS-2$
			else{
				Runnable job = new Runnable() {
					public void run() {
						VanetSimStart.getSimulationMaster().doOneStep();
					}
				};
				new Thread(job).start();
			}
		} else if("targetTimeApply".equals(command)){ //$NON-NLS-1$
			if(VanetSimStart.getMainControlPanel().getEditPanel().getEditMode() == true) ErrorLog.log(Messages.getString("SimulateControlPanel.simulationNotPossibleInEditMode"), 6, this.getName(), "startSim", null); //$NON-NLS-1$ //$NON-NLS-2$
			else{
				int target = ((Number)jumpToTargetTime_.getValue()).intValue();
				if(target <= Renderer.getInstance().getTimePassed()) ErrorLog.log(Messages.getString("SimulateControlPanel.jumpingBackwardsNotPossible"), 6, this.getName(), "jumpToTime", null);  //$NON-NLS-1$//$NON-NLS-2$
				else VanetSimStart.getSimulationMaster().jumpToTime(target);
			}	
		} else if("targetStepTimeApply".equals(command)){ //$NON-NLS-1$
				int tmp = ((Number)targetStepTime_.getValue()).intValue();
				if(tmp < 0){
					ErrorLog.log(Messages.getString("SimulateControlPanel.noNegativeTargetStepTime"), 6, this.getName(), "targetStepTimeApply", null); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					VanetSimStart.getSimulationMaster().setTargetStepTime(tmp);
				}
		}
	}

	/**
	 * An implemented <code>ChangeListener</code> for the zooming slider which performs the necessary actions.
	 * 
	 * @param e a <code>ChangeEvent</code>
	 */	
	public void stateChanged(ChangeEvent e) {		
		JSlider source = (JSlider)e.getSource();
		if (!source.getValueIsAdjusting()) {	// only perform action when mouse button is released!
			int value = source.getValue();
			double scale = Math.exp(value/50.0)/1000;
			if(dontReRenderZoom_) dontReRenderZoom_ = false;
			else{
				Renderer.getInstance().setMapZoom(scale);
				ReRenderManager.getInstance().doReRender();
			}
		}
	}
	
	/**
	 * Invoked when an item changes. Used for the JCheckBox.
	 * 
	 * @param e the change event
	 * 
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e){
		boolean state;
		if(e.getStateChange() == ItemEvent.SELECTED) state = true;
		else state = false;
		if(e.getSource() == communicationDisplayCheckBox_) Renderer.getInstance().setHighlightCommunication(state);
		if(e.getSource() == mixZoneDisplayCheckBox_) Renderer.getInstance().setHideMixZones(state);
		if(e.getSource() == vehicleIDDisplayCheckBox_) Renderer.getInstance().setDisplayVehicleIDs(state);
		ReRenderManager.getInstance().doReRender();
	}

}