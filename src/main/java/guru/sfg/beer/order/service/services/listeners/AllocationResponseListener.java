package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocationOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
@RequiredArgsConstructor
@Component
public class AllocationResponseListener {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderManager beerOrderManager;
    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void getAllocationResponse(AllocationOrderResponse allocationOrderResponse){
        Boolean allocationError = allocationOrderResponse.getAllocationError();
        Boolean allocationPending = allocationOrderResponse.getAllocationPending();
        BeerOrderDto beerOrderDto = allocationOrderResponse.getBeerOrderDto();
        beerOrderManager.sendAllocationResult(beerOrderDto, allocationPending, allocationError);
    }
}
