package com.sla.matercard.inbound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * @author samwel.wafula
 * Created on 27/06/2024
 * Time 09:51
 * Project Transfast
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Invoices {

    @JsonProperty("Invoices")
    private List<Invoice> invoices;
}
