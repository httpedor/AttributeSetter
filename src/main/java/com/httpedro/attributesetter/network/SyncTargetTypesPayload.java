package com.httpedro.attributesetter.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SyncTargetTypesPayload implements CustomPacketPayload {
    public static final Type<SyncTargetTypesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("attributesetter", "sync_target_types"));

    public static final StreamCodec<FriendlyByteBuf, SyncTargetTypesPayload> CODEC = new StreamCodec<FriendlyByteBuf, SyncTargetTypesPayload>() {
        private final Gson gson = new GsonBuilder().create();

        @Override
        public SyncTargetTypesPayload decode(FriendlyByteBuf buf) {
            int size = buf.readInt();
            Map<ResourceLocation, JsonElement> entries = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String namespace = buf.readUtf(32767);
                String path = buf.readUtf(32767);
                String json = buf.readUtf(32767);
                ResourceLocation res = ResourceLocation.fromNamespaceAndPath(namespace, path);
                entries.put(res, gson.fromJson(json, JsonElement.class));
            }
            return new SyncTargetTypesPayload(entries);
        }

        @Override
        public void encode(FriendlyByteBuf buf, SyncTargetTypesPayload payload) {
            buf.writeInt(payload.entries.size());
            for (var entry : payload.entries.entrySet()) {
                buf.writeUtf(entry.getKey().getNamespace(), 32767);
                buf.writeUtf(entry.getKey().getPath(), 32767);
                buf.writeUtf(gson.toJson(entry.getValue()), 32767);
            }
        }
    };

    private final Map<ResourceLocation, JsonElement> entries;

    public SyncTargetTypesPayload(Map<ResourceLocation, JsonElement> entries) {
        this.entries = entries;
    }

    public Map<ResourceLocation, JsonElement> getEntries() {
        return entries;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}



