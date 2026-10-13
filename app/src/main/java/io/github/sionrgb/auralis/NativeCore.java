package io.github.sionrgb.auralis;
public final class NativeCore {
    static { System.loadLibrary("auralis_bridge"); }
    public static native int sourceKind(String source);
    public static native float presetGain(int preset, int band);
    private NativeCore() {}
}
