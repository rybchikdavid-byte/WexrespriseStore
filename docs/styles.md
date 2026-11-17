# Стили Wexresprise Store (`.ws`)

Стиль — это ZIP-архив с расширением `.ws`. Внутри:

```
manifest.json
classes.dex            (можно classes2.dex, classes3.dex …)
icon.png               (необязательно, до 26×26 dp для списка стилей)
assets/…               (необязательно, любые файлы; путь придёт в StyleConfig.assetsDir)
```

Ограничения при установке: архив до 24 МБ, распакованные файлы до 20 МБ,
не больше 200 записей.

## manifest.json

```json
{
  "id": "com.example.neon",
  "name": "Neon Nights",
  "version": "1.0",
  "author": "you",
  "class": "com.example.neon.NeonStyle",
  "description": "Яркий неоновый стиль с крупными карточками."
}
```

- `id` — латиница, цифры, `.`, `_`, `-`, до 80 символов. Папка стиля называется так же.
- `class` — полное имя класса, который реализует `com.wexresprise.store.style.WexStyle`
  и имеет публичный конструктор без аргументов.
- `name`, `version`, `author`, `description` — показываются в списке.

## Что может стиль

`WexStyle.apply(StyleConfig style)` вызывается при создании каждого экрана.
Поля `StyleConfig` можно свободно менять:

| Поле | Значение |
| --- | --- |
| `accent`, `onAccent` | акцент и цвет текста на нём |
| `background`, `surface` | фон окна и непрозрачная поверхность |
| `text`, `subtext`, `outline` | цвета текста и тонких рамок |
| `cardFill`, `cardStroke`, `cardRadius` | заливка, обводка и радиус карточек |
| `radius` | радиус кнопок и небольших панелей, dp |
| `fontScale` | масштаб текста, 0.85–1.25 |
| `backgroundMode` | `BACKGROUND_AURORA`, `BACKGROUND_GRADIENT`, `BACKGROUND_SOLID` |
| `cardStyle` | `CARDS_GLASS`, `CARDS_SOLID`, `CARDS_OUTLINE` |
| `density` | `DENSITY_COMFORTABLE`, `DENSITY_COMPACT` |
| `iconTint` | цвет логотипа в шапке |
| `assetsDir` | абсолютный путь к распакованным `assets/` или `null` |

Вспомогательные методы: `StyleConfig.contrast(color)` — чёрный или белый поверх цвета,
`StyleConfig.alpha(color, percent)` — тот же цвет с прозрачностью в процентах,
`setAccent(color)` — акцент вместе с `onAccent` и `iconTint`.

Пример:

```java
package com.example.neon;

import com.wexresprise.store.style.StyleConfig;
import com.wexresprise.store.style.WexStyle;

public class NeonStyle implements WexStyle {
    @Override
    public void apply(StyleConfig s) {
        s.setAccent(0xFF00E5FF);
        s.background = 0xFF05070C;
        s.backgroundMode = StyleConfig.BACKGROUND_SOLID;
        s.cardStyle = StyleConfig.CARDS_OUTLINE;
        s.cardFill = 0x1400E5FF;
        s.cardStroke = 0xAA00E5FF;
        s.cardRadius = 4f;
        s.radius = 4f;
        s.text = 0xFFE8FBFF;
        s.subtext = 0xFF7FD8E8;
        s.outline = 0x5500E5FF;
        s.density = StyleConfig.DENSITY_COMPACT;
        s.fontScale = 0.95f;
    }
}
```

Стиль может и переключить `night`, но системная тема диалогов выбирается до его
загрузки, так что лучше оставить это поле как есть.

## Как собрать

Нужны JDK 17 и Android SDK (`d8` из `build-tools`).

```bash
# 1. Скопируй API стилей из репозитория
mkdir -p src/com/wexresprise/store/style
cp WexrespriseStore/app/src/main/java/com/wexresprise/store/style/*.java \
   src/com/wexresprise/store/style/

# 2. Положи свой класс рядом, например src/com/example/neon/NeonStyle.java

# 3. Скомпилируй под Java 8
javac -source 8 -target 8 -d out $(find src -name '*.java')

# 4. Сконвертируй в dex
d8 --lib $ANDROID_HOME/platforms/android-35/android.jar --output . \
   $(find out -name '*.class')

# 5. Собери архив: manifest.json, classes.dex, при желании icon.png и assets/
zip -r neon.ws manifest.json classes.dex icon.png assets
```

`assets/` в архив кладётся как есть; в приложении его путь будет в
`StyleConfig.assetsDir`.

## Безопасность

`.ws` — это код, а не данные. Он выполняется внутри Wexresprise Store с теми же
разрешениями, что и само приложение: доступ к сети, к выбранным файлам и к
настройкам. Прочитать чужой стиль перед установкой нельзя, поэтому приложение
показывает предупреждение. Устанавливай только свои стили или стили авторов,
которым доверяешь.

## Удаление и выбор

На экране **Оформление** → **Стили на Java (.ws)**:

- нажатие на стиль — применить;
- долгое нажатие — удалить с телефона;
- **Встроенное оформление** — вернуться к настройкам из приложения.

Одновременно применён один стиль. Если стиль сломается при загрузке, приложение
продолжит работать со встроенным оформлением, а причина появится в разделе стилей.
