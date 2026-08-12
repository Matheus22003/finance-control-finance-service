package com.financecontrol.finance.domain;

import java.util.List;

public final class Category {

    public static final String FOOD = "FOOD";
    public static final String TRANSPORT = "TRANSPORT";
    public static final String RENT = "RENT";
    public static final String LEISURE = "LEISURE";
    public static final String HEALTH = "HEALTH";
    public static final String OTHER = "OTHER";

    private static final List<DefaultCategory> DEFAULTS = List.of(
            new DefaultCategory(FOOD, "Alimentação"),
            new DefaultCategory(TRANSPORT, "Transporte"),
            new DefaultCategory(RENT, "Moradia"),
            new DefaultCategory(LEISURE, "Lazer"),
            new DefaultCategory(HEALTH, "Saúde"),
            new DefaultCategory(OTHER, "Outros"));

    private Category() {
    }

    public static List<DefaultCategory> defaults() {
        return DEFAULTS;
    }

    public record DefaultCategory(String code, String name) {
    }
}
