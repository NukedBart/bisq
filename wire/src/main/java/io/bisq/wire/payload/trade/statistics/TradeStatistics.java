package io.bisq.wire.payload.trade.statistics;

import io.bisq.common.app.Capabilities;
import io.bisq.common.app.Version;
import io.bisq.common.locale.CurrencyUtil;
import io.bisq.common.monetary.Altcoin;
import io.bisq.common.monetary.AltcoinExchangeRate;
import io.bisq.common.monetary.Price;
import io.bisq.common.monetary.Volume;
import io.bisq.common.util.JsonExclude;
import io.bisq.wire.payload.CapabilityRequiringPayload;
import io.bisq.wire.payload.LazyProcessedStoragePayload;
import io.bisq.wire.payload.PersistedStoragePayload;
import io.bisq.wire.payload.crypto.PubKeyRing;
import io.bisq.wire.payload.offer.OfferPayload;
import io.bisq.wire.proto.Messages;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Immutable
public final class TradeStatistics implements LazyProcessedStoragePayload, CapabilityRequiringPayload, PersistedStoragePayload {
    private static final Logger log = LoggerFactory.getLogger(TradeStatistics.class);

    @JsonExclude
    private static final long serialVersionUID = Version.P2P_NETWORK_VERSION;
    @JsonExclude
    public static final long TTL = TimeUnit.DAYS.toMillis(30);

    // Payload
    public final String baseCurrency;
    public final String counterCurrency;
    public final OfferPayload.Direction direction;
    public final long tradePrice;
    public final long tradeAmount;
    public final long tradeDate;
    public final String paymentMethodId;
    public final long offerDate;
    public final boolean useMarketBasedPrice;
    public final double marketPriceMargin;
    public final long offerAmount;
    public final long offerMinAmount;
    public final String offerId;
    public final String depositTxId;
    @JsonExclude
    public final PubKeyRing pubKeyRing;

    public TradeStatistics(OfferPayload offerPayload,
                           Price tradePrice,
                           Coin tradeAmount,
                           Date tradeDate,
                           String depositTxId,
                           PubKeyRing pubKeyRing) {
        this(offerPayload.getDirection(),
                offerPayload.getBaseCurrencyCode(),
                offerPayload.getCounterCurrencyCode(),
                offerPayload.getPaymentMethodId(),
                offerPayload.getDate(),
                offerPayload.isUseMarketBasedPrice(),
                offerPayload.getMarketPriceMargin(),
                offerPayload.getAmount(),
                offerPayload.getMinAmount(),
                offerPayload.getId(),
                tradePrice.getValue(),
                tradeAmount.value,
                tradeDate.getTime(),
                depositTxId,
                pubKeyRing);
    }

    public TradeStatistics(OfferPayload.Direction direction,
                           String baseCurrency,
                           String counterCurrency,
                           String offerPaymentMethod,
                           long offerDate,
                           boolean offerUseMarketBasedPrice,
                           double offerMarketPriceMargin,
                           long offerAmount,
                           long offerMinAmount,
                           String offerId,
                           long tradePrice,
                           long tradeAmount,
                           long tradeDate,
                           String depositTxId,
                           PubKeyRing pubKeyRing) {
        this.direction = direction;
        this.baseCurrency = baseCurrency;
        this.counterCurrency = counterCurrency;
        this.paymentMethodId = offerPaymentMethod;
        this.offerDate = offerDate;
        this.useMarketBasedPrice = offerUseMarketBasedPrice;
        this.marketPriceMargin = offerMarketPriceMargin;
        this.offerAmount = offerAmount;
        this.offerMinAmount = offerMinAmount;
        this.offerId = offerId;

        this.tradePrice = tradePrice;
        this.tradeAmount = tradeAmount;
        this.tradeDate = tradeDate;
        this.depositTxId = depositTxId;
        this.pubKeyRing = pubKeyRing;
    }


    @Override
    public long getTTL() {
        return TTL;
    }

    @Override
    public PublicKey getOwnerPubKey() {
        return pubKeyRing.getSignaturePubKey();
    }

    @Override
    public List<Integer> getRequiredCapabilities() {
        return Collections.singletonList(
                Capabilities.Capability.TRADE_STATISTICS.ordinal()
        );
    }

    public Date getTradeDate() {
        return new Date(tradeDate);
    }

    public Price getTradePrice() {
        return Price.valueOf(getCurrencyCode(), tradePrice);
    }

    public String getCurrencyCode() {
        return CurrencyUtil.isCryptoCurrency(baseCurrency) ? baseCurrency : counterCurrency;
    }

    public Coin getTradeAmount() {
        return Coin.valueOf(tradeAmount);
    }

