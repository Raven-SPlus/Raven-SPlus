package keystrokesmod.utility.render;

/**
 * Implemented via mixin on {@code TextureAtlasSprite} to track whether a sprite was ever used.
 * Used to skip ticking animations for never-referenced sprites (helps large atlases).
 */
public interface SpriteUsageAccess {
    boolean raven$isUsed();
    void raven$markUsed();
}

