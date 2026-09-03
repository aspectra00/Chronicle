package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.gui.ToastInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("chronicle")
public final class ChronicleNeoForge {
    private static String version = "—";
    private final ChronicleClient client = new ChronicleClient();

    public ChronicleNeoForge() {
        version = ModList.get().getModContainerById("chronicle")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("—");
        client.initialize(FMLPaths.CONFIGDIR.get());
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::mousePressed);
        MinecraftForge.EVENT_BUS.addListener(this::shuttingDown);
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
        if (event.phase == TickEvent.Phase.END) client.tick(Minecraft.getInstance());
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