    public Volume getTradeVolume() {
        if (getTradePrice().getMonetary() instanceof Altcoin)
            return new Volume(new AltcoinExchangeRate((Altcoin) getTradePrice().getMonetary()).coinToAltcoin(getTradeAmount()));
        else
            return new Volume(new ExchangeRate((Fiat) getTradePrice().getMonetary()).coinToFiat(getTradeAmount()));
    }

    public String getOfferId() {
        return offerId;
    }

    @Override
    public Messages.StoragePayload toProtoBuf() {
        return Messages.StoragePayload.newBuilder().setTradeStatistics(Messages.TradeStatistics.newBuilder()
                .setTTL(TTL)
                .setBaseCurrency(baseCurrency)
                .setCounterCurrency(counterCurrency)
                .setDirection(Messages.OfferPayload.Direction.forNumber(direction.ordinal()))
                .setTradePrice(tradePrice)
                .setTradeAmount(tradeAmount)
                .setTradeDate(tradeDate)
                .setPaymentMethodId(paymentMethodId)
                .setOfferDate(offerDate)
                .setUseMarketBasedPrice(useMarketBasedPrice)
                .setMarketPriceMargin(marketPriceMargin)
                .setOfferAmount(offerAmount)
                .setOfferMinAmount(offerMinAmount)
                .setOfferId(offerId)
                .setDepositTxId(depositTxId)
                .setPubKeyRing((Messages.PubKeyRing) pubKeyRing.toProtoBuf())).build();
    }


    // We don't include the pubKeyRing as both traders might publish it if the offerer uses an old
    // version and update later (taker publishes first, then later offerer)
    // We also don't include the trade date as that is set locally and different for offerer and taker
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeStatistics)) return false;

        TradeStatistics that = (TradeStatistics) o;

        if (tradePrice != that.tradePrice) return false;
        if (tradeAmount != that.tradeAmount) return false;
        if (offerDate != that.offerDate) return false;
        if (useMarketBasedPrice != that.useMarketBasedPrice) return false;
        if (Double.compare(that.marketPriceMargin, marketPriceMargin) != 0) return false;
        if (offerAmount != that.offerAmount) return false;
        if (offerMinAmount != that.offerMinAmount) return false;
        if (baseCurrency != null ? !baseCurrency.equals(that.baseCurrency) : that.baseCurrency != null) return false;
        if (counterCurrency != null ? !counterCurrency.equals(that.counterCurrency) : that.counterCurrency != null)
            return false;

        if (direction != null && that.direction != null && direction.ordinal() != that.direction.ordinal())
            return false;
        else if ((direction == null && that.direction != null) || (direction != null && that.direction == null))
            return false;

        if (paymentMethodId != null ? !paymentMethodId.equals(that.paymentMethodId) : that.paymentMethodId != null)
            return false;
        if (getOfferId() != null ? !getOfferId().equals(that.getOfferId()) : that.getOfferId() != null) return false;
        return !(depositTxId != null ? !depositTxId.equals(that.depositTxId) : that.depositTxId != null);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = baseCurrency != null ? baseCurrency.hashCode() : 0;
        result = 31 * result + (counterCurrency != null ? counterCurrency.hashCode() : 0);
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        result = 31 * result + (int) (tradePrice ^ (tradePrice >>> 32));
        result = 31 * result + (int) (tradeAmount ^ (tradeAmount >>> 32));
        result = 31 * result + (paymentMethodId != null ? paymentMethodId.hashCode() : 0);
        result = 31 * result + (int) (offerDate ^ (offerDate >>> 32));
        result = 31 * result + (useMarketBasedPrice ? 1 : 0);
        temp = Double.doubleToLongBits(marketPriceMargin);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (offerAmount ^ (offerAmount >>> 32));
        result = 31 * result + (int) (offerMinAmount ^ (offerMinAmount >>> 32));
        result = 31 * result + (offerId != null ? offerId.hashCode() : 0);
        result = 31 * result + (depositTxId != null ? depositTxId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TradeStatistics{" +
                "baseCurrency='" + baseCurrency + '\'' +
                ", counterCurrency=" + counterCurrency +
                ", direction=" + direction +
                ", tradePrice=" + tradePrice +
                ", tradeAmount=" + tradeAmount +
                ", tradeDate=" + tradeDate +
                ", paymentMethod='" + paymentMethodId + '\'' +
                ", offerDate=" + offerDate +
                ", useMarketBasedPrice=" + useMarketBasedPrice +
                ", marketPriceMargin=" + marketPriceMargin +
                ", offerAmount=" + offerAmount +
                ", offerMinAmount=" + offerMinAmount +
                ", offerId='" + getOfferId() + '\'' +
                ", depositTxId='" + depositTxId + '\'' +
                ", pubKeyRing=" + pubKeyRing +
                ", hashCode=" + hashCode() +
                '}';
    }
}
