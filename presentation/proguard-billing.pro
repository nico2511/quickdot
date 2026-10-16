# Play Billing, play release only. The billing AAR also ships consumer rules;
# these are repeated here so R8 still keeps the client if those are dropped.
-keep class com.android.vending.billing.** { *; }
-keep class com.google.android.apps.play.billingtestcompanion.aidl.** { *; }
-keep class com.android.billingclient.api.** { *; }
-keepnames class com.android.billingclient.api.ProxyBillingActivity
-keepnames class com.android.billingclient.api.ProxyBillingActivityV2
-keepclassmembers class * extends com.google.android.gms.internal.play_billing.zzgp {
    <fields>;
}
-dontwarn com.android.billingclient.**
-dontwarn com.google.android.gms.internal.play_billing.**
-dontwarn libcore.io.Memory
-dontwarn sun.misc.Unsafe
