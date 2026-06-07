package com.httpedro.attributesetter.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1.0").optional();

        registrar.playToClient(
                SyncTargetTypesPayload.TYPE,
                SyncTargetTypesPayload.CODEC,
                ClientPayloadHandler.INSTANCE
        );
    }
}




