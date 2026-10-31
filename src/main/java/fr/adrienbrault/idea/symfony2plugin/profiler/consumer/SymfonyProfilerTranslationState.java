package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import org.jspecify.annotations.Nullable;

public enum SymfonyProfilerTranslationState {
    DEFINED(0, "defined", 2),
    MISSING(1, "missing", 0),
    FALLBACK(2, "fallback", 1);

    public static final Companion Companion = new Companion();

    private final int myCode;
    private final String myCsvValue;
    private final int myDisplayOrder;

    SymfonyProfilerTranslationState(int code, String csvValue, int displayOrder) {
        myCode = code;
        myCsvValue = csvValue;
        myDisplayOrder = displayOrder;
    }

    public int getCode() {
        return myCode;
    }

    public String getCsvValue() {
        return myCsvValue;
    }

    public int getDisplayOrder() {
        return myDisplayOrder;
    }

    public static final class Companion {
        private Companion() {
        }

        public SymfonyProfilerTranslationState from(@Nullable ProfilerValue value) {
            if (value instanceof ProfilerInteger integer) {
                for (SymfonyProfilerTranslationState state : values()) {
                    if ((long) state.getCode() == integer.getValue()) {
                        return state;
                    }
                }
                return DEFINED;
            }
            if (value instanceof ProfilerString string) {
                String text = string.utf8StringOrNull();
                if (text != null) {
                    for (SymfonyProfilerTranslationState state : values()) {
                        if (state.getCsvValue().equalsIgnoreCase(text)) {
                            return state;
                        }
                    }
                }
                return DEFINED;
            }
            return DEFINED;
        }
    }
}
