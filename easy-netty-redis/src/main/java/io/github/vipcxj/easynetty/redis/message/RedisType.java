package io.github.vipcxj.easynetty.redis.message;

public enum RedisType {
    SIMPLE_STRING('+'), ERROR('-'), INTEGER(':'), BULK_STRING('$'), ARRAY('*'), INLINE('\0');

    private final char sign;

    RedisType(char sign) {
        this.sign = sign;
    }

    public char sign() {
        return sign;
    }

    @Override
    public String toString() {
        return String.valueOf(sign);
    }
}
