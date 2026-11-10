# VisionAndroid — Vision

Prototipo Android nativo para un asistente personal **offline-first**.

## Estado actual

Esta primera versión crea la base compilable de la aplicación: interfaz de chat, configuración de endpoint remoto y una capa de motor separada de la interfaz. El modo local muestra claramente que necesita un modelo descargado; no finge ejecutar un modelo si todavía no está instalado.

## Dispositivo objetivo

- Samsung Galaxy S23 FE
- Exynos 2200
- 6 GB de RAM
- Android 14 o superior

## Arquitectura

- **UI:** Kotlin + Jetpack Compose + Material 3.
- **App:** Activity única y estado de conversación.
- **Motor:** interfaz `AssistantEngine`, preparada para motor local y remoto.
- **Privacidad:** el modo local no requiere red. La app solo usa Internet al configurar un endpoint remoto.
- **Modelo local:** objetivo inicial recomendado: un modelo instruct pequeño cuantizado (aprox. 1–2B, GGUF Q4), sujeto a pruebas de memoria y velocidad en el dispositivo.

## Abrir y compilar

Abrí el repositorio con Android Studio y ejecutá la configuración Gradle. Para compilar desde terminal:

```bash
./gradlew assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Próximos pasos

1. Probar la compilación en el S23 FE.
2. Integrar un runtime local GGUF (JNI/llama.cpp) y descarga explícita del modelo.
3. Añadir persistencia cifrada del historial.
4. Conectar un backend remoto opcional con autenticación y límites.

> Este repositorio es un prototipo inicial, no un asistente de IA terminado. No incluyas claves API en el código ni en Git.
