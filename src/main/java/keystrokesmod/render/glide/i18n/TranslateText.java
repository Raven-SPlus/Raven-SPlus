package keystrokesmod.render.glide.i18n;

/**
 * Ported subset of Glide's TranslateText. Contains entries needed for
 * module categories, tab labels, setting labels, and common UI strings.
 * Add more entries from the original enum as needed.
 */
public enum TranslateText {

    // ---- Tab / navigation labels ----
    HOME("text.home"),
    MODULE("text.module"),
    COSMETICS("text.cosmetics"),
    MUSIC("text.music"),
    SCREENSHOT("text.screenshot"),
    PROFILE("text.profile"),
    SETTINGS("text.settings"),
    SCRIPT("text.script"),
    CHANGELOG("text.changelog"),
    NEWS("text.news"),
    SEARCH("text.search"),

    // ---- Module categories ----
    PLAYER("text.player"),
    RENDER("text.render"),
    WORLD("text.world"),
    OTHER("text.other"),
    HUD("text.hud"),

    // ---- Settings page ----
    APPEARANCE("text.appearance"),
    APPEARANCE_DESCRIPTION("text.appearance.description"),
    LANGUAGE("text.language"),
    LANGUAGE_DESCRIPTION("text.language.description"),
    KEYBINDS("text.keybinds"),
    KEYBINDS_DESCRIPTION("text.keybinds.description"),
    ACCENT_COLOR("text.accentcolor"),
    HUD_THEME("text.hudtheme"),
    THEME("text.theme"),
    GENERAL("text.general"),
    GENERAL_DESCRIPTION("text.general.description"),
    UI_BLUR("text.ui.blur"),
    MC_FONT("text.hud.mcfont"),

    // ---- Common setting labels ----
    MODE("text.mode"),
    STYLE("text.style"),
    TYPE("text.type"),
    DESIGN("text.design"),
    SCALE("text.scale"),
    SPEED("text.speed"),
    AMOUNT("text.amount"),
    INTENSITY("text.intensity"),
    ALPHA("text.alpha"),
    WIDTH("text.width"),
    HEIGHT("text.height"),
    RANGE("text.range"),
    DELAY("text.delay"),
    DURATION("text.duration"),
    FACTOR("text.factor"),
    MULTIPLIER("text.multiplier"),
    VOLUME("text.volume"),
    RADIUS("text.radius"),
    VALUE("text.value"),
    DISTANCE("text.distance"),
    COLOR("text.color"),

    // ---- Common values / modes ----
    NONE("text.none"),
    SIMPLE("text.simple"),
    FANCY("text.fancy"),
    COMPACT("text.compact"),
    NORMAL("text.normal"),
    VANILLA("text.vanilla"),
    BASIC("text.basic"),
    SMOOTH("text.smooth"),
    CUSTOM("text.custom"),
    GRADIENT("text.gradient"),
    GRADIENT_SIMPLE("text.gradientsimple"),
    MODERN("text.modern"),
    ALWAYS("text.always"),
    TOGGLE("text.toggle"),
    KEYDOWN("text.keydown"),
    SYNC("text.sync"),
    RAINBOW("text.rainbow"),
    VERTICAL("text.vertical"),
    HORIZONTAL("text.horizontal"),
    RIGHT("text.right"),
    LEFT("text.left"),
    FRONT("text.front"),
    BEHIND("text.behind"),

    // ---- Positions / axes ----
    X("text.x"),
    Y("text.y"),
    Z("text.z"),
    X_SCALE("text.xscale"),
    Y_SCALE("text.yscale"),
    Z_SCALE("text.zscale"),

    // ---- Color property labels ----
    HUE("text.hue"),
    SATURATION("text.saturation"),
    CONTRAST("text.contrast"),
    BRIGHTNESS("text.brightness"),
    OUTLINE("text.outline"),
    OUTLINE_WIDTH("text.outlinewidth"),
    OUTLINE_COLOR("text.outlinecolor"),
    OUTLINE_ALPHA("text.outlinealpha"),
    FILL("text.fill"),
    FILL_COLOR("text.fillcolor"),
    FILL_ALPHA("text.fillalpha"),
    CUSTOM_COLOR("text.customcolor"),
    BACKGROUND("text.background"),
    SHADOW("text.shadow"),
    GLOW("text.glow"),
    DEPTH("text.depth"),
    ANIMATION("text.animation"),
    ANIMATION_TYPE("text.animationtype"),
    ICON("text.icon"),
    IMAGE("text.image"),
    GRAPH("text.graph"),
    OVERLAY("text.overlay"),
    WAVEFORM("text.waveform"),
    HEAD("text.head"),

    // ---- Profile page ----
    ADD_PROFILE("text.addprofile"),
    AUTO_LOAD("text.autoload"),
    SERVER_IP("text.serverip"),
    CREATE("text.create"),

    // ---- Keybind ----
    KEYBIND("text.keybind"),

    // ---- General purpose strings ----
    NAME("text.name"),
    PREFIX("text.prefix"),
    IGN("text.ign"),
    FPS("text.fps"),
    FOV("text.fov"),
    TIME("text.time"),
    WEATHER("text.weather"),
    HEALTH("text.health"),
    MAX("text.max"),
    ALL("text.all"),
    FAVORITE("text.favorite"),
    ADD("text.add"),
    MESSAGE("text.message"),
    NUMBER("text.number"),
    TEXT("text.text"),
    SOUND("text.sound"),
    EFFECT("text.effect"),
    PARTICLE("text.particle"),
    SCROLL("text.scroll"),
    TAG("text.tag"),
    LOADING("text.loading"),
    ERROR("text.error"),
    ADDED("text.added"),
    FIXED("text.fixed"),
    REMOVED("text.removed"),
    SOON("text.soon"),
    PURCHASE("text.purchase"),
    PREMIUM("text.premium"),
    LIFETIME("text.lifetime"),
    MONTH("text.month"),
    LIGHT("text.light"),
    DARK("text.dark"),
    CLIENT("text.client"),

