package info.openrocket.core.file.openrocket.importt;

import java.util.HashMap;

import info.openrocket.core.logging.WarningSet;
import info.openrocket.core.file.DocumentLoadingContext;
import info.openrocket.core.file.simplesax.AbstractElementHandler;
import info.openrocket.core.file.simplesax.ElementHandler;
import info.openrocket.core.file.simplesax.PlainTextHandler;
import info.openrocket.core.simulation.SimulationOptions;

import org.xml.sax.SAXException;

class AtmosphereHandler extends AbstractElementHandler {
	@SuppressWarnings("unused")
	private final DocumentLoadingContext context;
	private final String model;
	private double temperature = Double.NaN;
	private double pressure = Double.NaN;

	public AtmosphereHandler(String model, DocumentLoadingContext context) {
		this.model = model;
		this.context = context;
	}

	@Override
	public ElementHandler openElement(String element, HashMap<String, String> attributes,
			WarningSet warnings) {
		return PlainTextHandler.INSTANCE;
	}

	@Override
	public void closeElement(String element, HashMap<String, String> attributes,
			String content, WarningSet warnings) throws SAXException {

		double d = Double.NaN;
		try {
			d = Double.parseDouble(content);
		} catch (NumberFormatException ignore) {
		}

		if (element.equals("basetemperature")) {
			if (Double.isNaN(d)) {
				warnings.add("Illegal base temperature specified, ignoring.");
			}
			temperature = d;
		} else if (element.equals("basepressure")) {
			if (Double.isNaN(d)) {
				warnings.add("Illegal base pressure specified, ignoring.");
			}
			pressure = d;
		} else {
			super.closeElement(element, attributes, content, warnings);
		}
	}

	public void storeSettings(SimulationOptions cond, WarningSet warnings) {
		if (!Double.isNaN(pressure)) {
			cond.setLaunchPressure(pressure);
		}
		if (!Double.isNaN(temperature)) {
			cond.setLaunchTemperature(temperature);
		}

		if ("isa".equals(model)) {
			cond.setISAAtmosphere(true);
		} else if ("extendedisa".equals(model)) {
			cond.setISAAtmosphere(false);
		} else {
			cond.setISAAtmosphere(true);
			warnings.add("Unknown atmospheric model, using ISA.");
		}
	}

}