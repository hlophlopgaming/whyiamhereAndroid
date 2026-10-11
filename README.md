# Зачем я здесь? — Android

Локальный помощник для осознанного использования приложений. Kotlin, Jetpack
Compose Material 3, Room и Preferences DataStore. Без сети и root.

## Текущий этап: 1 — каркас

Добавлены экран настроек, тема, адаптивная иконка, модели `Session` и
`HistoryItem`, Room Database и DataStore. Служба специальных возможностей,
выбор приложений, форма и виджет пока не реализованы. Приложение ещё не
ограничивает использование других приложений.

Пакет: `dev.cena.whyiamhere`. Android 8.0+ (minSdk 26), compile/target SDK 37.
Сессии хранят абсолютное время окончания в миллисекундах Unix; изменение часов
телефона изменит оставшееся время. Облачное резервное копирование отключено.

## Сборка

Нужны JDK 17 и Android SDK с платформой 37.0 и Build Tools 36.0.0.
Откройте проект в Android Studio с поддержкой AGP 9.1.1 либо задайте
`ANDROID_HOME` / `sdk.dir` в локальном `local.properties`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Проверка на телефоне: установить APK, открыть «Зачем я здесь?», убедиться,
что экран «Настройки» виден, включая светлую и тёмную темы, затем перезапустить.
Release-подпись и публикация будут добавлены на этапе 8; ключи не коммитить.

## Следующие этапы

2. Прототип `WhyService`, фильтр пакетов, Logcat; проверка на телефоне.
3. Выбор приложений, сохранение настроек и онбординг.
4. `GateActivity`, запуск сессии, отмена ведёт домой.
5. Перетаскиваемый `TYPE_ACCESSIBILITY_OVERLAY` с целью и таймером.
6. Предупреждение, звук и три пути завершения сессии.
7. История и быстрый выбор целей.
8. Восстановление, проверки прошивок, release APK и инструкция установки.

До этапа 2 нужно согласовать способ фильтрации окон: `getWindows()` требует
`canRetrieveWindowContent=true` и `flagRetrieveInteractiveWindows`. С
`canRetrieveWindowContent=false` доступна фильтрация пакетов событий, которая
нуждается в проверке на телефоне. См.
[AccessibilityServiceInfo](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo).

Вне MVP: графики, расписания, экспорт и Google Play. AccessibilityService —
добровольный помощник, а не гарантия блокировки: пользователь может отключить
службу, и приложение может кратко появиться до формы. Возврат домой не
останавливает фоновое воспроизведение.
