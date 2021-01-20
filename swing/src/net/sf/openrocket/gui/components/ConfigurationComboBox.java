package net.sf.openrocket.gui.components;

import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.openrocket.rocketcomponent.Rocket;
import net.sf.openrocket.rocketcomponent.FlightConfiguration;
import net.sf.openrocket.rocketcomponent.FlightConfigurationId;

// combobox for flight configurations
public class ConfigurationComboBox extends JComboBox<FlightConfiguration> {
	private static final Logger log = LoggerFactory.getLogger(ConfigurationComboBox.class);

    public class ConfigurationModel implements MutableComboBoxModel<FlightConfiguration> {
		
		private final Rocket rkt;
		
		public ConfigurationModel(final Rocket _rkt) {
			this.rkt = _rkt;
		}
		
		@Override
		public FlightConfiguration getSelectedItem() {
			return rkt.getSelectedConfiguration();
		}
		
		@Override
		public void setSelectedItem(Object nextItem) {
			if( nextItem instanceof FlightConfiguration ){
				FlightConfigurationId selectedId = ((FlightConfiguration)nextItem).getId();
				rkt.setSelectedConfiguration(selectedId);
			}
		}
		
		@Override
		public FlightConfiguration getElementAt( final int configIndex) {
			return rkt.getFlightConfigurationByIndex(configIndex, true);
		}
		
		@Override
		public int getSize() {
			// plus the default config
			return rkt.getConfigurationCount()+1;
		}
		
		// ====== MutableComboBoxModel Functions ======
		// these functions don't need to do anything, just being a 'mutable' version of the combo box
		// is enough to allow updating the UI
		
		@Override
		public void addListDataListener(ListDataListener l) {}
		
		@Override
		public void removeListDataListener(ListDataListener l) {}
		
		@Override
		public void addElement(FlightConfiguration arg0) {}
		
		@Override
		public void insertElementAt(FlightConfiguration arg0, int arg1) {}
		
		@Override
		public void removeElement(Object arg0) {}
		
		@Override
		public void removeElementAt(int arg0) {}
		
	}
	
    private final Rocket rkt;

    public ConfigurationComboBox(Rocket _rkt) {
	rkt = _rkt;
	setModel(new ConfigurationModel(rkt));

	addPopupMenuListener(new PopupMenuListener()
	    {
		public void popupMenuCanceled(PopupMenuEvent e) {}
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
		
		public void popupMenuWillBecomeVisible(PopupMenuEvent e)
		{
		    setModel(new ConfigurationModel(rkt));		    
		}
		
	    });
	
    }
}
