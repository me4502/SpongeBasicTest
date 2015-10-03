package com.me4502.sexystuff.modules;

import com.me4502.modularframework.module.Module;
import com.me4502.sexystuff.SexyStuff;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.args.GenericArguments;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Module(moduleName = "MidiSequencer", onEnable = "onInit", onDisable = "onDisable")
public class MidiSequencer {

    private static final int[] instruments = {
            0, 0, 0, 0, 0, 0, 0, 5, // 8
            6, 0, 0, 0, 0, 0, 0, 0, // 16
            0, 0, 0, 0, 0, 0, 0, 5, // 24
            5, 5, 5, 5, 5, 5, 5, 5, // 32
            6, 6, 6, 6, 6, 6, 6, 6, // 40
            5, 5, 5, 5, 5, 5, 5, 2, // 48
            5, 5, 5, 5, 0, 0, 0, 0, // 56
            0, 0, 0, 0, 0, 0, 0, 0, // 64
            0, 0, 0, 0, 0, 0, 0, 0, // 72
            0, 0, 0, 0, 0, 0, 0, 0, // 80
            0, 0, 0, 0, 0, 0, 0, 0, // 88
            0, 0, 0, 0, 0, 0, 0, 0, // 96
            0, 0, 0, 0, 0, 0, 0, 0, // 104
            0, 0, 0, 0, 0, 0, 0, 0, // 112
            1, 1, 1, 3, 1, 1, 1, 5, // 120
            1, 1, 1, 1, 1, 2, 4, 3, // 128
    };


    private static final int[] percussion = {
            3, 3, 4, 4, 3, 2, 3, 2, //8 - Electric Snare
            2, 2, 2, 2, 2, 2, 2, 2, //16 - Hi Mid Tom
            3, 2, 3, 3, 3, 0, 3, 3, //24 - Cowbell
            3, 3, 3, 2, 2, 3, 3, 3, //32 - Low Conga
            2, 2, 0, 0, 2, 2, 0, 0, //40 - Long Whistle
            3, 3, 3, 3, 3, 3, 5, 5, //48 - Open Cuica
            3, 3,                   //50 - Open Triangle
    };

