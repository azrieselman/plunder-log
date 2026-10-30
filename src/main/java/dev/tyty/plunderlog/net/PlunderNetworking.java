package dev.tyty.plunderlog.net;

import dev.tyty.plunderlog.client.ClientJournal;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PlunderNetworking {
    private PlunderNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncJournalPayload.TYPE, SyncJournalPayload.STREAM_CODEC, PlunderNetworking::handle);
    }

    private static void handle(SyncJournalPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            // ClientJournal is only initialized when this runs, which is the client receive path.
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientJournal.accept(payload);
            }
        });
    }
}
