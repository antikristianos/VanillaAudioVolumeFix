package antikristianos.vanillaaudiovolumefix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.SoundManager;

@Patch(className = "zombie.SoundManager", methodName = "Update", warmUp = true, strictMatch = true)
public final class PatchSoundManagerUpdate {
    private PatchSoundManagerUpdate() { }

    @Patch.OnEnter
    public static void enter(@Patch.This SoundManager manager) {
        AudioVolumeRuntime.update(manager);
    }
}
