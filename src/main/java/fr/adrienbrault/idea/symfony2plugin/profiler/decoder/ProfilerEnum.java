package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes;
import org.jspecify.annotations.Nullable;

public final class ProfilerEnum implements ProfilerValue {
    private final PhpBytes myEnumName;
    private final PhpBytes myCaseName;

    public ProfilerEnum(PhpBytes enumName, PhpBytes caseName) {
        myEnumName = enumName;
        myCaseName = caseName;
    }

    public PhpBytes getEnumName() {
        return myEnumName;
    }

    public PhpBytes getCaseName() {
        return myCaseName;
    }

    public PhpBytes component1() {
        return myEnumName;
    }

    public PhpBytes component2() {
        return myCaseName;
    }

    public ProfilerEnum copy(PhpBytes enumName, PhpBytes caseName) {
        return new ProfilerEnum(enumName, caseName);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfilerEnum that)) {
            return false;
        }
        return myEnumName.equals(that.myEnumName)
            && myCaseName.equals(that.myCaseName);
    }

    @Override
    public int hashCode() {
        int result = myEnumName.hashCode();
        result = 31 * result + myCaseName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ProfilerEnum(enumName=" + myEnumName + ", caseName=" + myCaseName + ")";
    }
}
