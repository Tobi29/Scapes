# Make EnumMap work
-keep enum *
-keepclassmembers enum * {
    *;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

# Avoid some spam
-dontwarn java.lang.invoke.**
-dontwarn java.applet.**, java.awt.**
-dontwarn javax.imageio.**, javax.print.**, javax.sound.**, javax.swing.**, javax.xml.**
-dontwarn org.osgi.**
-dontwarn sun.util.calendar.**, sun.misc.**

-dontwarn java.util.function.Consumer, java.util.Spliterator, java.util.Collection
-dontwarn aQute.bnd.annotation.Version

-dontwarn java.net.URLClassLoader
-dontwarn java.util.concurrent.ForkJoinPool

-dontnote android.net.http.SslCertificate
-dontnote android.net.http.SslError
-dontnote android.net.http.SslCertificate$DName
-dontnote org.apache.http.conn.scheme.SocketFactory
-dontnote org.apache.http.conn.scheme.HostNameResolver
-dontnote org.apache.http.conn.ConnectTimeoutException
-dontnote org.apache.http.params.HttpParams


# Kotlin
-keep class kotlin.jvm.internal.DefaultConstructorMarker
-keepattributes Signature
-dontwarn kotlin.**

# Antlr
-keep class org.antlr.v4.runtime.atn.ATNConfigSet
-dontnote org.antlr.v4.codegen.OutputModelWalker
-dontnote org.antlr.v4.runtime.misc.TestRig
-dontwarn org.antlr.stringtemplate.StringTemplate
-dontwarn org.antlr.v4.gui.TreeViewer, org.antlr.v4.gui.TreeViewer$TreeNodeWrapper
-dontwarn org.stringtemplate.v4.gui.STViewFrame
-dontwarn org.antlr.v4.gui.JFileChooserConfirmOverwrite

# ThreeTen
-keep class org.threeten.bp.Duration
-keep class org.threeten.bp.LocalDate
-keep class org.threeten.bp.LocalDateTime
-keep class org.threeten.bp.ZoneOffset
-keep class org.threeten.bp.format.DateTimeParseContext
-keep class org.threeten.bp.format.DateTimePrintContext
-keep class org.threeten.bp.temporal.IsoFields$1
-keep class org.threeten.bp.temporal.Temporal
-keep class org.threeten.bp.temporal.TemporalAccessor
-keep class org.threeten.bp.temporal.TemporalField
-keep class org.threeten.bp.temporal.TemporalQuery
-keep class org.threeten.bp.temporal.TemporalUnit
-keep class org.threeten.bp.temporal.ValueRange
-keep class org.threeten.bp.zone.ZoneRulesProvider
-keep class org.threeten.bp.zone.TzdbZoneRulesProvider

# Tika
-keep class org.apache.tika.mime.MimeTypes
-keep class org.apache.tika.mime.MimeTypesReader
-dontnote org.apache.tika.utils.CharsetUtils

# JLayer
-dontwarn javazoom.jl.player.PlayerApplet
-keep class javazoom.jl.decoder.JavaLayerUtils

# Java8
-dontnote java8.util.**
-dontwarn build.IgnoreJava8API

# ALAN
-keep class paulscode.android.sound.ALAN

# Scapes
-dontwarn org.tobi29.scapes.vanilla.basics.material.block.rock.BlockStoneRock$createModels$1$1$2
-dontwarn org.tobi29.scapes.vanilla.basics.material.block.vegetation.BlockCrop$createModels$1$1$2
-dontwarn java.util.concurrent.ConcurrentHashMap$KeySetView
-dontwarn org.tobi29.scapes.server.extension.base.DebugCommandsExtension$init$4$1$1

# Keep filesystem access
-keep class org.tobi29.scapes.engine.utils.io.filesystem.spi.FileSystemProvider
-keep class * implements org.tobi29.scapes.engine.utils.io.filesystem.spi.FileSystemProvider

# Keep profiler dispatcher
-keep class org.tobi29.scapes.engine.utils.profiler.spi.ProfilerDispatcherProvider
-keep class * implements org.tobi29.scapes.engine.utils.profiler.spi.ProfilerDispatcherProvider

# Keep codecs
-keep class org.tobi29.scapes.engine.utils.codec.spi.ReadableAudioStreamProvider
-keep class * implements org.tobi29.scapes.engine.utils.codec.spi.ReadableAudioStreamProvider

# Keep codecs
-keep class org.tobi29.scapes.engine.server.spi.SSLProviderProvider
-keep class * implements org.tobi29.scapes.engine.server.spi.SSLProviderProvider

# Keep plugins
-keep class org.tobi29.scapes.plugins.Plugin
-keep class * implements org.tobi29.scapes.plugins.Plugin

# Keep extensions
-keep class org.tobi29.scapes.server.extension.spi.ServerExtensionProvider
-keep class * implements org.tobi29.scapes.server.extension.spi.ServerExtensionProvider
