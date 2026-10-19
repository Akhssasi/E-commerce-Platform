package com.example.market.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated payment gateway wrapped in a circuit breaker to demonstrate external-call resilience.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Component
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);

    private final double failureRate;

    public PaymentGatewayClient(@Value("${market.payment.simulated-failure-rate:0.05}") double failureRate) {
        this.failureRate = failureRate;
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "paymentUnavailable")
    public PaymentResult charge(String idempotencyKey, BigDecimal amount, String customerRef) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Invalid payment amount");
        }
        log.info("Payment charge idempotencyKey={} amount={} customer={}", idempotencyKey, amount, customerRef);

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new PaymentUnavailableException("Transient failure in the payment provider", null);
        }
        return new PaymentResult("PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(), "APPROVED");
    }

    @SuppressWarnings("unused")
    public PaymentResult paymentUnavailable(String idempotencyKey, BigDecimal amount, String customerRef, Throwable ex) {
        log.warn("Payment fallback triggered: {}", ex.toString());
        throw new PaymentUnavailableException("The payment provider is temporarily unavailable", ex);
    }

    public record PaymentResult(String reference, String status) { }

    public static class PaymentUnavailableException extends RuntimeException {
        public PaymentUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
