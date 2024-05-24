package com.project.inventoryservice.controller;

import com.project.inventoryservice.dto.InventoryResponse;
import com.project.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode){ //We Are taking list of String as request parameter and passing skuCode
        // list of string into isInStock method and querying repository to
        // find out all inventory obj for given skuCode and maping inventory objs to the InvenResp obj and sending list of Inventry resp obj as resp

        return inventoryService.isInStock(skuCode);
    }
}
