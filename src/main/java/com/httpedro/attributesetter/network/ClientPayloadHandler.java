package com.httpedro.attributesetter.network;

import com.httpedro.attributesetter.DataReloader;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class ClientPayloadHandler implements IPayloadHandler<SyncTargetTypesPayload> {

    public static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    @Override
    public void handle(SyncTargetTypesPayload payload, IPayloadContext context) {
        // Schedule this on the main thread
        context.enqueueWork(() -> {
            DataReloader reloader = new DataReloader();
            for (var entry : payload.getEntries().entrySet()) {
                reloader.addEntry(entry.getKey(), entry.getValue());
            }
        });
    }
}




