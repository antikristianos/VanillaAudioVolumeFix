package antikristianos.vanillaaudiovolumefix;

import fmod.fmod.FMODManager;
import fmod.javafmod;
import zombie.SoundManager;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterMusicState;
import zombie.core.Core;
import zombie.network.GameServer;

public final class AudioVolumeRuntime {
    private static final NonBankVolume NON_BANK = new NonBankVolume();
    private static final NonBankVolume.Sink NATIVE_SINK = javafmod::FMOD_ChannelGroup_SetVolume;
    private static SoundManager owner;
    private static ParameterMusicState musicState;
    private static boolean nonBankActiveLogged;
    private static boolean failureLogged;
    private static int loggedMenuState = -1;

    private AudioVolumeRuntime() { }

    public static void update(SoundManager manager) {
        if (GameServer.server || Core.soundDisabled || manager == null) {
            return;
        }
        try {
            setOwner(manager);
            FMODManager fmod = FMODManager.instance;
            if (fmod == null) {
                return;
            }
            if (NON_BANK.update(fmod.channelGroupInGameNonBankSounds,
                    manager.getSoundVolume(), System.nanoTime(), NATIVE_SINK) && !nonBankActiveLogged) {
                nonBankActiveLogged = true;
                System.out.println("[VanillaAudioVolumeFix] ACTIVE 1.1: InGameNonBank follows SFX; gain="
                        + manager.getSoundVolume());
            }
        } catch (RuntimeException | LinkageError error) {
            reportFailure(error);
        }
    }

    public static float clipVolume(GameSoundClip clip, float original) {
        if (GameServer.server || Core.soundDisabled || clip == null || clip.gameSound == null) {
            return original;
        }
        String soundName = clip.gameSound.name;
        boolean transitionSound = VolumePolicy.isTransitionSound(soundName);
        if ((!transitionSound && !"MusicCombined".equals(soundName))
                || !(SoundManager.instance instanceof SoundManager)) {
            return original;
        }
        try {
            SoundManager manager = (SoundManager) SoundManager.instance;
            setOwner(manager);
            // UI effects need the current effects slider, but no initialized music parameter.
            if (transitionSound) {
                return original * VolumePolicy.effectGain(manager.getSoundVolume());
            }
            if (musicState == null) {
                FMODParameterList parameters = manager.getFMODParameters();
                if (parameters != null) {
                    for (int index = 0; index < parameters.parameterList.size(); index++) {
                        FMODParameter parameter = parameters.parameterList.get(index);
                        if (parameter instanceof ParameterMusicState) {
                            musicState = (ParameterMusicState) parameter;
                            break;
                        }
                    }
                }
            }
            if (musicState == null) {
                return original;
            }
            // Read current state, not the last FMOD update's cached parameter value.
            float state = musicState.calculateCurrentValue();
            int menuState = state == VolumePolicy.MAIN_MENU_STATE ? 1
                    : state == VolumePolicy.LOADING_STATE ? 2 : 0;
            float gain = VolumePolicy.musicGain(1.0f, soundName, state, manager.getMusicVolume());
            if (menuState != loggedMenuState) {
                loggedMenuState = menuState;
                System.out.println("[VanillaAudioVolumeFix] MusicCombined additional factor=" + gain
                        + (menuState == 1 ? " (MainMenu)"
                                : menuState == 2 ? " (Loading)" : " (outside MainMenu/Loading)"));
            }
            return original * gain;
        } catch (RuntimeException | LinkageError error) {
            reportFailure(error);
            return original;
        }
    }

    private static void setOwner(SoundManager manager) {
        if (owner != manager) {
            owner = manager;
            musicState = null;
            NON_BANK.reset();
            nonBankActiveLogged = false;
            loggedMenuState = -1;
        }
    }

    private static void reportFailure(Throwable error) {
        if (!failureLogged) {
            failureLogged = true;
            System.err.println("[VanillaAudioVolumeFix] Audio hook failed; "
                    + "check the test log: " + error);
        }
    }
}
