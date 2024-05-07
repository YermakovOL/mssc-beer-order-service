package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Created by jt on 11/29/19.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor beerOrderStateInterceptor;
    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void sendValidationResult(UUID orderId, Boolean isValid) {
        BeerOrder referenceById = beerOrderRepository.getReferenceById(orderId);

        if(isValid){
            sendBeerOrderEvent(referenceById,BeerOrderEventEnum.VALIDATION_PASSED);

            BeerOrder updatedOrder = beerOrderRepository.getReferenceById(orderId);

            sendBeerOrderEvent(updatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
        }
        else sendBeerOrderEvent(referenceById,BeerOrderEventEnum.VALIDATION_FAILED);
    }

    @Override
    public void sendAllocationResult(BeerOrderDto beerOrderDto, Boolean inventoryPending, Boolean allocationError) {
        BeerOrder beerOrder = beerOrderRepository.getReferenceById(beerOrderDto.getId());
        if(inventoryPending) sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
        else if(allocationError) sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.ALLOCATION_FAILED);
        else sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);

        updateAllocatedQty(beerOrderDto, beerOrder);
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
        Map<UUID, BeerOrderLineDto> dtoMap = beerOrderDto.getBeerOrderLines()
                .stream()
                .collect(Collectors.toMap(BeerOrderLineDto::getBeerId, Function.identity()));

        beerOrder.getBeerOrderLines().stream()
                .filter(beerOrderLine -> dtoMap.containsKey(beerOrderLine.getBeerId()))
                .forEach(beerOrderLine -> {
                    BeerOrderLineDto dto = dtoMap.get(beerOrderLine.getBeerId());
                    beerOrderLine.setQuantityAllocated(dto.getQuantityAllocated());
                });
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum){
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);

        Message msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();

        sm.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder){
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                    sma.addStateMachineInterceptor(beerOrderStateInterceptor);
                });

        sm.start();

        return sm;
    }
}
