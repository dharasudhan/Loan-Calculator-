import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers

fun main() {
    val clazz = RobolectricDeviceQualifiers::class.java
    clazz.declaredFields.forEach { println(it.name) }
}
