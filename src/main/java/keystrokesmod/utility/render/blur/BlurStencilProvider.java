package keystrokesmod.utility.render.blur;

/**
 * Duck-typed interface implemented by the GuiButton mixin so GuiScreen can
 * batch all button blur stencils into a single blur pass.
 */
public interface BlurStencilProvider {
    /**
     * Writes this button's blur area into the stencil buffer.
     * Color writes are expected to be disabled by the caller.
     */
    void ravenAPlus$writeButtonBlurStencil(int mouseX, int mouseY, boolean shouldBlurSettingEnabled);
}

