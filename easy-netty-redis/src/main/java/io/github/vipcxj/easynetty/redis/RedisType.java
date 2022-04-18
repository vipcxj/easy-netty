package io.github.vipcxj.easynetty.redis;

public enum RedisType {
    SIMPLE_STRING('+'), ERROR('-'), INTEGER(':'), BULK_STRING('$'), ARRAY('*');

    private final char sign;

    RedisType(char sign) {
        this.sign = sign;
    }

    public char sign() {
        return sign;
    }
}
