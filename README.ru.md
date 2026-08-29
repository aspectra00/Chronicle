<p align="center">
  <a href="README.md">English</a> ·
  <strong>Русский</strong> ·
  <a href="README.zh-CN.md">简体中文</a> ·
  <a href="README.es.md">Español</a> ·
  <a href="README.de.md">Deutsch</a>
</p>

<p align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/849c602a1ac00208e0371ed231540da69e4fbfb3.png" alt="Chronicle" width="600">
</p>

<p align="center">
  <a href="https://github.com/aspectra00/Chronicle"><img src="https://i.imgur.com/vFmBpDq.png" alt="GitHub" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://ko-fi.com/aspectra"><img src="https://i.imgur.com/H08GkHi.png" alt="Ko-fi" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://modrinth.com/mod/chronicle-reminders"><img src="https://i.imgur.com/VROd79E.png" alt="Modrinth" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/chronicle-reminders"><img src="https://i.imgur.com/IDs74bZ.png" alt="CurseForge" width="64" height="64"></a>
</p>

<p align="center">
  <img src="badges-for-readme/minecraft.svg" alt="Minecraft" height="38">
  <img src="badges-for-readme/fabric.svg" alt="Fabric Loader" height="38">
  <img src="badges-for-readme/java.svg" alt="Java" height="38">
  <img src="badges-for-readme/chronicle.svg" alt="Chronicle" height="38">
</p>

Chronicle — клиентский мод напоминаний для Minecraft. Он работает в одиночной игре и на многопользовательских серверах без установки на сервер.

## Поддержать Chronicle

Chronicle распространяется бесплатно и поддерживается для каждой совместимой версии Minecraft. Если мод помог вам сэкономить время или не забыть что-то важное, вы можете поддержать дальнейшую разработку и тестирование обновлений.

<p align="center">
  <a href="https://ko-fi.com/aspectra"><img src="https://storage.ko-fi.com/cdn/brandasset/v2/support_me_on_kofi_blue.png" alt="Поддержать Chronicle на Ko-fi" width="220"></a>
</p>

Поддержка идёт на совместимость, тестирование релизов и новые функции напоминаний. Участники также могут по желанию появиться на внутриигровом экране Community Supporters.

## Возможности

### Напоминания

- Ежедневное расписание
- Еженедельное расписание с выбором дней
- Пользовательские интервалы повторения
- Создание, изменение, отключение и удаление напоминаний в игре
- Сохранение, отключение или удаление напоминания после срабатывания
- Проверка текущих настроек уведомлений прямо из меню

### Правила срабатывания

Напоминание может сработать, когда:

- Здоровье, голод или запас воздуха достигает заданного уровня
- Инвентарь заполнен
- Прочность предмета в руке достигает заданного предела
- Игрок входит в другое измерение
- Игрок входит в заданную область по координатам X/Z

Правило срабатывает при переходе условия из невыполненного состояния в выполненное. Оно снова становится готовым после того, как условие перестаёт выполняться.

### Watch This

Наведитесь на поддерживаемый объект и нажмите `R`, чтобы начать или прекратить наблюдение. Chronicle может сообщить, когда:

- Урожай полностью вырастет
- Улей или пчелиное гнездо наполнится мёдом
- Котёл или компостер будет готов
- На пещерной лозе вырастут ягоды
- Печь, коптильня или плавильная печь остановится
- Медь полностью окислится
- Детёныш моба вырастет

На экране Watches отображаются активные цели текущего мира или сервера. Chronicle проверяет только данные, уже доступные клиенту, поэтому цели в незагруженных областях остаются в ожидании.

### Уведомления

- Современный и ванильный макеты
- Необязательные кнопки Snooze и Dismiss в современном макете
- Отложить уведомление можно на 5, 10, 15, 30 или 60 минут
- История пропущенных, выполненных и отложенных напоминаний
- Темы Minimal, Neon, Glass и Matrix
- Настройка заголовка, значка, цветов, размеров и анимации
- Необязательный фон PNG или JPG для современных уведомлений
- Предпросмотр изменений в реальном времени
- Ванильный, приглушённый или пользовательский звук уведомления

Для пользовательского звука поддерживаются MP3, OGG, WAV, AIFF и AU. JLayer включён в мод для декодирования MP3; подробности приведены в [уведомлениях о сторонних компонентах](THIRD_PARTY_NOTICES.md).

### Подстановки

В тексте напоминаний доступны:

- `{world}`
- `{coords}`
- `{biome}`
- `{dimension}`

Также поддерживаются подстановки, зарегистрированные через Text Placeholder API.

### Языки

- Английский
- Русский
- Упрощённый китайский
- Испанский
- Немецкий

## Управление

| Клавиша | Действие |
|---|---|
| `J` | Открыть Chronicle |
| `R` | Начать или прекратить наблюдение за объектом под прицелом |

Обе клавиши можно изменить в настройках управления Minecraft.

## Требования

| Зависимость | Версия |
|---|---:|
| Minecraft | 1.21.10 |
| Fabric Loader | 0.17.0 или новее (рекомендуется 0.19.3) |
| Fabric API | 0.138.4+1.21.10 |
| Java | 21 |

Mod Menu необязателен. Text Placeholder API уже включён в JAR Chronicle.

## Установка

1. Установите Fabric Loader и Fabric API для указанной версии Minecraft.
2. Скопируйте JAR Chronicle в папку `mods`.
3. Запустите Minecraft и нажмите `J`.

Настройки хранятся в `config/chronicle.json`.

## Сборка

Используйте указанную выше версию Java и выполните:

```powershell
.\gradlew.bat clean build
```

Готовый JAR появится в `build/libs`.

## Лицензия

Chronicle распространяется по [лицензии MIT](LICENSE).
