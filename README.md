# Wexresprise Store

Отдельное Android-приложение — полка файлов сообщества. Не сайт и не WebView.

## Что умеет

- Две вкладки: **Все** и **Wexresprise Projects**.
- **Все** — все опубликованные файлы: APK, видео, аудио, картинки, документы.
- **Wexresprise Projects** — файлы, у которых включён флажок проекта или в названии есть «Wexresprise».
- **Опубликовать** — выбрать любой файл на телефоне, добавить название и описание, загрузить.
- Карточка файла: иконка с типом, название, описание, размер, дата и кнопка **Скачать**.
- Тёмная тема, Liquid Glass карточки, живой градиент «аврора», иконка-корзина с «W».
- Материал You: на Android 12+ акцентный цвет берётся из системной палитры.

## Сервер (Supabase)

Общий список файлов требует бэкенда. Приложение работает с Supabase напрямую,
без своего сервера и без библиотек — только `HttpURLConnection`.

1. Создай бесплатный проект на [supabase.com](https://supabase.com).
2. Открой **SQL Editor**, вставь скрипт из `app/src/main/res/raw/setup.sql` и нажми **Run**.
   Скрипт создаёт таблицу `public.files`, политики доступа и публичный бакет `wexstore`.
3. Открой **Project Settings → API** и скопируй **Project URL** и **anon public key**.
4. В приложении: шестерёнка → вставь URL и ключ → **Сохранить** → **Проверить подключение**.

Anon-ключ публичный по задумке Supabase, доступ ограничивают политики из SQL.
Бесплатный тариф принимает файлы до 50 МБ.

Пока сервер не подключён, приложение показывает встроенные демо-файлы и не даёт
публиковать — кнопка «Опубликовать» предложит открыть настройки.

Можно вшить сервер в сборку: заполнить `DEFAULT_URL` и `DEFAULT_KEY`
в `app/src/main/java/com/wexresprise/store/StoreConfig.java`.

## Сборка

Через GitHub Actions: workflow `.github/workflows/build.yml` собирает release APK
и кладёт его в артефакт `WexrespriseStore-apk`.

Локально в Android Studio: открыть папку проекта, JDK 17, Android SDK 35.

Подпись release — `keystore/wexresprise.p12` (alias `wexresprise`).
Ключ тот же, что у Wexresprise Launcher, поэтому обновления совместимы.

## Структура

```
app/src/main/java/com/wexresprise/store/
  MainActivity.java     — вкладки, список, публикация, скачивание
  SettingsActivity.java — подключение Supabase, SQL, проверка
  StoreApi.java         — PostgREST + Storage, загрузка с прогрессом
  StoreAdapter.java     — стеклянные карточки файлов
  AuroraView.java       — анимированный фон
  GlassDrawable.java    — «жидкое стекло»
  BadgeView.java        — значок типа файла
  DemoData.java         — демо-список
demo/                   — файлы для демо-режима (раздаются с GitHub raw)
```
