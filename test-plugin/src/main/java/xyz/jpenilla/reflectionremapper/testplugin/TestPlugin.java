/*
 * reflection-remapper
 *
 * Copyright (c) 2021 Jason Penilla
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
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MCUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
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
      .handler(ctx -> {
        final ServerPlayer serverPlayer = ((CraftPlayer) ctx.getSender()).getHandle();
        Reflection.SERVER_PLAYER.createEndPlatform(serverPlayer, serverPlayer.getLevel(), new BlockPos(serverPlayer.getBlockX(), serverPlayer.getBlockY(), serverPlayer.getBlockZ()));
      });
    manager.command(createEndPlatform);

    final Command.Builder<CommandSender> strikeLightning = manager.commandBuilder("strike_lightning")
      .senderType(Player.class)
      .handler(ctx -> {
        final ServerPlayer serverPlayer = ((CraftPlayer) ctx.getSender()).getHandle();
        final BlockPos lightningTarget = Reflection.SERVER_LEVEL.findLightningTargetAround(
          serverPlayer.getLevel(),
          new BlockPos(serverPlayer.getBlockX(), serverPlayer.getBlockY(), serverPlayer.getBlockZ())
        );
        ((Player) ctx.getSender()).getWorld().strikeLightning(MCUtil.toLocation(serverPlayer.getLevel(), lightningTarget));
      });
    manager.command(strikeLightning);
  }

  public static final class Reflection {
    public static final ServerLevelProxy SERVER_LEVEL;
    public static final ServerPlayerProxy SERVER_PLAYER;

    static {
      // ReflectionRemapper loads mappings into memory, which can be quite large, so once we are done with it don't store a ref anywhere so it can be gc'd
      final ReflectionRemapper reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar();
      // ReflectionProxyFactory stores a ref to it's ReflectionRemapper
      final ReflectionProxyFactory reflectionProxyFactory = ReflectionProxyFactory.create(reflectionRemapper, Reflection.class.getClassLoader());

      // proxy instances are safe to hold onto
      SERVER_LEVEL = reflectionProxyFactory.reflectionProxy(ServerLevelProxy.class);
      SERVER_PLAYER = reflectionProxyFactory.reflectionProxy(ServerPlayerProxy.class);
    }
  }

  @Proxies(ServerLevel.class)
  private interface ServerLevelProxy {
    BlockPos findLightningTargetAround(ServerLevel instance, BlockPos pos);
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
