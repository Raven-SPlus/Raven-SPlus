package keystrokesmod.module.impl.combat.autoclicker;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.CoolDown;
import keystrokesmod.utility.Utils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalAutoClicker extends SubMode<IAutoClicker> {
    private final SliderSetting minCPS;
    private final SliderSetting maxCPS;
    private final SliderSetting cpsLimiter;
    private final ButtonSetting butterFly;
    
    // Over-randomization settings (for base CPS/delay)
    private final ButtonSetting enableOverRandomization;
    private final SliderSetting cpsRandomization;
    private final SliderSetting delayRandomization;
    private final SliderSetting timingVariation;
    
    // Spike/Burst settings for chaotic patterns
    private final ButtonSetting enableSpikePatterns;
    private final SliderSetting spikeFrequency;
    private final SliderSetting spikeIntensity;
    private final SliderSetting spikeDuration;
    private final SliderSetting burstChance;
    private final SliderSetting burstSize;
    // Spike/Burst over-randomization settings
    private final ButtonSetting enableSpikeOverRandomization;
    private final SliderSetting spikeCpsRandomization;
    private final SliderSetting spikeDelayRandomization;
    private final SliderSetting spikeTimingVariation;
    
    // Chaotic timing settings
    private final ButtonSetting enableChaoticTiming;
    private final SliderSetting irregularityFactor;
    private final SliderSetting jitterAmount;
    private final SliderSetting patternVariation;
    
    // Press/Release delay settings
    private final ButtonSetting enablePressReleaseDelay;
    private final SliderSetting minPressDelay;
    private final SliderSetting maxPressDelay;
    private final SliderSetting minHoldTime;
    private final SliderSetting maxHoldTime;
    private final SliderSetting minReleaseDelay;
    private final SliderSetting maxReleaseDelay;
    
    // Humanization settings
    private final ButtonSetting enableMissedClicks;
    private final SliderSetting missedClickChance;
    private final ButtonSetting enableNaturalPauses;
    private final SliderSetting pauseChance;
    private final SliderSetting pauseDuration;
    // Press/Release delay over-randomization settings
    private final ButtonSetting enablePressReleaseOverRandomization;
    private final SliderSetting pressReleaseCpsRandomization;
    private final SliderSetting pressReleaseDelayRandomization;
    private final SliderSetting pressReleaseTimingVariation;
    
    private final boolean leftClick;
    private final boolean rightClick;
    private final boolean always;

    private final CoolDown clickStopWatch = new CoolDown(0);
    private int ticksDown;
    private long nextSwing;
    private final AtomicBoolean isClicking = new AtomicBoolean(false);
    private long clickingStartTime = 0;
    private static final long MAX_CLICKING_TIMEOUT = 5000; // 5 seconds timeout
    
    // Spike pattern state
    private boolean inSpike = false;
    private int spikeTicksRemaining = 0;
    private int burstClicksRemaining = 0;
    private long lastSpikeTime = 0;
    
    // StdDev drift to defeat anti-cheat variance checks
    private long stdDevDriftNextUpdate = 0;
    private double stdDevDrift = 1.0;

    public NormalAutoClicker(String name, @NotNull IAutoClicker parent, boolean left, boolean always) {
        super(name, parent);
        this.leftClick = left;
        this.rightClick = !left;
        this.always = always;

        minCPS = new SliderSetting("Min CPS", 8, 1, 40, 0.1);
        maxCPS = new SliderSetting("Max CPS", 14, 1, 40, 0.1);
        cpsLimiter = new SliderSetting("CPS limiter", 20, 1, 40, 0.1);
        butterFly = new ButtonSetting("Butterfly", true);
        
        // Over-randomization settings (for base CPS/delay)
        enableOverRandomization = new ButtonSetting("Over-randomization", true);
        cpsRandomization = new SliderSetting("CPS randomization", 15, 0, 50, 1, "%", enableOverRandomization::isToggled);
        delayRandomization = new SliderSetting("Delay randomization", 20, 0, 50, 1, "%", enableOverRandomization::isToggled);
        timingVariation = new SliderSetting("Timing variation", 10, 0, 30, 1, "%", enableOverRandomization::isToggled);
        
        // Spike/Burst settings for chaotic patterns
        enableSpikePatterns = new ButtonSetting("Spike patterns", true);
        spikeFrequency = new SliderSetting("Spike frequency", 25, 5, 80, 1, "%", enableSpikePatterns::isToggled);
        spikeIntensity = new SliderSetting("Spike intensity", 2.5, 1.5, 5.0, 0.1, "x", enableSpikePatterns::isToggled);
        spikeDuration = new SliderSetting("Spike duration", 3, 1, 10, 1, "clicks", enableSpikePatterns::isToggled);
        burstChance = new SliderSetting("Burst chance", 15, 0, 50, 1, "%", enableSpikePatterns::isToggled);
        burstSize = new SliderSetting("Burst size", 3, 2, 8, 1, "clicks", enableSpikePatterns::isToggled);
        // Spike/Burst over-randomization settings
        enableSpikeOverRandomization = new ButtonSetting("Spike over-randomization", true, enableSpikePatterns::isToggled);
        spikeCpsRandomization = new SliderSetting("Spike CPS randomization", 15, 0, 50, 1, "%", () -> enableSpikePatterns.isToggled() && enableSpikeOverRandomization.isToggled());
        spikeDelayRandomization = new SliderSetting("Spike delay randomization", 20, 0, 50, 1, "%", () -> enableSpikePatterns.isToggled() && enableSpikeOverRandomization.isToggled());
        spikeTimingVariation = new SliderSetting("Spike timing variation", 10, 0, 30, 1, "%", () -> enableSpikePatterns.isToggled() && enableSpikeOverRandomization.isToggled());
        
        // Chaotic timing settings
        enableChaoticTiming = new ButtonSetting("Chaotic timing", true);
        irregularityFactor = new SliderSetting("Irregularity", 40, 0, 100, 1, "%", enableChaoticTiming::isToggled);
        jitterAmount = new SliderSetting("Jitter amount", 30, 0, 100, 1, "%", enableChaoticTiming::isToggled);
        patternVariation = new SliderSetting("Pattern variation", 50, 0, 100, 1, "%", enableChaoticTiming::isToggled);
        
        // Press/Release delay settings
        enablePressReleaseDelay = new ButtonSetting("Press/Release delay", true);
        minPressDelay = new SliderSetting("Min press delay", 0, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        maxPressDelay = new SliderSetting("Max press delay", 5, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        minHoldTime = new SliderSetting("Min hold time", 5, 1, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        maxHoldTime = new SliderSetting("Max hold time", 25, 1, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        minReleaseDelay = new SliderSetting("Min release delay", 0, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        maxReleaseDelay = new SliderSetting("Max release delay", 5, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        // Press/Release delay over-randomization settings
        enablePressReleaseOverRandomization = new ButtonSetting("Press/Release over-randomization", true, enablePressReleaseDelay::isToggled);
        pressReleaseCpsRandomization = new SliderSetting("Press/Release CPS randomization", 15, 0, 50, 1, "%", () -> enablePressReleaseDelay.isToggled() && enablePressReleaseOverRandomization.isToggled());
        pressReleaseDelayRandomization = new SliderSetting("Press/Release delay randomization", 20, 0, 50, 1, "%", () -> enablePressReleaseDelay.isToggled() && enablePressReleaseOverRandomization.isToggled());
        pressReleaseTimingVariation = new SliderSetting("Press/Release timing variation", 10, 0, 30, 1, "%", () -> enablePressReleaseDelay.isToggled() && enablePressReleaseOverRandomization.isToggled());
        
        // Humanization settings
        enableMissedClicks = new ButtonSetting("Missed clicks", true);
        missedClickChance = new SliderSetting("Miss chance", 2, 0, 10, 0.1, "%", enableMissedClicks::isToggled);
        enableNaturalPauses = new ButtonSetting("Natural pauses", true);
        pauseChance = new SliderSetting("Pause chance", 3, 0, 15, 0.1, "%", enableNaturalPauses::isToggled);
        pauseDuration = new SliderSetting("Pause duration", 50, 20, 200, 5, "ms", enableNaturalPauses::isToggled);
        
        this.registerSetting(minCPS, maxCPS, cpsLimiter, butterFly, 
                enableOverRandomization, cpsRandomization, delayRandomization, timingVariation,
                enableSpikePatterns, spikeFrequency, spikeIntensity, spikeDuration, burstChance, burstSize,
                enableSpikeOverRandomization, spikeCpsRandomization, spikeDelayRandomization, spikeTimingVariation,
                enableChaoticTiming, irregularityFactor, jitterAmount, patternVariation,
                enablePressReleaseDelay, minPressDelay, maxPressDelay, minHoldTime, maxHoldTime, minReleaseDelay, maxReleaseDelay,
                enablePressReleaseOverRandomization, pressReleaseCpsRandomization, pressReleaseDelayRandomization, pressReleaseTimingVariation,
                enableMissedClicks, missedClickChance, enableNaturalPauses, pauseChance, pauseDuration);
    }

    @Override
    public void guiUpdate() {
        Utils.correctValue(minCPS, maxCPS);
        Utils.correctValue(minPressDelay, maxPressDelay);
        Utils.correctValue(minHoldTime, maxHoldTime);
        Utils.correctValue(minReleaseDelay, maxReleaseDelay);
    }

    @Override
    public void onUpdate() {
        // Check if KillAura is using AutoClicker settings - if so, treat as always=true
        // True when this submode is running under KillAura's own click mode
        boolean parentIsKillAura = parent instanceof KillAura;
        boolean killAuraActive = ModuleManager.killAura != null && ModuleManager.killAura.isEnabled()
                && ModuleManager.killAura.useAutoClickerSettings.isToggled();

        // Only halt when we're inside KillAura and it has no target; standalone autoclicker
        // should keep running even if KillAura is enabled elsewhere.
        if (parentIsKillAura && KillAura.target == null) {
            return;
        }
        
        // Safety check: Reset stuck clicking flag
        if (isClicking.get()) {
            long currentTime = System.currentTimeMillis();
            if (clickingStartTime > 0 && (currentTime - clickingStartTime) > MAX_CLICKING_TIMEOUT) {
                // Flag has been stuck for too long, reset it
                isClicking.set(false);
                clickingStartTime = 0;
            }
        }
        
        clickStopWatch.setCooldown(nextSwing);
        if (clickStopWatch.hasFinished() && !isClicking.get()) {
            // Handle spike pattern state
            if (enableSpikePatterns.isToggled()) {
                // Check if we should start a new spike
                if (!inSpike && spikeTicksRemaining <= 0) {
                    if (Math.random() * 100 < spikeFrequency.getInput()) {
                        inSpike = true;
                        spikeTicksRemaining = (int) Utils.randomizeDouble(1, spikeDuration.getInput());
                    }
                }
                
                // Check for burst pattern
                if (burstClicksRemaining <= 0 && Math.random() * 100 < burstChance.getInput()) {
                    burstClicksRemaining = (int) Utils.randomizeDouble(2, burstSize.getInput());
                }
            }
            
            // Check for missed clicks (human-like behavior)
            if (enableMissedClicks.isToggled() && Math.random() * 100 < missedClickChance.getInput()) {
                // Skip this click cycle - humans sometimes miss clicks
                this.clickStopWatch.start();
                return;
            }
            
            // Check for natural pauses (human-like behavior)
            if (enableNaturalPauses.isToggled() && Math.random() * 100 < pauseChance.getInput()) {
                // Add a natural pause - humans sometimes pause briefly
                long pauseTime = (long) Utils.randomizeGaussian(
                    pauseDuration.getInput(), 
                    pauseDuration.getInput() * 0.3,
                    pauseDuration.getInput() * 0.5,
                    pauseDuration.getInput() * 1.5
                );
                this.nextSwing = pauseTime;
                this.clickStopWatch.start();
                return;
            }
            
            // Calculate base CPS with over-randomization (randomized every click)
            // Use Gaussian distribution for more human-like CPS variation
            // If running under KillAura or being reused by it, use its CPS; otherwise keep
            // the standalone AutoClicker CPS values.
            double baseCPS;
            if ((parentIsKillAura || killAuraActive) && ModuleManager.killAura != null) {
                double[] cpsRange = ModuleManager.killAura.getCurrentCPSRange();
                double mean = (cpsRange[0] + cpsRange[1]) / 2.0;
                double stdDev = (cpsRange[1] - cpsRange[0]) / 6.0; // 3-sigma rule
                baseCPS = Utils.randomizeGaussian(mean, stdDev, cpsRange[0], cpsRange[1]);
            } else {
                double mean = (minCPS.getInput() + maxCPS.getInput()) / 2.0;
                double stdDev = (maxCPS.getInput() - minCPS.getInput()) / 6.0; // 3-sigma rule
                baseCPS = Utils.randomizeGaussian(mean, stdDev, minCPS.getInput(), maxCPS.getInput());
            }
            
            // Apply spike pattern (create spikes in CPS)
            if (enableSpikePatterns.isToggled() && inSpike) {
                // Use Gaussian for more natural spike intensity
                double spikeMean = spikeIntensity.getInput();
                double spikeStdDev = spikeIntensity.getInput() * 0.1;
                double spikeMultiplier = Utils.randomizeGaussian(spikeMean, spikeStdDev, spikeIntensity.getInput() * 0.8, spikeIntensity.getInput() * 1.2);
                baseCPS *= spikeMultiplier;
                
                // Apply spike-specific over-randomization using Gaussian
                if (enableSpikeOverRandomization.isToggled()) {
                    double spikeCpsRandomPercent = spikeCpsRandomization.getInput() / 100.0;
                    double spikeCpsRandomFactor = 1.0 + Utils.randomizeGaussian(0, spikeCpsRandomPercent * 0.33, -spikeCpsRandomPercent, spikeCpsRandomPercent);
                    baseCPS *= spikeCpsRandomFactor;
                }
                
                spikeTicksRemaining--;
                if (spikeTicksRemaining <= 0) {
                    inSpike = false;
                    // Add random cooldown after spike
                    lastSpikeTime = System.currentTimeMillis();
                }
            }
            
            // Apply burst pattern (rapid clicks)
            boolean isBurst = false;
            if (enableSpikePatterns.isToggled() && burstClicksRemaining > 0) {
                // Use Gaussian for more natural burst intensity
                double burstMean = 3.0;
                double burstStdDev = 0.5;
                double burstMultiplier = Utils.randomizeGaussian(burstMean, burstStdDev, 2.0, 4.0);
                baseCPS *= burstMultiplier;
                
                // Apply spike-specific over-randomization for bursts using Gaussian
                if (enableSpikeOverRandomization.isToggled()) {
                    double spikeCpsRandomPercent = spikeCpsRandomization.getInput() / 100.0;
                    double spikeCpsRandomFactor = 1.0 + Utils.randomizeGaussian(0, spikeCpsRandomPercent * 0.33, -spikeCpsRandomPercent, spikeCpsRandomPercent);
                    baseCPS *= spikeCpsRandomFactor;
                }
                
                burstClicksRemaining--;
                isBurst = true;
            }
            
            if (enableOverRandomization.isToggled()) {
                // Apply CPS randomization using Gaussian for more natural variation
                double cpsRandomPercent = cpsRandomization.getInput() / 100.0;
                double cpsRandomFactor = 1.0 + Utils.randomizeGaussian(0, cpsRandomPercent * 0.33, -cpsRandomPercent, cpsRandomPercent);
                baseCPS *= cpsRandomFactor;
            }

            // Frequency / Consistency Check Bypass:
            // We need to actively vary the "movements" (ticks between clicks).
            // 10-14 CPS results in mostly 1 or 2 ticks delay.
            // To increase deviation and reduce duplicates (AutoClicker A/E), we need occassional 3-tick delays (~6 CPS) and 0-tick delays (bursts).
            // Randomly drop CPS significantly to force a 3+ tick delay.
            // Moved here to ensure it overrides spikes/randomization multipliers.
            boolean forceDrop = false;
            if (Math.random() < 0.22) { // 22% chance to drop CPS (increased from 18%)
                baseCPS = Utils.randomizeDouble(3, 7); // Forces ~3-7 ticks delay (definite pauses)
                forceDrop = true;
            }

            boolean shouldClick = Mouse.isButtonDown(0) || always || parentIsKillAura || killAuraActive;
            
            if (shouldClick) {
                ticksDown++;
            } else {
                ticksDown = 0;
            }

            // Calculate next swing delay with chaotic timing (randomized every click)
            // Removed aggressive butterfly compensation that forces fast clicks after slow ones
            if (this.nextSwing >= 50 * 2 && butterFly.isToggled() && Math.random() < 0.3) {
                // Occasionally allow a fast burst after a slow click, but not always
                this.nextSwing = (long) (Math.random() * 100);
            } else {
                // Calculate base delay from CPS (randomized every click)
                // Use Gaussian distribution for more human-like timing
                long baseDelay = (long) (1000.0 / baseCPS);
                
                // Apply chaotic timing variations (randomized every click)
                if (enableChaoticTiming.isToggled()) {
                    // Irregularity factor - makes timing completely unpredictable using Gaussian
                    double irregularityPercent = irregularityFactor.getInput() / 100.0;
                    double irregularity = Utils.randomizeGaussian(0, irregularityPercent * 0.33, -irregularityPercent, irregularityPercent);
                    baseDelay = (long) (baseDelay * (1.0 + irregularity));
                    
                    // Jitter - random micro-adjustments using Gaussian for natural variation
                    double jitterPercent = jitterAmount.getInput() / 100.0;
                    double jitter = Utils.randomizeGaussian(0, jitterPercent * 0.33, -jitterPercent, jitterPercent);
                    baseDelay = (long) (baseDelay * (1.0 + jitter));
                    
                    // Pattern variation - breaks any predictable patterns
                    double patternVarPercent = patternVariation.getInput() / 100.0;
                    double patternVar = Utils.randomizeGaussian(0, patternVarPercent * 0.33, -patternVarPercent, patternVarPercent);
                    baseDelay = (long) (baseDelay * (1.0 + patternVar));
                    
                    // Additional chaos layer - Gaussian multiplier for more natural variation
                    double chaosMean = 1.0;
                    double chaosStdDev = 0.25;
                    double chaosMultiplier = Utils.randomizeGaussian(chaosMean, chaosStdDev, 0.3, 1.8);
                    baseDelay = (long) (baseDelay * chaosMultiplier);
                }
                
                // Anti stdDev checks: keep variance moving over time instead of converging.
                // We periodically drift the multiplier and add per-click spread so rolling
                // stdDev never stabilizes at a narrow band.
                long now = System.currentTimeMillis();
                if (now >= stdDevDriftNextUpdate) {
                    stdDevDrift = 1.0 + Utils.randomizeGaussian(0, 0.12, -0.28, 0.28);
                    stdDevDriftNextUpdate = now + (long) Utils.randomizeDouble(350, 900);
                }
                double stdDevSpread = 1.0 + (Math.random() - 0.5) * 0.36; // per-click widen/narrow
                baseDelay = (long) (baseDelay * stdDevDrift * stdDevSpread);
                
                if (enableOverRandomization.isToggled()) {
                    // Apply delay randomization using Gaussian for more natural variation
                    double delayRandomPercent = delayRandomization.getInput() / 100.0;
                    double delayRandomFactor = 1.0 + Utils.randomizeGaussian(0, delayRandomPercent * 0.33, -delayRandomPercent, delayRandomPercent);
                    baseDelay = (long) (baseDelay * delayRandomFactor);
                    
                    // Apply timing variation (micro-adjustments) using Gaussian
                    double timingVarPercent = timingVariation.getInput() / 100.0;
                    double timingVar = Utils.randomizeGaussian(0, timingVarPercent * 0.33, -timingVarPercent, timingVarPercent);
                    baseDelay = (long) (baseDelay * (1.0 + timingVar));
                }
                
                // Add micro-variations that humans naturally have (always applied)
                // Small Gaussian noise to break perfect timing patterns
                double microVariation = Utils.randomizeGaussian(0, 0.02, -0.05, 0.05); // ±2% micro-variation
                baseDelay = (long) (baseDelay * (1.0 + microVariation));
                
                // For bursts, make delay extremely short to create spikes (randomized every click)
                if (isBurst) {
                    // Use Gaussian for more natural burst delay
                    // Allow near-zero delay for same-tick bursts (movements=0)
                    double burstDelayMean = 0.2;
                    double burstDelayStdDev = 0.05;
                    double burstDelayMultiplier = Utils.randomizeGaussian(burstDelayMean, burstDelayStdDev, 0.0, 0.3);
                    baseDelay = (long) (baseDelay * burstDelayMultiplier);
                    
                    // Apply spike-specific delay randomization using Gaussian
                    if (enableSpikeOverRandomization.isToggled()) {
                        double spikeDelayRandomPercent = spikeDelayRandomization.getInput() / 100.0;
                        double spikeDelayRandomFactor = 1.0 + Utils.randomizeGaussian(0, spikeDelayRandomPercent * 0.33, -spikeDelayRandomPercent, spikeDelayRandomPercent);
                        baseDelay = (long) (baseDelay * spikeDelayRandomFactor);
                        
                        // Apply spike-specific timing variation using Gaussian
                        double spikeTimingVarPercent = spikeTimingVariation.getInput() / 100.0;
                        double spikeTimingVar = Utils.randomizeGaussian(0, spikeTimingVarPercent * 0.33, -spikeTimingVarPercent, spikeTimingVarPercent);
                        baseDelay = (long) (baseDelay * (1.0 + spikeTimingVar));
                    }
                }
                
                // Enforce CPS limiter by ensuring delay is never less than minimum required
                // This ensures CPS never exceeds the limit, even with all modifications (spikes, bursts, etc.)
                // If KillAura is using autoclicker settings or parent is KillAura, use KillAura's max CPS (hard limit) as limiter
                // Otherwise, use autoclicker's maxCPS as limiter (not cpsLimiter, as that's a separate setting)
                double maxAllowedCPS;
                if ((parentIsKillAura || killAuraActive) && ModuleManager.killAura != null) {
                    maxAllowedCPS = ModuleManager.killAura.maxCPS.getInput(); // KillAura hard limit
                } else {
                    maxAllowedCPS = maxCPS.getInput(); // AutoClicker max CPS
                }
                long minDelayForLimit = (long) (1000.0 / maxAllowedCPS);
                
                // If we forced a drop, don't let limiter speed it up excessively, but ensure we don't go super slow if not intended
                if (!forceDrop) {
                     baseDelay = Math.max(baseDelay, minDelayForLimit);
                }
                
                this.nextSwing = Math.max(1, baseDelay);
            }

            // Handle clicks with press/release delay
            if (rightClick && ((Mouse.isButtonDown(1) && !Mouse.isButtonDown(0)) || always)) {
                performClickWithDelay();
                
                // Butterfly mode double click chance with randomization
                if (butterFly.isToggled() && Math.random() > 0.9) {
                    // Add small random delay before double click
                    long doubleClickDelay = enableOverRandomization.isToggled() 
                        ? (long) Utils.randomizeDouble(1, 10) 
                        : 5;
                    Raven.getExecutor().schedule(() -> {
                        try {
                            performClickWithDelay();
                        } catch (Exception e) {
                            // Ignore errors in scheduled tasks
                        }
                    }, doubleClickDelay, TimeUnit.MILLISECONDS);
                }
                
                // Additional burst clicks for more spikes
                if (isBurst && Math.random() > 0.5) {
                    long burstDelay = (long) Utils.randomizeDouble(1, 5);
                    Raven.getExecutor().schedule(() -> {
                        try {
                            performClickWithDelay();
                        } catch (Exception e) {
                            // Ignore errors in scheduled tasks
                        }
                    }, burstDelay, TimeUnit.MILLISECONDS);
                }
            }

            if (leftClick && ticksDown > 1 && (!Mouse.isButtonDown(1) || always || parentIsKillAura)) {
                performClickWithDelay();
                
                // Additional burst clicks for more spikes
                if (isBurst && Math.random() > 0.5) {
                    long burstDelay = (long) Utils.randomizeDouble(1, 5);
                    Raven.getExecutor().schedule(() -> {
                        try {
                            performClickWithDelay();
                        } catch (Exception e) {
                            // Ignore errors in scheduled tasks
                        }
                    }, burstDelay, TimeUnit.MILLISECONDS);
                }
            }

            this.clickStopWatch.start();
        }
    }
    
    private void performClickWithDelay() {
        if (isClicking.get()) {
            // Check if it's been stuck for too long
            long currentTime = System.currentTimeMillis();
            if (clickingStartTime > 0 && (currentTime - clickingStartTime) > MAX_CLICKING_TIMEOUT) {
                // Reset stuck flag
                isClicking.set(false);
                clickingStartTime = 0;
            } else {
                return;
            }
        }
        
        isClicking.set(true);
        clickingStartTime = System.currentTimeMillis();
        
        // Determine which button to use (0 = left, 1 = right)
        final int button = leftClick ? 0 : 1;
        
        if (enablePressReleaseDelay.isToggled()) {
            // Calculate randomized delays with multiple layers of chaos
            // Use Gaussian distribution for more human-like press/release timing
            double pressMean = (minPressDelay.getInput() + maxPressDelay.getInput()) / 2.0;
            double pressStdDev = (maxPressDelay.getInput() - minPressDelay.getInput()) / 6.0;
            long pressDelayCalc = (long) Utils.randomizeGaussian(pressMean, pressStdDev, minPressDelay.getInput(), maxPressDelay.getInput());
            
            double holdMean = (minHoldTime.getInput() + maxHoldTime.getInput()) / 2.0;
            double holdStdDev = (maxHoldTime.getInput() - minHoldTime.getInput()) / 6.0;
            long holdTimeCalc = (long) Utils.randomizeGaussian(holdMean, holdStdDev, minHoldTime.getInput(), maxHoldTime.getInput());
            
            double releaseMean = (minReleaseDelay.getInput() + maxReleaseDelay.getInput()) / 2.0;
            double releaseStdDev = (maxReleaseDelay.getInput() - minReleaseDelay.getInput()) / 6.0;
            long releaseDelayCalc = (long) Utils.randomizeGaussian(releaseMean, releaseStdDev, minReleaseDelay.getInput(), maxReleaseDelay.getInput());
            
            // Apply chaotic timing variations using Gaussian
            if (enableChaoticTiming.isToggled()) {
                // Add irregularity to press delay
                double pressIrregularityPercent = (irregularityFactor.getInput() * 0.5) / 100.0;
                double pressIrregularity = Utils.randomizeGaussian(0, pressIrregularityPercent * 0.33, -pressIrregularityPercent, pressIrregularityPercent);
                pressDelayCalc = (long) (pressDelayCalc * (1.0 + pressIrregularity));
                
                // Add jitter to hold time - humans have variable hold times
                double holdJitterPercent = (jitterAmount.getInput() * 0.3) / 100.0;
                double holdJitter = Utils.randomizeGaussian(0, holdJitterPercent * 0.33, -holdJitterPercent, holdJitterPercent);
                holdTimeCalc = (long) (holdTimeCalc * (1.0 + holdJitter));
                
                // Add pattern variation to release delay
                double releasePatternVarPercent = (patternVariation.getInput() * 0.4) / 100.0;
                double releasePatternVar = Utils.randomizeGaussian(0, releasePatternVarPercent * 0.33, -releasePatternVarPercent, releasePatternVarPercent);
                releaseDelayCalc = (long) (releaseDelayCalc * (1.0 + releasePatternVar));
            }
            
            // Apply press/release delay-specific over-randomization using Gaussian
            if (enablePressReleaseOverRandomization.isToggled()) {
                // Apply CPS randomization to delays
                double cpsRandomPercent = pressReleaseCpsRandomization.getInput() / 100.0;
                double pressReleaseCpsRandomFactor = 1.0 + Utils.randomizeGaussian(0, cpsRandomPercent * 0.33, -cpsRandomPercent, cpsRandomPercent);
                pressDelayCalc = (long) (pressDelayCalc * pressReleaseCpsRandomFactor);
                holdTimeCalc = (long) (holdTimeCalc * pressReleaseCpsRandomFactor);
                releaseDelayCalc = (long) (releaseDelayCalc * pressReleaseCpsRandomFactor);
                
                // Apply delay randomization
                double delayRandomPercent = pressReleaseDelayRandomization.getInput() / 100.0;
                double pressReleaseDelayRandomFactor = 1.0 + Utils.randomizeGaussian(0, delayRandomPercent * 0.33, -delayRandomPercent, delayRandomPercent);
                pressDelayCalc = (long) (pressDelayCalc * pressReleaseDelayRandomFactor);
                holdTimeCalc = (long) (holdTimeCalc * pressReleaseDelayRandomFactor);
                releaseDelayCalc = (long) (releaseDelayCalc * pressReleaseDelayRandomFactor);
                
                // Apply timing variation
                double timingVarPercent = pressReleaseTimingVariation.getInput() / 100.0;
                double pressReleaseTimingVar = Utils.randomizeGaussian(0, timingVarPercent * 0.33, -timingVarPercent, timingVarPercent);
                pressDelayCalc = (long) (pressDelayCalc * (1.0 + pressReleaseTimingVar));
                holdTimeCalc = (long) (holdTimeCalc * (1.0 + pressReleaseTimingVar));
                releaseDelayCalc = (long) (releaseDelayCalc * (1.0 + pressReleaseTimingVar));
            }
            
            // Always add micro-variations to press/release timing for human-like behavior
            double pressMicroVar = Utils.randomizeGaussian(0, 0.015, -0.03, 0.03);
            double holdMicroVar = Utils.randomizeGaussian(0, 0.02, -0.04, 0.04);
            double releaseMicroVar = Utils.randomizeGaussian(0, 0.015, -0.03, 0.03);
            pressDelayCalc = (long) (pressDelayCalc * (1.0 + pressMicroVar));
            holdTimeCalc = (long) (holdTimeCalc * (1.0 + holdMicroVar));
            releaseDelayCalc = (long) (releaseDelayCalc * (1.0 + releaseMicroVar));
            
            // Ensure minimum values
            pressDelayCalc = Math.max(0, pressDelayCalc);
            holdTimeCalc = Math.max(1, holdTimeCalc);
            releaseDelayCalc = Math.max(0, releaseDelayCalc);
            
            // Create final copies for use in lambda expressions
            final long pressDelay = pressDelayCalc;
            final long holdTime = holdTimeCalc;
            long finalReleaseDelay = releaseDelayCalc;
            
            // Add chaotic + micro variation to the delay that separates press and release.
            // This delay was previously only used to delay the flag reset, which caused
            // near-instant releases and easy detection. Now it actually spaces the release event.
            if (enableChaoticTiming.isToggled() && Math.random() > 0.7) {
                double extraDelay = Utils.randomizeGaussian(5, 2, 0, 15);
                finalReleaseDelay += (long) extraDelay;
            }
            double finalReleaseMicroVar = Utils.randomizeGaussian(0, 0.01, -0.02, 0.02);
            finalReleaseDelay = (long) (finalReleaseDelay * (1.0 + finalReleaseMicroVar));
            finalReleaseDelay = Math.max(0, finalReleaseDelay);
            
            // Total time from press to release
            final long releaseScheduleDelay = Math.max(0, holdTime + finalReleaseDelay);
            
            // Schedule press with delay
            Raven.getExecutor().schedule(() -> {
                try {
                    // Use parent.click() to respect all parent logic (block checks, inventory, etc.)
                    // This sends the press event
                    boolean clicked = parent.click();
                    
                    if (clicked) {
                        // Schedule release after hold + release delays
                        Raven.getExecutor().schedule(() -> {
                            try {
                                Utils.sendClick(button, false);
                            } catch (Exception ignored) {
                                // fall through to flag reset
                            } finally {
                                isClicking.set(false);
                                clickingStartTime = 0;
                            }
                        }, releaseScheduleDelay, TimeUnit.MILLISECONDS);
                    } else {
                        isClicking.set(false);
                        clickingStartTime = 0;
                    }
                } catch (Exception e) {
                    // Ensure flag is reset even on error
                    isClicking.set(false);
                    clickingStartTime = 0;
                }
            }, pressDelay, TimeUnit.MILLISECONDS);
        } else {
            // Original behavior without delays
            try {
                parent.click();
            } catch (Exception e) {
                // Ignore errors but ensure flag is reset
            } finally {
                isClicking.set(false);
                clickingStartTime = 0;
            }
        }
    }
}