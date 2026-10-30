package dev.tyty.plunderlog.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import dev.tyty.plunderlog.PlunderLog;
import dev.tyty.plunderlog.StructureNames;
import dev.tyty.plunderlog.net.JournalEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Optional Xaero's Minimap hook. Uses the same third-party waypoint path Xaero uses for Waystones
 * (Minimap 26.x). Missing classes are ignored so the journal still works.
 * World Map shows these marks when it is installed beside Minimap. World Map alone does not
 * expose this API, so marks are skipped in that case.
 */
public final class XaeroWaypoints {
    private static final ResourceLocation ORIGIN = ResourceLocation.fromNamespaceAndPath(PlunderLog.MOD_ID, "cleared");
    private static Api api;
    private static boolean unavailable;
    private static List<JournalEntry> pending = List.of();
    private static Component status = Component.empty();

    private XaeroWaypoints() {}

    public static boolean installed() {
        return ModList.get().isLoaded("xaerominimap") || ModList.get().isLoaded("xaeroworldmap");
    }

    public static Component status() {
        return status;
    }

    public static void offer(List<JournalEntry> entries) {
        pending = List.copyOf(entries);
        flush();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!pending.isEmpty()) {
            flush();
        }
    }

    private static void flush() {
        if (pending.isEmpty() || unavailable || !installed()) {
            return;
        }
        try {
            Api resolved = api == null ? (api = Api.load()) : api;
            if (resolved == null) {
                unavailable = true;
                status = Component.translatable("plunderlog.xaero.missing");
                return;
            }
            int added = resolved.addAll(pending);
            if (added >= 0) {
                pending = List.of();
                status = Component.translatable("plunderlog.xaero.added", added);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            unavailable = true;
            pending = List.of();
            status = Component.translatable("plunderlog.xaero.missing");
            PlunderLog.LOGGER.warn("Xaero waypoint API did not match this version; journal still works", exception);
        }
    }

    private static final class Api {
        final Method getCurrentSession;
        final Object minimapModule;
        final Method getWorldManager;
        final Method getAutoRoot;
        final Method getDimensionHelper;
        final Method getDimensionDirectoryName;
        final Method getPath;
        final Method resolve;
        final Method addSubContainer;
        final Method getThirdParty;
        final Method getGroup;
        final Method addWaypoint;
        final Method setVisibility;
        final Constructor<?> waypointCtor;
        final Object green;
        final Object normal;
        final Object global;

        private Api(
                Method getCurrentSession,
                Object minimapModule,
                Method getWorldManager,
                Method getAutoRoot,
                Method getDimensionHelper,
                Method getDimensionDirectoryName,
                Method getPath,
                Method resolve,
                Method addSubContainer,
                Method getThirdParty,
                Method getGroup,
                Method addWaypoint,
                Method setVisibility,
                Constructor<?> waypointCtor,
                Object green,
                Object normal,
                Object global) {
            this.getCurrentSession = getCurrentSession;
            this.minimapModule = minimapModule;
            this.getWorldManager = getWorldManager;
            this.getAutoRoot = getAutoRoot;
            this.getDimensionHelper = getDimensionHelper;
            this.getDimensionDirectoryName = getDimensionDirectoryName;
            this.getPath = getPath;
            this.resolve = resolve;
            this.addSubContainer = addSubContainer;
            this.getThirdParty = getThirdParty;
            this.getGroup = getGroup;
            this.addWaypoint = addWaypoint;
            this.setVisibility = setVisibility;
            this.waypointCtor = waypointCtor;
            this.green = green;
            this.normal = normal;
            this.global = global;
        }

        static Api load() throws ReflectiveOperationException {
            Class<?> modules = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            Object minimap = modules.getField("MINIMAP").get(null);
            Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Class<?> colorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
            Class<?> purposeClass = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");
            Class<?> visibilityClass = Class.forName("xaero.common.minimap.waypoints.WaypointVisibilityType");
            return new Api(
                    minimap.getClass().getMethod("getCurrentSession"),
                    minimap,
                    Class.forName("xaero.hud.minimap.module.MinimapSession").getMethod("getWorldManager"),
                    Class.forName("xaero.hud.minimap.world.MinimapWorldManager").getMethod("getAutoRootContainer"),
                    Class.forName("xaero.hud.minimap.module.MinimapSession").getMethod("getDimensionHelper"),
                    Class.forName("xaero.hud.minimap.world.MinimapDimensionHelper").getMethod("getDimensionDirectoryName", ResourceKey.class),
                    Class.forName("xaero.hud.minimap.world.container.MinimapWorldRootContainer").getMethod("getPath"),
                    Class.forName("xaero.hud.path.XaeroPath").getMethod("resolve", String.class),
                    Class.forName("xaero.hud.minimap.world.container.MinimapWorldRootContainer").getMethod("addSubContainer", Class.forName("xaero.hud.path.XaeroPath")),
                    Class.forName("xaero.hud.minimap.world.container.MinimapWorldContainer").getMethod("getThirdPartyWaypointManager"),
                    Class.forName("xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypointManager").getMethod("get", ResourceLocation.class),
                    Class.forName("xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints").getMethod("add", String.class, waypointClass),
                    waypointClass.getMethod("setVisibility", visibilityClass),
                    waypointClass.getConstructor(int.class, int.class, int.class, String.class, String.class, colorClass, purposeClass),
                    enumConstant(colorClass, "GREEN"),
                    enumConstant(purposeClass, "NORMAL"),
                    enumConstant(visibilityClass, "GLOBAL"));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object enumConstant(Class<?> type, String name) {
            return Enum.valueOf((Class) type, name);
        }

        int addAll(List<JournalEntry> entries) throws ReflectiveOperationException {
            Object session = getCurrentSession.invoke(minimapModule);
            if (session == null) {
                return -1;
            }
            Object root = getAutoRoot.invoke(getWorldManager.invoke(session));
            if (root == null) {
                return -1;
            }
            Object helper = getDimensionHelper.invoke(session);
            for (JournalEntry entry : entries) {
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, entry.dimension());
                String directory = (String) getDimensionDirectoryName.invoke(helper, dimension);
                Object path = resolve.invoke(getPath.invoke(root), directory);
                Object container = addSubContainer.invoke(root, path);
                Object group = getGroup.invoke(getThirdParty.invoke(container), ORIGIN);
                String name = limit(StructureNames.structure(entry.structure()), 32);
                Object waypoint = waypointCtor.newInstance(
                        entry.pos().getX(),
                        entry.pos().getY(),
                        entry.pos().getZ(),
                        name,
                        initials(name),
                        green,
                        normal);
                setVisibility.invoke(waypoint, global);
                addWaypoint.invoke(group, entry.waypointId(), waypoint);
            }
            return entries.size();
        }

    }

    private static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length() && out.length() < 2; i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(Character.toUpperCase(c));
            }
        }
        return out.isEmpty() ? "P" : out.toString();
    }

    private static String limit(String value, int max) {
        String cleaned = value.replace(':', ' ').trim();
        if (cleaned.isEmpty()) {
            return "Cleared";
        }
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
