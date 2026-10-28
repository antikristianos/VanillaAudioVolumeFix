package antikristianos.vanillaaudiovolumefix;

/** Tracks only successful native writes; unchanged frames do not call FMOD. */
public final class NonBankVolume {
    @FunctionalInterface
    public interface Sink {
        int setVolume(long group, float gain);
    }

    private long appliedGroup;
    private float appliedGain = Float.NaN;
    private long retryAfter;
    private boolean waitingToRetry;
    private static final long RETRY_NANOS = 2_000_000_000L;

    public void reset() {
        appliedGroup = 0L;
        appliedGain = Float.NaN;
        waitingToRetry = false;
    }

    public boolean update(long group, float gain, long now, Sink sink) {
        if (group == 0L) {
            reset();
            return false;
        }
        gain = VolumePolicy.effectGain(gain);
        if (group == appliedGroup && Float.compare(gain, appliedGain) == 0) {
            return false;
        }
        if (waitingToRetry && now - retryAfter < 0L) {
            return false;
        }
        waitingToRetry = false;
        // Also rate-limit exceptions, while allowing callers to report them.
        retryAfter = now + RETRY_NANOS;
        waitingToRetry = true;
        int result = sink.setVolume(group, gain);
        if (result != 0) {
            throw new IllegalStateException("FMOD ChannelGroup_SetVolume returned " + result);
        }
        waitingToRetry = false;
        appliedGroup = group;
        appliedGain = gain;
        return true;
    }
}
