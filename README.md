# reflection-remapper
reflection-remapper is a library to simplify making reflective calls, with specific support for remapped environments.

## Getting reflection-remapper
There are currently no release builds.

> <details open>
> <summary>Using snapshot builds</summary>
>
> Snapshot builds are available on the [`https://repo.jpenilla.xyz/snapshots/`](https://repo.jpenilla.xyz/snapshots/) maven repository.
>
> Consult your build tool's documentation for details on adding maven repositories to your project.
> </details>

### Gradle Kotlin DSL
> <details open>
> <summary>Click to show build.gradle.kts</summary>
> 
> ```kotlin
> dependencies {
>   implementation("xyz.jpenilla", "reflection-remapper", "0.1.0-SNAPSHOT")
> }
> ```
> </details>

### Gradle Groovy DSL
> <details open>
> <summary>Click to show build.gradle</summary>
> 
> ```groovy
> dependencies {
>   implementation 'xyz.jpenilla:reflection-remapper:0.1.0-SNAPSHOT'
> }
> ```
> </details>

## Using reflection-remapper

*Note: The following is a basic introduction; For further details, consult the reflection-remapper Javadocs.*

### `ReflectionRemapper`
`ReflectionRemapper` is the interface provided by reflection-remapper for remapping class, method, and field names for reflection lookups.

> <details open>
> <summary>Click to see Java code snippet</summary>
> 
> ```java
> final Path mappingsFile = /* get path to mappings ... */;
> // ReflectionRemapper provides various static factory methods, in this example we use the one from a Path
> // Note that the standard ReflectionRemapper implementations store their mappings in memory, which can be multiple megabytes large in some cases.
> // This means it's best to use the remapper when your program starts and then dispose of any reference to it, so it can be garbage collected.
> final ReflectionRemapper reflectionRemapper = ReflectionRemapper.forMappings(mappingsFile, "fromNamespace", "toNamespace");
>
> final String runtimeName = reflectionRemapper.remapClassName("net.minecraft.server.level.ServerPlayer");
> final Class<?> serverPlayerClass = Class.forName(runtimeName); // Exception handling omitted for brevity
> final String runtimeFieldName = reflectionRemapper.remapFieldName(serverPlayerClass, "seenCredits");
> // ...
> ```
> </details>

### Reflection Proxies

A reflection proxy is a runtime generated implementation of a "reflection proxy interface", which uses cached `MethodHandle`s to invoke the targets specified by
the interface definition. Reflection proxies can also make use of a `ReflectionRemapper` on creation in order to allow them to work in a remapped (or not) environment.
The `ReflectionRemapper` on its own, while powerful, tends to make already verbose reflection code even more verbose. When combined with reflection proxies, we
can have relatively clean reflection code while still properly remapping.

> <details open>
> <summary>Click to see Java code snippet</summary>
> 
> #### Example target class
> ```java
> public final class ServerLevel {
>   private SleepStatus sleepStatus;
>
>   private BlockPos findLightningTargetAround(BlockPos pos) {
>     // implementation
>   }
> }
> ```
>
> #### Example reflection proxy interface
> ```java
> @Proxies(ServerLevel.class) // Can use Class or fully qualified class name (for inaccessible classes)
> private interface ServerLevelProxy {
>   BlockPos findLightningTargetAround(ServerLevel instance, BlockPos pos);
>
>   @FieldSetter("sleepStatus")
>   void setSleepStatus(ServerLevel instance, SleepStatus value);
> }
> ```
>
> #### Creating and using reflection proxy instance
> ```java
> final ReflectionRemapper reflectionRemapper = /* get ReflectionRemapper ... */;
> final ClassLoader classLoader = /* get ClassLoader for loading proxy implementations, generally it needs to be able to see any reflection proxy interfaces you want to implement */;
> final ReflectionProxyFactory reflectionProxyFactory = ReflectionProxyFactory.create(reflectionRemapper, classLoader); // ReflectionProxyFactory holds a ref to it's ReflectionRemapper
>
> // Generated proxy instances do not hold a ref to the ReflectionRemapper, and are fine to keep around.
> final ServerLevelProxy proxyInstance = reflectionProxyFactory.reflectionProxy(ServerLevelProxy.class);
>
> // ...
> 
> final ServerLevel level = /* ... */;
> final BlockPos blockPos = /* ... */;
> final BlockPos target = proxyInstance.findLightningTargetAround(level, blockPos);
> proxyInstance.setSleepStatus(level, new SleepStatus());
> ```
> </details>
