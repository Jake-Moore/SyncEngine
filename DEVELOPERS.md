## Developer Guide

The SyncEngine project only has one module. Depending on how its used, a developer can achieve different goals.  
For instance, you can shade it into your own plugin if you'd like. You just need to do a few things:

**Developers hooking into the SyncEngine plugin**: Follow Steps 1 & 2  
**Developers shading SyncEngine**: Follow Steps 1 through 4

### Step 1 - Adding the Repository
To do anything with SyncEngine, you'll have to add it to your project.  
First you'll need to add the public maven repository:
#### Maven [pom.xml]:
```xml
<repository>
  <id>luxious-public</id>
  <name>Luxious Repository</name>
  <url>https://repo.luxiouslabs.net/repository/maven-public/</url>
</repository>
```
#### Gradle (kotlin) [build.gradle.kts]:
```kotlin
maven {
    name = "luxiousPublic"
    url = uri("https://repo.luxiouslabs.net/repository/maven-public/")
}
```
#### Gradle (groovy) [build.gradle]:
```groovy
maven {
  name "luxiousPublic"
  url "https://repo.luxiouslabs.net/repository/maven-public/"
}
```

### Step 2 - Adding the Dependency
Next, add the dependency to your project.
#### Maven Dependency [pom.xml]
```xml
<dependency>
  <groupId>com.kamikazejam</groupId>
  <artifactId>SyncEngine</artifactId>
  <version>{VERSION}</version>
  <scope>provided</scope> <!-- OR `compile` with the shade plugin -->
</dependency>
```

#### Gradle Dependency (groovy) [build.gradle]
```groovy
// or shadow/implementation for shading
compileOnly "com.kamikazejam:SyncEngine:{VERSION}"
```

#### Gradle Dependency (kotlin) [build.gradle.kts]
```kotlin
// or shadow/implementation for shading
compileOnly("com.kamikazejam:SyncEngine:{VERSION}")
```


### Step 3 - Initializing `EngineSource`
If you're a developer wanting to tie into SyncEngine, and you're assuming SyncEngine is installed as a plugin, then you are done!  

If you are a developer wanting to shade SyncEngine, then you have a few more steps:

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
SyncEngine does not shade `KamiCommon`, as such it is necessary that it finds it on the classpath.  
The easiest way to do this is to install KamiCommon on the server side, and depend on it in your `plugin.yml`.  

For developers who don't want to do this, you can shade the KamiCommon `spigot-jar` module and use its `PluginSource` class in a similar way to `EngineSource`.

All other dependencies (like MongoJack) are shaded into the jar, so you don't need to worry about them.



