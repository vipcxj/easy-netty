package io.github.vipcxj.easynetty.redis.bus;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.utils.BytesUtils;
import io.github.vipcxj.jasync.ng.spec.JPromise;

@SuppressWarnings("unused")
public class RedisClusterMessage {
    private static final byte[] SIG = new byte[] {'R', 'C', 'm', 'b'};
    private static final int CLUSTER_SLOTS = 16384;
    private static final int CLUSTER_NAMELEN = 40;
    private static final int NET_IP_STR_LEN = 46;

    // char sig[4]; /* Signature "RCmb" (Redis Cluster message bus). */
    private static final int OFFSET_SIG = 0;
    // uint32_t totlen; /* Total length of this message */
    private static final int OFFSET_TOT_LEN = 4;
    // uint16_t ver; /* Protocol version, currently set to 1. */
    private static final int OFFSET_VER = 8;
    private static final int OFFSET_PORT = 10;
    private static final int OFFSET_TYPE = 12;
    private static final int OFFSET_COUNT = 14;
    private static final int OFFSET_CURRENT_EPOCH = 16;
    private static final int OFFSET_CONFIG_EPOCH = 24;
    private static final int OFFSET_OFFSET = 32;
    private static final int OFFSET_SENDER = 40;
    private static final int OFFSET_MY_SLOTS = 80;
    private static final int OFFSET_SLAVE_OF = 2128;
    private static final int OFFSET_MY_IP = 2168;
    private static final int OFFSET_EXTENSIONS = 2214;
    private static final int OFFSET_NOT_USED1 = 2216;
    private static final int OFFSET_P_PORT = 2246;
    private static final int OFFSET_C_PORT = 2248;
    private static final int OFFSET_FLAGS = 2250;
    private static final int OFFSET_STATE = 2252;
    private static final int OFFSET_M_FLAGS = 2253;
    private static final int OFFSET_DATA = 2256;

    // GOSSIP
    // char nodename[CLUSTER_NAMELEN]
    private static final int OFFSET_GOSSIP_NODE_NAME = 2256;
    // uint32_t ping_sent
    private static final int OFFSET_GOSSIP_PING_SENT = 2296;
    // uint32_t pong_received
    private static final int OFFSET_GOSSIP_PONG_RECEIVED = 2300;
    // char ip[NET_IP_STR_LEN]; /* IP address last time it was seen */
    private static final int OFFSET_GOSSIP_IP = 2304;
    // uint16_t port; /* base port last time it was seen */
    private static final int OFFSET_GOSSIP_PORT = 2344;
    // uint16_t cport; /* cluster port last time it was seen */
    private static final int OFFSET_GOSSIP_C_PORT = 2346;
    // uint16_t flags; /* node->flags copy */
    private static final int OFFSET_GOSSIP_FLAGS = 2348;
    // uint16_t pport; /* plaintext-port when base port is TLS */
    private static final int OFFSET_GOSSIP_P_PORT = 2350;
    // uint16_t notused1;
    private static final int OFFSET_GOSSIP_NOT_USED1 = 2352;

    // fail
    // char nodename[CLUSTER_NAMELEN];
    private static final int OFFSET_FAIL_NODE_NAME = 2256;

    // publish
    // uint32_t channel_len;
    private static final int OFFSET_PUBLISH_CHANNEL_LEN = 2256;
    // uint32_t message_len;
    private static final int OFFSET_PUBLISH_MESSAGE_LEN = 2260;
    // unsigned char bulk_data[8]; /* 8 bytes just as placeholder */
    private static final int OFFSET_PUBLISH_BULK_DATA = 2264;

    // update
    // uint64_t configEpoch; /* Config epoch of the specified instance */
    private static final int OFFSET_UPDATE_CONFIG_EPOCH = 2256;
    // char nodename[CLUSTER_NAMELEN]; /* Name of the slots owner */
    private static final int OFFSET_UPDATE_NODE_NAME = 2264;
    // unsigned char slots[CLUSTER_SLOT/8]; /* Slots bitmap. */
    private static final int OFFSET_UPDATE_SLOTS = 2304;

    // module
    // uint64_t module_id; /* ID of the sender module */
    private static final int OFFSET_MODULE_MODULE_ID = 2256;
    // uint32_t len;
    private static final int OFFSET_MODULE_LEN = 2264;
    // uint8_t type; /* Type from 0 to 255. */
    private static final int OFFSET_MODULE_TYPE = 2268;
    // unsigned char bulk_data[3]; /* 3 bytes just as placeholder */
    private static final int OFFSET_MODULE_BULK_DATA = 2269;

    private static final int CLUSTERMSG_TYPE_PING = 0;          /* Ping */
    private static final int CLUSTERMSG_TYPE_PONG = 1;          /* Pong (reply to Ping) */
    private static final int CLUSTERMSG_TYPE_MEET = 2;          /* Meet "let's join" message */
    private static final int CLUSTERMSG_TYPE_FAIL = 3;          /* Mark node xxx as failing */
    private static final int CLUSTERMSG_TYPE_PUBLISH = 4;       /* Pub/Sub Publish propagation */
    private static final int CLUSTERMSG_TYPE_FAILOVER_AUTH_REQUEST = 5; /* May I failover? */
    private static final int CLUSTERMSG_TYPE_FAILOVER_AUTH_ACK = 6;     /* Yes, you have my vote */
    private static final int CLUSTERMSG_TYPE_UPDATE = 7;        /* Another node slots configuration */
    private static final int CLUSTERMSG_TYPE_MFSTART = 8;       /* Pause clients for manual failover */
    private static final int CLUSTERMSG_TYPE_MODULE = 9;        /* Module cluster API message. */
    private static final int CLUSTERMSG_TYPE_PUBLISHSHARD = 10; /* Pub/Sub Publish shard propagation */

