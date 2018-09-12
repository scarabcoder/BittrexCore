package com.scarabcoder.bittrexcore.tick;

import com.google.common.base.CaseFormat;

public enum  TickInterval {

    ONE_MIN(60000L),
    FIVE_MIN(300000L),
    THIRTY_MIN(1800000L),
    HOUR(3600000L),
    DAY(86400000L);

    final long timeLength;

    TickInterval(Long timeLength) {
        this.timeLength = timeLength;
    }

    public long getTimeLength() {
        return timeLength;
    }

    public String getAPIName() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    }

}
