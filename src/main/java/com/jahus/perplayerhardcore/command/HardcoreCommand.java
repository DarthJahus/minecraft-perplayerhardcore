package com.jahus.perplayerhardcore.command;

import com.jahus.perplayerhardcore.data.HardcoreAttachments;
import com.jahus.perplayerhardcore.data.HardcoreData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class HardcoreCommand {

    private static MutableComponent prefix() {
        return Component.translatable("perplayerhardcore.prefix").withStyle(ChatFormatting.RED);
    }

    private static ChatFormatting colorForLives(int remaining) {
        if (remaining <= 1) return ChatFormatting.RED;
        if (remaining == 2) return ChatFormatting.GOLD;
        return ChatFormatting.GREEN;
    }

    private static Component livesWord(int n) {
        return n == 1
                ? Component.translatable("perplayerhardcore.life")
                : Component.translatable("perplayerhardcore.lives");
    }

    private static Component modeDescription(HardcoreData.Mode mode) {
        return mode == HardcoreData.Mode.SPECTATOR
                ? Component.translatable("perplayerhardcore.mode.spectator")
                : Component.translatable("perplayerhardcore.mode.worldspawn");
    }

    private static void notifyPlayer(ServerPlayer player, MutableComponent message) {
        player.sendSystemMessage(prefix().append(message));
    }

    private static void notifyAdmin(CommandSourceStack source, MutableComponent message) {
        source.sendSuccess(() -> prefix().append(message), true);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(
            Commands.literal("hardcore")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("worldspawn");
                                builder.suggest("spectator");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> start(ctx, 1))
                            .then(Commands.argument("lives", IntegerArgumentType.integer(1))
                                .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "lives")))
                            )
                        )
                    )
                )
                .then(Commands.literal("stop")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(HardcoreCommand::stop)
                    )
                )
                .then(Commands.literal("setspawn")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(HardcoreCommand::setSpawn)
                        )
                    )
                )
                .then(Commands.literal("lives")
                    .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> livesAdd(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                            )
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> livesRemove(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                            )
                        )
                    )
                )
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(HardcoreCommand::get)
                    )
                )
                .then(Commands.literal("list")
                    .executes(HardcoreCommand::list)
                )
        );
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int lives) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String modeStr = StringArgumentType.getString(ctx, "mode");
        HardcoreData.Mode mode = HardcoreData.Mode.fromString(modeStr);

        if (HardcoreAttachments.isTracked(player)) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.already_tracked").withStyle(ChatFormatting.RED)));
            return 0;
        }

        HardcoreData data = new HardcoreData(mode, lives);
        HardcoreAttachments.set(player, data);

        // Player: "Hardcore mode started! 2 lives. Spectator at game over."
        notifyPlayer(player, Component.translatable(
                "perplayerhardcore.start.player",
                lives,
                livesWord(lives),
                modeDescription(mode)
        ).withStyle(colorForLives(lives)));

        // Admin (includes setspawn tip)
        notifyAdmin(ctx.getSource(), Component.translatable(
                "perplayerhardcore.start.admin",
                player.getName().getString(),
                lives + " " + (lives == 1 ? "life" : "lives") + ". " + modeDescription(mode).getString()
        ).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        if (!HardcoreAttachments.isTracked(player)) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.not_tracked").withStyle(ChatFormatting.RED)));
            return 0;
        }

        HardcoreAttachments.remove(player);

        notifyPlayer(player, Component.translatable("perplayerhardcore.stop.player").withStyle(ChatFormatting.GREEN));
        notifyAdmin(ctx.getSource(), Component.translatable("perplayerhardcore.stop.admin", player.getName().getString()).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int setSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.not_tracked").withStyle(ChatFormatting.RED)));
            return 0;
        }

        data.setHardcoreSpawn(pos);
        HardcoreAttachments.set(player, data);

        String posStr = pos.getX() + " " + pos.getY() + " " + pos.getZ();

        notifyPlayer(player, Component.translatable("perplayerhardcore.setspawn.player", posStr).withStyle(ChatFormatting.GREEN));
        notifyAdmin(ctx.getSource(), Component.translatable("perplayerhardcore.setspawn.admin", player.getName().getString(), posStr).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int livesAdd(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.not_tracked").withStyle(ChatFormatting.RED)));
            return 0;
        }

        data.addLives(amount);
        HardcoreAttachments.set(player, data);

        int rem = data.getRemainingLives();
        int total = data.getTotalGranted();
        Component word = livesWord(amount);

        notifyPlayer(player, Component.translatable(
                "perplayerhardcore.lives_added.player",
                amount, word, total, rem
        ).withStyle(colorForLives(rem)));

        notifyAdmin(ctx.getSource(), Component.translatable(
                "perplayerhardcore.lives_added.admin",
                player.getName().getString(), amount, word, total, rem
        ).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int livesRemove(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.not_tracked").withStyle(ChatFormatting.RED)));
            return 0;
        }

        if (data.getRemainingLives() <= 0) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.lives_remove_none").withStyle(ChatFormatting.RED)));
            return 0;
        }

        data.removeLives(amount);
        HardcoreAttachments.set(player, data);

        int rem = data.getRemainingLives();
        int total = data.getTotalGranted();
        Component word = livesWord(amount);

        notifyPlayer(player, Component.translatable(
                "perplayerhardcore.lives_removed.player",
                amount, word, total, rem
        ).withStyle(colorForLives(rem)));

        notifyAdmin(ctx.getSource(), Component.translatable(
                "perplayerhardcore.lives_removed.admin",
                player.getName().getString(), amount, word, total, rem
        ).withStyle(ChatFormatting.YELLOW));

        return 1;
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.translatable("perplayerhardcore.not_tracked").withStyle(ChatFormatting.RED)));
            return 0;
        }

        String spawnStr = data.getHardcoreSpawn() != null
                ? data.getHardcoreSpawn().getX() + " " + data.getHardcoreSpawn().getY() + " " + data.getHardcoreSpawn().getZ()
                : "world spawn (not set)";

        ctx.getSource().sendSuccess(() -> prefix().append(Component.translatable(
                "perplayerhardcore.get",
                player.getName().getString(),
                data.getMode().toString(),
                data.getInitialLives(),
                data.getBonusLives(),
                data.getTotalGranted(),
                data.getRemainingLives(),
                spawnStr
        ).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        MutableComponent result = Component.translatable("perplayerhardcore.list.header").append("\n");

        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HardcoreData data = HardcoreAttachments.get(player);
            if (data != null) {
                count++;
                result.append(Component.translatable(
                        "perplayerhardcore.list.entry",
                        player.getName().getString(),
                        data.getMode().toString(),
                        data.getInitialLives(),
                        data.getBonusLives(),
                        data.getRemainingLives()
                )).append("\n");
            }
        }

        if (count == 0) {
            result.append(Component.translatable("perplayerhardcore.list.none"));
        }

        MutableComponent finalResult = result;
        ctx.getSource().sendSuccess(() -> prefix().append(finalResult.withStyle(ChatFormatting.AQUA)), false);
        return count;
    }
}
