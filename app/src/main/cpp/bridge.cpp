#include <jni.h>
#include <cstddef>
extern "C" int auralis_source_kind(const unsigned char*, size_t);
extern "C" float auralis_preset_gain(int, int);
extern "C" JNIEXPORT jint JNICALL Java_io_github_sionrgb_auralis_NativeCore_sourceKind(JNIEnv* env, jclass, jstring text) {
    if (!text) return -1;
    const char* bytes = env->GetStringUTFChars(text, nullptr);
    if (!bytes) return -1;
    int kind = auralis_source_kind(reinterpret_cast<const unsigned char*>(bytes), env->GetStringUTFLength(text));
    env->ReleaseStringUTFChars(text, bytes);
    return kind;
}
extern "C" JNIEXPORT jfloat JNICALL Java_io_github_sionrgb_auralis_NativeCore_presetGain(JNIEnv*, jclass, jint preset, jint band) { return auralis_preset_gain(preset, band); }
