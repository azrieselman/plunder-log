package dev.tyty.plunderlog.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import dev.tyty.plunderlog.StructureNames;
import dev.tyty.plunderlog.net.JournalEntry;
import dev.tyty.plunderlog.world.ClearTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class JournalScreen extends Screen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final int ROW = 28;

    private final List<JournalEntry> entries;
    private int scroll;

    public JournalScreen(List<JournalEntry> entries) {
        super(Component.translatable("plunderlog.journal.title"));
        this.entries = List.copyOf(entries);
    }

    @Override
    protected void init() {
        if (XaeroWaypoints.installed()) {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(this.width / 2 - 154, this.height - 28, 150, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("plunderlog.journal.waypoints"), button -> XaeroWaypoints.offer(entries))
                    .bounds(this.width / 2 + 4, this.height - 28, 150, 20)
                    .build());
        } else {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        Component count = entries.isEmpty()
                ? Component.translatable("plunderlog.journal.empty")
                : Component.translatable("plunderlog.journal.count", entries.size());
        graphics.drawCenteredString(this.font, count, this.width / 2, 26, 0xCFCFCF);

        int top = 42;
        int bottom = this.height - 44;
        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("plunderlog.journal.hint"), this.width / 2, top + 12, 0xA0A0A0);
            return;
        }

        int maxScroll = maxScroll(top, bottom);
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll);
        graphics.enableScissor(16, top, this.width - 16, bottom);
        int y = top - this.scroll;
        for (JournalEntry entry : entries) {
            if (y + ROW >= top && y <= bottom) {
                drawEntry(graphics, entry, 24, y);
            }
            y += ROW;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = top;
            int trackHeight = bottom - top;
            int thumb = Math.max(12, trackHeight * trackHeight / (entries.size() * ROW));
            int thumbY = trackTop + (trackHeight - thumb) * this.scroll / maxScroll;
            graphics.fill(this.width - 10, trackTop, this.width - 6, bottom, 0x40FFFFFF);
            graphics.fill(this.width - 10, thumbY, this.width - 6, thumbY + thumb, 0xFFD0C080);
        }

        if (XaeroWaypoints.installed()) {
            graphics.drawCenteredString(this.font, XaeroWaypoints.status(), this.width / 2, this.height - 42, 0xA0A0A0);
        }
    }

    private void drawEntry(GuiGraphics graphics, JournalEntry entry, int x, int y) {
        String name = StructureNames.structure(entry.structure());
        graphics.drawString(this.font, name, x, y, 0xFFE0C36A, false);
        String reason = ClearTracker.REASON_BOSS.equals(entry.reason())
                ? Component.translatable("plunderlog.reason.boss").getString()
                : Component.translatable("plunderlog.reason.chests").getString();
        String detail = StructureNames.dimension(entry.dimension())
                + "  " + entry.pos().getX() + ", " + entry.pos().getY() + ", " + entry.pos().getZ()
                + "  " + when(entry)
                + "  " + reason;
        graphics.drawString(this.font, this.font.plainSubstrByWidth(detail, this.width - x - 24), x, y + 12, 0xFFD0D0D0, false);
    }

    private int maxScroll(int top, int bottom) {
        return Math.max(0, entries.size() * ROW - (bottom - top));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll(42, this.height - 44);
        if (max <= 0) {
            return false;
        }
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY) * ROW, 0, max);
        return true;
    }

    private static String when(JournalEntry entry) {
        String clock = CLOCK.format(Instant.ofEpochMilli(entry.clearedAtMillis()));
        long day = entry.gameTime() / 24000L;
        return clock + ", day " + day;
    }
}
