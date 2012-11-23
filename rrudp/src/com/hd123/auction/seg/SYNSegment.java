package com.hd123.auction.seg;

public class SYNSegment extends Segment {

	//默认值
	private static final int MAX_SEG_NUM = 0xff;
	private static final int OPT_FLAG = 0x0;
	private static final int MAX_SEG_SIZE = 0xffff;
	private static final int RE_TRANS_TIMEOUT = 300;
	private static final int ACC_TIMEOUT = 300;
	private static final int NULL_TIMEOUT = 300;
	private static final int STATE_TIMEOUT = 300;
	private static final int MAX_RE_TRANSTIMES = 3;
	private static final int MAX_ACK_NUM = 0xff;
	private static final int MAX_OUT_SEQ_SIZE = 8;
	private static final int MAX_AUTO_RESET = 8;
	// 同步段头部大小
	private static final int SYN_HEADER_LEN = RUDP_HEADER_LEN + 16;

	private int version;
	private int maxSegNum;
	private int optFlag;

	private int maxSegSize;
	private int reTransTimeout;

	private int accTimeout;
	private int nullTimeout;

	private int stateTimeout;
	private int maxReTransTimes;
	private int maxAckNum;

	private int maxOutSeqSize;
	private int maxAutoReset;

	public SYNSegment() {
	}

	public SYNSegment(int seqn) {
		this(seqn, MAX_SEG_NUM, OPT_FLAG, MAX_SEG_SIZE, RE_TRANS_TIMEOUT,
				ACC_TIMEOUT, NULL_TIMEOUT, STATE_TIMEOUT, MAX_RE_TRANSTIMES,
				MAX_ACK_NUM, MAX_OUT_SEQ_SIZE, MAX_AUTO_RESET);
	}

	public SYNSegment(int seqn, int maxSegNum, int optFlag, int maxSegSize,
			int reTransTimeout, int accTimeout, int nullTimeout,
			int stateTimeout, int maxReTransTimes, int maxAckNum,
			int maxOutSeqSize, int maxAutoReset) {
		init(SYN_FLAG, seqn, SYN_HEADER_LEN);

		this.version = RUDP_VERSION;
		this.maxSegNum = maxSegNum;
		this.optFlag = optFlag;
		this.maxSegSize = maxSegSize;
		this.reTransTimeout = reTransTimeout;
		this.accTimeout = accTimeout;
		this.nullTimeout = nullTimeout;
		this.stateTimeout = stateTimeout;
		this.maxReTransTimes = maxReTransTimes;
		this.maxAckNum = maxAckNum;
		this.maxOutSeqSize = maxOutSeqSize;
		this.maxAutoReset = maxAutoReset;
	}

	public byte[] getBytes() {
		byte[] buffer = super.getBytes();
		buffer[4] = (byte) ((version << 4) & 0xFF);
		buffer[5] = (byte) (maxSegNum & 0xFF);
		buffer[6] = (byte) (optFlag & 0xFF);
		buffer[7] = 0;
		buffer[8] = (byte) ((maxSegSize >>> 8) & 0xFF);
		buffer[9] = (byte) ((maxSegSize >>> 0) & 0xFF);
		buffer[10] = (byte) ((reTransTimeout >>> 8) & 0xFF);
		buffer[11] = (byte) ((reTransTimeout >>> 0) & 0xFF);
		buffer[12] = (byte) ((accTimeout >>> 8) & 0xFF);
		buffer[13] = (byte) ((accTimeout >>> 0) & 0xFF);
		buffer[14] = (byte) ((nullTimeout >>> 8) & 0xFF);
		buffer[15] = (byte) ((nullTimeout >>> 0) & 0xFF);
		buffer[16] = (byte) ((stateTimeout >>> 8) & 0xFF);
		buffer[17] = (byte) ((stateTimeout >>> 0) & 0xFF);
		buffer[18] = (byte) (maxReTransTimes & 0xFF);
		buffer[19] = (byte) (maxAckNum & 0xFF);
		buffer[20] = (byte) (maxOutSeqSize & 0xFF);
		buffer[21] = (byte) (maxAutoReset & 0xFF);

		return buffer;
	}

	protected void parseBytes(byte[] buffer, int off, int len) {
		super.parseBytes(buffer, off, len);

		if (len < (SYN_HEADER_LEN)) {
			throw new IllegalArgumentException("Invalid SYN segment");
		}

		version = ((buffer[off + 4] & 0xFF) >>> 4);
		if (version != RUDP_VERSION) {
			throw new IllegalArgumentException("Invalid RUDP version");
		}

		maxSegNum = (buffer[off + 5] & 0xFF);
		optFlag = (buffer[off + 6] & 0xFF);
		maxSegSize = ((buffer[off + 8] & 0xFF) << 8)
				| ((buffer[off + 9] & 0xFF) << 0);
		reTransTimeout = ((buffer[off + 10] & 0xFF) << 8)
				| ((buffer[off + 11] & 0xFF) << 0);
		accTimeout = ((buffer[off + 12] & 0xFF) << 8)
				| ((buffer[off + 13] & 0xFF) << 0);
		nullTimeout = ((buffer[off + 14] & 0xFF) << 8)
				| ((buffer[off + 15] & 0xFF) << 0);
		stateTimeout = ((buffer[off + 16] & 0xFF) << 8)
				| ((buffer[off + 17] & 0xFF) << 0);
		maxReTransTimes = (buffer[off + 18] & 0xFF);
		maxAckNum = (buffer[off + 19] & 0xFF);
		maxOutSeqSize = (buffer[off + 20] & 0xFF);
		maxAutoReset = (buffer[off + 21] & 0xFF);
	}

	public String type() {
		return "SYN";
	}

}
