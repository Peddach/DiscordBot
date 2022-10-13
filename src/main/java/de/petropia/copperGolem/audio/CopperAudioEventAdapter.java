package de.petropia.copperGolem.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class CopperAudioEventAdapter extends AudioEventAdapter {

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            WaitingMusic.getInstance().next();
            return;
        }
        System.out.println("Track not finished properly! Reason: " + endReason);
        WaitingMusic.getInstance().chooseNextSong();
    }
}
