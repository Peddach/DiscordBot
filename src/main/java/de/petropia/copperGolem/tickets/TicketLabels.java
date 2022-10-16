package de.petropia.copperGolem.tickets;

public enum TicketLabels {

    BUG_REPORT("Bug"),
    PLAYER_REPORT("Spieler melden"),
    OTHER("Sonstiges");

    private final String label;
    TicketLabels(String label){
        this.label = label;
    }

    public String label(){return label;}
}
