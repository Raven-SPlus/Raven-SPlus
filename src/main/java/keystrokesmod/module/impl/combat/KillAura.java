package keystrokesmod.module.impl.combat;

import keystrokesmod.event.*;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.autoclicker.DragClickAutoClicker;
import keystrokesmod.module.impl.combat.autoclicker.IAutoClicker;
import keystrokesmod.module.impl.combat.autoclicker.NormalAutoClicker;
import keystrokesmod.module.impl.combat.autoclicker.RecordAutoClicker;
import keystrokesmod.module.impl.combat.killaura.KillAuraAutoBlock;
import keystrokesmod.module.impl.combat.killaura.KillAuraRotation;
import keystrokesmod.module.impl.combat.killaura.KillAuraTargeting;
import keystrokesmod.module.impl.other.RecordClick;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.impl.player.Blink;
import keystrokesmod.module.impl.player.antivoid.HypixelAntiVoid;
import keystrokesmod.module.impl.exploit.disabler.IntaveDisabler;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.*;
import keystrokesmod.module.setting.utils.ModeOnly;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.*;
import keystrokesmod.utility.aim.AimSimulator;
import lombok.Getter;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class KillAura extends IAutoClicker {
    public static EntityLivingBase target;
    
    // Settings
    public final ModeValue clickMode;
    public final ModeSetting autoBlockMode;
    public final SliderSetting fov;
    public final ModeSetting attackMode;
    public final SliderSetting attackRange;
    public final SliderSetting swingRange;
    public final SliderSetting blockRange;
    public final SliderSetting preAimRange;

    public final ModeSetting pointRandomizationMode;
    public final ModeSetting rotationMode;
    public final ModeSetting rotationAlgorithmMode;
    public final ModeSetting moveFixMode;
    public final ModeSetting rayCastMode;
    public final ButtonSetting nearest;
    public final ButtonSetting v2Nearest;
    public final SliderSetting nearestAccuracy;
    public final ButtonSetting lazy;
    public final ButtonSetting v2Lazy;
    public final SliderSetting lazyAccuracy;
    public final ButtonSetting constant;
    public final ButtonSetting v2Constant;
    public final ButtonSetting constantOnlyIfNotMoving;
    public final ButtonSetting noise;
    public final ButtonSetting v2Noise;
    public final SliderSetting noiseHorizontal;
    public final SliderSetting noiseVertical;
    public final SliderSetting noiseAimSpeed;
    public final SliderSetting noiseDelay;
    public final ButtonSetting noiseRangeDecrease;
    public final SliderSetting noiseRangeDecreaseThreshold;
    public final SliderSetting noiseRangeDecreaseDisable;
    public final ButtonSetting delayAim;
    public final ButtonSetting v2DelayAim;
    public final SliderSetting delayAimAmount;
    public final SliderSetting minRotationSpeed;
    public final SliderSetting maxRotationSpeed;
    public final SliderSetting v2MinRotationSpeed;
    public final SliderSetting v2MaxRotationSpeed;
    public final ModeSetting v2RotationType;
    public final ButtonSetting rotationSmoothing;
    public final ButtonSetting v2RotationSmoothing;
    public final ButtonSetting mxPitchBypass;
    public final ButtonSetting overshoot;
    public final ButtonSetting v2Overshoot;
    public final SliderSetting overshootAmount;
    public final SliderSetting overshootChance;
    public final SliderSetting inertiaAcceleration;
    public final SliderSetting inertiaMaxSpeed;
    public final SliderSetting inertiaFriction;
    public final SliderSetting inertiaDeadZone;
    
    // Advanced Rotation Mode Settings
    public final ModeSetting advAimPointMode;
    public final SliderSetting advAimOffset;
    public final ButtonSetting advPrediction;
    public final SliderSetting advPredictionStrength;
    public final ModeSetting advPredictionMode;
    public final SliderSetting advSmoothingBase;
    public final SliderSetting advSmoothingVar;
    public final SliderSetting advAcceleration;
    public final SliderSetting advYawPitchRatio;
    public final ButtonSetting advNoise;
    public final SliderSetting advNoiseH;
    public final SliderSetting advNoiseV;
    public final SliderSetting advNoiseSpeed;
    public final ModeSetting advGcdMode;
    public final SliderSetting advGcdVariance;
    public final ButtonSetting advSensitivitySim;
    public final SliderSetting advSensitivity;
    public final ButtonSetting advEntropyBypass;
    public final SliderSetting advEntropyVariance;
    public final ButtonSetting advStrafeDesync;
    public final SliderSetting advStrafeDesyncChance;
    public final ButtonSetting advFactorBypass;
    public final ButtonSetting advConstantBypass;
    public final ButtonSetting advSmoothBypass;
    public final ButtonSetting advPatternDiversity;
    public final ButtonSetting advModuloBypass;
    public final ButtonSetting advCombatAwareness;
    public final ButtonSetting advMicroCorrection;
    public final SliderSetting advMicroCorrectionStr;
    public final ButtonSetting advOvershoot;
    public final SliderSetting advOvershootAmount;

    public final ModeSetting targetingMode;
    public final ModeSetting sortMode;
    public final SliderSetting switchDelay;
    public final SliderSetting targets;
    public final ButtonSetting targetInvisible;
    public final SliderSetting interpolationTime;

    public final ButtonSetting disableInInventory;
    public final ButtonSetting disableWhileBlocking;
    public final ButtonSetting disableWhileMining;
    public final ButtonSetting fixSlotReset;
    public final ButtonSetting fixNoSlowFlag;
    public final SliderSetting postDelay;
    public final ButtonSetting hitThroughBlocks;
    public final ButtonSetting ignoreTeammates;
    public final ButtonSetting manualBlock;
    public final ButtonSetting requireMouseDown;
    public final ButtonSetting silentSwing;
    public final ButtonSetting weaponOnly;
    public final ButtonSetting useAutoClickerSettings;
    public final ButtonSetting lookAttackJitter;
    public final SliderSetting lookAttackJitterMin;
    public final SliderSetting lookAttackJitterMax;

    public final ButtonSetting dot;
    public final SliderSetting dotSize;

    // CPS Settings (always visible, even when using autoclicker settings)
    public final SliderSetting minCPS;
    public final SliderSetting maxCPS;
    public final SliderSetting baseLowCPS;
    public final SliderSetting baseHighCPS;
    public final SliderSetting baseCPSDeviation;
    public final SliderSetting notTargetedLowCPS;
    public final SliderSetting notTargetedHighCPS;
    public final SliderSetting earlyTargetedLowCPS;
    public final SliderSetting earlyTargetedHighCPS;
    public final SliderSetting earlyTargetedDuration;
    public final SliderSetting targetedLowCPS;
    public final SliderSetting targetedHighCPS;
    public final ButtonSetting enableExhaustion;
    public final SliderSetting exhaustionLowCPS;
    public final SliderSetting exhaustionHighCPS;
    public final SliderSetting exhaustionChance;

    private final String[] rotationModes = new String[]{"None", "Silent", "Lock view"};
    
    // Components
    public final KillAuraTargeting targeting;
    public final KillAuraRotation rotation;
    public final KillAuraAutoBlock autoBlock;
    
    // Legacy Fields for External Access
    public AtomicBoolean block;
    
    // State
    @Getter
    private boolean attack;
    private boolean swing;
    public boolean rmbDown;
    private boolean autoClickerWasEnabled = false;
    
    // Target tracking for early targeted state
    private long targetFirstSeenTime = -1;
    private EntityLivingBase lastTarget = null;

    // AntiGamingChair AutoClicker A & E bypass tracking
    private long lastAnimationPacketTime = 0L;
    private boolean lastPacketWasAnimation = false;
    private int swingCount = 0;
    private int movementCount = 0;
    private long lastMovementResetTime = 0L;

    public KillAura() {
        super("KillAura", category.combat);
        
        // Settings Registration
        this.registerSetting(new DescriptionSetting("Clicking"));
        this.registerSetting(useAutoClickerSettings = new ButtonSetting("Use AutoClicker settings", false));
        this.registerSetting(clickMode = new ModeValue("Click mode", this, () -> !useAutoClickerSettings.isToggled())
                .add(new NormalAutoClicker("Normal", this, true, true))
                .add(new DragClickAutoClicker("Drag Click", this, true, true))
                .add(new RecordAutoClicker("Record", this, true, true))
                .setDefaultValue("Normal")
        );
        for (SubMode<?> subMode : clickMode.getSubModeValues()) {
            for (Setting setting : subMode.getSettings()) {
                final Supplier<Boolean> originalCheck = setting.visibleCheck;
                setting.visibleCheck = () -> clickMode.isVisible() && originalCheck.get();
            }
        }
        this.registerSetting(attackMode = new ModeSetting("Attack mode", new String[]{"Legit", "Packet"}, 1));
        this.registerSetting(requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));

        // CPS Settings (always visible, even when using autoclicker settings)
        this.registerSetting(new DescriptionSetting("CPS"));
        this.registerSetting(minCPS = new SliderSetting("Min CPS (hard limit)", 1, 1, 40, 0.1));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS (hard limit)", 20, 1, 40, 0.1));
        this.registerSetting(new DescriptionSetting("Base CPS (other situations)"));
        this.registerSetting(baseLowCPS = new SliderSetting("Base low CPS", 8, 1, 40, 0.1));
        this.registerSetting(baseHighCPS = new SliderSetting("Base high CPS", 14, 1, 40, 0.1));
        this.registerSetting(baseCPSDeviation = new SliderSetting("Base CPS deviation", 10, 0, 50, 1, "%"));
        this.registerSetting(new DescriptionSetting("Not targeted"));
        this.registerSetting(notTargetedLowCPS = new SliderSetting("Not targeted low CPS", 5, 1, 40, 0.1));
        this.registerSetting(notTargetedHighCPS = new SliderSetting("Not targeted high CPS", 10, 1, 40, 0.1));
        this.registerSetting(new DescriptionSetting("Early targeted"));
        this.registerSetting(earlyTargetedLowCPS = new SliderSetting("Early targeted low CPS", 10, 1, 40, 0.1));
        this.registerSetting(earlyTargetedHighCPS = new SliderSetting("Early targeted high CPS", 18, 1, 40, 0.1));
        this.registerSetting(earlyTargetedDuration = new SliderSetting("Early targeted duration", 2000, 500, 5000, 100, "ms"));
        this.registerSetting(new DescriptionSetting("Targeted"));
        this.registerSetting(targetedLowCPS = new SliderSetting("Targeted low CPS", 12, 1, 40, 0.1));
        this.registerSetting(targetedHighCPS = new SliderSetting("Targeted high CPS", 20, 1, 40, 0.1));
        this.registerSetting(new DescriptionSetting("Exhaustion"));
        this.registerSetting(enableExhaustion = new ButtonSetting("Exhaustion", false));
        this.registerSetting(exhaustionLowCPS = new SliderSetting("Exhaustion low CPS", 3, 1, 40, 0.1, enableExhaustion::isToggled));
        this.registerSetting(exhaustionHighCPS = new SliderSetting("Exhaustion high CPS", 8, 1, 40, 0.1, enableExhaustion::isToggled));
        this.registerSetting(exhaustionChance = new SliderSetting("Exhaustion chance", 15, 0, 100, 1, "%", enableExhaustion::isToggled));

        this.registerSetting(new DescriptionSetting("Blocking"));
        String[] autoBlockModes = new String[]{"Manual", "Vanilla", "Post", "Swap", "Interact A", "Interact B", "Fake", "Partial", "QuickMacro", "Hypixel"};
        this.registerSetting(autoBlockMode = new ModeSetting("Autoblock", autoBlockModes, 0));
        this.registerSetting(manualBlock = new ButtonSetting("Manual block", false));
        this.registerSetting(disableWhileBlocking = new ButtonSetting("Disable while blocking", false));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing while blocking", false));
        this.registerSetting(fixNoSlowFlag = new ButtonSetting("Fix NoSlow flag", false));
        this.registerSetting(postDelay = new SliderSetting("Post delay", 10, 1, 20, 1, fixNoSlowFlag::isToggled));

        this.registerSetting(new DescriptionSetting("Range"));
        this.registerSetting(attackRange = new SliderSetting("Attack range", 3.0, 3.0, 6.0, 0.1));
        this.registerSetting(swingRange = new SliderSetting("Swing range", 3.0, 3.0, 8.0, 0.1));
        this.registerSetting(blockRange = new SliderSetting("Block range", 6.0, 3.0, 12.0, 0.1));
        this.registerSetting(preAimRange = new SliderSetting("PreAim range", 6.0, 3.0, 12.0, 0.1));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
        
        this.registerSetting(new DescriptionSetting("Rotation"));
        this.registerSetting(rotationMode = new ModeSetting("Rotation", rotationModes, 1));
        final ModeOnly doRotation = new ModeOnly(rotationMode, 1, 2);
        // Rotation algorithm modes
        this.registerSetting(rotationAlgorithmMode = new ModeSetting("Rotation mode", new String[]{"Classic", "Vulcan / V2", "Inertia", "Grim", "General", "Advanced"}, 0, doRotation));
        final java.util.function.Supplier<Boolean> classicMode = doRotation.extend(() -> rotationAlgorithmMode.getInput() == 0);
        final java.util.function.Supplier<Boolean> v2RotationMode = doRotation.extend(() -> rotationAlgorithmMode.getInput() == 1);
        final java.util.function.Supplier<Boolean> inertiaRotationMode = doRotation.extend(() -> rotationAlgorithmMode.getInput() == 2);
        final java.util.function.Supplier<Boolean> v2InertiaGeneralMode = doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return algo == 1 || algo == 2 || algo == 4;
        });
        final java.util.function.Supplier<Boolean> grimRotationMode = doRotation.extend(() -> rotationAlgorithmMode.getInput() == 3);
        final java.util.function.Supplier<Boolean> advancedRotationMode = doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5);
        this.registerSetting(minRotationSpeed = new SliderSetting("Min rotation speed", 8, 0, 10, 0.05, classicMode));
        this.registerSetting(maxRotationSpeed = new SliderSetting("Max rotation speed", 10, 0, 10, 0.05, classicMode));
        this.registerSetting(v2MinRotationSpeed = new SliderSetting("Min rotation speed", 2, 2, 20, 0.1, v2RotationMode));
        this.registerSetting(v2MaxRotationSpeed = new SliderSetting("Max rotation speed", 20, 2, 20, 0.1, v2RotationMode));
        this.registerSetting(v2RotationType = new ModeSetting("Rotation type", new String[]{"Instant", "Nearest"}, 0, v2RotationMode));
        this.registerSetting(rotationSmoothing = new ButtonSetting("Rotation smoothing", false, classicMode));
        this.registerSetting(v2RotationSmoothing = new ButtonSetting("Rotation smoothing", false, v2InertiaGeneralMode));
        this.registerSetting(mxPitchBypass = new ButtonSetting("MX Pitch Bypass", false, v2InertiaGeneralMode));
        this.registerSetting(overshoot = new ButtonSetting("Overshoot", false, classicMode));
        this.registerSetting(v2Overshoot = new ButtonSetting("Overshoot", false, v2InertiaGeneralMode));
        this.registerSetting(overshootAmount = new SliderSetting("Overshoot amount", 2.0, 0.5, 5.0, 0.1, doRotation.extend(() -> overshoot.isToggled() || ((rotationAlgorithmMode.getInput() == 1 || rotationAlgorithmMode.getInput() == 2 || rotationAlgorithmMode.getInput() == 4) && v2Overshoot.isToggled()))));
        this.registerSetting(overshootChance = new SliderSetting("Overshoot chance", 30.0, 0.0, 100.0, 1.0, "%", doRotation.extend(() -> overshoot.isToggled() || ((rotationAlgorithmMode.getInput() == 1 || rotationAlgorithmMode.getInput() == 2 || rotationAlgorithmMode.getInput() == 4) && v2Overshoot.isToggled()))));
        this.registerSetting(inertiaAcceleration = new SliderSetting("Inertia acceleration", 1.1, 0.1, 5.0, 0.05, inertiaRotationMode));
        this.registerSetting(inertiaMaxSpeed = new SliderSetting("Inertia max speed", 12.0, 2.0, 40.0, 0.5, inertiaRotationMode));
        this.registerSetting(inertiaFriction = new SliderSetting("Inertia friction", 0.88, 0.6, 1.0, 0.01, inertiaRotationMode));
        this.registerSetting(inertiaDeadZone = new SliderSetting("Inertia deadzone", 0.35, 0.0, 3.0, 0.05, inertiaRotationMode));
        this.registerSetting(moveFixMode = new ModeSetting("Move fix", RotationHandler.MoveFix.MODES, 0, new ModeOnly(rotationMode, 1)));
        this.registerSetting(rayCastMode = new ModeSetting("Ray cast", new String[]{"None", "Normal", "Strict"}, 1, doRotation));
        this.registerSetting(nearest = new ButtonSetting("Nearest", false, classicMode));
        this.registerSetting(v2Nearest = new ButtonSetting("Nearest", false, v2InertiaGeneralMode));
        this.registerSetting(nearestAccuracy = new SliderSetting("Nearest accuracy", 1, 0.8, 1, 0.01, doRotation.extend(() -> (rotationAlgorithmMode.getInput() == 0 && nearest.isToggled()) || ((rotationAlgorithmMode.getInput() == 1 || rotationAlgorithmMode.getInput() == 2 || rotationAlgorithmMode.getInput() == 4) && v2Nearest.isToggled()))));
        this.registerSetting(lazy = new ButtonSetting("Lazy", false, classicMode));
        this.registerSetting(v2Lazy = new ButtonSetting("Lazy", false, v2InertiaGeneralMode));
        this.registerSetting(lazyAccuracy = new SliderSetting("Lazy accuracy", 0.95, 0.6, 1, 0.01, doRotation.extend(() -> (rotationAlgorithmMode.getInput() == 0 && lazy.isToggled()) || ((rotationAlgorithmMode.getInput() == 1 || rotationAlgorithmMode.getInput() == 2 || rotationAlgorithmMode.getInput() == 4) && v2Lazy.isToggled()))));
        this.registerSetting(constant = new ButtonSetting("Constant", false, classicMode));
        this.registerSetting(v2Constant = new ButtonSetting("Constant", false, v2InertiaGeneralMode));
        this.registerSetting(constantOnlyIfNotMoving = new ButtonSetting("Constant only if not moving", false, doRotation.extend(() -> (rotationAlgorithmMode.getInput() == 0 && constant.isToggled()) || ((rotationAlgorithmMode.getInput() == 1 || rotationAlgorithmMode.getInput() == 2 || rotationAlgorithmMode.getInput() == 4) && v2Constant.isToggled()))));
        
        this.registerSetting(noise = new ButtonSetting("Noise", false, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return algo == 0;
        })));
        this.registerSetting(v2Noise = new ButtonSetting("Noise", false, v2InertiaGeneralMode));
        this.registerSetting(pointRandomizationMode = new ModeSetting("Mode", new String[]{"None", "Basic", "Linear", "Smooth"}, 0, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled());
        })));
        this.registerSetting(noiseHorizontal = new SliderSetting("Noise horizontal", 0.35, 0.01, 1, 0.01, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled());
        })));
        this.registerSetting(noiseVertical = new SliderSetting("Noise vertical", 0.5, 0.01, 1, 0.01, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled());
        })));
        this.registerSetting(noiseAimSpeed = new SliderSetting("Noise aim speed", 0.35, 0.01, 1, 0.01, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled());
        })));
        this.registerSetting(noiseDelay = new SliderSetting("Noise delay", 100, 50, 500, 10, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled());
        })));
        this.registerSetting(noiseRangeDecrease = new ButtonSetting("Range decrease", false, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled());
        })));
        this.registerSetting(noiseRangeDecreaseThreshold = new SliderSetting("Range decrease threshold", 3.0, 1.0, 6.0, 0.1, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (((algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled())) && noiseRangeDecrease.isToggled());
        })));
        this.registerSetting(noiseRangeDecreaseDisable = new SliderSetting("Range decrease disable", 2.0, 0.5, 4.0, 0.1, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (((algo == 0 && noise.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2Noise.isToggled())) && noiseRangeDecrease.isToggled());
        })));
        this.registerSetting(delayAim = new ButtonSetting("Delay aim", false, classicMode));
        this.registerSetting(v2DelayAim = new ButtonSetting("Delay aim", false, v2InertiaGeneralMode));
        this.registerSetting(delayAimAmount = new SliderSetting("Delay aim amount", 5, 5, 100, 1, doRotation.extend(() -> {
            int algo = (int) rotationAlgorithmMode.getInput();
            return (algo == 0 && delayAim.isToggled()) || ((algo == 1 || algo == 2 || algo == 4) && v2DelayAim.isToggled());
        })));
        
        // === Advanced Mode Settings ===
        this.registerSetting(new DescriptionSetting("Advanced Settings", advancedRotationMode));
        
        // Aim Point Settings
        this.registerSetting(advAimPointMode = new ModeSetting("Aim point", new String[]{"Center", "Eyes", "Feet", "Nearest", "Adaptive"}, 4, advancedRotationMode));
        this.registerSetting(advAimOffset = new SliderSetting("Aim offset", 0.0, -1.0, 1.0, 0.05, advancedRotationMode));
        
        // Prediction Settings
        this.registerSetting(advPrediction = new ButtonSetting("Prediction", true, advancedRotationMode));
        this.registerSetting(advPredictionStrength = new SliderSetting("Prediction strength", 0.5, 0.0, 1.0, 0.05, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advPrediction.isToggled())));
        this.registerSetting(advPredictionMode = new ModeSetting("Prediction mode", new String[]{"Linear", "Cubic", "Exponential"}, 0, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advPrediction.isToggled())));
        
        // Smoothing Settings
        this.registerSetting(new DescriptionSetting("Smoothing", advancedRotationMode));
        this.registerSetting(advSmoothingBase = new SliderSetting("Smoothing base", 0.4, 0.1, 1.0, 0.05, advancedRotationMode));
        this.registerSetting(advSmoothingVar = new SliderSetting("Smoothing variance", 0.15, 0.0, 0.5, 0.01, advancedRotationMode));
        this.registerSetting(advAcceleration = new SliderSetting("Acceleration", 0.3, 0.0, 1.0, 0.05, advancedRotationMode));
        this.registerSetting(advYawPitchRatio = new SliderSetting("Yaw/Pitch ratio", 1.2, 0.5, 2.0, 0.1, advancedRotationMode));
        
        // Noise Settings
        this.registerSetting(new DescriptionSetting("Noise", advancedRotationMode));
        this.registerSetting(advNoise = new ButtonSetting("Noise", true, advancedRotationMode));
        this.registerSetting(advNoiseH = new SliderSetting("Noise horizontal", 0.4, 0.0, 1.5, 0.05, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advNoise.isToggled())));
        this.registerSetting(advNoiseV = new SliderSetting("Noise vertical", 0.3, 0.0, 1.5, 0.05, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advNoise.isToggled())));
        this.registerSetting(advNoiseSpeed = new SliderSetting("Noise speed", 0.5, 0.1, 1.0, 0.05, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advNoise.isToggled())));
        
        // GCD Settings
        this.registerSetting(new DescriptionSetting("GCD & Mouse", advancedRotationMode));
        this.registerSetting(advGcdMode = new ModeSetting("GCD mode", new String[]{"Off", "Basic", "Dynamic", "Adaptive"}, 3, advancedRotationMode));
        this.registerSetting(advGcdVariance = new SliderSetting("GCD variance", 0.15, 0.0, 0.5, 0.01, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advGcdMode.getInput() >= 2)));
        this.registerSetting(advSensitivitySim = new ButtonSetting("Sensitivity simulation", true, advancedRotationMode));
        this.registerSetting(advSensitivity = new SliderSetting("Simulated sensitivity", 100, 50, 200, 1, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advSensitivitySim.isToggled())));
        
        // Anti-Cheat Bypass Settings
        this.registerSetting(new DescriptionSetting("Anti-Detection", advancedRotationMode));
        this.registerSetting(advEntropyBypass = new ButtonSetting("Entropy bypass", true, advancedRotationMode));
        this.registerSetting(advEntropyVariance = new SliderSetting("Entropy variance", 0.12, 0.05, 0.3, 0.01, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advEntropyBypass.isToggled())));
        this.registerSetting(advStrafeDesync = new ButtonSetting("Strafe desync", true, advancedRotationMode));
        this.registerSetting(advStrafeDesyncChance = new SliderSetting("Desync chance", 30, 10, 80, 5, "%", doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advStrafeDesync.isToggled())));
        this.registerSetting(advFactorBypass = new ButtonSetting("Factor bypass", true, advancedRotationMode));
        this.registerSetting(advConstantBypass = new ButtonSetting("Constant bypass", true, advancedRotationMode));
        this.registerSetting(advSmoothBypass = new ButtonSetting("Smooth bypass", true, advancedRotationMode));
        this.registerSetting(advPatternDiversity = new ButtonSetting("Pattern diversity", true, advancedRotationMode));
        this.registerSetting(advModuloBypass = new ButtonSetting("Modulo bypass", true, advancedRotationMode));
        
        // Combat State Settings
        this.registerSetting(new DescriptionSetting("Combat", advancedRotationMode));
        this.registerSetting(advCombatAwareness = new ButtonSetting("Combat awareness", true, advancedRotationMode));
        this.registerSetting(advMicroCorrection = new ButtonSetting("Micro-correction", true, advancedRotationMode));
        this.registerSetting(advMicroCorrectionStr = new SliderSetting("Correction strength", 0.3, 0.1, 0.8, 0.05, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advMicroCorrection.isToggled())));
        this.registerSetting(advOvershoot = new ButtonSetting("Overshoot", false, advancedRotationMode));
        this.registerSetting(advOvershootAmount = new SliderSetting("Overshoot amount", 0.1, 0.0, 0.5, 0.01, doRotation.extend(() -> rotationAlgorithmMode.getInput() == 5 && advOvershoot.isToggled())));
        
        this.registerSetting(new DescriptionSetting("Targets"));
        this.registerSetting(targetingMode = new ModeSetting("Targeting mode", new String[]{"Single", "Switch"}, 0));
        this.registerSetting(sortMode = new ModeSetting("Sort mode", new String[]{"Health", "HurtTime", "Distance", "Yaw"}, 0));
        this.registerSetting(targets = new SliderSetting("Targets", 2.0, 2.0, 10.0, 1.0, () -> targetingMode.getInput() == 1));
        this.registerSetting(switchDelay = new SliderSetting("Switch delay", 200.0, 0.0, 1000.0, 50.0, "ms", () -> targetingMode.getInput() == 1));
        this.registerSetting(interpolationTime = new SliderSetting("Interpolation time", 150.0, 0.0, 500.0, 10.0, "ms"));
        this.registerSetting(targetInvisible = new ButtonSetting("Target invisible", true));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(hitThroughBlocks = new ButtonSetting("Hit through blocks", true));
        
        this.registerSetting(new DescriptionSetting("Miscellaneous"));
        this.registerSetting(disableInInventory = new ButtonSetting("Disable in inventory", true));
        this.registerSetting(disableWhileMining = new ButtonSetting("Disable while mining", false));
        this.registerSetting(fixSlotReset = new ButtonSetting("Fix slot reset", false));
        this.registerSetting(lookAttackJitter = new ButtonSetting("Look->Attack jitter", true));
        this.registerSetting(lookAttackJitterMin = new SliderSetting("Jitter min", 25, 0, 200, 1, "ms", lookAttackJitter::isToggled));
        this.registerSetting(lookAttackJitterMax = new SliderSetting("Jitter max", 45, 0, 250, 1, "ms", lookAttackJitter::isToggled));
        
        this.registerSetting(new DescriptionSetting("Visual"));
        this.registerSetting(dot = new ButtonSetting("Dot", false));
        this.registerSetting(dotSize = new SliderSetting("Dot size", 0.1, 0.05, 0.2, 0.05, dot::isToggled));

        // Initialize Components
        this.targeting = new KillAuraTargeting(this);
        this.rotation = new KillAuraRotation(this);
        this.autoBlock = new KillAuraAutoBlock(this);
        
        // Link legacy field to component's field
        this.block = this.autoBlock.block;
    }

    @Override
    public void onEnable() {
        if (useAutoClickerSettings.isToggled() && ModuleManager.autoClicker != null) {
            autoClickerWasEnabled = ModuleManager.autoClicker.isEnabled();
            if (!autoClickerWasEnabled) {
                // Use AutoClicker silently without enabling it
                ModuleManager.autoClicker.useSilently(this);
            }
            ModuleManager.autoClicker.mode.enable();
        } else {
            clickMode.enable();
        }
        
        rotation.onEnable();
        // Targeting and AutoBlock don't have specific onEnable logic but we can add if needed
    }

    @Override
    public void onDisable() {
        if (useAutoClickerSettings.isToggled() && ModuleManager.autoClicker != null) {
            ModuleManager.autoClicker.mode.disable();
            if (!autoClickerWasEnabled) {
                // Stop using AutoClicker silently
                ModuleManager.autoClicker.stopUsingSilently(this);
            }
        } else {
            clickMode.disable();
        }
        
        target = null;
        attack = false;
        swing = false;
        rmbDown = false;
        
        // Reset target tracking
        targetFirstSeenTime = -1;
        lastTarget = null;
        
        targeting.onDisable();
        rotation.onDisable();
        autoBlock.onDisable();
        
        // Fix rotation snap on disable:
        // If Silent rotation was active, the client's real rotation might be different from the server-side rotation.
        // However, the issue described is likely due to the sudden snap from a large modulo value (e.g. 36000) 
        // back to a normal value (e.g. 45) in a single tick, which Grim detects as a massive rotation.
        // We need to ensure the transition is handled or that we don't send a packet with a huge delta.
        // Since we modify the yaw sent to the server, disabling the module stops that modification.
        // The next packet sent by the client will use mc.thePlayer.rotationYaw (e.g. 45).
        // If the last packet sent was 36045, the server sees a delta of -36000.
        
        // To fix this, we should ideally sync the client's rotation to the last sent server rotation (wrapped), 
        // or ensure the last sent packet was normalized. But we can't easily undo the last packet.
        // Instead, we can try to set the client's rotation to a value that minimizes the delta for the NEXT packet,
        // but that would snap the user's view.
        
        // Better approach: The "AimModulo360" bypass adds 36000. When disabling, we are effectively removing this offset.
        // This big jump is exactly what Grim detects. 
        // WE CANNOT FIX THIS PERFECTLY without risking a view snap or a flag.
        // BUT, if we are using Silent mode, the client view is already decoupled.
        // 
        // If we just let it disable, the next packet is normal.
        // Maybe we can force a "reset" packet or similar?
        // Actually, if we modify RotationHandler to NOT return the offset when KA is disabled, that handles it.
        // The issue is that the LAST packet had the offset. The CURRENT/NEXT packet will not.
        
        if (Utils.nullCheck()) mc.thePlayer.stopUsingItem();
    }

    @Override
    public void guiUpdate() {
        Utils.correctValue(attackRange, swingRange);
        Utils.correctValue(swingRange, preAimRange);
        Utils.correctValue(minRotationSpeed, maxRotationSpeed);
        Utils.correctValue(v2MinRotationSpeed, v2MaxRotationSpeed);
        
        // CPS validation
        Utils.correctValue(minCPS, maxCPS);
        Utils.correctValue(baseLowCPS, baseHighCPS);
        Utils.correctValue(notTargetedLowCPS, notTargetedHighCPS);
        Utils.correctValue(earlyTargetedLowCPS, earlyTargetedHighCPS);
        Utils.correctValue(targetedLowCPS, targetedHighCPS);
        if (enableExhaustion.isToggled()) {
            Utils.correctValue(exhaustionLowCPS, exhaustionHighCPS);
        }
        
        // Ensure all CPS values respect hard limits
        if (baseLowCPS.getInput() < minCPS.getInput()) baseLowCPS.setValue(minCPS.getInput());
        if (baseHighCPS.getInput() > maxCPS.getInput()) baseHighCPS.setValue(maxCPS.getInput());
        if (notTargetedLowCPS.getInput() < minCPS.getInput()) notTargetedLowCPS.setValue(minCPS.getInput());
        if (notTargetedHighCPS.getInput() > maxCPS.getInput()) notTargetedHighCPS.setValue(maxCPS.getInput());
        if (earlyTargetedLowCPS.getInput() < minCPS.getInput()) earlyTargetedLowCPS.setValue(minCPS.getInput());
        if (earlyTargetedHighCPS.getInput() > maxCPS.getInput()) earlyTargetedHighCPS.setValue(maxCPS.getInput());
        if (targetedLowCPS.getInput() < minCPS.getInput()) targetedLowCPS.setValue(minCPS.getInput());
        if (targetedHighCPS.getInput() > maxCPS.getInput()) targetedHighCPS.setValue(maxCPS.getInput());
        if (enableExhaustion.isToggled()) {
            if (exhaustionLowCPS.getInput() < minCPS.getInput()) exhaustionLowCPS.setValue(minCPS.getInput());
            if (exhaustionHighCPS.getInput() > maxCPS.getInput()) exhaustionHighCPS.setValue(maxCPS.getInput());
        }
        Utils.correctValue(lookAttackJitterMin, lookAttackJitterMax);
        
        if (targetingMode.getInput() == 1 && targets.getInput() < 2) {
            targets.setValue(2.0);
        }
        if (noiseRangeDecrease.isToggled()) {
            Utils.correctValue(noiseRangeDecreaseDisable, noiseRangeDecreaseThreshold);
        }
        
        if (isEnabled()) {
            if (useAutoClickerSettings.isToggled() && ModuleManager.autoClicker != null) {
                clickMode.disable();
                if (!ModuleManager.autoClicker.isEnabled()) {
                    autoClickerWasEnabled = false;
                    // Use AutoClicker silently without enabling it
                    ModuleManager.autoClicker.useSilently(this);
                } else {
                    autoClickerWasEnabled = true;
                }
                if (attackMode.getInput() == 1) {
                    for (int i = 0; i < ModuleManager.autoClicker.mode.getSubModeValues().size(); i++) {
                        if (ModuleManager.autoClicker.mode.getSubModeValues().get(i) instanceof NormalAutoClicker) {
                            ModuleManager.autoClicker.mode.setValueRaw(i);
                            break;
                        }
                    }
                }
                ModuleManager.autoClicker.mode.enable();
            } else {
                if (ModuleManager.autoClicker != null && !autoClickerWasEnabled) {
                    // Stop using AutoClicker silently
                    ModuleManager.autoClicker.stopUsingSilently(this);
                }
                ModuleManager.autoClicker.mode.disable();
                clickMode.enable();
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        rotation.onRenderTick(ev);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        rotation.onRenderWorldLast(event);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (useAutoClickerSettings.isToggled() && ModuleManager.autoClicker != null && ModuleManager.autoClicker.isEnabled()) {
            keystrokesmod.module.setting.impl.SubMode<?> selectedMode = ModuleManager.autoClicker.mode.getSelected();
            if (selectedMode != null && selectedMode.isEnabled()) {
                try {
                    selectedMode.onUpdate();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }

        if (gameNoAction() || playerNoAction()) {
            target = null;
            targeting.onDisable();
            autoBlock.onDisable(); // Resets blink state
            return;
        }

        // Update Target
        EntityLivingBase newTarget = targeting.updateTarget(rotation.getCurrentRotations());
        rotation.handleTargetChange(target, newTarget);
        
        // Track target changes for early targeted state
        if (newTarget != lastTarget) {
            if (newTarget != null) {
                // New target acquired
                targetFirstSeenTime = System.currentTimeMillis();
            } else {
                // Target lost
                targetFirstSeenTime = -1;
            }
            lastTarget = newTarget;
        }
        
        target = newTarget;
        
        // Auto Block Logic
        double dist = target != null ? mc.thePlayer.getDistanceToEntity(target) : 100;
        autoBlock.updateBlockState(target, dist);
        autoBlock.block(target);
        
        // Update Hypixel autoblock every tick (cryptix implementation)
        if (autoBlockMode.getInput() == 9) {
            autoBlock.updateHypixelAutoblock(target);
        }

        // Check disable conditions
        if (ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.currentBlock != null) {
            // In legit mode, always disable aura (don't check allowAura). Otherwise check allowAura setting
            boolean shouldDisable = ModuleManager.bedAura.mode.getInput() == 0 || !ModuleManager.bedAura.allowAura.isToggled();
            if (shouldDisable) {
                autoBlock.resetBlinkState(true);
                return;
            }
        }
        if (ModuleManager.autoGapple != null && ModuleManager.autoGapple.disableKillAura.isToggled() && ModuleManager.autoGapple.working) {
            autoBlock.resetBlinkState(true);
            return;
        }
        if ((mc.thePlayer.isBlocking() || autoBlock.block.get()) && disableWhileBlocking.isToggled()) {
            autoBlock.resetBlinkState(true);
            return;
        }

        // Attack Logic (PreUpdate)
        if (target != null && dist <= swingRange.getInput()) {
            swing = true;
        } else {
            swing = false;
        }

        boolean swingWhileBlocking = !silentSwing.isToggled() || !autoBlock.block.get();
        if (swing && attack && HitSelect.canSwing()) {
            // AntiGamingChair AutoClicker A bypass: limit swings per movement cycle
            // AutoClicker A flags when swings > 20 in 20 movements
            // Limit to 19 swings per 20 movements to stay safe
            if (swingCount >= 19 && movementCount < 20) {
                // Too many swings in this cycle, skip this one
                return;
            }
            
            // AntiGamingChair AutoClicker E bypass: prevent consecutive animation packets without movement
            // AutoClicker E flags when two animation packets are sent consecutively
            long currentTime = System.currentTimeMillis();
            if (lastPacketWasAnimation && currentTime - lastAnimationPacketTime < 50L) {
                // Last packet was animation, need to wait for a movement packet first
                // Skip this swing to avoid consecutive animations
                return;
            }
            
            if (swingWhileBlocking) {
                mc.thePlayer.swingItem();
                RecordClick.click();
            } else {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
                RecordClick.click();
            }
            lastAnimationPacketTime = currentTime;
            lastPacketWasAnimation = true;
            swingCount++;
        }
        
        // Handle attack and interact
        if (attack) {
            // Hypixel mode: cancel attack if blocking state doesn't allow
            if (autoBlockMode.getInput() == 9 && autoBlock.asw != 1) {
                autoBlock.resetBlinkState(true);
                attack = false;
                return;
            }
            autoBlock.resetBlinkState(true);
            attack = false;
            if (rotationAlgorithmMode != null && rotationAlgorithmMode.getInput() == 3 && !rotation.isGrimReady()) return;
            if (noAimToEntity()) return;
            // Intave deltaVL mitigation: optional LOOK->ATTACK spacing jitter
            if (lookAttackJitter.isToggled()) {
                long sinceRotUpdate = System.currentTimeMillis() - rotation.getLastUpdateMs();
                int minJ = (int) Math.min(lookAttackJitterMin.getInput(), lookAttackJitterMax.getInput());
                int maxJ = (int) Math.max(lookAttackJitterMin.getInput(), lookAttackJitterMax.getInput());
                int jitterBudget = ThreadLocalRandom.current().nextInt(minJ, maxJ + 1);
                if (sinceRotUpdate < jitterBudget) {
                    // Re-arm attack for next tick to spread LOOK -> ATTACK spacing
                    attack = true;
                    return;
                }
            }
            // IntaveDisabler IntaveReach: notify for position spoof when attacking at range
            if (ModuleManager.disabler != null && ModuleManager.disabler.isEnabled()) {
                Object sel = ModuleManager.disabler.mode.getSelected();
                if (sel instanceof IntaveDisabler) {
                    ((IntaveDisabler) sel).onKillAuraAttack(target);
                }
            }
            Utils.attackEntity(target, swingWhileBlocking);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreMotion(RotationEvent e) {
        // AntiGamingChair AutoClicker A & E bypass: track movements
        // Movement packets are sent every tick, so reset animation flag
        lastPacketWasAnimation = false;
        movementCount++;
        
        // AutoClicker A: Reset counters every 20 movements (matches check interval)
        // AutoClicker A flags when swings > 20 in 20 movements
        long currentTime = System.currentTimeMillis();
        if (movementCount >= 20) {
            // Check if we exceeded the threshold
            if (swingCount > 20) {
                // We exceeded, need to be more conservative next cycle
                // This will be handled by reducing swing frequency in the swing logic
            }
            movementCount = 0;
            swingCount = 0;
            lastMovementResetTime = currentTime;
        }
        
        if (gameNoAction() || playerNoAction()) return;
        
        // Rotation is handled in onPreUpdate's targeting update and rotation.getRotations
        // But we need to set the event yaw/pitch here
        
        float[] rots = rotation.getRotations(target);
        
        if (rotationMode.getInput() == 1) {
             e.setYaw(rots[0]);
             e.setPitch(rots[1]);
             e.setMoveFix(RotationHandler.MoveFix.values()[(int) moveFixMode.getInput()]);
        } else if (rotationMode.getInput() == 0) {
            // None mode smoothing
             float currentYaw = rotation.getCurrentRotations()[0];
             float currentPitch = rotation.getCurrentRotations()[1];
             float playerYaw = mc.thePlayer.rotationYaw;
             float playerPitch = mc.thePlayer.rotationPitch;
             
             float yawDelta = RotationUtils.normalize(playerYaw - currentYaw);
             float pitchDelta = playerPitch - currentPitch;
             float transitionSpeed = 5.0f;
             
             if (Math.abs(yawDelta) > transitionSpeed) {
                currentYaw += yawDelta > 0 ? transitionSpeed : -transitionSpeed;
            } else {
                currentYaw = playerYaw;
            }
            if (Math.abs(pitchDelta) > transitionSpeed) {
                currentPitch += pitchDelta > 0 ? transitionSpeed : -transitionSpeed;
            } else {
                currentPitch = playerPitch;
            }
            rotation.setRotations(new float[]{currentYaw, currentPitch});
        }
        
        // AutoBlock special cases
        if (autoBlockMode.getInput() == 2 && autoBlock.block.get() && Utils.holdingSword()) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (autoBlockMode.getInput() == 2 && autoBlock.block.get() && Utils.holdingSword()) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(SlotHandler.getHeldItem()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        
        // For Hypixel autoblock, if Blink is being used silently, let Blink handle it
        if (autoBlockMode.getInput() == 9 && ModuleManager.blink != null && ModuleManager.blink.isUsedSilently()) {
            // Blink's handler will take care of packet interception
            return;
        }
        
        // Otherwise, use local packet handling
        if (!autoBlock.blinking) {
            return;
        }
        if (e.getPacket().getClass().getSimpleName().startsWith("S")) return;
        
        autoBlock.blinkedPackets.add(e.getPacket());
        e.setCanceled(true);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        autoBlock.handlePacketReceive(e);
    }

    @SubscribeEvent
    public void onMouse(final @NotNull MouseEvent mouseEvent) {
        if (mouseEvent.button == 0 && mouseEvent.buttonstate) {
            if (target != null || swing) {
                mouseEvent.setCanceled(true);
            }
        } else if (mouseEvent.button == 1) {
            rmbDown = mouseEvent.buttonstate;
            if (autoBlockMode.getInput() >= 1 && Utils.holdingSword() && autoBlock.block.get() && autoBlockMode.getInput() != 7) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                if (target == null && mc.objectMouseOver != null) {
                    if (mc.objectMouseOver.entityHit != null && AntiBot.isBot(mc.objectMouseOver.entityHit)) return;
                    final BlockPos getBlockPos = mc.objectMouseOver.getBlockPos();
                    if (getBlockPos != null && (BlockUtils.check(getBlockPos, Blocks.chest) || BlockUtils.check(getBlockPos, Blocks.ender_chest))) return;
                }
                mouseEvent.setCanceled(true);
            }
        }
    }

    @Override
    public String getInfo() {
        String targetStatus = targeting.getStatus();
        if (targetStatus != null) return targetStatus;
        return rotationModes[(int) rotationMode.getInput()];
    }

    public boolean noAimToEntity() {
        if (target == null) return true;
        if (rotationMode.getInput() == 0) return false;

        boolean noAim = false;
        switch ((int) rayCastMode.getInput()) {
            default:
            case 2:
                noAim = !RotationUtils.isMouseOver(RotationHandler.getRotationYaw(), RotationHandler.getRotationPitch(), target, (float) attackRange.getInput());
            case 1:
                if (noAim) break;
                Object[] rayCasted = Reach.getEntity(attackRange.getInput(), -0.05, rotationMode.getInput() == 1 ? rotation.getCurrentRotations() : null);
                noAim = rayCasted == null || rayCasted[0] != target;
                break;
            case 0:
                return false;
        }
        return noAim;
    }

    private boolean gameNoAction() {
        if (!Utils.nullCheck()) return true;
        if (ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.currentBlock != null) {
            // In legit mode, always disable aura (don't check allowAura). Otherwise check allowAura setting
            boolean shouldDisable = ModuleManager.bedAura.mode.getInput() == 0 || !ModuleManager.bedAura.allowAura.isToggled();
            if (shouldDisable) return true;
        }
        if (Blink.isBlinking()) return true;
        if (HypixelAntiVoid.getInstance() != null && HypixelAntiVoid.getInstance().blink.isEnabled()) return true;
        return mc.thePlayer.isDead;
    }

    private boolean playerNoAction() {
        if (requireMouseDown.isToggled() && !Mouse.isButtonDown(0)) return true;
        else if (!Utils.holdingWeapon() && weaponOnly.isToggled()) return true;
        else if (isMining() && disableWhileMining.isToggled()) return true;
        else if (fixNoSlowFlag.isToggled() && autoBlock.blockingTime > (int) postDelay.getInput()) {
            autoBlock.unBlock();
            // autoBlock.blockingTime = 0; // Handled in unBlock
        } else if (ModuleManager.scaffold.isEnabled()) return true;
        return mc.currentScreen != null && disableInInventory.isToggled();
    }

    private boolean isMining() {
        return Mouse.isButtonDown(0) && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }
    
    // Static helper for KillAuraTargeting
    public static boolean behindBlocks(float[] rotations, EntityLivingBase target) {
        try {
            Vec3 eyePos = Utils.getEyePos();
            MovingObjectPosition hitResult = RotationUtils.rayCast(
                    RotationUtils.getNearestPoint(target.getEntityBoundingBox(), eyePos).distanceTo(eyePos) - 0.01,
                    RotationHandler.getRotationYaw(), RotationHandler.getRotationPitch()
            );
            return hitResult != null;
        } catch (NullPointerException ignored) {
        }
        return false;
    }
    
    // Legacy API wrapper
    public void resetBlinkState(boolean unblock) {
        autoBlock.resetBlinkState(unblock);
    }

    @Override
    public boolean click() {
        if (useAutoClickerSettings.isToggled() && ModuleManager.autoClicker != null && ModuleManager.autoClicker.isEnabled()) {
            if (target != null && mc.thePlayer.getDistanceToEntity(target) <= swingRange.getInput()) {
                if (mc.currentScreen == null && HitSelect.canAttack()) {
                    // Hypixel mode: cancel attack if blocking state doesn't allow
                    if (autoBlockMode.getInput() == 9 && autoBlock.asw != 1) {
                        return false;
                    }
                    Utils.sendClick(0, true);
                    if (swing) attack = true;
                    return true;
                }
            }
            return false;
        }
        
        switch ((int) attackMode.getInput()) {
            case 0:
                if (target != null && mc.thePlayer.getDistanceToEntity(target) <= swingRange.getInput()) {
                    Utils.sendClick(0, true);
                    Utils.sendClick(0, false);
                    return true;
                }
                return false;
            default:
            case 1:
                if (swing) attack = true;
                return swing;
        }
    }
    
    // Public accessors for components if needed by other modules (e.g. for script)
    public KillAuraTargeting getTargeting() { return targeting; }
    public KillAuraRotation getRotation() { return rotation; }
    public KillAuraAutoBlock getAutoBlock() { return autoBlock; }
    
    /**
     * Gets the current CPS range based on target state.
     * Returns a pair of (minCPS, maxCPS) values.
     * Note: This just calls normal auto clicker with different ranges.
     */
    public double[] getCurrentCPSRange() {
        double min, max;
        
        // Check if exhaustion should be used (if enabled and chance triggers)
        boolean useExhaustion = enableExhaustion.isToggled() && 
                               Math.random() * 100 < exhaustionChance.getInput();
        
        if (useExhaustion) {
            // Use exhaustion range
            min = exhaustionLowCPS.getInput();
            max = exhaustionHighCPS.getInput();
        } else if (target == null) {
            // Not targeted
            min = notTargetedLowCPS.getInput();
            max = notTargetedHighCPS.getInput();
        } else {
            // Check if early targeted (first few seconds)
            long currentTime = System.currentTimeMillis();
            boolean isEarlyTargeted = targetFirstSeenTime != -1 && 
                                     (currentTime - targetFirstSeenTime) < earlyTargetedDuration.getInput();
            
            if (isEarlyTargeted) {
                // Early targeted
                min = earlyTargetedLowCPS.getInput();
                max = earlyTargetedHighCPS.getInput();
            } else {
                // Targeted (normal)
                min = targetedLowCPS.getInput();
                max = targetedHighCPS.getInput();
            }
        }
        
        // Apply base CPS deviation if applicable (for "other situations")
        // This adds randomization to the base CPS
        if (baseCPSDeviation.getInput() > 0) {
            double deviation = baseCPSDeviation.getInput() / 100.0;
            double baseMin = baseLowCPS.getInput();
            double baseMax = baseHighCPS.getInput();
            
            // Blend with base CPS based on deviation
            min = min * (1 - deviation) + baseMin * deviation;
            max = max * (1 - deviation) + baseMax * deviation;
        }
        
        // General / Generic Anti-Cheat bypass logic:
        // Ensure we don't attack with perfectly consistent tick intervals (KillAuraE)
        // and ensure movement-per-click deviation is high enough (AutoClickerA).
        // We achieve this by adding a "burst" randomness.
        if (Math.random() > 0.85) { // 15% chance to burst or lag
             if (Math.random() > 0.5) {
                 // Lag: Reduce CPS temporarily
                 min = Math.max(1, min - 4);
                 max = Math.max(min, max - 4);
             } else {
                 // Burst: Increase CPS slightly
                 min = Math.min(20, min + 3);
                 max = Math.min(20, max + 3);
             }
        }

        // Enforce hard limits
        min = Math.max(min, minCPS.getInput());
        max = Math.min(max, maxCPS.getInput());
        
        // Ensure min <= max
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        
        return new double[]{min, max};
    }
}
