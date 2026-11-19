package app.remoto.control.core.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro de diagnóstico en memoria, para que el usuario lo copie y nos lo mande cuando
 * algo no funciona. **Nunca sale del teléfono por sí solo** y no se guarda en disco.
 *
 * Todo lo que entra pasa por [LogRedactor]: quien llama no tiene que acordarse de ocultar IP
 * ni identificadores. Aun así, no registres nombres de teles ni nada que escriba el usuario.
 */
@Singleton
class DiagnosticsLog(
    private val capacity: Int,
    private val clock: () -> Long,
) {
    @Inject
    constructor() : this(DEFAULT_CAPACITY, System::currentTimeMillis)

    data class Entry(val timeMillis: Long, val tag: String, val message: String)

    private val entries = ArrayDeque<Entry>()

    fun log(tag: String, message: String) {
        val entry = Entry(clock(), tag, LogRedactor.redact(message))
        synchronized(entries) {
            entries.addLast(entry)
            while (entries.size > capacity) entries.removeFirst()
        }
    }

    /** Registra una excepción por su tipo y mensaje, sin la pila (puede ser muy larga). */
    fun log(tag: String, message: String, error: Throwable) {
        log(tag, "$message: ${error::class.java.simpleName}: ${error.message.orEmpty()}")
    }

    fun snapshot(): List<Entry> = synchronized(entries) { entries.toList() }

    fun clear() = synchronized(entries) { entries.clear() }

    /**
     * Texto listo para copiar. [header] son líneas de contexto que pone la interfaz
     * (versión de la app y de Android); también se pasan por el redactor.
     */
    fun export(header: List<String> = emptyList()): String {
        val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)
        return buildString {
            header.forEach { appendLine(LogRedactor.redact(it)) }
            if (header.isNotEmpty()) appendLine("---")
            snapshot().forEach { e -> appendLine("${format.format(Date(e.timeMillis))} [${e.tag}] ${e.message}") }
        }.trimEnd()
    }

    companion object {
        const val DEFAULT_CAPACITY = 300
    }
}
