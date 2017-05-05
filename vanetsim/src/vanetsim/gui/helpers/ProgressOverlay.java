package vanetsim.gui.helpers;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;

import vanetsim.VanetSimStart;
import vanetsim.localization.Messages;

/**
 * A ProgressBar overlay in an undecorated <code>JDialog</code>. This should be activated when longer calculations are
 * made. The user can't click anywhere else while it's visible.
 */
public final class ProgressOverlay extends JDialog implements ActionListener{
	
	/** The necessary constant for serializing. */
	private static final long serialVersionUID = 6272889496006127410L;

	/**
	 * Instantiates a new progress bar.
	 */
	public ProgressOverlay(){		
		super(VanetSimStart.getMainFrame());
		getRootPane().setWindowDecorationStyle(JRootPane.NONE); 
		JProgressBar progressBar = new JProgressBar(0, 100);      
        progressBar.setIndeterminate(true);
        setLayout(new BorderLayout());
        add(progressBar, BorderLayout.PAGE_START);
        add(ButtonCreator.getJButton("shutdown.png", "shutdown", Messages.getString("ProgressOverlay.quitProgram"), this), BorderLayout.PAGE_END); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        pack();       
        setVisible(false);
	}
	
	/** 
	 * Overwriting the original setVisible function to always set position in center of screen if it's set to visible.
	 * 
	 * @see java.awt.Window#setVisible(boolean)
	 */
	public void setVisible(boolean state){
		if(state == true){
			VanetSimStart.getMainFrame().setEnabled(false);
			Point p = VanetSimStart.getMainFrame().getLocationOnScreen();
			setLocation((VanetSimStart.getMainFrame().getBounds().width - getBounds().width) / 2 + p.x,(VanetSimStart.getMainFrame().getBounds().height - getBounds().height) / 2 + p.y);
		} else VanetSimStart.getMainFrame().setEnabled(true);
        super.setVisible(state);		
	}

	/**
	 * An implemented <code>ActionListener</code> which allows to exit the program when the Quit-button
	 * is clicked.
	 * 
	 * @param e	an <code>ActionEvent</code>
	 */
	public void actionPerformed(ActionEvent e) {
		System.exit(ABORT);		
	}
}