package com.exchange.account.service;

import com.exchange.account.dto.BalanceResponse;
import com.exchange.account.entity.Balance;
import com.exchange.account.repository.BalanceRepository;
import com.exchange.common.exception.ExchangeException;
import com.exchange.common.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final BalanceRepository balanceRepository;

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalances(String userId) {
        return balanceRepository.findByUserId(userId)
                .stream().map(BalanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId, String asset) {
        return balanceRepository.findByUserIdAndAsset(userId, asset)
                .map(BalanceResponse::from)
                .orElseThrow(() -> new ExchangeException("BALANCE_NOT_FOUND",
                        "No balance for user=" + userId + " asset=" + asset));
    }

    // ------------------------------------------------------------------
    // Mutations — all use pessimistic locking to prevent concurrent issues
    // ------------------------------------------------------------------

    /**
     * Freeze funds when an order is placed.
     * available -= amount, frozen += amount
     */
    @Transactional
    public void freeze(String userId, String asset, BigDecimal amount) {
        Balance balance = getBalanceLocked(userId, asset);
        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new InsufficientFundsException(userId, asset);
        }
        balance.setAvailable(balance.getAvailable().subtract(amount));
        balance.setFrozen(balance.getFrozen().add(amount));
        balanceRepository.save(balance);
        log.debug("Frozen {} {} for user {}", amount, asset, userId);
    }

    /**
     * Unfreeze funds when an order is cancelled.
     * frozen -= amount, available += amount
     */
    @Transactional
    public void unfreeze(String userId, String asset, BigDecimal amount) {
        Balance balance = getBalanceLocked(userId, asset);
        if (balance.getFrozen().compareTo(amount) < 0) {
            throw new ExchangeException("INSUFFICIENT_FROZEN",
                    "Frozen balance insufficient to unfreeze " + amount + " " + asset);
        }
        balance.setFrozen(balance.getFrozen().subtract(amount));
        balance.setAvailable(balance.getAvailable().add(amount));
        balanceRepository.save(balance);
        log.debug("Unfrozen {} {} for user {}", amount, asset, userId);
    }

    /**
     * Settle a trade fill: deduct from seller's frozen, credit buyer's available.
     * Called by TradeEventConsumer after a match.
     */
    @Transactional
    public void settleTrade(
            String buyerUserId, String sellerUserId,
            String baseAsset, String quoteAsset,
            BigDecimal baseQty, BigDecimal quoteAmount) {

        // Deduct quote (e.g. USDT) from buyer's frozen
        Balance buyerQuote = getBalanceLocked(buyerUserId, quoteAsset);
        if (buyerQuote.getFrozen().compareTo(quoteAmount) < 0) {
            throw new ExchangeException("SETTLE_ERROR", "Buyer frozen quote insufficient");
        }
        buyerQuote.setFrozen(buyerQuote.getFrozen().subtract(quoteAmount));
        balanceRepository.save(buyerQuote);

        // Credit base (e.g. BTC) to buyer's available
        Balance buyerBase = balanceRepository.findByUserIdAndAsset(buyerUserId, baseAsset)
                .orElse(Balance.builder().userId(buyerUserId).asset(baseAsset).build());
        buyerBase.setAvailable(buyerBase.getAvailable().add(baseQty));
        balanceRepository.save(buyerBase);

        // Deduct base from seller's frozen
        Balance sellerBase = getBalanceLocked(sellerUserId, baseAsset);
        if (sellerBase.getFrozen().compareTo(baseQty) < 0) {
            throw new ExchangeException("SETTLE_ERROR", "Seller frozen base insufficient");
        }
        sellerBase.setFrozen(sellerBase.getFrozen().subtract(baseQty));
        balanceRepository.save(sellerBase);

        // Credit quote to seller's available
        Balance sellerQuote = balanceRepository.findByUserIdAndAsset(sellerUserId, quoteAsset)
                .orElse(Balance.builder().userId(sellerUserId).asset(quoteAsset).build());
        sellerQuote.setAvailable(sellerQuote.getAvailable().add(quoteAmount));
        balanceRepository.save(sellerQuote);

        log.info("Trade settled: buyer={} seller={} base={} qty={} quote={} amount={}",
                buyerUserId, sellerUserId, baseAsset, baseQty, quoteAsset, quoteAmount);
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private Balance getBalanceLocked(String userId, String asset) {
        return balanceRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() -> new ExchangeException("BALANCE_NOT_FOUND",
                        "No balance for user=" + userId + " asset=" + asset));
    }
}
