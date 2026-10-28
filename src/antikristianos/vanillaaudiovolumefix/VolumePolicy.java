package antikristianos.vanillaaudiovolumefix;

public final class VolumePolicy {
    // ParameterMusicState.State.MainMenu label in the supported game; checked by the build.
    public static final float MAIN_MENU_STATE = 0.0f;
    public static final float LOADING_STATE = 1.0f;

    private VolumePolicy() { }

    public static float effectGain(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(1.0f, value)) : 1.0f;
    }

    public static boolean isTransitionSound(String soundName) {
        return "UIActivatePlayButton".equals(soundName) || "UIClickToStart".equals(soundName);
    }

    public static float musicGain(float original, String soundName, float state, float musicVolume) {
        return "MusicCombined".equals(soundName) && (state == MAIN_MENU_STATE || state == LOADING_STATE)
                ? original * effectGain(musicVolume) : original;
    }
}
