package com.pettrade.practiceplatform.service.im;

import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;

public final class ImMessageTypeResolver {

    private ImMessageTypeResolver() {
    }

    public static ImMessageType parse(String type) {
        return ImMessageType.valueOf(type.trim().toUpperCase());
    }
}
