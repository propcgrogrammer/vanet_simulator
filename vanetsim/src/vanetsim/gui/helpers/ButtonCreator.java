package vanetsim.gui.helpers;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import vanetsim.ErrorLog;
import vanetsim.localization.Messages;

public final class ButtonCreator{
	/**
	 * Convenience method to get a JButton with an image on it.
	 * 
	 * @param imageName		the filename of the image relative to the <code>vanetsim/images</code> directory
	 * @param command		a command string which can be used to track events in <code>actionPerformed()</code>
	 * @param altString		an alternative string to display on the button when the original image can't be loaded and as a tooltip
	 * @param listener		an <code>ActionListener</code> which performs actions on button clicks
	 * 
	 * @return the <code>JButton</code> created
	 */
	public static JButton getJButton(String imageName, String command, String altString, ActionListener listener){
		JButton button;
		if(imageName.equals("")){ //$NON-NLS-1$
			button = new JButton(altString);
			button.setPreferredSize(new Dimension(42, 42));
		} else {
			URL url = ClassLoader.getSystemResource("vanetsim/images/" + imageName); //$NON-NLS-1$
			if (url != null){
				button = new JButton(new ImageIcon(url));
			} else {
				button = new JButton(altString);
				button.setPreferredSize(new Dimension(42, 42));
				ErrorLog.log(Messages.getString("ButtonCreator.imageNotFound") + imageName, 5, ButtonCreator.class.getName(), "getJButton", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		button.setToolTipText(altString);
		button.setActionCommand(command);
		button.addActionListener(listener);
		return button;
	}
}