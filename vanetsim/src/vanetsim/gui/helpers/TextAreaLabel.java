package vanetsim.gui.helpers;

import java.awt.Dimension;

import javax.swing.JTextArea;

/**
 * A class which uses a <code>JTextArea</code> to imitate a <code>JLabel</code> with automatic linewrap.
 */
public final class TextAreaLabel extends JTextArea{
	
	/** The necessary constant for serializing. */
	private static final long serialVersionUID = 6703416429165263141L;

	/**
	 * Instantiates a new <code>JTextArea</code> with automatic linewrap.
	 * 
	 * @param text the text
	 */
	public TextAreaLabel(String text){
		super(text);
		setOpaque(false);
		setBorder(null);
		setFocusable(false);
		setWrapStyleWord(true);
		setLineWrap(true);
	}
	
	/**
	 * Return a little bit smaller width than original JTextArea would. This should help
	 * to compensate appearing scrollbars on the right side of this area. 
	 * 
	 * @return the modified preferred size
	 */
	public Dimension getPreferredSize(){
		Dimension dim = super.getPreferredSize();
		dim.width -= 6;
		return dim;
	}
}