package vanetsim.gui.controlpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import vanetsim.gui.helpers.TextAreaLabel;
import vanetsim.localization.Messages;


/**
 * This class contains the control elements for display of statistics and mix zone information
 */
public final class AboutControlPanel extends JPanel{
	
	/** The necessary constant for serializing. */
	private static final long serialVersionUID = 5121979914528330821L;
	

	/**
	 * Constructor for this control panel.
	 */
	public AboutControlPanel(){
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 0.5;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.insets = new Insets(5,5,5,5);
		
		c.gridwidth = 1;
		
		//label for display of credits.
		++c.gridy;
		add(new JLabel("<html><b>" + Messages.getString("AboutDialog.creditsHeader") + "</b></html>"), c);
		++c.gridy;
		
		add(new TextAreaLabel(Messages.getString("AboutDialog.credits")), c);
		
		//to consume the rest of the space
		c.weighty = 1.0;
		++c.gridy;
		add(new JPanel(), c);
	}
}