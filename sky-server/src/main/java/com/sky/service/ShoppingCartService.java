package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void ShoppingCardadd(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> sclist();

    void shoppingCardclean();

    void shoppingCardsub(ShoppingCartDTO shoppingCartDTO);
}
