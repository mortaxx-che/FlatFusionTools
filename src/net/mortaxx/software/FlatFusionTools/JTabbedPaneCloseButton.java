package net.mortaxx.software.FlatFusionTools;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.metal.MetalIconFactory;
import net.mortaxx.software.FlatFusionTools.FTLEditEventListener;

/**
 * @author 6dc
 *
 * A class which creates a JTabbedPane and auto sets a close button when you add a tab
 */
public class JTabbedPaneCloseButton extends JTabbedPane {

	private EventListenerList listenerList = null;
	
    public JTabbedPaneCloseButton() {
        super();
    }

    public JTabbedPaneCloseButton(int pos, int mode) {
        super(pos, mode);
        listenerList = new EventListenerList();
    }
    /* Override Addtab in order to add the close Button everytime */
    @Override
    public void addTab(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
        int count = this.getTabCount() - 1;
        setTabComponentAt(count, new CloseButtonTab(component, title, icon));
    }

    @Override
    public void addTab(String title, Icon icon, Component component) {
        addTab(title, icon, component, null);
    }

    @Override
    public void addTab(String title, Component component) {
        addTab(title, null, component);
    }

    /* addTabNoExit */
    public void addTabNoExit(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
    }

    public void addTabNoExit(String title, Icon icon, Component component) {
        addTabNoExit(title, icon, component, null);
    }

    public void addTabNoExit(String title, Component component) {
        addTabNoExit(title, null, component);
    }

    public void addListener(FTLEditEventListener aListener) {
    	listenerList.add(FTLEditEventListener.class, aListener);
    }

 // Methode zum Feuern eines Events
	 
 	protected boolean fireTabShouldClose()
 	{
 	     // Ermittle registrierte Listener
 	     Object[] listeners = listenerList.getListenerList();
 	     boolean bCanClose = true;

 	     // informiere alle Listener über das Event in rückwärtiger Folge
 	     for (int i = listeners.length-2; i>=0; i-=2) 
 	     {
 	         if (listeners[i]==FTLEditEventListener.class) {
 	             bCanClose = ((FTLEditEventListener)listeners[i+1]).handleJTabbedPaneClose();
 	         }
 	     }
 	     return bCanClose;
 	}
 	
    /* Button */
    public class CloseButtonTab extends JPanel {
        private Component tab;

        public CloseButtonTab(final Component tab, String title, Icon icon) {
            this.tab = tab;
            setOpaque(false);
            FlowLayout flowLayout = new FlowLayout(FlowLayout.CENTER, 3, 3);
            setLayout(flowLayout);
            JLabel jLabel = new JLabel(title);
            jLabel.setIcon(icon);
            add(jLabel);
            JButton button = new JButton(MetalIconFactory.getInternalFrameCloseIcon(16));
            button.setMargin(new Insets(5, 5, 5, 5));
            button.setBorder(null);
            button.addMouseListener(new CloseListener(tab));
            add(button);
        }
    }
    /* ClickListener */
    public class CloseListener implements MouseListener
    {
        private Component tab;

        public CloseListener(Component tab){
            this.tab=tab;
        }

        public void mouseClicked(MouseEvent e) {
            if(e.getSource() instanceof JButton){
                JButton clickedButton = (JButton) e.getSource();
                JTabbedPaneCloseButton tabbedPane = (JTabbedPaneCloseButton) clickedButton.getParent().getParent().getParent();
                if(tabbedPane.fireTabShouldClose() == true) {
                	tabbedPane.remove(tab);
                }
            }
        }

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}

        public void mouseEntered(MouseEvent e) {
            if(e.getSource() instanceof JButton){
                JButton clickedButton = (JButton) e.getSource();
             //   clickedButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,3));
            }
        }

        public void mouseExited(MouseEvent e) {
            if(e.getSource() instanceof JButton){
                JButton clickedButton = (JButton) e.getSource();
             //   clickedButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY,3));
            }
        }
    }
}