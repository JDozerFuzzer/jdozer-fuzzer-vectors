/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors.storage.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Vector {

    private Integer id;
    private String description;
    private String script;
    private String type;
    private String owaspCategory;
    private String subcategory;
    private String context;
    private String tags;
    private String technique;
    private String browserSpecific;

}
