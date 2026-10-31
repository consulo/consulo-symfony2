package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SymfonyProfilerLog {
    private final SymfonyProfilerLogSection mySection;
    private final @Nullable String myTimestamp;
    private final @Nullable Long myTimestampEpochMillis;
    private final int myPriority;
    private final @Nullable String myChannel;
    private final String myMessage;
    private final long myOccurrences;

    public SymfonyProfilerLog(SymfonyProfilerLogSection section, @Nullable String timestamp, @Nullable Long timestampEpochMillis, int priority, @Nullable String channel, String message, long occurrences) {
        mySection = section;
        myTimestamp = timestamp;
        myTimestampEpochMillis = timestampEpochMillis;
        myPriority = priority;
        myChannel = channel;
        myMessage = message;
        myOccurrences = occurrences;
    }

    public SymfonyProfilerLogSection getSection() {
        return mySection;
    }

    public @Nullable String getTimestamp() {
        return myTimestamp;
    }

    public @Nullable Long getTimestampEpochMillis() {
        return myTimestampEpochMillis;
    }

    public int getPriority() {
        return myPriority;
    }

    public @Nullable String getChannel() {
        return myChannel;
    }

    public String getMessage() {
        return myMessage;
    }

    public long getOccurrences() {
        return myOccurrences;
    }

    public SymfonyProfilerLogSection component1() {
        return mySection;
    }

    public @Nullable String component2() {
        return myTimestamp;
    }

    public @Nullable Long component3() {
        return myTimestampEpochMillis;
    }

    public int component4() {
        return myPriority;
    }

    public @Nullable String component5() {
        return myChannel;
    }

    public String component6() {
        return myMessage;
    }

    public long component7() {
        return myOccurrences;
    }

    public SymfonyProfilerLog copy(SymfonyProfilerLogSection section, @Nullable String timestamp, @Nullable Long timestampEpochMillis, int priority, @Nullable String channel, String message, long occurrences) {
        return new SymfonyProfilerLog(section, timestamp, timestampEpochMillis, priority, channel, message, occurrences);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerLog that)) {
            return false;
        }
        return mySection.equals(that.mySection)
            && Objects.equals(myTimestamp, that.myTimestamp)
            && Objects.equals(myTimestampEpochMillis, that.myTimestampEpochMillis)
            && myPriority == that.myPriority
            && Objects.equals(myChannel, that.myChannel)
            && myMessage.equals(that.myMessage)
            && myOccurrences == that.myOccurrences;
    }

    @Override
    public int hashCode() {
        int result = mySection.hashCode();
        result = 31 * result + (myTimestamp == null ? 0 : myTimestamp.hashCode());
        result = 31 * result + (myTimestampEpochMillis == null ? 0 : myTimestampEpochMillis.hashCode());
        result = 31 * result + Integer.hashCode(myPriority);
        result = 31 * result + (myChannel == null ? 0 : myChannel.hashCode());
        result = 31 * result + myMessage.hashCode();
        result = 31 * result + Long.hashCode(myOccurrences);
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerLog(section=" + mySection + ", timestamp=" + myTimestamp + ", timestampEpochMillis=" + myTimestampEpochMillis + ", priority=" + myPriority + ", channel=" + myChannel + ", message=" + myMessage + ", occurrences=" + myOccurrences + ")";
    }
}
