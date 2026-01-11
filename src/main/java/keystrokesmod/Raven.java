package keystrokesmod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import keystrokesmod.keystroke.KeySrokeRenderer;
import keystrokesmod.keystroke.KeyStrokeConfigGui;
import keystrokesmod.keystroke.keystrokeCommand;
import keystrokesmod.module.Module;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.script.ScriptManager;
import keystrokesmod.utility.*;
import keystrokesmod.utility.clicks.CPSCalculator;
import keystrokesmod.utility.i18n.I18nManager;
import keystrokesmod.utility.profile.Profile;
import keystrokesmod.utility.profile.ProfileManager;
import keystrokesmod.utility.render.blur.HudBlurBatcher;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.jetbrains.annotations.NotNull;

@Mod(
        modid = "keystrokes",
        name = "KeystrokesMod",
        version = "KMV5",
        acceptedMinecraftVersions = "[1.8.9]"
)
public class Raven {
    public static boolean debugger = false;
    public static Minecraft mc = Minecraft.getMinecraft();
    private static KeySrokeRenderer keySrokeRenderer;
    private static boolean isKeyStrokeConfigGuiToggled;
    private static final ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);
    public static ModuleManager moduleManager;
    public static ClickGui clickGui;
    public static ProfileManager profileManager;
    public static ScriptManager scriptManager;
    public static Profile currentProfile;
    public static BadPacketsHandler badPacketsHandler;
    private boolean loaded = false;

    public static int moduleCounter;
    public static int settingCounter;

    /**
     * Keybind polling used as a safety fallback (e.g., mouse binds)
     * while avoiding per-frame full scans at high FPS.
     */
    private static final long KEYBIND_POLL_INTERVAL_MS = 25L; // ~40Hz
    private static volatile long lastKeybindPollMs = 0L;

    public Raven() {
        moduleManager = new ModuleManager();
    }

    @EventHandler
    public void init(FMLInitializationEvent ignored) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Ensure executor is stopped
            ex.shutdown();
            // Auto-save current config to the latest profile when the game is closing
            try {
                if (profileManager != null) {
                    Profile profileToSave = currentProfile;
                    if (profileToSave == null && !profileManager.getProfileFiles().isEmpty()) {
                        // Fallback to default if for some reason currentProfile is null
                        profileToSave = profileManager.getProfile("latest");
                        if (profileToSave == null) {
                            profileToSave = profileManager.getProfile("default");
                        }
                    }
                    if (profileToSave != null) {
                        profileManager.saveProfile(profileToSave);
                    }
                }
            } catch (Throwable ignored1) {
            }
        }));
        ClientCommandHandler.instance.registerCommand(new keystrokeCommand());
        FMLCommonHandler.instance().bus().register(this);
        FMLCommonHandler.instance().bus().register(new DebugInfoRenderer());
        FMLCommonHandler.instance().bus().register(new CPSCalculator());
        FMLCommonHandler.instance().bus().register(new KeySrokeRenderer());
        FMLCommonHandler.instance().bus().register(new Ping());
        FMLCommonHandler.instance().bus().register(badPacketsHandler = new BadPacketsHandler());
        FMLCommonHandler.instance().bus().register(new HudBlurBatcher());
        Reflection.getFields();
        Reflection.getMethods();
        moduleManager.register();
        scriptManager = new ScriptManager();
        keySrokeRenderer = new KeySrokeRenderer();
        clickGui = new ClickGui();
        profileManager = new ProfileManager();
        profileManager.loadProfiles();
        profileManager.loadProfile();
        Reflection.setKeyBindings();
        scriptManager.loadScripts();
        FMLCommonHandler.instance().bus().register(ModuleManager.tower);
        FMLCommonHandler.instance().bus().register(ModuleManager.rotationHandler);
        FMLCommonHandler.instance().bus().register(ModuleManager.slotHandler);
        FMLCommonHandler.instance().bus().register(ModuleManager.dynamicManager);

        I18nManager.init();
        AutoUpdate.init();
    }

    @SubscribeEvent
    public void onTick(@NotNull ClientTickEvent e) {
        if (e.phase == Phase.END) {
            try {
                if (Utils.nullCheck()) {
                    if (Reflection.sendMessage) {
                        Utils.sendMessage("&cThere was an error, relaunch the game.");
                        Reflection.sendMessage = false;
                    }
                    for (Module module : getModuleManager().getModules()) {
                        if (mc.currentScreen instanceof ClickGui) {
                            module.guiUpdate();
                        }

                        if (module.isEnabled()) {
                            module.onUpdate();
                        }
                    }
                }

                if (isKeyStrokeConfigGuiToggled) {
                    isKeyStrokeConfigGuiToggled = false;
                    mc.displayGuiScreen(new KeyStrokeConfigGui());
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(@NotNull InputEvent.KeyInputEvent event) {
        // Event-driven toggles (fast + avoids per-frame scans)
        try {
            if (Utils.nullCheck() && mc.currentScreen == null) {
                pollKeybinds();
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase == Phase.END) {
            try {
                if (Utils.nullCheck()) {
                    // Safety fallback for mouse binds / edge cases:
                    // throttle to avoid scanning all modules every frame at high FPS.
                    long now = System.currentTimeMillis();
                    if (mc.currentScreen == null && (now - lastKeybindPollMs) >= KEYBIND_POLL_INTERVAL_MS) {
                        lastKeybindPollMs = now;
                        pollKeybinds();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void pollKeybinds() {
        for (Module module : getModuleManager().getModules()) {
            if (module.canBeEnabled()) {
                module.keybind();
            }
        }
        synchronized (Raven.profileManager.profiles) {
            for (Profile profile : Raven.profileManager.profiles) {
                profile.getModule().keybind();
            }
        }
        for (Module module : Raven.scriptManager.scripts.values()) {
            module.keybind();
        }
    }

    public static ScheduledExecutorService getExecutor() {
        return ex;
    }

    public static KeySrokeRenderer getKeyStrokeRenderer() {
        return keySrokeRenderer;
    }

    public static void toggleKeyStrokeConfigGui() {
        isKeyStrokeConfigGuiToggled = true;
    }

    // Explicit getter to avoid Lombok dependency
    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
}
