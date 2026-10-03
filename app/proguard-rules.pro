# ML Kit (the QR scanner) finds its components by reading class names from the manifest and
# instantiating them by reflection through their no-argument constructors. R8 sees no caller,
# strips the constructors, and the scanner fails at runtime ("Invalid component registrar").
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
