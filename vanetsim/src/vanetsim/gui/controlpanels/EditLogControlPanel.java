package vanetsim.gui.controlpanels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import vanetsim.gui.Renderer;
import vanetsim.gui.helpers.AttackLogWriter;
import vanetsim.gui.helpers.PrivacyLogWriter;
import vanetsim.gui.helpers.TextAreaLabel;
import vanetsim.localization.Messages;
import vanetsim.scenario.Vehicle;

/**
 * This class represents the control panel for adding mix zones.
 */
public class EditLogControlPanel extends JPanel implements  FocusListener, ActionListener{

	/** The necessary constant for serializing. */
	private static final long serialVersionUID = -8294786435746799533L;


	/** CheckBox to log attacker data */
	private final JCheckBox logAttackerCheckBox_;	
	
	/** The input field for the log path */
	private final JFormattedTextField logAttackerPath_;

	/** flag to avoid loop when starting file chooser */
	private boolean attackerFlag = true;
	
	/** CheckBox to switch encrypted log on / off. */
	private final JCheckBox encryptedLogging_;	

	/** CheckBox to log privacy data */
	private final JCheckBox logPrivacyCheckBox_;	
	
	/** The input field for the privacy log path */
	private final JFormattedTextField logPrivacyPath_;

	/** flag to avoid loop when starting file chooser */
	private boolean privacyFlag = true;
	
	/** Note to describe add ... */
	TextAreaLabel dummyNote_;
	
	/** FileFilter to choose only ".log" files from FileChooser */
	private FileFilter logFileFilter_;
	
	/**
	 * Constructor, creating GUI items.
	 */
	public EditLogControlPanel() {
		setLayout(new GridBagLayout());
		
		// global layout settings
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		
		c.gridwidth = 1;

		c.insets = new Insets(5,5,5,5);
		
		c.gridx = 0;
		add(new JLabel(Messages.getString("EditLogControlPanel.attackerLogLabel")),c); //$NON-NLS-1$
		
		c.gridx = 1;
		logAttackerCheckBox_ = new JCheckBox();
		logAttackerCheckBox_.setSelected(false);
		logAttackerCheckBox_.setActionCommand("logAttackerData"); //$NON-NLS-1$
		logAttackerCheckBox_.addActionListener(this);
		add(logAttackerCheckBox_,c);	
		
		c.gridwidth = 2;
		++c.gridy;
		c.gridx = 0;	
		add(new JLabel(Messages.getString("EditLogControlPanel.attackerPathLabel")),c); //$NON-NLS-1$

		logAttackerPath_ = new JFormattedTextField();
		logAttackerPath_.setValue(System.getProperty("user.dir"));
		logAttackerPath_.setPreferredSize(new Dimension(100,20));
		logAttackerPath_.setName("attackerPath");
		logAttackerPath_.setCaretPosition(logAttackerPath_.getText().length());
		logAttackerPath_.setToolTipText(logAttackerPath_.getText());
		logAttackerPath_.addFocusListener(this);
		c.gridx = 1;
		add(logAttackerPath_,c);
		
		//Checkbox to enable encrypted logging
		c.gridx = 0;
		++c.gridy;
		add(new JLabel(Messages.getString("AttackerPanel.EncryptedLoggingLabel")),c);	
		
		encryptedLogging_ = new JCheckBox();
		encryptedLogging_.setSelected(false);
		encryptedLogging_.setActionCommand("encryptedLogging"); //$NON-NLS-1$
		c.gridx = 1;
		add(encryptedLogging_,c);
		encryptedLogging_.addActionListener(this);	
	      
		++c.gridy;
		c.gridwidth = 2;
		c.gridx = 0;	
		add(new JSeparator(SwingConstants.HORIZONTAL),c);
		
		
		++c.gridy;
		//c.gridwidth = 1;
		c.gridx = 0;
		add(new JLabel(Messages.getString("EditLogControlPanel.privacyLogLabel")),c); //$NON-NLS-1$
		
		c.gridx = 1;
		logPrivacyCheckBox_ = new JCheckBox();
		logPrivacyCheckBox_.setSelected(false);
		logPrivacyCheckBox_.setActionCommand("logPrivacyData"); //$NON-NLS-1$
		logPrivacyCheckBox_.addActionListener(this);
		add(logPrivacyCheckBox_,c);	
		
		c.gridwidth = 2;
		++c.gridy;
		c.gridx = 0;	
		add(new JLabel(Messages.getString("EditLogControlPanel.privacyPathLabel")),c); //$NON-NLS-1$

		logPrivacyPath_ = new JFormattedTextField();
		logPrivacyPath_.setValue(System.getProperty("user.dir"));
		logPrivacyPath_.setName("privacyPath");
		logPrivacyPath_.setPreferredSize(new Dimension(100,20));
		logPrivacyPath_.setCaretPosition(logPrivacyPath_.getText().length());
		logPrivacyPath_.setToolTipText(logPrivacyPath_.getText());
		logPrivacyPath_.addFocusListener(this);
		c.gridx = 1;
		add(logPrivacyPath_,c);
	      		
		//define FileFilter for fileChooser
		logFileFilter_ = new FileFilter(){
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				return f.getName().toLowerCase().endsWith(".log"); //$NON-NLS-1$
			}
			public String getDescription () { 
				return Messages.getString("EditLogControlPanel.logFiles") + " (*.log)"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		};

			
		//to consume the rest of the space
		c.weighty = 1.0;
		++c.gridy;
		add(new JPanel(), c);
	}
	