    protected final EasyNettyContext context;
    protected byte[] header;
    protected byte[] data;
    protected DateType dateType;
    private boolean complete;

    public RedisClusterMessage(EasyNettyContext context) {
        this.context = context;
    }

    public JPromise<Void> readHeader() {
        if (header == null) {
            header = context.readBytes(OFFSET_DATA).await();
            int type = getType();
            switch (type) {
                case CLUSTERMSG_TYPE_PING:
                case CLUSTERMSG_TYPE_PONG:
                case CLUSTERMSG_TYPE_MEET:
                    dateType = DateType.GOSSIP;
                    break;
                case CLUSTERMSG_TYPE_UPDATE:
                    dateType = DateType.UPDATE;
                    break;
                case CLUSTERMSG_TYPE_FAIL:
                    dateType = DateType.FAIL;
                    break;
                case CLUSTERMSG_TYPE_PUBLISH:
                case CLUSTERMSG_TYPE_PUBLISHSHARD:
                    dateType = DateType.PUBLISH;
                    break;
                case CLUSTERMSG_TYPE_MODULE:
                    dateType = DateType.MODULE;
                    break;
                default:
                    dateType = DateType.UNKNOWN;
            }
        }
        return JPromise.empty();
    }

    public JPromise<Void> readBody() {
        readHeader().await();
        long totalLen = getTotalLen();
        long bodyLen = totalLen - OFFSET_DATA;
        this.data = context.readBytes((int) bodyLen).await();
        complete = true;
        return JPromise.empty();
    }

    private void assetHeaderReady() {
        if (header == null) {
            throw new IllegalStateException("Call `readHeader().await()` at first.");
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public long getTotalLen() {
        assetHeaderReady();
        // uint32_t totlen;    /* Total length of this message */
        return BytesUtils.getUnsignedIntLE(header, OFFSET_TOT_LEN);
    }

    public int getProtocolVersion() {
        assetHeaderReady();
        // uint16_t ver;       /* Protocol version, currently set to 1. */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_VER);
    }

    public int getPort() {
        assetHeaderReady();
        // uint16_t port;      /* TCP base port number. */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_PORT);
    }

    public int getType() {
        assetHeaderReady();
        // uint16_t type;      /* Message type */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_TYPE);
    }

    public int getCount() {
        assetHeaderReady();
        // uint16_t count;     /* Only used for some kind of messages. */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_COUNT);
    }

    public long getCurrentEpoch() {
        assetHeaderReady();
        // uint64_t currentEpoch;  /* The epoch accordingly to the sending node. */
        return BytesUtils.getLongLE(header, OFFSET_CURRENT_EPOCH);
    }

    public long getConfigEpoch() {
        assetHeaderReady();
        // uint64_t configEpoch;
        return BytesUtils.getLongLE(header, OFFSET_CONFIG_EPOCH);
    }

    public long getOffset() {
        assetHeaderReady();
        // uint64_t offset;
        return BytesUtils.getLongLE(header, OFFSET_OFFSET);
    }

    public String getSender() {
        assetHeaderReady();
        // char sender[CLUSTER_NAMELEN]; /* Name of the sender node */
        return BytesUtils.getString(header, OFFSET_SENDER, CLUSTER_NAMELEN);
    }

    public String getSlaveOf() {
        assetHeaderReady();
        // char slaveof[CLUSTER_NAMELEN];
        return BytesUtils.getString(header, OFFSET_SLAVE_OF, CLUSTER_NAMELEN);
    }

    public String getMyIp() {
        assetHeaderReady();
        // char myip[NET_IP_STR_LEN];    /* Sender IP, if not all zeroed. */
        return BytesUtils.getString(header, OFFSET_MY_IP, NET_IP_STR_LEN);
    }

    public int getExtensions() {
        assetHeaderReady();
        // uint16_t extensions; /* Number of extensions sent along with this packet. */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_EXTENSIONS);
    }

    public int getPlainTextPort() {
        assetHeaderReady();
        // uint16_t pport;      /* Sender TCP plaintext port, if base port is TLS */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_P_PORT);
    }

    public int getClusterBusPort() {
        assetHeaderReady();
        // uint16_t cport;      /* Sender TCP cluster bus port */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_C_PORT);
    }

    public int getFlags() {
        assetHeaderReady();
        // uint16_t flags;      /* Sender node flags */
        return BytesUtils.getUnsignedShortLE(header, OFFSET_FLAGS);
    }

    public int getStateAndMFlags() {
        assetHeaderReady();
        // unsigned char state; /* Cluster state from the POV of the sender */
        // unsigned char mflags[3]; /* Message flags: CLUSTERMSG_FLAG[012]_... */
        return BytesUtils.getIntLE(header, OFFSET_STATE);
    }

    public enum DateType {
        GOSSIP, FAIL, PUBLISH, UPDATE, MODULE, UNKNOWN
    }
}
