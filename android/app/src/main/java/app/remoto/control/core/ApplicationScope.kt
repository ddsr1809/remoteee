package app.remoto.control.core

import javax.inject.Qualifier

/** `CoroutineScope` que vive tanto como la app. Lo provee `di/AppModule`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
