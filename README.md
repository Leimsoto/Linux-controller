# Linux Controller + IA CoPilot 🐧⚙️🤖

Una aplicación Android nativa, moderna y elegante desarrollada en **Kotlin** y **Jetpack Compose (Material Design 3)**, diseñada para administrar, monitorear y controlar servidores Linux remotos de forma remota a través de SSH/SFTP, potenciada con un **Copiloto de Inteligencia Artificial de Google Gemini y Gemma 2**.

---

## 🚀 Características Clave

### 1. 🎛️ Gestor de Conexiones SSH
- **Multi-Perfil incorporado**: Crea, edita y gestiona accesos para múltiples servidores de manera local y segura.
- **Autenticación robusta**: Compatible tanto con contraseña clásica como con claves privadas SSH.
- **Soporte Sudo Automatizado**: Asigna opcionalmente una contraseña de Sudo/Root por perfil para ejecutar tareas con permisos de administrador en un solo toque, sin interrupciones.

### 2. ⚡ Atajos & Comandos Rápidos
- Registra tus comandos favoritos clasificados en categorías útiles.
- Ejecuta utilidades complejas con un solo tap (ej. monitor de recursos, levantar contenedores, limpiar caché) y observa las respuestas en un HUD contextual de inmediato.

### 3. 💻 Terminal Interactiva Integrada
- Consola líquida nativa para la ejecución fluida de cualquier comando en tiempo real.
- Historial limpio con sistema de autodesplazamiento y soporte de limpieza instantánea.

### 4. 📂 Explorador Remoto de Archivos ("Archivos")
- Conexión **SFTP** incorporada para visualizar estructuras de directorios remotos.
- Sube archivos locales del terminal al servidor o descarga cualquier archivo a tu espacio local con un solo toque.
- Comparte, renombra y elimina archivos remotos directamente desde el dispositivo Android.

### 5. 🤖 Copiloto de Inteligencia Artificial (Copiloto IA)
- **Modelos Avanzados**: Integra soporte nativo para **Gemma 2 27B (Gemma 4 31B)** y **Gemini 2.5 Flash / Pro** ingresando tu propia API Key de Google de forma segura.
- **Búsqueda en Tiempo Real (Grounding)**: Activa la integración de Google Search para que el copiloto consulte páginas web y use información del último minuto para tus comandos.
- **System Prompts personalizables**: Cambia las directrices de respuesta de la IA cuando lo consideres oportuno.
- **Extracción de comandos interactivos**: Si el copiloto sugiere comandos útiles, la interfaz los renderiza como **chips accionables**. Tócalos para ejecutarlos instantáneamente sobre tu servidor activo actual.

---

## 🛠️ Stack Tecnológico

- **Interfaz de Usuario**: Jetpack Compose 100%, Material Design 3, animaciones adaptativas y diseño responsive.
- **Base de Datos Local**: SQL / Room Database con KSP para la persistencia de perfiles y comandos.
- **Motor SSH y SFTP**: JSch (Java Secure Channel) optimizado para transacciones asíncronas móviles.
- **IA y Red**: Retrofit y OkHttp optimizados con timeouts de 60 segundos con Moshi Converter para la comunicación con la API oficial de Google AI de forma nativa e hiperveloz.

---

## 📦 Configuración y Despliegue

### Requisitos Previos
1. Un dispositivo o emulador Android (Android 8.0+ / API 26 mínimo) con conexión a internet.
2. Acceso SSH y SFTP al servidor de destino (fuegos artificiales de red permitidos).
3. Una **Gemini API Key** (opcional, requerida para el Copiloto IA), que puedes obtener gratis en [Google AI Studio](https://aistudio.google.com/).

### Instrucciones de Uso

1. **Agregar Servidor**:
   - Ve a la pestaña **Conexiones** y haz clic en el botón de agregar (+).
   - Rellena el Host (IP/Dominio), Puerto, Usuario y Contraseña o Llave Privada.
   - Agrega tu contraseña sudo opcional si deseas ejecutar comandos que la requieran automáticamente.
2. **Configurar el Copiloto**:
   - Accede a la pantalla **Copiloto IA**.
   - Despliega la pestaña superior **Configuración Copiloto Gemini & Gemma**.
   - Pega tu API Key y selecciona tu modelo favorito (ej. **Gemma 2 27B** o **Gemini 2.5 Flash**).
   - ¡Listo! Comienza a chatear y a tocar comandos recomendados para que se ejecuten directamente sobre tu consola SSH.

---

## 🛡️ Seguridad y Privacidad
Todas tus credenciales, claves SSH, contraseñas de Sudo y API Keys de Gemini se guardan **únicamente localmente** en el espacio seguro de almacenamiento de la aplicación (SharedPreferences encriptables / BD SQLite local). Ninguna información se comparte con terceros, garantizando la privacidad absoluta de tus servidores.

---

## 🔒 Estabilidad y Calidad de Código (Últimas Mejoras)

### 1. ⚡ Prevención Activa de Crashes en la Interfaz (Zero-Crash Scroll Support)
Se implementaron salvaguardas avanzadas en las animaciones de desplazamiento automático (`animateScrollToItem`) que previenen colisiones en el hilo de renderizado o excepciones de Compose asíncronas cuando se reciben actualizaciones ráfaga del terminal SSH en tiempo real o en historiales de chat dinámicos del Copiloto IA.

### 2. 🧪 Cobertura de Pruebas Unitarias Robustas (`ExampleUnitTest`)
Diseño de un set de pruebas unitarias locales en el JVM que valida las capacidades de parsing del motor extractor de comandos:
- Extracción limpia de comandos bajo bloques estructurados (` ```bash `...` ``` `) y de acentos graves simples (como `` `ls` ``).
- Robustez contra respuestas de la IA mal formateadas u truncadas (bloques abiertos/sin cerrar al final del mensaje).
- Validación de inicialización asíncrona segura de Repositorios y ViewModels en Room local e in-memory.
