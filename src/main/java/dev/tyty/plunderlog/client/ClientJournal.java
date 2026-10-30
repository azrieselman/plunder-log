package dev.tyty.plunderlog.client;

import dev.tyty.plunderlog.net.SyncJournalPayload;
import net.minecraft.client.Minecraft;

public final class ClientJournal {
    private ClientJournal() {}

    public static void accept(SyncJournalPayload payload) {
        XaeroWaypoints.offer(payload.entries());
        if (payload.openScreen()) {
            Minecraft.getInstance().setScreen(new JournalScreen(payload.entries()));
        }
    }
}
