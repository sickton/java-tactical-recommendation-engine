package com.sickton.jgaffer.domain;

/**
 * Enumerates standard football formations.
 *
 * <p>Reserved for future use — planned to link tactical recommendations
 * to specific formation structures.</p>
 *
 * @author sickton
 */
public enum Formation {
    F_4_3_3("4-3-3"),
    F_3_4_3("3-4-3"),
    F_4_2_3_1("4-2-3-1"),
    F_3_5_2("3-5-2"),
    F_5_3_2("5-3-2");

    private final String label;

    Formation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
