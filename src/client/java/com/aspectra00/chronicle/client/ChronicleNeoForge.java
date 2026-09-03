package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.gui.ToastInteractionManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.TickEvent;

@Mod("chronicle")
public final class ChronicleNeoForge {
    private static String version = "—";
    private final ChronicleClient client;

    public ChronicleNeoForge(IEventBus modBus, ModContainer container) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            client = null;
            return;
        }
        client = new ChronicleClient();
        version = container.getModInfo().getVersion().toString();
        client.initialize(FMLPaths.CONFIGDIR.get());
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::mousePressed);
        NeoForge.EVENT_BUS.addListener(this::shuttingDown);
    }

    public static String version() {
        return version;
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ChronicleClient.OPEN_MENU_KEY);
        event.register(ChronicleClient.WATCH_TARGET_KEY);
        event.register(ChronicleClient.INTERACT_TOAST_KEY);
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            client.tick(Minecraft.getInstance());
        }
    }

    private void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ToastInteractionManager.handleMouseClick(
                Minecraft.getInstance(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    private void shuttingDown(GameShuttingDownEvent event) {
        client.close(Minecraft.getInstance());
    }
}
