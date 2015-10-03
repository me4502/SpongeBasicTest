package com.me4502.sexystuff.modules;

import com.me4502.modularframework.module.Module;
import com.me4502.sexystuff.SexyStuff;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Set;

@Module(moduleName = "MagicCarpet", onEnable = "onInit")
public class MagicCarpet {

    private Set<Player> enabledPlayers = new HashSet<>();

    public void onInit() {

        CommandSpec myCommandSpec = CommandSpec.builder()
                .description(Texts.of("Magic Carpet Command"))
                .executor(new MagicCarpetExecutor())
                .build();

        SexyStuff.game.getCommandDispatcher().register(SexyStuff.plugin, myCommandSpec, "magiccarpet", "mc", "carpet");
    }

    public void onDisable() {
        for(Player player : enabledPlayers) {

            Location<World> oldBase = player.getLocation().getRelative(Direction.DOWN);

            for(int x = -1; x < 2; x++)
                for(int z = -1; z < 2; z++)
                    for(int y = -1; y < 1; y++)
                        if(oldBase.add(x,y,z).getBlockType() == BlockTypes.BARRIER)
                            oldBase.add(x,y,z).setBlockType(BlockTypes.AIR);
        }
    }

    @Listener
    public void onPlayerMove(DisplaceEntityEvent.Move.TargetPlayer event) {
        if(!enabledPlayers.contains(event.getTargetEntity()))
            return;

        Location<World> oldBase = event.getFromTransform().getLocation().getRelative(Direction.DOWN);

        for(int x = -1; x < 2; x++)
            for(int z = -1; z < 2; z++)
                for(int y = -1; y < 1; y++)
                    if(oldBase.add(x,y,z).getBlockType() == BlockTypes.BARRIER)
                        oldBase.add(x,y,z).setBlockType(BlockTypes.AIR);

        Location<World> base = event.getToTransform().getLocation().getRelative(Direction.DOWN);

        if(event.getTargetEntity().get(Keys.IS_SNEAKING).get())
            base = base.getRelative(Direction.DOWN);

        for(int x = -1; x < 2; x++)
            for(int z = -1; z < 2; z++)
                if(base.add(x,0,z).getBlockType() == BlockTypes.AIR)
                    base.add(x,0,z).setBlockType(BlockTypes.BARRIER);
    }

    private class MagicCarpetExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if(src instanceof Player) {
                if(enabledPlayers.contains(src)) {
                    enabledPlayers.remove(src);

                    Location<World> oldBase = ((Player)src).getLocation().getRelative(Direction.DOWN);

                    for(int x = -1; x < 2; x++)
                        for(int z = -1; z < 2; z++)
                            for(int y = -1; y < 1; y++)
                                if(oldBase.add(x,y,z).getBlockType() == BlockTypes.BARRIER)
                                    oldBase.add(x,y,z).setBlockType(BlockTypes.AIR);
                } else
                    enabledPlayers.add((Player) src);

                src.sendMessage(Texts.of(TextColors.YELLOW, "MagicCarpet state: ", (enabledPlayers.contains(src) ? (TextColors.GREEN) : (TextColors.RED )), (enabledPlayers.contains(src) ? ("On") : ("Off"))));
            } else
                src.sendMessage(Texts.of("This command can only be performed by a player!"));
            return CommandResult.empty();
        }
    }
}
