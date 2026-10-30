package dev.tyty.plunderlog;

import dev.tyty.plunderlog.item.PlunderJournalItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PlunderItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlunderLog.MOD_ID);

    public static final DeferredItem<PlunderJournalItem> JOURNAL = ITEMS.register("plunder_journal",
            () -> new PlunderJournalItem(new Item.Properties().stacksTo(1)));

    private PlunderItems() {}

    static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(JOURNAL);
        }
    }
}
