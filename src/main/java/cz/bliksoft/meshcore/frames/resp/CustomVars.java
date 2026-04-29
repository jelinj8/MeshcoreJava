package cz.bliksoft.meshcore.frames.resp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import cz.bliksoft.meshcore.companion.MeshcoreCompanion;
import cz.bliksoft.meshcore.frames.ResponseFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.ResponseFrameType;
import cz.bliksoft.meshcore.utils.ByteReader;

/**
 * Response to {@link cz.bliksoft.meshcore.frames.cmd.CmdGetCustomVars}
 * containing the device's custom key-value variables parsed from their
 * comma-separated wire encoding.
 */
public class CustomVars extends ResponseFrame {

	final Map<String, String> variables;

	/**
	 * @return ordered map of custom variable names to their string values
	 */
	public Map<String, String> getVariables() {
		return variables;
	}

	public CustomVars(MeshcoreCompanion source, byte[] data) {
		super(source, data);
		ByteReader br = new ByteReader(data);
		br.skip();

		String content = br.readFixedCString(512);
		variables = new LinkedHashMap<>();

		if (content == null || content.length() == 0)
			return;
		for (String part : content.split(",")) {
			String t = part.trim();
			if (t.isEmpty())
				continue;
			int ix = t.indexOf(':');
			if (ix < 0)
				variables.put(t, "");
			else
				variables.put(t.substring(0, ix).trim(), t.substring(ix + 1).trim());
		}
	}

	@Override
	public ResponseFrameType getFrameType() {
		return ResponseFrameType.RESP_CUSTOM_VARS;
	}

	@Override
	public String toString() {
		return "RESP_CUSTOM_VARS " + variables.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(","));
	}

}
