package antikristianos.vanillaaudiovolumefix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.audio.GameSoundClip;

@Patch(className = "zombie.audio.GameSoundClip", methodName = "getEffectiveVolume",
        warmUp = true, strictMatch = true)
public final class PatchMusicClipVolume {
    private PatchMusicClipVolume() { }

    @Patch.OnExit
    public static void exit(@Patch.This GameSoundClip clip, @Patch.Return(readOnly = false) float volume) {
        volume = AudioVolumeRuntime.clipVolume(clip, volume);
    }
}
