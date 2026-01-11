package keystrokesmod.mixins.impl.client;

import keystrokesmod.event.ClickEvent;
import keystrokesmod.event.PreTickEvent;
import keystrokesmod.event.RightClickEvent;
import keystrokesmod.event.WorldChangeEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.HitBox;
import keystrokesmod.module.impl.combat.Reach;
import keystrokesmod.module.impl.exploit.ExploitFixer;
import keystrokesmod.module.impl.render.Animations;
import keystrokesmod.module.impl.render.FreeLook;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static keystrokesmod.Raven.mc;

@Mixin(value = Minecraft.class, priority = 1001)
public abstract class MixinMinecraft {

    @Unique private @Nullable WorldClient raven_APlus$lastWorld = null;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void runTickPre(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PreTickEvent());

        if (raven_APlus$lastWorld != mc.theWorld && Utils.nullCheck()) {
            MinecraftForge.EVENT_BUS.post(new WorldChangeEvent());
        }

        this.raven_APlus$lastWorld = mc.theWorld;
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;onStoppedUsingItem(Lnet/minecraft/entity/player/EntityPlayer;)V",
            shift = At.Shift.BY, by = 2
    ))
    private void onRunTick$usingWhileDigging(CallbackInfo ci) {
        if (ModuleManager.animations != null && ModuleManager.animations.isEnabled() && Animations.swingWhileDigging.isToggled()
                && mc.gameSettings.keyBindAttack.isKeyDown()) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                mc.thePlayer.swingItem();
            }
        }
    }

    @Inject(method = "clickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingItem()V"), cancellable = true)
    private void beforeSwingByClick(CallbackInfo ci) {
        ClickEvent event = new ClickEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            ci.cancel();
    }

    /**
     * @author xia__mc
     * @reason to fix reach and hitBox won't work with autoClicker
     */
    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void onLeftClickMouse(CallbackInfo ci) {
        FreeLook.call();
        Reach.call();
        HitBox.call();
    }

    /**
     * @author xia__mc
     * @reason to fix freelook do impossible action
     */
    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClickMouse(CallbackInfo ci) {
        RightClickEvent event = new RightClickEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            ci.cancel();
    }

    @Inject(method = "crashed", at = @At("HEAD"), cancellable = true)
    private void onCrashed(CrashReport crashReport, CallbackInfo ci) {
        try {
            if (ExploitFixer.onCrash(crashReport)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Remove framerate limit on all GUIs (not in-game)
     * Based on: SmoothMenu, MenuFPSUnlocker mods
     * Allows smooth rendering at full FPS in menus, options, inventory, etc.
     */
    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void onGetLimitFramerate(CallbackInfoReturnable<Integer> cir) {
        // Check if we're in a menu/GUI context (not in-game)
        // - mc.currentScreen != null: any GUI screen is open (inventory, options, etc.)
        // - mc.theWorld == null: we're not in a world (main menu, loading screens, etc.)
        // This ensures FPS is unlimited even if currentScreen is temporarily null during transitions
        if (mc.currentScreen != null || mc.theWorld == null) {
            // Return unlimited framerate (260 is Minecraft's "unlimited" value)
            // This allows smooth rendering at high refresh rates in all GUIs
            cir.setReturnValue(260);
        }
    }

    /**
     * Also override isFramerateLimitBelowMax to allow high FPS in all GUIs
     */
    @Inject(method = "isFramerateLimitBelowMax", at = @At("HEAD"), cancellable = true)
    private void onIsFramerateLimitBelowMax(CallbackInfoReturnable<Boolean> cir) {
        // If we're in a menu/GUI context, treat framerate as not limited (allows high FPS)
        // - mc.currentScreen != null: any GUI screen is open
        // - mc.theWorld == null: not in a world (main menu, loading, etc.)
        // This applies to main menu, options, inventory, clickgui, etc.
        if (mc.currentScreen != null || mc.theWorld == null) {
            cir.setReturnValue(false);
        }
    }

}
