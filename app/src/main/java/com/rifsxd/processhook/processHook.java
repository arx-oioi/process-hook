package com.rifsxd.processhook;

import android.annotation.SuppressLint;
import android.view.Display;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;

import javax.microedition.khronos.opengles.GL10;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

@SuppressLint("DiscouragedPrivateApi")
public class processHook implements IXposedHookLoadPackage {

    private final String TAG = processHook.class.getSimpleName();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String packageName = lpparam.packageName;
        deviceInfo properties = deviceProperties.DEVICE_MAP.get(packageName);

        if (properties != null) {
            spoofDeviceProperties(properties);
            spoofRefreshRate(properties);
            XposedBridge.log("Spoofed " + packageName + " as " + properties.device);
        }

        hookIMEI(lpparam);
        hookSerial(lpparam);
        hookDRM(lpparam);
        hookGAID(lpparam);
        hookAndroidId(lpparam);
        hookOpenGL(lpparam);
    }

    private void spoofDeviceProperties(deviceInfo properties) {
        setPropValue("MANUFACTURER", properties.manufacturer);
        setPropValue("BRAND", properties.brand);
        setPropValue("PRODUCT", properties.product);
        setPropValue("DEVICE", properties.device);
        setPropValue("MODEL", properties.model);
        setPropValue("HARDWARE", properties.hardware);
        setPropValue("BOARD", properties.board);
        setPropValue("BOOTLOADER", properties.bootloader);
        setPropValue("USER", properties.username);
        setPropValue("HOST", properties.hostname);
        setPropValue("FINGERPRINT", properties.fingerprint);
    }

    private void spoofRefreshRate(deviceInfo properties) {
        if (properties.refreshrate != null) {
            try {
                float spoofedRefreshRate = Float.parseFloat(properties.refreshrate);
                XposedBridge.hookAllMethods(Display.class, "getRefreshRate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(spoofedRefreshRate);
                            XposedBridge.log("Spoofed refresh rate to " + spoofedRefreshRate + " Hz");
                        }
                    });
            } catch (NumberFormatException e) {
                XposedBridge.log("Invalid refresh rate value: " + properties.refreshrate);
            }
        }
    }

    private void setPropValue(String key, Object value) {
        if (value != null) {
            try {
                Log.d(TAG, "Defining prop " + key + " to " + value);
                Field field = Build.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(null, value);
                field.setAccessible(false);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                XposedBridge.log("Failed to set prop: " + key + "\n" + Log.getStackTraceString(e));
            }
        }
    }

    private void hookIMEI(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephony = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader);
            final String fakeImei1 = "067530912345678";
            final String fakeImei2 = "077530912345679";

            XC_MethodHook hook = new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length == 1 && param.args[0] instanceof Integer) {
                        int slot = (int) param.args[0];
                        if (slot == 0) {
                            param.setResult(fakeImei1);
                        } else if (slot == 1) {
                            param.setResult(fakeImei2);
                        }
                    } else {
                        param.setResult(fakeImei1); // Default
                    }
                }
            };

            XposedBridge.hookAllMethods(telephony, "getDeviceId", hook);
            XposedBridge.hookAllMethods(telephony, "getImei", hook);

        } catch (Throwable t) {
            XposedBridge.log("IMEI Hook fail: " + t.getMessage());
        }
    }
    

    private void hookSerial(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Field serialField = Build.class.getDeclaredField("SERIAL");
            serialField.setAccessible(true);
            serialField.set(null, "38bf25a445ab2f8e");
        } catch (Throwable t) {
            XposedBridge.log("Serial Hook fail: " + t.getMessage());
        }
    }

    private void hookDRM(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> mediaDrm = XposedHelpers.findClass("android.media.MediaDrm", lpparam.classLoader);

            XposedBridge.hookAllMethods(mediaDrm, "getPropertyByteArray", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ("deviceUniqueId".equals(param.args[0])) {
                            param.setResult("BTHjXRK4w0phs/M2bKISGA==".getBytes());
                        }
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("DRM Hook fail: " + t.getMessage());
        }
    }

    private void hookGAID(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> infoClass = XposedHelpers.findClass("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info", lpparam.classLoader);

            XposedBridge.hookAllMethods(infoClass, "getId", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult("29f82bf3-8f62-403d-9a25-56044dcd8273");
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("GAID Hook fail: " + t.getMessage());
        }
    }

    private void hookAndroidId(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> settingsSecure = XposedHelpers.findClass("android.provider.Settings$Secure", lpparam.classLoader);
            XposedBridge.hookAllMethods(settingsSecure, "getString", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ("android_id".equals(param.args[1])) {
                            param.setResult("317ffdda75b8970d");
                        }
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("Android ID Hook fail: " + t.getMessage());
        }
    }

    private void hookOpenGL(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gl10 = XposedHelpers.findClass("android.opengl.GLES20", lpparam.classLoader);
            final String fakeVersion = "OpenGL ES 3.2 v1.r32p1-01eac0.b99cf1793b111173c0bb23abcfef1974de4";
            final String fakeVendor = "ARM";
            final String fakeRenderer = "Mali-G68 ARX";

            XposedBridge.hookAllMethods(gl10, "glGetString", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        int name = (Integer) param.args[0];
                        switch (name) {
                            case GL10.GL_VERSION:
                                param.setResult(fakeVersion);
                                break;
                            case GL10.GL_VENDOR:
                                param.setResult(fakeVendor);
                                break;
                            case GL10.GL_RENDERER:
                                param.setResult(fakeRenderer);
                                break;
                            default:
                                break;
                        }
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("OpenGL Hook fail: " + t.getMessage());
        }
    }
}