    // ---- Languages ----
    ENGLISH("text.english"),
    ENGLISH_US("text.englishus"),
    ENGLISH_GB("text.englishgb"),
    JAPANESE("text.japanese"),
    CHINESE("text.chinese"),
    POLISH("text.polish"),
    FRENCH("text.french"),

    // ---- Multiplayer / singleplayer ----
    SINGLEPLAYER("text.singleplayer"),
    MULTIPLAYER("text.multiplayer"),
    LOGIN_MESSAGE("text.loginmessage"),
    MICROSOFT_LOGIN("text.microsoftlogin"),
    OFFLINE_LOGIN("text.offlinelogin"),
    LOGIN("text.login"),

    // ---- Game menu ----
    EXIT_WORLD_SINGLEPLAYER("text.gamemenu.exitworldsingleplayer"),
    OPEN_MOD_MENU("text.gamemenu.openglidemenu"),
    EDIT_HUD("text.gamemenu.edithud"),

    // ---- Music ----
    NOTHING_IS_PLAYING("text.nothingisplaying"),
    ADD_SONG("text.addsong"),
    ADDED_MUSIC_QUEUE("text.addedmusicqueue"),
    MUSIC_DOWNLOAD_COMPLETE("text.musicdownloadcomplete"),
    MUSIC_DOWNLOAD_FAILED("text.musicdownloadfailed"),

    // ---- Misc module names (commonly referenced) ----
    FPS_DISPLAY("text.fpsdisplay.name"),
    FPS_DISPLAY_DESCRIPTION("text.fpsdisplay.description"),
    TOGGLE_SPRINT("text.togglesprint.name"),
    TOGGLE_SPRINT_DESCRIPTION("text.togglesprint.description"),
    TOGGLE_SNEAK("text.togglesneak.name"),
    TOGGLE_SNEAK_DESCRIPTION("text.togglesneak.description"),
    FULLBRIGHT("text.fullbright.name"),
    FULLBRIGHT_DESCRIPTION("text.fullbright.description"),
    MOTION_BLUR("text.motionblur.name"),
    MOTION_BLUR_DESCRIPTION("text.motionblur.description"),
    KEYSTROKES("text.keystrokes.name"),
    KEYSTROKES_DESCRIPTION("text.keystrokes.description"),
    ZOOM("text.zoom.name"),
    ZOOM_DESCRIPTION("text.zoom.description"),
    ZOOM_SPEED("text.zoomspeed"),
    ZOOM_FACTOR("text.zoomfactor"),
    SMOOTH_ZOOM("text.smoothzoom"),
    SMOOTH_CAMERA("text.smoothcamera"),
    MINIMAP("text.minimap.name"),
    MINIMAP_DESCRIPTION("text.minimap.description"),
    NAME_DISPLAY("text.namedisplay.name"),
    NAME_DISPLAY_DESCRIPTION("text.namedisplay.description"),
    COMBO_COUNTER("text.combocounter.name"),
    COMBO_COUNTER_DESCRIPTION("text.combocounter.description"),
    OLD_ANIMATION("text.oldanimations.name"),
    OLD_ANIMATION_DESCRIPTION("text.oldanimations.description"),
    BLOCK_HIT("text.blockhit"),
    TARGET_INFO("text.targetinfo.name"),
    TARGET_INFO_DESCRIPTION("text.targetinfo.description"),
    SCOREBOARD("text.scoreboard.name"),
    SCOREBOARD_DESCRIPTION("text.scoreboard.description"),
    ARRAY_LIST("text.arraylist.name"),
    ARRAY_LIST_DESCRIPTION("text.arraylist.description"),
    CHAT("text.chat.name"),
    CHAT_DESCRIPTION("text.chat.description"),
    CROSSHAIR("text.crosshair.name"),
    CROSSHAIR_DESCRIPTION("text.crosshair.description"),
    OVERLAY_EDITOR("text.overlayeditor.name"),
    OVERLAY_EDITOR_DESCRIPTION("text.overlayeditor.description"),

    // ---- Discord ----
    DISCORD_RPC("text.discordrpc.name"),
    DISCORD_RPC_DESCRIPTION("text.discordrpc.description"),
    JOIN_OUR_DISCORD_SERVER("text.joindiscordserver"),
    JOIN("text.join"),

    // ---- UI sounds ----
    UI_SOUNDS("text.ui.sounds"),
    UI_SOUNDS_DESCRIPTION("text.ui.sounds.description"),

    // ---- Welcome ----
    WELCOME_TO_SOAR("text.welcometosoar"),

    // ---- Premium ----
    PRICING_PLANS("text.pricingplans"),
    PRICING_PLANS_DESCRIPTION("text.pricingplans.description"),
    PREMIUM_ONLY("text.premiumonly"),
    SPECIAL_BADGE("text.specialbadge"),
    SPECIAL_CAPE("text.specialcape"),
    CUSTOM_CAPE("text.customcape"),

    // ---- Search complete / web ----
    SEARCH_COMPLETE("text.search_complete"),
    SEARCH_FAILED("text.search_failed"),
    REQUIRED_FILE_MISSING("text.requiredfilemissing"),

    // ---- Select background ----
    SELECT_BACKGROUND("text.selectbackground"),
    NIGHT("text.night");

    private String key;
    private String text;

    private TranslateText(String key) {
        this.key = key;
    }

    public String getText() {
        return text == null ? "null" : text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getKey() {
        return key;
    }
}
