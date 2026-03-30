# ReCasino Addon API

`ReCasino` теперь собран как multi-module проект:

- `plugin` - основной плагин казино
- `recasino-api` - публичный API для внешних аддонов
- `example-addon` - готовый пример аддона на этом API

## Подключение через JitPack

Для подключения `recasino-api` используйте:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.sxkadamn.ReCasino</groupId>
        <artifactId>recasino-api</artifactId>
        <version>v1.1.3</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Как получить API

```java
import net.recasino.api.ReCasinoApi;
import org.bukkit.Bukkit;

ReCasinoApi api = Bukkit.getServicesManager().load(ReCasinoApi.class);
if (api == null) {
    getLogger().severe("ReCasino API недоступен");
    return;
}
```

## Регистрация режима

```java
api.registerMode(this, new MyCasinoMode());
```

## Регистрация анимации рулетки

```java
api.registerRouletteAnimation(this, new MyRouletteAnimation());
```

Активный провайдер анимации выбирается в [plugin/src/main/resources/config.yml](D:/pluginslie/LeakedCasino/plugin/src/main/resources/config.yml):

```yml
animations:
  roulette:
    provider: "default"
```

## Hex-цвета в аддонах

В API есть публичный util: [TextUtil.java](D:/pluginslie/LeakedCasino/api/src/main/java/net/recasino/api/util/TextUtil.java)

Он поддерживает:

- обычные `&`-цвета
- hex-цвета вида `&#FFD54F`

Пример:

```java
import net.recasino.api.util.TextUtil;

player.sendMessage(TextUtil.color("&#FFD54FПример &7сообщения"));
```

## Пример аддона

Смотрите модуль [example-addon](D:/pluginslie/LeakedCasino/example-addon):

- [ExampleAddonPlugin.java](D:/pluginslie/LeakedCasino/example-addon/src/main/java/net/recasino/example/ExampleAddonPlugin.java) - получает `ReCasinoApi` через Bukkit Services и регистрирует расширения
- [CoinFlipMode.java](D:/pluginslie/LeakedCasino/example-addon/src/main/java/net/recasino/example/CoinFlipMode.java) - добавляет свой режим в главное меню
- [PulseRouletteAnimation.java](D:/pluginslie/LeakedCasino/example-addon/src/main/java/net/recasino/example/PulseRouletteAnimation.java) - добавляет свою анимацию рулетки

Готовый jar примера собирается в `example-addon/target/`.
