package de.petropia.copperGolem.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import de.petropia.copperGolem.CopperGolem;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WaitingMusic {
    private int currentSongPlayed;
    private static WaitingMusic instance;
    private final List<File> songList = new ArrayList<>();
    private File currentSong;
    private AudioPlayerManager playerManager;
    private AudioPlayer audioPlayer;

    public WaitingMusic(){
        instance = this;
        try {
            loadFileList();
        } catch (IOException exception){
            exception.printStackTrace();
            return;
        }
        if(songList.size() == 0){
            System.out.println("There are no Songs in the Songs directory. Can't play waiting music!");
            return;
        }
        String waitingChannelID = CopperGolem.getInstance().getProperties().getProperty("SupportWaitingChannel");
        Optional<ServerChannel> channel = CopperGolem.getInstance().getServer().getChannelById(waitingChannelID);
        if(channel.isEmpty()) return;
        if(channel.get().asServerVoiceChannel().isEmpty()) return;
        ServerVoiceChannel voiceChannel = channel.get().asServerVoiceChannel().get();
        voiceChannel.connect().thenAccept(audioConnection -> {
            //Create a player Manager for managing the audio https://javacord.org/wiki/advanced-topics/playing-audio.html#playing-music
            this.playerManager = new DefaultAudioPlayerManager();
            playerManager.registerSourceManager(new LocalAudioSourceManager());
            audioPlayer = playerManager.createPlayer();
            audioPlayer.setVolume(Integer.parseInt(CopperGolem.getInstance().getProperties().getProperty("Volume")));
            audioPlayer.addListener(new CopperAudioEventAdapter()); //Add an Event handler in to repeat songs and so on
            //Creating an audio source and playing it in the audioConnection
            AudioSource source = new CopperLavaPlayerAudioSource(CopperGolem.getInstance().getAPI(), audioPlayer);
            audioConnection.setPrioritySpeaking(true);
            audioConnection.setAudioSource(source);
            audioConnection.setSelfDeafened(false);
            next();
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });

    }

    private synchronized void loadFileList() throws IOException {
        File songsDir = new File("./Songs");
        if(!songsDir.exists()){
            System.out.println("Songs directory not found! Creating new one!");
            if(!songsDir.mkdir()){
                System.out.println("Can't create Songs directory");
                return;
            }
        }
        for(File song : songsDir.listFiles()){
            if(!song.isFile()){
                continue;
            }
            System.out.println("Loaded Song: " + song.getName());
            songList.add(song);
        }
        Collections.shuffle(songList);
    }

    public void next(){
        if(currentSong == null){
            chooseNextSong();
            return;
        }
        int maxPlayed = Integer.parseInt(CopperGolem.getInstance().getProperties().getProperty("RepeatSong"));
        if(currentSongPlayed >= maxPlayed){
            chooseNextSong();
            return;
        }
        repeatLastSong();
    }

    public void repeatLastSong(){
        currentSongPlayed++;
        playerManager.loadItem(currentSong.getAbsolutePath(), new CopperAudioLoadResultHandler(audioPlayer));
    }

    public void chooseNextSong(){
        currentSongPlayed = 0;
        if(songList.size() == 0){
            System.out.println("There are 0 Songs available");
            return;
        }
        if(songList.size() == 1){
            System.out.println("There are only 1 Song available! Won't Shuffle Songs!");
            currentSong = songList.get(0);
            playerManager.loadItem(songList.get(0).getAbsolutePath(), new CopperAudioLoadResultHandler(audioPlayer));
            return;
        }
        List<File> songListClone = new ArrayList<>(songList);
        if(currentSong != null){
            songListClone.remove(currentSong);
        }
        Random random = new Random();
        File song = songListClone.get(random.nextInt(songListClone.size()));
        currentSong = song;
        playerManager.loadItem(song.getAbsolutePath(), new CopperAudioLoadResultHandler(audioPlayer));
    }

    public static WaitingMusic getInstance(){
        return instance;
    }
}
