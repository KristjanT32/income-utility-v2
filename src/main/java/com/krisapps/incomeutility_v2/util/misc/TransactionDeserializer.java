package com.krisapps.incomeutility_v2.util.misc;

import com.google.gson.*;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDeserializer implements JsonDeserializer<Transaction> {

    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .create();

    @Override
    public Transaction deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext ctx) {

        JsonObject obj = json.getAsJsonObject();

        if (obj.has("cashewTransactionId")) {
            return gson.fromJson(obj, CashewTransaction.class);
        }

        return gson.fromJson(obj, Transaction.class);
    }
}
