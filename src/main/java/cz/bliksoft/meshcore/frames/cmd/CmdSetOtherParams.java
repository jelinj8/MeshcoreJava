package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertLocPolicy;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.TelemetryModeFlags;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetOtherParams extends CommandFrame {

	final boolean manualAddContacts;
	final boolean telemetryBaseAll;
	final boolean telemetryBaseFav;
	final boolean telemetryLocAll;
	final boolean telemetryLocFav;
	final boolean telemetryEnvAll;
	final boolean telemetryEnvFav;
	final AdvertLocPolicy advertLocPolicy;
	final boolean multiAcks;

	public CmdSetOtherParams(boolean manualAddContacts, boolean telemetryBaseAll, boolean telemetryBaseFav,
			boolean telemetryLocAll, boolean telemetryLocFav, boolean telemetryEnvAll, boolean telemetryEnvFav,
			AdvertLocPolicy advertLocPolicy, boolean multiAcks) {
		this.manualAddContacts = manualAddContacts;
		this.telemetryBaseAll = telemetryBaseAll;
		this.telemetryBaseFav = telemetryBaseFav;
		this.telemetryEnvAll = telemetryEnvAll;
		this.telemetryEnvFav = telemetryEnvFav;
		this.telemetryLocAll = telemetryLocAll;
		this.telemetryLocFav = telemetryLocFav;
		this.advertLocPolicy = advertLocPolicy;
		this.multiAcks = multiAcks;
	}

	@Override
	public CommandFrameType getFrameType() {
		return CommandFrameType.CMD_SET_OTHER_PARAMS;
	}

	@Override
	public byte[] getBytes() {
		ByteBuilder bb = new ByteBuilder();
		bb.put(getTypeCode());
		bb.put((byte) (manualAddContacts ? 0x01 : 0x00));
		byte policy = 0;
		if (telemetryBaseAll)
			policy |= TelemetryModeFlags.BASE_ALLOW_ALL.mask();
		if (telemetryBaseFav)
			policy |= TelemetryModeFlags.BASE_ALLOW_FAVORITES.mask();
		if (telemetryLocAll)
			policy |= TelemetryModeFlags.LOC_ALLOW_ALL.mask();
		if (telemetryLocFav)
			policy |= TelemetryModeFlags.LOC_ALLOW_FAVORITES.mask();
		if (telemetryEnvAll)
			policy |= TelemetryModeFlags.ENV_ALLOW_ALL.mask();
		if (telemetryEnvFav)
			policy |= TelemetryModeFlags.ENV_ALLOW_FAVORITES.mask();
		bb.put(policy);

		bb.put(advertLocPolicy.code());

		bb.put(((byte) (multiAcks ? 0x01 : 0x00)));

		return bb.toArray();
	}

	@Override
	public byte[] expectedResponses() {
		return EXPECTED_OK;
	}
}
