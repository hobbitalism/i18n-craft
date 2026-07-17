# i18n-craft

A lightweight localization (i18n) framework for Bukkit/Paper Minecraft servers, written in Java.

## Features

- **Resource-based translations** — Bundle YAML translation files inside your plugin JAR
- **JSON manifest** — Declare available locale files in a simple manifest
- **Auto-patching** — New keys from bundled resources are automatically appended to on-disk files
- **Placeholder substitution** — `%key%` style placeholders with a customizable `PlaceholderProcessor`
- **Reload support** — Synchronous (`reload()`) and asynchronous (`reloadAsync()`) reloading
- **Lightweight** — Zero external dependencies

## Usage

```java
ResourceTranslator translator = I18nCraft.createTranslator(config);

// Translate with a specific locale
String message = translator.translate("en_US", "greeting.welcome", "Welcome!");

// Placeholder substitution
String message = translator.translate("en_US", "greeting.welcome", 
    PlaceholderUtil.of("player", "Steve"));
```

## Installation

### Gradle

```groovy
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.hobbitalism:i18n-craft:i18n-craft-core:0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.hobbitalism</groupId>
    <artifactId>i18n-craft</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Advanced Usage

### Custom PlaceholderProcessor

Implement `PlaceholderProcessor` to define your own placeholder syntax:

```java
public class BracketsProcessor implements PlaceholderProcessor {
    @Override
    public String transform(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return text;
    }
}
```

Then wire it into the translator config:

```java
ResourceTranslator translator = ResourceTranslator.builder()
        .plugin(plugin)
        .translationMetadata(metadata)
        .translationConfigMap(new HashMap<>())
        .translatorConfig(TranslatorConfig.builder()
                .fallbackLanguage("en_US")
                .placeholderProcessor(new BracketsProcessor())
                .build())
        .build();
translator.loadFromResource(Path.of("i18n/i18n-manifest.json"));
```

### Manual TranslatorConfig

For full control (custom config directory, fallback locale, placeholder processor):

```java
TranslatorConfig config = TranslatorConfig.builder()
        .fallbackLanguage("vi_VN")
        .configDirectory("translations")
        .build();
```

### Reloading

Reload all translation files from disk when they change:

```java
// Synchronous
translator.reload();

// Asynchronous
translator.reloadAsync().thenRun(() ->
        plugin.getLogger().info("Translations reloaded!"));
```

### Auto-patching

When a plugin update adds new translation keys to the bundled YAML files,
`ResourceTranslator` automatically appends those keys to the existing on-disk
files on first access — without overwriting any existing translations.

### Resource Structure

Place your translation files and manifest inside your plugin JAR:

```
src/main/resources/
└── i18n/
    ├── i18n-manifest.json     # ["i18n/en_US.yml", "i18n/vi_VN.yml"]
    ├── en_US.yml
    └── vi_VN.yml
```

**Manifest format** — a JSON array of resource paths:

```json
["i18n/en_US.yml", "i18n/vi_VN.yml"]
```

**Translation file format** — standard YAML key-value pairs:

```yaml
greeting:
  welcome: "Welcome, %player%!"
  goodbye: "See you later, %player%!"
error:
  generic: "Something went wrong."
```

## License

[MIT](LICENSE)
