# Proguard rules for Kodyar24
-keep class com.example.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
