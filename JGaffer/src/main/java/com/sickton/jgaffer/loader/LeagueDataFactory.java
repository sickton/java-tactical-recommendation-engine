package com.sickton.jgaffer.loader;

import com.sickton.jgaffer.domain.Team;
import com.sickton.jgaffer.utility.ApplicationParser;
import com.sickton.jgaffer.utility.FileStorage;

import java.util.Map;

/**
 * Factory class for constructing active domain objects from CSV data.
 *
 * <p>The current product flow only needs {@link Team} objects for club selection
 * and puzzle graph generation.</p>
 *
 * @see Team
 * @see ApplicationParser
 */
public class LeagueDataFactory {

    /**
     * Builds a Team domain object from the given team name and parsed squad data.
     *
     * <p>Uses PCA-derived weights when present and otherwise falls back to the
     * team's default constructor path.</p>
     */
    public static Team buildTeamFromName(String name, Map<String, FileStorage> teamData) {
        FileStorage file = teamData.get(name);
        if (file == null) {
            return null;
        }

        return file.hasCustomWeights()
                ? new Team(
                        file.getSquadData(),
                        file.getStaminaData(),
                        file.getAdaptabilityData(),
                        file.getAtkWeight(),
                        file.getDefWeight(),
                        file.getCtrlWeight()
                )
                : new Team(file.getSquadData(), file.getStaminaData(), file.getAdaptabilityData());
    }
}
