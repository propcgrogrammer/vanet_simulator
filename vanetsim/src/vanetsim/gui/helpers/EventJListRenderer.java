package vanetsim.gui.helpers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import vanetsim.scenario.events.Event;

/**
 * This class implements an own <code>ListCellRenderer</code> in order to display the list of events appropriately.
 */
public final class EventJListRenderer extends JPanel implements ListCellRenderer {
	
	/** The constant for serializing. */
	private static final long serialVersionUID = -4716099862947417497L;

	/**
	 * Overwriting the standard cell renderer to get the information in it the way we want.
	 * 
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
		JPanel pane = new JPanel();
		JLabel line1 = new JLabel();
		JLabel line2 = new JLabel();
		Event event = (Event)value;
		line1.setText(event.getTime() + " ms"); //$NON-NLS-1$
		line2.setText(event.getText());		

		line1.setForeground(new Color(0,0,0));
		line2.setForeground(event.getTextColor());

		pane.setLayout(new BoxLayout(pane,BoxLayout.Y_AXIS));
		pane.add(line1);
		pane.add(line2);

		if(isSelected) {
			pane.setBorder(BorderFactory.createLineBorder(new Color(0,0,0)));
			pane.setBackground(new Color(230,230,230));
		} else pane.setBackground(super.getBackground());
		return pane;
	}
}