    public void onInit() {
        CommandSpec myCommandSpec = CommandSpec.builder()
                .description(Texts.of("Jukebox Command"))
                .arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("midi"))))
                .executor(new JukeboxExecutor())
                .build();

        SexyStuff.game.getCommandDispatcher().register(SexyStuff.plugin, myCommandSpec, "jukebox", "music", "audio");
    }

    public void onDisable() {

    }

    private class JukeboxExecutor implements CommandExecutor {

        private Map<Player, MidiPlayer> midiPlayers = new HashMap<>();

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if(src instanceof Player) {
                String midiName = (String) args.getOne("midi").get();

                if(midiName.equals("list")) {
                    List<String> midis = new ArrayList<>();

                    File file = new File("music/");
                    if(file.exists()) {
                        for (File f : file.listFiles()) {
                            if (f.getName().endsWith(".mid") || f.getName().endsWith("midi"))
                                midis.add(f.getName());
                        }
                    }

                    src.sendMessage(Texts.of(TextColors.YELLOW, StringUtils.join(midis, ", ")));
                } else if(midiName.equals("stop")) {
                    if (midiPlayers.containsKey(src))
                        midiPlayers.get(src).stop();
                } else {

                    File file = new File("music/" + midiName);
                    if (!file.exists())
                        file = new File("music/" + midiName + ".mid");
                    if (!file.exists())
                        file = new File("music/" + midiName + ".midi");

                    try {
                        if (midiPlayers.containsKey(src))
                            midiPlayers.get(src).stop();
                        MidiPlayer player = new MidiPlayer((Player) src, file);
                        midiPlayers.put((Player) src, player);
                    } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {
                        src.sendMessage(Texts.of(TextColors.RED, "Unknown Song!"));
                        e.printStackTrace();
                    }
                }
            } else
                src.sendMessage(Texts.of("This command can only be performed by a player!"));
            return CommandResult.empty();
        }
    }


    private class MidiPlayer {

        Player player;
        Sequencer sequencer;

        final Map<Integer, Integer> patches = new HashMap<>();

        public MidiPlayer(Player player, File midiFile) throws MidiUnavailableException, InvalidMidiDataException, IOException {
            this.player = player;

            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            Sequence seq = MidiSystem.getSequence(midiFile);
            sequencer.setSequence(seq);

            sequencer.getTransmitter().setReceiver(new Receiver() {
                @Override
                public void send(MidiMessage message, long timeStamp) {
                    if ((message.getStatus() & 0xF0) == ShortMessage.PROGRAM_CHANGE) {

                        ShortMessage msg = (ShortMessage) message;
                        int chan = msg.getChannel();
                        int patch = msg.getData1();
                        patches.put(chan, patch);
                    } else if ((message.getStatus() & 0xF0) == ShortMessage.NOTE_ON) {

                        ShortMessage msg = (ShortMessage) message;
                        int chan = msg.getChannel();
                        int n = msg.getData1();

                        Note note;

                        if (chan == 9) { // Percussion
                            // Sounds like utter crap
                            note = new Note(toMCSound(toMCPercussion(patches.get(chan))), toMCNote(n),  10 * (msg.getData2() / 255f));
                        } else {
                            note = new Note(toMCSound(toMCInstrument(patches.get(chan))), toMCNote(n), 10 * (msg.getData2() / 127f));
                        }

                        player.playSound(toSound(note.getInstrument()), player.getLocation().getPosition(), note.getVelocity(), note.getNote());
                    }
                }

                @Override
                public void close() {
                    //ded
                }
            });

            sequencer.start();
        }

        public void stop() {
            if(sequencer.isOpen())
                sequencer.close();
        }
    }

    public class Note {

        Instrument instrument;
        byte note;
        float velocity;

        public Note(Instrument instrument, byte note, float velocity) {

            this.instrument = instrument;
            this.note = note;
            this.velocity = velocity;
        }

        public Instrument getInstrument() {

            return instrument;
        }

        public float getNote() {

            return (float) Math.pow(2.0D, (note - 12) / 12.0D);
        }

        public float getVelocity() {

            return velocity;
        }
    }

    public enum Instrument {

        GUITAR, PIANO, BASS, BASS_GUITAR, STICKS, BASS_DRUM, SNARE_DRUM;
    }

    protected static byte toMCNote(int n) {

        if (n < 54) return (byte) ((n - 6) % (18 - 6));
        else if (n > 78) return (byte) ((n - 6) % (18 - 6) + 12);
        else return (byte) (n - 54);
    }

    protected static byte toMCInstrument(Integer patch) {

        if (patch == null) return 0;

        if (patch < 0 || patch >= instruments.length) return 0;

        return (byte) instruments[patch];
    }

    protected Instrument toMCSound(byte instrument) {

        switch (instrument) {
            case 1:
                return Instrument.BASS_GUITAR;
            case 2:
                return Instrument.SNARE_DRUM;
            case 3:
                return Instrument.STICKS;
            case 4:
                return Instrument.BASS_DRUM;
            case 5:
                return Instrument.GUITAR;
            case 6:
                return Instrument.BASS;
            default:
                return Instrument.PIANO;
        }
    }

    protected static byte toMCPercussion(Integer patch) {

        if(patch == null)
            return 0;

        int i = patch - 33;
        if (i < 0 || i >= percussion.length) {
            return 1;
        }

        return (byte) percussion[i];
    }

    public SoundType toSound(Instrument instrument) {

        switch(instrument) {
            case PIANO:
                return SoundTypes.NOTE_PIANO;
            case GUITAR:
                return SoundTypes.NOTE_PLING;
            case BASS:
                return SoundTypes.NOTE_BASS;
            case BASS_GUITAR:
                return SoundTypes.NOTE_BASS_GUITAR;
            case STICKS:
                return SoundTypes.NOTE_STICKS;
            case BASS_DRUM:
                return SoundTypes.NOTE_BASS_DRUM;
            case SNARE_DRUM:
                return SoundTypes.NOTE_SNARE_DRUM;
            default:
                return SoundTypes.NOTE_PIANO;
        }
    }
}
