package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SymfonyProfilerDatabaseStackFrame {
    private final @Nullable String myFile;
    private final @Nullable Integer myLine;
    private final @Nullable String myClassName;
    private final @Nullable String myFunction;
    private final @Nullable String myCallType;

    public SymfonyProfilerDatabaseStackFrame(@Nullable String file, @Nullable Integer line, @Nullable String className, @Nullable String function, @Nullable String callType) {
        myFile = file;
        myLine = line;
        myClassName = className;
        myFunction = function;
        myCallType = callType;
    }

    public @Nullable String getFile() {
        return myFile;
    }

    public @Nullable Integer getLine() {
        return myLine;
    }

    public @Nullable String getClassName() {
        return myClassName;
    }

    public @Nullable String getFunction() {
        return myFunction;
    }

    public @Nullable String getCallType() {
        return myCallType;
    }

    public @Nullable String component1() {
        return myFile;
    }

    public @Nullable Integer component2() {
        return myLine;
    }

    public @Nullable String component3() {
        return myClassName;
    }

    public @Nullable String component4() {
        return myFunction;
    }

    public @Nullable String component5() {
        return myCallType;
    }

    public SymfonyProfilerDatabaseStackFrame copy(@Nullable String file, @Nullable Integer line, @Nullable String className, @Nullable String function, @Nullable String callType) {
        return new SymfonyProfilerDatabaseStackFrame(file, line, className, function, callType);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SymfonyProfilerDatabaseStackFrame that)) {
            return false;
        }
        return Objects.equals(myFile, that.myFile)
            && Objects.equals(myLine, that.myLine)
            && Objects.equals(myClassName, that.myClassName)
            && Objects.equals(myFunction, that.myFunction)
            && Objects.equals(myCallType, that.myCallType);
    }

    @Override
    public int hashCode() {
        int result = (myFile == null ? 0 : myFile.hashCode());
        result = 31 * result + (myLine == null ? 0 : myLine.hashCode());
        result = 31 * result + (myClassName == null ? 0 : myClassName.hashCode());
        result = 31 * result + (myFunction == null ? 0 : myFunction.hashCode());
        result = 31 * result + (myCallType == null ? 0 : myCallType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "SymfonyProfilerDatabaseStackFrame(file=" + myFile + ", line=" + myLine + ", className=" + myClassName + ", function=" + myFunction + ", callType=" + myCallType + ")";
    }
}
