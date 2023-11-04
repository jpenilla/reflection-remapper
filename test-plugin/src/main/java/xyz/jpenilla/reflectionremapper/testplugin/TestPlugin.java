/*
 * reflection-remapper
 *
 * Copyright (c) 2021-2023 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jpenilla.reflectionremapper.testplugin;

import cloud.commandframework.Command;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import io.papermc.paper.util.MCUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@DefaultQualifier(NonNull.class)
public final class TestPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    Reflection.class.getClassLoader(); // init Reflection

    final PaperCommandManager<CommandSender> commandManager = createCommandManager(this);
    this.registerCommands(commandManager);
  }

  private void registerCommands(final PaperCommandManager<CommandSender> manager) {
    final Command.Builder<CommandSender> createEndPlatform = manager.commandBuilder("create_end_platform")
      .senderType(Player.class)
      .handler(this::executeCreateEndPlatform);
    manager.command(createEndPlatform);

    final Command.Builder<CommandSender> strikeLightning = manager.commandBuilder("strike_lightning")
      .senderType(Player.class)
      .handler(this::executeStrikeLightning);
    manager.command(strikeLightning);
  }

  private void executeCreateEndPlatform(final CommandContext<CommandSender> ctx) {
    final ServerPlayer serverPlayer = ((CraftPlayer) ctx.getSender()).getHandle();
    Reflection.SERVER_PLAYER.createEndPlatform(serverPlayer, (ServerLevel) serverPlayer.level(), serverPlayer.blockPosition());
  }

  private void executeStrikeLightning(final CommandContext<CommandSender> ctx) {
    final ServerPlayer serverPlayer = ((CraftPlayer) ctx.getSender()).getHandle();

    System.out.println(Reflection.SERVER_LEVEL.neighborUpdater(serverPlayer.level()));
    System.out.println(Reflection.LEVEL.neighborUpdater(serverPlayer.level()));

    Reflection.SERVER_LEVEL.test1();
    Reflection.SERVER_LEVEL.test2();
    Reflection.LEVEL.test1();
    Reflection.SERVER_LEVEL.test0();
    Reflection.LEVEL.test0();

    final BlockPos lightningTarget = Reflection.SERVER_LEVEL.findLightningTargetAround((ServerLevel) serverPlayer.level(), serverPlayer.blockPosition());
    ((Player) ctx.getSender()).getWorld().strikeLightning(MCUtil.toLocation(serverPlayer.level(), lightningTarget));
  }

  public static final class Reflection {
    public static final ServerLevelProxy SERVER_LEVEL;
    public static final LevelProxy LEVEL;
    public static final ServerPlayerProxy SERVER_PLAYER;

    static {
      // ReflectionRemapper loads mappings into memory, which can be quite large, so once we are done with it don't store a ref anywhere so it can be gc'd
      final ReflectionRemapper reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar();
      // ReflectionProxyFactory stores a ref to it's ReflectionRemapper
      final ReflectionProxyFactory reflectionProxyFactory = ReflectionProxyFactory.create(reflectionRemapper, Reflection.class.getClassLoader());

      // proxy instances are safe to hold onto
      SERVER_LEVEL = reflectionProxyFactory.reflectionProxy(ServerLevelProxy.class);
      LEVEL = reflectionProxyFactory.reflectionProxy(LevelProxy.class);
      SERVER_PLAYER = reflectionProxyFactory.reflectionProxy(ServerPlayerProxy.class);
    }
  }

  @Proxies(Level.class)
  private interface LevelProxy {
    @FieldGetter("neighborUpdater")
    NeighborUpdater neighborUpdater(Level instance);

    default void test0() {
      System.out.println("LP 0");
    }

    default void test1() {
      System.out.println("LP 1");
    }
  }

  @Proxies(ServerLevel.class)
  private interface ServerLevelProxy extends LevelProxy {
    BlockPos findLightningTargetAround(ServerLevel instance, BlockPos pos);

    @Override
    default void test1() {
      LevelProxy.super.test1();
      System.out.println("SLP 1");
    }

    default void test2() {
      System.out.println("SLP 2");
    }
  }

  @Proxies(ServerPlayer.class)
  private interface ServerPlayerProxy {
    void createEndPlatform(ServerPlayer instance, ServerLevel world, BlockPos centerPos);
  }

  private static PaperCommandManager<CommandSender> createCommandManager(final JavaPlugin plugin) {
    final PaperCommandManager<CommandSender> manager;
    try {
      manager = PaperCommandManager.createNative(plugin, CommandExecutionCoordinator.simpleCoordinator());
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    manager.registerBrigadier();
    manager.registerAsynchronousCompletions();
    final @Nullable CloudBrigadierManager<CommandSender, ?> brigMgr = manager.brigadierManager();
    if (brigMgr != null) {
      brigMgr.setNativeNumberSuggestions(false);
    }
    new MinecraftExceptionHandler<CommandSender>()
      .withDefaultHandlers()
      .apply(manager, AudienceProvider.nativeAudience());
    return manager;
  }
}
