package de.petropia.copperGolem.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class CopperAudioLoadResultHandler implements AudioLoadResultHandler {

    private final AudioPlayer player;

    public CopperAudioLoadResultHandler(AudioPlayer player){
        this.player = player;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        player.playTrack(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        for (AudioTrack track : playlist.getTracks()) {
            player.playTrack(track);
        }
    }

    @Override
    public void noMatches() {
        System.out.println("No Matches Found");
        WaitingMusic.getInstance().chooseNextSong();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        exception.printStackTrace();
        System.out.println("Load failed!");
    }
}
