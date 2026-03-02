package cz.bliksoft.meshcore.frames.cmd;

import cz.bliksoft.meshcore.frames.CommandFrame;
import cz.bliksoft.meshcore.frames.FrameConstants.AdvertLocPolicy;
import cz.bliksoft.meshcore.frames.FrameConstants.CommandFrameType;
import cz.bliksoft.meshcore.frames.FrameConstants.TelemetryModeFlags;
import cz.bliksoft.meshcore.utils.ByteBuilder;

public class CmdSetOtherParams extends CommandFrame {

	final boolean manualAddContacts;
	final boolean telemetryBaseEn;
	final boolean telemetryBaseFav;
	final boolean telemetryLocEn;
	final boolean telemetryLocFav;
	final boolean telemetryEnvEn;
	final boolean telemetryEnvFav;
	final AdvertLocPolicy advertLocPolicy;
	final boolean multiAcks;

	public CmdSetOtherParams(boolean manualAddContacts, boolean telemetryBaseEn, boolean telemetryBaseFav,
			boolean telemetryLocEn, boolean telemetryLocFav, boolean telemetryEnvEn, boolean telemetryEnvFav,
			AdvertLocPolicy advertLocPolicy, boolean multiAcks) {
		this.manualAddContacts = manualAddContacts;
		this.telemetryBaseEn = telemetryBaseEn;
		this.telemetryBaseFav = telemetryBaseFav;
		this.telemetryEnvEn = telemetryEnvEn;
		this.telemetryEnvFav = telemetryEnvFav;
		this.telemetryLocEn = telemetryLocEn;
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
		if (telemetryBaseEn)
			policy |= TelemetryModeFlags.BASE_ENABLED.mask();
		if (telemetryBaseFav)
			policy |= TelemetryModeFlags.BASE_FAVORITES_ONLY.mask();
		if (telemetryLocEn)
			policy |= TelemetryModeFlags.LOC_ENABLED.mask();
		if (telemetryLocFav)
			policy |= TelemetryModeFlags.LOC_FAVORITES_ONLY.mask();
		if (telemetryEnvEn)
			policy |= TelemetryModeFlags.ENV_ENABLED.mask();
		if (telemetryEnvFav)
			policy |= TelemetryModeFlags.ENV_FAVORITES_ONLY.mask();
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
