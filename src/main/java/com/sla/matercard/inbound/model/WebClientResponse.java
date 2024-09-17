package com.sla.matercard.inbound.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author samwel.wafula
 * Created on 18/06/2024
 * Time 10:55
 * Project Transfast
 */
@Getter
@Setter
public class WebClientResponse {
    private JsonNode jsonNode;
    private int statusCode;
}
