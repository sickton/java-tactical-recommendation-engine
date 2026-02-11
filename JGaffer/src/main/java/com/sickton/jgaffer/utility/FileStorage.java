package com.sickton.jgaffer.utility;

import com.sickton.jgaffer.domain.Squad;
import com.sickton.jgaffer.domain.StaminaLevel;
import com.sickton.jgaffer.domain.TeamAdaptability;

public class FileStorage {
    private final Squad squadData;
    private final TeamAdaptability adaptabilityData;
    private final StaminaLevel staminaData;

    public FileStorage(Squad s, TeamAdaptability a, StaminaLevel st ) {
        this.squadData = s;
        this.adaptabilityData = a;
        this.staminaData = st;
    }

    public Squad getSquadData() {
        return squadData;
    }

    public TeamAdaptability getAdaptabilityData() {
        return adaptabilityData;
    }

    public StaminaLevel getStaminaData() {
        return staminaData;
    }
}