	public void setFilePath(JFormattedTextField textField){
		//begin with creation of new file
		JFileChooser fc = new JFileChooser();
		//set directory and ".log" filter
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setFileFilter(logFileFilter_);
		
		int status = fc.showDialog(this, Messages.getString("EditLogControlPanel.approveButton"));
		
		if(status == JFileChooser.APPROVE_OPTION){
				textField.setValue(fc.getSelectedFile().getAbsolutePath());
				textField.setToolTipText(fc.getSelectedFile().getAbsolutePath());
				textField.setCaretPosition(textField.getText().length());
		}
	}
	
	

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		if("privacyPath".equals(((JFormattedTextField) arg0.getSource()).getName()) && attackerFlag){
			attackerFlag = false;
			setFilePath(logPrivacyPath_);
		}
		else{
			logAttackerPath_.setCaretPosition(logAttackerPath_.getText().length());
			attackerFlag = true;
		}
		
		if("attackerPath".equals(((JFormattedTextField) arg0.getSource()).getName()) && privacyFlag) {
			privacyFlag = false;
			setFilePath(logAttackerPath_);
		}	
		else {
			logPrivacyPath_.setCaretPosition(logPrivacyPath_.getText().length());
			privacyFlag = true;
		}

	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		String command = arg0.getActionCommand();

		if("logAttackerData".equals(command)){	
			Vehicle.setAttackerDataLogged_(logAttackerCheckBox_.isSelected());
			if(logAttackerCheckBox_.isSelected()){
				AttackLogWriter.setLogPath(logAttackerPath_.getValue().toString());
				if(Renderer.getInstance().getAttackerVehicle() == null) JOptionPane.showMessageDialog(null, Messages.getString("AttackerPanel.MsgBox"), Messages.getString("AttackerPanel.MsgBoxTitel"), JOptionPane.OK_OPTION);
			}
			if(!logAttackerCheckBox_.isSelected()){
				encryptedLogging_.setSelected(false);
				Vehicle.setAttackerEncryptedDataLogged_(false);
			}
		}
		else if("encryptedLogging".equals(command)){
			Vehicle.setAttackerEncryptedDataLogged_(encryptedLogging_.isSelected());
			if(encryptedLogging_.isSelected() && !logAttackerCheckBox_.isSelected()){
				logAttackerCheckBox_.setSelected(true);
				Vehicle.setAttackerDataLogged_(logAttackerCheckBox_.isSelected());
				AttackLogWriter.setLogPath(logAttackerPath_.getValue().toString());
				if(Renderer.getInstance().getAttackerVehicle() == null) JOptionPane.showMessageDialog(null, Messages.getString("AttackerPanel.MsgBox"), Messages.getString("AttackerPanel.MsgBoxTitel"), JOptionPane.OK_OPTION);
			}
		}
		else if("logPrivacyData".equals(command)){
			Vehicle.setPrivacyDataLogged_(logPrivacyCheckBox_.isSelected());
			PrivacyLogWriter.setLogPath(logPrivacyPath_.getValue().toString());
		}

	}
	
	

	
	public JCheckBox getEncryptedLogging_() {
		return encryptedLogging_;
	}

	public JCheckBox getLogAttackerCheckBox_() {
		return logAttackerCheckBox_;
	}
	
	public void refreshGUI() {
		logAttackerCheckBox_.setSelected(Vehicle.isAttackerDataLogged_());
	}

	public JCheckBox getLogPrivacyCheckBox_() {
		return logPrivacyCheckBox_;
	}

	public JFormattedTextField getLogPrivacyPath_() {
		return logPrivacyPath_;
	}
}