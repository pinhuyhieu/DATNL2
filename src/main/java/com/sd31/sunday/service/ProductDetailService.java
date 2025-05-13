package com.sd31.sunday.service;

import com.sd31.sunday.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductDetailService {

    @Autowired
    private SanPhamRepository sanPhamRepository;


}