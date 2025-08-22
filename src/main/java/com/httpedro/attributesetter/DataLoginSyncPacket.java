package com.httpedro.attributesetter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.HashMap;
import java.util.Map;

import static com.httpedro.attributesetter.Attributesetter.dr;

public record DataLoginSyncPacket(Map<ResourceLocation, JsonObject> entries) implements CustomPacketPayload {

    public static final Type<DataLoginSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Attributesetter.MODID, "attributesetterloginpacket"));

    public static final StreamCodec<ByteBuf, DataLoginSyncPacket> DECODER = StreamCodec.ofMember(
            DataLoginSyncPacket::write,
            DataLoginSyncPacket::read
    );

    public static final IPayloadHandler<DataLoginSyncPacket> HANDLER = (packet, ctx) -> {
        ctx.enqueueWork(() -> {
            for (Map.Entry<ResourceLocation, JsonObject> entry : packet.entries.entrySet()) {
                dr.addEntry(entry.getKey(), entry.getValue());
            }
        });
    };

    public static DataLoginSyncPacket read(ByteBuf bbuf) {
        HashMap<ResourceLocation, JsonObject> map = new HashMap<>();
        var buf = new FriendlyByteBuf(bbuf);
        buf.readMap(i -> map, ResourceLocation.STREAM_CODEC, (res) -> JsonParser.parseString(res.readUtf()).getAsJsonObject());
        return new DataLoginSyncPacket(map);
    }

    public void write(ByteBuf bbuf) {
        var buf = new FriendlyByteBuf(bbuf);
        buf.writeMap(entries, ResourceLocation.STREAM_CODEC, (b, json) -> b.writeUtf(json.toString()));
    }


    @Override
    public Type<DataLoginSyncPacket> type() {
        return TYPE;
    }
}

