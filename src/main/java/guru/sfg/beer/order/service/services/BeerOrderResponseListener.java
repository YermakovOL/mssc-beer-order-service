package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderResponseListener {
    private final BeerOrderManager beerOrderManager;
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE)
    void getValidateOrderResult(ValidateOrderResult validateOrderResult){
        UUID orderId = validateOrderResult.getId();
        Boolean isValid = validateOrderResult.getIsValid();
        beerOrderManager.sendValidationResult(orderId, isValid);
    }
}
