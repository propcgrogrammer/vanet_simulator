package vanetsim.gui.controlpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import vanetsim.VanetSimStart;
import vanetsim.localization.Messages;

/**
 * A credits dialog
 */


public final class AboutDialog extends JDialog{
	
	/** The necessary constant for serializing. */
	private static final long serialVersionUID = -2918735209479587896L;

	
	/**
	 * Constructor. Creating GUI items.
	 */
	public AboutDialog(){
		//some JDialog options
		setUndecorated(true);
		setLayout(new GridBagLayout());
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		//WindowAdapter to catch closing event
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                    closeDialog();
            }
        }
        );  
        
		setModal(true);

		//some basic options
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.PAGE_START;
		c.weightx = 0.5;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 2;	
		c.insets = new Insets(5,5,5,5);
		
	
		++c.gridy;
		JLabel label = new JLabel(Messages.getString("AboutDialog.credits"));
		label.setSize(100, 100);
		add(label, c); //$NON-NLS-1$
	
/*
		//to consume the rest of the space
		c.weighty = 1.0;
		++c.gridy;
		*/
		add(new JPanel(), c);
		
		//resize window
		pack();
		//adjust window size
		setLocationRelativeTo(VanetSimStart.getMainFrame());
		//show window
		setVisible(true);
	}

	/**
	 * Methode is evoked when closing JDialog
	 */
	public void closeDialog(){
		//close JDialog
		this.dispose();
	}	
}