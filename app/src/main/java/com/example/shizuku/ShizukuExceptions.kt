package com.example.shizuku

/**
 * Base exception for errors occurring while communicating with the Shizuku server service.
 */
sealed class ShizukuCommunicationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class ShizukuNotRunningException(
    message: String = "Shizuku service is not running or responding. Please ensure the Shizuku app is active or enable Test Mode."
) : ShizukuCommunicationException(message)

class ShizukuPermissionDeniedException(
    message: String = "Shizuku authorization has not been granted. GFX Pro Booster needs elevated permission to write configuration."
) : ShizukuCommunicationException(message)

class ShizukuCommandExecutionException(
    val command: String,
    val exitCode: Int,
    val errorOutput: String,
    message: String = "Failed executing command via Shizuku: $command (Exit code: $exitCode). Output: $errorOutput"
) : ShizukuCommunicationException(message)

class ShizukuFileAccessException(
    val path: String,
    message: String,
    cause: Throwable? = null
) : ShizukuCommunicationException("Shizuku file access failure on $path: $message", cause)
