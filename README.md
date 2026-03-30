# ReCasino Addon API

`ReCasino` is now a multi-module project:

- `plugin` - the main casino plugin
- `recasino-api` - public API for external addons
- `example-addon` - working example addon for this API

## JitPack

For this repository, the API dependency is:

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
        <version>v1.1.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Getting The API

```java
import net.recasino.api.ReCasinoApi;
import org.bukkit.Bukkit;

ReCasinoApi api = Bukkit.getServicesManager().load(ReCasinoApi.class);
if (api == null) {
    getLogger().severe("ReCasino API is not available");
    return;
}
```

## Registering A Mode

```java
api.registerMode(this, new MyCasinoMode());
```

## Registering A Roulette Animation

```java
api.registerRouletteAnimation(this, new MyRouletteAnimation());
```

The active roulette animation provider is selected in `plugin/src/main/resources/config.yml`:

```yml
animations:
  roulette:
    provider: "default"
```

## Example Addon

See [example-addon](D:/pluginslie/LeakedCasino/example-addon):

- [ExampleAddonPlugin.java](D:/pluginslie/LeakedCasino/example-addon/src/main/java/net/recasino/example/ExampleAddonPlugin.java) loads `ReCasinoApi` from Bukkit services and registers the addon.
- [CoinFlipMode.java](D:/pluginslie/LeakedCasino/example-addon/src/main/java/net/recasino/example/CoinFlipMode.java) adds a custom game mode into the main menu.
- [PulseRouletteAnimation.java](D:/pluginslie/LeakedCasino/example-addon/src/main/java/net/recasino/example/PulseRouletteAnimation.java) registers a custom roulette animation provider.

Build output for the example addon will be created in `example-addon/target/`.
