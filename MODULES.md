## spigot-jar
The spigot-jar module compiles a jar meant for installation on a spigot server. It has one dependency: `KamiCommon`.  
This module just provides the `:core` module with a plugin source.

## core
The core module is the primary module for SyncEngine code. It contains everything needed to run SyncEngine **except the providing plugin**. For this it uses the `EngineSource` class which is assumed to be provided with a valid plugin.  
This module is great for shading into your own plugin if you'd like. You just need to do a few things:

### Step 1 - Adding the Repository
First you'll need to add the public maven repository:
#### Maven [pom.xml]:
```xml
<repository>
  <id>luxious-public</id>
  <name>Luxious Repository</name>
  <url>https://nexus.luxiouslabs.net/public</url>
</repository>
```
#### Gradle (kotlin) [build.gradle.kts]:
```kotlin
maven {
    name = "luxiousPublic"
    url = uri("https://nexus.luxiouslabs.net/public")
}
```
#### Gradle (groovy) [build.gradle]:
```groovy
maven {
  name "luxiousPublic"
  url "https://nexus.luxiouslabs.net/public"
}
```

### Step 2 - Adding the Dependency
#### Maven Dependency [pom.xml]
```xml
<dependency>
  <groupId>com.kamikazejam.syncengine</groupId>
  <artifactId>core</artifactId>
  <version>{VERSION}</version>
  <scope>compile</scope> <!-- for shading -->
</dependency>
```

#### Gradle Dependency (groovy) [build.gradle]
```groovy
implementation "com.kamikazejam.kamicommon:core:{VERSION}"
```

#### Gradle Dependency (kotlin) [build.gradle.kts]
```kotlin
implementation("com.kamikazejam.kamicommon:core:{VERSION}")
```


### Step 3 - Initializing `EngineSource`
EngineSource is heavily integrated with `KamiCommon`, as such it expects a slightly modified plugin provider: `KamiPlugin`  
You can swap out your `extends JavaPlugin` with `extends KamiPlugin` and then rename `onEnable` and `onDisable` to `onEnableInner` and `onDisableInner`.

Here's an example, just make sure to call `EngineSource` before using it.
```java
public class MyPlugin extends KamiPlugin {
    @Override
    public void onEnableInner() {
        EngineSource.onEnable(this);
        // Code using SyncEngine
    }

    @Override
    public void onDisableInner() {
        EngineSource.onDisable();
    }
}
```

### Step 4 - Providing SyncEngine's Dependencies
SyncEngine has a few dependencies, but the only one that a developer needs to ensure is `KamiCommon`.  
SyncEngine does not shade `KamiCommon`, as such it is necessary that it finds it on the classpath. The `:spigot-jar` module does this by setting a plugin dependency.  
As a developer using SyncEngine you can either add a `KamiCommon` plugin dependency to your plugin, or shade `KamiCommon` into your project as well.  
The process for shading `KamiCommon` is very similar, and just requires you use `KamiCommon`'s `PluginSource` in the same manner as `EngineSource`. (Call `PluginSource` first on enable).  

All other dependencies (like MongoJack) are shaded into the `core` module, so you don't need to worry about them.



