package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderResponseListener {
    private final BeerOrderManager beerOrderManager;
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    void getValidateOrderResult(ValidateOrderResult validateOrderResult){
        UUID orderId = validateOrderResult.getId();
        Boolean isValid = validateOrderResult.getIsValid();
        beerOrderManager.sendValidationResult(orderId, isValid);
    }
}
