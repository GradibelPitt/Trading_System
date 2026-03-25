package com.exchange.account.service;

import com.exchange.account.entity.Balance;
import com.exchange.account.repository.BalanceRepository;
import com.exchange.common.exception.InsufficientFundsException;
import com.exchange.common.event.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    @Transactional(readOnly = true)
    public List<Balance> getBalances(String userId) {
        return balanceRepository.findByUserId(userId);
    }

    /**
     * Freeze funds when an order is placed.
     * BUY order  → freeze quote asset (e.g. USDT): price * qty
     * SELL order → freeze base asset (e.g. BTC):   qty
     */
    @Transactional
    public void freeze(String userId, String asset, BigDecimal amount) {
        Balance balance = getOrCreate(userId, asset);

        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new InsufficientFundsException(userId, asset);
        }

        balance.setAvailable(balance.getAvailable().subtract(amount));
        balance.setFrozen(balance.getFrozen().add(amount));
        balanceRepository.save(balance);
        log.debug("Frozen {} {} for user {}", amount, asset, userId);
    }

    /**
     * Unfreeze funds on order cancellation.
     */
    @Transactional
    public void unfreeze(String userId, String asset, BigDecimal amount) {
        Balance balance = balanceRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() -> new com.exchange.common.exception.ExchangeException(
                        "BALANCE_NOT_FOUND", "Balance not found for " + userId + " " + asset));

        balance.setFrozen(balance.getFrozen().subtract(amount));
        balance.setAvailable(balance.getAvailable().add(amount));
        balanceRepository.save(balance);
        log.debug("Unfrozen {} {} for user {}", amount, asset, userId);
    }

    /**
     * Settle a trade: deduct from frozen (already locked at order placement),
     * credit the received asset. Called when a TradeEvent arrives from Kafka.
     */
    @Transactional
    public void settle(TradeEvent trade) {
        String[] parts = trade.getInstrument().split("-");
        String base = parts[0];    // e.g. BTC
        String quote = parts[1];   // e.g. USDT
        BigDecimal notional = trade.getPrice().multiply(trade.getQuantity());

        // Determine maker/taker roles based on takerSide
        boolean takerIsBuyer = trade.getTakerSide() ==
                com.exchange.common.enums.OrderSide.BUY;

        String buyerId  = takerIsBuyer ? trade.getTakerUserId() : trade.getMakerUserId();
        String sellerId = takerIsBuyer ? trade.getMakerUserId() : trade.getTakerUserId();

        // Buyer: deduct USDT from frozen, credit BTC
        deductFrozen(buyerId,  quote, notional.add(trade.getTakerFee()));
        credit(buyerId,  base,  trade.getQuantity());

        // Seller: deduct BTC from frozen, credit USDT
        deductFrozen(sellerId, base,  trade.getQuantity());
        credit(sellerId, quote, notional.subtract(trade.getMakerFee()));

        log.info("Settled trade {} — buyer={} seller={} qty={} price={}",
                trade.getTradeId(), buyerId, sellerId,
                trade.getQuantity(), trade.getPrice());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void deductFrozen(String userId, String asset, BigDecimal amount) {
        Balance b = balanceRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() -> new com.exchange.common.exception.ExchangeException(
                        "BALANCE_NOT_FOUND", "Balance not found"));
        b.setFrozen(b.getFrozen().subtract(amount));
        balanceRepository.save(b);
    }

    private void credit(String userId, String asset, BigDecimal amount) {
        Balance b = getOrCreate(userId, asset);
        b.setAvailable(b.getAvailable().add(amount));
        balanceRepository.save(b);
    }

    private Balance getOrCreate(String userId, String asset) {
        return balanceRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseGet(() -> balanceRepository.save(
                        Balance.builder().userId(userId).asset(asset).build()));
    }
}
