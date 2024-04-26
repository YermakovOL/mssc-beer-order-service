package guru.sfg.beer.order.service.web.model;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.Builder;

@Builder
public class ValidateOrderRequest {
    private BeerOrderDto beerOrderDto;
}
