<div align="center">

# MisTareasApp

Aplicacion Android de gestion de tareas construida con **Kotlin + Jetpack Compose + Material Design 3**.

<p>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" />
  <img alt="Min SDK" src="https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white" />
  <img alt="Target SDK" src="https://img.shields.io/badge/targetSdk-36-34A853?logo=android&logoColor=white" />
</p>

</div>

---

## Descripcion

`MisTareasApp` permite crear, editar y gestionar tareas con un flujo visual simple y moderno.
La interfaz usa Compose puro y una experiencia centrada en productividad diaria.

## Funcionalidades principales

- Creacion de tareas con nombre, descripcion, fecha de inicio y fecha de fin.
- Selector de fechas con calendario (`DatePickerDialog`).
- Estados de tarea por chip: `En espera`, `Iniciado`, `Finalizado`.
- Confirmacion de finalizacion mediante check dedicado cuando la tarea esta en `Finalizado`.
- Edicion completa de tareas con boton de lapiz.
- Seleccion/deseleccion masiva de tareas.
- Indicador de progreso diario.
- Persistencia local (las tareas se mantienen al reiniciar la app).
- Modo oscuro/claro.

## Stack tecnico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** Estado local en Compose (single-activity)
- **Persistencia:** `SharedPreferences` + JSON (`org.json`)
- **Build System:** Gradle Kotlin DSL

## Requisitos

- Android Studio (version reciente recomendada)
- JDK 11
- Android SDK:
  - `minSdk = 24`
  - `targetSdk = 36`
  - `compileSdk = 36`

## Ejecucion local

### Windows (PowerShell)

```powershell
.\gradlew.bat :app:installDebug
```

### Build rapido

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## Estructura del proyecto

```text
MisTareasApp/
├─ app/
│  ├─ src/main/java/ni/edu/uam/mistareasapp/MainActivity.kt
│  └─ build.gradle.kts
├─ build.gradle.kts
├─ settings.gradle.kts
└─ gradle/libs.versions.toml
```

## Hoja de ruta

- [ ] Migrar persistencia a DataStore o Room.
- [ ] Agregar filtros por estado y fecha.
- [ ] Incorporar pruebas UI con Compose Test.
- [ ] Mejorar accesibilidad y localizacion.

## Autor

Proyecto academico base para practica de Programacion Orientada a Objetos + Material Design 3.

---

<p align="center"><sub>Hecho con Kotlin y Jetpack Compose.</sub></p>

