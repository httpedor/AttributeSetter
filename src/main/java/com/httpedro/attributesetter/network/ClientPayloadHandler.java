package com.httpedro.attributesetter.network;

import com.httpedro.attributesetter.DataReloader;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class ClientPayloadHandler implements IPayloadHandler<SyncTargetTypesPayload> {

    public static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    @Override
    public void handle(SyncTargetTypesPayload payload, IPayloadContext context) {
        // Schedule this on the main thread. The client goes through the same path as a reload so that the
        // client-side state (removed items in the creative tabs, item components) matches the server's; the
        // server-only passes are skipped, since what it sends is already filtered.
        context.enqueueWork(() -> {
            DataReloader reloader = new DataReloader();
            reloader.load(payload.getEntries(), false);
        });
    }
}




