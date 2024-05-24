package com.project.orderservice.service;

import com.project.orderservice.dto.InventoryResponse;
import com.project.orderservice.dto.OrderLineItemsDto;
import com.project.orderservice.dto.OrderRequest;
import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderLineItems;
import com.project.orderservice.event.OrderPlaceEvent;
import com.project.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
   @Autowired
    private WebClient.Builder webClientBuilder;

   @Autowired
   private KafkaTemplate<String,OrderPlaceEvent> kafkaTemplate;
    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        //collect all suCode from OrderObj
        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        //cal inventoryservice and place order if product is in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get().uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                        .retrieve() //to retreive response
                                .bodyToMono(InventoryResponse[].class)//webclient will parse the response to an array of inventory response objects and will provide it as result
                // to able to read response from webclient response
                .block() ;//to make synchronus req to http8082

       boolean allProductsInArray =  Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock); //we got this list of invenResPArray we are converting it into a stream and calling allmatch method it will check whether isInStock var is true inside array or not
        if(allProductsInArray){
            kafkaTemplate.send("notificationTopic",new OrderPlaceEvent( order.getOrderNumber()));
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Product is not in stock pleasetry again later");
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
