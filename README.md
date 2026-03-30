# ReCasino Addon API

`ReCasino` теперь собирается как multi-module проект:

- `plugin` - основной плагин
- `recasino-api` - публичный API для аддонов

## JitPack

Для multi-module проекта JitPack публикует каждый модуль отдельно. Для `recasino-api` подключение выглядит так:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.<GitHubUser>.<RepoName></groupId>
        <artifactId>recasino-api</artifactId>
        <version>v1.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Если нужен весь репозиторий целиком, JitPack также собирает агрегирующий артефакт корня.

## Как получить API

```java
import net.recasino.api.ReCasinoApi;
import org.bukkit.Bukkit;

ReCasinoApi api = Bukkit.getServicesManager().load(ReCasinoApi.class);
if (api == null) {
    getLogger().severe("ReCasino API is not available");
    return;
}
```

## Регистрация режима

```java
api.registerMode(this, new MyCasinoMode());
```

Режим получает:

- слот в главном меню `ReCasino`
- своё собственное GUI-окно
- профиль игрока `CasinoPlayerProfile`
- доступ к балансу монет/рилликов через `CasinoModeContext`

## Регистрация анимации рулетки

```java
api.registerRouletteAnimation(this, new MyRouletteAnimation());
```

Активный провайдер выбирается в `config.yml`:

```yml
animations:
  roulette:
    provider: "default"
```

## Что умеет API

- добавлять свои режимы казино в главное меню
- перехватывать клики и полностью рисовать своё GUI
- читать и менять профиль игрока
- регистрировать собственные анимации рулетки
- автоматически очищать регистрации, когда аддон-плагин выключается
