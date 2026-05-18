package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.WalletEntity;
import com.nomnom.backend.model.WalletDetailResponse;
import com.nomnom.backend.service.WalletService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ApiResponse<WalletEntity> get(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        WalletEntity wallet = walletService.getWallet(userId);
        if (wallet == null) {
            return ApiResponse.ok(walletService.createWallet(userId, 1));
        }
        return ApiResponse.ok(wallet);
    }

    @GetMapping("/info")
    public ApiResponse<List<WalletDetailResponse>> getInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        WalletEntity wallet = walletService.getWallet(userId);
        if (wallet == null) {
            return ApiResponse.fail("钱包不存在");
        }
        return ApiResponse.ok(walletService.getWalletInfo(wallet.getId().longValue()));
    }
}
