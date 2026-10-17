package net.jahus.perplayerhardcore.command;

import net.jahus.perplayerhardcore.config.ModConfig;
import net.jahus.perplayerhardcore.data.HardcoreAttachments;
import net.jahus.perplayerhardcore.data.HardcoreData;
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
        return Component.literal(ModConfig.prefix).withStyle(ChatFormatting.RED);
    }

    private static ChatFormatting colorForLives(int remaining) {
        if (remaining <= 1) return ChatFormatting.RED;
        if (remaining == 2) return ChatFormatting.GOLD;
        return ChatFormatting.GREEN;
    }

    private static String livesWord(int n) {
        return n == 1 ? ModConfig.life : ModConfig.lives;
    }

    private static String modeDescription(HardcoreData.Mode mode) {
        return mode == HardcoreData.Mode.SPECTATOR ? ModConfig.modeSpectator : ModConfig.modeWorldspawn;
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
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.alreadyTracked).withStyle(ChatFormatting.RED)));
            return 0;
        }

        HardcoreData data = new HardcoreData(mode, lives);
        HardcoreAttachments.set(player, data);

        String word = livesWord(lives);
        String modeDesc = modeDescription(mode);

        notifyPlayer(player, Component.literal(String.format(ModConfig.startPlayer, lives, word, modeDesc)).withStyle(colorForLives(lives)));
        notifyAdmin(ctx.getSource(), Component.literal(String.format(ModConfig.startAdmin, player.getName().getString(), lives, word, modeDesc)).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

        if (!HardcoreAttachments.isTracked(player)) {
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.notTracked).withStyle(ChatFormatting.RED)));
            return 0;
        }

        HardcoreAttachments.remove(player);

        notifyPlayer(player, Component.literal(ModConfig.stopPlayer).withStyle(ChatFormatting.GREEN));
        notifyAdmin(ctx.getSource(), Component.literal(String.format(ModConfig.stopAdmin, player.getName().getString())).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int setSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.notTracked).withStyle(ChatFormatting.RED)));
            return 0;
        }

        data.setHardcoreSpawn(pos);
        HardcoreAttachments.set(player, data);

        String posStr = pos.getX() + " " + pos.getY() + " " + pos.getZ();

        notifyPlayer(player, Component.literal(String.format(ModConfig.setspawnPlayer, posStr)).withStyle(ChatFormatting.GREEN));
        notifyAdmin(ctx.getSource(), Component.literal(String.format(ModConfig.setspawnAdmin, player.getName().getString(), posStr)).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int livesAdd(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.notTracked).withStyle(ChatFormatting.RED)));
            return 0;
        }

        data.addLives(amount);
        HardcoreAttachments.set(player, data);

        int rem = data.getRemainingLives();
        int total = data.getTotalGranted();
        String word = livesWord(amount);

        notifyPlayer(player, Component.literal(String.format(ModConfig.livesAddedPlayer, amount, word, total, rem)).withStyle(colorForLives(rem)));
        notifyAdmin(ctx.getSource(), Component.literal(String.format(ModConfig.livesAddedAdmin, player.getName().getString(), amount, word, total, rem)).withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int livesRemove(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.notTracked).withStyle(ChatFormatting.RED)));
            return 0;
        }

        if (data.getRemainingLives() <= 0) {
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.livesRemoveNone).withStyle(ChatFormatting.RED)));
            return 0;
        }

        data.removeLives(amount);
        HardcoreAttachments.set(player, data);

        int rem = data.getRemainingLives();
        int total = data.getTotalGranted();
        String word = livesWord(amount);

        notifyPlayer(player, Component.literal(String.format(ModConfig.livesRemovedPlayer, amount, word, total, rem)).withStyle(colorForLives(rem)));
        notifyAdmin(ctx.getSource(), Component.literal(String.format(ModConfig.livesRemovedAdmin, player.getName().getString(), amount, word, total, rem)).withStyle(ChatFormatting.YELLOW));

        return 1;
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        HardcoreData data = HardcoreAttachments.get(player);
        if (data == null) {
            ctx.getSource().sendFailure(prefix().append(Component.literal(ModConfig.notTracked).withStyle(ChatFormatting.RED)));
            return 0;
        }

        String spawnStr = data.getHardcoreSpawn() != null
                ? data.getHardcoreSpawn().getX() + " " + data.getHardcoreSpawn().getY() + " " + data.getHardcoreSpawn().getZ()
                : "world spawn (not set)";

        String msg = String.format(ModConfig.get,
                player.getName().getString(),
                data.getMode().toString(),
                data.getInitialLives(),
                data.getBonusLives(),
                data.getTotalGranted(),
                data.getRemainingLives(),
                spawnStr);

        ctx.getSource().sendSuccess(() -> prefix().append(Component.literal(msg).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        StringBuilder sb = new StringBuilder(ModConfig.listHeader).append("\n");

        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HardcoreData data = HardcoreAttachments.get(player);
            if (data != null) {
                count++;
                sb.append(String.format(ModConfig.listEntry,
                        player.getName().getString(),
                        data.getMode().toString(),
                        data.getInitialLives(),
                        data.getBonusLives(),
                        data.getRemainingLives())).append("\n");
            }
        }

        if (count == 0) {
            sb.append(ModConfig.listNone);
        }

        ctx.getSource().sendSuccess(() -> prefix().append(Component.literal(sb.toString().trim()).withStyle(ChatFormatting.AQUA)), false);
        return count;
    }
}
