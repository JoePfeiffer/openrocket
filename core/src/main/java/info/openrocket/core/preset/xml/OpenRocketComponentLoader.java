package info.openrocket.core.preset.xml;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.openrocket.core.file.Loader;
import info.openrocket.core.preset.ComponentPreset;
import info.openrocket.core.preset.InvalidComponentPresetException;
import info.openrocket.core.util.BugException;

public class OpenRocketComponentLoader implements Loader<ComponentPreset> {

	private static final Logger log = LoggerFactory.getLogger(OpenRocketComponentLoader.class);

	@Override
	public Collection<ComponentPreset> load(InputStream stream, String filename) {

		log.debug("Loading presets from file " + filename);

		if (!(stream instanceof BufferedInputStream)) {
			stream = new BufferedInputStream(stream);
		}

		try {
			List<ComponentPreset> presets;
			presets = (new OpenRocketComponentSaver().unmarshalFromOpenRocketComponent(new InputStreamReader(stream)))
					.asComponentPresets();
			log.debug("ComponentPreset file " + filename + " contained " + presets.size() + " presets");
			return presets;
		} catch (JAXBException | InvalidComponentPresetException e) {
			throw new BugException("Unable to parse file: " + filename, e);
		}

	}

}
