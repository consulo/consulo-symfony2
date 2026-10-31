package fr.adrienbrault.idea.symfony2plugin.phpUnserializer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class PhpEnum implements PhpValue {
    private final PhpBytes myEnumName;
    private final PhpBytes myCaseName;

    public PhpEnum(PhpBytes enumName, PhpBytes caseName) {
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

    public PhpEnum copy(PhpBytes enumName, PhpBytes caseName) {
        return new PhpEnum(enumName, caseName);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhpEnum that)) {
            return false;
        }
        return Objects.equals(myEnumName, that.myEnumName) && Objects.equals(myCaseName, that.myCaseName);
    }

    @Override
    public int hashCode() {
        int result = myEnumName.hashCode();
        result = result * 31 + myCaseName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhpEnum(enumName=" + myEnumName + ", caseName=" + myCaseName + ")";
    }
}
