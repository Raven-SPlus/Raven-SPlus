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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalAutoClicker extends SubMode<IAutoClicker> {
    private static final long MAX_CLICKING_TIMEOUT = 5000L;
    private static final int TIMING_WINDOW_SIZE = 18;

    private final SliderSetting minCPS;
    private final SliderSetting maxCPS;
    private final SliderSetting cpsLimiter;
    private final ButtonSetting butterFly;

    private final ButtonSetting enableOverRandomization;
    private final SliderSetting cpsRandomization;
    private final SliderSetting delayRandomization;
    private final SliderSetting timingVariation;

    private final ButtonSetting enableSpikePatterns;
    private final SliderSetting spikeFrequency;
    private final SliderSetting spikeIntensity;
    private final SliderSetting spikeDuration;
    private final SliderSetting burstChance;
    private final SliderSetting burstSize;
    private final ButtonSetting enableSpikeOverRandomization;
    private final SliderSetting spikeCpsRandomization;
    private final SliderSetting spikeDelayRandomization;
    private final SliderSetting spikeTimingVariation;

    private final ButtonSetting enableChaoticTiming;
    private final SliderSetting irregularityFactor;
    private final SliderSetting jitterAmount;
    private final SliderSetting patternVariation;

    private final ButtonSetting enablePressReleaseDelay;
    private final SliderSetting minPressDelay;
    private final SliderSetting maxPressDelay;
    private final SliderSetting minHoldTime;
    private final SliderSetting maxHoldTime;
    private final SliderSetting minReleaseDelay;
    private final SliderSetting maxReleaseDelay;

    private final ButtonSetting enableMissedClicks;
    private final SliderSetting missedClickChance;
    private final ButtonSetting enableNaturalPauses;
    private final SliderSetting pauseChance;
    private final SliderSetting pauseDuration;
    private final ButtonSetting enablePressReleaseOverRandomization;
    private final SliderSetting pressReleaseCpsRandomization;
    private final SliderSetting pressReleaseDelayRandomization;
    private final SliderSetting pressReleaseTimingVariation;

    private final boolean leftClick;
    private final boolean rightClick;
    private final boolean always;

    private final CoolDown clickStopWatch = new CoolDown(0);
    private final AtomicBoolean isClicking = new AtomicBoolean(false);
    private final Object timingLock = new Object();
    private final ArrayDeque<Long> emittedClickTimes = new ArrayDeque<>();
    private final ArrayDeque<Long> recentIntervals = new ArrayDeque<>();
    private final ArrayDeque<Integer> recentBuckets = new ArrayDeque<>();
    private final Random sessionRandom = new Random();

    private int ticksDown;
    private long nextSwing;
    private long clickingStartTime;
    private long lastActualClickTime = -1L;

    private boolean inSpike;
    private int spikeTicksRemaining;
    private int burstClicksRemaining;

    private TempoState tempoState = TempoState.STEADY;
    private long tempoStateUntil;
    private double driftingCps = -1.0;
    private double driftVelocity;
    private long driftRetargetAt;
    private double sessionCpsBias = 1.0;
    private double sessionDelayBias = 1.0;

    public NormalAutoClicker(String name, @NotNull IAutoClicker parent, boolean left, boolean always) {
        super(name, parent);
        this.leftClick = left;
        this.rightClick = !left;
        this.always = always;

        minCPS = new SliderSetting("Min CPS", 8, 1, 40, 0.1);
        maxCPS = new SliderSetting("Max CPS", 14, 1, 40, 0.1);
        cpsLimiter = new SliderSetting("CPS limiter", 20, 1, 40, 0.1);
        butterFly = new ButtonSetting("Butterfly", true);

        enableOverRandomization = new ButtonSetting("Over-randomization", true);
        cpsRandomization = new SliderSetting("CPS randomization", 15, 0, 50, 1, "%", enableOverRandomization::isToggled);
        delayRandomization = new SliderSetting("Delay randomization", 20, 0, 50, 1, "%", enableOverRandomization::isToggled);
        timingVariation = new SliderSetting("Timing variation", 10, 0, 30, 1, "%", enableOverRandomization::isToggled);

        enableSpikePatterns = new ButtonSetting("Spike patterns", true);
        spikeFrequency = new SliderSetting("Spike frequency", 25, 5, 80, 1, "%", enableSpikePatterns::isToggled);
        spikeIntensity = new SliderSetting("Spike intensity", 2.5, 1.5, 5.0, 0.1, "x", enableSpikePatterns::isToggled);
        spikeDuration = new SliderSetting("Spike duration", 3, 1, 10, 1, "clicks", enableSpikePatterns::isToggled);
        burstChance = new SliderSetting("Burst chance", 15, 0, 50, 1, "%", enableSpikePatterns::isToggled);
        burstSize = new SliderSetting("Burst size", 3, 2, 8, 1, "clicks", enableSpikePatterns::isToggled);
        enableSpikeOverRandomization = new ButtonSetting("Spike over-randomization", true, enableSpikePatterns::isToggled);
        spikeCpsRandomization = new SliderSetting("Spike CPS randomization", 15, 0, 50, 1, "%", () -> enableSpikePatterns.isToggled() && enableSpikeOverRandomization.isToggled());
        spikeDelayRandomization = new SliderSetting("Spike delay randomization", 20, 0, 50, 1, "%", () -> enableSpikePatterns.isToggled() && enableSpikeOverRandomization.isToggled());
        spikeTimingVariation = new SliderSetting("Spike timing variation", 10, 0, 30, 1, "%", () -> enableSpikePatterns.isToggled() && enableSpikeOverRandomization.isToggled());

        enableChaoticTiming = new ButtonSetting("Chaotic timing", true);
        irregularityFactor = new SliderSetting("Irregularity", 40, 0, 100, 1, "%", enableChaoticTiming::isToggled);
        jitterAmount = new SliderSetting("Jitter amount", 30, 0, 100, 1, "%", enableChaoticTiming::isToggled);
        patternVariation = new SliderSetting("Pattern variation", 50, 0, 100, 1, "%", enableChaoticTiming::isToggled);

        enablePressReleaseDelay = new ButtonSetting("Press/Release delay", true);
        minPressDelay = new SliderSetting("Min press delay", 0, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        maxPressDelay = new SliderSetting("Max press delay", 5, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        minHoldTime = new SliderSetting("Min hold time", 5, 1, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        maxHoldTime = new SliderSetting("Max hold time", 25, 1, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        minReleaseDelay = new SliderSetting("Min release delay", 0, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        maxReleaseDelay = new SliderSetting("Max release delay", 5, 0, 50, 1, "ms", enablePressReleaseDelay::isToggled);
        enablePressReleaseOverRandomization = new ButtonSetting("Press/Release over-randomization", true, enablePressReleaseDelay::isToggled);
        pressReleaseCpsRandomization = new SliderSetting("Press/Release CPS randomization", 15, 0, 50, 1, "%", () -> enablePressReleaseDelay.isToggled() && enablePressReleaseOverRandomization.isToggled());
        pressReleaseDelayRandomization = new SliderSetting("Press/Release delay randomization", 20, 0, 50, 1, "%", () -> enablePressReleaseDelay.isToggled() && enablePressReleaseOverRandomization.isToggled());
        pressReleaseTimingVariation = new SliderSetting("Press/Release timing variation", 10, 0, 30, 1, "%", () -> enablePressReleaseDelay.isToggled() && enablePressReleaseOverRandomization.isToggled());

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
    public void onEnable() {
        resetTimingState();
    }

    @Override
    public void onDisable() {
        resetTimingState();
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
        boolean parentIsKillAura = parent instanceof KillAura;
        boolean killAuraActive = ModuleManager.killAura != null && ModuleManager.killAura.isEnabled()
                && ModuleManager.killAura.useAutoClickerSettings.isToggled();

        if (parentIsKillAura && KillAura.target == null) {
            return;
        }

        resetStuckClickState();

        boolean leftActive = Mouse.isButtonDown(0) || always || parentIsKillAura || killAuraActive;
        boolean rightActive = (Mouse.isButtonDown(1) && !Mouse.isButtonDown(0)) || always;

        if (leftClick) {
            ticksDown = leftActive ? ticksDown + 1 : 0;
            if (!leftActive) {
                clearPendingSwing();
                return;
            }
        } else if (!rightActive) {
            clearPendingSwing();
            return;
        }

        clickStopWatch.setCooldown(nextSwing);
        if (!clickStopWatch.hasFinished() || isClicking.get()) {
            return;
        }

        double hardLimitCps = getHardLimitCps(parentIsKillAura, killAuraActive);
        long now = System.currentTimeMillis();
        if (!canEmitClick(now, hardLimitCps)) {
            nextSwing = getLimiterRecoveryDelay(now, hardLimitCps);
            clickStopWatch.start();
            return;
        }

        TimingPlan plan = buildTimingPlan(parentIsKillAura, killAuraActive, hardLimitCps);
        nextSwing = plan.nextSwing;

        boolean shouldClick = false;
        if (rightClick && rightActive) {
            shouldClick = true;
        }
        if (leftClick && ticksDown > 1 && (!Mouse.isButtonDown(1) || always || parentIsKillAura)) {
            shouldClick = true;
        }

        if (shouldClick && !(enableMissedClicks.isToggled() && chance(missedClickChance.getInput()))) {
            performClickWithDelay(plan.tempoState, hardLimitCps);
            if (plan.followUpDelay > 0) {
                scheduleFollowUpClick(plan.followUpDelay, plan.tempoState, hardLimitCps);
            }
        }

        clickStopWatch.start();
    }

    private void clearPendingSwing() {
        nextSwing = 0L;
        clickStopWatch.finish();
    }

    private void resetTimingState() {
        long seed = System.nanoTime() ^ (31L * System.identityHashCode(this)) ^ name.hashCode();
        sessionRandom.setSeed(seed);

        ticksDown = 0;
        nextSwing = 0L;
        clickingStartTime = 0L;
        lastActualClickTime = -1L;
        inSpike = false;
        spikeTicksRemaining = 0;
        burstClicksRemaining = 0;
        tempoState = TempoState.STEADY;
        tempoStateUntil = 0L;
        driftingCps = -1.0;
        driftVelocity = 0.0;
        driftRetargetAt = 0L;
        sessionCpsBias = uniform(0.95, 1.05);
        sessionDelayBias = uniform(0.95, 1.08);
        isClicking.set(false);
        clickStopWatch.finish();

        synchronized (timingLock) {
            emittedClickTimes.clear();
            recentIntervals.clear();
            recentBuckets.clear();
        }
    }

    private void resetStuckClickState() {
        if (!isClicking.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (clickingStartTime > 0 && now - clickingStartTime > MAX_CLICKING_TIMEOUT) {
            resetClickState();
        }
    }

    private void resetClickState() {
        isClicking.set(false);
        clickingStartTime = 0L;
    }

    private TimingPlan buildTimingPlan(boolean parentIsKillAura, boolean killAuraActive, double hardLimitCps) {
        long now = System.currentTimeMillis();
        updatePatternState();

        double[] cpsRange = getCpsRange(parentIsKillAura, killAuraActive);
        double minRange = Math.max(1.0, Math.min(cpsRange[0], hardLimitCps));
        double maxRange = Math.max(minRange, Math.min(cpsRange[1], hardLimitCps));

        WindowStats stats = computeWindowStats();
        double regularityPressure = computeRegularityPressure(stats);
        TempoState state = selectTempoState(now, regularityPressure);

        double targetCps = computeTargetCps(now, minRange, maxRange, hardLimitCps, state, regularityPressure);
        long delay = shapeDelay(Math.round(1000.0 / targetCps), state, stats, regularityPressure, hardLimitCps);

        if (enableNaturalPauses.isToggled() && chance(pauseChance.getInput())) {
            delay += samplePauseDuration(state);
        }

        long followUpDelay = -1L;
        boolean wantsBurstFollowUp = enableSpikePatterns.isToggled() && burstClicksRemaining > 0;
        boolean wantsButterflyFollowUp = butterFly.isToggled() && (state == TempoState.BURST || state == TempoState.PUSHING) && chance(10.0);
        if ((wantsBurstFollowUp || wantsButterflyFollowUp) && hardLimitCps > 6.0) {
            followUpDelay = buildFollowUpDelay(hardLimitCps, state);
            delay = Math.max(delay, Math.max(1L, followUpDelay / 2L));
            if (wantsBurstFollowUp && burstClicksRemaining > 0) {
                burstClicksRemaining--;
            }
        }

        if (inSpike) {
            spikeTicksRemaining--;
            if (spikeTicksRemaining <= 0) {
                inSpike = false;
            }
        }

        return new TimingPlan(Math.max(1L, delay), followUpDelay, state);
    }

    private void updatePatternState() {
        if (!enableSpikePatterns.isToggled()) {
            inSpike = false;
            spikeTicksRemaining = 0;
            burstClicksRemaining = 0;
            return;
        }

        if (!inSpike && chance(spikeFrequency.getInput())) {
            inSpike = true;
            spikeTicksRemaining = Math.max(1, (int) Math.round(uniform(1.0, spikeDuration.getInput())));
        }

        if (burstClicksRemaining <= 0 && chance(burstChance.getInput())) {
            burstClicksRemaining = Math.max(1, (int) Math.round(uniform(1.0, burstSize.getInput())));
        }
    }

    private double[] getCpsRange(boolean parentIsKillAura, boolean killAuraActive) {
        if ((parentIsKillAura || killAuraActive) && ModuleManager.killAura != null) {
            return ModuleManager.killAura.getCurrentCPSRange();
        }
        return new double[]{minCPS.getInput(), maxCPS.getInput()};
    }

    private double getHardLimitCps(boolean parentIsKillAura, boolean killAuraActive) {
        double hardLimit = cpsLimiter.getInput();
        if ((parentIsKillAura || killAuraActive) && ModuleManager.killAura != null) {
            hardLimit = Math.min(hardLimit, ModuleManager.killAura.maxCPS.getInput());
        }
        return Math.max(1.0, hardLimit);
    }

    private TempoState selectTempoState(long now, double regularityPressure) {
        if (now < tempoStateUntil) {
            return tempoState;
        }

        TempoState nextState;
        if (hasSpikeActivity()) {
            nextState = TempoState.BURST;
        } else if (regularityPressure > 1.15) {
            nextState = chance(55.0) ? TempoState.RECOVERY : TempoState.FATIGUE;
        } else {
            double roll = uniform(0.0, 1.0);
            if (roll < 0.34) {
                nextState = TempoState.STEADY;
            } else if (roll < 0.58) {
                nextState = TempoState.PUSHING;
            } else if (roll < 0.77) {
                nextState = TempoState.RECOVERY;
            } else if (roll < 0.9) {
                nextState = TempoState.FATIGUE;
            } else {
                nextState = TempoState.BURST;
            }
        }

        tempoState = nextState;
        double minDwell = nextState == TempoState.BURST ? 120.0 : 220.0;
        double maxDwell = nextState == TempoState.BURST ? 300.0 : 650.0;
        if (regularityPressure > 1.0 && (nextState == TempoState.RECOVERY || nextState == TempoState.FATIGUE)) {
            maxDwell += 180.0;
        }
        tempoStateUntil = now + (long) uniform(minDwell, maxDwell);
        return tempoState;
    }

    private double computeTargetCps(long now, double minRange, double maxRange, double hardLimitCps, TempoState state, double regularityPressure) {
        double mean = (minRange + maxRange) / 2.0;
        double spread = Math.max(0.12, (maxRange - minRange) / 5.5);
        double biasedMean = clamp(mean * sessionCpsBias, minRange, maxRange);

        if (driftingCps < 0.0 || now >= driftRetargetAt) {
            driftingCps = boundedGaussian(biasedMean, spread, minRange, maxRange);
            driftVelocity = boundedGaussian(0.0, 0.18 + (patternVariation.getInput() / 100.0) * 0.14, -0.45, 0.45);
            driftRetargetAt = now + (long) uniform(180.0, 520.0);
        } else {
            driftingCps = clamp(driftingCps + driftVelocity, minRange, maxRange);
            if (chance(8.0 + patternVariation.getInput() * 0.15)) {
                driftVelocity *= -uniform(0.65, 1.25);
            }
        }

        double targetCps = driftingCps;
        targetCps *= sampleTempoScale(state,
                1.08, 1.18,
                1.02, 1.10,
                0.72, 0.88,
                0.86, 0.98,
                0.98, 1.04);

        if (inSpike) {
            double spikeBoost = clamp(spikeIntensity.getInput(), 1.05, 1.6);
            targetCps *= boundedGaussian(spikeBoost, 0.09, 1.02, spikeBoost);
        }

        if (enableSpikePatterns.isToggled() && burstClicksRemaining > 0) {
            targetCps *= uniform(1.03, 1.1);
        }

        if (enableOverRandomization.isToggled()) {
            targetCps *= samplePercentScale(cpsRandomization.getInput() / 100.0, 0.18, 0.45, 0.65);
        }

        if (useSpikeRandomization()) {
            targetCps *= samplePercentScale(spikeCpsRandomization.getInput() / 100.0, 0.15, 0.4, 0.55);
        }

        if (regularityPressure > 0.7) {
            targetCps *= uniform(0.76, 0.92);
        }

        return clamp(targetCps, 1.0, hardLimitCps);
    }

    private long shapeDelay(long baseDelay, TempoState state, WindowStats stats, double regularityPressure, double hardLimitCps) {
        double multiplier = sessionDelayBias;
        multiplier *= sampleTempoScale(state,
                0.88, 0.97,
                0.94, 1.02,
                1.12, 1.26,
                1.02, 1.14,
                0.98, 1.05);

        if (enableChaoticTiming.isToggled()) {
            multiplier *= samplePercentScale(irregularityFactor.getInput() / 100.0, 0.15, 0.3, 0.45);
            multiplier *= samplePercentScale(jitterAmount.getInput() / 100.0, 0.12, 0.22, 0.32);
            multiplier *= samplePercentScale(patternVariation.getInput() / 100.0, 0.14, 0.28, 0.4);
        }

        if (enableOverRandomization.isToggled()) {
            multiplier *= samplePercentScale(delayRandomization.getInput() / 100.0, 0.18, 0.35, 0.55);
            multiplier *= samplePercentScale(timingVariation.getInput() / 100.0, 0.16, 0.3, 0.45);
        }

        if (useSpikeRandomization()) {
            multiplier *= samplePercentScale(spikeDelayRandomization.getInput() / 100.0, 0.15, 0.3, 0.45);
            multiplier *= samplePercentScale(spikeTimingVariation.getInput() / 100.0, 0.14, 0.28, 0.42);
        }

        long delay = Math.max(1L, Math.round(baseDelay * Math.max(0.45, multiplier)));
        delay = applyWindowCorrection(delay, stats, regularityPressure, hardLimitCps);

        if (chance(6.0 + patternVariation.getInput() * 0.08)) {
            delay += (long) uniform(6.0, 24.0);
        }

        return Math.max(getMinimumSpacingMs(hardLimitCps), delay);
    }

    private long applyWindowCorrection(long delay, WindowStats stats, double regularityPressure, double hardLimitCps) {
        if (stats.sampleCount < 6 || regularityPressure <= 0.0) {
            return Math.max(getMinimumSpacingMs(hardLimitCps), delay);
        }

        long correctedDelay = delay;
        if (stats.averageTicks < 1.45 && stats.stdDevTicks < 0.9) {
            correctedDelay += (long) uniform(45.0, 120.0 + regularityPressure * 35.0);
        }

        if (stats.dominantRatio > 0.55) {
            int targetBucket = stats.dominantBucket <= 1 ? 3 : 1;
            long bucketDelay = Math.max(1L, Math.round(targetBucket * 50.0 + uniform(-10.0, 18.0)));
            correctedDelay = Math.max(correctedDelay, bucketDelay);
        }

        if (stats.maxDuplicateStreak >= 3 && chance(20.0 + regularityPressure * 25.0)) {
            correctedDelay += (long) uniform(30.0, 80.0);
        }

        if (stats.entropy < 1.35) {
            correctedDelay += (long) uniform(18.0, 55.0);
        }

        return Math.max(getMinimumSpacingMs(hardLimitCps), correctedDelay);
    }

    private long samplePauseDuration(TempoState state) {
        double basePause = boundedGaussian(
                pauseDuration.getInput(),
                Math.max(2.0, pauseDuration.getInput() * 0.22),
                pauseDuration.getInput() * 0.5,
                pauseDuration.getInput() * 1.7
        );

        basePause *= sampleTempoScale(state,
                0.75, 1.0,
                1.0, 1.0,
                1.1, 1.4,
                0.95, 1.2,
                1.0, 1.0);

        return Math.max(20L, Math.round(basePause));
    }

    private long buildFollowUpDelay(double hardLimitCps, TempoState state) {
        long minSpacing = getMinimumSpacingMs(hardLimitCps);
        double stateScale = state == TempoState.BURST ? uniform(1.0, 1.2) : uniform(1.15, 1.45);
        long followUpDelay = Math.max(minSpacing, Math.round(minSpacing * stateScale));

        if (enableChaoticTiming.isToggled()) {
            double jitterPercent = jitterAmount.getInput() / 100.0;
            followUpDelay = Math.max(minSpacing, Math.round(followUpDelay * (1.0 + boundedGaussian(0.0, jitterPercent * 0.12, -jitterPercent * 0.2, jitterPercent * 0.28))));
        }

        return followUpDelay;
    }

    private void scheduleFollowUpClick(long delayMs, TempoState state, double hardLimitCps) {
        Raven.getExecutor().schedule(() -> {
            try {
                performClickWithDelay(state, hardLimitCps);
            } catch (Exception ignored) {
            }
        }, Math.max(1L, delayMs), TimeUnit.MILLISECONDS);
    }

    private void performClickWithDelay(TempoState state, double hardLimitCps) {
        if (!beginClickCycle()) {
            return;
        }

        final int button = leftClick ? 0 : 1;
        if (!enablePressReleaseDelay.isToggled()) {
            try {
                long clickTime = System.currentTimeMillis();
                if (!canEmitClick(clickTime, hardLimitCps)) {
                    return;
                }
                if (parent.click()) {
                    recordActualClick(clickTime);
                }
            } catch (Exception ignored) {
            } finally {
                resetClickState();
            }
            return;
        }

        final ClickShape clickShape = buildClickShape(state);
        Raven.getExecutor().schedule(() -> {
            try {
                long clickTime = System.currentTimeMillis();
                if (!canEmitClick(clickTime, hardLimitCps)) {
                    resetClickState();
                    return;
                }

                boolean clicked = parent.click();
                if (!clicked) {
                    resetClickState();
                    return;
                }

                recordActualClick(clickTime);
                Raven.getExecutor().schedule(() -> {
                    try {
                        Utils.sendClick(button, false);
                    } catch (Exception ignored) {
                    } finally {
                        resetClickState();
                    }
                }, clickShape.releaseScheduleDelay, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                resetClickState();
            }
        }, clickShape.pressDelay, TimeUnit.MILLISECONDS);
    }

    private ClickShape buildClickShape(TempoState state) {
        long pressDelay = sampleBoundedDelay(minPressDelay.getInput(), maxPressDelay.getInput());
        long holdTime = Math.max(1L, sampleBoundedDelay(minHoldTime.getInput(), maxHoldTime.getInput()));
        long releaseDelay = sampleBoundedDelay(minReleaseDelay.getInput(), maxReleaseDelay.getInput());

        double pressBias = 1.0;
        double holdBias = 1.0;
        double releaseBias = 1.0;
        switch (state) {
            case BURST:
                pressBias = 0.65;
                holdBias = 0.7;
                releaseBias = 0.75;
                break;
            case PUSHING:
                pressBias = 0.85;
                holdBias = 0.9;
                releaseBias = 0.92;
                break;
            case FATIGUE:
                pressBias = 1.18;
                holdBias = 1.12;
                releaseBias = 1.15;
                break;
            case RECOVERY:
                pressBias = 1.08;
                holdBias = 1.06;
                releaseBias = 1.08;
                break;
            case STEADY:
            default:
                break;
        }

        pressDelay = Math.max(0L, Math.round(pressDelay * pressBias * uniform(0.95, 1.08)));
        holdTime = Math.max(1L, Math.round(holdTime * holdBias * uniform(0.96, 1.08)));
        releaseDelay = Math.max(0L, Math.round(releaseDelay * releaseBias * uniform(0.95, 1.08)));

        if (enableChaoticTiming.isToggled()) {
            pressDelay = scaleRoundedLong(pressDelay, samplePercentScale(irregularityFactor.getInput() / 100.0, 0.12, 0.22, 0.32), 0L);
            holdTime = scaleRoundedLong(holdTime, samplePercentScale(jitterAmount.getInput() / 100.0, 0.1, 0.18, 0.28), 1L);
            releaseDelay = scaleRoundedLong(releaseDelay, samplePercentScale(patternVariation.getInput() / 100.0, 0.11, 0.2, 0.3), 0L);
        }

        if (enablePressReleaseOverRandomization.isToggled()) {
            double scale = samplePercentScale(pressReleaseCpsRandomization.getInput() / 100.0, 0.14, 0.22, 0.35);
            pressDelay = scaleRoundedLong(pressDelay, scale, 0L);
            holdTime = scaleRoundedLong(holdTime, scale, 1L);
            releaseDelay = scaleRoundedLong(releaseDelay, scale, 0L);

            pressDelay = scaleRoundedLong(pressDelay, samplePercentScale(pressReleaseDelayRandomization.getInput() / 100.0, 0.12, 0.22, 0.32), 0L);
            holdTime = scaleRoundedLong(holdTime, samplePercentScale(pressReleaseDelayRandomization.getInput() / 100.0, 0.1, 0.18, 0.28), 1L);
            releaseDelay = scaleRoundedLong(releaseDelay, samplePercentScale(pressReleaseTimingVariation.getInput() / 100.0, 0.11, 0.2, 0.3), 0L);
        }

        long releaseScheduleDelay = Math.max(1L, holdTime + releaseDelay);
        return new ClickShape(pressDelay, releaseScheduleDelay);
    }

    private long sampleBoundedDelay(double min, double max) {
        if (max <= min) {
            return Math.max(0L, Math.round(min));
        }

        double mean = (min + max) / 2.0;
        double stdDev = Math.max(0.2, (max - min) / 5.5);
        return Math.max(0L, Math.round(boundedGaussian(mean, stdDev, min, max)));
    }

    private boolean beginClickCycle() {
        resetStuckClickState();
        if (isClicking.get()) {
            return false;
        }
        isClicking.set(true);
        clickingStartTime = System.currentTimeMillis();
        return true;
    }

    private boolean canEmitClick(long now, double hardLimitCps) {
        synchronized (timingLock) {
            pruneOldClicks(now);
            long minSpacing = getMinimumSpacingMs(hardLimitCps);
            if (lastActualClickTime > 0 && now - lastActualClickTime < minSpacing) {
                return false;
            }

            int maxClicksPerSecond = Math.max(1, (int) Math.floor(hardLimitCps));
            return emittedClickTimes.size() < maxClicksPerSecond;
        }
    }

    private long getLimiterRecoveryDelay(long now, double hardLimitCps) {
        synchronized (timingLock) {
            pruneOldClicks(now);
            long minSpacing = getMinimumSpacingMs(hardLimitCps);
            long spacingWait = lastActualClickTime > 0 ? Math.max(0L, minSpacing - (now - lastActualClickTime)) : 0L;

            int maxClicksPerSecond = Math.max(1, (int) Math.floor(hardLimitCps));
            long windowWait = 0L;
            if (emittedClickTimes.size() >= maxClicksPerSecond && !emittedClickTimes.isEmpty()) {
                windowWait = Math.max(0L, 1000L - (now - emittedClickTimes.peekFirst()));
            }
            return Math.max(1L, Math.max(spacingWait, windowWait));
        }
    }

    private void recordActualClick(long now) {
        synchronized (timingLock) {
            pruneOldClicks(now);
            if (lastActualClickTime > 0L) {
                long interval = now - lastActualClickTime;
                recentIntervals.addLast(interval);
                recentBuckets.addLast(delayToBucket(interval));
                trimWindow(recentIntervals);
                trimWindow(recentBuckets);
            }
            emittedClickTimes.addLast(now);
            lastActualClickTime = now;
        }
    }

    private void pruneOldClicks(long now) {
        while (!emittedClickTimes.isEmpty() && now - emittedClickTimes.peekFirst() >= 1000L) {
            emittedClickTimes.pollFirst();
        }
    }

    private <T> void trimWindow(ArrayDeque<T> deque) {
        while (deque.size() > TIMING_WINDOW_SIZE) {
            deque.pollFirst();
        }
    }

    private WindowStats computeWindowStats() {
        synchronized (timingLock) {
            if (recentIntervals.isEmpty()) {
                return WindowStats.EMPTY;
            }

            Map<Integer, Integer> counts = new HashMap<>();
            double sumTicks = 0.0;
            double sumSquares = 0.0;
            int dominantBucket = 0;
            int dominantCount = 0;
            int maxDuplicateStreak = 1;
            int currentStreak = 0;
            int lastBucket = Integer.MIN_VALUE;
            int sampleCount = 0;

            java.util.Iterator<Long> intervalIterator = recentIntervals.iterator();
            java.util.Iterator<Integer> bucketIterator = recentBuckets.iterator();
            while (intervalIterator.hasNext() && bucketIterator.hasNext()) {
                long interval = intervalIterator.next();
                int bucket = bucketIterator.next();
                sampleCount++;

                double ticks = interval / 50.0;
                sumTicks += ticks;
                sumSquares += ticks * ticks;

                int count = counts.getOrDefault(bucket, 0) + 1;
                counts.put(bucket, count);
                if (count > dominantCount) {
                    dominantCount = count;
                    dominantBucket = bucket;
                }

                if (bucket == lastBucket) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                    lastBucket = bucket;
                }
                if (currentStreak > maxDuplicateStreak) {
                    maxDuplicateStreak = currentStreak;
                }
            }

            double averageTicks = sumTicks / sampleCount;
            double variance = Math.max(0.0, (sumSquares / sampleCount) - (averageTicks * averageTicks));
            double entropy = 0.0;
            for (Integer count : counts.values()) {
                double p = count / (double) sampleCount;
                entropy -= p * (Math.log(p) / Math.log(2));
            }

            return new WindowStats(
                    sampleCount,
                    averageTicks,
                    Math.sqrt(variance),
                    entropy,
                    counts.size(),
                    dominantBucket,
                    dominantCount / (double) sampleCount,
                    maxDuplicateStreak
            );
        }
    }

    private double computeRegularityPressure(WindowStats stats) {
        if (stats.sampleCount < 6) {
            return 0.0;
        }

        double pressure = 0.0;
        if (stats.averageTicks <= 1.45 && stats.stdDevTicks < 0.9) {
            pressure += (0.9 - stats.stdDevTicks) * 1.35;
        } else if (stats.stdDevTicks < 0.75) {
            pressure += (0.75 - stats.stdDevTicks);
        }

        if (stats.entropy < 1.45) {
            pressure += (1.45 - stats.entropy) * 0.7;
        }

        if (stats.uniqueBuckets <= 3) {
            pressure += (3 - stats.uniqueBuckets) * 0.35;
        }

        if (stats.dominantRatio > 0.55) {
            pressure += (stats.dominantRatio - 0.55) * 1.8;
        }

        if (stats.maxDuplicateStreak >= 3) {
            pressure += (stats.maxDuplicateStreak - 2) * 0.18;
        }

        return Math.min(2.0, pressure);
    }

    private int delayToBucket(long delayMs) {
        return Math.max(0, (int) Math.round(delayMs / 50.0));
    }

    private long getMinimumSpacingMs(double hardLimitCps) {
        return Math.max(1L, (long) Math.ceil(1000.0 / hardLimitCps));
    }

    private boolean hasSpikeActivity() {
        return enableSpikePatterns.isToggled() && (inSpike || burstClicksRemaining > 0);
    }

    private boolean useSpikeRandomization() {
        return hasSpikeActivity() && enableSpikeOverRandomization.isToggled();
    }

    private double sampleTempoScale(TempoState state,
                                    double burstMin, double burstMax,
                                    double pushingMin, double pushingMax,
                                    double fatigueMin, double fatigueMax,
                                    double recoveryMin, double recoveryMax,
                                    double steadyMin, double steadyMax) {
        switch (state) {
            case BURST:
                return uniform(burstMin, burstMax);
            case PUSHING:
                return uniform(pushingMin, pushingMax);
            case FATIGUE:
                return uniform(fatigueMin, fatigueMax);
            case RECOVERY:
                return uniform(recoveryMin, recoveryMax);
            case STEADY:
            default:
                return uniform(steadyMin, steadyMax);
        }
    }

    private double samplePercentScale(double percent, double stdFactor, double negativeFactor, double positiveFactor) {
        if (percent <= 0.0) {
            return 1.0;
        }
        return 1.0 + boundedGaussian(0.0, percent * stdFactor, -percent * negativeFactor, percent * positiveFactor);
    }

    private long scaleRoundedLong(long value, double factor, long minimum) {
        return Math.max(minimum, Math.round(value * factor));
    }

    private boolean chance(double percent) {
        return uniform(0.0, 100.0) < percent;
    }

    private double uniform(double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + sessionRandom.nextDouble() * (max - min);
    }

    private double boundedGaussian(double mean, double stdDev, double min, double max) {
        if (max <= min) {
            return min;
        }
        if (stdDev <= 0.0) {
            return clamp(mean, min, max);
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            double value = sessionRandom.nextGaussian() * stdDev + mean;
            if (value >= min && value <= max) {
                return value;
            }
        }
        return uniform(min, max);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum TempoState {
        STEADY,
        PUSHING,
        BURST,
        FATIGUE,
        RECOVERY
    }

    private static final class TimingPlan {
        private final long nextSwing;
        private final long followUpDelay;
        private final TempoState tempoState;

        private TimingPlan(long nextSwing, long followUpDelay, TempoState tempoState) {
            this.nextSwing = nextSwing;
            this.followUpDelay = followUpDelay;
            this.tempoState = tempoState;
        }
    }

    private static final class ClickShape {
        private final long pressDelay;
        private final long releaseScheduleDelay;

        private ClickShape(long pressDelay, long releaseScheduleDelay) {
            this.pressDelay = pressDelay;
            this.releaseScheduleDelay = releaseScheduleDelay;
        }
    }

    private static final class WindowStats {
        private static final WindowStats EMPTY = new WindowStats(0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0);

        private final int sampleCount;
        private final double averageTicks;
        private final double stdDevTicks;
        private final double entropy;
        private final int uniqueBuckets;
        private final int dominantBucket;
        private final double dominantRatio;
        private final int maxDuplicateStreak;

        private WindowStats(int sampleCount, double averageTicks, double stdDevTicks, double entropy,
                            int uniqueBuckets, int dominantBucket, double dominantRatio, int maxDuplicateStreak) {
            this.sampleCount = sampleCount;
            this.averageTicks = averageTicks;
            this.stdDevTicks = stdDevTicks;
            this.entropy = entropy;
            this.uniqueBuckets = uniqueBuckets;
            this.dominantBucket = dominantBucket;
            this.dominantRatio = dominantRatio;
            this.maxDuplicateStreak = maxDuplicateStreak;
        }
    }
}