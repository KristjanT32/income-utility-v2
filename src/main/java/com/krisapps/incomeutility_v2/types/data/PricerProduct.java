package com.krisapps.incomeutility_v2.types.data;

import java.util.UUID;

public record PricerProduct(
        String name,
        Double price,
        Integer unitCount,
        String unitName,
        Double durationInDays,
        UUID id
) {
}
