package com.sickton.jgaffer.utility;

import com.sickton.jgaffer.domain.Squad;
import com.sickton.jgaffer.domain.StaminaLevel;
import com.sickton.jgaffer.domain.TeamAdaptability;

/**
 * Intermediate data holder for squad information parsed from CSV.
 *
 * <p>Bundles a {@link Squad}, {@link TeamAdaptability}, and {@link StaminaLevel}
 * together as they are read from the SquadInformation CSV file. Used by
 * {@link ApplicationParser} and {@link com.sickton.jgaffer.demoUI.PremierLeagueFactory}
 * to construct {@link com.sickton.jgaffer.domain.Team} objects.</p>
 *
 * @see ApplicationParser#parseSquadInformation()
 * @author sickton
 */
public class FileStorage {
    private final Squad squadData;
    private final TeamAdaptability adaptabilityData;
    private final StaminaLevel staminaData;

    /**
     * Constructs a new {@code FileStorage} bundling squad, adaptability, and stamina data.
     *
     * @param s  the squad data parsed from CSV
     * @param a  the team adaptability level
     * @param st the team stamina level
     */
    public FileStorage(Squad s, TeamAdaptability a, StaminaLevel st ) {
        this.squadData = s;
        this.adaptabilityData = a;
        this.staminaData = st;
    }

    /**
     * Returns the squad data for this entry.
     *
     * @return the {@link Squad} containing name, manager, and style information
     */
    public Squad getSquadData() {
        return squadData;
    }

    /**
     * Returns the team adaptability level for this entry.
     *
     * @return the {@link TeamAdaptability} level
     */
    public TeamAdaptability getAdaptabilityData() {
        return adaptabilityData;
    }

    /**
     * Returns the team stamina level for this entry.
     *
     * @return the {@link StaminaLevel} of the team
     */
    public StaminaLevel getStaminaData() {
        return staminaData;
    }
}
